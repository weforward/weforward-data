/**
 * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package cn.weforward.data.mysql.counter;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.util.LruCache.DirtyData;
import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.data.counter.Counter;
import cn.weforward.data.counter.support.CounterItem;
import cn.weforward.data.counter.support.DbCounter;
import cn.weforward.data.counter.support.DbCounterFactory;
import cn.weforward.data.jdbc.DataProvider;
import cn.weforward.data.jdbc.SqlString;
import cn.weforward.data.jdbc.TemplateJdbc;
import cn.weforward.data.mysql.MysqlConst;
import cn.weforward.data.util.Flusher;

/**
 * 在mysql下的计数器实现
 * 
 * @author liangyi
 *
 */
public class MysqlCounterFactory extends DbCounterFactory {
	protected final static Logger _Logger = LoggerFactory.getLogger(MysqlCounterFactory.class);

	protected DataProvider m_DataProvider;
	private String m_FieldName;

	public MysqlCounterFactory(String serverId, String connectionString, Flusher flusher) {
		this(serverId, MysqlConst.DEFAULT_DRICER_CLASS, connectionString, MysqlConst.DEFAULT_POOL_MAX_SIZE, flusher);
	}

	public MysqlCounterFactory(String serverId, String driverClass, String connectionString, int maxSize,
			Flusher flusher) {
		super(serverId);
		m_DataProvider = new DataProvider(driverClass, connectionString, maxSize);
		m_FieldName = "v_" + getServerId();
		setFlusher(flusher);
	}

	@Override
	protected CounterItem doLoad(DbCounter counter, String id) {
		ResultSet rs = null;
		TemplateJdbc jdbc = null;
		try {
			String sql;
			StringBuilder builder = StringBuilderPool._128.poll();
			try {
				builder.append("SELECT * FROM `").append(counter.getLableName()).append("` WHERE '");
				SqlString.escape(id, builder).append("'=id");
				sql = builder.toString();
			} finally {
				StringBuilderPool._8k.offer(builder);
			}
			jdbc = ((MysqlCounter) counter).getDataProvider().beginTranstacion();
			rs = jdbc.sqlExecuteQuery(sql);
			if (!rs.next()) {
				// 没有计数项
				jdbc.commit();
				jdbc = null;
				return null;
			}
			// 遍历记录各服务器标识下字段的值
			CounterItem item = new CounterItem(id);
			ResultSetMetaData md = rs.getMetaData();
			String current = getFieldName().toLowerCase();
			for (int i = 1; i <= md.getColumnCount(); i++) {
				String name = md.getColumnName(i).toLowerCase();
				if (name.startsWith("v_")) {
					if (name.equals(current)) {
						// 当前的
						item.value = rs.getLong(i);
					} else {
						// 其它的
						item.hold += rs.getLong(i);
					}
				}
			}
			jdbc.commit();
			jdbc = null;
			return item;
		} catch (SQLException e) {
			_Logger.error(id + " 计数器项加载失败", e);
		} finally {
			if (null != rs) {
				try {
					rs.close();
				} catch (Throwable e) {
					_Logger.warn("忽略关闭异常", e);
				}
			}
			if (null != jdbc && !jdbc.isCompleted()) {
				jdbc.rollback();
			}
		}
		return null;
	}

	@Override
	protected void doUpdate(DbCounter counter, DirtyData<CounterItem> data) {
		TemplateJdbc jdbc = null;
		data.begin();
		try {
			jdbc = ((MysqlCounter) counter).getDataProvider().beginTranstacion();
			while (data.hasNext()) {
				update(jdbc, counter, data.next());
			}
			jdbc.commit();
			jdbc = null;
			data.commit();
			data = null;
		} catch (SQLException e) {
			_Logger.error("更新计数项失败 " + data, e);
		} finally {
			if (null != jdbc && !jdbc.isCompleted()) {
				jdbc.rollback();
				if (null != data) {
					data.rollback();
				}
			}
		}
	}

	private void update(TemplateJdbc jdbc, DbCounter counter, CounterItem item) throws SQLException {
		// 先尝试更新计数项
		String sql;
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			builder.append("UPDATE `").append(counter.getLableName()).append("` SET `").append(getFieldName())
					.append("`=").append(item.value).append(" WHERE id='");
			SqlString.escape(item.id, builder).append("'");
			sql = builder.toString();
		} finally {
			StringBuilderPool._128.offer(builder);
			builder = null;
		}
		if (0 == jdbc.sqlExecuteUpdate(sql)) {
			// 若未有记录则插入
			builder = StringBuilderPool._128.poll();
			try {
				builder.append("INSERT INTO `").append(counter.getLableName()).append("` (id,`").append(getFieldName())
						.append("`) VALUES ('");
				SqlString.escape(item.id, builder).append("',").append(item.value).append(")");
				sql = builder.toString();
			} finally {
				StringBuilderPool._128.offer(builder);
				builder = null;
			}
			jdbc.sqlExecuteUpdate(sql);
		}
	}

	@Override
	protected void doNew(DbCounter counter, CounterItem item) {
		TemplateJdbc jdbc = null;
		try {
			jdbc = ((MysqlCounter) counter).getDataProvider().beginTranstacion();
			update(jdbc, counter, item);
			jdbc.commit();
			jdbc = null;
		} catch (SQLException e) {
			_Logger.error("更新计数项失败 " + item, e);
		} finally {
			if (null != jdbc && !jdbc.isCompleted()) {
				jdbc.rollback();
			}
		}
	}

	@Override
	protected Counter doCreateCounter(String name) {
		MysqlCounter counter = new MysqlCounter(name, this);
		return counter;
	}

	private String getFieldName() {
		return m_FieldName;
	}

	/**
	 * 初始化计数器的表结构
	 * 
	 * @param tableName 表名
	 */
	protected void init(String tableName) {
		TemplateJdbc jdbc = null;
		String sql = null;
		try {
			// 先尝试创建表
			sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "`(id VARCHAR(200),`" + getFieldName()
					+ "` BIGINT DEFAULT 0,PRIMARY KEY(id))";
			jdbc = m_DataProvider.beginTranstacion();
			jdbc.sqlExecuteUpdate(sql);
			// 再尝试调整表结构
			sql = "ALTER TABLE `" + tableName + "` ADD `" + getFieldName() + "` BIGINT DEFAULT 0";
			try {
				jdbc.sqlExecuteUpdate(sql);
			} catch (SQLException e) {
				String msg = e.getMessage();
				if (null == msg || !msg.startsWith("Duplicate column name")) {
					throw e;
				}
			}
			jdbc.commit();
			jdbc = null;
		} catch (SQLException e) {
			_Logger.error("初始化计数器表失败 " + sql, e);
		} finally {
			if (null != jdbc && !jdbc.isCompleted()) {
				jdbc.rollback();
			}
		}
	}

}

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
package cn.weforward.data.mysql.log;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.weforward.common.KvPair;
import cn.weforward.common.ResultPage;
import cn.weforward.common.util.SimpleKvPair;
import cn.weforward.data.exception.DataAccessException;
import cn.weforward.data.jdbc.TemplateJdbc;
import cn.weforward.data.log.BusinessLog;
import cn.weforward.data.log.support.AbstractBusinessLogger;
import cn.weforward.data.log.vo.BusinessLogVo;
import cn.weforward.data.mysql.util.MysqlResultPage;
import cn.weforward.data.mysql.util.SqlColumn;
import cn.weforward.data.mysql.util.SqlColumnType;
import cn.weforward.data.mysql.util.SqlTable;
import cn.weforward.data.mysql.util.SqlUtil;
import cn.weforward.data.persister.Condition;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.support.datatype.SimpleDtString;

/**
 * 基于mysql的日志记录器
 * 
 * @author daibo
 *
 */
public class MysqlBusinessLogger extends AbstractBusinessLogger {
	/** id属性 */
	public final static String ID = Condition.ID;
	/** 动作（部分） */
	public final static String ACTION = "ac";
	/** 作者 */
	public final static String AUTHOR = "a";
	/** 备注 */
	public final static String NOTE = "n";
	/** 什么（部分） */
	public final static String WHAT = "w";
	/** 工厂 */
	private MysqlBusinessLoggerFactory m_Factory;
	/** 默认的字符串长度 */
	private int m_DefaultStringLength;
	/** sql表 */
	private SqlTable m_Table;

	public MysqlBusinessLogger(MysqlBusinessLoggerFactory factory, String name, String serverId,
			int defaultStringLength) {
		super(name);
		m_Factory = factory;
		m_DefaultStringLength = defaultStringLength;
	}

	@Override
	public String getServerId() {
		return m_Factory.getServerId();
	}

	private SqlTable getTable() {
		if (null != m_Table) {
			return m_Table;
		}
		synchronized (this) {
			if (null != m_Table) {
				return m_Table;
			}
			SqlColumnType strinType = SqlColumnType.getStringType(m_DefaultStringLength);
			SqlColumn id = new SqlColumn(ID, strinType);
			List<SqlColumn> myColumns = new ArrayList<>();
			myColumns.add(new SqlColumn(AUTHOR, strinType));
			myColumns.add(new SqlColumn(ACTION, strinType));
			myColumns.add(new SqlColumn(WHAT, strinType));
			myColumns.add(new SqlColumn(NOTE, strinType));
			m_Table = SqlUtil.openTable(m_Factory.getProvider(), getTabelName(), id, myColumns,
					Collections.emptyList());
		}
		return m_Table;
	}

	private String getTabelName() {
		return getName().toLowerCase() + "_log";
	}

	@Override
	public void writeLog(BusinessLog log) {
		SqlTable table = getTable();
		String mid = log.getId();
		Map<String, DtBase> content = new HashMap<>();
		content.put(AUTHOR, new SimpleDtString(log.getAuthor()));
		content.put(ACTION, new SimpleDtString(log.getAction()));
		content.put(WHAT, new SimpleDtString(log.getWhat()));
		content.put(NOTE, new SimpleDtString(log.getNote()));
		synchronized (table) {
			SqlUtil.checkTable(m_Factory.getProvider(), table, Collections.emptyList(), content, m_DefaultStringLength);
		}
		TemplateJdbc jdbc = null;
		try {
			jdbc = m_Factory.getProvider().beginTranstacion();
			KvPair<String, DtBase> id = SimpleKvPair.valueOf(ID, new SimpleDtString(mid));
			String sql = SqlUtil.getInsertSql(getTabelName(), id, content);
			jdbc.sqlExecuteUpdate(sql);
			jdbc.commit();
		} catch (SQLException e) {
			throw new DataAccessException("写入异常", e);
		} finally {
			if (null != jdbc && !jdbc.isCompleted()) {
				jdbc.rollback();
			}
		}

	}

	@Override
	public ResultPage<BusinessLog> searchLogs(String target, Date begin, Date end) {
		String where = SqlUtil.wrapField(ID) + "BETWEEN "
				+ SqlUtil.wrapValue(toId(target, null == begin ? 0 : begin.getTime())) + " AND "
				+ SqlUtil.wrapValue(toId(target, null == end ? Long.MAX_VALUE : end.getTime()));
		return new MysqlResultPage<BusinessLog>(m_Factory.getProvider(), getTabelName(), null, where, null) {

			@Override
			protected BusinessLog to(ResultSet rs) throws SQLException {
				BusinessLogVo vo = createVoById(rs.getString(ID));
				vo.setAction(rs.getString("ac"));
				vo.setAuthor(rs.getString("a"));
				vo.setNote(rs.getString("n"));
				vo.setWhat(rs.getString("w"));
				return vo;
			}
		};
	}

}

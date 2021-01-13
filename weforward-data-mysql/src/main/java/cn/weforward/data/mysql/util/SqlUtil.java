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
package cn.weforward.data.mysql.util;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.KvPair;
import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.exception.DataAccessException;
import cn.weforward.data.jdbc.DataProvider;
import cn.weforward.data.jdbc.SqlString;
import cn.weforward.data.jdbc.TemplateJdbc;
import cn.weforward.data.mysql.persister.MysqlPersister;
import cn.weforward.data.mysql.persister.util.MysqlUtil;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtBoolean;
import cn.weforward.protocol.datatype.DtDate;
import cn.weforward.protocol.datatype.DtList;
import cn.weforward.protocol.datatype.DtNumber;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.datatype.DtString;

/**
 * sql工具类
 * 
 * @author daibo
 *
 */
public class SqlUtil {
	/** 日志记录器 */
	protected final static Logger _Logger = LoggerFactory.getLogger(MysqlPersister.class);

	/**
	 * 包装属性
	 * 
	 * @param field 属性名
	 * @return 包装后的属性值
	 */
	public static String wrapField(String field) {
		return "`" + field + "`";
	}

	/**
	 * 包装值
	 * 
	 * @param value 属性值
	 * @return 包装后的值
	 */
	public static String wrapValue(String value) {
		return "'" + SqlString.escape(value) + "'";
	}

	/**
	 * 打开表
	 * 
	 * @param provider  供应商
	 * @param tableName 表
	 * @param id        主键
	 * @param myColumns 列
	 * @param myIndexs  索引
	 * @return sql表
	 */
	public static SqlTable openTable(DataProvider provider, String tableName, SqlColumn id, List<SqlColumn> myColumns,
			List<SqlIndex> myIndexs) {
		ConcurrentMap<String, SqlColumnType> columns = new ConcurrentHashMap<>();
		List<String> existsIndexs = new ArrayList<>();
		SQLException error = null;
		{
			ResultSet rs = null;
			TemplateJdbc jdbc = null;
			try {
				String sql = "select * from " + tableName + " where `" + id.getName() + "`='id'";
				jdbc = provider.beginTranstacion();
				rs = jdbc.sqlExecuteQuery(sql);
				ResultSetMetaData md = rs.getMetaData();
				for (int i = 1; i <= md.getColumnCount(); i++) {
					String name = md.getColumnName(i);
					columns.put(name, new SqlColumnType(md.getColumnTypeName(i), md.getColumnDisplaySize(i)));
				}
				jdbc.commit();
			} catch (SQLException e) {
				error = e;
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
		}
		if (null != error) {
			if (isNoExistTabelException(tableName, error)) {
				ResultSet rs = null;
				TemplateJdbc jdbc = null;
				try {
					jdbc = provider.beginTranstacion();
					String sql = getRegisterSql(tableName, id, myColumns);
					jdbc.sqlExecuteUpdate(sql);
					jdbc.commit();
				} catch (SQLException ee) {
					throw new DataAccessException("注册" + tableName + "时发生异常", ee);
				} finally {
					if (null != rs) {
						try {
							rs.close();
						} catch (Throwable e) {
							_Logger.warn("忽略关闭异常", e);
						}
					}
					if (!jdbc.isCompleted()) {
						jdbc.rollback();
					}
				}
				columns.put(id.getName(), id.getType());
				myColumns.forEach((e) -> columns.put(id.getName(), id.getType()));
			} else {
				throw new DataAccessException("查询" + tableName + "时发生异常", error);
			}
		} else {
			String sql = "show index from " + tableName;
			ResultSet rs = null;
			TemplateJdbc jdbc = null;
			try {
				jdbc = provider.beginTranstacion();
				rs = jdbc.sqlExecuteQuery(sql);
				while (rs.next()) {
					existsIndexs.add(rs.getString("Column_name"));
				}
				jdbc.commit();
			} catch (SQLException e) {
				error = e;
			} finally {
				if (null != rs) {
					try {
						rs.close();
					} catch (Throwable e) {
						_Logger.warn("忽略关闭异常", e);
					}
				}
				if (!jdbc.isCompleted()) {
					jdbc.rollback();
				}
			}
		}
		String sql = "create index  ? on `" + tableName + "`(`?`)";
		TemplateJdbc jdbc = null;
		try {
			jdbc = provider.beginTranstacion();
			PreparedStatement statement = jdbc.sqlPrepareStatement(sql);
			for (SqlIndex index : myIndexs) {
				if (null == index) {
					continue;
				}
				if (columns.get(index.getColumnName()) == null) {
					continue;
				}
				if (existsIndexs.contains(index.getIndexName())) {
					continue;
				}
				statement.setString(1, index.getIndexName());
				statement.setString(2, index.getColumnName());
				statement.executeUpdate();
			}
			jdbc.commit();
		} catch (SQLException e) {
			_Logger.warn("忽略创建索引异常", e);
		} finally {
			if (!jdbc.isCompleted()) {
				jdbc.rollback();
			}
		}
		return new SqlTable(tableName, columns);
	}

	/**
	 * 是否为表不存在的异常
	 * 
	 * @param tablename 表名
	 * @param e         异常类
	 * @return 是否为表不存在的异常
	 */
	public static boolean isNoExistTabelException(String tablename, SQLException e) {
		// com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: Table
		// 'bill.ticketbill202003' doesn't exist
		String message = e.getMessage();
		return message.contains("Table") && message.contains("doesn't exist");
	}

	public static void checkTable(DataProvider provider, SqlTable table, List<SqlIndex> myIndexs,
			Map<String, DtBase> content, int defaultStringLenth) {
		List<SqlColumn> miss = new ArrayList<>();
		List<SqlColumn> change = new ArrayList<>();
		for (Map.Entry<String, DtBase> e : content.entrySet()) {
			String name = e.getKey();
			DtBase value = e.getValue();
			if (null == value) {
				continue;
			}
			SqlColumnType currentType = SqlColumnType.getType(value, defaultStringLenth);
			SqlColumnType type = table.getType(name);
			if (null == type) {
				miss.add(new SqlColumn(name, currentType));
			} else if (!StringUtil.eq(type.getName(), type.getName()) || type.getLength() < currentType.getLength()) {
				change.add(new SqlColumn(name, currentType));
			}
		}
		List<SqlIndex> needIndex = new ArrayList<>();
		if (!miss.isEmpty()) {
			TemplateJdbc jdbc = null;
			try {
				jdbc = provider.beginTranstacion();
				for (SqlColumn c : miss) {
					String sql = "ALTER TABLE " + table.getName() + " ADD " + c.getName() + " "
							+ c.getType().toString();
					jdbc.sqlExecuteUpdate(sql);
					table.put(c);
					for (SqlIndex index : myIndexs) {
						if (StringUtil.eq(index.getColumnName(), c.getName())) {
							needIndex.add(index);
						}
					}
				}
				jdbc.commit();
			} catch (SQLException e) {
				throw new DataAccessException("更新列异常", e);
			} finally {
				if (null != jdbc && !jdbc.isCompleted()) {
					jdbc.rollback();
				}
			}
		}
		if (!change.isEmpty()) {
			TemplateJdbc jdbc = null;
			try {
				jdbc = provider.beginTranstacion();
				for (SqlColumn c : change) {
					String sql = "ALTER TABLE " + table.getName() + " MODIFY COLUMN  " + c.getName() + " "
							+ c.getType().toString();
					jdbc.sqlExecuteUpdate(sql);
					table.put(c);
				}
				jdbc.commit();
			} catch (SQLException e) {
				throw new DataAccessException("更新列异常", e);
			} finally {
				if (null != jdbc && !jdbc.isCompleted()) {
					jdbc.rollback();
				}
			}
		}
		if (!needIndex.isEmpty()) {
			String sql = "create index  ? on `" + table.getName() + "`(`?`)";
			TemplateJdbc jdbc = null;
			try {
				jdbc = provider.beginTranstacion();
				PreparedStatement statement = jdbc.sqlPrepareStatement(sql);
				for (SqlIndex index : myIndexs) {
					statement.setString(1, index.getIndexName());
					statement.setString(2, index.getColumnName());
					statement.executeUpdate();
				}
				jdbc.commit();
			} catch (SQLException e) {
				_Logger.warn("忽略创建索引异常", e);
			} finally {
				if (!jdbc.isCompleted()) {
					jdbc.rollback();
				}
			}
		}
	}

	/**
	 * 获取插入语句
	 * 
	 * @param tabelName 表名
	 * @param id        id值
	 * @param content   内容
	 * @return 语句
	 */
	public static String getInsertSql(String tabelName, KvPair<String, DtBase> id, Map<String, DtBase> content) {
		StringBuilder into = StringBuilderPool._128.poll();
		StringBuilder value = null;
		try {
			value = StringBuilderPool._8k.poll();
			into.append("INSERT INTO ").append(tabelName).append("(");
			into.append(id.getKey());
			value.append("VALUES(");
			toValue(id.getValue(), value);
			Iterator<Map.Entry<String, DtBase>> entry = content.entrySet().iterator();
			while (entry.hasNext()) {
				Map.Entry<String, DtBase> e = entry.next();
				if (e.getValue() == null) {
					continue;
				}
				into.append(',');
				into.append(e.getKey());
				value.append(",");
				toValue(e.getValue(), value);
			}
			into.append(')');
			value.append(')');
			into.append(" ").append(value);
			return into.toString();
		} finally {
			StringBuilderPool._128.offer(into);
			if (null != value) {
				StringBuilderPool._8k.offer(value);
			}
		}
	}

	private static void toValue(DtBase value, StringBuilder builder) {
		if (null == value) {
			return;
		}
		if (value instanceof DtObject) {
			DtObject object = (DtObject) value;
			builder.append("'");
			try {
				MysqlUtil.formatObject(object, builder);
			} catch (IOException e) {
				throw new DataAccessException("转换数据异常", e);
			}
			builder.append("'");
			return;
		}
		if (value instanceof DtList) {
			DtList object = (DtList) value;
			builder.append("'");
			try {
				MysqlUtil.formatList(object, builder);
			} catch (IOException e) {
				throw new DataAccessException("转换数据异常", e);
			}
			builder.append("'");
			return;
		}
		if (value instanceof DtDate) {
			builder.append("'").append(((DtDate) value).value()).append("'");
			return;
		}
		if (value instanceof DtString) {
			builder.append("'");
			SqlString.escape(((DtString) value).value(), builder);
			builder.append("'");
			return;
		}
		if (value instanceof DtNumber) {
			DtNumber n = (DtNumber) value;
			if (n.isDouble()) {
				builder.append((n.valueDouble()));
				return;
			}
			if (n.isLong()) {
				builder.append(n.valueLong());
				return;
			}
			if (n.isInt()) {
				builder.append(n.valueInt());
				return;
			}
			builder.append(n.valueDouble());
			return;
		}
		if (value instanceof DtBoolean) {
			builder.append(((DtBoolean) value).value());
			return;
		}
		throw new UnsupportedOperationException("不支持的数据类型:" + value.getClass());
	}

	/*
	 * 获取注册表sql
	 */
	private static String getRegisterSql(String tabelName, SqlColumn id, List<SqlColumn> columns) {
		StringBuilder sb = new StringBuilder();
		sb.append("create table if not exists `").append(tabelName).append("` ( ");
		sb.append(" `").append(id.getName()).append("` ").append(id.getType()).append(" ,");
		columns.forEach((e) -> sb.append(" `").append(e.getName()).append("` ").append(e.getType()).append(" ,"));
		sb.append("PRIMARY KEY(`").append(id.getName()).append("`) );");
		return sb.toString();
	}

}

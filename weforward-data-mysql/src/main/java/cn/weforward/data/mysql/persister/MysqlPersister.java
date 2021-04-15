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
package cn.weforward.data.mysql.persister;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.KvPair;
import cn.weforward.common.NameItem;
import cn.weforward.common.ResultPage;
import cn.weforward.common.util.ListUtil;
import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.UniteId;
import cn.weforward.data.exception.DataAccessException;
import cn.weforward.data.jdbc.DataProvider;
import cn.weforward.data.jdbc.SqlString;
import cn.weforward.data.jdbc.TemplateJdbc;
import cn.weforward.data.mysql.EntityListener;
import cn.weforward.data.mysql.EntityWatcher;
import cn.weforward.data.mysql.persister.util.MysqlUtil;
import cn.weforward.data.mysql.util.MysqlResultPage;
import cn.weforward.data.mysql.util.SqlColumnType;
import cn.weforward.data.mysql.util.SqlUtil;
import cn.weforward.data.persister.ChangeListener;
import cn.weforward.data.persister.Condition;
import cn.weforward.data.persister.ObjectWithVersion;
import cn.weforward.data.persister.OrderBy;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.PersistentListener;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.persister.Reloadable;
import cn.weforward.data.persister.support.AbstractPersister;
import cn.weforward.data.util.AutoObjectMapper;
import cn.weforward.data.util.Flushable;
import cn.weforward.data.util.Flusher;
import cn.weforward.data.util.VersionTags;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtBoolean;
import cn.weforward.protocol.datatype.DtDate;
import cn.weforward.protocol.datatype.DtList;
import cn.weforward.protocol.datatype.DtNumber;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.datatype.DtString;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.serial.JsonDtList;
import cn.weforward.protocol.serial.JsonDtObject;
import cn.weforward.protocol.support.datatype.SimpleDtBoolean;
import cn.weforward.protocol.support.datatype.SimpleDtDate;
import cn.weforward.protocol.support.datatype.SimpleDtNumber;
import cn.weforward.protocol.support.datatype.SimpleDtObject;
import cn.weforward.protocol.support.datatype.SimpleDtString;

/**
 * 基于mysql的持久类实现
 * 
 * @author daibo
 *
 * @param <E>
 *            持久类
 */
public class MysqlPersister<E extends Persistent> extends AbstractPersister<E>
		implements EntityListener {
	/** 日志记录器 */
	protected final static Logger _Logger = LoggerFactory.getLogger(MysqlPersister.class);
	/** 映射表 */
	final ObjectMapper<E> m_Mapper;
	/** 数据提供者 */
	final DataProvider m_Provider;
	/** 列 */
	protected ConcurrentMap<String, SqlColumnType> m_Columns;
	/** 列锁 */
	protected Object COLUMNS_LOCK = new Object();
	/** id属性 */
	public final static String ID = Condition.ID;
	/** 最后修改时间 */
	public final static String LASTMODIFIED = Condition.LASTMODIFIED;
	/** 版本 */
	public final static String VERSION = Condition.VERSION;
	/** 服务器id */
	public final static String SERVERID = Condition.SERVERID;
	/** 控制实例id */
	public final static String DRIVEIT = Condition.DRIVEIT;
	/** 默认的字符串长度 */
	private int m_DefaultStringLength;
	/** 需要索引的属性列表 */
	private List<String> m_NeedIndexs;
	/** 监控 */
	private EntityWatcher m_Watcher;

	public MysqlPersister(DataProvider provider, ObjectMapper<E> mapper, int defaultStringLength) {
		super(mapper.getName());
		m_Mapper = mapper;
		m_Name = mapper.getName();
		m_Provider = provider;
		m_NeedIndexs = new ArrayList<>();
		m_NeedIndexs.add(LASTMODIFIED);
		if (m_Mapper instanceof AutoObjectMapper) {
			Enumeration<String> indexAttribute = ((AutoObjectMapper<E>) m_Mapper)
					.getIndexAttributeNames();
			while (indexAttribute.hasMoreElements()) {
				m_NeedIndexs.add(indexAttribute.nextElement());
			}
		}
		m_DefaultStringLength = defaultStringLength;
	}

	public DataProvider getProvider() {
		return m_Provider;
	}

	public void setFlusher(Flusher flusher) {
		super.setFlusher(flusher);
		getFlusher().flush(new InitFlushable());
	}

	public ConcurrentMap<String, SqlColumnType> getColumns() {
		if (null == m_Columns) {
			synchronized (COLUMNS_LOCK) {
				if (null == m_Columns) {
					m_Columns = doInit();
				}
			}
		}
		return m_Columns;
	}

	private ConcurrentMap<String, SqlColumnType> doInit() {
		DataProvider provider = m_Provider;
		String tablename = getTabelName();

		ConcurrentMap<String, SqlColumnType> columns = new ConcurrentHashMap<>();
		SQLException error = null;
		{
			ResultSet rs = null;
			TemplateJdbc jdbc = null;
			try {
				String sql = "select * from " + tablename + " where `" + ID + "`='id'";
				jdbc = provider.beginTranstacion();
				rs = jdbc.sqlExecuteQuery(sql);
				ResultSetMetaData md = rs.getMetaData();
				for (int i = 1; i <= md.getColumnCount(); i++) {
					String name = md.getColumnName(i);
					columns.put(name,
							new SqlColumnType(md.getColumnTypeName(i), md.getColumnDisplaySize(i)));
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
		List<String> indexs = new ArrayList<String>(getNeedIndexs());
		if (null != error) {
			if (isNoExistTabelException(tablename, error)) {
				TemplateJdbc jdbc = null;
				try {
					jdbc = provider.beginTranstacion();
					String sql = getRegisterSql();
					jdbc.sqlExecuteUpdate(sql);
					jdbc.commit();
				} catch (SQLException ee) {
					throw new DataAccessException("注册" + tablename + "时发生异常", ee);
				} finally {
					if (!jdbc.isCompleted()) {
						jdbc.rollback();
					}
				}
				columns.put(ID, getStringType());
				columns.put(VERSION, getStringType());
				columns.put(SERVERID, getStringType());
				columns.put(DRIVEIT, getStringType());
				columns.put(LASTMODIFIED, getLongType());
			} else {
				throw new DataAccessException("查询" + tablename + "时发生异常", error);
			}
		} else {
			String sql = "show index from " + getTabelName();
			ResultSet rs = null;
			TemplateJdbc jdbc = null;
			try {
				jdbc = provider.beginTranstacion();
				rs = jdbc.sqlExecuteQuery(sql);
				while (rs.next()) {
					indexs.remove(rs.getString("Column_name"));
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
		for (String index : indexs) {
			if (columns.get(index) == null) {
				continue;
			}
			createIndex(index);
		}
		return columns;
	}

	private void createIndex(String index) {
		TemplateJdbc jdbc = null;
		String indexsql = "create index " + index + "_doc on `" + getTabelName() + "`(`" + index
				+ "`)";
		try {
			jdbc = getProvider().beginTranstacion();
			if (_Logger.isTraceEnabled()) {
				_Logger.trace("exe " + indexsql);
			}
			jdbc.sqlExecuteUpdate(indexsql);
			jdbc.commit();
		} catch (SQLException ee) {
			_Logger.error("忽略执行" + indexsql + "出错", ee);
		} finally {
			if (!jdbc.isCompleted()) {
				jdbc.rollback();
			}
		}

	}

	private Collection<? extends String> getNeedIndexs() {
		return m_NeedIndexs;
	}

	private String getRegisterSql() {
		StringBuilder sb = new StringBuilder();
		sb.append("create table if not exists `").append(getTabelName()).append("` ( ");
		sb.append(" `").append(ID).append("` ").append(getStringType()).append(" ,");
		sb.append(" `").append(VERSION).append("` ").append(getStringType()).append(" ,");
		sb.append(" `").append(SERVERID).append("` ").append(getStringType()).append(" ,");
		sb.append(" `").append(DRIVEIT).append("` ").append(getStringType()).append(" ,");
		sb.append(" `").append(LASTMODIFIED).append("` ").append(getLongType()).append(" ,");
		sb.append("PRIMARY KEY(`").append(ID).append("`) );");
		return sb.toString();
	}

	private static boolean isNoExistTabelException(String tablename, SQLException e) {
		// com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: Table
		// 'bill.ticketbill202003' doesn't exist
		String message = e.getMessage();
		return message.contains("Table") && message.contains("doesn't exist");
	}

	public void setWatcher(EntityWatcher w) {
		m_Watcher = w;
	}

	@Override
	public String getDatabase() {
		return getProvider().getDatabase();
	}

	public String getTabelName() {
		return m_Name.toLowerCase();
	}

	@Override
	public ResultPage<String> startsWithOfId(String prefix) {
		if (StringUtil.isEmpty(prefix)) {
			return toResult(null, null);
		} else {
			return searchRangeOfId(prefix, prefix + StringUtil.UNICODE_REPLACEMENT_STRING);
		}
	}

	@Override
	public ResultPage<String> searchOfId(Date begin, Date end) {
		String range;
		if (null == begin && null == end) {
			range = null;
		} else if (null == begin) {
			range = SqlUtil.wrapField(LASTMODIFIED) + "<=" + end.getTime();
		} else if (null == end) {
			range = SqlUtil.wrapField(LASTMODIFIED) + ">=" + begin.getTime();
		} else {
			range = SqlUtil.wrapField(LASTMODIFIED) + " BETWEEN " + begin.getTime() + " AND "
					+ end.getTime();
		}
		return toResult(range, null);
	}

	@Override
	public ResultPage<String> searchRangeOfId(String from, String to) {
		String range;
		if (StringUtil.isEmpty(from) && StringUtil.isEmpty(to)) {
			range = null;
		} else if (StringUtil.isEmpty(from)) {
			range = SqlUtil.wrapField(ID) + " <= " + SqlUtil.wrapValue(to);
		} else if (StringUtil.isEmpty(to)) {
			range = SqlUtil.wrapField(ID) + " >= " + SqlUtil.wrapValue(from);
		} else {
			range = SqlUtil.wrapField(ID) + " BETWEEN " + SqlUtil.wrapValue(from) + " AND "
					+ SqlUtil.wrapValue(to);
		}
		return toResult(range, null);
	}

	@Override
	public Iterator<String> searchOfId(String serverId, Date begin, Date end) {
		String range;
		if (null == begin && null == end) {
			range = null;
		} else if (null == begin) {
			range = SqlUtil.wrapField(LASTMODIFIED) + "<=" + end.getTime();
		} else if (null == end) {
			range = SqlUtil.wrapField(LASTMODIFIED) + ">=" + begin.getTime();
		} else {
			range = SqlUtil.wrapField(LASTMODIFIED) + " BETWEEN " + begin.getTime() + " AND "
					+ end.getTime();
		}
		String server;
		if (null == serverId) {
			server = null;
		} else {
			server = SqlUtil.wrapField(SERVERID) + "=" + SqlUtil.wrapValue(serverId);
		}
		String whereDesc;
		if (null == range && null == server) {
			whereDesc = null;
		} else if (null == range) {
			whereDesc = server;
		} else if (null == server) {
			whereDesc = range;
		} else {
			whereDesc = range + " AND " + server;
		}
		return ResultPageHelper.toForeach(toResult(whereDesc, null)).iterator();
	}

	@Override
	public Iterator<String> searchRangeOfId(String serverId, String from, String to) {
		String range;
		if (StringUtil.isEmpty(from) && StringUtil.isEmpty(to)) {
			range = null;
		} else if (StringUtil.isEmpty(from)) {
			range = SqlUtil.wrapField(ID) + " <= " + to;
		} else if (StringUtil.isEmpty(to)) {
			range = SqlUtil.wrapField(ID) + " >= " + from;
		} else {
			range = SqlUtil.wrapField(ID) + " BETWEEN " + from + " AND " + to;
		}
		String server;
		if (null == serverId) {
			server = null;
		} else {
			server = SqlUtil.wrapField(SERVERID) + "=" + SqlUtil.wrapValue(serverId);
		}
		String whereDesc;
		if (null == range && null == server) {
			whereDesc = null;
		} else if (null == range) {
			whereDesc = server;
		} else if (null == server) {
			whereDesc = range;
		} else {
			whereDesc = range + " AND " + server;
		}
		return ResultPageHelper.toForeach(toResult(whereDesc, null)).iterator();
	}

	@Override
	public ResultPage<String> searchOfId(Condition condition, OrderBy orderBy) {
		String whereDesc = toWhere(condition);
		String orderByDesc = toOrderBy(orderBy);
		return toResult(whereDesc, orderByDesc);
	}

	private String toWhere(Condition c) {
		if (null == c) {
			return null;
		}
		int type = c.getType();
		if (type == Condition.TYPE_AND) {
			List<Condition> items = c.getItems();
			int i = 0;
			StringBuilder sb = new StringBuilder();
			for (; i < items.size(); i++) {
				String where = toWhere(items.get(i));
				if (null == where) {
					continue;
				}
				sb.append("(");
				sb.append(where);
				sb.append(")");
				break;
			}
			for (i = i + 1; i < items.size(); i++) {
				String where = toWhere(items.get(i));
				if (null == where) {
					continue;
				}
				sb.append(" AND ");
				sb.append("(");
				sb.append(where);
				sb.append(")");
			}
			return sb.toString();
		}
		if (type == Condition.TYPE_OR) {
			List<Condition> items = c.getItems();
			int i = 0;
			StringBuilder sb = new StringBuilder();
			for (; i < items.size(); i++) {
				String where = toWhere(items.get(i));
				if (null == where) {
					continue;
				}
				sb.append("(");
				sb.append(where);
				sb.append(")");
				break;
			}
			for (i = i + 1; i < items.size(); i++) {
				String where = toWhere(items.get(i));
				if (null == where) {
					continue;
				}
				sb.append(" OR ");
				sb.append("(");
				sb.append(where);
				sb.append(")");
			}
			return sb.toString();
		}

		String name = c.getName();
		boolean exists;
		int index = name.indexOf(Condition.FIELD_SPEARATOR);
		if (index > 0) {
			String col = name.substring(0, index);
			String child = name.substring(index + 1);
			exists = getColumns().containsKey(col);
			name = "JSON_EXTRACT(`" + col + "`,'$." + child + "')";
		} else {
			exists = getColumns().containsKey(name);
			name = SqlUtil.wrapField(name);
		}
		String v = toTItem(c.getValue());
		if (type == Condition.TYPE_EQ) {
			if (!exists) {
				if (null == v) {
					return "1=1";// 如果没有该列，则表现全部符合
				} else {
					return "1<>1";// 如果没有该列，则表现全部不符合
				}
			}
			if (null == v) {
				return name + " IS NULL";
			} else {
				return name + "=" + v;
			}
		}
		if (type == Condition.TYPE_NE) {
			if (!exists) {
				if (null != v) {
					return "1=1";
				} else {
					return "1<>1";
				}
			}
			if (null == v) {
				return name + " IS NOT NULL";
			} else {
				return name + "<>" + v;
			}
		}
		if (!exists) {
			return "1<>1";
		}
		if (type == Condition.TYPE_LT) {
			return name + "<" + v;
		}
		if (type == Condition.TYPE_GT) {
			return name + ">" + v;
		}
		if (type == Condition.TYPE_LTE) {
			return name + "<=" + v;
		}
		if (type == Condition.TYPE_GTE) {
			return name + ">=" + v;
		}
		throw new UnsupportedOperationException("不支持的类型[" + type + "]");

	}

	private String toTItem(Object value) {
		if (null == value) {
			return null;
		}
		if (value instanceof Number) {
			return value.toString();
		} else if (value instanceof Date) {
			return SqlUtil.wrapValue(DtDate.Formater.formatDateTime((Date) value));
		}
		return SqlUtil.wrapValue(value.toString());// 暂时先不用处理类型转换
	}

	private String toOrderBy(OrderBy orderBy) {
		if (null == orderBy) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String asc : orderBy.getAsc()) {
			if (StringUtil.isEmpty(asc) || !getColumns().containsKey(asc)) {
				continue;
			}
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}
			sb.append(SqlUtil.wrapField(asc));
			sb.append(" ASC");
		}
		for (String desc : orderBy.getDesc()) {
			if (StringUtil.isEmpty(desc) || !getColumns().containsKey(desc)) {
				continue;
			}
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}
			sb.append(SqlUtil.wrapField(desc));
			sb.append(" DESC");
		}
		if (first) {
			return null;
		}
		return sb.toString();
	}

	private ResultPage<String> toResult(String whereDesc, String orderByDesc) {
		return new MysqlResultPage<String>(getProvider(), getTabelName(), SqlUtil.wrapField(ID),
				whereDesc, orderByDesc) {

			@Override
			protected String to(ResultSet rs) throws SQLException {
				return rs.getString(ID);
			}
		};
	}

	@Override
	protected ObjectWithVersion<E> innerLoad(String id) {
		Map<String, SqlColumnType> columns = getColumns();
		String tablename = getTabelName();
		String sql = "SELECT * FROM " + tablename + " WHERE `" + ID + "`='" + SqlString.escape(id)
				+ "'";
		TemplateJdbc jdbc = null;
		ResultSet rs = null;
		try {
			jdbc = getProvider().beginTranstacion();
			rs = jdbc.sqlExecuteQuery(sql);
			E e = null;
			String version = null;
			String driveit = null;
			while (rs.next()) {
				e = wrap(columns, rs);
				version = rs.getString(VERSION);
				driveit = rs.getString(DRIVEIT);
				break;
			}
			jdbc.commit();
			if (null == e) {
				return null;
			}
			return new ObjectWithVersion<E>(e, version, driveit);
		} catch (SQLException e) {
			if (isNoExistTabelException(tablename, e)) {
				return null;
			}
			throw new DataAccessException("获取数据异常", e);
		} finally {
			if (null != rs) {
				try {
					rs.close();
				} catch (SQLException e) {
					_Logger.warn("忽略关闭异常", e);
				}
			}
			if (null != jdbc && !jdbc.isCompleted()) {
				jdbc.rollback();
			}
		}

	} /* 包装 */

	private E wrap(Map<String, SqlColumnType> columns, ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columnCount = md.getColumnCount();
		SimpleDtObject dt = new SimpleDtObject();
		for (int i = 1; i <= columnCount; i++) {
			String name = md.getColumnName(i);
			dt.put(name, toDtBase(columns, name, rs.getObject(i)));
		}
		return m_Mapper.fromDtObject(dt);
	}

	private DtBase toDtBase(Map<String, SqlColumnType> columns, String name, Object object) {
		SqlColumnType col = columns.get(name);
		DtBase base = toDtBase(object);
		if (null != col) {
			if (StringUtil.eq(col.getName(), "JSON")) {
				if (base instanceof DtString) {
					String json = ((DtString) base).value();
					if (json.charAt(0) == '{') {
						return new JsonDtObject(json);
					} else {
						return new JsonDtList(json);
					}
				}
			}
		}
		return base;
	}

	private DtBase toDtBase(Object object) {
		if (null == object) {
			return null;
		}
		if (object instanceof Double) {
			return new SimpleDtNumber((double) object);
		} else if (object instanceof Long) {
			return new SimpleDtNumber((long) object);
		} else if (object instanceof Integer) {
			return new SimpleDtNumber((int) object);
		} else if (object instanceof String) {
			return new SimpleDtString((String) object);
		} else if (object instanceof Boolean) {
			return new SimpleDtBoolean((Boolean) object);
		} else if (object instanceof Timestamp) {
			return SimpleDtDate.valueOf(new Date(((Timestamp) object).getTime()));
		} else {
			throw new DataAccessException("不支持的类型:" + object.getClass());
		}
	}

	protected String innerSave(E object) {
		return innerSave(object, null);
	}

	protected String innerSave(E object, String oldVersion) {
		String id = object.getPersistenceId().getOrdinal();
		String version;
		DtObject dt = m_Mapper.toDtObject(object);
		Enumeration<KvPair<String, DtBase>> attr = dt.getAttributes();
		Map<String, SqlColumnType> miss = new HashMap<>();
		Map<String, SqlColumnType> change = new HashMap<>();
		Map<String, String> content = new HashMap<>();
		ConcurrentMap<String, SqlColumnType> columns = getColumns();
		while (attr.hasMoreElements()) {
			KvPair<String, DtBase> pair = attr.nextElement();
			DtBase value = pair.getValue();
			String name = pair.getKey();
			SqlColumnType type = columns.get(name);
			if (null == type) {
				if (null == value) {
					continue;
				}
				type = changeType(value);
				miss.putIfAbsent(name, type);
			} else {
				SqlColumnType currentType = changeType(value);
				if (!StringUtil.eq(currentType.getName(), type.getName())
						&& currentType.getLength() > type.getLength()) {
					change.put(name, currentType);
				}
			}
			String myValue = toValue(value);
			content.put(name, myValue);
		}
		version = genVersion(oldVersion);
		content.put(VERSION, "'" + version + "'");
		if (null == columns.get(VERSION)) {
			miss.put(VERSION, getStringType());
		}
		content.put(SERVERID, "'" + getPersisterId() + "'");
		if (null == columns.get(SERVERID)) {
			miss.put(SERVERID, getStringType());
		}
		if (object instanceof cn.weforward.common.DistributedObject) {
			content.put(DRIVEIT,
					"'" + ((cn.weforward.common.DistributedObject) object).getDriveIt() + "'");
			if (null == columns.get(DRIVEIT)) {
				miss.put(DRIVEIT, getStringType());
			}
		}
		content.put(LASTMODIFIED, String.valueOf(System.currentTimeMillis()));
		if (null == columns.get(LASTMODIFIED)) {
			miss.put(LASTMODIFIED, getLongType());
		}
		if (!miss.isEmpty()) {
			addColumns(miss);
		}
		if (!change.isEmpty()) {
			changeColumns(change);
		}
		String update = toUpdate(getTabelName(), id, content);
		TemplateJdbc jdbc = null;
		try {
			jdbc = getProvider().beginTranstacion();
			int num = jdbc.sqlExecuteUpdate(update);
			if (num == 0) {
				String insert = toInsert(getTabelName(), id, content);
				jdbc.sqlExecuteUpdate(insert);
			}
			jdbc.commit();
		} catch (SQLException e) {
			throw new DataAccessException("更新数据异常", e);
		} finally {
			if (null != jdbc && !jdbc.isCompleted()) {
				jdbc.rollback();
			}
		}
		return version;
	}

	private void changeColumns(Map<String, SqlColumnType> modify) {
		TemplateJdbc jdbc = null;
		try {
			jdbc = getProvider().beginTranstacion();
			for (Map.Entry<String, SqlColumnType> entry : modify.entrySet()) {
				String key = entry.getKey();
				SqlColumnType value = entry.getValue();
				String sql = "ALTER TABLE " + getTabelName() + " CHANGE " + key + " " + key + " "
						+ value.toString();
				if (_Logger.isTraceEnabled()) {
					_Logger.trace("exe " + sql);
				}
				jdbc.sqlExecuteUpdate(sql);
				m_Columns.put(key, value);
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

	private static String toUpdate(String tabelName, String id, Map<String, String> content) {
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE ").append(tabelName).append(" SET ");
		Iterator<Map.Entry<String, String>> entry = content.entrySet().iterator();
		while (entry.hasNext()) {
			Map.Entry<String, String> e = entry.next();
			sb.append('`').append(e.getKey()).append("`=");
			sb.append(e.getValue());
			break;
		}
		while (entry.hasNext()) {
			Map.Entry<String, String> e = entry.next();
			sb.append(',');
			sb.append('`').append(e.getKey()).append("`=");
			sb.append(e.getValue());
		}
		sb.append(" WHERE `").append(ID).append("`='").append(id).append('\'');
		return sb.toString();
	}

	private static String toInsert(String tabelName, String id, Map<String, String> content) {
		StringBuilder into = new StringBuilder();
		StringBuilder value = new StringBuilder();
		into.append("INSERT INTO ").append(tabelName).append("(");
		into.append(ID);
		value.append("VALUES('").append(id).append("'");
		Iterator<Map.Entry<String, String>> entry = content.entrySet().iterator();
		while (entry.hasNext()) {
			Map.Entry<String, String> e = entry.next();
			if (e.getValue() == null) {
				continue;
			}
			into.append(',');
			into.append(e.getKey());
			value.append(",");
			value.append(e.getValue());
		}
		into.append(')');
		value.append(')');
		return into.toString() + " " + value.toString();
	}

	private String genVersion(String version) {
		return VersionTags.next(getPersisterId(), version, false);
	}

	private String toValue(DtBase value) {
		if (null == value) {
			return null;
		}
		if (value instanceof DtObject) {
			DtObject object = (DtObject) value;
			StringBuilder sb = new StringBuilder();
			sb.append("'");
			try {
				MysqlUtil.formatObject(object, sb);
			} catch (IOException e) {
				throw new DataAccessException("转换数据异常", e);
			}
			sb.append("'");
			return sb.toString();
		} else if (value instanceof DtList) {
			DtList object = (DtList) value;
			StringBuilder sb = new StringBuilder();
			sb.append("'");
			try {
				MysqlUtil.formatList(object, sb);
			} catch (IOException e) {
				throw new DataAccessException("转换数据异常", e);
			}
			sb.append("'");
			return sb.toString();
		} else if (value instanceof DtDate) {
			return "'" + ((DtDate) value).value() + "'";
		} else if (value instanceof DtString) {
			return "'" + SqlString.escape(((DtString) value).value()) + "'";
		} else if (value instanceof DtNumber) {
			DtNumber n = (DtNumber) value;
			if (n.isDouble()) {
				return String.valueOf(n.valueDouble());
			} else if (n.isLong()) {
				return String.valueOf(n.valueLong());
			} else if (n.isInt()) {
				return String.valueOf(n.valueInt());
			} else {
				return String.valueOf(n.valueDouble());
			}
		} else if (value instanceof DtBoolean) {
			return String.valueOf(((DtBoolean) value).value());
		} else {
			throw new UnsupportedOperationException("不支持的数据类型:" + value.getClass());
		}
	}

	private void addColumns(Map<String, SqlColumnType> miss) {
		List<String> indexs = new ArrayList<>();
		TemplateJdbc jdbc = null;
		try {
			jdbc = getProvider().beginTranstacion();
			for (Map.Entry<String, SqlColumnType> entry : miss.entrySet()) {
				String key = entry.getKey();
				SqlColumnType value = entry.getValue();
				String sql = "ALTER TABLE " + getTabelName() + " ADD " + key + " "
						+ value.toString();
				if (_Logger.isTraceEnabled()) {
					_Logger.trace("exe " + sql);
				}
				jdbc.sqlExecuteUpdate(sql);
				m_Columns.putIfAbsent(key, value);
				if (getNeedIndexs().contains(key)) {
					indexs.add(key);
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
		for (String index : indexs) {
			createIndex(index);
		}
	}

	private SqlColumnType changeType(DtBase dttype) {
		if (dttype instanceof DtObject) {
			return getJsonType();
		} else if (dttype instanceof DtList) {
			return getJsonType();
		} else if (dttype instanceof DtDate) {
			return new SqlColumnType("CHAR", 24);
		} else if (dttype instanceof DtString) {
			SqlColumnType type = getStringType();
			int l = ((DtString) dttype).value().length();
			if (l > type.getLength()) {
				int s = l / m_DefaultStringLength;
				type.setLength(m_DefaultStringLength * (s + 1));
			}
			return type;
		} else if (dttype instanceof DtNumber) {
			DtNumber n = (DtNumber) dttype;
			if (n.isDouble()) {
				return new SqlColumnType("DOUBLE", 0);
			} else if (n.isLong()) {
				return getLongType();
			} else if (n.isInt()) {
				return new SqlColumnType("INT", 0);
			} else {
				return new SqlColumnType("DECIMAL", 0);
			}
		} else if (dttype instanceof DtBoolean) {
			return new SqlColumnType("BOOL", 0);
		} else {
			throw new UnsupportedOperationException("不支持的数据类型:" + dttype.getClass());
		}
	}

	private SqlColumnType getJsonType() {
		return new SqlColumnType("JSON", 0);
	}

	private SqlColumnType getLongType() {
		return new SqlColumnType("BIGINT", 0);
	}

	private SqlColumnType getStringType() {
		return new SqlColumnType("VARCHAR", m_DefaultStringLength);
	}

	@Override
	protected String innerNew(E object) {
		return innerSave(object);
	}

	@Override
	protected boolean innerDelete(String id) {
		int num = 0;
		TemplateJdbc jdbc = null;
		try {
			jdbc = getProvider().beginTranstacion();
			String sql = "DELETE FROM " + getTabelName() + " WHERE " + SqlUtil.wrapField(ID) + "="
					+ SqlUtil.wrapValue(id);
			if (_Logger.isTraceEnabled()) {
				_Logger.trace("exe " + sql);
			}
			num = jdbc.sqlExecuteUpdate(sql);
			jdbc.commit();
		} catch (SQLException e) {
			throw new DataAccessException("删除列异常", e);
		} finally {
			if (null != jdbc && !jdbc.isCompleted()) {
				jdbc.rollback();
			}
		}
		return num > 0;
	}

	public void clear() {
		m_Cache.removeAll();
	}

	class InitFlushable implements Flushable {

		@Override
		public void flush() throws IOException {
			getColumns();
		}

	}

	@Override
	public boolean setReloadEnabled(boolean enabled) {
		super.setReloadEnabled(enabled);
		if (enabled) {
			startWacherIfNeed();
		} else if (null != m_Watcher) {
			stopWacherIfNeed();
		}
		return true;
	}

	private synchronized void startWacherIfNeed() {
		if (null == m_Watcher) {
			throw new UnsupportedOperationException("请先设置Watcher");
		}
		m_Watcher.register(this);
	}

	private synchronized void stopWacherIfNeed() {
		if (null == m_Watcher) {
			return;
		}
		if (!ListUtil.isEmpty(m_Listeners) || isReloadEnabled()) {
			return;
		}
		m_Watcher.unRegister(this);

	}

	@Override
	public synchronized void addListener(ChangeListener<E> l) {
		super.addListener(l);
		startWacherIfNeed();
	}

	@Override
	public synchronized void removeListener(ChangeListener<E> l) {
		super.removeListener(l);
		stopWacherIfNeed();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onChange(ChangeEntity entity) {
		int op = entity.getType();
		if (op == EntityListener.INSERT || op == EntityListener.UPDATE
				|| op == EntityListener.DELETE) {
			if (StringUtil.eq(getPersisterId(), entity.getString(SERVERID))) {
				return;// 自己改的..
			}
			String id = entity.getString(ID);
			E data = null;
			try {
				E e;
				synchronized (m_Cache) {
					e = m_Cache.get(id);
				}
				if (e instanceof Reloadable) {
					data = getVo(entity);
					if (data instanceof PersistentListener) {
						// 调用持久对象反射后事件
						PersistentListener listener = (PersistentListener) data;
						listener.onAfterReflect((Persister<? extends Persistent>) this,
								UniteId.valueOf(id, data.getClass()), entity.getString(VERSION),
								entity.getString(DRIVEIT));
					}
					Reloadable<E> able = (Reloadable<E>) e;
					able.onReloadAccepted(this, data);
				}
			} catch (Throwable e) {
				_Logger.warn("忽略onReloadAccepted通知异常," + id, e);
			}
			List<ChangeListener<E>> list = m_Listeners;
			Supplier<E> supplierdata;
			if (null == data) {
				supplierdata = () -> getVo(entity);
			} else {
				supplierdata = Optional.of(data)::get;
			}
			NameItem type;
			switch (op) {
			case INSERT:
				type = ChangeListener.TYPE_NEW;
				break;
			case UPDATE:
				type = ChangeListener.TYPE_UPDATE;
				break;
			case DELETE:
				type = ChangeListener.TYPE_DELETE;
				break;
			default:
				type = ChangeListener.TYPE_UNKNOW;
				break;
			}
			for (ChangeListener<E> l : list) {
				try {
					l.onChange(type, id, supplierdata);
				} catch (Throwable e) {
					_Logger.warn("忽略Listener通知异常," + id, e);
				}
			}

		}

	}

	private E getVo(ChangeEntity entity) {
		return getVo(innerLoad(entity.getString(ID)));// 先简单的重新查一次
	}

	private E getVo(ObjectWithVersion<E> ow) {
		return null == ow ? null : ow.getObject();
	}

}

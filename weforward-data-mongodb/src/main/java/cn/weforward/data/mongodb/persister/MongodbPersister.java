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
package cn.weforward.data.mongodb.persister;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import cn.weforward.common.NameItem;
import cn.weforward.common.ResultPage;
import cn.weforward.common.util.ListUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.TransList;
import cn.weforward.data.UniteId;
import cn.weforward.data.mongodb.util.MongodbIterator;
import cn.weforward.data.mongodb.util.MongodbResultPage;
import cn.weforward.data.mongodb.util.MongodbUtil;
import cn.weforward.data.mongodb.util.MongodbWatcher;
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
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 基于mongodb的持久类
 * 
 * @author daibo
 *
 */
public class MongodbPersister<E extends Persistent> extends AbstractPersister<E>
		implements MongodbWatcher.DocumentChange {
	/** 日志 */
	protected final static Logger _Logger = LoggerFactory.getLogger(MongodbPersister.class);
	/** 映射表 */
	final ObjectMapper<E> m_Mapper;
	/** 链接 */
	private MongoCollection<Document> m_Collection;
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
	/** 数据库 */
	protected MongodbPersisterFactory m_Factory;
	/** 投影条件，只要id */
	private static final BsonDocument PROJECTION_ID_ONLY = new BsonDocument();
	static {
		PROJECTION_ID_ONLY.put(ID, new BsonInt32(1));
	}
	/** 替换条件 */
	private static final ReplaceOptions REPLACE_OPTIONS = new ReplaceOptions().upsert(true);
	/** 升序 */
	public static final short ORDERBY_ASC = 1;
	/** 降序 */
	public static final short ORDERBY_DESC = -1;

	public MongodbPersister(MongodbPersisterFactory factory, ObjectMapper<E> mapper) {
		this(factory, mapper, mapper.getName());
	}

	public MongodbPersister(MongodbPersisterFactory factory, ObjectMapper<E> mapper, String name) {
		super(name);
		m_Mapper = mapper;
		m_Factory = factory;
	}

	public void setFlusher(Flusher flusher) {
		super.setFlusher(flusher);
		getFlusher().flush(new InitFlushable());
	}

	@Override
	public boolean setReloadEnabled(boolean enabled) {
		super.setReloadEnabled(enabled);
		if (enabled) {
			startWacherIfNeed();
		} else {
			stopWacherIfNeed();
		}
		return true;
	}

	private void startWacherIfNeed() {
		m_Factory.wacher(this);
	}

	private void stopWacherIfNeed() {
		m_Factory.unWacher(this);
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

	@Override
	public ResultPage<String> startsWithOfId(String prefix) {
		if (StringUtil.isEmpty(prefix)) {
			return toResult(null);
		} else {
			return searchRangeOfId(prefix, prefix + StringUtil.UNICODE_REPLACEMENT_STRING);
		}
	}

	@Override
	public ResultPage<String> searchOfId(Date begin, Date end) {
		long from = null == begin ? Long.MIN_VALUE : begin.getTime();
		long to = null == end ? Long.MAX_VALUE : end.getTime();
		Bson range = Filters.and(Filters.gt(LASTMODIFIED, from), Filters.lte(LASTMODIFIED, to));
		return toResult(range);
	}

	@Override
	public ResultPage<String> searchRangeOfId(String from, String to) {
		from = null == from ? "" : from;
		to = null == to ? "" : to;
		Bson range = Filters.and(Filters.gte(ID, from), Filters.lte(ID, to));
		return toResult(range);
	}

	@Override
	public Iterator<String> searchOfId(String serverId, Date begin, Date end) {
		Bson eq = Filters.eq(SERVERID, serverId);
		long from = null == begin ? Long.MIN_VALUE : begin.getTime();
		long to = null == end ? Long.MAX_VALUE : end.getTime();
		Bson range = Filters.and(Filters.gt(LASTMODIFIED, from), Filters.lte(LASTMODIFIED, to));
		Bson filter = Filters.and(eq, range);
		return toIt(filter);
	}

	@Override
	public Iterator<String> searchRangeOfId(String serverId, String from, String to) {
		Bson eq = Filters.eq(SERVERID, serverId);
		from = null == from ? "" : from;
		to = null == to ? "" : to;
		Bson range = Filters.and(Filters.gte(ID, from), Filters.lte(ID, to));
		Bson filter = Filters.and(eq, range);
		return toIt(filter);
	}

	@Override
	public ResultPage<String> searchOfId(Condition condition, OrderBy orderBy) {
		Bson filter = toBson(condition);
		Bson sort = toBson(orderBy);
		return toResult(filter, sort);
	}

	private Bson toBson(Condition c) {
		if (null == c) {
			return null;
		}
		int type = c.getType();
		switch (type) {
		case Condition.TYPE_AND:
			return Filters.and(TransList.valueOf(c.getItems(), (item) -> toBson(item)));
		case Condition.TYPE_OR:
			return Filters.or(TransList.valueOf(c.getItems(), (item) -> toBson(item)));
		case Condition.TYPE_EQ:
			return Filters.eq(c.getName(), toTItem(c.getValue()));
		case Condition.TYPE_NE:
			return Filters.ne(c.getName(), toTItem(c.getValue()));
		case Condition.TYPE_LT:
			return Filters.lt(c.getName(), toTItem(c.getValue()));
		case Condition.TYPE_GT:
			return Filters.gt(c.getName(), toTItem(c.getValue()));
		case Condition.TYPE_LTE:
			return Filters.lte(c.getName(), toTItem(c.getValue()));
		case Condition.TYPE_GTE:
			return Filters.gte(c.getName(), toTItem(c.getValue()));
		default:
			throw new UnsupportedOperationException("不支持的类型[" + type + "]");
		}

	}

	private Object toTItem(Object value) {
		return value;// 暂时先不用处理类型转换
	}

	private Bson toBson(OrderBy value) {
		if (null == value) {
			return null;
		}
		List<Bson> list = new ArrayList<>();
		List<String> asc = value.getAsc();
		List<String> desc = value.getDesc();
		if (!ListUtil.isEmpty(asc)) {
			for (String name : asc) {
				list.add(Filters.eq(name, ORDERBY_ASC));
			}
		}
		if (!ListUtil.isEmpty(desc)) {
			for (String name : desc) {
				list.add(Filters.eq(name, ORDERBY_DESC));
			}
		}
		if (list.isEmpty()) {
			return null;
		} else if (list.size() == 1) {
			return list.get(0);
		} else {
			return Filters.and(list);
		}
	}

	@Override
	protected ObjectWithVersion<E> innerLoad(String id) {
		FindIterable<Document> it = getCollection().find(Filters.eq(ID, getId(id)));
		Document doc = it.first();
		if (null == doc) {
			return null;
		}
		E e = wrap(doc);
		return new ObjectWithVersion<E>(e, doc.getString(VERSION), doc.getString(DRIVEIT));
	}

	protected synchronized String innerSave(E object) {
		return innerSave(object, null);
	}

	protected synchronized String innerSave(E object, String oldVersion) {
		// String version = oldVersion;
		// if (object instanceof AbstractPersistent<?>) {
		// version = ((AbstractPersistent<?>) object).getPersistenceVersion();
		// }
		MongoCollection<Document> c = getCollection();
		// Document doc = toDoc(object, version);
		Document doc = toDoc(object, oldVersion);
		Bson filter = Filters.eq(ID, getId(object.getPersistenceId()));
		UpdateResult result = c.replaceOne(filter, doc, REPLACE_OPTIONS);
		if (_Logger.isDebugEnabled()) {
			_Logger.debug("matchedCount:" + result.getMatchedCount());
			_Logger.debug("modifiedCount:" + result.getModifiedCount());
			_Logger.debug("upsertedId:" + result.getUpsertedId());
		}
		// if (result.getMatchedCount() == 0) {
		// c.insertOne(doc);
		// }
		return doc.getString(VERSION);
	}

	@Override
	protected String innerNew(E object) {
		return innerSave(object);
		// MongoCollection<Document> c = getCollection();
		// Document doc = new Document();
		// doc.append(ID, getId(object.getPersistenceId()));
		// try {
		// c.insertOne(doc);
		// } catch (Throwable e) {
		// }
		// return null;
	}

	@Override
	protected boolean innerDelete(String id) {
		Bson filter = Filters.eq(ID, getId(id));
		DeleteResult result = getCollection().deleteOne(filter);
		return result.getDeletedCount() > 0;
	}

	/* 获取链接 */
	public MongoCollection<Document> getCollection() {
		if (null == m_Collection) {
			synchronized (this) {
				if (null == m_Collection) {
					m_Collection = initCollection();
				}
			}
		}
		return m_Collection;
	}

	private MongoCollection<Document> initCollection() {
		MongoCollection<Document> c = m_Factory.getCollection(getName().toLowerCase());
		MongoCursor<Document> it = c.listIndexes().iterator();
		ArrayList<String> indexs = new ArrayList<>();
		indexs.add(LASTMODIFIED);
		if (m_Mapper instanceof AutoObjectMapper) {
			Enumeration<String> names = ((AutoObjectMapper<?>) m_Mapper).getIndexAttributeNames();
			while (names.hasMoreElements()) {
				indexs.add(names.nextElement());
			}
		}
		while (it.hasNext()) {
			Document doc = it.next();
			String name = doc.getString("name");
			for (int i = 0; i < indexs.size(); i++) {
				String index = indexs.get(i);
				if (null == index) {
					continue;
				}
				if (name.startsWith(index)) {
					indexs.set(i, null);// 已有索引
					break;
				}
			}
		}
		for (String index : indexs) {
			if (null == index) {
				continue;
			}
			IndexOptions options = new IndexOptions();
			options.name(index);
			c.createIndex(Filters.eq(index, 1), options);
		}
		return c;
	}

	/* 包装 */
	private E wrap(Document doc) {
		SimpleDtObject dt = new SimpleDtObject();
		dt.put(ID, String.valueOf(doc.get(ID)));
		dt.put(VERSION, doc.getString(VERSION));
		dt.put(LASTMODIFIED, doc.getLong(LASTMODIFIED));
		dt.put(SERVERID, StringUtil.toString(doc.get(SERVERID)));
		dt = MongodbUtil.docToDt(dt, doc);
		return m_Mapper.fromDtObject(dt);
	}

	/* 转换文档 */
	private Document toDoc(E object, String version) {
		DtObject dt = m_Mapper.toDtObject(object);
		Document doc = new Document();
		doc.append(ID, getId(object.getPersistenceId()));
		doc.append(VERSION, genVersion(version));
		doc.append(LASTMODIFIED, System.currentTimeMillis());
		doc.append(SERVERID, getPersisterId());
		if (object instanceof cn.weforward.common.DistributedObject) {
			doc.append(DRIVEIT, ((cn.weforward.common.DistributedObject) object).getDriveIt());
		}
		doc = MongodbUtil.dtToDoc(doc, dt);
		return doc;
	}

	/* 生成版本 */
	private String genVersion(String version) {
		return VersionTags.next(getPersisterId(), version, false);
	}

	/* 获取id */
	private Object getId(UniteId id) {
		return id.getOrdinal();
	}

	/* 获取id */
	private String getId(String id) {
		return UniteId.getOrdinal(id);
	}

	/* 转换结果id */
	private ResultPage<String> toResult(Bson filter) {
		return toResult(filter, null);
	}

	/* 转换结果id */
	private ResultPage<String> toResult(Bson filter, Bson sort) {
		MongodbResultPage<String> rp = new MongodbResultPage<String>(getCollection(), filter,
				sort) {

			@Override
			protected String to(Document doc) {
				return null == doc ? null : doc.getString(ID);
			}

		};
		rp.setProjection(PROJECTION_ID_ONLY);
		return rp;
	}

	/* 遍历器 */
	private Iterator<String> toIt(Bson filter) {
		MongodbIterator<String> it = new MongodbIterator<String>(getCollection(), filter) {

			@Override
			protected String to(Document doc) {
				return doc.getString(MongodbPersister.ID);
			}

		};
		it.setProjection(PROJECTION_ID_ONLY);
		return it;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onChange(ChangeStreamDocument<Document> doc) {
		OperationType op = doc.getOperationType();
		if (op == OperationType.INSERT || op == OperationType.UPDATE || op == OperationType.REPLACE
				|| op == OperationType.DELETE) {
			Document full = doc.getFullDocument();
			if (null == full) {
				return;
			}
			if (StringUtil.eq(getPersisterId(), getString(full, SERVERID))) {
				return;// 自己改的..
			}
			String id = full.getString(ID);
			E data = null;
			try {
				E e;
				synchronized (m_Cache) {
					e = m_Cache.get(id);
				}
				if (e instanceof Reloadable) {
					data = wrap(doc.getFullDocument());
					if (data instanceof PersistentListener) {
						// 调用持久对象反射后事件
						PersistentListener listener = (PersistentListener) data;
						listener.onAfterReflect((Persister<? extends Persistent>) this,
								UniteId.valueOf(id, data.getClass()), full.getString(VERSION),
								full.getString(DRIVEIT));
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
				supplierdata = () -> wrap(doc.getFullDocument());
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
			case REPLACE:
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

	private String getString(Document full, String key) {
		Object v = full.get(key);
		return null == v ? null : String.valueOf(v);
	}

	class InitFlushable implements Flushable {

		@Override
		public void flush() throws IOException {
			getCollection();
		}

	}

}

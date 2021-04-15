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
package cn.weforward.data.mongodb.array;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.DeleteResult;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.UniteId;
import cn.weforward.data.array.Label;
import cn.weforward.data.array.LabelElement;
import cn.weforward.data.mongodb.util.MongodbResultPage;
import cn.weforward.data.mongodb.util.MongodbUtil;
import cn.weforward.data.util.VersionTags;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 基于mongodb的label
 * 
 * @author daibo
 *
 */
public class MongodbLabel<E extends LabelElement> implements Label<E> {
	/** 日志 */
	protected static Logger _Logger = LoggerFactory.getLogger(MongodbLabel.class);
	/** 持久器名 */
	final String m_Name;
	/** 映射表 */
	final ObjectMapper<E> m_Mapper;
	/** 链接 */
	final MongoCollection<Document> m_Collection;
	/** id属性 */
	final static String ID = "_id";
	/** 最后修改时间 */
	final static String LASTMODIFIED = "_lastmodified";
	/** 版本 */
	final static String VERSION = "_version";
	/** 服务器id */
	final static String SERVERID = "_serverid";
	/** 服务器id */
	protected String m_Serverid;
	/** hash集合的大小 */
	final int m_HashSize;

	public MongodbLabel(MongoDatabase db, ObjectMapper<E> mapper, String name, String serverid, int hashsize) {
		m_Mapper = mapper;
		m_HashSize = hashsize;
		m_Name = name.toLowerCase();
		MongoCollection<Document> c;
		if (m_HashSize > 0) {
			c = db.getCollection(String.valueOf(Math.abs((m_Name.hashCode()) % m_HashSize)));
		} else {
			c = db.getCollection(m_Name);
		}
		MongoCursor<Document> it = c.listIndexes().iterator();
		boolean hasIndex = false;
		while (it.hasNext()) {
			Document doc = it.next();
			if (StringUtil.eq(doc.getString("name"), LASTMODIFIED)) {
				hasIndex = true;
			}
		}
		if (!hasIndex) {
			IndexOptions options = new IndexOptions();
			options.name(LASTMODIFIED);
			c.createIndex(Filters.eq(LASTMODIFIED, 1), options);
		}
		m_Collection = c;
		m_Serverid = serverid;
	}

	private Bson getLimit() {
		if (m_HashSize > 0) {
			return toBson(m_Name, m_Name + StringUtil.UNICODE_REPLACEMENT_CHAR);
		} else {
			return new BsonDocument();
		}
	}

	private Bson getLimit(String begin, String end) {
		if (m_HashSize > 0) {
			return toBson(m_Name + UniteId.OBJECT_SPEARATOR + StringUtil.toString(begin),
					m_Name + UniteId.OBJECT_SPEARATOR + StringUtil.toString(end) + StringUtil.UNICODE_REPLACEMENT_CHAR);
		} else {
			return toBson(begin, end);
		}
	}

	private String genId(LabelElement ele) {
		return genId(ele.getIdForLabel());
	}

	private String genId(String id) {
		if (m_HashSize > 0) {
			return m_Name + UniteId.OBJECT_SPEARATOR + id;
		} else {
			return id;
		}
	}

	@Override
	public String getName() {
		return m_Name;
	}

	@Override
	public ResultPage<E> resultPage() {
		ResultPage<E> rp = new MongodbResultPage<E>(getCollection(), getLimit()) {

			@Override
			protected E to(Document doc) {
				return wrap(doc);
			}
		};
		return rp;
	}

	@Override
	public void add(E element) {
		put(element, OPTION_NONE);
	}

	@Override
	public synchronized E put(E element, int options) {
		String id = genId(element);
		MongoCollection<Document> c = getCollection();
		FindIterable<Document> it = getCollection().find(Filters.eq(ID, id));
		Document old = it.first();
		String v = null == old ? null : old.getString(VERSION);
		Document doc = toDoc(element, genVersion(v), m_Serverid, System.currentTimeMillis());
		if (isOptions(OPTION_IF_ABSENT, options) && null == old) {
			try {
				c.insertOne(doc);
			} catch (MongoException e) {
				if (MongodbUtil.isDuplicateKeyError(e)) {
					return wrap(old);
				} else {
					throw e;
				}
			}
		}
		if (null == old) {
			try {
				c.insertOne(doc);
			} catch (MongoException e) {
				if (MongodbUtil.isDuplicateKeyError(e)) {
					c.replaceOne(Filters.eq(ID, id), doc);
				} else {
					throw e;
				}
			}

		} else {
			c.replaceOne(Filters.eq(ID, id), doc);
		}
		return wrap(old);
	}

	@Override
	public E get(String id) {
		FindIterable<Document> it = getCollection().find(Filters.eq(ID, genId(id)));
		return wrap(it.first());
	}

	@Override
	public ResultPage<E> searchRange(String first, String last) {
		return new MongodbResultPage<E>(getCollection(), getLimit(first, last)) {

			@Override
			protected E to(Document doc) {
				return wrap(doc);
			}
		};
	}

	@Override
	public ResultPage<E> startsWith(String prefix) {
		return searchRange(prefix, prefix + StringUtil.UNICODE_REPLACEMENT_CHAR);
	}

	@Override
	public E remove(String id) {
		Document doc = getCollection().findOneAndDelete(Filters.eq(ID, genId(id)));
		return wrap(doc);
	}

	@Override
	public void removeAll() {
		if (m_HashSize > 0) {
			removeRange(null, null);
		} else {
			getCollection().drop();
		}
	}

	@Override
	public long removeRange(String first, String last) {
		DeleteResult r = getCollection().deleteMany(getLimit(first, last));
		return r.getDeletedCount();
	}

	/* 转换条件 */
	static Bson toBson(String begin, String to) {
		if (null == begin && null == to) {
			return new BsonDocument();
		}
		begin = null == begin ? "" : begin;
		to = null == to ? "" : to;
		return Filters.and(Filters.gte(ID, begin), Filters.lte(ID, to));
	}

	/* 获取链接 */
	private MongoCollection<Document> getCollection() {
		return m_Collection;
	}

	/* 生成版本 */
	private String genVersion(String version) {
		return VersionTags.next(m_Serverid, version, false);
	}

	/* 转换成文档 */
	private Document toDoc(E object, String version, String serverid, long timestamp) {
		Document doc = new Document();
		doc.append(ID, genId(object));
		doc.append(VERSION, version);
		doc.append(LASTMODIFIED, timestamp);
		doc.append(SERVERID, serverid);
		DtObject dt = m_Mapper.toDtObject(object);
		doc = MongodbUtil.dtToDoc(doc, dt);
		return doc;
	}

	/* 包装文档 */
	private E wrap(Document doc) {
		if (null == doc) {
			return null;
		}
		SimpleDtObject dt = new SimpleDtObject();
		dt.put(ID, String.valueOf(doc.get(ID)));
		dt.put(VERSION, doc.getString(VERSION));
		dt.put(LASTMODIFIED, doc.getLong(LASTMODIFIED));
		dt.put(SERVERID, StringUtil.toString(doc.get(SERVERID)));
		dt = MongodbUtil.docToDt(dt, doc);
		return m_Mapper.fromDtObject(dt);
	}

	/* 是否包含指定选项 */
	private static boolean isOptions(int option, int options) {
		return (option == (option & options));
	}

	@Override
	public String toString() {
		return "mongod:" + m_Name;
	}

}

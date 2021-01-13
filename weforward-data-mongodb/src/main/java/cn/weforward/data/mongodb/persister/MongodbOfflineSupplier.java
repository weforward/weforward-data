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

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.TransResultPage;
import cn.weforward.data.mongodb.util.MongodbResultPage;
import cn.weforward.data.mongodb.util.MongodbUtil;
import cn.weforward.data.persister.ObjectWithVersion;
import cn.weforward.data.persister.support.AbstractOfflineSupplier;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 基于mongodb为远端对象提供脱机支持
 * 
 * @author daibo
 *
 */
public class MongodbOfflineSupplier<E> extends AbstractOfflineSupplier<E> {
	/** 持久器名 */
	final String m_Name;
	/** 映射表 */
	final ObjectMapper<E> m_Mapper;
	/** 数据库 */
	final MongoDatabase m_Db;
	/** 链接 */
	final MongoCollection<Document> m_Collection;
	/** id属性 */
	final static String ID = MongodbPersister.ID;
	final static String DRIVEIT = MongodbPersister.DRIVEIT;

	public MongodbOfflineSupplier(MongoDatabase db, ObjectMapper<E> mapper, String name) {
		m_Mapper = mapper;
		m_Name = name.toLowerCase();
		m_Db = db;
		m_Collection = db.getCollection(m_Name);
	}

	/* 获取链接 */
	private MongoCollection<Document> getCollection() {
		return m_Collection;
	}

	@Override
	protected ObjectWithVersion<E> doGet(String id) {
		FindIterable<Document> it = getCollection().find(Filters.eq(ID, id));
		Document doc = it.first();
		if (null == doc) {
			return null;
		}
		E e = wrap(doc);
		return new ObjectWithVersion<E>(e, null, doc.getString(DRIVEIT));
	}

	@Override
	protected synchronized String doUpdate(String id, E obj) {
		MongoCollection<Document> c = getCollection();
		Document doc = toDoc(obj, id);
		Bson filter = Filters.eq(ID, id);
		UpdateResult result = c.replaceOne(filter, doc);
		if (result.getMatchedCount() == 0) {
			c.insertOne(doc);
		}
		return null;
	}

	@Override
	protected boolean doRemove(String id) {
		Bson filter = Filters.eq(ID, id);
		DeleteResult result = getCollection().deleteOne(filter);
		return result.getDeletedCount() > 0;
	}

	@Override
	public void removeAll() {
		getCollection().drop();
	}

	private E wrap(Document doc) {
		doc.put("id", doc.getString(ID));// 特殊处理掉id属性
		SimpleDtObject dt = new SimpleDtObject();
		dt = MongodbUtil.docToDt(dt, doc);
		return m_Mapper.fromDtObject(dt);
	}

	private Document toDoc(E object, String id) {
		DtObject dt = m_Mapper.toDtObject(object);
		Document doc = new Document();
		doc.append(ID, id);
		if (object instanceof cn.weforward.common.DistributedObject) {
			doc.append(DRIVEIT, ((cn.weforward.common.DistributedObject) object).getDriveIt());
		}
		doc = MongodbUtil.dtToDoc(doc, dt);
		doc.remove("id");// 特殊处理掉id属性
		return doc;
	}

	@Override
	public ResultPage<E> searchRange(String first, String last) {
		first = null == first ? "" : first;
		last = null == last ? StringUtil.UNICODE_REPLACEMENT_STRING : last + StringUtil.UNICODE_REPLACEMENT_CHAR;
		Bson range = Filters.and(Filters.gt(ID, first), Filters.lte(ID, last));
		return toResult(range);
	}

	@Override
	public void cleanup() {
	}

	/* 转换结果 */
	private ResultPage<E> toResult(Bson filter) {
		ResultPage<String> rp = toResultId(filter);
		return new TransResultPage<E, String>(rp) {

			@Override
			protected E trans(String src) {
				ObjectWithVersion<E> v = get(src);
				return null == v ? null : v.getObject();
			}
		};
	}

	/* 转换结果id */
	private ResultPage<String> toResultId(Bson filter) {
		return new MongodbResultPage<String>(getCollection(), filter) {

			@Override
			protected String to(Document doc) {
				return null == doc ? null : doc.getString(ID);
			}

		};
	}

}

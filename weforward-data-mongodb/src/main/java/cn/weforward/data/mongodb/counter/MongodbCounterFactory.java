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
package cn.weforward.data.mongodb.counter;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;

import cn.weforward.common.util.LruCache.DirtyData;
import cn.weforward.data.counter.Counter;
import cn.weforward.data.counter.support.CounterItem;
import cn.weforward.data.counter.support.DbCounter;
import cn.weforward.data.counter.support.DbCounterFactory;
import cn.weforward.data.mongodb.util.MongodbUtil;
import cn.weforward.data.util.Flusher;

/**
 * 在Mongodb下的计数器实现
 * 
 * @author liangyi
 *
 */
public class MongodbCounterFactory extends DbCounterFactory {
	protected final static Logger _Logger = LoggerFactory.getLogger(MongodbCounterFactory.class);

	protected MongoDatabase m_Db;
	protected String m_FieldName;
	protected UpdateOptions m_UpdateOptions;

	public MongodbCounterFactory(String serverId, String connection, String dbname,
			Flusher flusher) {
		this(serverId, MongodbUtil.create(connection).getDatabase(dbname), flusher);
	}

	public MongodbCounterFactory(String serverId, MongoDatabase db, Flusher flusher) {
		super(serverId);
		m_Db = db;
		m_FieldName = "v_" + getServerId();
		m_UpdateOptions = new UpdateOptions();
		m_UpdateOptions.upsert(true);
		setFlusher(flusher);
	}

	private long toLong(Object value) {
		if (value instanceof Long) {
			return (Long) value;
		}
		return 0;
	}

	@Override
	protected CounterItem doLoad(DbCounter counter, String id) {
		MongodbCounter mdbCounter = (MongodbCounter) counter;
		FindIterable<Document> it = mdbCounter.getCollection().find(Filters.eq(MongodbUtil.ID, id));
		Document doc = it.first();
		if (null == doc) {
			return null;
		}
		Set<Map.Entry<String, Object>> set = doc.entrySet();
		// 遍历记录各服务器标识下字段的值
		CounterItem item = new CounterItem(id);
		String current = getFieldName().toLowerCase();
		for (Map.Entry<String, Object> entry : set) {
			String name = entry.getKey().toLowerCase();
			if (name.startsWith("v_")) {
				if (name.equals(current)) {
					// 当前的
					item.value = toLong(entry.getValue());
				} else {
					// 其它的
					item.hold += toLong(entry.getValue());
				}
			}
		}
		return item;
	}

	@Override
	protected void doUpdate(DbCounter counter, DirtyData<CounterItem> data) {
		MongodbCounter mdbCounter = (MongodbCounter) counter;
		long ts = 0;
		if (_Logger.isTraceEnabled()) {
			ts = System.currentTimeMillis();
		}
		data.begin();
		CounterItem item;
		try {
			LinkedList<UpdateOneModel<Document>> requests = new LinkedList<>();
			while (data.hasNext()) {
				item = data.next();
				BsonDocument update = new BsonDocument();
				update.put(getFieldName(), new BsonInt64(item.value));
				update = new BsonDocument("$set", update);
				UpdateOneModel<Document> updateOneModel = new UpdateOneModel<Document>(
						Filters.eq(MongodbUtil.ID, item.id), update, m_UpdateOptions);
				requests.add(updateOneModel);
			}
			if (requests.isEmpty()) {
				// 没有需要刷写项
				data.commit();
				data = null;
				return;
			}
			BulkWriteResult result = mdbCounter.getCollection().bulkWrite(requests);
			if (result.getMatchedCount() == requests.size()) {
				// 批处理成功
				data.commit();
				data = null;
			} else {
				// 批处理（部分）不成功
				_Logger.warn("{requests:" + requests.size() + ",matched:" + result.getMatchedCount()
						+ ",modified:" + result.getModifiedCount() + ",inserted:"
						+ result.getInsertedCount() + "}");
			}
			if (ts > 0) {
				ts = System.currentTimeMillis() - ts;
				if (ts > 100) {
					_Logger.trace("{mills:" + ts + ",requests:" + requests.size() + ",matched:"
							+ result.getMatchedCount() + ",modified:" + result.getModifiedCount()
							+ ",inserted:" + result.getInsertedCount() + "}");
				}
			}
		} finally {
			if (null != data) {
				data.rollback();
			}
		}
	}

	@Override
	protected void doNew(DbCounter counter, CounterItem item) {
		MongodbCounter mdbCounter = (MongodbCounter) counter;
		// {"$set":{"x005d":100}},{upsert:true}
		BsonDocument update = new BsonDocument();
		update.put(getFieldName(), new BsonInt64(item.value));
		update = new BsonDocument("$set", update);
		mdbCounter.getCollection().updateOne(Filters.eq(MongodbUtil.ID, item.id), update,
				m_UpdateOptions);
	}

	@Override
	protected Counter doCreateCounter(String name) {
		MongodbCounter counter = new MongodbCounter(name, this);
		return counter;
	}

	private String getFieldName() {
		return m_FieldName;
	}
}

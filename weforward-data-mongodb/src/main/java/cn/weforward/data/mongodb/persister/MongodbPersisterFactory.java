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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import cn.weforward.data.mongodb.util.MongodbUtil;
import cn.weforward.data.mongodb.util.MongodbWatcher;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.persister.PersisterFactory;
import cn.weforward.data.persister.PersisterSet;
import cn.weforward.data.persister.support.AbstractPersisterFactory;
import cn.weforward.data.persister.support.SimplePersisterSet;
import cn.weforward.protocol.ext.ObjectMapper;

/**
 * 基于mongodb的持久器工厂
 * 
 * @author daibo
 *
 */
public class MongodbPersisterFactory extends AbstractPersisterFactory implements PersisterFactory {
	/** mongodata数据库 */
	final MongoDatabase m_Db;
	/** 监控 */
	final MongodbWatcher m_Watcher;

	public MongodbPersisterFactory(String connection, String dbname) {
		this(MongodbUtil.create(connection).getDatabase(dbname), new SimplePersisterSet());
	}

	public MongodbPersisterFactory(String connection, String dbname, PersisterSet ps) {
		this(MongodbUtil.create(connection).getDatabase(dbname), ps);
	}

	public MongodbPersisterFactory(MongoDatabase db, PersisterSet ps) {
		super(ps);
		m_Db = db;
		m_Watcher = new MongodbWatcher(db);
	}

	public void setMaxAwaitTime(long v) {
		m_Watcher.setMaxAwaitTime(v);
	}

	@Override
	public <E extends Persistent> Persister<E> doCreatePersister(Class<E> clazz, ObjectMapper<E> mapper) {
		return new MongodbPersister<E>(this, mapper);
	}

	public MongoCollection<Document> getCollection(String collectionName) {
		return m_Db.getCollection(collectionName);
	}

	public void wacher(MongodbWatcher.DocumentChange change) {
		m_Watcher.put(change);

	}

	public void unWacher(MongodbWatcher.DocumentChange change) {
		m_Watcher.remove(change);
	}

}

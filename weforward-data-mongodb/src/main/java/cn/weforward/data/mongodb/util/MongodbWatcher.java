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
package cn.weforward.data.mongodb.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

/**
 * mongodb监控
 * 
 * @author daibo
 *
 */
public class MongodbWatcher extends AbstractMongodbChangeSupport {
	/** 变化通知 */
	private Map<String, DocumentChange> m_Changes;

	public MongodbWatcher(MongoDatabase db) {
		super(db);
		m_Changes = new ConcurrentHashMap<>();
	}

	public void put(DocumentChange change) {
		if (m_Changes.isEmpty()) {
			start();
		}
		m_Changes.put(change.getCollection().getNamespace().getCollectionName(), change);
	}

	public void remove(DocumentChange change) {
		m_Changes.remove(change.getCollection().getNamespace().getCollectionName());
		if (m_Changes.isEmpty()) {
			stop();
		}
	}

	protected void onChange(ChangeStreamDocument<Document> doc) {
		String collection = doc.getNamespace().getCollectionName();
		DocumentChange change = m_Changes.get(collection);
		if (null != change) {
			change.onChange(doc);
		}

	}

	/**
	 * 文档变化
	 * 
	 * @author daibo
	 *
	 */
	public static interface DocumentChange {
		/**
		 * 变化事件
		 * 
		 * @param doc 变化文档
		 */
		void onChange(ChangeStreamDocument<Document> doc);

		/**
		 * 获取集合
		 * 
		 * @return 集合
		 */
		MongoCollection<Document> getCollection();
	}

}

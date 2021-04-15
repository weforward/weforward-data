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
import org.junit.Test;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

import cn.weforward.data.mongodb.util.MongodbUtil;
import cn.weforward.data.mongodb.util.MongodbWatcher;
import cn.weforward.data.mongodb.util.MongodbWatcher.DocumentChange;

public class TestWatcher {
	@Test
	public void test() {
		String connection = System.getProperty("mongo.url");
		MongoClient client = MongodbUtil.create(connection);
		MongoDatabase db = client.getDatabase("test");
		MongodbWatcher wather = new MongodbWatcher(db);
		wather.put(new DocumentChange() {

			@Override
			public void onChange(ChangeStreamDocument<Document> doc) {
				System.out.println(doc);
			}

			@Override
			public MongoCollection<Document> getCollection() {
				return db.getCollection("mydata");
			}
		});

		wather.put(new DocumentChange() {

			@Override
			public void onChange(ChangeStreamDocument<Document> doc) {
				System.out.println(doc);
			}

			@Override
			public MongoCollection<Document> getCollection() {
				return db.getCollection("mydata2");
			}
		});
		synchronized (wather) {
			try {
				wather.wait(1000000000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}

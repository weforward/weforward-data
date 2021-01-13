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
import com.mongodb.client.model.changestream.ChangeStreamDocument;

import cn.weforward.data.mongodb.persister.MongodbWatcher.DocumentChange;
import cn.weforward.data.mongodb.util.MongodbUtil;

public class TestWatcher {
	@Test
	public void test() {
		String connection = System.getProperty("mongo.url");
		MongoClient client = MongodbUtil.create(connection);
		MongoCollection<Document> collections = client.getDatabase("test").getCollection("mydata");
		MongodbWatcher wather = new MongodbWatcher(new DocumentChange() {

			@Override
			public String getName() {
				return "123";
			}

			@Override
			public void onChange(ChangeStreamDocument<Document> doc) {
				System.out.println(doc);
			}

			@Override
			public MongoCollection<Document> getCollection() {
				return collections;
			}
		});

		synchronized (wather) {
			try {
				wather.wait(100000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}

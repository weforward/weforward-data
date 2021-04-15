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

import java.util.Arrays;

import org.bson.Document;
import org.junit.Test;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import cn.weforward.common.ResultPage;
import cn.weforward.data.UniteId;
import cn.weforward.data.mongodb.util.MongodbUtil;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.persister.ext.ConditionUtil;
import cn.weforward.data.util.DelayFlusher;

public class TestPersister {
	@Test
	public void testremove() {
		String connection = System.getProperty("mongo.url");
		MongodbPersisterFactory factory = new MongodbPersisterFactory(connection, "test");
		factory.setServerId("x00ff");
		factory.setFlusher(new DelayFlusher());
		DataDi di = new DataDi(factory);
		Persister<MyIndex> ps = factory.createPersister(MyIndex.class, di);
		MyIndex index = new MyIndex(di, "HelloWorld", 18, "20200202");
		System.out.println(ps.get(index.getPersistenceId()));
		ps.remove(index.getPersistenceId());
		index.setName("HaHaHa");
		System.out.println(ps.get(index.getPersistenceId()));

	}

	// @Test
	public void testindex() {
		String connection = System.getProperty("mongo.url");
		MongodbPersisterFactory factory = new MongodbPersisterFactory(connection, "test");
		factory.setServerId("x00ff");
		factory.setFlusher(new DelayFlusher());
		DataDi di = new DataDi(factory);
		Persister<MyIndex> ps = factory.createPersister(MyIndex.class, di);
		// 相等
		ps.search(ConditionUtil.and(ConditionUtil.eq(ConditionUtil.field("name"), "HelloWorld"),
				ConditionUtil.eq(ConditionUtil.field("name"), "20200808")));

		ResultPage<MyIndex> rp = ps
				.search(ConditionUtil.range(ConditionUtil.field("m_Vo", "myString"), "20200808", "202008109"));
		int v = 20200808;
		for (int i = 0; i < 20; i++) {
			new MyIndex(di, "HelloWorld", 99, String.valueOf(v + i));
		}
		for (int i = 1; rp.gotoPage(i); i++) {
			for (MyIndex index : rp) {
				System.out.println(index.m_Name + "," + index.m_Age + "," + index.m_Vo.myString);
			}
		}

	}

	// @Test
	public void testclose() {
		String connection = System.getProperty("mongo.url");
		MongoClient client = MongodbUtil.create(connection);
		MongoCollection<Document> collections = client.getDatabase("test").getCollection("mydata");
		for (int i = 0; i < 600; i++) {
			MongoCursor<Document> c = collections.find().iterator();
			if (c.hasNext()) {
				c.next();
			}
			System.out.println(c + " " + i);
		}
	}

	// @Test
	public void testgc() {
		String connection = System.getProperty("mongo.url");
		MongodbPersisterFactory factory = new MongodbPersisterFactory(connection, "test");
		factory.setServerId("x00ff");
		factory.setFlusher(new DelayFlusher());
		DataDi di = new DataDi(factory);
		Persister<MyData> ps = factory.createPersister(MyData.class, di);
		System.out.println(ps);
		int count = 0;
		while (true) {
			for (int i = 0; i < 1000; i++) {
				Vo vo = new Vo();
				vo.init();
				MyData data = new MyData(di, null);
				data.setVo(vo);
				data.setVos(new Vo[] { vo, vo });
				data.setVoList(Arrays.asList(vo, vo));
				// data.init();
				data.save();
				count++;
			}
			synchronized (this) {
				try {
					this.wait(1000 * 10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println(count);
			// System.gc();

		}
	}

	// @Test
	public void testData() {
		String connection = System.getProperty("mongo.url");

//		MongoClient client = MongodbUtil.create(connection);
//		MongoDatabase db = client.getDatabase("test");
//		MongoCollection<Document> c = db.getCollection("mydata");
//		FindIterable<Document> it = c.find();
//		MongoCursor<Document> cursor = it.iterator();
//		while (cursor.hasNext()) {
//			System.out.println(cursor.next());
//		}
		MongodbPersisterFactory factory = new MongodbPersisterFactory(connection, "test2");
		factory.setServerId("x00ff");
		factory.setFlusher(new DelayFlusher());
		DataDi di = new DataDi(factory);
		Persister<MyData> ps = factory.createPersister(MyData.class, di);
		new MyData(di, UniteId.valueOf("1234", MyData.class)).save();
		ResultPage<MyData> rp = ps.startsWith(null);
		rp.setPageSize(102);
		for (int i = 1; rp.gotoPage(i); i++) {
			System.out.println(i);
			while (rp.hasNext()) {
				System.out.println(rp.next());
			}
		}
//		for (int i = 0; i < 100; i++) {
//			Vo vo = new Vo();
//			vo.init();
//			MyData data = new MyData(di, null);
//			data.setVo(vo);
//			data.setVos(new Vo[] { vo, vo });
//			data.setVoList(Arrays.asList(vo, vo));
//			// data.init();
//			data.save();
//		}
		// System.out.println(ps);
//		UniteId uid = UniteId.valueOf("MyData$0171b4db750d0103-x00ff");
//		MyData data = ps.get(uid);
//		System.out.println(data);
//		if (null == data) {
//			Vo vo = new Vo();
//			vo.init();
//			data = new MyData(di, uid);
//			data.setVo(vo);
//			data.setVos(new Vo[] { vo, vo });
//			data.setVoList(Arrays.asList(vo, vo));
//			// data.init();
//			data.save();
//		} else {
//			data.setVo(null);
//			data.save();
//		}
//		Vo vo = data.getVo();
//		if (null != vo) {
//			vo.print(System.out);
//			if (null != data.getVos()) {
//				for (Vo v : data.getVos()) {
//					v.print(System.out);
//				}
//			}
//			if (null != data.getVoList()) {
//				for (Vo v : data.getVoList()) {
//					v.print(System.out);
//				}
//			}
//		} else {
//			data.print(System.out);
//		}

	}
}

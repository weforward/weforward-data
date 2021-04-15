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
package cn.weforward.data.mongodb;

import org.junit.Test;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.data.counter.Counter;
import cn.weforward.data.mongodb.counter.MongodbCounterFactory;
import cn.weforward.data.util.DelayFlusher;

public class MongodbCounterTest {

	protected MongodbCounterFactory m_Factory;

	public MongodbCounterTest() {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
		m_Factory = new MongodbCounterFactory("x00ff", System.getProperty("url"), "test", new DelayFlusher());
	}

	@Test
	public void test() throws Exception {
//		create();
		get();
		get();
		count();
		startsWith();
		searchRange();
	}

	public void create() throws Exception {
		Counter counter = m_Factory.createCounter("test");
		System.out.println(counter.inc("计数项.1"));
		System.out.println(counter.inc("计数项.Z"));
	}

	public void get() {
		Counter counter = m_Factory.openCounter("test");
		System.out.println(counter.get("计数项.1"));
		System.out.println(counter.get("计数项.2"));
	}

	public void count() throws Exception {
		Counter counter = m_Factory.openCounter("test");
		for (int i = 0; i < 1800; i++) {
			System.out.println(counter.inc(String.valueOf(i)));
		}
		for (int i = 0; i < 10; i++) {
			System.out.println(counter.dec(String.valueOf((char) ('A' + i))));
		}
	}

	void dump(ResultPage<String> rp) {
		for (String s : ResultPageHelper.toForeach(rp)) {
			System.out.println(s);
		}
	}

	public void startsWith() {
		Counter counter = m_Factory.openCounter("test");
		dump(counter.startsWith(null));
		dump(counter.startsWith("计数项."));
	}

	public void searchRange() {
		Counter counter = m_Factory.openCounter("test");
		dump(counter.searchRange("0", "Z"));
	}
}

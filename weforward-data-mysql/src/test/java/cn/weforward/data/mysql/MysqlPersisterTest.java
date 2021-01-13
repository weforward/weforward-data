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
package cn.weforward.data.mysql;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.ListUtil;
import cn.weforward.data.mysql.persister.MysqlPersister;
import cn.weforward.data.mysql.persister.MysqlPersisterFactory;
import cn.weforward.data.mysql.util.CanalWather;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.util.DelayFlusher;

public class MysqlPersisterTest implements TestDi {

	protected MysqlPersisterFactory m_Factory;

	// @Before
	public void Before() {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
		m_Factory = new MysqlPersisterFactory(System.getProperty("url"));
		CanalWather w = new CanalWather("127.0.0.1", 11111, "example", "", "");
		w.setDatabase("test");
		m_Factory.setWatcher(w);
		m_Factory.setServerId("x00ff");
		m_Factory.setFlusher(new DelayFlusher());
		m_Factory.setDefaultStringLength(128);
	}

	// @Test
	public void simple() throws Exception {
		Persister<MyData> ps = m_Factory.createPersister(MyData.class, this);
		System.out.println(ps.getName());
		synchronized (this) {
			this.wait(10000000);
		}
		// ResultPage<SimpleData> rp = ps.startsWith(null);
		// System.out.println(rp.getCount());
//		Calendar cal = Calendar.getInstance();
//		for (int i = 100; i < 1000; i++) {
//			cal.set(Calendar.MILLISECOND, i);
//			SimpleVo vo;
//			if (i % 2 == 0) {
//				vo = new SimpleVo();
//				vo.m_MyValue = "MY-" + i;
//			} else {
//				vo = null;
//			}
//			new MyData(this, String.valueOf(i), 1000 + i, "H" + i, cal.getTime(), vo);
//		}
//		ResultPage<MyData> rp = ps.search(ConditionUtil.range("time", DtDate.Formater.parse("2020-11-06T03:57:13.100Z"),
//				DtDate.Formater.parse("2020-11-06T03:57:13.200Z")));
		// ResultPage<MyData> rp = ps.search(ConditionUtil.range("num", 1100, 1101));
//		ResultPage<MyData> rp = ps.search(ConditionUtil.eq(ConditionUtil.field("我不存在"), (String) null));
//		int count = 0;
//		for (int i = 1; rp.gotoPage(i); i++) {
//			for (MyData data : rp) {
//				count++;
//				System.out.println(data.getPersistenceId() + "," + data.m_Value);
//			}
//		}
//		System.out.println(count == rp.getCount());
	}

	// @Test
	public void query() {
		MysqlPersister<TestData> ps = (MysqlPersister<TestData>) m_Factory.createPersister(TestData.class, this);
		ResultPage<TestData> rp = ps.startsWith(null);
		rp.setPageSize(1);
		System.out.println(rp.getCount());
		for (int i = 1; rp.gotoPage(i); i++) {
			while (rp.hasNext()) {
				System.out.println(rp.next().getPersistenceId());
			}
		}
	}

	// @Test
	public void save() {
		MysqlPersister<TestData> ps = (MysqlPersister<TestData>) m_Factory.createPersister(TestData.class, this);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1; i++) {
			sb.append("我");
		}
		String v = sb.toString();
		TestData data = new TestData(this, v);
		String id = data.getPersistenceId().getId();
		ps.cleanup();
		data = ps.get(id);
		// System.out.println(new SimpleDateFormat("yyyy-MM-dd
		// HH:mm:ss.SSS").format(data.m_DateValue));
		assertTrue(data.m_BooleanInit == false);
		assertTrue(data.m_BooleanTrue == true);
		assertTrue(data.m_BooleanFalse == false);

		assertTrue(data.m_BooleanInitObject == null);
		assertTrue(data.m_BooleanTrueObject == true);
		assertTrue(data.m_BooleanFalseObject == false);

		assertTrue(data.m_IntInit == 0);
		assertTrue(data.m_ByteMax == Byte.MAX_VALUE);
		assertTrue(data.m_ByteMin == Byte.MIN_VALUE);
		assertTrue(data.m_ByteZero == 0);

		assertTrue(data.m_ByteInitObject == null);
		assertTrue(data.m_ByteMaxObject == Byte.MAX_VALUE);
		assertTrue(data.m_ByteMinObject == Byte.MIN_VALUE);
		assertTrue(data.m_ByteZeroObject == 0);

		assertTrue(data.m_ShortInit == 0);
		assertTrue(data.m_ShortMax == Short.MAX_VALUE);
		assertTrue(data.m_ShortMin == Short.MIN_VALUE);
		assertTrue(data.m_ShortZero == 0);

		assertTrue(data.m_ShortInitObject == null);
		assertTrue(data.m_ShortMaxObject == Short.MAX_VALUE);
		assertTrue(data.m_ShortMinObject == Short.MIN_VALUE);
		assertTrue(data.m_ShortZeroObject == 0);

		assertTrue(data.m_IntInit == 0);
		assertTrue(data.m_IntMax == Integer.MAX_VALUE);
		assertTrue(data.m_IntMin == Integer.MIN_VALUE);
		assertTrue(data.m_IntZero == 0);

		assertTrue(data.m_IntInitObject == null);
		assertTrue(data.m_IntMaxObject == Integer.MAX_VALUE);
		assertTrue(data.m_IntMinObject == Integer.MIN_VALUE);
		assertTrue(data.m_IntZeroObject == 0);

		assertTrue(data.m_LongInit == 0);
		assertTrue(data.m_LongMax == Long.MAX_VALUE);
		assertTrue(data.m_LongMin == Long.MIN_VALUE);
		assertTrue(data.m_LongZero == 0);

		assertTrue(data.m_FloatInitObject == null);
		assertTrue(data.m_FloatMaxObject == Float.MAX_VALUE);
		assertTrue(data.m_FloatMinObject == Float.MIN_VALUE);
		assertTrue(data.m_FloatZeroObject == 0);

		assertTrue(data.m_FloatInitObject == null);
		assertTrue(data.m_FloatMaxObject == Float.MAX_VALUE);
		assertTrue(data.m_FloatMinObject == Float.MIN_VALUE);
		assertTrue(data.m_FloatZeroObject == 0);

		assertTrue(data.m_DoubleInitObject == null);
		assertTrue(data.m_DoubleMaxObject == Double.MAX_VALUE);
		assertTrue(data.m_DoubleMinObject == Double.MIN_VALUE);
		assertTrue(data.m_DoubleZeroObject == 0);

		assertTrue(data.m_DoubleInitObject == null);
		assertTrue(data.m_DoubleMaxObject == Double.MAX_VALUE);
		assertTrue(data.m_DoubleMinObject == Double.MIN_VALUE);
		assertTrue(data.m_DoubleZeroObject == 0);

		assertTrue(data.m_StringInit == null);
		assertTrue(data.m_StringEmpty.equals(""));
		assertTrue(data.m_StringValue.equals(v));

		assertTrue(data.m_DateInit == null);
		assertTrue(data.m_DateMin.getTime() == 0);
		assertTrue(data.m_DateMax.getTime() == TestData.MAX.getTime());

		assertTrue(data.m_VoInit == null);
		assertTrue(data.m_VoObject.equals(new DataVo(v)));

		assertTrue(data.m_ListInit == null);
		assertTrue(data.m_ListEmpty.size() == 0);
		assertTrue(ListUtil.eq(data.m_ListInteger, Arrays.asList(1, 2, 3)));
		assertTrue(ListUtil.eq(data.m_ListDouble, Arrays.asList(1.1, 2.2, 3.3)));
		assertTrue(ListUtil.eq(data.m_ListString, Arrays.asList("Hello", "World")));
		assertTrue(ListUtil.eq(data.m_ListVos, Arrays.asList(new DataVo("Hello"), new DataVo("World"))));
	}

	// @Test
	public void delete() {
		MysqlPersister<TestData> ps = (MysqlPersister<TestData>) m_Factory.createPersister(TestData.class, this);
		System.out.println(ps.remove("0175977d52c201-x00ff"));
	}

	@Override
	public <E extends Persistent> Persister<E> getPersister(Class<E> clazz) {
		return m_Factory.getPersister(clazz);
	}
}

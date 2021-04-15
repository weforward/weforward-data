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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.common.util.TimeUtil;
import cn.weforward.data.UniteId;
import cn.weforward.data.array.LabelSet;
import cn.weforward.data.log.BusinessLog;
import cn.weforward.data.log.BusinessLogger;
import cn.weforward.data.log.label.LabelBusinessLoggerFactory;
import cn.weforward.data.util.FieldMapper;

public class TestArray {
	MongodbLabelSetFactory factory;

	@Before
	public void setUp() {
		String connection = System.getProperty("mongo.url");
		factory = new MongodbLabelSetFactory(connection);
		factory.setHashSize(0);

	}

	// @Test
	public void test() {
		LabelSet<MyData> labelSet = factory.createLabelSet("testlabel", FieldMapper.valueOf(MyData.class));
		String label = "mylabel";
		String id = "1234";
		MyData element = labelSet.get(label, id);
		if (null == element) {
			element = new MyData(id);
			element.init();
			Vo vo = new Vo();
			vo.init();
			element.setVo(vo);
			element.setVos(new Vo[] { vo, vo });
			element.setVoList(Arrays.asList(vo, vo));
			System.out.println(labelSet.put(label, element));
		}
		System.out.println(element.getIdForLabel());
	}

	@Test
	public void testUUid() {
		LabelSet<UuidData> labelSet = factory.createLabelSet("uuidlabel", FieldMapper.valueOf(UuidData.class));
		String label = "mylabel";
		UuidData element = labelSet.get(label, "Object$123");
		System.out.println(element);
	}

	// @Test
	public void testLog() {
		LabelBusinessLoggerFactory lf = new LabelBusinessLoggerFactory(factory);
		BusinessLogger logger = lf.openLogger("order_log");
		UniteId uid = UniteId.valueOf("Order$123");
		logger.writeLog(uid.getId(), "System", "order", "提交订单", uid.toString());
		logger.writeLog(uid.getId(), "System", "order", "取消订单");
		ResultPage<BusinessLog> rp = logger.getLogs(uid.getId());
		for (BusinessLog log : ResultPageHelper.toForeach(rp)) {
			System.out.println(TimeUtil.formatDateTime(log.getTime()) + "," + log.getAuthor() + "," + log.getAction()
					+ "," + log.getWhat() + "," + log.getNote());
		}
	}

}

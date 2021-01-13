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

import java.util.Date;

import org.junit.Test;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.common.util.TimeUtil;
import cn.weforward.data.log.BusinessLog;
import cn.weforward.data.log.BusinessLogger;
import cn.weforward.data.mysql.log.MysqlBusinessLoggerFactory;

public class MysqlLogTest {

	protected MysqlBusinessLoggerFactory m_Factory;

	public MysqlLogTest() {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
		m_Factory = new MysqlBusinessLoggerFactory("x00ff", System.getProperty("url"));
	}

	@Test
	public void test() throws Exception {
		BusinessLogger logger = m_Factory.openLogger("test");
		logger.writeLog("account-1", "ll", "add", "money",
				"加100W " + TimeUtil.formatTimestamp(System.currentTimeMillis(), null));
		logger.writeLog("account-2", "ll", "sub", "money",
				"减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W减100W");
		dump(logger.getLogs("account-1"));
		dump(logger.searchLogs("account-2", new Date(System.currentTimeMillis() - (60 * 1000)),
				new Date(System.currentTimeMillis() + (10 * 1000))));
	}

	void dump(ResultPage<BusinessLog> rp) {
		for (BusinessLog s : ResultPageHelper.toForeach(rp)) {
			System.out.println(s);
		}
	}
}

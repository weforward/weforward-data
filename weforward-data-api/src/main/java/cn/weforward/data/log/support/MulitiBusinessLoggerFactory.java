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
package cn.weforward.data.log.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.weforward.common.ResultPage;
import cn.weforward.data.log.BusinessLog;
import cn.weforward.data.log.BusinessLogger;
import cn.weforward.data.log.BusinessLoggerFactory;

/**
 * 多个业务日志记录器集合
 * 
 * @author daibo
 *
 */
public class MulitiBusinessLoggerFactory extends AbstractBusinessLoggerFactory {
	/** 工厂集合 */
	protected List<BusinessLoggerFactory> m_Factorys;
	/** 主工厂 */
	protected int m_Main;

	public MulitiBusinessLoggerFactory(String serverId, List<BusinessLoggerFactory> factorys, int main) {
		super(serverId);
		m_Factorys = factorys;
		m_Main = main;
	}

	@Override
	protected BusinessLogger doCreateLogger(String name) {
		List<AbstractBusinessLogger> list = new ArrayList<>();
		for (BusinessLoggerFactory ps : m_Factorys) {
			BusinessLogger p = ps.createLogger(name);
			list.add((AbstractBusinessLogger) p);
		}
		MultiBusinessLogger ps = new MultiBusinessLogger(list, name);
		return ps;
	}

	public class MultiBusinessLogger extends AbstractBusinessLogger {

		protected List<AbstractBusinessLogger> m_List;

		public MultiBusinessLogger(List<AbstractBusinessLogger> list, String name) {
			super(name);
			m_List = list;
		}

		@Override
		public ResultPage<BusinessLog> searchLogs(String id, Date begin, Date end) {
			return m_List.get(m_Main).searchLogs(id, begin, end);
		}

		@Override
		public String getServerId() {
			return MulitiBusinessLoggerFactory.this.getServerId();
		}

		@Override
		protected void writeLog(BusinessLog log) {
			for (AbstractBusinessLogger l : m_List) {
				l.writeLog(log);
			}
		}

	}

}

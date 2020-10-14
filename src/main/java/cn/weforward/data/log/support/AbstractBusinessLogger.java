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

import java.util.Date;

import cn.weforward.common.ResultPage;
import cn.weforward.data.log.BusinessLog;
import cn.weforward.data.log.BusinessLogger;
import cn.weforward.data.log.vo.BusinessLogVo;

/**
 * 抽象日志记录器实现
 * 
 * @author daibo
 *
 */
public abstract class AbstractBusinessLogger implements BusinessLogger {
	/** 名称 */
	protected String m_Name;

	public AbstractBusinessLogger(String name) {
		m_Name = name;
	}

	@Override
	public String getName() {
		return m_Name;
	}

	@Override
	public void writeLog(String id, String author, String action, String what) {
		writeLog(id, author, action, what, null);
	}

	@Override
	public void writeLog(String id, String author, String action, String what, String note) {
		writeLog(new BusinessLogVo(null, new Date(), id, author, action, what, note));
	}

	@Override
	public ResultPage<BusinessLog> getLogs(String id) {
		return searchLogs(id, null, null);
	}

}

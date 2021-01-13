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
package cn.weforward.data.log.label;

import java.util.Date;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.data.array.Label;
import cn.weforward.data.array.LabelSet;
import cn.weforward.data.array.LabelSetFactory;
import cn.weforward.data.log.BusinessLog;
import cn.weforward.data.log.support.AbstractBusinessLogger;
import cn.weforward.protocol.ext.ObjectMapper;

/**
 * labetSet日志记录器
 * 
 * @author daibo
 *
 */
public class LabelBusinessLogger extends AbstractBusinessLogger {
	/** 使用标签集实现日志存储 */
	private LabelSet<LogItem> m_LabelSet;
	/** 服务器标识 */
	protected String m_ServerId;
	/** 计数（避免同时间点的日志ID冲突） */
	protected int m_Ordinal;
	/** 最后日志时间 */
	protected long m_LastTime;

	public LabelBusinessLogger(String sid, LabelSetFactory factory, String name) {
		super(name);
		m_ServerId = sid;
		ObjectMapper<LogItem> log = LogItem._MAPPER;
		m_LabelSet = factory.createLabelSet(name, log);
	}

	@Override
	public void writeLog(BusinessLog log) {
		long t = log.getTime().getTime();
		synchronized (this) {
			if (t != m_LastTime) {
				m_LastTime = t;
				m_Ordinal = 0;
			} else {
				++m_Ordinal;
			}
		}
		LogItem e = new LogItem(log, m_ServerId, m_Ordinal);
		m_LabelSet.add(log.getTarget(), e);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ResultPage<BusinessLog> searchLogs(String id, Date begin, Date end) {
		Label<? extends BusinessLog> label = m_LabelSet.getLabel(id);
		if (null == label) {
			return ResultPageHelper.empty();
		}
		if (null == begin && null == end) {
			return (ResultPage<BusinessLog>) label.resultPage();
		}
		String first = null;
		String last = null;
		if (null != begin) {
			first = LogItem.genId(begin.getTime(), 0, null);
		}
		if (null != end) {
			last = LogItem.genId(end.getTime(), 0, null);
		}
		ResultPage<? extends BusinessLog> rp = label.searchRange(first, last);
		return (ResultPage<BusinessLog>) rp;
	}

	@Override
	public String toString() {
		return m_LabelSet.toString();
	}

	@Override
	public String getServerId() {
		return m_ServerId;
	}
}

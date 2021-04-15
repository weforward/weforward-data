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

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.weforward.data.log.BusinessLogger;
import cn.weforward.data.log.BusinessLoggerFactory;

/**
 * 抽象日志工厂实现
 * 
 * @author daibo
 *
 */
public abstract class AbstractBusinessLoggerFactory implements BusinessLoggerFactory {
	/** 数据项 */
	protected ConcurrentMap<String, BusinessLogger> m_Items;
	/** 服务器标识 */
	protected String m_ServerId;

	public AbstractBusinessLoggerFactory(String serverId) {
		m_Items = new ConcurrentHashMap<>();
		m_ServerId = serverId;
	}

	public String getServerId() {
		return m_ServerId;
	}

	@Override
	public Iterator<BusinessLogger> iterator() {
		return m_Items.values().iterator();
	}

	@Override
	public BusinessLogger openLogger(String name) {
		BusinessLogger c = m_Items.get(name);
		if (null != c) {
			return c;
		}
		c = doCreateLogger(name);
		BusinessLogger old = m_Items.putIfAbsent(name, c);
		return null == old ? c : old;
	}

	@Override
	public BusinessLogger createLogger(String name) {
		BusinessLogger c = m_Items.get(name);
		if (null != c) {
			throw new IllegalArgumentException("已存在同名的日志器[" + name + "]");
		}
		c = doCreateLogger(name);
		if (null == m_Items.putIfAbsent(name, c)) {
			return c;
		} else {
			throw new IllegalArgumentException("已存在同名的日志器[" + name + "]");
		}
	}

	@Override
	public BusinessLogger getLogger(String name) {
		return m_Items.get(name);
	}

	/* 创建计数器 */
	protected abstract BusinessLogger doCreateLogger(String name);
}

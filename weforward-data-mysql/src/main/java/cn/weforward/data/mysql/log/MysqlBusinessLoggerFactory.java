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
package cn.weforward.data.mysql.log;

import cn.weforward.data.jdbc.DataProvider;
import cn.weforward.data.log.BusinessLogger;
import cn.weforward.data.log.support.AbstractBusinessLoggerFactory;
import cn.weforward.data.mysql.MysqlConst;

/**
 * 基于mysql的业务日志工厂
 * 
 * @author daibo
 *
 */
public class MysqlBusinessLoggerFactory extends AbstractBusinessLoggerFactory {
	/** 数据提供者 */
	final DataProvider m_Provider;
	/** 默认的字符串长度 */
	private int m_DefaultStringLength = MysqlConst.DEFAULT_STRING_LENGTH;

	public MysqlBusinessLoggerFactory(String serviceId, String connectionString) {
		this(serviceId, connectionString, MysqlConst.DEFAULT_POOL_MAX_SIZE);
	}

	public MysqlBusinessLoggerFactory(String serviceId, String connectionString, int maxSize) {
		this(serviceId, MysqlConst.DEFAULT_DRICER_CLASS, connectionString, maxSize);
	}

	public MysqlBusinessLoggerFactory(String serviceId, String driverClass, String connectionString, int maxSize) {
		this(serviceId, driverClass, connectionString, maxSize, 0);
	}

	public MysqlBusinessLoggerFactory(String serviceId, String driverClass, String connectionString, int maxSize,
			int defaultStringLength) {
		super(serviceId);
		m_Provider = new DataProvider(driverClass, connectionString, maxSize);
		if (defaultStringLength > 0) {
			m_DefaultStringLength = defaultStringLength;
		}
	}

	@Override
	protected BusinessLogger doCreateLogger(String name) {
		return new MysqlBusinessLogger(this, name, m_ServerId, m_DefaultStringLength);
	}

	public DataProvider getProvider() {
		return m_Provider;
	}

}

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
package cn.weforward.data.log.ms;

import cn.weforward.common.util.StringUtil;
import cn.weforward.data.log.BusinessLogger;
import cn.weforward.data.log.support.AbstractBusinessLoggerFactory;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.client.ServiceInvokerFactory;

/**
 * 基于微服务的日志工厂
 * 
 * @author daibo
 *
 */
public class MicroserviceBusinessLoggerFactory extends AbstractBusinessLoggerFactory {
	/** 调整器 */
	protected ServiceInvoker m_Invoker;
	/** 数据库名 */
	protected String m_DbName;
	/** 方法组 */
	protected String m_MethodGroup = "";

	public MicroserviceBusinessLoggerFactory(String serverId, String preUrl, String accessId, String accessKey,
			String serviceName, String dbName) {
		super(serverId);
		m_Invoker = ServiceInvokerFactory.create(serviceName, preUrl, accessId, accessKey);
		m_DbName = dbName;
	}

	public void setMethodGroup(String v) {
		m_MethodGroup = StringUtil.toString(v);
	}

	public ServiceInvoker getInvoker() {
		return m_Invoker;
	}

	public String getDbName() {
		return m_DbName;
	}

	public String getMethodGroup() {
		return m_MethodGroup;
	}

	@Override
	protected BusinessLogger doCreateLogger(String name) {
		return new MicroserviceBusinessLogger(this, name);
	}

}

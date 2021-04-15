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

import java.util.Date;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.data.log.BusinessLog;
import cn.weforward.data.log.support.AbstractBusinessLogger;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.client.execption.GatewayException;
import cn.weforward.protocol.client.execption.MicroserviceException;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 基于微服务的日志记录器,暂时只实现了保存功能
 * 
 * @author daibo
 *
 */
public class MicroserviceBusinessLogger extends AbstractBusinessLogger {

	protected MicroserviceBusinessLoggerFactory m_Factory;

	public MicroserviceBusinessLogger(MicroserviceBusinessLoggerFactory factory, String name) {
		super(name);
		m_Factory = factory;
	}

	@Override
	public ResultPage<BusinessLog> searchLogs(String id, Date begin, Date end) {
		return ResultPageHelper.empty();
	}

	@Override
	public String getServerId() {
		return m_Factory.getServerId();
	}

	private String getCollectionName() {
		String name = getName();
		if (name.endsWith("_log")) {
			return name.toLowerCase();
		}
		return name.toLowerCase() + "_log";
	}

	@Override
	protected void writeLog(BusinessLog log) {
		SimpleDtObject content = new SimpleDtObject();
		content.put("_id", log.getId());
		content.put("ac", log.getAction());
		content.put("a", log.getAuthor());
		content.put("n", log.getNote());
		content.put("w", log.getWhat());
		ServiceInvoker invoker = m_Factory.getInvoker();
		String method = m_Factory.getMethodGroup() + "save";
		SimpleDtObject params = new SimpleDtObject();
		params.put("dbName", m_Factory.getDbName());
		params.put("collection", getCollectionName());
		params.put("content", content);
		Response response = invoker.invoke(method, params);
		GatewayException.checkException(response);
		DtObject serviceResult = response.getServiceResult();
		MicroserviceException.checkException(serviceResult);
	}

}

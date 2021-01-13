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
package cn.weforward.data.persister.remote;

import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.persister.PersisterSet;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.client.ServiceInvokerFactory;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.support.NamingConverter;

/**
 * 远调用持久类工具
 * 
 * @author daibo
 *
 */
public class RemoteInvokerPersisterFactory extends AbstractRemotePersisterFactory {
	/** 服务调用器 */
	protected ServiceInvoker m_Invoker;
	/** 方法组名 */
	protected String m_MethodGroup;

	/**
	 * 构造
	 * 
	 * @param preUrl      网关地址
	 * @param accessId    访问id
	 * @param accessKey   访问key
	 * @param serviceName 服务名
	 * @param methodGroup 方法组
	 */
	public RemoteInvokerPersisterFactory(String preUrl, String accessId, String accessKey, String serviceName,
			String methodGroup) {
		this(ServiceInvokerFactory.create(serviceName, preUrl, accessId, accessKey), methodGroup);
	}

	/**
	 * 构造
	 * 
	 * @param invoker     调用器
	 * @param methodGroup 方法组
	 */
	public RemoteInvokerPersisterFactory(ServiceInvoker invoker, String methodGroup) {
		this(null, invoker, methodGroup);
	}

	/**
	 * 构造
	 * 
	 * @param ps          持久器集合
	 * @param preUrl      网关地址
	 * @param accessId    访问id
	 * @param accessKey   访问key
	 * @param serviceName 服务名
	 * @param methodGroup 方法组
	 */
	public RemoteInvokerPersisterFactory(PersisterSet ps, String preUrl, String accessId, String accessKey,
			String serviceName, String methodGroup) {
		this(ps, ServiceInvokerFactory.create(serviceName, preUrl, accessId, accessKey), methodGroup);
	}

	/**
	 * 构造
	 * 
	 * @param ps          持久器集合
	 * @param invoker     调用器
	 * @param methodGroup 方法组
	 */
	public RemoteInvokerPersisterFactory(PersisterSet ps, ServiceInvoker invoker, String methodGroup) {
		super(ps);
		m_Invoker = invoker;
		m_MethodGroup = null == methodGroup ? "" : NamingConverter.camelToWf(methodGroup);
	}

	/**
	 * 生成方法名
	 * 
	 * @param method 方法名
	 * @return 结果方法名
	 */
	public String genMethod(String method) {
		return m_MethodGroup + NamingConverter.camelToWf(method);
	}

	/**
	 * 获取调用器
	 * 
	 * @return 调用器
	 */
	public ServiceInvoker getInvoker() {
		return m_Invoker;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected <E extends Persistent> Persister<E> doCreatePersister(Class<E> clazz, ObjectMapper<E> mapper,
			ObjectMapper<?> vomapper) {
		return new RemoteInvokerPersister(this, mapper, vomapper);
	}

}

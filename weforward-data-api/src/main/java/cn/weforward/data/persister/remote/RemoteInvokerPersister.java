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

import java.util.Date;
import java.util.Iterator;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.data.UniteId;
import cn.weforward.data.exception.DataAccessException;
import cn.weforward.data.persister.ChangeListener;
import cn.weforward.data.persister.Condition;
import cn.weforward.data.persister.ObjectWithVersion;
import cn.weforward.data.persister.OrderBy;
import cn.weforward.data.persister.PersistentListener;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.client.ext.RemoteResultPage;
import cn.weforward.protocol.client.ext.RequestInvokeParam;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.support.datatype.FriendlyObject;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 通过服务支撑的远程的持久化对象
 * 
 * @author daibo
 *
 * @param <E> 业务对象
 * @param <V> 值对象
 */
public class RemoteInvokerPersister<E extends AbstractRemotePersistent<?, V>, V> extends AbstractRemotePersister<E, V> {
	/** 工厂 */
	protected RemoteInvokerPersisterFactory m_Factory;
	/** 对象映射 */
	protected ObjectMapper<E> m_Mapper;
	/** 对象映射 */
	protected ObjectMapper<V> m_VoMapper;
	/** 空对象 */
	private static final DtObject DT_EMPTY = new SimpleDtObject();

	protected RemoteInvokerPersister(RemoteInvokerPersisterFactory factory, ObjectMapper<E> mapper,
			ObjectMapper<V> voMapper) {
		super(mapper.getName());
		m_Factory = factory;
		m_Mapper = mapper;
		m_VoMapper = voMapper;
	}

	/* 生成方法名 */
	protected String genMethod(String method) {
		return m_Factory.genMethod(method);
	}

	/* 获取调用器 */
	protected ServiceInvoker getInvoker() {
		return m_Factory.getInvoker();
	}

	@Override
	public ResultPage<String> startsWithOfId(String prefix) {
		String method = genMethod("startsWith" + getName());
		return new RemoteResultPage<String>(String.class, getInvoker(), method,
				RequestInvokeParam.valueOf("prefix", prefix));
	}

	@Override
	public ResultPage<String> searchOfId(Date begin, Date end) {
		String method = genMethod("search" + getName());
		return new RemoteResultPage<String>(String.class, getInvoker(), method,
				RequestInvokeParam.valueOf("begin", begin), RequestInvokeParam.valueOf("end", end));
	}

	@Override
	public ResultPage<String> searchRangeOfId(String from, String to) {
		String method = genMethod("searchRange" + getName());
		return new RemoteResultPage<String>(String.class, getInvoker(), method,
				RequestInvokeParam.valueOf("from", from), RequestInvokeParam.valueOf("to", to));
	}

	@Override
	public Iterator<String> searchOfId(String serverId, Date begin, Date end) {
		String method = genMethod("search" + getName());
		RemoteResultPage<String> rp = new RemoteResultPage<String>(String.class, getInvoker(), method,
				RequestInvokeParam.valueOf("begin", begin), RequestInvokeParam.valueOf("end", end));
		return ResultPageHelper.toForeach(rp).iterator();
	}

	@Override
	public Iterator<String> searchRangeOfId(String serverId, String from, String to) {
		String method = genMethod("searchRange" + getName());
		RemoteResultPage<String> ids = new RemoteResultPage<String>(String.class, getInvoker(), method,
				RequestInvokeParam.valueOf("serverId", serverId), RequestInvokeParam.valueOf("from", from),
				RequestInvokeParam.valueOf("to", to));
		return ResultPageHelper.toForeach(ids).iterator();
	}

	@Override
	protected E create(String id, ObjectWithVersion<V> vo) {
		E e = m_Mapper.fromDtObject(DT_EMPTY);
		if (e instanceof PersistentListener) {
			UniteId persistenceId = UniteId.fixId(id, getName());
			String version = null == vo ? null : vo.getVersion();
			String driverIt = null == vo ? null : vo.getDriveIt();
			((PersistentListener) e).onAfterReflect(this, persistenceId, version, driverIt);
		}
		if (null != vo) {
			e.updateVo(vo.getObject(), vo.getVersion());
		}
		return e;
	}

	/**
	 * 检查网关异常
	 * 
	 * @param response 响应
	 * @throws DataAccessException 数据异常
	 */
	public static void checkGateWayException(Response response) throws DataAccessException {
		if (response.getResponseCode() != 0) {
			String msg = "网关响应异常:" + response.getResponseCode() + "/" + response.getResponseMsg();
			throw new DataAccessException(msg);
		}
	}

	/**
	 * 检查微服务异常
	 * 
	 * @param serviceResult 结果
	 * @throws DataAccessException 据异常
	 */
	public static void checkMicroserviceException(FriendlyObject serviceResult) throws DataAccessException {
		if (serviceResult.getInt("code") != 0) {
			String msg = "微服务响应异常:" + serviceResult.getInt("code") + "/" + serviceResult.getString("msg");
			throw new DataAccessException(msg);
		}
	}

	@Override
	protected String remoteNew(String id, V object) {
		String method = genMethod("create" + getName());
		SimpleDtObject params = new SimpleDtObject();
		params.put("id", id);
		params.put("vo", m_VoMapper.toDtObject(object));
		Response response = getInvoker().invoke(method, params);
		checkGateWayException(response);
		FriendlyObject serviceResult = FriendlyObject.valueOf(response.getServiceResult());
		checkMicroserviceException(serviceResult);
		return serviceResult.getString("content");
	}

	@Override
	protected ObjectWithVersion<V> remoteLoad(String id, String version) {
		String method = genMethod("load" + getName());
		SimpleDtObject params = new SimpleDtObject();
		params.put("id", id);
		params.put("version", version);
		Response response = getInvoker().invoke(method, params);
		checkGateWayException(response);
		FriendlyObject serviceResult = FriendlyObject.valueOf(response.getServiceResult());
		checkMicroserviceException(serviceResult);
		FriendlyObject content = serviceResult.getFriendlyObject("content");
		String ver = content.getString("version");
		String driverIt = content.getString("driverIt");
		DtObject object = content.getObject("vo");
		if (null == object) {
			return null;
		}
		V vo = m_VoMapper.fromDtObject(object);
		return new ObjectWithVersion<V>(vo, ver, driverIt);
	}

	@Override
	protected String remoteSave(String id, V object) {
		String method = genMethod("save" + getName());
		SimpleDtObject params = new SimpleDtObject();
		params.put("id", id);
		params.put("vo", m_VoMapper.toDtObject(object));
		Response response = getInvoker().invoke(method, params);
		checkGateWayException(response);
		FriendlyObject serviceResult = FriendlyObject.valueOf(response.getServiceResult());
		checkMicroserviceException(serviceResult);
		return serviceResult.getString("content");
	}

	@Override
	protected boolean remoteDelete(String id) {
		String method = genMethod("delete" + getName());
		SimpleDtObject params = new SimpleDtObject();
		params.put("id", id);
		Response response = getInvoker().invoke(method, params);
		checkGateWayException(response);
		FriendlyObject serviceResult = FriendlyObject.valueOf(response.getServiceResult());
		checkMicroserviceException(serviceResult);
		return serviceResult.getBoolean("content", false);
	}

	@Override
	public void addListener(ChangeListener<E> l) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeListener(ChangeListener<E> l) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultPage<String> searchOfId(Condition condition, OrderBy orderBy) {
		throw new UnsupportedOperationException();
	}

}

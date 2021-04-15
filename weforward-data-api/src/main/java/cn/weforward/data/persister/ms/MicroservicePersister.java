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
package cn.weforward.data.persister.ms;

import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import cn.weforward.common.KvPair;
import cn.weforward.common.ResultPage;
import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.data.UniteId;
import cn.weforward.data.persister.Condition;
import cn.weforward.data.persister.ObjectWithVersion;
import cn.weforward.data.persister.OrderBy;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.remote.exception.GatewayException;
import cn.weforward.data.persister.remote.exception.MicroserviceException;
import cn.weforward.data.persister.support.AbstractPersistent;
import cn.weforward.data.persister.support.AbstractPersister;
import cn.weforward.data.util.VersionTags;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 基于微服务的存储器,暂时只实现了保存功能
 * 
 * @author daibo
 *
 */
public class MicroservicePersister<E extends Persistent> extends AbstractPersister<E> {
	/** id属性 */
	public final static String ID = Condition.ID;
	/** 最后修改时间 */
	public final static String LASTMODIFIED = Condition.LASTMODIFIED;
	/** 版本 */
	public final static String VERSION = Condition.VERSION;
	/** 服务器id */
	public final static String SERVERID = Condition.SERVERID;
	/** 控制实例id */
	public final static String DRIVEIT = Condition.DRIVEIT;

	protected MicroservicePersisterFactory m_Factory;

	protected ObjectMapper<E> m_Mapper;
	protected String m_Collection;

	protected MicroservicePersister(MicroservicePersisterFactory factory, ObjectMapper<E> mapper) {
		super(mapper.getName());
		m_Factory = factory;
		m_Mapper = mapper;
	}

	protected String getCollectionName() {
		return m_Mapper.getName().toLowerCase();
	}

	@Override
	protected String innerSave(E object) {
		String version = null;
		if (object instanceof AbstractPersistent<?>) {
			version = ((AbstractPersistent<?>) object).getPersistenceVersion();
		}
		version = genVersion(version);
		SimpleDtObject content = new SimpleDtObject();
		content.put(ID, getId(object.getPersistenceId()));
		content.put(VERSION, version);
		content.put(LASTMODIFIED, System.currentTimeMillis());
		content.put(SERVERID, getPersisterId());
		if (object instanceof cn.weforward.common.DistributedObject) {
			content.put(DRIVEIT, ((cn.weforward.common.DistributedObject) object).getDriveIt());
		}
		DtObject dt = m_Mapper.toDtObject(object);
		Enumeration<KvPair<String, DtBase>> myenum = dt.getAttributes();
		while (myenum.hasMoreElements()) {
			KvPair<String, DtBase> pair = myenum.nextElement();
			content.put(pair.getKey(), pair.getValue());
		}
		ServiceInvoker invoker = m_Factory.getInvoker();
		String method = m_Factory.getMethodGroup() + "save";
		SimpleDtObject params = new SimpleDtObject();
		params.put("dbName", m_Factory.getDbName());
		params.put("collection", getCollectionName());
		params.put("content", content);
		try {
			Response response = invoker.invoke(method, params);
			GatewayException.checkException(response);
			DtObject serviceResult = response.getServiceResult();
			MicroserviceException.checkException(serviceResult);
		} catch (RuntimeException e) {
			if (m_Factory.isIgnoreError()) {
				_Logger.warn("忽略异常", e);
			} else {
				throw e;
			}
		}
		return version;
	}

	/* 获取id */
	private String getId(UniteId id) {
		return id.getOrdinal();
	}

	/* 生成版本 */
	private String genVersion(String version) {
		return VersionTags.next(getPersisterId(), version, false);
	}

	@Override
	protected String innerNew(E object) {
		return innerSave(object);
	}

	@Override
	protected boolean innerDelete(String id) {
		return false;
	}

	@Override
	protected ObjectWithVersion<E> innerLoad(String id) {
		return null;
	}

	@Override
	public ResultPage<String> startsWithOfId(String prefix) {
		return ResultPageHelper.empty();
	}

	@Override
	public ResultPage<String> searchOfId(Date begin, Date end) {
		return ResultPageHelper.empty();
	}

	@Override
	public ResultPage<String> searchRangeOfId(String from, String to) {
		return ResultPageHelper.empty();
	}

	@Override
	public Iterator<String> searchOfId(String serverId, Date begin, Date end) {
		return Collections.emptyIterator();
	}

	@Override
	public Iterator<String> searchRangeOfId(String serverId, String from, String to) {
		return Collections.emptyIterator();
	}

	@Override
	public ResultPage<String> searchOfId(Condition condition, OrderBy orderBy) {
		return ResultPageHelper.empty();
	}

}

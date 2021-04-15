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

import cn.weforward.common.util.ClassUtil;
import cn.weforward.data.persister.OfflineSupplier;
import cn.weforward.data.persister.OfflineSupplierFactory;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.persister.PersisterSet;
import cn.weforward.data.persister.support.AbstractPersisterFactory;
import cn.weforward.data.util.FieldMapper;
import cn.weforward.data.util.MethodMapper;
import cn.weforward.protocol.ext.ObjectMapper;

/**
 * 远程持久类工厂
 * 
 * @author daibo
 *
 */
public abstract class AbstractRemotePersisterFactory extends AbstractPersisterFactory {
	/** 默认刷新加载超时值（毫秒） */
	static final int DEFAULT_REFRESHTIMEOUT = 1000;
	/** 默认VO过期时间 （毫秒） */
	static final int DEFAULT_EXPIRT = 60 * 1000;
	/** 默认是否共享加载器 */
	static final boolean DEFAULT_SHAREDELAYLOADER = false;

	/** 刷新加载超时值（毫秒） */
	protected int m_RefreshTimeout = DEFAULT_REFRESHTIMEOUT;
	/** VO过期时间（毫秒） */
	protected int m_Expiry = DEFAULT_EXPIRT;
	/** 是否共享加载器 */
	protected boolean m_ShareDelayLoader = DEFAULT_SHAREDELAYLOADER;
	/** 离线工厂 */
	protected OfflineSupplierFactory m_OfflineSupplierFactory;

	public AbstractRemotePersisterFactory() {
		super();
	}

	public AbstractRemotePersisterFactory(PersisterSet ps) {
		super(ps);
	}

	/**
	 * 刷新加载超时值（秒）
	 * 
	 * @param time 时间
	 */
	public void setRefreshTimeoutSecond(int time) {
		setRefreshTimeout(time * 1000);
	}

	/**
	 * 刷新加载超时值（毫秒）
	 * 
	 * @param time 时间
	 */
	public void setRefreshTimeout(int time) {
		m_RefreshTimeout = time * 1000;
	}

	/**
	 * VO过期时间（秒）
	 * 
	 * @param time 时间
	 */
	public void setExpirySecond(int time) {
		setExpiry(time * 1000);
	}

	/**
	 * VO过期时间（毫秒）
	 * 
	 * @param time 时间
	 */
	public void setExpiry(int time) {
		m_Expiry = time;
	}

	/**
	 * 是否共享加载器
	 * 
	 * @param share 是否共享
	 */
	public void setShareDelayLoader(boolean share) {
		m_ShareDelayLoader = share;
	}

	/**
	 * 离线工厂
	 * 
	 * @param factory 工厂
	 */
	public void setOfflineSupplierFactory(OfflineSupplierFactory factory) {
		m_OfflineSupplierFactory = factory;
	}

	@Override
	protected <E extends Persistent> Persister<E> doCreatePersister(Class<E> clazz, ObjectMapper<E> mapper) {
		if (!(AbstractRemotePersistent.class.isAssignableFrom(clazz))) {
			throw new UnsupportedOperationException("不支持的类:" + clazz);
		}
		Class<?> vo = ClassUtil.find(clazz, AbstractRemotePersistent.class, "V");
		ObjectMapper<?> vomapper;
		String type = m_MapperType;
		switch (type) {
		case MAPPER_FIELD:
			vomapper = FieldMapper.valueOf(vo, m_PersisterSet.getMappers());
			break;
		case MAPPER_METHOD:
			vomapper = MethodMapper.valueOf(vo, m_PersisterSet.getMappers());
			break;
		default:
			throw new UnsupportedOperationException("不支持的映射方式:" + type);
		}
		Persister<E> ps = doCreatePersister(clazz, mapper, vomapper);
		if (ps instanceof AbstractRemotePersister) {
			AbstractRemotePersister<?, ?> rp = (AbstractRemotePersister<?, ?>) ps;
			rp.setRefreshTimeout(m_RefreshTimeout);
			rp.setExpiry(m_Expiry);
			rp.setShareDelayLoader(m_ShareDelayLoader);
			if (null != m_OfflineSupplierFactory) {
				OfflineSupplier<?> offline = (OfflineSupplier<?>) m_OfflineSupplierFactory.createOfflineSupplier(vo);
				rp.setOffline(offline);
			}
		}
		return ps;
	}

	protected abstract <E extends Persistent> Persister<E> doCreatePersister(Class<E> clazz, ObjectMapper<E> mapper,
			ObjectMapper<?> vomapper);
}

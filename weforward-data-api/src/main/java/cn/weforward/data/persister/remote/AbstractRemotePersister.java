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

import cn.weforward.data.persister.ObjectWithVersion;
import cn.weforward.data.persister.OfflineSupplier;
import cn.weforward.data.persister.support.AbstractPersister;

/**
 * 远程的持久化对象
 * 
 * @author daibo
 *
 * @param <E> 持久对象
 * @param <V> 值对象
 */
public abstract class AbstractRemotePersister<E extends AbstractRemotePersistent<?, V>, V>
		extends AbstractPersister<E> {
	/** 脱机缓存 */
	protected OfflineSupplier<V> m_Offline;
	/** 刷新加载超时值（毫秒） */
	protected int m_RefreshTimeout = AbstractRemotePersisterFactory.DEFAULT_REFRESHTIMEOUT;
	/** VO过期时间（毫秒） */
	protected int m_Expiry = AbstractRemotePersisterFactory.DEFAULT_EXPIRT;
	/** 是否共享加载器 */
	protected boolean m_ShareDelayLoader = AbstractRemotePersisterFactory.DEFAULT_SHAREDELAYLOADER;

	protected AbstractRemotePersister(String name) {
		super(name);
	}

	/* 设置离线缓存 */
	@SuppressWarnings("unchecked")
	void setOffline(OfflineSupplier<?> offline) {
		m_Offline = (OfflineSupplier<V>) offline;
	}

	/* 更新离线缓存 */
	public void updateOffline(String id, V vo) {
		OfflineSupplier<V> offline = m_Offline;
		if (null != offline) {
			offline.update(id, vo);
		}
	}

	/**
	 * 刷新VO时加载超时值（毫秒）
	 * 
	 * @param mills 加载超时值（毫秒）
	 */
	public void setRefreshTimeout(int mills) {
		m_RefreshTimeout = mills;
	}

	/**
	 * VO过期时间
	 * 
	 * @param expiry 时间
	 */
	public void setExpiry(int expiry) {
		m_Expiry = expiry;
	}

	/**
	 * VO过期时间
	 * 
	 * @return 时间
	 */
	public int getExpiry() {
		return m_Expiry;
	}

	/**
	 * 是否共享加载器
	 * 
	 * @param shareDelayLoader 是否共享
	 */
	public void setShareDelayLoader(boolean shareDelayLoader) {
		m_ShareDelayLoader = shareDelayLoader;
	}

	/**
	 * 是否共享加载器
	 * 
	 * @return 是否共享
	 */
	public boolean isShareDelayLoader() {
		return m_ShareDelayLoader;
	}

	@Override
	protected ObjectWithVersion<E> innerLoad(String id) {
		OfflineSupplier<V> offline = m_Offline;
		if (null != offline) {
			ObjectWithVersion<V> vo = offline.get(id);
			if (null != vo) {
				// lucky
				E e = create(id, vo);
				return new ObjectWithVersion<>(e, vo.getVersion(), vo.getDriveIt());
			}
		}
		return remoteLoad(null, id, null);
	}

	@Override
	protected String innerSave(E object) {
		String v = remoteSave(object);
		updateOffline(object.getPersistenceId().getId(), object.acceptVo());
		return v;
	}

	@Override
	protected String innerNew(E object) {
		return remoteNew(object);
	}

	@Override
	protected boolean innerDelete(String id) {
		OfflineSupplier<V> offline = m_Offline;
		if (null != offline) {
			offline.remove(id);
		}
		return remoteDelete(id);
	}

	/**
	 * 从远程装入对象
	 * 
	 * @param e       对象
	 * @param id      对象ID
	 * @param version 当前对象的版本号,为null则表示未知,如果不为null且与远端版本一样可返回无修改提高性能
	 * @return 所加载的对象项及其版本号，没有则返回null
	 */
	protected ObjectWithVersion<E> remoteLoad(E e, String id, String version) {
		ObjectWithVersion<V> vo = remoteLoad(id, null);
		if (null == vo) {
			return null;// 木有对象
		}
		if (null == e) {
			e = create(id, vo);
		} else {
			e.updateVo(vo.getObject(), vo.getVersion());
		}
		return new ObjectWithVersion<>(e, vo.getVersion(), vo.getDriveIt());
	}

	/**
	 * 新增的对象（用于持久器在对象未刷写前能进行查询）到远端
	 * 
	 * @param object 对象
	 * @return 对象保存后版本号
	 */
	protected String remoteNew(E object) {
		return remoteNew(object.getPersistenceId().getOrdinal(), object.acceptVo());
	}

	/**
	 * 保存对象状态到远端
	 * 
	 * @param object 对象
	 * @return 对象保存后版本号
	 */
	protected String remoteSave(E object) {
		return remoteSave(object.getPersistenceId().getOrdinal(), object.acceptVo());
	}

	/**
	 * 创建对象
	 * 
	 * @param id 对象id
	 * @param vo 对象VO
	 * @return 对象
	 */
	protected abstract E create(String id, ObjectWithVersion<V> vo);

	/**
	 * 从远程装入对象
	 * 
	 * @param id      对象ID
	 * @param version 当前对象的版本号,为null则表示未知,如果不为null且与远端版本一样可返回无修改提高性能
	 * @return 所加载的对象项及其版本号，没有则返回null
	 */
	protected abstract ObjectWithVersion<V> remoteLoad(String id, String version);

	/**
	 * 新增的对象（用于持久器在对象未刷写前能进行查询）到远端
	 * 
	 * @param id     对象id
	 * @param object 对象值vo
	 * @return 对象保存后版本号
	 */
	protected abstract String remoteNew(String id, V object);

	/**
	 * 保存对象状态到远端
	 * 
	 * @param id     对象id
	 * @param object 对象值vo
	 * @return 对象保存后版本号
	 */
	protected abstract String remoteSave(String id, V object);

	/**
	 * 从远端删除对象
	 * 
	 * @param id 对象ID
	 * @return 成功/失败
	 */
	protected abstract boolean remoteDelete(String id);

}

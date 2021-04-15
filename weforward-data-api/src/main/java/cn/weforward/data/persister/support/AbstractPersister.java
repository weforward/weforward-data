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
package cn.weforward.data.persister.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.ResultPage;
import cn.weforward.common.sys.GcCleaner;
import cn.weforward.common.sys.IdGenerator;
import cn.weforward.common.util.FreezedList;
import cn.weforward.common.util.LruCache;
import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.TransIterator;
import cn.weforward.common.util.TransResultPage;
import cn.weforward.data.UniteId;
import cn.weforward.data.exception.IdDuplicateException;
import cn.weforward.data.persister.ChangeListener;
import cn.weforward.data.persister.Condition;
import cn.weforward.data.persister.ObjectWithVersion;
import cn.weforward.data.persister.OrderBy;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.PersistentListener;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.util.DelayFlusher;
import cn.weforward.data.util.Flusher;

/**
 * 支持缓存的抽象持久器
 * 
 * @author daibo
 * 
 * @param <E>
 *            持久对象子类
 */
public abstract class AbstractPersister<E extends Persistent> implements Persister<E> {
	/** 日志记录器 */
	protected final static Logger _Logger = LoggerFactory.getLogger(AbstractPersister.class);
	/** 持久器名 */
	protected String m_Name;
	/** 缓存 */
	protected Cache m_Cache;
	/** 是否开始重载接口 */
	protected boolean m_ReloadEnabled = false;
	/** 是否只处理当前服务器持久类 */
	protected boolean m_ForOwnerEnabled = true;
	/** 检查新ID有重复时的重试次数（不大于0表示不检查） */
	protected int m_VerifyNewIdTrys;
	/** 刷写器 */
	protected Flusher m_Flusher;
	/** id生成器 */
	protected IdGenerator.Tick m_IdGenerator;
	/** 监听器 */
	protected List<ChangeListener<E>> m_Listeners = Collections.emptyList();
	/** 加载器 */
	LruCache.Loader<String, E> m_Loader = new LruCache.Loader<String, E>() {
		@Override
		public E load(String key, LruCache.CacheNode<String, E> node) {
			ObjectWithVersion<E> ov = innerLoad(key);
			if (null == ov) {
				return null;
			}
			if (ov.getObject() instanceof PersistentListener) {
				// 调用持久对象反射后事件
				PersistentListener listener = (PersistentListener) ov.getObject();
				listener.onAfterReflect(AbstractPersister.this,
						UniteId.valueOf(key, getName(), null), ov.getVersion(), ov.getDriveIt());
			}
			((Cache.PersistNode) node).setVersion(ov.getVersion());
			return ov.getObject();
		}
	};

	protected AbstractPersister(String name) {
		m_Name = name;
		m_Cache = new Cache(name);
		GcCleaner.register(m_Cache);
	}

	@Override
	public String getName() {
		return m_Name;
	}

	/**
	 * 检查新ID有重复时的重试次数（不大于0表示不检查）
	 * 
	 * @return 重试次数
	 */
	public int getVerifyNewIdTrys() {
		return m_VerifyNewIdTrys;
	}

	/**
	 * 检查新ID有重复时的重试次数（不大于0表示不检查）
	 * 
	 * @param verifyNewIdTrys
	 *            重试次数
	 */
	public void setVerifyNewIdTrys(int verifyNewIdTrys) {
		m_VerifyNewIdTrys = verifyNewIdTrys;
	}

	/**
	 * 是否控制对象单例
	 * 
	 * @param enabled
	 *            true/单例，false/非单例
	 */
	public void setReachable(boolean enabled) {
		getCache().setReachable(enabled);
	}

	public boolean isReloadEnabled() {
		return m_ReloadEnabled;
	}

	@Override
	public boolean setReloadEnabled(boolean enabled) {
		m_ReloadEnabled = enabled;
		return m_ReloadEnabled;
	}

	@Override
	public synchronized void addListener(ChangeListener<E> l) {
		if (null == l) {
			return;
		}
		List<ChangeListener<E>> olds = m_Listeners;
		if (olds.contains(l)) {
			return;
		}
		m_Listeners = FreezedList.addToFreezed(olds, olds.size(), l);
	}

	@Override
	public synchronized void removeListener(ChangeListener<E> l) {
		List<ChangeListener<E>> olds = m_Listeners;
		List<ChangeListener<E>> news = new ArrayList<>();
		for (ChangeListener<E> cl : olds) {
			if (cl.equals(l)) {
				continue;
			}
			news.add(cl);
		}
		if (news.size() == olds.size()) {
			return;
		}
		m_Listeners = FreezedList.freezed(news);
	}

	@Override
	public boolean isForOwnerEnabled() {
		return m_ForOwnerEnabled;
	}

	@Override
	public boolean setForOwnerEnabled(boolean enabled) {
		m_ForOwnerEnabled = enabled;
		return m_ForOwnerEnabled;
	}

	/**
	 * 为持久器产生的对象ID加上标识，标识的范围为0x01~0xFF
	 * 
	 * @param id
	 *            服务器标识
	 */
	public void setPersisterId(String id) {
		m_IdGenerator = new IdGenerator.Tick(id);
	}

	public String getPersisterId() {
		return getTick().getServerId();
	}

	private IdGenerator.Tick getTick() {
		if (null == m_IdGenerator) {
			m_IdGenerator = new IdGenerator.Tick("");
		}
		return m_IdGenerator;
	}

	public void setFlusher(Flusher flusher) {
		m_Flusher = flusher;
	}

	public Flusher getFlusher() {
		if (null == m_Flusher) {
			DelayFlusher f = new DelayFlusher();
			f.setName("flusher-" + toString());
			m_Flusher = f;
		}
		return m_Flusher;
	}

	@Override
	public ResultPage<E> startsWith(String prefix) {
		ResultPage<String> ids = startsWithOfId(prefix);
		return new TransResultPage<E, String>(ids) {

			@Override
			protected E trans(String src) {
				return get(src);
			}
		};
	}

	@Override
	public ResultPage<E> search(Date begin, Date end) {
		ResultPage<String> ids = searchOfId(begin, end);
		return new TransResultPage<E, String>(ids) {

			@Override
			protected E trans(String src) {
				return get(src);
			}
		};
	}

	@Override
	public ResultPage<E> searchRange(String from, String to) {
		ResultPage<String> ids = searchRangeOfId(from, to);
		return new TransResultPage<E, String>(ids) {

			@Override
			protected E trans(String src) {
				return get(src);
			}
		};
	}

	@Override
	public Iterator<E> search(String serverId, Date begin, Date end) {
		Iterator<String> ids = searchOfId(serverId, begin, end);
		return new TransIterator<E, String>(ids) {

			@Override
			protected E trans(String src) {
				return get(src);
			}
		};
	}

	@Override
	public Iterator<E> searchRange(String serverId, String from, String to) {
		Iterator<String> ids = searchRangeOfId(serverId, from, to);
		return new TransIterator<E, String>(ids) {

			@Override
			protected E trans(String src) {
				return get(src);
			}
		};
	}

	/**
	 * 装入对象
	 * 
	 * @param id
	 *            对象ID
	 * @return 所加载的对象项及其版本号，没有则返回null
	 */
	abstract protected ObjectWithVersion<E> innerLoad(String id);

	/**
	 * 保存对象状态
	 * 
	 * @param object
	 *            对象
	 * @return 对象保存后版本号
	 */
	abstract protected String innerSave(E object);

	/**
	 * 新增的对象（用于持久器在对象未刷写前能进行查询）
	 * 
	 * @param object
	 *            新增的对象
	 * @return 创建后的版本号
	 */
	abstract protected String innerNew(E object);

	/**
	 * 删除对象
	 * 
	 * @param id
	 *            对象ID
	 * @return 成功/失败
	 */
	abstract protected boolean innerDelete(String id);

	/**
	 * 保存对象状态
	 * 
	 * @param object
	 *            对象
	 * @param oldVersion
	 *            持久化前的版本号
	 * @return 对象保存后版本号
	 */
	protected String innerSave(E object, String oldVersion) {
		return innerSave(object);
	}

	@Override
	public boolean isOwner(E obj) {
		if (null == obj) {
			return false;
		}
		UniteId id = obj.getPersistenceId();
		if (null == id) {
			return false;
		}
		return StringUtil.eq(IdGenerator.getServerId(id.getOrdinal()), getPersisterId());
	}

	@Override
	public final UniteId getNewId() throws IdDuplicateException {
		return getNewId(null);
	}

	@Override
	public UniteId getNewId(String prefix) throws IdDuplicateException {
		UniteId uuid;
		uuid = UniteId.valueOf(getTick().genId(prefix), getName(), null);
		if (m_VerifyNewIdTrys <= 0) {
			// 不校验
			return uuid;
		}
		for (int i = 0; i < m_VerifyNewIdTrys; i++) {
			if (null == get(uuid)) {
				// 没有重复 :)
				return uuid;
			}
		}
		// 尝试N次也重复，出错
		throw new IdDuplicateException(
				"[" + getName() + "]" + m_VerifyNewIdTrys + "次尝试生成的ID都重复" + m_IdGenerator);
	}

	/**
	 * 由缓存取得
	 * 
	 * @param id
	 *            对象id
	 * @return 对象
	 */
	public E getOfCache(String id) {
		return m_Cache.get(UniteId.getOrdinal(id));
	}

	public boolean hold(E object) {
		UniteId unid = object.getPersistenceId();
		if (UniteId.isEmtpy(unid)) {
			// 不允许置入空ID的对象
			throw new IllegalArgumentException("Object persistence ID is null");
		}
		String type = getName();
		if (null != type && type.length() > 0 && !type.equals(unid.getType())) {
			// 不是持久器支持的对象类型
			_Logger.warn(unid + " isn't a " + getName());
		}
		return (m_Cache.putIfAbsent(unid.getOrdinal(), object) == object);
	}

	/**
	 * 置入缓存
	 * 
	 * @param object
	 *            要进入缓存的对象实例
	 * @return 在缓冲被替换掉的对象实例
	 */
	public E putOfCache(E object) {
		UniteId unid = object.getPersistenceId();
		if (UniteId.isEmtpy(unid)) {
			// 不允许置入空ID的对象
			throw new IllegalArgumentException("Object persistence ID is null");
		}
		String type = getName();
		if (null != type && type.length() > 0 && !type.equals(unid.getType())) {
			// 不是持久器支持的对象类型
			throw new IllegalArgumentException(unid + " isn't a " + getName());
		}
		return m_Cache.put(unid.getOrdinal(), object);
	}

	/**
	 * 只由缓存中删除
	 * 
	 * @param id
	 *            缓存项标识
	 * @return 有则删除且返回删除的项
	 */
	public E removeOfCache(String id) {
		return m_Cache.remove(id);
	}

	public void cleanup() {
		// TODO
	}

	public void flush(E object) {
		if (null == object) {
			throw new NullPointerException("param 'object' is null");
		}
		m_Cache.flush(object);
	}

	public E get(String id) {
		if (null == id || id.length() == 0) {
			return null;
		}
		// 修正联合ID
		return get(UniteId.fixId(id, getName()));
	}

	public E get(UniteId id) {
		if (null == id) {
			return null;
		}
		String ordinal = id.getOrdinal();
		String type = getName();
		if (null != type && type.length() > 0 && !type.equals(id.getType())) {
			// 不是持久器支持的对象类型
			if (_Logger.isWarnEnabled()) {
				// 对象ID表示其不属于当前持久器
				_Logger.warn(id + " isn't a " + getName());
			}
			return null;
		}
		E p;
		if (m_Cache.getNullTimeout() > 0) {
			p = m_Cache.getHintLoad(ordinal, m_Loader);
		} else {
			p = m_Cache.getAndLoad(ordinal, m_Loader, 0);
		}
		return p;
	}

	public boolean remove(String id) {
		if (null == id || id.length() == 0) {
			return false;
		}
		// 修正联合ID
		return remove(UniteId.fixId(id, getName()));
	}

	public boolean remove(UniteId id) {
		String ordinal = id.getOrdinal();
		// 先由缓存删除
		m_Cache.remove(ordinal);
		// 由实际存储删除
		boolean ret = innerDelete(ordinal);
		return ret;
	}

	public void update(E object) {
		if (null == object) {
			throw new NullPointerException("Object is null");
		}

		UniteId unid = object.getPersistenceId();
		if (UniteId.isEmtpy(unid)) {
			// 不允许置入空ID的对象
			throw new IllegalArgumentException("persistence ID is null");
		}
		String type = getName();
		if (null != type && type.length() > 0 && !type.equals(unid.getType())) {
			// 不是持久器支持的对象类型
			throw new IllegalArgumentException(unid + " isn't a " + getName());
		}

		// 先置入缓存
		Cache.PersistNode node = m_Cache.updating(object);
		Flusher f = getFlusher();
		if (null != node && null != f) {
			// 标记到刷写队列
			f.mark(node);
			return;
		}

		// 不能进入更新列表的，只好立刻执行持久化
		String version = persist(object, null != node ? node.getVersion() : null);
		m_Cache.updateVersion(unid.getOrdinal(), object, version);
	}

	@Override
	public void persist(E object) {
		UniteId unid = object.getPersistenceId();
		String version = persist(object, null != object ? getVersion(unid) : null);
		m_Cache.updateVersion(unid.getOrdinal(), object, version);
	}

	public PersistentCache<E> getCache() {
		return m_Cache;
	}

	protected String persist(E object, String oldVersion) {
		PersistentListener listener = null;
		if (object instanceof PersistentListener) {
			// 调用对象持久前事件
			listener = (PersistentListener) object;
			listener.onBeforePersistence(this);
		}
		String version;
		synchronized (object) {
			version = innerSave(object, oldVersion);
		}
		if (null != listener) {
			// 调用对象持久后事件
			listener.onAfterPersistence(this, version);
		}
		return version;
	}

	public boolean isDirty(E instance) {
		// 检查是否待更新
		return m_Cache.isDirty(instance.getPersistenceId().getOrdinal());
	}

	@Override
	public ResultPage<E> search(Condition condition) {
		return search(condition, null);
	}

	@Override
	public ResultPage<String> searchOfId(Condition condition) {
		return searchOfId(condition, null);
	}

	@Override
	public ResultPage<E> search(Condition condition, OrderBy orderBy) {
		ResultPage<String> ids = searchOfId(condition, orderBy);
		return new TransResultPage<E, String>(ids) {

			@Override
			protected E trans(String src) {
				return get(src);
			}
		};
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof String) {
			return obj.equals(getName());
		}
		return false;
	}

	@Override
	public String getVersion(UniteId id) {
		// E object = get(id);
		// if (null == object) {
		// return null;
		// }
		return m_Cache.getVersion(id.getOrdinal());
	}

	/**
	 * 持久器的ID生成器
	 * 
	 * @return 生成器
	 */
	public IdGenerator getIdGenerator() {
		return m_IdGenerator;
	}

	/**
	 * 缓存
	 * 
	 * @author liangyi
	 */
	protected class Cache extends PersistentCache<E> {

		protected Cache(String name) {
			super(name);
		}

		// @Override
		// protected void afterNodeLoad(CacheNode<String, E> node) {
		// super.afterNodeLoad(node);
		// // FIXME 若要保证在缓存取出就必须初始化好，得在Loader.load处理
		// E obj = node.getValue();
		// if (obj instanceof PersistentListener) {
		// // 调用持久对象反射后事件
		// PersistentListener listener = (PersistentListener) obj;
		// listener.onAfterReflect(AbstractPersister.this,
		// UniteId.valueOf(node.getKey(), getName(), null),
		// ((PersistNode) node).getVersion());
		// }
		// }

		@Override
		protected String persist(E object, PersistNode node) {
			String oldVersion = node.getVersion();
			// 先把状态置于更新中
			node.updating();
			String version = AbstractPersister.this.persist(object, oldVersion);
			node.clean(version);
			return version;
		}

		@Override
		protected String newer(E object) {
			return innerNew(object);
		}
	}
}

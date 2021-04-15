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

import java.io.IOException;

import cn.weforward.common.util.LruCache;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.util.Flushable;

/**
 * 用于支撑持久化对象的缓冲
 * 
 * @author liangyi
 *
 * @param <E>
 *            可持久化对象
 */
public abstract class PersistentCache<E extends Persistent> extends LruCache<String, E> {
	/** 标示正处理更新中，这种状态下的版本号可认为是相同的 */
	public static final String VERSION_UPDATING = "...";

	/**
	 * 
	 * @author liangyi
	 */
	protected class PersistNode extends CacheNode<String, E> implements Flushable {
		protected String version;

		public PersistNode(int hash, String key, E value, Node<String, E> next) {
			super(hash, key, value, next);
		}

		public void clean(String ver) {
			this.version = ver;
			super.clean();
		}

		public String getVersion() {
			return this.version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		@Override
		public void flush() throws IOException {
			E v = getValue();
			if (!isDirty()) {
				if (_Logger.isTraceEnabled()) {
					_Logger.trace("unchanged:" + this);
				}
				return;
			}
			persist(v, this);
			// PersistentCache.this.putLru(this);
		}

		/**
		 * 标记为更新中
		 */
		public void updating() {
			this.version = VERSION_UPDATING;
		}

		@Override
		public String toString() {
			int tick = _clock.getTicker();
			return "{id:" + key + ",t:" + (tick - lastAccess) + ",r:" + (tick - lastReady) + ",ver:"
					+ this.version + ",obj:" + value + '}';
		}
	}

	public PersistentCache(String name) {
		super(name);
		// 启用弱引用检查缓存对象来保持单例
		setReachable(true);
		// 1秒的空项重加载间隔
		setNullTimeout(1);
		// 缓存项空闲时间15分钟
		setTimeout(15 * 60);
	}

	public int getNullTimeout() {
		return m_NullTimeout;
	}

	/**
	 * 把对象持久化
	 * 
	 * @param object
	 *            要持久化的对象
	 * @param node
	 *            对应的缓存节点
	 * @return 持久化后的版本号
	 */
	protected abstract String persist(E object, PersistNode node);

	/**
	 * 标记（可能是）新的持久化对象（用于对象未刷写前能进行查询）
	 * 
	 * @param object
	 *            （可能是）新的持久化对象
	 * @return 持久化后的版本号
	 */
	protected abstract String newer(E object);

	@Override
	protected PersistNode openNode(int hash, String key) {
		return (PersistNode) super.openNode(hash, key);
	}

	@Override
	protected PersistNode getNode(int hash, Object key) {
		return (PersistNode) super.getNode(hash, key);
	}

	@Override
	protected PersistNode newNode(int hash, String key, E value, Node<String, E> next) {
		return new PersistNode(hash, key, value, next);
	}

	public boolean isDirty(String ordinal) {
		CacheNode<String, E> node = getNode(hash(ordinal), ordinal);
		return null != node && node.isDirty();
	}

	public String getVersion(String ordinal) {
		PersistNode node = getNode(hash(ordinal), ordinal);
		return (null != node) ? node.getVersion() : null;
	}

	/**
	 * 更新若缓存项的版本号
	 * 
	 * @param ordinal
	 *            缓存项标识
	 * @param value
	 *            对象
	 * @param version
	 *            要更新到的版本号
	 * @return 旧版本号
	 */
	public String updateVersion(String ordinal, E value, String version) {
		if (null == version || version.length() == 0) {
			// 没版本号
			return null;
		}
		PersistNode node = getNode(hash(ordinal), ordinal);
		String oldVersion = null;
		if (null != node) {
			oldVersion = node.getVersion();
			synchronized (node) {
				if (node.getValue() == value) {
					node.clean(version);
				}
			}
		}
		return oldVersion;
	}

	/**
	 * 把状态变化对象置入缓存
	 * 
	 * @param object
	 *            状态变化的对象
	 * @return 缓存节点
	 */
	protected PersistNode updating(E object) {
		String key = object.getPersistenceId().getOrdinal();
		E old;
		PersistNode node = openNode(hash(key), key);
		old = node.setValue(object);
		afterNodeUpdate(node);
		boolean isMaybeNew;
		isMaybeNew = (null == old);
		if (isMaybeNew) {
			// 也许是新对象
			try {
				String ver = newer(object);
				// node.clean(ver);
				node.setVersion(ver);
				if (_Logger.isTraceEnabled()) {
					_Logger.trace("newer[" + ver + "]: " + object);
				}
			} catch (Throwable ex) {
				// 忽略这步的错误
				_Logger.error(ex.getMessage(), ex);
			}
		}
		return node;
	}

	@Override
	protected void afterNodeUpdate(Node<String, E> p) {
		// 独立flush，不进入更新链表批量更新，更新LRU即可
		putLru(p);
	}

	@Override
	protected void afterNodeRemoval(Node<String, E> p) {
		E e = p.getValue();
		super.afterNodeRemoval(p);
		if (e instanceof AbstractPersistent<?>) {
			((AbstractPersistent<?>) e).enablDelete();
		}
	}

	protected PersistNode flush(E object) {
		String key = object.getPersistenceId().getOrdinal();
		E old;
		PersistNode node = openNode(hash(key), key);
		old = node.setValue(object);
		afterNodeUpdate(node);

		if (!node.isDirty()) {
			if (_Logger.isWarnEnabled()) {
				_Logger.warn("Ignore flush operation,object not changed [" + key + "]" + object);
			}
			return node;
		}
		if (old != object && _Logger.isWarnEnabled()) {
			_Logger.warn("Flush/Replace instance on cache [" + key + "]" + old + " => " + object);
		}
		// 持久化
		persist(object, node);
		return node;
	}
}
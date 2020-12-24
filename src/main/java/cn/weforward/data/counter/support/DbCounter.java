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
package cn.weforward.data.counter.support;

import java.util.Iterator;
import java.util.NoSuchElementException;

import cn.weforward.common.GcCleanable;
import cn.weforward.common.util.LruCache;
import cn.weforward.common.util.LruCache.CacheNode;
import cn.weforward.common.util.SinglyLinkedLifo;
import cn.weforward.data.counter.Counter;
import cn.weforward.data.util.Flushable;

/**
 * 暂存内存且持久化于数据库的计数器基类
 * 
 * @author liangyi
 *
 */
public abstract class DbCounter extends AbstractCounter implements Counter, GcCleanable {
	protected Cache m_Cache;
	protected LruCache.Loader<String, CounterItem> m_Loader;
	protected DbCounterFactory m_Factory;

	public DbCounter(String name, DbCounterFactory factory) {
		super(name);
		m_Factory = factory;
		m_Cache = new Cache(name);
		m_Loader = new LruCache.Loader<String, CounterItem>() {
			@Override
			public CounterItem load(String key, CacheNode<String, CounterItem> node) {
				CounterItem item;
				item = m_Factory.doLoad(DbCounter.this, key);
				return item;
			}
		};
	}

	public String getLableName() {
		return getName() + "_cnt";
	}

	@Override
	public boolean remove(String id) {
		CounterItem item = m_Cache.getHintLoad(id, m_Loader);
		if (null == item) {
			return false;
		}
		// 置零
		synchronized (item) {
			if (0 == item.getTotal()) {
				return false;
			}
			item.set(0);
		}
		m_Cache.markUpdated(id);
		return true;
	}

	@Override
	public void removeAll() {
		m_Cache.clear();
	}

	@Override
	public long get(String id) {
		CounterItem item = m_Cache.getHintLoad(id, m_Loader);
		return (null == item) ? 0 : item.getTotal();
	}

	@Override
	public long inc(String id, int step) {
		Adder adder = new Adder(step);
		m_Cache.update(id, adder);
		return adder.value;
	}

	@Override
	public long set(String id, long value) {
		Setter setter = new Setter(value);
		m_Cache.update(id, setter);
		return value;
	}

	@Override
	public boolean compareAndSet(String id, long expect, long value) {
		CounterItem item = m_Cache.getHintLoad(id, m_Loader);
		if (null == item) {
			if (0 == expect) {
				// 还没有的计数项当期望为0时，置值
				set(id, value);
				return true;
			}
			return false;
		}
		synchronized (item) {
			if (item.getTotal() != expect) {
				return false;
			}
			item.set(value);
		}
		m_Cache.markUpdated(id);
		return true;
	}

	@Override
	public void onGcCleanup(int policy) {
		m_Cache.onGcCleanup(policy);
	}

	public Cache getCache() {
		return m_Cache;
	}

	public void setNullTimeout(int seconds) {
		m_Cache.setNullTimeout(seconds);
	}

	@Override
	public String toString() {
		return m_Cache.toString();
	}

	/**
	 * 加/减法
	 * 
	 * @author liangyi
	 *
	 */
	class Adder implements LruCache.Updater<String, CounterItem> {
		int step;
		long value;

		public Adder(int step) {
			this.step = step;
		}

		@Override
		public CounterItem update(String key, CounterItem current) {
			if (null == current) {
				// 先加载
				current = m_Loader.load(key, null);
				if (null == current) {
					current = new CounterItem(key);
					value = current.addAndGet(step);
					m_Factory.doNew(DbCounter.this, current);
					return current;
				}
			}
			value = current.addAndGet(step);
			return current;
		}

		public int getIntValue() {
			return CounterItem.long2int(value);
		}
	}

	/**
	 * 指定值
	 * 
	 * @author liangyi
	 *
	 */
	class Setter implements LruCache.Updater<String, CounterItem> {
		long value;

		public Setter(long value) {
			this.value = value;
		}

		@Override
		public CounterItem update(String key, CounterItem current) {
			if (null == current) {
				// 先加载
				current = m_Loader.load(key, null);
				if (null == current) {
					current = new CounterItem(key);
					current.set(value);
					m_Factory.doNew(DbCounter.this, current);
					return current;
				}
			}
			current.set(value);
			return current;
		}
	}

	/**
	 * 待保存的计数项
	 * 
	 * @author liangyi
	 *
	 */
	public interface DirtyData extends Iterator<CounterItem> {
		/**
		 * 在遍历待保存项前执行其开启rollback/commit支持
		 */
		void begin();

		/**
		 * 若使用begin开始，则当存储到数据库完成执行其
		 */
		void commit();

		/**
		 * 当存储到数据库出错时回滚则调用其把计数项标记回刷写列表，必须在开始时执行了begin
		 */
		void rollback();

		/**
		 * 当存储到数据库只有前面的部分成功时调用其把余下项标记回刷写列表，不需要开始时执行begin
		 */
		void abort();
	}

	/**
	 * 计数项缓存器
	 * 
	 * @author liangyi
	 *
	 */
	public class Cache extends LruCache<String, CounterItem> implements Flushable {
		public Cache(String name) {
			super(name);
			setNullTimeout(m_Factory.m_NullTimeout);
		}

		/**
		 * 复制模式下
		 */
		class CopyOn extends SinglyLinkedLifo<CacheNode<String, CounterItem>> implements DirtyData {
			SinglyLinkedLifo.Node<CacheNode<String, CounterItem>> m_Next;

			CopyOn() {
				CacheNode<String, CounterItem> p, next;
				synchronized (updatedLock()) {
					p = m_UpdatedChain;
					while (null != p) {
						next = p.getUpdatedNext();
						if (p.isDirty()) {
							addHead(p);
						}
						p.clean();
						p = next;
					}
					m_UpdatedChain = null;
				}
			}

			public void reset() {
				m_Next = getHead();
			}

			@Override
			public boolean hasNext() {
				return null != m_Next;
			}

			@Override
			public CounterItem next() {
				return nextNode().getValueFast();
			}

			@Override
			public void begin() {
				// 啥也不用做
			}

			@Override
			public void commit() {
				// 未遍历的部分标记回列表
				abort();
			}

			@Override
			public void rollback() {
				// 全部标记回
				reset();
				abort();
			}

			@Override
			public void abort() {
				// 把后面的标记回待刷写列表
				while (hasNext()) {
					afterNodeUpdate(nextNode());
				}
			}

			private CacheNode<String, CounterItem> nextNode() {
				Node<CacheNode<String, CounterItem>> p = m_Next;
				if (null == p) {
					throw new NoSuchElementException("没有啦");
				}
				m_Next = p.getNext();
				return p.value;
			}
		}

		/**
		 * 直接模式
		 */
		class Direct implements DirtyData {
			CacheNode<String, CounterItem> m_First, m_Next;

			Direct(CacheNode<String, CounterItem> node) {
				m_First = m_Next = node;
			}

			@Override
			public boolean hasNext() {
				return null != m_Next;
			}

			@Override
			public CounterItem next() {
				return nextNode().getValueFast();
			}

			@Override
			public void begin() {
				// 啥也不用做
			}

			@Override
			public void rollback() {
				m_Next = m_First;
			}

			@Override
			public void abort() {
				clean();
			}

			@Override
			public void commit() {
				m_Next = null;
				clean();
			}

			/*
			 * 把first->next的项标记为已存储
			 */
			private void clean() {
				CacheNode<String, CounterItem> p, next = m_First;
				while (null != next && next != m_Next) {
					p = next;
					next = p.getUpdatedNext();
					p.clean();
				}
				m_First = null;
				m_Next = null;
			}

			private CacheNode<String, CounterItem> nextNode() {
				CacheNode<String, CounterItem> p = m_Next;
				if (null == p) {
					throw new NoSuchElementException("没有啦");
				}
				m_Next = p.getUpdatedNext();
				return p;
			}
		}

		public void flush() {
			if (m_Factory.isCopyOnFlush()) {
				// 复制待更新表后（不阻塞）存储
				CopyOn dirtyData = new CopyOn();
				m_Factory.doUpdate(DbCounter.this, dirtyData);
			} else {
				// 加锁阻塞存储
				synchronized (updatedLock()) {
					Direct dirtyData = new Direct(m_UpdatedChain);
					m_Factory.doUpdate(DbCounter.this, dirtyData);
					m_UpdatedChain = null;
				}
			}
			// 若刷写完后还有变化，只好再标记要刷写
			CacheNode<String, CounterItem> p = m_UpdatedChain;
			if (null != p && p.isDirty()) {
				m_Factory.getFlusher().mark(this);
			}
		}

		@Override
		protected void afterNodeUpdate(Node<String, CounterItem> p) {
			super.afterNodeUpdate(p);
			if (null == ((CacheNode<String, CounterItem>) p).getUpdatedNext()) {
				// 这是更新表首项，需要标记刷写
				m_Factory.getFlusher().mark(this);
			}
		}
	}
}

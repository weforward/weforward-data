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
package cn.weforward.data.counter.label;

import cn.weforward.common.GcCleanable;
import cn.weforward.common.ResultPage;
import cn.weforward.common.util.LruCache;
import cn.weforward.common.util.LruCache.CacheNode;
import cn.weforward.common.util.SinglyLinkedLifo;
import cn.weforward.common.util.TransResultPage;
import cn.weforward.data.array.Label;
import cn.weforward.data.counter.Counter;
import cn.weforward.data.counter.support.AbstractCounter;
import cn.weforward.data.util.Flushable;

/**
 * 暂存内存且持久化于labelset的计数器
 * 
 * @author liangyi
 *
 */
public class LabelCounter extends AbstractCounter implements Counter, GcCleanable {
	Cache m_Cache;
	LruCache.Loader<String, CounterItem> m_Loader;
	Label<CounterItem> m_Label;
	LabelCounterFactory m_Factory;

	public LabelCounter(LabelCounterFactory factory, Label<CounterItem> label, String name) {
		super(name);
		m_Factory = factory;
		m_Label = label;
		m_Cache = new Cache(name);
		m_Loader = new LruCache.Loader<String, CounterItem>() {
			@Override
			public CounterItem load(String key, CacheNode<String, CounterItem> node) {
				CounterItem item = m_Factory.getItem(LabelCounter.this, key);
				return item;
			}
		};
	}

	@Override
	public ResultPage<String> searchRange(String first, String last) {
		return wrap(m_Label.searchRange(first, last));
	}

	@Override
	public ResultPage<String> startsWith(String prefix) {
		return wrap(m_Label.startsWith(prefix));
	}

	private ResultPage<String> wrap(ResultPage<CounterItem> rp) {
		return new TransResultPage<String, CounterItem>(rp) {
			@Override
			protected String trans(CounterItem src) {
				return src.getIdForLabel();
			}
		};
	}

	@Override
	public boolean remove(String id) {
		// // 同时在label及缓存中删除
		// if (null == m_Label.remove(id) && null == m_Cache.remove(id)) {
		// return false;
		// }
		// return true;

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
		m_Label.removeAll();
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(64);
		builder.append(m_Cache).append(m_Label);
		return builder.toString();
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
				}
			}
			current.set(value);
			return current;
		}
	}

	/**
	 * 缓存
	 * 
	 * @author liangyi
	 *
	 */
	public class Cache extends LruCache<String, CounterItem> implements Flushable {
		public Cache(String name) {
			super(name);
		}

		/**
		 * 先复制更新表，再逐项写
		 */
		private void copyOnFlush() {
			// 先把更新表复制到一个链表
			SinglyLinkedLifo<CounterItem> updating = new SinglyLinkedLifo<CounterItem>();
			CacheNode<String, CounterItem> p, next;
			synchronized (updatedLock()) {
				p = m_UpdatedChain;
				while (null != p) {
					next = p.getUpdatedNext();
					if (p.isDirty()) {
						updating.addHead(p.getValueFast());
					}
					p.clean();
					p = next;
				}
				m_UpdatedChain = null;
			}
			// 再逐项写
			SinglyLinkedLifo.Node<CounterItem> it = updating.detach();
			while (null != it) {
				m_Label.put(it.value, Label.OPTION_FORCE);
				it = it.next;
			}

			// 若刷写完后还有变化，只好再标记要刷写
			p = m_UpdatedChain;
			if (null != p && p.isDirty()) {
				m_Factory.getFlusher().mark(this);
			}
		}

		public void flush() {
			if (m_Factory.isCopyOnFlush()) {
				copyOnFlush();
				return;
			}

			CacheNode<String, CounterItem> p, next;
			synchronized (updatedLock()) {
				p = m_UpdatedChain;
				while (null != p) {
					next = p.getUpdatedNext();
					if (p.isDirty()) {
						m_Label.put(p.getValueFast(), Label.OPTION_FORCE);
					}
					p.clean();
					p = next;
				}
				m_UpdatedChain = null;
			}
			// 若刷写完后还有变化，只好再标记要刷写
			p = m_UpdatedChain;
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

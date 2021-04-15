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

import cn.weforward.common.GcCleanable;
import cn.weforward.common.sys.GcCleaner;
import cn.weforward.common.util.LruCache.DirtyData;
import cn.weforward.data.counter.Counter;
import cn.weforward.data.counter.CounterFactory;
import cn.weforward.data.util.Flusher;

/**
 * 存储于数据库的计数器实现
 * 
 * 以“计数器名_cnt”组织，多个服务器存储于同一记录下的不同字段，其相加值则为总计数值，由此支持最终一致的分布计数器
 * 分布存储值的各字段名为：“v_服务器标识”
 * 
 * @author liangyi
 *
 */
public abstract class DbCounterFactory extends AbstractCounterFactory
		implements CounterFactory, GcCleanable {
	protected String m_ServerId;
	protected Flusher m_Flusher;
	protected boolean m_CopyOnFlush;
	/** NULL值重新加载间隔（秒） */
	protected int m_NullTimeout;
	/** 每次批量刷写的项数 */
	protected int m_MaxBatch;
	/** 计数器项重新加载间隔（秒） */
	protected int m_Expire;

	public DbCounterFactory(String serverId) {
		m_ServerId = serverId;
		m_MaxBatch = 1000;
		setNullTimeout(2);
		setExpire(5);
		GcCleaner.register(this);
	}

	public void setFlusher(Flusher flusher) {
		m_Flusher = flusher;
	}

	public Flusher getFlusher() {
		return m_Flusher;
	}

	public String getServerId() {
		return m_ServerId;
	}

	/**
	 * 开启/关闭刷写时先复制待更新列表，用于在LabelSet.put慢速或直接写入的情况下使用
	 * 
	 * @param enabled
	 *            是否开启
	 */
	public void setCopyOnFlush(boolean enabled) {
		m_CopyOnFlush = enabled;
	}

	public boolean isCopyOnFlush() {
		return m_CopyOnFlush;
	}

	public void setNullTimeout(int seconds) {
		m_NullTimeout = seconds;
		for (Counter c : this) {
			if (c instanceof DbCounter) {
				((DbCounter) c).setNullTimeout(seconds);
			}
		}
	}

	public void setMaxBatch(int limit) {
		m_MaxBatch = limit;
	}

	public int getMaxBatch() {
		return m_MaxBatch;
	}

	public void setExpire(int expire) {
		m_Expire = expire;
	}

	public int getExpire() {
		return m_Expire;
	}

	@Override
	public void onGcCleanup(int policy) {
		for (Counter c : this) {
			if (c instanceof GcCleanable) {
				((GcCleanable) c).onGcCleanup(policy);
			}
		}
	}

	/**
	 * 由数据库加载计数项
	 * 
	 * @param counter
	 *            计数器
	 * @param id
	 *            计数项id
	 * @return 相应的项，没有则返回null
	 */
	protected abstract CounterItem doLoad(DbCounter counter, String id);

	/**
	 * 更新计数项到数据库
	 * 
	 * @param counter
	 *            计数器
	 * @param data
	 *            要更新的计数项列表
	 */
	protected abstract void doUpdate(DbCounter counter, DirtyData<CounterItem> data);

	/**
	 * （可能是）新的计数项，用于及时把新项写入数据库，此实现可选择略过
	 * 
	 * @param counter
	 *            计数器
	 * @param item
	 *            新计数项
	 */
	protected abstract void doNew(DbCounter counter, CounterItem item);
}

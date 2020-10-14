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
import cn.weforward.common.sys.GcCleaner;
import cn.weforward.data.array.Label;
import cn.weforward.data.array.LabelSet;
import cn.weforward.data.array.LabelSetFactory;
import cn.weforward.data.counter.Counter;
import cn.weforward.data.counter.CounterFactory;
import cn.weforward.data.counter.support.AbstractCounterFactory;
import cn.weforward.data.util.Flusher;

/**
 * 基于labelset的计数器实现
 * 
 * 以“计数器名.服务器标识”为label组织，通过把相同计数器名的多个服务器label相加则为总计数值，由此实现计数器的数据分布
 * 
 * @author liangyi
 *
 */
public class LabelCounterFactory extends AbstractCounterFactory
		implements CounterFactory, GcCleanable {
	protected LabelSet<CounterItem> m_Labelset;
	protected String m_ServerId;
	protected Flusher m_Flusher;
	protected boolean m_CopyOnFlush;

	public LabelCounterFactory(LabelSetFactory factory, String serverId) {
		m_ServerId = serverId;
		m_Labelset = factory.createLabelSet("__counter", CounterItem._Mapper);
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

	@Override
	synchronized protected LabelCounter doCreateCounter(String name) {
		String label = name + "." + getServerId();
		return new LabelCounter(this, m_Labelset.openLabel(label), name);
	}

	@Override
	public void onGcCleanup(int policy) {
		for (Counter c : this) {
			if (c instanceof LabelCounter) {
				((LabelCounter) c).onGcCleanup(policy);
			}
		}
	}

	public CounterItem getItem(LabelCounter counter, String key) {
		// 查出所有服务器的数据器相应label
		CounterItem item = ((null == counter) ? null : counter.m_Label.get(key));
		ResultPage<Label<CounterItem>> rp = m_Labelset.startsWith(counter.getName() + ".");
		if (rp.getCount() <= 1) {
			return item;
		}
		if (rp.getCount() > 100) {
			// XXX 不会吧，有这么多服务器？
		}
		rp.setPageSize(100);
		rp.gotoPage(1);

		// 把除当前服务器外的所有服务器的值累计到item.hold
		CounterItem other;
		for (Label<CounterItem> p : rp) {
			if (p == counter.m_Label) {
				// 排除
				continue;
			}
			other = p.get(key);
			if (null == other) {
				continue;
			}
			if (null == item) {
				item = new CounterItem(key);
			}
			item.hold += other.value;
		}
		return item;
	}
}

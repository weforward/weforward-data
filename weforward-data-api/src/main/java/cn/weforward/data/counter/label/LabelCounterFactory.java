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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.GcCleanable;
import cn.weforward.common.ResultPage;
import cn.weforward.common.util.LruCache.DirtyData;
import cn.weforward.data.array.Label;
import cn.weforward.data.array.LabelSet;
import cn.weforward.data.array.LabelSetFactory;
import cn.weforward.data.counter.CounterFactory;
import cn.weforward.data.counter.support.CounterItem;
import cn.weforward.data.counter.support.DbCounter;
import cn.weforward.data.counter.support.DbCounterFactory;

/**
 * 基于labelset的计数器实现
 * 
 * 以“计数器名.服务器标识”为label组织，通过把相同计数器名的多个服务器label相加则为总计数值，由此实现计数器的数据分布
 * 
 * @deprecated
 * @see cn.weforward.data.mongodb.counter.MongodbCounterFactory
 * @see cn.weforward.data.mysql.counter.MysqlCounterFactory
 */
public class LabelCounterFactory extends DbCounterFactory implements CounterFactory, GcCleanable {
	protected final static Logger _Logger = LoggerFactory.getLogger(LabelCounterFactory.class);

	protected LabelSet<CounterItem> m_Labelset;

	public LabelCounterFactory(LabelSetFactory factory, String serverId) {
		super(serverId);
		m_Labelset = factory.createLabelSet("__counter", CounterItem._Mapper);
	}

	@Override
	synchronized protected LabelCounter doCreateCounter(String name) {
		String label = name + "." + getServerId();
		return new LabelCounter(this, m_Labelset.openLabel(label), name);
	}

	protected CounterItem doLoad(DbCounter counter, String key) {
		LabelCounter labelCounter = (LabelCounter) counter;
		// 查出所有服务器的数据器相应label
		CounterItem item = ((null == labelCounter) ? null : labelCounter.m_Label.get(key));
		ResultPage<Label<CounterItem>> rp = m_Labelset.startsWith(labelCounter.getName() + ".");
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
			if (p == labelCounter.m_Label) {
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

	@Override
	protected void doUpdate(DbCounter counter, DirtyData<CounterItem> data) {
		LabelCounter labelCounter = (LabelCounter) counter;
		try {
			while (data.hasNext()) {
				CounterItem item = data.next();
				if (null != item) {
					labelCounter.m_Label.put(item, Label.OPTION_FORCE);
				}
			}
		} catch (Exception e) {
			_Logger.error("保存计数项失败 " + data, e);
		} finally {
			data.abort();
		}
	}

	@Override
	protected void doNew(DbCounter counter, CounterItem item) {
		((LabelCounter) counter).m_Label.put(item, Label.OPTION_FORCE);
	}
}

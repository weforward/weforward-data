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

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.TransResultPage;
import cn.weforward.data.array.Label;
import cn.weforward.data.counter.support.CounterItem;
import cn.weforward.data.counter.support.DbCounter;

/**
 * 暂存内存且持久化于labelset的计数器
 * 
 * @deprecated
 */
public class LabelCounter extends DbCounter {
	Label<CounterItem> m_Label;
	LabelCounterFactory m_Factory;

	public LabelCounter(LabelCounterFactory factory, Label<CounterItem> label, String name) {
		super(name, factory);
		m_Factory = factory;
		m_Label = label;
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
	public void removeAll() {
		m_Label.removeAll();
		super.removeAll();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(64);
		builder.append(super.toString()).append(m_Label);
		return builder.toString();
	}
}

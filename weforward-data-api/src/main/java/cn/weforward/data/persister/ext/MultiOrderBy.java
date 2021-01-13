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
package cn.weforward.data.persister.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cn.weforward.data.persister.OrderBy;

/**
 * 多种条件
 * 
 * @author daibo
 *
 */
public class MultiOrderBy implements Iterable<OrderBy>, OrderBy {
	/** 条件项 */
	protected List<OrderBy> m_Items;
	/** And 空对象 */
	public final static MultiOrderBy EMPTYD = new MultiOrderBy(Collections.emptyList());

	public MultiOrderBy() {
		m_Items = new ArrayList<>();
	}

	public MultiOrderBy(List<OrderBy> list) {
		m_Items = list;
	}

	public void add(OrderBy item) {
		m_Items.add(item);
	}

	public List<OrderBy> getItems() {
		return m_Items;
	}

	@Override
	public Iterator<OrderBy> iterator() {
		return m_Items.iterator();
	}

	@Override
	public List<String> getAsc() {
		List<String> list = new ArrayList<>();
		for (OrderBy by : getItems()) {
			list.addAll(by.getAsc());
		}
		return list;
	}

	@Override
	public List<String> getDesc() {
		List<String> list = new ArrayList<>();
		for (OrderBy by : getItems()) {
			list.addAll(by.getDesc());
		}
		return list;
	}
}

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
package cn.weforward.data.array.support;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.weforward.data.array.LabelElement;
import cn.weforward.data.array.LabelSet;
import cn.weforward.data.array.LabelSetFactory;
import cn.weforward.protocol.ext.ObjectMapper;

/**
 * 抽象 标签组织的集合工厂实现
 * 
 * @author daibo
 *
 */
public abstract class AbstractLabelSetFactory implements LabelSetFactory {
	/** 数据项 */
	private ConcurrentMap<String, LabelSet<? extends LabelElement>> m_Items;

	public AbstractLabelSetFactory() {
		m_Items = new ConcurrentHashMap<>();
	}

	@Override
	public Iterator<LabelSet<? extends LabelElement>> iterator() {
		return m_Items.values().iterator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends LabelElement> LabelSet<E> createLabelSet(String name, ObjectMapper<E> mapper) {
		LabelSet<E> c = (LabelSet<E>) m_Items.get(name);
		if (null != c) {
			throw new IllegalArgumentException("已存在同名的计数器[" + name + "]");
		}
		c = doCreateLabelSet(name, mapper);
		if (null == m_Items.putIfAbsent(name, c)) {
			return c;
		} else {
			throw new IllegalArgumentException("已存在同名的计数器[" + name + "]");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends LabelElement> LabelSet<E> getLabelSet(String name) {
		return (LabelSet<E>) m_Items.get(name);
	}

	/* 创建计数器 */
	protected abstract <E extends LabelElement> LabelSet<E> doCreateLabelSet(String name, ObjectMapper<E> mapper);

}

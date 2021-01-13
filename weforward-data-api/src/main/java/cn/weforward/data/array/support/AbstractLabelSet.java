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

import cn.weforward.common.ResultPage;
import cn.weforward.data.array.Label;
import cn.weforward.data.array.LabelElement;
import cn.weforward.data.array.LabelSet;

/**
 * 按标签组织的集合抽象实现
 * 
 * @author daibo
 *
 * @param <E> 元素类
 */
public abstract class AbstractLabelSet<E extends LabelElement> implements LabelSet<E> {
	/** 名称 */
	protected String m_Name;

	public AbstractLabelSet(String name) {
		m_Name = name;
	}

	@Override
	public String getName() {
		return m_Name;
	}

	@Override
	public void add(String label, E element) {
		openLabel(label).add(element);
	}

	@Override
	public E remove(String label, String id) {
		Label<E> l = getLabel(label);
		return null == l ? null : l.remove(id);
	}

	@Override
	public E put(String label, E element) {
		return openLabel(label).put(element, Label.OPTION_NONE);
	}

	@Override
	public boolean putIfAbsent(String label, E element) {
		return openLabel(label).put(element, Label.OPTION_IF_ABSENT) == null;
	}

	@Override
	public E get(String label, String id) {
		Label<E> l = getLabel(label);
		return null == l ? null : l.get(id);
	}

	@Override
	public ResultPage<Label<E>> getLabels() {
		return startsWith(null);
	}

}

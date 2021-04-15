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
import java.util.Iterator;
import java.util.List;

import cn.weforward.data.persister.Condition;
import cn.weforward.protocol.datatype.DtBase;

/**
 * 多种条件
 * 
 * @author daibo
 *
 */
public class MultiCondition implements Iterable<Condition>, Condition {
	/** 条件项 */
	protected List<Condition> m_Items;
	/** 类型 */
	protected short m_Type;

	/** And 空对象 */
	public final static MultiCondition EMPTY_AND = new MultiCondition(TYPE_AND);
	/** Or 空对象 */
	public final static MultiCondition EMPTY_OR = new MultiCondition(TYPE_OR);

	public MultiCondition(short type) {
		this(0, type);
	}

	public MultiCondition(int length, short type) {
		if (length > 0) {
			m_Items = new ArrayList<>(length);
		} else {
			m_Items = new ArrayList<>();
		}
		m_Type = type;
	}

	public MultiCondition(List<Condition> list, short type) {
		m_Items = list;
		m_Type = type;
	}

	public void add(Condition item) {
		m_Items.add(item);
	}

	public List<Condition> getItems() {
		return m_Items;
	}

	public short getType() {
		return m_Type;
	}

	@Override
	public Iterator<Condition> iterator() {
		return m_Items.iterator();
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public DtBase getValue() {
		return null;
	}
}

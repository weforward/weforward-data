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

import java.util.Collections;
import java.util.List;

import cn.weforward.data.persister.Condition;

/**
 * 单一条件
 * 
 * @author daibo
 *
 */
public class SingleCondition implements Condition {
	/** 属性名 */
	protected String m_Name;
	/** 属性值 */
	protected Object m_Value;
	/** 类型 */
	protected short m_Type;

	public SingleCondition(String name, Object value, short type) {
		m_Name = name;
		m_Value = value;
		m_Type = type;
	}

	public String getName() {
		return m_Name;
	}

	public Object getValue() {
		return m_Value;
	}

	public short getType() {
		return m_Type;
	}

	@Override
	public List<Condition> getItems() {
		return Collections.emptyList();
	}
}

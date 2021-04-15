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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.weforward.data.persister.OrderBy;

/**
 * 单一排序
 * 
 * @author daibo
 *
 */
public class SingleOrderBy implements OrderBy {
	/** 属性名 */
	protected String m_Name;
	/** 类型 */
	protected int m_Type;
	/** 升序 */
	public static final short ASC = 1;
	/** 降序 */
	public static final short DESC = 2;

	public SingleOrderBy(String name, int type) {
		m_Name = name;
		m_Type = type;
	}

	public String getName() {
		return m_Name;
	}

	public int getType() {
		return m_Type;
	}

	@Override
	public List<String> getAsc() {
		if (ASC == m_Type) {
			return Arrays.asList(m_Name);
		}
		return Collections.emptyList();
	}

	@Override
	public List<String> getDesc() {
		if (DESC == m_Type) {
			return Arrays.asList(m_Name);
		}
		return Collections.emptyList();
	}

}

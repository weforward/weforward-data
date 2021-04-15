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
package cn.weforward.data.mysql.util;

import cn.weforward.common.util.StringUtil;

/**
 * sql列
 * 
 * @author daibo
 *
 */
public class SqlColumn {
	/** 名称 */
	protected String m_Name;
	/** 类型 */
	protected SqlColumnType m_Type;

	public SqlColumn(String name, SqlColumnType type) {
		m_Name = name;
		m_Type = type;
	}

	/**
	 * 获取名称
	 * 
	 * @return 名称
	 */
	public String getName() {
		return m_Name;
	}

	/**
	 * 获取类型
	 * 
	 * @return 类型
	 */
	public SqlColumnType getType() {
		return m_Type;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SqlColumn) {
			SqlColumn other = (SqlColumn) obj;
			if (!StringUtil.eq(other.getName(), getName())) {
				return false;
			}
			if (null == getType()) {
				return null == other.getType();
			}
			return getType().equals(other.getType());
		} else {
			return false;
		}
	}
}

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

import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtBoolean;
import cn.weforward.protocol.datatype.DtDate;
import cn.weforward.protocol.datatype.DtList;
import cn.weforward.protocol.datatype.DtNumber;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.datatype.DtString;

/**
 * sql列类型
 * 
 * @author daibo
 *
 */
public class SqlColumnType {

	/** 名称 */
	protected String m_Name;
	/** 长度 */
	protected int m_Length;

	private static final SqlColumnType BOOLEAN_TYPE = new SqlColumnType("BOOL", 0);

	private static final SqlColumnType INT_TYPE = new SqlColumnType("INT", 0);

	private static final SqlColumnType LONG_TYPE = new SqlColumnType("BIGINT", 0);

	private static final SqlColumnType DOUBLE_TYPE = new SqlColumnType("DOUBLE", 0);

	private static final SqlColumnType DECIMAL_TYPE = new SqlColumnType("DECIMAL", 0);

	private static final SqlColumnType DATE_TYPE = new SqlColumnType("CHAR", 24);

	private static final SqlColumnType STRING_TYPE = new SqlColumnType("VARCHAR", 0);

	private static final SqlColumnType JSON_TYPE = new SqlColumnType("JSON", 0);

	/**
	 * 构造
	 * 
	 * @param name   名称
	 * @param length 长度
	 */
	public SqlColumnType(String name, int length) {
		this(name, length, null);
	}

	/**
	 * 构造
	 * 
	 * @param name    名称
	 * @param length  长度
	 * @param comment 注解
	 */
	public SqlColumnType(String name, int length, String comment) {
		m_Name = name;
		m_Length = length;
	}

	/**
	 * 获取列名
	 * 
	 * @return 列名
	 */
	public String getName() {
		return m_Name;
	}

	/**
	 * 设置长度
	 * 
	 * @param l 长度
	 */
	public void setLength(int l) {
		m_Length = l;
	}

	/**
	 * 获取长度
	 * 
	 * @return 列长度
	 */
	public int getLength() {
		return m_Length;
	}

	public static SqlColumnType getStringType(int length) {
		return new SqlColumnType(STRING_TYPE.getName(), length);
	}

	public static SqlColumnType getLongType() {
		return LONG_TYPE;
	}

	public static SqlColumnType getIntType() {
		return INT_TYPE;
	}

	public static SqlColumnType getDoubleType() {
		return DOUBLE_TYPE;
	}

	public static SqlColumnType getDateType() {
		return DATE_TYPE;
	}

	public static SqlColumnType getJsonType() {
		return JSON_TYPE;
	}

	public static SqlColumnType getType(DtBase value, int defaultStringLenth) {
		if (value instanceof DtObject) {
			return getJsonType();
		} else if (value instanceof DtList) {
			return getJsonType();
		} else if (value instanceof DtDate) {
			return getDateType();
		} else if (value instanceof DtString) {
			SqlColumnType type = getStringType(defaultStringLenth);
			int l = ((DtString) value).value().length();
			if (l > type.getLength()) {
				int s = l / defaultStringLenth;
				type.setLength(defaultStringLenth * (s + 1));
			}
			return type;
		} else if (value instanceof DtNumber) {
			DtNumber n = (DtNumber) value;
			if (n.isDouble()) {
				return getDoubleType();
			} else if (n.isLong()) {
				return getLongType();
			} else if (n.isInt()) {
				return getIntType();
			} else {
				return DECIMAL_TYPE;
			}
		} else if (value instanceof DtBoolean) {
			return BOOLEAN_TYPE;
		} else {
			throw new UnsupportedOperationException("不支持的数据类型:" + value.getClass());
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SqlColumnType) {
			SqlColumnType other = (SqlColumnType) obj;
			return getName().equalsIgnoreCase(other.getName()) && getLength() == other.getLength();
		}
		return false;
	}

	@Override
	public String toString() {
		return m_Length > 0 ? m_Name + "(" + m_Length + ")" : m_Name;
	}

}
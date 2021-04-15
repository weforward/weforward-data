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
package cn.weforward.data.search.vo;

import cn.weforward.common.KvPair;
import cn.weforward.data.search.IndexAttribute;

/**
 * 索引索引vo
 * 
 * @author daibo
 *
 */
public class IndexAttributeVo implements IndexAttribute {

	protected String m_Key;

	protected Object m_Value;

	public IndexAttributeVo(String key, Object value) {
		if (null != value) {
			Class<?> clazz = value.getClass();
			if (!Number.class.isAssignableFrom(clazz) && !String.class.isAssignableFrom(clazz)) {
				throw new UnsupportedOperationException("属性只能是数字或字符串");
			}
		}
		m_Key = key;
		m_Value = value;
	}

	/**
	 * 构造类
	 * 
	 * @param pair 参数对
	 * @return 属性vo
	 * @deprecated 拼写错误使用 {@link #valueOf(KvPair)}
	 */
	@Deprecated
	public static IndexAttributeVo vlaueOf(KvPair<String, String> pair) {
		return valueOf(pair);
	}

	/**
	 * 构造类
	 * 
	 * @param pair 参数对
	 * @return 属性vo
	 */
	public static IndexAttributeVo valueOf(KvPair<String, String> pair) {
		return new IndexAttributeVo(pair.getKey(), pair.getValue());
	}

	@Override
	public String getKey() {
		return m_Key;
	}

	@Override
	public Object getValue() {
		return m_Value;
	}

}

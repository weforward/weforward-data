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
package cn.weforward.data.search.util;

import cn.weforward.data.search.IndexAttribute;
import cn.weforward.data.search.vo.IndexAttributeVo;

/**
 * 索引属性vo
 * 
 * @author daibo
 *
 */
public class IndexAttributeHelper {

	private IndexAttributeHelper() {

	}

	/**
	 * 构造一个属性
	 * 
	 * @param key   键
	 * @param value 值
	 * @return 索引属性
	 */
	public static IndexAttribute newAttribute(String key, String value) {
		return new IndexAttributeVo(key, value);
	}

	/**
	 * 构造一个属性
	 * 
	 * @param key    键
	 * @param number 值
	 * @return 索引属性
	 */
	public static IndexAttribute newAttribute(String key, Number number) {
		return new IndexAttributeVo(key, number);
	}

}

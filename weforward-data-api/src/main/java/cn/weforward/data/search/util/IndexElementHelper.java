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

import java.util.List;

import cn.weforward.data.UniteId;
import cn.weforward.data.search.IndexAttribute;
import cn.weforward.data.search.IndexElement;
import cn.weforward.data.search.vo.IndexElementVo;

/**
 * IndexElement工具类
 * 
 * @author daibo
 *
 */
public class IndexElementHelper {
	/**
	 * 构造一个新元素
	 * 
	 * @param uid 唯一id
	 * @return 索引元素
	 */
	public static IndexElement newElement(String uid) {
		return IndexElementVo.valueOf(uid);
	}

	/**
	 * 构造一个新元素
	 * 
	 * @param uid 唯一id
	 * @return 索引元素
	 */
	public static IndexElement newElement(UniteId uid) {
		return IndexElementVo.valueOf(uid);
	}

	/**
	 * 构造一个新元素
	 * 
	 * @param uid        被索引条目ID
	 * @param attributes 属性（主要用于排序）
	 * @return 索引元素
	 */
	public static IndexElement newElement(UniteId uid, List<IndexAttribute> attributes) {
		return new IndexElementVo(uid.getId(), uid.getCaption(), null, attributes);
	}

	/**
	 * 构造一个新元素
	 * 
	 * @param id         被索引条目ID
	 * @param attributes 属性（主要用于排序）
	 * @return 索引元素
	 */
	public static IndexElement newElement(String id, List<IndexAttribute> attributes) {
		return new IndexElementVo(id, null, null, attributes);
	}

	/**
	 * 构造一个新元素
	 * 
	 * @param key        被索引条目ID
	 * @param caption    标题
	 * @param summary    摘要
	 * @param attributes 属性（主要用于排序）
	 * @return 索引元素
	 */
	public static IndexElement newElement(String key, String caption, String summary, List<IndexAttribute> attributes) {
		return new IndexElementVo(key, caption, summary, attributes);
	}

}

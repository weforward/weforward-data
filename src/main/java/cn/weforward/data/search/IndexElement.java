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
package cn.weforward.data.search;

import java.util.List;

/**
 * 索引项，以标识键作被索引对象的关联（通常是对象ID）
 * 
 * @author daibo
 * 
 */
public interface IndexElement {

	/**
	 * 键
	 * 
	 * @return 键
	 */
	String getKey();

	/**
	 * 摘要
	 * 
	 * @return 摘要
	 */
	String getSummary();

	/**
	 * 总结
	 * 
	 * @return 总结
	 */
	String getCaption();

	/**
	 * 属性
	 * 
	 * @param name 属性名（忽略大小写）
	 * @return 相应的属性，没有则返回null
	 */
	Object getAttribute(String name);

	/**
	 * 属性（主要用于排序）
	 * 
	 * @return 属性
	 */
	List<IndexAttribute> getAttributes();

}

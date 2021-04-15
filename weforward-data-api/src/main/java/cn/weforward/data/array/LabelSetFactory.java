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
package cn.weforward.data.array;

import cn.weforward.protocol.ext.ObjectMapper;

/**
 * 标签组织的集合创建工厂
 * 
 * @author daibo
 *
 */
public interface LabelSetFactory extends Iterable<LabelSet<? extends LabelElement>> {
	/**
	 * 创建labelset
	 * 
	 * @param <E>    元素类
	 * @param name   集合名
	 * @param mapper 映射表
	 * @return 标签集合
	 */
	<E extends LabelElement> LabelSet<E> createLabelSet(String name, ObjectMapper<E> mapper);

	/**
	 * 获取lebelset
	 * 
	 * @param <E>  元素类
	 * @param name 集合名
	 * @return 标签集合
	 */
	<E extends LabelElement> LabelSet<E> getLabelSet(String name);

}

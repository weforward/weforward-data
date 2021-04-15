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

import cn.weforward.common.Nameable;
import cn.weforward.common.ResultPage;

/**
 * 按标签组织的集合
 * 
 * @author liangyi
 * 
 */
public interface LabelSet<E extends LabelElement> extends Nameable {
	/**
	 * 取指定标签
	 * 
	 * @param label 标签名
	 * @return 有则返回相应的标签
	 */
	Label<E> getLabel(String label);

	/**
	 * 打开/创建标签项集合，若是新建的则为单项存储的标签项集合
	 * 
	 * @param label 标签名
	 * @return 标签项集合
	 */
	Label<E> openLabel(String label);

	/**
	 * 取得集合项
	 * 
	 * @param label 标签
	 * @param id    项ID
	 * @return 相应的集合项
	 */
	E get(String label, String id);

	/**
	 * 增加标签项
	 * 
	 * @param label   要增加项的所属标签
	 * @param element 要增加的标签项
	 */
	void add(String label, E element);

	/**
	 * 置入标签项
	 * 
	 * @param label   要置入标签项的所属标签
	 * @param element 要置入的标签项
	 * @return 返回非null表示覆盖旧的（相同）项
	 */
	E put(String label, E element);

	/**
	 * 标签项缺少即加入
	 * 
	 * @param label   要置入标签项的所属标签
	 * @param element 要置入的标签项
	 * @return 返回true表示加入
	 */
	boolean putIfAbsent(String label, E element);

	/**
	 * 移除标签项
	 * 
	 * @param label 要移除项的所属标签
	 * @param id    项ID
	 * @return 返回已移除的项，集合中没有则返回null
	 */
	E remove(String label, String id);

	/**
	 * 移除整个标签
	 * 
	 * @param label 要移的标签
	 * @return 指定的标签已移除返回true，若没有此标签返回false
	 */
	boolean remove(String label);

	/**
	 * 清空整个标签集
	 */
	void removeAll();

	/**
	 * 获取所有标签
	 * 
	 * @return 所有标签结果集
	 */
	ResultPage<Label<E>> getLabels();

	/**
	 * 取得指定前缀的标签集
	 * 
	 * @param prefix 标签前缀
	 * @return 结果页
	 */
	ResultPage<Label<E>> startsWith(String prefix);

	/**
	 * 获取ID区间（ID&gt;=first且ID&lt;=last）内的标签集
	 * 
	 * @param first 标签ID区间的开始，可以为null
	 * @param last  标签ID区间的结束，可以为null
	 * @return 结果集
	 */
	ResultPage<Label<E>> searchRange(String first, String last);
}

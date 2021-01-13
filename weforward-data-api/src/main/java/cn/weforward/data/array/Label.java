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
 * 标签方式聚合在一块的集合
 * 
 * @author liangyi
 * 
 */
public interface Label<E extends LabelElement> extends Nameable {
	/** 选项 - 不显式使用 */
	int OPTION_NONE = 0x00;
	/** 选项 - 在put的时候不管项是否相同，都强制标记项有变化 */
	int OPTION_FORCE = 0x10;
	/** 选项 - 在put的时候只增加新项 */
	int OPTION_IF_ABSENT = 0x02;

	/**
	 * 取标签/标识（ID）
	 * 
	 * @return 标签/标识
	 */
	String getName();

	/**
	 * 由ID获取标签项
	 * 
	 * @param id 标签项ID
	 * @return 相应的标签项
	 */
	E get(String id);

	/**
	 * 增加标签项
	 * 
	 * @param element 要增加的标签项
	 */
	void add(E element);

	/**
	 * 置入标签项
	 * 
	 * @param element 要置入的标签项
	 * @param options 选项 OPTION_xxx
	 * @return 返回null表示新增加，否则返回被覆盖的旧（相同）项
	 */
	E put(E element, int options);

	/**
	 * 移除标签项
	 * 
	 * @param id 要移除的标签项ID
	 * @return 返回已移除的项，集合中没有则返回null
	 */
	E remove(String id);

	/**
	 * 页结果封装的当前标签集合
	 * 
	 * @return 结果页
	 */
	ResultPage<E> resultPage();

	/**
	 * 前缀查询
	 * 
	 * @param prefix 标签项ID前缀，若为null或零长度的字串，则与resultPage()等效
	 * @return 结果页
	 */
	ResultPage<E> startsWith(String prefix);

	/**
	 * 获取ID区间（ID&gt;=first且ID&lt;=last）内的标签项
	 * 
	 * @param first ID区间的开始，可以为null
	 * @param last  ID区间的结束，可以为null
	 * @return 结果页
	 */
	ResultPage<E> searchRange(String first, String last);

	/**
	 * 删除ID区间（ID&gt;=first且ID&lt;=last）内的标签项，注：此操作不会产生版本日志
	 * 
	 * @param first ID区间的开始，可以为null
	 * @param last  ID区间的结束，可以为null
	 * @return 删除的项数
	 */
	long removeRange(String first, String last);

	/**
	 * 移除所有标签项
	 */
	void removeAll();
}

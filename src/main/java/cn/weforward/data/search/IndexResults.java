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

import cn.weforward.common.ResultPage;

/**
 * 查找结果集（暂只是简单地继承ResultPage&lt;IndexResult&gt;）
 * 
 * @author liangyi
 * 
 */
public interface IndexResults extends ResultPage<IndexResult> {
	/** 选项 - 顺序排序 */
	int OPTION_ORDER_BY_ASC = 0x00;
	/** 选项 - 倒序排序 */
	int OPTION_ORDER_BY_DESC = 0x01;

	/**
	 * 对结果按属性排序（排序后影响已进行的翻页及迭代），注：旧的实现或数据不支持什么也不执行但不会抛出错误
	 * 
	 * @param attribut 要排序的属性（必须在创建索引时在IndexElement指定）
	 * @param option   选项 OPTION_ORDER_BY_ASC/OPTION_ORDER_BY_DESC
	 */
	void sort(String attribut, int option);

	/**
	 * 产生一个快照（与当前互不影响）
	 * 
	 * @return 若不支持返回null
	 */
	IndexResults snapshot();

}

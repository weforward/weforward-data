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

/**
 * 
 * 索引关键词项，由关键词、匹配率及可选的所反向索引的索引项数
 * 
 * 匹配率：
 * <p>
 * ID &gt; NAME &gt; ANOTHER &gt; ELEMENT &gt; ATTRIBUTE &gt; BROAD
 * <p>
 * RATE_ANOTHER = (2*RATE_ELEMENT)+0x20
 * <p>
 * 
 * @author liangyi
 * 
 */
public interface IndexKeyword {
	/** 标记符-作者，如“A:liangyi” */
	String MARK_AUTHOR = "A:";
	/** 标记符-分类，如“K:酒店” */
	String MARK_KIND = "K:";
	/** 标记符-日期/时间 */
	String MARK_DATE = "D:";

	/**
	 * 选项-用于在查询时指定匹配的索引项key前缀，如“newIndexKeyword("myobject",RATE_PREFIX)”表示返回的必须是
	 * “myobject*”的索引项
	 */
	int RATE_PREFIX = -1;

	/** 多个关键词间的分隔号 */
	char BOUNDARY = ';';

	/** 匹配率连接号 */
	char SPEARATOR_RATE = '!';

	/**
	 * 关键词
	 * 
	 * @return 关键词
	 */
	String getKeyword();

	/**
	 * 匹配率应大于0
	 * 
	 * @return 匹配率
	 */
	long getRate();

}

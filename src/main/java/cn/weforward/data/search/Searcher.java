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

import cn.weforward.common.Nameable;

/**
 * 搜索器
 * 
 * @author daibo
 *
 */
public interface Searcher extends Nameable {
	/**
	 * 更新/创建索引条目
	 * 
	 * @param element  要索引的条目
	 * @param keywords 条目所关联的关键词表
	 */
	void updateElement(IndexElement element, List<? extends IndexKeyword> keywords);

	/**
	 * 更新/创建索引条目
	 * 
	 * @param element 要索引的条目
	 * @param keyword 条目所关联的关键词组
	 */
	void updateElement(IndexElement element, String... keyword);

	/**
	 * 删除索引条目
	 * 
	 * @param elementKey 索引项目的标识
	 * @return 条目在索引中且被删除返回true
	 */
	boolean removeElement(String elementKey);

	/**
	 * 搜索符合指定关键词表的条目
	 * 
	 * @param options 选项
	 * @param keyword 关键词（可多个），多个关键字为“&amp;”关系，全匹配才会返回
	 * @return 查询页结果，默认按id排序
	 */
	IndexResults search(SearchOption options, String... keyword);

	/**
	 * 搜索符合指定关键词表的条目
	 * 
	 * @param keywords 关键词列表，多个关键字为“&amp;”关系，全匹配才会返回
	 * @param options  选项
	 * @return 查询页结果，默认按id排序
	 */
	IndexResults search(List<? extends IndexKeyword> keywords, SearchOption options);

	/**
	 * 按两个区间词进行搜索，通常用于数值的区间查找，但数值转换为字串前需要补零对齐，如“010=&gt;100”，其它字串的区间如“abc =&gt; cde”
	 * 
	 * [begin,end)
	 * 
	 * @param begin   区间开始值
	 * @param to      区间结束值
	 * @param options 选项
	 * @return 查询页结果，默认按id排序
	 */
	IndexResults searchRange(String begin, String to, SearchOption options);

	/**
	 * 按两个区间词进行搜索，通常用于数值的区间查找，但数值转换为字串前需要补零对齐，，如“010=&gt;100”，其它字串的区间如“abc =&gt;
	 * cde”
	 * 
	 * [begin,end)
	 * 
	 * @param begin    区间开始值
	 * @param to       区间结束值
	 * @param keywords 除在begin-&gt;to之间外，还匹配keywords中的关键词，多个关键字为&amp;关系，全匹配才会返回
	 * @param options  选项
	 * @return 查询页结果，默认按id排序
	 */
	IndexResults searchRange(String begin, String to, List<? extends IndexKeyword> keywords, SearchOption options);

	/**
	 * 按多个区间词进行搜索，通常用于数值的区间查找，但数值转换为字串前需要补零对齐，，如“010=&gt;100”，其它字串的区间如“abc =&gt;
	 * cde”
	 * 
	 * [begin,end)
	 * 
	 * @param ranges   区间，区间为“&amp;”关系，全匹配才会返回
	 * @param keywords 除在区间外，还匹配keywords中的关键词，多个关键字为&amp;关系，全匹配才会返回
	 * @param options  选项
	 * @return 查询页结果，默认按id排序
	 */
	IndexResults searchRange(List<? extends IndexRange> ranges, List<? extends IndexKeyword> keywords,
			SearchOption options);

	/**
	 * 取指定关键词表的并集，与search的最大区别是前者的结果是“与”，后者的结果是“或”
	 * 
	 * @param options 选项
	 * @param keyword 关键词（可多个），多个关键字为“|”关系，单个匹配就会返回
	 * @return 查询页结果，默认按id排序
	 */
	IndexResults union(SearchOption options, String... keyword);

	/**
	 * 取指定关键词表的并集，与search的最大区别是前者的结果是“与”，后者的结果是“或”
	 * 
	 * @param keywords 关键词列表，多个关键字为“|”关系，单个匹配就会返回
	 * @param options  选项
	 * @return 查询页结果，默认按id排序
	 */
	IndexResults union(List<? extends IndexKeyword> keywords, SearchOption options);

	/**
	 * 按多个区间词进行搜索，通常用于数值的区间查找，但数值转换为字串前需要补零对齐，，如“010=&gt;100”，其它字串的区间如“abc =&gt;
	 * cde”
	 * 
	 * [begin,end)
	 * 
	 * @param ranges   区间，区间为“|”关系，全匹配才会返回
	 * @param keywords 除在区间外，还匹配keywords中的关键词，多个关键字为&amp;关系，全匹配才会返回
	 * @param options  选项
	 * @return 查询页结果，默认按id排序
	 */
	IndexResults unionRange(List<? extends IndexRange> ranges, List<? extends IndexKeyword> keywords,
			SearchOption options);

	/**
	 * 全索引功能
	 * 
	 * @param orRanges    “|”关系区间
	 * @param andRanges   “&amp;”关系区间
	 * @param orKeywords  “|”关系关键字
	 * @param andKeywords “&amp;”关系关键字
	 * @param options     选项
	 * @return 查询页结果，默认按id排序
	 */
	IndexResults searchAll(List<? extends IndexRange> andRanges, List<? extends IndexRange> orRanges,
			List<? extends IndexKeyword> andKeywords, List<? extends IndexKeyword> orKeywords, SearchOption options);

}

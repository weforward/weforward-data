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
package cn.weforward.data.search.support;

import java.util.Arrays;
import java.util.List;

import cn.weforward.data.search.IndexElement;
import cn.weforward.data.search.IndexKeyword;
import cn.weforward.data.search.IndexRange;
import cn.weforward.data.search.IndexResults;
import cn.weforward.data.search.SearchOption;
import cn.weforward.data.search.Searcher;
import cn.weforward.data.search.util.IndexKeywordHelper;
import cn.weforward.data.search.util.IndexResultsHelper;

/**
 * 抽象索引器实现
 * 
 * @author daibo
 *
 */
public abstract class AbstractSearcher implements Searcher {

	/** 名称 */
	protected String m_Name;

	public AbstractSearcher(String name) {
		m_Name = name;
	}

	@Override
	public String getName() {
		return m_Name;
	}

	@Override
	public void updateElement(IndexElement element, String... keyword) {
		updateElement(element, IndexKeywordHelper.toKeywords(keyword));
	}

	@Override
	public IndexResults search(SearchOption options, String... keyword) {
		if (null == keyword) {
			// 会有这种操作？
			return IndexResultsHelper.empty();
		}
		return search(IndexKeywordHelper.toKeywords(keyword), options);
	}

	@Override
	public IndexResults search(List<? extends IndexKeyword> keywords, SearchOption options) {
		return searchRange(null, null, keywords, options);
	}

	@Override
	public IndexResults searchRange(String begin, String to, SearchOption options) {
		if (null == begin && null == to) {
			return IndexResultsHelper.empty();
		}
		return searchRange(begin, to, null, options);
	}

	@Override
	public IndexResults searchRange(String begin, String to, List<? extends IndexKeyword> keywords,
			SearchOption options) {
		if (null == begin && null == to) {
			return searchRange(null, keywords, options);
		}
		return searchRange(Arrays.asList(IndexKeywordHelper.newRange(begin, to)), keywords, options);
	}

	@Override
	public IndexResults searchRange(List<? extends IndexRange> ranges, List<? extends IndexKeyword> keywords,
			SearchOption options) {
		return searchAll(ranges, null, keywords, null, options);
	}

	@Override
	public IndexResults union(SearchOption options, String... keyword) {
		if (null == keyword) {
			// 会有这种操作？
			return IndexResultsHelper.empty();
		}
		return union(IndexKeywordHelper.toKeywords(keyword), options);
	}

	@Override
	public IndexResults union(List<? extends IndexKeyword> keywords, SearchOption options) {
		return unionRange(null, keywords, options);
	}

	@Override
	public IndexResults unionRange(List<? extends IndexRange> ranges, List<? extends IndexKeyword> keywords,
			SearchOption options) {
		return searchAll(null, ranges, null, keywords, options);
	}

}

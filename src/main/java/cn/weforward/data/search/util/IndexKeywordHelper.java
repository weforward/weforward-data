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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.weforward.common.util.NumberUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.search.IndexKeyword;
import cn.weforward.data.search.IndexRange;
import cn.weforward.data.search.vo.KeywordVo;
import cn.weforward.data.search.vo.RangeVo;

/**
 * 
 * 索引关键词项，由关键词、匹配率及可选的关键词所索引的条目数
 * 
 * 匹配率：ID&gt;NAME&gt;ANOTHER&gt;ELEMENT&gt;ATTRIBUTE&gt;BROAD
 * <p>
 * 2*RATE_ELEMENT+0x20==RATE_ANOTHER
 * 
 * @author daibo
 * 
 */
public class IndexKeywordHelper {
	/** 空关键词 */
	private static final IndexKeyword _nil = new IndexKeyword() {

		@Override
		public String getKeyword() {
			return "";
		}

		@Override
		public long getRate() {
			return 0;
		}

	};

	private IndexKeywordHelper() {
	}

	/**
	 * 由格式串“&lt;关键词&gt;!&lt;匹配率&gt;”创建索引关键词项
	 * 
	 * @param format 关键字串
	 * @return 关键字
	 */
	static public IndexKeyword newKeyword(String format) {
		int idx = format.indexOf(IndexKeyword.SPEARATOR_RATE);
		if (-1 == idx) {
			return new KeywordVo(format, 0);
		}
		return new KeywordVo(format.substring(0, idx), NumberUtil.toInt(format.substring(1 + idx), 0));
	}

	/**
	 * 当关键字不为空时添加
	 * 
	 * @param list    要加入的列表
	 * @param keyword 关键词
	 * @param rate    匹配率
	 * @return 加入返回的列表
	 */
	public static List<IndexKeyword> addKeywordIfNotNull(List<IndexKeyword> list, String keyword, long rate) {
		if (null == list) {
			list = new ArrayList<>();
		}
		if (StringUtil.isEmpty(keyword)) {
			return list;
		}
		list.add(newKeyword(keyword, rate));
		return list;
	}

	/**
	 * 创建关键词项
	 * 
	 * @param keyword 关键词
	 * @param rate    匹配率
	 * @return 由此创建的关键词项
	 */
	public static IndexKeyword newKeyword(String keyword, long rate) {
		if (StringUtil.isEmpty(keyword)) {
			return _nil;
		}
		return new KeywordVo(keyword, rate);
	}

	/**
	 * 生成用于查询时限定索引项key前缀的关键词
	 * 
	 * @param prefix 前缀
	 * @return 关键词项
	 */
	public static IndexKeyword prefixKeyword(String prefix) {
		return new KeywordVo(prefix, IndexKeyword.RATE_PREFIX);
	}

	/**
	 * 转换字串数组为关键词数组
	 * 
	 * @param keywords 关键词
	 * @return 关键词项
	 */
	static public List<IndexKeyword> toKeywords(String... keywords) {
		if (null == keywords || 0 == keywords.length) {
			return null;
		}
		if (1 == keywords.length) {
			return Collections.singletonList(newKeyword(keywords[0], 0));
		}
		ArrayList<IndexKeyword> ks = new ArrayList<IndexKeyword>(keywords.length);
		for (int i = 0; i < keywords.length; i++) {
			String k = keywords[i];
			if (null == k || 0 == k.length()) {
				continue;
			}
			IndexKeyword ke = newKeyword(k, 0);
			ks.add(ke);
		}
		return ks;
	}

	/**
	 * 创建范围索引项
	 * 
	 * @param begin 开始
	 * @param to    结束
	 * @return 关键词项
	 */
	static public IndexRange newRange(String begin, String to) {
		return new RangeVo(begin, to);
	}

}

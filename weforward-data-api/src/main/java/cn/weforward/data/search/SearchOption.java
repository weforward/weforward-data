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
 * 搜索选项
 * 
 * @author daibo
 *
 */
public class SearchOption {
	/** 选项-不指定 */
	public static final int OPTION_NONE = 0;
	/** 选项-按匹配率排序 */
	public static final int OPTION_RATE_SORT = 0x00010000;
	/** 选项-指定至少匹配率（需另外setRate） */
	public static final int OPTION_RATE_LEAST = 0x00020000;
	/** 选项-限制搜索的项（需另外setLimit） */
	public static final int OPTION_RANGE_LIMIT = 0x00400000;
	/** 选项-指定匹配范围（需另外setStartRate,setEndRate） */
	public static final int OPTION_RATE_RANGE = 0x00800000;
	/** 选项- 需要IndexElement的信息（如：caption,summary,attributes...） */
	public static final int OPTION_RESULT_DETAIL = 0x00100000;

	/** 选项 */
	protected int m_Options;
	/** 限制数 */
	protected int m_Limit;
	/** 匹配率 */
	protected long m_Rate;

	/** 匹配率-开始 */
	protected long m_StartRate;
	/** 匹配率-结束 */
	protected long m_EndRate;

	protected SearchOption(int option) {
		m_Options = option;
	}

	/**
	 * 是否有选项
	 * 
	 * @param option 选项
	 * @return true/false
	 */
	public boolean isOption(int option) {
		return (m_Options & option) == option;
	}

	/**
	 * 设置选项
	 * 
	 * @param option 选项，若负数则为去除，0则置0
	 * @return 选项
	 */
	public SearchOption setOption(int option) {
		if (option < 0) {
			m_Options &= ~(-option);
		} else if (0 == option) {
			m_Options = 0;
		} else {
			m_Options |= option;
		}
		return this;
	}

	/***
	 * 限制数
	 * 
	 * @param limit 限制数
	 * @return 选项
	 */
	public SearchOption setLimit(int limit) {
		m_Limit = limit;
		return this;
	}

	/**
	 * 限制数
	 * 
	 * @return 选项
	 */
	public int getLimit() {
		return m_Limit;
	}

	/**
	 * 匹配率
	 * 
	 * @param rate 匹配率
	 * @return 选项
	 */
	public SearchOption setRate(long rate) {
		m_Rate = rate;
		return this;
	}

	/**
	 * 匹配率
	 * 
	 * @return 匹配率
	 */
	public long getRate() {
		return m_Rate;
	}

	/**
	 * 匹配率-开始
	 * 
	 * @return 匹配率
	 */
	public long getStartRate() {
		return m_StartRate;
	}

	/**
	 * 匹配率-开始
	 * 
	 * @param rate 匹配率
	 * @return 选项
	 */
	public SearchOption setStartRate(long rate) {
		m_StartRate = rate;
		return this;
	}

	/**
	 * 匹配率-结束
	 * 
	 * @return 匹配率
	 */
	public long getEndRate() {
		return m_EndRate;
	}

	/**
	 * 匹配率-结束
	 * 
	 * @param rate 匹配率
	 * @return 选项
	 */
	public SearchOption setEndRate(long rate) {
		m_EndRate = rate;
		return this;
	}

	/**
	 * 构造选项
	 * 
	 * @param option 选项
	 * @return 选项
	 */
	public static SearchOption valueOf(int option) {
		return new SearchOption(option);
	}

}

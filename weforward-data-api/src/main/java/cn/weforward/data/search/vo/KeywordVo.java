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
package cn.weforward.data.search.vo;

import cn.weforward.common.util.StringUtil;
import cn.weforward.data.search.IndexKeyword;

/**
 * 关键词VO
 * 
 * @author daibo
 * 
 */
public class KeywordVo implements IndexKeyword {

	protected String m_Keyword;
	protected long m_Rate;

	protected KeywordVo() {
	}

	public KeywordVo(String keyword, long rate) {
		m_Keyword = StringUtil.toString(keyword);
		m_Rate = rate;
	}

	@Override
	public String getKeyword() {
		return m_Keyword;
	}

	@Override
	public long getRate() {
		return m_Rate;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof IndexKeyword) {
			IndexKeyword k = (IndexKeyword) obj;
			return StringUtil.eq(k.getKeyword(), getKeyword()) && k.getRate() == getRate();
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return ((0 == m_Rate) ? (m_Keyword) : (m_Keyword + SPEARATOR_RATE + m_Rate));
	}
}
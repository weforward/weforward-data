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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import cn.weforward.common.util.FreezedList;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.UniteId;
import cn.weforward.data.search.IndexAttribute;
import cn.weforward.data.search.IndexElement;

/**
 * 索引项，以标识键作被索引对象的关联（通常是对象ID）
 * 
 * @author daibo
 * 
 */
public class IndexElementVo implements IndexElement {
	/** 索引项标识键 */
	protected String m_Key;
	/** 索引项标题 */
	protected String m_Caption;
	/** 索引项摘要 */
	protected String m_Summary;
	/** 属性（主要用于排序） */
	protected List<IndexAttribute> m_Attributes;

	private static final Comparator<IndexAttribute> _COMP_ATTRIBUTE = new Comparator<IndexAttribute>() {

		@Override
		public int compare(IndexAttribute o1, IndexAttribute o2) {
			if (o1 == o2) {
				return 0;
			}
			if (null == o1) {
				return 1;
			}
			if (null == o2) {
				return -1;
			}
			return o1.getKey().compareToIgnoreCase(o2.getKey());
		}

	};

	/**
	 * 由联合ID构建索引项
	 * 
	 * @param uuid 联合ID
	 * @return 索引项
	 */
	public static IndexElement valueOf(UniteId uuid) {
		return new IndexElementVo(uuid.getUuid(), uuid.getCaption());
	}

	/**
	 * 由对象ID（序号）构建索引项
	 * 
	 * @param id ID（序号）
	 * @return 索引项
	 */
	public static IndexElement valueOf(String id) {
		return new IndexElementVo(id);
	}

	protected IndexElementVo() {
	}

	/**
	 * 构造
	 * 
	 * @param key 被索引条目ID
	 */
	public IndexElementVo(String key) {
		this(key, null);
	}

	/**
	 * 构造
	 * 
	 * @param key     被索引条目ID
	 * @param caption 标题
	 */
	public IndexElementVo(String key, String caption) {
		this(key, caption, null);
	}

	/**
	 * 构造
	 * 
	 * @param key     被索引条目ID
	 * @param caption 标题
	 * @param summary 摘要
	 */
	public IndexElementVo(String key, String caption, String summary) {
		m_Key = key;
		m_Caption = StringUtil.toString(caption);
		m_Summary = StringUtil.toString(summary);
	}

	/**
	 * 构造
	 * 
	 * @param key        被索引条目ID
	 * @param caption    标题
	 * @param summary    摘要
	 * @param attributes 属性（主要用于排序）
	 */
	public IndexElementVo(String key, String caption, String summary, IndexAttribute... attributes) {
		this(key, caption, summary, Arrays.asList(attributes));
	}

	/**
	 * 构造
	 * 
	 * @param key        被索引条目ID
	 * @param caption    标题
	 * @param summary    摘要
	 * @param attributes 属性（主要用于排序）
	 */
	public IndexElementVo(String key, String caption, String summary, List<IndexAttribute> attributes) {
		m_Key = key;
		m_Caption = StringUtil.toString(caption);
		m_Summary = StringUtil.toString(summary);
		m_Attributes = (null == attributes) ? null : FreezedList.freezed(attributes, _COMP_ATTRIBUTE);
	}

	@Override
	public String getKey() {
		return m_Key;
	}

	@Override
	public String getSummary() {
		return m_Summary;
	}

	@Override
	public String getCaption() {
		return m_Caption;
	}

	@Override
	public List<IndexAttribute> getAttributes() {
		return m_Attributes;
	}

	@Override
	public Object getAttribute(String name) {
		if (StringUtil.isEmpty(name)) {
			return null;
		}
		List<IndexAttribute> ls = m_Attributes;
		int c;
		if (null != ls) {
			IndexAttribute pair;
			for (int i = 0; i < ls.size(); i++) {
				pair = ls.get(i);
				if (null == pair) {
					continue;
				}
				c = name.compareToIgnoreCase(pair.getKey());
				if (0 == c) {
					return pair.getValue();
				}
				if (c < 0) {
					break;
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKey());
		String caption = getCaption();
		if (null != caption) {
			sb.append('|');
			sb.append(caption);
		}
		String summary = getSummary();
		if (null != summary) {
			sb.append("|");
			sb.append(summary);
		}
		return sb.toString();
	}

	public boolean equals(IndexElement obj) {
		if (this == obj) {
			return true;
		}
		if (null == obj) {
			return false;
		}
		return (eq(getKey(), obj.getKey()) && eq(getCaption(), obj.getCaption()) && eq(getSummary(), obj.getSummary())
				&& eq(getAttributes(), obj.getAttributes()));
	}

	final private boolean eq(String s1, String s2) {
		if (s1 == s2) {
			return true;
		}
		if (null == s1) {
			return (0 == s2.length());
		}
		if (null == s2) {
			return (0 == s1.length());
		}
		return s1.equals(s2);
	}

	final private boolean eq(Object s1, Object s2) {
		if (s1 == s2) {
			return true;
		}
		return s1.equals(s2);
	}

	final boolean eq(List<IndexAttribute> ls1, List<IndexAttribute> ls2) {
		if (ls1 == ls2) {
			return true;
		}
		if (null == ls1) {
			return (ls2.size() == 0);
		}
		if (null == ls2) {
			return (ls1.size() == 0);
		}
		if (ls1.size() == ls2.size()) {
			IndexAttribute p1, p2;
			for (int i = ls1.size() - 1; i >= 0; i--) {
				p1 = ls1.get(i);
				p2 = ls2.get(i);
				if (p1 != p2) {
					if (null == p1 || null == p2) {
						return false;
					}
					if (!eq(p1.getKey(), p2.getKey()) || !eq(p1.getValue(), p2.getValue())) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

}

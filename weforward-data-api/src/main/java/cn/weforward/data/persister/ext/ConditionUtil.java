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
package cn.weforward.data.persister.ext;

import java.util.Date;
import java.util.List;

import cn.weforward.common.util.StringUtil;
import cn.weforward.data.persister.Condition;
import cn.weforward.protocol.support.NamingConverter;
import cn.weforward.protocol.support.datatype.SimpleDtBoolean;

/**
 * 条件工具类
 * 
 * @author daibo
 *
 */
public class ConditionUtil {

	private ConditionUtil() {

	}

	/**
	 * 相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition eq(String name, boolean value) {
		return new SingleCondition(name, value, Condition.TYPE_EQ);
	}

	/**
	 * 相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition eq(String name, int value) {
		return new SingleCondition(name, value, Condition.TYPE_EQ);
	}

	/**
	 * 相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition eq(String name, long value) {
		return new SingleCondition(name, value, Condition.TYPE_EQ);
	}

	/**
	 * 相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition eq(String name, double value) {
		return new SingleCondition(name, value, Condition.TYPE_EQ);
	}

	/**
	 * 相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition eq(String name, String value) {
		return new SingleCondition(name, value, Condition.TYPE_EQ);
	}

	/**
	 * 相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition eq(String name, Date value) {
		return new SingleCondition(name, value, Condition.TYPE_EQ);
	}

	/**
	 * 不相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition ne(String name, boolean value) {
		return new SingleCondition(name, new SimpleDtBoolean(value), Condition.TYPE_NE);
	}

	/**
	 * 不相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition ne(String name, int value) {
		return new SingleCondition(name, value, Condition.TYPE_NE);
	}

	/**
	 * 不相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition ne(String name, long value) {
		return new SingleCondition(name, value, Condition.TYPE_NE);
	}

	/**
	 * 不相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition ne(String name, double value) {
		return new SingleCondition(name, value, Condition.TYPE_NE);
	}

	/**
	 * 不相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition ne(String name, String value) {
		return new SingleCondition(name, value, Condition.TYPE_NE);
	}

	/**
	 * 不相等
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition ne(String name, Date value) {
		return new SingleCondition(name, value, Condition.TYPE_NE);
	}

	/**
	 * 小于 $lt;
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition lt(String name, boolean value) {
		return new SingleCondition(name, new SimpleDtBoolean(value), Condition.TYPE_LT);
	}

	/**
	 * 小于 $lt;
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition lt(String name, int value) {
		return new SingleCondition(name, value, Condition.TYPE_LT);
	}

	/**
	 * 小于 &lt;
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition lt(String name, long value) {
		return new SingleCondition(name, value, Condition.TYPE_LT);
	}

	/**
	 * 小于 &lt;
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition lt(String name, double value) {
		return new SingleCondition(name, value, Condition.TYPE_LT);
	}

	/**
	 * 小于 &lt;
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition lt(String name, String value) {
		return new SingleCondition(name, value, Condition.TYPE_LT);
	}

	/**
	 * 小于 &lt;
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition lt(String name, Date value) {
		return new SingleCondition(name, value, Condition.TYPE_LT);
	}

	/**
	 * 大于 &gt;
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition gt(String name, int value) {
		return new SingleCondition(name, value, Condition.TYPE_GT);
	}

	/**
	 * 大于 &gt;
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition gt(String name, long value) {
		return new SingleCondition(name, value, Condition.TYPE_GT);
	}

	/**
	 * 大于 &gt;
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition gt(String name, double value) {
		return new SingleCondition(name, value, Condition.TYPE_GT);
	}

	/**
	 * 大于 &gt;
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition gt(String name, String value) {
		return new SingleCondition(name, value, Condition.TYPE_GT);
	}

	/**
	 * 大于 &gt;
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition gt(String name, Date value) {
		return new SingleCondition(name, value, Condition.TYPE_GT);
	}

	/**
	 * 小于等于 &lt;=
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition lte(String name, int value) {
		return new SingleCondition(name, value, Condition.TYPE_LTE);
	}

	/**
	 * 小于等于 &lt;=
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition lte(String name, long value) {
		return new SingleCondition(name, value, Condition.TYPE_LTE);
	}

	/**
	 * 小于等于 &lt;=
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition lte(String name, double value) {
		return new SingleCondition(name, value, Condition.TYPE_LTE);
	}

	/**
	 * 小于等于 &lt;=
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition lte(String name, String value) {
		return new SingleCondition(name, value, Condition.TYPE_LTE);
	}

	/**
	 * 小于等于 &lt;=
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition lte(String name, Date value) {
		return new SingleCondition(name, value, Condition.TYPE_LTE);
	}

	/**
	 * 大于等于 &gt;=
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition gte(String name, int value) {
		return new SingleCondition(name, value, Condition.TYPE_GTE);
	}

	/**
	 * 大于等于 &gt;=
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition gte(String name, long value) {
		return new SingleCondition(name, value, Condition.TYPE_GTE);
	}

	/**
	 * 大于等于 &gt;=
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition gte(String name, double value) {
		return new SingleCondition(name, value, Condition.TYPE_GTE);
	}

	/**
	 * 大于等于 &gt;=
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition gte(String name, String value) {
		return new SingleCondition(name, value, Condition.TYPE_GTE);
	}

	/**
	 * 大于等于 &gt;=
	 * 
	 * @param name  名称
	 * @param value 值
	 * @return 条件
	 */
	public static Condition gte(String name, Date value) {
		return new SingleCondition(name, value, Condition.TYPE_GTE);
	}

	/**
	 * 区间，name&gt;=from and id&lt;=to
	 * 
	 * @param name 名称
	 * @param from 开始
	 * @param to   结束
	 * @return 条件
	 */
	public static Condition range(String name, int from, int to) {
		return and(gte(name, from), lte(name, to));
	}

	/**
	 * 区间，name&gt;=from and id&lt;=to
	 * 
	 * @param name 名称
	 * @param from 开始
	 * @param to   结束
	 * @return 条件
	 */
	public static Condition range(String name, long from, long to) {
		return and(gte(name, from), lte(name, to));
	}

	/**
	 * 区间，name&gt;=from and id&lt;=to
	 * 
	 * @param name 名称
	 * @param from 开始
	 * @param to   结束
	 * @return 条件
	 */
	public static Condition range(String name, double from, double to) {
		return and(gte(name, from), lte(name, to));
	}

	/**
	 * 区间，name&gt;=from and id&lt;=to
	 * 
	 * @param name 名称
	 * @param from 开始
	 * @param to   结束
	 * @return 条件
	 */
	public static Condition range(String name, String from, String to) {
		if (StringUtil.isEmpty(from)) {
			return lte(name, to);
		} else if (StringUtil.isEmpty(to)) {
			return gte(name, from);
		}
		return and(gte(name, from), lte(name, to));
	}

	/**
	 * 区间，name&gt;=from and id&lt;=to
	 * 
	 * @param name 名称
	 * @param from 开始
	 * @param to   结束
	 * @return 条件
	 */
	public static Condition range(String name, Date from, Date to) {
		if (null == from) {
			return lte(name, to);
		} else if (null == to) {
			return gte(name, from);
		}
		return and(gte(name, from), lte(name, to));
	}

	/**
	 * &amp;与条件
	 * 
	 * @param arrs 数组
	 * @return 条件
	 */
	public static Condition and(Condition... arrs) {
		if (null == arrs) {
			return MultiCondition.EMPTY_AND;
		}
		MultiCondition mc = new MultiCondition(arrs.length, Condition.TYPE_AND);
		for (Condition c : arrs) {
			mc.add(c);
		}
		return mc;
	}

	/**
	 * &amp; 与条件
	 * 
	 * @param list 列表
	 * @return 条件
	 */
	public static Condition and(List<Condition> list) {
		if (null == list) {
			return MultiCondition.EMPTY_AND;
		}
		return new MultiCondition(list, Condition.TYPE_AND);
	}

	/**
	 * | 或条件
	 * 
	 * @param arrs 数组
	 * @return 条件
	 */
	public static Condition or(Condition... arrs) {
		if (null == arrs) {
			return MultiCondition.EMPTY_OR;
		}
		MultiCondition mc = new MultiCondition(arrs.length, Condition.TYPE_OR);
		for (Condition c : arrs) {
			mc.add(c);
		}
		return mc;
	}

	/**
	 * | 或条件
	 * 
	 * @param list 列表
	 * @return 条件
	 */
	public static Condition or(List<Condition> list) {
		if (null == list) {
			return MultiCondition.EMPTY_OR;
		}
		return new MultiCondition(list, Condition.TYPE_OR);
	}

	/**
	 * 属性名转换
	 * 
	 * @param names 名称
	 * @return 条件
	 */
	public static String field(String... names) {
		if (null == names) {
			return null;
		} else if (names.length == 0) {
			return null;
		} else if (names.length == 1) {
			return fixName(names[0]);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(fixName(names[0]));
			for (int i = 1; i < names.length; i++) {
				sb.append(Condition.FIELD_SPEARATOR);
				sb.append(fixName(names[i]));
			}
			return sb.toString();
		}
	}

	private static String fixName(String name) {
		if (name.startsWith("m_")) {
			name = NamingConverter.camelToWf(Character.toLowerCase(name.charAt(2)) + name.substring(3));
		} else {
			name = NamingConverter.camelToWf(name);
		}
		return name;
	}
}

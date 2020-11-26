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

import java.util.Arrays;
import java.util.List;

import cn.weforward.common.util.ListUtil;
import cn.weforward.common.util.TransList;
import cn.weforward.data.persister.Condition;
import cn.weforward.data.persister.OrderBy;
import cn.weforward.protocol.support.NamingConverter;

/**
 * 排序工具类
 * 
 * @author daibo
 *
 */
public class OrderByUtil {

	private OrderByUtil() {

	}

	/**
	 * 升序
	 * 
	 * @param names 属性名
	 * @return 排序
	 */
	public static OrderBy asc(String... names) {
		if (null == names) {
			return null;
		}
		return asc(Arrays.asList(names));
	}

	/**
	 * 升序
	 * 
	 * @param list 属性名
	 * @return 排序
	 */
	public static OrderBy asc(List<String> list) {
		return unite(list, null);
	}

	/**
	 * 降序
	 * 
	 * @param names 属性名
	 * @return 排序
	 */
	public static OrderBy desc(String... names) {
		if (null == names) {
			return null;
		}
		return desc(Arrays.asList(names));
	}

	/**
	 * 降序
	 * 
	 * @param list 属性名
	 * @return 排序
	 */
	public static OrderBy desc(List<String> list) {
		return unite(null, list);
	}

	/**
	 * 联合排序
	 * 
	 * @param asc  升序属性
	 * @param desc 降序属性
	 * @return 排序
	 */
	public static OrderBy unite(List<String> asc, List<String> desc) {
		if (ListUtil.isEmpty(asc) && ListUtil.isEmpty(desc)) {
			return null;
		}
		if (ListUtil.isEmpty(asc)) {
			if (desc.size() == 1) {
				return new SingleOrderBy(desc.get(0), SingleOrderBy.DESC);
			} else {
				return new MultiOrderBy(TransList.valueOf(desc, (n) -> new SingleOrderBy(n, SingleOrderBy.DESC)));
			}
		} else if (ListUtil.isEmpty(desc)) {
			if (asc.size() == 1) {
				return new SingleOrderBy(asc.get(0), SingleOrderBy.ASC);
			} else {
				return new MultiOrderBy(TransList.valueOf(asc, (n) -> new SingleOrderBy(n, SingleOrderBy.ASC)));
			}
		} else {
			MultiOrderBy list = new MultiOrderBy();
			if (null != asc) {
				for (String n : asc) {
					list.add(new SingleOrderBy(n, SingleOrderBy.ASC));
				}
			}
			if (null != desc) {
				for (String n : desc) {
					list.add(new SingleOrderBy(n, SingleOrderBy.DESC));
				}
			}
			return list;
		}
	}

	/**
	 * 属性名转换
	 * 
	 * @param names 属性名称
	 * @return 合并后的属性
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

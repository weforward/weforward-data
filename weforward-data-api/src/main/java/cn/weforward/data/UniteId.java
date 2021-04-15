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
package cn.weforward.data;

import java.util.HashMap;
import java.util.Map;

import cn.weforward.common.crypto.Hex;
import cn.weforward.common.util.StringUtil;

/**
 * 联合标识（以便在整个系统中比较简单实现唯一标识）
 * 
 * 由三部分组成：
 * <p>
 * 类型名（对象/类）
 * <p>
 * 序号（同类型下的唯一标识）
 * <p>
 * 名称/说明
 * <p>
 * 如：“user$12345678!小明”；“12345678”；“12345678!小明”；“user$12345678”
 * 
 * 分隔符尽量避免在URL中传输需要编码（符号“$-_.+!*'(),”不需要编码，但“+”表示空格）
 * 
 * @author liangyi,daibo
 * 
 */
public final class UniteId extends cn.weforward.common.UniteId {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** 空的联合ID */
	public static final UniteId _nil = new UniteId(null);

	/**
	 * 构建联合ID
	 * 
	 * @param uniteid 联合ID格式串，如：user$12345678!小明
	 */
	public UniteId(String uniteid) {
		super(uniteid);
	}

	protected UniteId(int ordinal, String caption, String type) {
		super(ordinal, caption, type);
	}

	protected UniteId(String ordinal, int intOrdinal, String caption, String type) {
		super(ordinal, intOrdinal, caption, type);
	}

	/**
	 * 替换或增加类型部分
	 * 
	 * @param type 新类型
	 * @return 转换后的联合标识
	 */
	public UniteId changeType(Class<?> type) {
		return changeType(getType(type));
	}

	/**
	 * 替换或增加类型部分
	 * 
	 * @param type 新类型
	 * @return 转换后的联合标识
	 */
	public UniteId changeType(String type) {
		if (StringUtil.eq(type, getType())) {
			// 没变化
			return this;
		}
		return new UniteId(getOrdinal(), getIntOrdinal(), getCaption(), type);
	}

	/**
	 * 替换或增加描述部分
	 * 
	 * @param caption 新描述
	 * @return 转换后的联合标识
	 */
	public UniteId changeCaption(String caption) {
		if (StringUtil.eq(caption, getCaption())) {
			// 没变化
			return this;
		}
		return new UniteId(getOrdinal(), getIntOrdinal(), caption, getType());
	}

	/**
	 * 联合标识是否为空
	 * 
	 * @param unid 联合标识
	 * @return true/false
	 */
	public static final boolean isEmtpy(UniteId unid) {
		return (null == unid || 0 == unid.m_Unite.length());
	}

	/**
	 * 比较方法
	 * 
	 * @param id1 id对象
	 * @param id2 id对象
	 * @return true/false
	 */
	public static final boolean equals(String id1, String id2) {
		return getId(id1).equals(getId(id2));
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param uniteid 联合标识串
	 * @return 联合标识对象
	 */
	public static final UniteId valueOf(String uniteid) {
		if (null == uniteid || 0 == uniteid.length()) {
			return _nil;
		}
		return new UniteId(uniteid);
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal 序号部分
	 * @param type    类型名部分
	 * @param caption 名称/描述部分
	 * @return 联合标识
	 */
	public static final UniteId valueOf(String ordinal, String type, String caption) {
		return new UniteId(ordinal, 0, caption, type);
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal    序号部分
	 * @param intOrdinal 整数序号
	 * @param type       类型名部分
	 * @param caption    名称/描述部分
	 * @return 联合标识
	 */
	public static final UniteId valueOf(String ordinal, int intOrdinal, String type, String caption) {
		return new UniteId(ordinal, intOrdinal, caption, type);
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal 序号部分
	 * @param type    类型名部分
	 * @param caption 名称/描述部分
	 * @return 联合标识
	 */
	public static final UniteId valueOf(int ordinal, String type, String caption) {
		return new UniteId(ordinal, caption, type);
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal 序号部分
	 * @param type    对象类
	 * @return 联合标识
	 */
	public static final UniteId valueOf(String ordinal, Class<?> type) {
		return new UniteId(ordinal, 0, null, getType(type));
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal    序号部分
	 * @param intOrdinal 整数序号
	 * @param type       对象类
	 * @return 联合标识
	 */
	public static final UniteId valueOf(String ordinal, int intOrdinal, Class<?> type) {
		if (StringUtil.isEmpty(ordinal) && 0 == intOrdinal) {
			return _nil;
		}
		return new UniteId(ordinal, intOrdinal, null, getType(type));
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal 序号部分
	 * @param type    对象类
	 * @return 联合标识
	 */
	public static final UniteId valueOf(int ordinal, Class<?> type) {
		return new UniteId(ordinal, null, getType(type));
	}

	/**
	 * 由类型及数值序号组成联合标识串
	 * 
	 * @param type    类型
	 * @param ordinal 序号
	 * @return 标识串
	 */
	public static final String getUniteId(String type, int ordinal) {
		return getUniteId(ordinal, null, type);
	}

	/**
	 * 由类型及数值序号组成联合标识串
	 * 
	 * @param type    类型
	 * @param ordinal 序号
	 * @return 标识串
	 */
	public static final String getUniteId(Class<?> type, int ordinal) {
		return getUniteId(ordinal, null, getType(type));
	}

	/**
	 * 由类型及序号组成联合标识串
	 * 
	 * @param type    类型
	 * @param ordinal 序号
	 * @return 标识串
	 */
	public static final String getUniteId(Class<?> type, String ordinal) {
		return getUniteId(ordinal, null, getType(type));
	}

	/**
	 * 由数值序号及名称/说明组成联合标识串
	 * 
	 * @param ordinal 序号
	 * @param caption 说明
	 * @return 标识串
	 */
	public static final String getUniteId(int ordinal, String caption) {
		return getUniteId(Hex.toHex32(ordinal), caption, null);
	}

	/**
	 * 由数值序号、名称/说明及类型组成联合标识串
	 * 
	 * @param ordinal 序号
	 * @param caption 说明
	 * @param type    类型
	 * @return 标识串
	 */
	public static final String getUniteId(int ordinal, String caption, String type) {
		StringBuilder sb = new StringBuilder();
		if (null != type && type.length() > 0) {
			sb.append(type);
			sb.append(TYPE_SPEARATOR);
		}
		Hex.toHexFixed(ordinal, sb);
		if (null != caption) {
			sb.append(UNITE_SPEARATOR);
			sb.append(caption);
		}
		return sb.toString();
	}

	/**
	 * 由序号、名称/说明及类型组成联合标识串
	 * 
	 * @param ordinal 序号
	 * @param caption 说明
	 * @param type    类型
	 * @return 标识串
	 */
	public static final String getUniteId(String ordinal, String caption, String type) {
		StringBuilder sb = new StringBuilder();
		if (null != type && type.length() > 0) {
			sb.append(type);
			sb.append(TYPE_SPEARATOR);
		}
		sb.append(ordinal);
		if (null != caption) {
			sb.append(UNITE_SPEARATOR);
			sb.append(caption);
		}
		return sb.toString();
	}

	/**
	 * 取得类型部分
	 * 
	 * @param uniteid 联合id
	 * @return 标识串
	 */
	public static final String getType(String uniteid) {
		return UniteId.valueOf(uniteid).getType();
	}

	/**
	 * 由对象类型取得类型名
	 * 
	 * @param classOf 类
	 * @return 类型
	 */
	public static final String getType(Class<?> classOf) {
		return (null == classOf) ? null : getSimpleName(classOf);
	}

	/**
	 * 返回序号及类型部分的标识串（不包含名称/描述部分），与getUuid()方法等同
	 * 
	 * @param uniteid 联合标识串
	 * @return 标识串
	 */
	public static final String getId(String uniteid) {
		return UniteId.valueOf(uniteid).getId();
	}

	/**
	 * 返回序号及类型部分的标识串（不包含名称/描述部分），与getId()方法等同
	 * 
	 * @param uniteid 联合标识串
	 * @return 标识串
	 */
	public static final String getUuid(String uniteid) {
		return UniteId.valueOf(uniteid).getId();
	}

	/**
	 * 取得序号部分（数值型）
	 * 
	 * @param uniteid 联合标识串
	 * @return 数值型
	 */
	public static final int getIntOrdinal(String uniteid) {
		return UniteId.valueOf(uniteid).getIntOrdinal();
	}

	/**
	 * 只返回序号部分
	 * 
	 * @param uniteid 联合标识串
	 * @return 标识串
	 */
	public static final String getOrdinal(String uniteid) {
		return UniteId.valueOf(uniteid).getOrdinal();
	}

	/**
	 * 取得名称/说明部分
	 * 
	 * @param uniteid 联合标识串
	 * @return 名称
	 */
	public static final String getCaption(String uniteid) {
		return UniteId.valueOf(uniteid).getCaption();
	}

	/**
	 * 修正标识串为只有ID部分（类型及序号），若没有类型部分则把type补充上去
	 * 
	 * @param unitid 标识串
	 * @param clazz  类型
	 * @return 调整后的联合标识
	 */
	public static final UniteId fixId(String unitid, Class<?> clazz) {
		return fixId(unitid, getType(clazz));
	}

	/**
	 * 修正标识串为只有ID部分（类型及序号），若没有类型部分则把type补充上去
	 * 
	 * @param unitid 标识串
	 * @param type   类型名
	 * @return 调整后的联合标识
	 */
	public static final UniteId fixId(String unitid, String type) {
		if (-1 != unitid.indexOf(TYPE_SPEARATOR)) {
			// 已有分隔符
			return valueOf(unitid);
		}
		int idx = unitid.indexOf(UNITE_SPEARATOR);
		if (-1 == idx) {
			// 没找到TYPE_SPEARATOR及UNITE_SPEARATOR加上type即可
			return valueOf(unitid, type, null);
		}

		// 找到有UNITE_SPEARATOR去除caption部分
		return valueOf(unitid.substring(0, idx), type, null);
	}

	/**
	 * 替换类型部分（若标识串中有的话）
	 * 
	 * @param uniteid 联合标识串
	 * @param type    类型
	 * @return 替换后的联合标识串
	 */
	public static final String changeType(String uniteid, String type) {
		int idx = uniteid.indexOf(TYPE_SPEARATOR);
		if (-1 == idx) {
			// 没找到TYPE_SPEARATOR，直接加上type就行了
			return getUniteId(uniteid, null, type);
		}
		return (type + uniteid.substring(idx));
	}

	/**
	 * 替换类型部分（若标识串中有的话）
	 * 
	 * @param ordinal 序号
	 * @param type    对象类
	 * @return 替换后的联合标识串
	 */
	public static final String changeType(String ordinal, Class<?> type) {
		return changeType(ordinal, getType(type));
	}

	/**
	 * 编码序号中可能存在的分隔符
	 * 
	 * @param ordinal 编码前的序号
	 * @see #unescapeOrdinal(String)
	 * @return 编码后的序号
	 */
	public static final String escapeOrdinal(String ordinal) {
		return ordinal.replace(TYPE_SPEARATOR, ESCAPE);
	}

	/**
	 * 还原序号中可能存在的分隔符
	 * 
	 * @see #escapeOrdinal(String)
	 * @param ordinal 编码后的序号
	 * @return 解码后的序号
	 */
	public static final String unescapeOrdinal(String ordinal) {
		return ordinal.replace(ESCAPE, TYPE_SPEARATOR);
	}

	/**
	 * 取得类用于标识/映射对象的短名（主要代替Class.getSimpleName，性能高于其将近10倍）
	 * 
	 * @param clazz 类，如 {@link cn.weforward.data.UniteId}
	 * @return 短名，如 UniteId
	 */
	public static final String getSimpleName(Class<?> clazz) {
		String name;
		Map<Class<?>, String> cache = _SimpleNameCache;
		name = cache.get(clazz);
		if (null == name) {
			// synchronized (Metatype.class) {
			name = clazz.getSimpleName();
			cache = new HashMap<Class<?>, String>(cache);
			cache.put(clazz, name);
			_SimpleNameCache = cache;
			// }
		}
		return name;
	}

	@Override
	public String toString() {
		return m_Unite;
	}

	public String stringValue() {
		return m_Unite;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (null == obj) {
			return false;
		}
		if (obj instanceof UniteId) {
			return getId().equals(((UniteId) obj).getId());
		}
		return getId().equals(getId(obj.toString()));
	}

}

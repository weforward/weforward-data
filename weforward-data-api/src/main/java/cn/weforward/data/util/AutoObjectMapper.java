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
package cn.weforward.data.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.sys.StackTracer;
import cn.weforward.common.util.NumberUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.UniteId;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtBoolean;
import cn.weforward.protocol.datatype.DtDate;
import cn.weforward.protocol.datatype.DtList;
import cn.weforward.protocol.datatype.DtNumber;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.datatype.DtString;
import cn.weforward.protocol.exception.ObjectMappingException;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.ext.ObjectMapperSet;
import cn.weforward.protocol.support.AbstractObjectMapper;
import cn.weforward.protocol.support.datatype.SimpleDtBoolean;
import cn.weforward.protocol.support.datatype.SimpleDtDate;
import cn.weforward.protocol.support.datatype.SimpleDtList;
import cn.weforward.protocol.support.datatype.SimpleDtNumber;
import cn.weforward.protocol.support.datatype.SimpleDtObject;
import cn.weforward.protocol.support.datatype.SimpleDtString;

/**
 * 映射表扩展
 * 
 * @author daibo
 *
 */
public abstract class AutoObjectMapper<E> extends AbstractObjectMapper<E> {
	/** 索引属性的深度 */
	public static final int INDEX_DEEPIN = NumberUtil.toInt(System.getProperty("cn.weforward.data.util.indexdeepin"),
			6);
	/** 日志 */
	private static final Logger _Logger = LoggerFactory.getLogger(AutoObjectMapper.class);

	/** 空数组 */
	protected static Object[] _EMPTY = new Object[] {};
	/** 对应类 */
	protected Class<E> m_Clazz;
	/** 用于构造对象的参数列表 */
	protected Object[] m_Parameters;
	/** 构造器 */
	protected Constructor<E> m_Constructor;

	private static final String MAP_KEY_NAME = "_key";
	private static final String MAP_VALUE_NAME = "_value";

	protected AutoObjectMapper(Class<E> clazz, Object[] parameters) {
		m_Clazz = clazz;
		m_Constructor = getConstructor(clazz, parameters);
		m_Parameters = parameters;
	}

	@Override
	public String getName() {
		return UniteId.getSimpleName(m_Clazz);
	}

	/**
	 * 获取索引名属性名称
	 * 
	 * @return 属性
	 */
	public Enumeration<String> getIndexAttributeNames() {
		return getIndexAttributeNames(INDEX_DEEPIN);
	}

	/**
	 * 获取索引名属性名称
	 * 
	 * @param maxdeepin 属性的最大深度(层级) 
	 * @return 属性
	 */
	public abstract Enumeration<String> getIndexAttributeNames(int maxdeepin);

	@SuppressWarnings("unchecked")
	public static <E> Constructor<E> getConstructor(Class<E> clazz, Object[] parameters) {
		try {
			Constructor<E>[] cs = (Constructor<E>[]) clazz.getDeclaredConstructors();
			// 先尝试找到与参数匹配的构造方法
			if (null != parameters && parameters.length > 0) {
				for (Constructor<E> c : cs) {
					int mod = c.getModifiers();
					if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
						// 只使用public或protected的构造方法
						continue;
					}
					Class<?>[] ps = c.getParameterTypes();
					if (ps.length != parameters.length) {
						continue;
					}
					int i;
					for (i = ps.length - 1; i >= 0; i--) {
						if (!ps[i].isInstance(parameters[i])) {
							break;
						}
					}
					if (i < 0) {
						c.setAccessible(true);
						return c;
					}
				}
			}

			// 再尝试取得无参数构造
			for (Constructor<E> c : cs) {
				int mod = c.getModifiers();
				if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
					// 只使用public或protected的构造方法
					continue;
				}
				if (0 == c.getParameterTypes().length) {
					// 未有合适参数的构造方法，只好使用无参数构造方法
					if (_Logger.isDebugEnabled() && null != parameters && parameters.length > 0) {
						_Logger.debug(clazz + " 未有合适参数的构造方法，只好使用无参数构造方法");
					}
					c.setAccessible(true);
					return c;
				}
			}
		} catch (SecurityException e) {
			if (_Logger.isDebugEnabled()) {
				_Logger.debug(StackTracer.printStackTrace(e, null).toString());
			}
		}
		// 没有任何合适的构造方法及无参数构造方法
		if (null == parameters || 0 == parameters.length) {
			throw new ObjectMappingException(clazz + " 没有无参数的构造方法");
		} else {
			throw new ObjectMappingException(clazz + " 没有适合参数(" + Arrays.asList(parameters) + ")或无参数的构造方法");
		}
	}

	/*
	 * 构建对象
	 */
	protected Object newObject() {
		E object;
		try {
			if (null == m_Parameters || 0 == m_Constructor.getParameterTypes().length) {
				object = m_Constructor.newInstance();
			} else {
				object = m_Constructor.newInstance(m_Parameters);
			}
		} catch (InstantiationException e) {
			throw new ObjectMappingException(e);
		} catch (IllegalAccessException e) {
			throw new ObjectMappingException(e);
		} catch (InvocationTargetException e) {
			throw new ObjectMappingException(e);
		}
		return object;
	}

	@SuppressWarnings("unchecked")
	protected DtBase toBase(Object val) {
		if (null == val) {
			return null;
		} else if (val instanceof DtBase) {
			return (DtBase) val;
		} else if (val instanceof Boolean) {
			return SimpleDtBoolean.valueOf((Boolean) val);
		} else if (val instanceof String) {
			return SimpleDtString.valueOf(StringUtil.toString(val));
		} else if (val instanceof BigInteger) {
			return SimpleDtString.valueOf(StringUtil.toString(val));
		} else if (val instanceof BigDecimal) {
			return SimpleDtString.valueOf(StringUtil.toString(val));
		} else if (val instanceof Byte) {
			return SimpleDtNumber.valueOf((byte) val);
		} else if (val instanceof Short) {
			return SimpleDtNumber.valueOf((short) val);
		} else if (val instanceof Integer) {
			return SimpleDtNumber.valueOf((int) val);
		} else if (val instanceof Long) {
			return SimpleDtNumber.valueOf((long) val);
		} else if (val instanceof Float) {
			return SimpleDtNumber.valueOf((float) val);
		} else if (val instanceof Double) {
			return SimpleDtNumber.valueOf((double) val);
		} else if (val instanceof Date) {
			return SimpleDtDate.valueOf((Date) val);
		} else if (val.getClass().isArray()) {
			List<DtBase> list = new ArrayList<>();
			int l = Array.getLength(val);
			for (int i = 0; i < l; i++) {
				list.add(toBase(Array.get(val, i)));
			}
			return SimpleDtList.valueOf(list);
		} else if (val instanceof Collection<?>) {
			List<DtBase> list = new ArrayList<>();
			for (Object v : (Collection<?>) val) {
				list.add(toBase(v));
			}
			return SimpleDtList.valueOf(list);
		} else if (val instanceof Map<?, ?>) {
			List<DtBase> list = new ArrayList<>();
			for (Map.Entry<?, ?> e : ((Map<?, ?>) val).entrySet()) {
				SimpleDtObject entry = new SimpleDtObject();
				entry.put(MAP_KEY_NAME, toBase(e.getKey()));
				entry.put(MAP_VALUE_NAME, toBase(e.getValue()));
				list.add(entry);
			}
			return SimpleDtList.valueOf(list);
		} else {
			ObjectMapperSet set = getMappers();
			Class<?> clazz = val.getClass();
			ObjectMapper<Object> m = null;
			if (null != set) {
				m = (ObjectMapper<Object>) set.getObjectMapper(clazz);
			}
			if (null == m) {
				m = openMapper(val.getClass());
			}
			return m.toDtObject(val);
		}
	}

	/*
	 * 从参数中解析出对象
	 * 
	 */
	@SuppressWarnings("unchecked")
	protected Object fromBase(Class<?> clazz, List<Class<?>> components, DtBase params) {
		if (null == params) {
			if (boolean.class.isAssignableFrom(clazz)) {
				return false;
			}
			if (byte.class.isAssignableFrom(clazz)) {
				return 0;
			}
			if (short.class.isAssignableFrom(clazz)) {
				return 0;
			}
			if (int.class.isAssignableFrom(clazz)) {
				return 0;
			}
			if (long.class.isAssignableFrom(clazz)) {
				return 0l;
			}
			if (float.class.isAssignableFrom(clazz)) {
				return 0f;
			}
			if (double.class.isAssignableFrom(clazz)) {
				return 0d;
			}
			return null;
		}
		if (String.class.isAssignableFrom(clazz)) {
			if (params instanceof DtString) {
				return ((DtString) params).value();
			} else {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}
		} else if (BigInteger.class.isAssignableFrom(clazz)) {
			if (params instanceof DtString) {
				return new BigInteger(((DtString) params).value());
			} else if (params instanceof DtNumber) {
				return BigInteger.valueOf(((DtNumber) params).valueLong());
			} else {
				throw new UnsupportedOperationException("不支持的类型转换:" + params + "=>" + clazz);
			}
		} else if (BigDecimal.class.isAssignableFrom(clazz)) {
			if (params instanceof DtString) {
				return new BigDecimal(((DtString) params).value());
			} else if (params instanceof DtNumber) {
				return BigDecimal.valueOf(((DtNumber) params).valueDouble());
			} else {
				throw new UnsupportedOperationException("不支持的类型转换:" + params + "=>" + clazz);
			}
		} else if (Byte.class.isAssignableFrom(clazz) || byte.class.isAssignableFrom(clazz)) {
			if (params instanceof DtNumber) {
				return (byte) ((DtNumber) params).valueInt();
			} else if (params instanceof DtString) {
				return Byte.valueOf(((DtString) params).value());
			} else {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}
		} else if (Short.class.isAssignableFrom(clazz) || short.class.isAssignableFrom(clazz)) {
			if (params instanceof DtNumber) {
				return (short) ((DtNumber) params).valueInt();
			} else if (params instanceof DtString) {
				return Short.valueOf(((DtString) params).value());
			} else {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}
		} else if (Integer.class.isAssignableFrom(clazz) || int.class.isAssignableFrom(clazz)) {
			if (params instanceof DtNumber) {
				return ((DtNumber) params).valueInt();
			} else if (params instanceof DtString) {
				return Integer.valueOf(((DtString) params).value());
			} else {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}
		} else if (Float.class.isAssignableFrom(clazz) || float.class.isAssignableFrom(clazz)) {
			if (params instanceof DtNumber) {
				return (float) ((DtNumber) params).valueDouble();
			} else if (params instanceof DtString) {
				return Float.valueOf(((DtString) params).value());
			} else {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}
		} else if (Double.class.isAssignableFrom(clazz) || double.class.isAssignableFrom(clazz)) {
			if (params instanceof DtNumber) {
				return ((DtNumber) params).valueDouble();
			} else if (params instanceof DtString) {
				return Double.valueOf(((DtString) params).value());
			} else {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}
		} else if (Long.class.isAssignableFrom(clazz) || long.class.isAssignableFrom(clazz)) {
			if (params instanceof DtNumber) {
				return ((DtNumber) params).valueLong();
			} else if (params instanceof DtString) {
				return Long.valueOf(((DtString) params).value());
			} else {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}
		} else if (Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) {
			if (params instanceof DtBoolean) {
				return ((DtBoolean) params).value();
			} else {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}
		} else if (clazz.isArray()) {
			if (params instanceof DtList) {
				Class<?> component = null;
				if (null == components || components.isEmpty()) {
					component = clazz.getComponentType();
				} else {
					components = components.subList(1, components.size());
				}
				DtList items = (DtList) params;
				Object elements = Array.newInstance(component, items.size());
				for (int i = 0; i < items.size(); i++) {
					Object v = fromBase(component, components, items.getItem(i));
					Array.set(elements, i, v);
				}
				return elements;
			} else {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}

		} else if (Date.class == clazz) {
			if (params instanceof DtDate) {
				return ((DtDate) params).valueDate();
			} else if (params instanceof DtString) {
				try {
					return DtDate.Formater.parse(((DtString) params).value());
				} catch (ParseException e) {
					throw new ObjectMappingException("解析数据异常:" + params);
				}
			} else {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}
		} else if (Collection.class.isAssignableFrom(clazz)) {
			Class<?> component = null;
			if (null == components || components.isEmpty()) {
				throw new ObjectMappingException("未指定component:" + params + "=>" + clazz);
			} else {
				component = components.get(0);
				if (components.size() > 1) {
					components = components.subList(1, components.size());
				} else {
					components = Collections.emptyList();
				}
			}
			if (!(params instanceof DtList)) {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}
			DtList items = (DtList) params;
			if (Collection.class == clazz || AbstractCollection.class == clazz || List.class == clazz
					|| AbstractList.class == clazz || ArrayList.class == clazz) {
				ArrayList<Object> list = new ArrayList<Object>();
				for (int i = 0; i < items.size(); i++) {
					list.add(fromBase(component, components, items.getItem(i)));
				}
				return list;
			}
			if (Set.class == clazz || AbstractSet.class == clazz || HashSet.class == clazz) {
				HashSet<Object> list = new HashSet<Object>();
				for (int i = 0; i < items.size(); i++) {
					list.add(fromBase(component, components, items.getItem(i)));
				}
				return list;
			}
			throw new ObjectMappingException("不支持的列表类型:" + clazz);
		} else if (Map.class.isAssignableFrom(clazz)) {
			Class<?> keyComponent = null;
			Class<?> valueComponent = null;
			if (null == components || components.size() < 2) {
				throw new ObjectMappingException("未指定component:" + params + "=>" + clazz);
			} else {
				keyComponent = components.get(0);
				valueComponent = components.get(1);
				if (components.size() > 2) {
					components = components.subList(2, components.size());
				} else {
					components = Collections.emptyList();
				}
			}
			if (!(params instanceof DtList)) {
				throw new ObjectMappingException("不支持的类型转换:" + params + "=>" + clazz);
			}
			DtList items = (DtList) params;
			if (Map.class == clazz || AbstractMap.class == clazz || HashMap.class == clazz
					|| ConcurrentMap.class == clazz || ConcurrentHashMap.class == clazz) {
				Map<Object, Object> map;
				if (ConcurrentMap.class.isAssignableFrom(clazz)) {
					map = new ConcurrentHashMap<Object, Object>();
				} else {
					map = new HashMap<Object, Object>();
				}
				for (int i = 0; i < items.size(); i++) {
					DtObject dt = (DtObject) items.getItem(i);
					map.put(fromBase(keyComponent, Collections.emptyList(), dt.getAttribute(MAP_KEY_NAME)),
							fromBase(valueComponent, components, dt.getAttribute(MAP_VALUE_NAME)));
				}
				return map;
			}
			throw new ObjectMappingException("不支持的列表类型:" + clazz);
		} else

		{
			ObjectMapper<Object> m = null;
			ObjectMapperSet set = getMappers();
			if (null != set) {
				m = (ObjectMapper<Object>) set.getObjectMapper(clazz);
			}
			if (null == m) {
				m = (ObjectMapper<Object>) openMapper(clazz);
			}
			return m.fromDtObject((DtObject) params);
		}
	}

	/**
	 * 装箱
	 * 
	 * <pre>
	 * 基于规则为：调用目标对象的valueOf(xxx)静态方法生成目标对象
	 * 如
	 * String=&gt;UniteId
	 * 则调用
	 * UniteId.valueOf(String)方法
	 * 
	 * 如
	 * Double=&gt;UniteId
	 * 则调用
	 *	UniteId.valueOf(Double)方法
	 * </pre>
	 * 
	 * @param sourceObject 转换前源对象
	 * @param sourceType   转换前源类
	 * @param targetType   转换后的对象
	 * @return 对象
	 */
	protected static Object boxing(Object sourceObject, Class<?> sourceType, Class<?> targetType) {
		if (null == sourceObject) {
			return null;
		}
		Method method;
		String methodName = "valueOf";
		try {
			method = targetType.getMethod(methodName, sourceType);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new ObjectMappingException(sourceType + "无" + methodName + "(" + sourceType + ")方法");
		}
		Object object;
		try {
			object = method.invoke(null, sourceObject);
		} catch (InvocationTargetException e) {
			Throwable target = e.getTargetException();
			if (target instanceof RuntimeException) {
				throw (RuntimeException) target;
			}
			throw new ObjectMappingException("调用" + UniteId.getSimpleName(targetType) + "." + method + "方法异常", target);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new ObjectMappingException("调用" + UniteId.getSimpleName(targetType) + "." + method + "方法异常", e);
		}
		return object;
	}

	/**
	 * 拆箱
	 * 
	 * <pre>
	 * 基于规则为：调用源对象的XXXValue方法转换成目标对象
	 * 如
	 * UniteId=&gt;String
	 * 则调用
	 * UniteId.stringValue()方法
	 * 
	 * 如
	 * UniteId=&gt;Double
	 * 则调用
	 * UniteId.doubleValue()方法
	 * </pre>
	 * 
	 * @param sourceObject 转换前源对象
	 * @param sourceType   转换前源类
	 * @param targetType   转换后的对象
	 * @return 对象
	 */
	protected static Object unboxing(Object sourceObject, Class<?> sourceType, Class<?> targetType) {
		if (null == sourceObject) {
			return null;
		}
		Method method;
		String name = UniteId.getSimpleName(targetType);
		String methodName = Character.toLowerCase(name.charAt(0)) + name.substring(1) + "Value";
		try {
			method = sourceType.getDeclaredMethod(methodName);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new ObjectMappingException(sourceType + "无" + methodName + "方法");
		}
		Object object;
		try {
			object = method.invoke(sourceObject);
		} catch (InvocationTargetException e) {
			Throwable target = e.getTargetException();
			if (target instanceof RuntimeException) {
				throw (RuntimeException) target;
			}
			throw new ObjectMappingException("调用" + UniteId.getSimpleName(sourceType) + "." + method + "方法异常", target);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new ObjectMappingException("调用" + UniteId.getSimpleName(sourceType) + "." + method + "方法异常", e);
		}
		return object;
	}

	/* 对象转数据类型 */
	protected abstract ObjectMapper<Object> openMapper(Class<?> clazz);

}

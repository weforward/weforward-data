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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import cn.weforward.common.util.ClassUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.annotation.Index;
import cn.weforward.data.annotation.ResourceExt;
import cn.weforward.data.annotation.Transient;
import cn.weforward.data.persister.Condition;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtList;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.exception.ObjectMappingException;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.ext.ObjectMapperSet;
import cn.weforward.protocol.support.NamingConverter;
import cn.weforward.protocol.support.SimpleObjectMapperSet;
import cn.weforward.protocol.support.datatype.FriendlyList;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 基于 get/set方法的映射表
 * 
 * @author daibo
 *
 * @param <E> 对象
 */
public class MethodMapper<E> extends AutoObjectMapper<E> {

	/** 映射表集合 */
	protected final static SimpleObjectMapperSet SET = new SimpleObjectMapperSet();
	/** 类映射 */
	private final static ConcurrentMap<Class<?>, Map<String, Method>> CLASS_SET_MAP = new ConcurrentHashMap<Class<?>, Map<String, Method>>();
	/** 类映射 */
	private final static ConcurrentMap<Class<?>, Map<String, Method>> CLASS_GET_MAP = new ConcurrentHashMap<Class<?>, Map<String, Method>>();

	/**
	 * 构造映射表
	 * 
	 * @param <E>   对象
	 * @param clazz 类
	 * @return 映射表
	 */
	public static <E> ObjectMapper<E> valueOf(Class<E> clazz) {
		return valueOf(clazz, (ObjectMapperSet) null);
	}

	/**
	 * 构造映射表
	 * 
	 * @param <E>     对象
	 * @param clazz   类
	 * @param mappers 映射表集合
	 * @return 映射表
	 */
	public static <E> ObjectMapper<E> valueOf(Class<E> clazz, ObjectMapperSet mappers) {
		return valueOf(clazz, mappers, _EMPTY);
	}

	/**
	 * 构造映射表
	 * 
	 * @param <E>    对象
	 * @param clazz  类
	 * @param params 参数
	 * @return 映射表
	 */
	public static <E> ObjectMapper<E> valueOf(Class<E> clazz, Object... params) {
		return valueOf(clazz, null, params);
	}

	/**
	 * 构造映射表
	 * 
	 * @param <E>     对象
	 * @param clazz   类
	 * @param mappers 映射表
	 * @param params  参数
	 * @return 映射表
	 */
	public static <E> ObjectMapper<E> valueOf(Class<E> clazz, ObjectMapperSet mappers, Object... params) {
		ObjectMapper<E> m = SET.getObjectMapper(clazz);
		if (null != m) {
			return m;
		}
		synchronized (SET) {
			m = SET.getObjectMapper(clazz);
			if (null != m) {
				return m;
			}
			MethodMapper<E> mapper = new MethodMapper<E>(clazz, params);
			if (null != mappers) {
				mapper.setMappers(mappers);
			}
			m = mapper;
		}
		return m;
	}

	private static Map<String, Method> getGetMethods(Class<?> clazz) {
		if (String.class == clazz || Date.class == clazz) {
			return Collections.emptyMap();
		}
		Map<String, Method> map = CLASS_GET_MAP.get(clazz);
		if (null != map) {
			return map;
		}
		map = new HashMap<>();
		Map<String, Method> old = CLASS_GET_MAP.putIfAbsent(clazz, map);
		if (null != old) {
			return old;
		}
		Method[] ms = clazz.getMethods();
		for (Method m : ms) {
			if (m.getParameterTypes().length > 0) {
				continue;// 有参数
			}
			String name = findGetName(m);
			if (StringUtil.isEmpty(name)) {
				continue;
			}
			map.put(name, m);
		}
		return map;
	}

	private static String findGetName(Method method) {
		String name = method.getName();
		if (StringUtil.eq(name, "getClass")) {
			return null;
		}
		boolean isGet = name.startsWith("get");
		boolean isIs = name.startsWith("is");
		if (isGet) {
			return NamingConverter.camelToWf(Character.toLowerCase(name.charAt(3)) + name.substring(4));
		} else if (isIs) {
			return NamingConverter.camelToWf(Character.toLowerCase(name.charAt(2)) + name.substring(3));
		} else {
			return null;
		}
	}

	private static Map<String, Method> getSetMethods(Class<?> clazz) {
		if (String.class == clazz || Date.class == clazz) {
			return Collections.emptyMap();
		}
		Map<String, Method> map = CLASS_SET_MAP.get(clazz);
		if (null != map) {
			return map;
		}
		map = new HashMap<>();
		Map<String, Method> old = CLASS_SET_MAP.putIfAbsent(clazz, map);
		if (null != old) {
			return old;
		}
		Method[] ms = clazz.getMethods();
		for (Method m : ms) {
			if (m.getParameterTypes().length != 1) {
				continue;
			}
			String name = findSetName(m);
			if (StringUtil.isEmpty(name)) {
				continue;
			}
			map.put(name, m);
		}
		return map;
	}

	private static String findSetName(Method method) {
		String name = method.getName();
		boolean isSet = name.startsWith("set");
		if (isSet) {
			return NamingConverter.camelToWf(Character.toLowerCase(name.charAt(3)) + name.substring(4));
		} else {
			return null;
		}
	}

	protected MethodMapper(Class<E> clazz, Object[] params) {
		super(clazz, params);
	}

	@Override
	public DtObject toDtObject(E object) throws ObjectMappingException {
		SimpleDtObject result = new SimpleDtObject();
		for (Map.Entry<String, Method> entry : getGetMethods().entrySet()) {
			Method method = entry.getValue();
			if (method.isAnnotationPresent(Transient.class)) {
				continue;
			}
			String name = entry.getKey();
			Class<?> resourceType = null;
			ResourceExt rese = method.getAnnotation(ResourceExt.class);
			Resource res = method.getAnnotation(Resource.class);
			if (null != res) {
				resourceType = res.type();
			} else if (null != rese) {
				resourceType = rese.type();
			}
			Class<?> returnType = method.getReturnType();
			if (resourceType == null || resourceType == Object.class) {
				resourceType = returnType;
			}
			Object v;
			try {
				v = (Object) method.invoke(object);
			} catch (InvocationTargetException e) {
				Throwable target = e.getTargetException();
				if (target instanceof RuntimeException) {
					throw (RuntimeException) target;
				}
				throw new ObjectMappingException("调用" + getName() + "." + method.getName() + "方法异常", target);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new ObjectMappingException("调用" + getName() + "." + method.getName() + "方法异常", e);
			}
			if (returnType != resourceType) {
				v = unboxing(v, returnType, resourceType);
			}
			DtBase dt = toBase(v);
			if (null != dt) {
				result.put(name, dt);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E fromDtObject(DtObject obj) throws ObjectMappingException {
		Object object = newObject();
		for (Map.Entry<String, Method> entry : getSetMethods().entrySet()) {
			String name = entry.getKey();
			DtBase base = obj.getAttribute(name);
			if (null == base) {
				continue;
			}
			Method method = entry.getValue();
			Class<?> resourceType = null;
			ResourceExt rese = method.getAnnotation(ResourceExt.class);
			Resource res = method.getAnnotation(Resource.class);
			if (null != res) {
				resourceType = res.type();
			} else if (null != rese) {
				resourceType = rese.type();
			}
			Class<?> methodType = method.getParameterTypes()[0];
			if (resourceType == null || resourceType == Object.class) {
				resourceType = methodType;
			}
			List<Class<?>> methodComponent = Collections.emptyList();
			if (null != rese) {
				if (null != rese.components() && rese.components().length > 0) {
					methodComponent = Arrays.asList(rese.components());
				} else if (null != rese.component() && rese.component() != Object.class) {
					methodComponent = Arrays.asList(rese.component());
				}
			}
			if (methodComponent.isEmpty()) {
				if (Collection.class.isAssignableFrom(methodType)) {
					methodComponent = Arrays.asList(ClassUtil.find(method.getGenericParameterTypes()[0], 0));
				} else if (Map.class.isAssignableFrom(methodType)) {
					methodComponent = Arrays.asList(ClassUtil.find(method.getGenericParameterTypes()[0], 0),
							ClassUtil.find(method.getGenericParameterTypes()[0], 1));
				} else {
					methodComponent = null;
				}
			}
			Object v = fromBase(resourceType, methodComponent, base);
			if (resourceType != methodType) {
				v = boxing(v, resourceType, methodType);
			}
			try {
				method.invoke(object, v);
			} catch (InvocationTargetException e) {
				Throwable target = e.getTargetException();
				if (target instanceof RuntimeException) {
					throw (RuntimeException) target;
				}
				throw new ObjectMappingException("调用" + getName() + "." + method.getName() + "方法异常", target);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new ObjectMappingException("调用" + getName() + "." + method.getName() + "方法异常", e);
			}
		}
		return (E) object;

	}

	@SuppressWarnings("unchecked")
	protected ObjectMapper<Object> openMapper(Class<?> clazz) {
		return (ObjectMapper<Object>) valueOf(clazz);
	}

	private Map<String, Method> getGetMethods() {
		return getGetMethods(m_Clazz);
	}

	private Map<String, Method> getSetMethods() {
		return getSetMethods(m_Clazz);
	}

	/**
	 * 获取实例
	 * 
	 * @param <E>    对象
	 * @param clazz  类
	 * @param object 数据对象
	 * @return 实例
	 */
	public static <E> E getInstance(Class<E> clazz, DtObject object) {
		if (null == object) {
			return null;
		}
		return valueOf(clazz).fromDtObject(object);
	}

	/**
	 * 获取实例列表
	 * 
	 * @param <E>    对象
	 * @param clazz  类
	 * @param dtlist 数据对象列表
	 * @return 实例列表
	 */
	public static <E> List<E> getInstanceList(Class<E> clazz, DtList dtlist) {
		if (null == dtlist) {
			return Collections.emptyList();
		}
		List<E> list = new ArrayList<>(dtlist.size());
		for (int i = 0; i < dtlist.size(); i++) {
			DtBase base = dtlist.getItem(i);
			if (base instanceof DtObject) {
				list.add(valueOf(clazz).fromDtObject((DtObject) base));
			} else {
				throw new UnsupportedOperationException(base + "非对象类型");
			}
		}
		return list;
	}

	/**
	 * 获取实例列表
	 * 
	 * @param <E>    对象
	 * @param clazz  类
	 * @param dtlist 数据对象列表
	 * @return 实例列表
	 */
	public static <E> List<E> getInstanceList(Class<E> clazz, FriendlyList dtlist) {
		if (null == dtlist) {
			return Collections.emptyList();
		}
		List<E> list = new ArrayList<>(dtlist.size());
		for (int i = 0; i < dtlist.size(); i++) {
			list.add(valueOf(clazz).fromDtObject(dtlist.getObject(i)));
		}
		return list;
	}

	@Override
	public Enumeration<String> getIndexAttributeNames(int maxdeepin) {
		List<String> indexs = new ArrayList<>();
		for (Map.Entry<String, Method> e : getGetMethods().entrySet()) {
			findIndex(e.getKey(), e.getValue(), indexs, 0, maxdeepin);
		}
		return new ListEnumeration<String>(indexs);
	}

	private void findIndex(String name, Method method, List<String> indexs, int deepin, int maxdeepin) {
		if (deepin >= maxdeepin) {
			return;
		}
		if (method.isAnnotationPresent(Index.class)) {
			if (!indexs.contains(name)) {
				indexs.add(name);
			}
		}
		for (Map.Entry<String, Method> e : getGetMethods(method.getReturnType()).entrySet()) {
			findIndex(name + Condition.FIELD_SPEARATOR + e.getKey(), e.getValue(), indexs, deepin + 1, maxdeepin);
		}
	}

}

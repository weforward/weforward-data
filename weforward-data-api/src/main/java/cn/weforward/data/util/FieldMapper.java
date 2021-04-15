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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

import cn.weforward.common.util.StringUtil;
import cn.weforward.data.annotation.Index;
import cn.weforward.data.annotation.Inherited;
import cn.weforward.data.annotation.ResourceExt;
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
 * 基于属性映射表
 * 
 * 通过 {@link Resource} 用 {@link ResourceExt} 注释映射 如
 * 
 * <code>
 * 
 * public class User{
 *  &#64;Resource(type=String.class)
 * 	protected UniteId m_Id;
 *  &#64;Resource
 *  protected String m_Name;
 *  &#64;ResourceExt(component=String.class)
 *  protected List&lt;String&gt; m_LoginNames;
 * }
 * </code>
 * 
 * 
 * @author daibo
 *
 * @param <E> 对象
 */
public class FieldMapper<E> extends AutoObjectMapper<E> {
	/** 映射表集合 */
	protected final static SimpleObjectMapperSet SET = new SimpleObjectMapperSet();
	/** 类映射 */
	private final static ConcurrentMap<Class<?>, Map<String, Field>> CLASS_MAP = new ConcurrentHashMap<Class<?>, Map<String, Field>>();

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
	 * @param mappers 映射表集合
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
			FieldMapper<E> mapper = new FieldMapper<E>(clazz, params);
			if (null != mappers) {
				mapper.setMappers(mappers);
			}
			m = mapper;
		}
		return m;
	}

	private static String findName(Field field) {
		String name;
		Resource res = field.getAnnotation(Resource.class);
		if (null == res) {
			ResourceExt rese = field.getAnnotation(ResourceExt.class);
			if (null == rese) {
				return null;
			} else {
				name = rese.name();
			}
		} else {
			name = res.name();
		}
		if (StringUtil.isEmpty(name)) {
			name = field.getName();
		}
		if (name.startsWith("m_")) {
			name = NamingConverter.camelToWf(Character.toLowerCase(name.charAt(2)) + name.substring(3));
		} else {
			name = NamingConverter.camelToWf(name);
		}
		return name;
	}

	private Map<String, Field> getFields(Class<?> clazz) {
		Map<String, Field> map = CLASS_MAP.get(clazz);
		if (null != map) {
			return map;
		}
		map = new HashMap<>();
		Map<String, Field> old = CLASS_MAP.putIfAbsent(clazz, map);
		if (null != old) {
			return old;
		}
		Class<?> loop = clazz;
		while (null != loop) {
			Field[] fs = loop.getDeclaredFields();
			for (Field field : fs) {
				String name = findName(field);
				if (StringUtil.isEmpty(name)) {
					continue;
				}
				map.put(name, field);
			}
			if (loop.isAnnotationPresent(Inherited.class)) {
				loop = loop.getSuperclass();
			} else {
				loop = null;
			}
		}
		return map;
	}

	protected FieldMapper(Class<E> clazz, Object[] parameters) {
		super(clazz, parameters);
	}

	@Override
	public DtObject toDtObject(E object) throws ObjectMappingException {
		SimpleDtObject result = new SimpleDtObject();
		for (Map.Entry<String, Field> entry : getFields().entrySet()) {
			String name = entry.getKey();
			Field field = entry.getValue();

			Class<?> resourceType = null;
			ResourceExt rese = field.getAnnotation(ResourceExt.class);
			Resource res = field.getAnnotation(Resource.class);
			if (null != res) {
				resourceType = res.type();
			} else if (null != rese) {
				resourceType = rese.type();
			}
			Class<?> returnType = field.getType();
			if (resourceType == null || resourceType == Object.class) {
				resourceType = returnType;
			}
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			Object v;
			try {
				v = (Object) field.get(object);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new ObjectMappingException("获取" + getName() + "#" + field.getName() + "属性异常", e);
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
		for (Map.Entry<String, Field> entry : getFields().entrySet()) {
			String name = entry.getKey();
			DtBase base = obj.getAttribute(name);
			if (null == base) {
				continue;
			}
			Field field = entry.getValue();
			Class<?> resourceType = null;
			ResourceExt rese = field.getAnnotation(ResourceExt.class);
			Resource res = field.getAnnotation(Resource.class);
			if (null != res) {
				resourceType = res.type();
			} else if (null != rese) {
				resourceType = rese.type();
			}
			Class<?> methodType = field.getType();
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
			} else if (Collection.class.isAssignableFrom(resourceType)) {
				methodComponent = getComponentsByCollection(field);
			}
			Object v = fromBase(resourceType, methodComponent, base);
			if (resourceType != methodType) {
				v = boxing(v, resourceType, methodType);
			}
			try {
				if (!field.isAccessible()) {
					field.setAccessible(true);
				}
				field.set(object, v);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new ObjectMappingException("设置" + getName() + "#" + field.getName() + "属性异常", e);
			}
		}
		return (E) object;
	}

	// 从泛型声明读取components
	private List<Class<?>> getComponentsByCollection(Field field) {
		Type t = field.getGenericType();
		if (t instanceof ParameterizedType) {
			Type[] actuals = ((ParameterizedType) t).getActualTypeArguments();
			if (1 == actuals.length) {
				if (actuals[0] instanceof Class) {
					return Collections.singletonList((Class<?>) actuals[0]);
				}
			} else if (actuals.length > 1) {
				List<Class<?>> list = new ArrayList<>(actuals.length);
				for (Type a : actuals) {
					if (a instanceof Class) {
						list.add((Class<?>) a);
					} else {
						// 不支持二次泛型嵌套
						break;
					}
				}
				if (list.size() == actuals.length) {
					return list;
				}
			}
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ObjectMapper<Object> openMapper(Class<?> clazz) {
		return (ObjectMapper<Object>) valueOf(clazz);
	}

	private Map<String, Field> getFields() {
		return getFields(m_Clazz);
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
		for (Map.Entry<String, Field> e : getFields().entrySet()) {
			findIndex(e.getKey(), e.getValue(), indexs, 0, maxdeepin);
		}
		return new ListEnumeration<String>(indexs);
	}

	private void findIndex(String name, Field field, List<String> indexs, int deepin, int maxdeepin) {
		if (deepin >= maxdeepin) {
			return;
		}
		if (field.isAnnotationPresent(Index.class)) {
			if (!indexs.contains(name)) {
				indexs.add(name);
			}
		}
		for (Map.Entry<String, Field> e : getFields(field.getType()).entrySet()) {
			findIndex(name + Condition.FIELD_SPEARATOR + e.getKey(), e.getValue(), indexs, deepin + 1, maxdeepin);
		}
	}

}

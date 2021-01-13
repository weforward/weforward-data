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
package cn.weforward.data.persister;

import java.util.Iterator;

import cn.weforward.data.UniteId;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.ext.ObjectMapperSet;

/**
 * 对象持久器集合
 * 
 * @author liangyi
 * 
 */
public interface PersisterSet {
	/**
	 * 取得持久器集合中的名称项集合
	 * 
	 * @return 名称集合
	 */
	Iterator<String> getNames();

	/**
	 * 由ID取得对象
	 * 
	 * @param <E> 持久对象类型
	 * @param id  对象ID
	 * @return 找到返回对象实例，否则为null
	 */
	<E extends Persistent> E get(String id);

	/**
	 * 由联合ID取得对象
	 * 
	 * @param <E>  持久对象类型
	 * @param uuid 对象联合ID
	 * @return 找到返回对象实例，否则为null
	 */
	<E extends Persistent> E get(UniteId uuid);

	/**
	 * 返回对象的持久器
	 * 
	 * @param <E>  持久对象类型
	 * @param name 名称
	 * @return 对应于名称的持久器
	 */
	<E extends Persistent> Persister<E> getPersister(String name);

	/**
	 * 返回对象的持久器
	 * 
	 * @param <E>   持久对象类型
	 * @param clazz 对象类型
	 * @return 对应的持久器
	 */
	<E extends Persistent> Persister<E> getPersister(Class<E> clazz);

	/**
	 * 返回对象的持久器
	 * 
	 * @param <E>    持久对象类型
	 * @param object 要寻找持久器的对象
	 * @return 适用于此对象的持久器
	 */
	<E extends Persistent> Persister<E> getPersister(E object);

	/**
	 * 取得所知的映射器集
	 * 
	 * @return 映射器集
	 */
	ObjectMapperSet getMappers();

	/**
	 * 取得Class相应的映射器
	 * 
	 * @param <E>   持久对象类型
	 * @param clazz 对象类
	 * @return 对应的映射器
	 */
	<E> ObjectMapper<E> getMapper(Class<E> clazz);

	/**
	 * 注册持久器（集中所有项）
	 * 
	 * @param persisters 另一个持久器集
	 */
	void regsiterAll(PersisterSet persisters);

	/**
	 * 注册对象持久器
	 * 
	 * @param <E>       持久对象类型
	 * @param name      持久器名称
	 * @param persister 持久器
	 * @return 若已有项返回原注册的同名持久器，否则返回null
	 */
	<E extends Persistent> Persister<E> regsiter(String name, Persister<E> persister);

	/**
	 * 把持久器注册到集合中
	 * 
	 * @param <E>       持久对象类型
	 * @param clazz     对象类
	 * @param persister 持久器
	 * @return 若已经有同类型的持久器，返回之前注册的
	 */
	<E extends Persistent> Persister<E> regsiter(Class<E> clazz, Persister<E> persister);

	/**
	 * 把持久器及映射器注册到集合中
	 * 
	 * @param <E>       持久对象类型
	 * @param clazz     对象类
	 * @param persister 持久器
	 * @param mapper    映射器
	 * @return 若已经有同类型的持久器，返回之前注册的
	 */
	<E extends Persistent> Persister<E> regsiter(Class<E> clazz, Persister<E> persister, ObjectMapper<E> mapper);

	/**
	 * 以其名称注册持久器
	 * 
	 * @param <E>       持久对象类型
	 * @param persister 持久器
	 * @return 若已经有同名的持久器，返回之前注册的
	 */
	<E extends Persistent> Persister<E> regsiter(Persister<E> persister);

	/**
	 * 注销对象持久器
	 * 
	 * @param persister 要注销的持久器
	 * @return 成功/失败
	 */
	boolean unregsiter(Persister<?> persister);

	/**
	 * 清理持久器集中所有的持久器
	 */
	void cleanup();
}

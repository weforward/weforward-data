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

import cn.weforward.protocol.ext.ObjectMapper;

/**
 * 对象持久器工厂
 * 
 * 
 * 
 * @author daibo
 *
 */
public interface PersisterFactory {
	/**
	 * 创建持久对象的持久器
	 * 
	 * @param <E>   持久对象类型
	 * @param clazz 持久对象类
	 * @param di    依赖di
	 * @return 新创建的持久器
	 */
	<E extends Persistent> Persister<E> createPersister(Class<E> clazz, BusinessDi di);

	/**
	 * 创建持久对象的持久器
	 * 
	 * @param <E>    持久对象类型
	 * @param clazz  持久对象类
	 * @param mapper 持久对象的<code>Mapper</code>
	 * @return 新创建的持久器
	 */
	<E extends Persistent> Persister<E> createPersister(Class<E> clazz, ObjectMapper<E> mapper);

	/**
	 * 获取持久器类
	 * 
	 * @param <E>   持久对象类型
	 * @param clazz 持久对象类
	 * @return 新创建的持久器
	 */
	<E extends Persistent> Persister<E> getPersister(Class<E> clazz);

	/**
	 * 获取对象
	 * 
	 * @param <E> 持久对象类型
	 * @param id  ID
	 * @return 新创建的持久器
	 */
	<E extends Persistent> E get(String id);

	/**
	 * 获取持久器集
	 * 
	 * @return 持久集合
	 */
	PersisterSet getPersisters();
}

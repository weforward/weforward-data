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
package cn.weforward.data.persister.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.Destroyable;
import cn.weforward.data.UniteId;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.persister.PersisterSet;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.ext.ObjectMapperSet;
import cn.weforward.protocol.support.SimpleObjectMapperSet;

/**
 * 对象持久器集合
 * 
 * @author liangyi
 * 
 */
public class SimplePersisterSet implements PersisterSet, Destroyable {
	/** 日志记录器 */
	private static final Logger _Logger = LoggerFactory.getLogger(SimplePersisterSet.class);
	// 集合中的持久器
	protected volatile Map<String, Persister<? extends Persistent>> m_Persisters;
	// 对象映射器管理器
	final protected ObjectMapperSet m_Mappers;

	public SimplePersisterSet() {
		this(new SimpleObjectMapperSet());
	}

	/**
	 * 以自定的映射器集构造持久器集
	 * 
	 * @param mappers 映射器集
	 */
	public SimplePersisterSet(ObjectMapperSet mappers) {
		m_Persisters = Collections.emptyMap();
		m_Mappers = mappers == null ? new SimpleObjectMapperSet() : mappers;
		// 注册在VM终止时执行清理
		// Shutdown.register(this);
	}

	public Iterator<String> getNames() {
		return m_Persisters.keySet().iterator();
	}

	@Override
	public ObjectMapperSet getMappers() {
		return m_Mappers;
	}

	@Override
	public <E> ObjectMapper<E> getMapper(Class<E> clazz) {
		return m_Mappers.getObjectMapper(clazz);
	}

	@Override
	public <E extends Persistent> Persister<E> regsiter(Class<E> clazz, Persister<E> persister) {
		return regsiter(UniteId.getSimpleName(clazz), persister);
	}

	@Override
	public <E extends Persistent> Persister<E> regsiter(Persister<E> persister) {
		return regsiter(persister.getName(), persister);
	}

	@Override
	public <E extends Persistent> Persister<E> regsiter(Class<E> clazz, Persister<E> persister,
			ObjectMapper<E> mapper) {
		if (null != m_Mappers) {
			m_Mappers.register(mapper, clazz);
		}
		return regsiter(UniteId.getSimpleName(clazz), persister);
	}

	@Override
	@SuppressWarnings("unchecked")
	synchronized public <E extends Persistent> Persister<E> regsiter(String name, Persister<E> persister) {
		if (_Logger.isDebugEnabled()) {
			_Logger.debug("注册持久器{this:" + this + ",name:" + name + ",persister:" + persister + "}");
		}
		// 为了提高读取性能，复制一个新的HashMap，处理完后再替换
		Map<String, Persister<? extends Persistent>> newPersisters = new HashMap<String, Persister<? extends Persistent>>(
				m_Persisters);
		Persister<? extends Persistent> old;
		if (null == persister) {
			// 如果persister为null，删除项
			old = newPersisters.remove(name);
		} else {
			// 替换或置入
			old = newPersisters.put(name, persister);
		}
		m_Persisters = newPersisters;
		if (null != old) {
			// 旧的持久器执行清理动作
			old.cleanup();
		}
		return (Persister<E>) old;
	}

	@Override
	synchronized public void regsiterAll(PersisterSet persisters) {
		if (null == persisters) {
			return;
		}
		Map<String, Persister<?>> newPersisters = new HashMap<String, Persister<?>>(m_Persisters);
		if (persisters instanceof SimplePersisterSet) {
			SimplePersisterSet src = (SimplePersisterSet) persisters;
			newPersisters.putAll(src.m_Persisters);
		} else {
			Iterator<String> names = persisters.getNames();
			while (names.hasNext()) {
				String name = names.next();
				Persister<?> persister = m_Persisters.get(name);
				newPersisters.put(name, persister);
				if (_Logger.isDebugEnabled()) {
					_Logger.debug("注册持久器{this:" + this + ",name:" + name + ",persister:" + persister + "}");
				}
			}
		}
		m_Persisters = newPersisters;
		if (null != m_Mappers) {
			m_Mappers.registerAll(persisters.getMappers());
		}
	}

	@Override
	synchronized public boolean unregsiter(Persister<?> persister) {
		// 为了提高读取性能，复制一个新的HashMap，处理完后再替换
		Map<String, Persister<? extends Persistent>> persisters = new HashMap<String, Persister<? extends Persistent>>(
				m_Persisters);
		boolean ret = false;
		Iterator<Map.Entry<String, Persister<?>>> entrys = persisters.entrySet().iterator();
		while (entrys.hasNext()) {
			Map.Entry<String, Persister<?>> e = entrys.next();
			if (e.getValue() == persister) {
				// 删除匹配的项
				entrys.remove();
				persisters.remove(e.getKey());
				ret = true;
			}
		}
		m_Persisters = persisters;
		return ret;
	}

	@Override
	public <E extends Persistent> Persister<E> getPersister(Class<E> clazz) {
		return getPersister(UniteId.getSimpleName(clazz));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E extends Persistent> Persister<E> getPersister(String name) {
		return (Persister<E>) m_Persisters.get(name);
	}

	@Override
	public <E extends Persistent> Persister<E> getPersister(E object) {
		return getPersister(object.getPersistenceId().getType());
	}

	@Override
	public <E extends Persistent> E get(String id) {
		return get(UniteId.valueOf(id));
	}

	@Override
	public <E extends Persistent> E get(UniteId uuid) {
		Persister<E> persister = getPersister(uuid.getType());
		if (null == persister) {
			return null;
		}
		return persister.get(uuid);
	}

	@Override
	public void cleanup() {
		// 遍历Persister执行cleanup
		Set<Map.Entry<String, Persister<?>>> entrys = m_Persisters.entrySet();
		for (Map.Entry<String, Persister<?>> p : entrys) {
			Persister<?> persister = p.getValue();
			if (null == persister) {
				continue;
			}
			try {
				persister.cleanup();
			} catch (Exception e) {
				_Logger.error("clearup failed[" + persister.getName() + "]", e);
			}
		}
	}

	@Override
	public void destroy() {
		cleanup();
	}

}

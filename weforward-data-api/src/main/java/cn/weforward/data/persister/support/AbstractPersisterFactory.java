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

import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;

import cn.weforward.data.persister.BusinessDi;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.persister.PersisterFactory;
import cn.weforward.data.persister.PersisterSet;
import cn.weforward.data.persister.Reloadable;
import cn.weforward.data.util.FieldMapper;
import cn.weforward.data.util.Flusher;
import cn.weforward.data.util.MethodMapper;
import cn.weforward.protocol.ext.ObjectMapper;

/**
 * 抽象持久工厂类
 * 
 * @author daibo
 *
 */
public abstract class AbstractPersisterFactory implements PersisterFactory {
	/**
	 * 日志记录器
	 */
	public final static org.slf4j.Logger _Logger = LoggerFactory.getLogger(AbstractPersisterFactory.class);
	/** 映射方式-属性 */
	protected static final String MAPPER_FIELD = "field";
	/** 映射方式-方法 */
	protected static final String MAPPER_METHOD = "method";
	/** 映射方式-全部 */
	protected static final List<String> MAPPER_ALL = Arrays.asList(MAPPER_FIELD, MAPPER_METHOD);

	/** 持久器集 */
	protected final PersisterSet m_PersisterSet;
	/** 刷写器 */
	protected Flusher m_Flusher;
	/** 服务器ID */
	protected String m_ServerId;
	/** 映射方式 */
	protected String m_MapperType = MAPPER_FIELD;

	public AbstractPersisterFactory() {
		this(null);
	}

	public AbstractPersisterFactory(PersisterSet ps) {
		m_PersisterSet = ps == null ? new SimplePersisterSet() : ps;
	}

	/**
	 * 映射方法
	 * 
	 * @param type {@link #MAPPER_ALL}
	 */
	public void setMapperType(String type) {
		if (!MAPPER_ALL.contains(type)) {
			throw new UnsupportedOperationException("不支持的映射方式:" + type);
		}
		m_MapperType = type;
	}

	/**
	 * 设置服务器标识（ID）
	 * 
	 * @param serverId 如 x000a
	 */
	public void setServerId(String serverId) {
		m_ServerId = serverId;
	}

	public String getServerId() {
		return m_ServerId;
	}

	public Flusher getFlusher() {
		return m_Flusher;
	}

	public void setFlusher(Flusher f) {
		m_Flusher = f;
	}

	@Override
	public <E extends Persistent> Persister<E> getPersister(Class<E> clazz) {
		return m_PersisterSet.getPersister(clazz);
	}

	@Override
	public <E extends Persistent> Persister<E> createPersister(Class<E> clazz, BusinessDi di) {
		ObjectMapper<E> mapper;
		String type = m_MapperType;
		switch (type) {
		case MAPPER_FIELD:
			mapper = FieldMapper.valueOf(clazz, m_PersisterSet.getMappers(), di);
			break;
		case MAPPER_METHOD:
			mapper = MethodMapper.valueOf(clazz, m_PersisterSet.getMappers(), di);
			break;
		default:
			throw new UnsupportedOperationException("不支持的映射方式:" + type);
		}
		return createPersister(clazz, mapper);
	}

	@Override
	public <E extends Persistent> Persister<E> createPersister(Class<E> clazz, ObjectMapper<E> mapper) {
		Persister<E> ps = doCreatePersister(clazz, mapper);
		if (ps instanceof AbstractPersister<?>) {
			((AbstractPersister<?>) ps).setPersisterId(m_ServerId);
			Flusher f = getFlusher();
			if (null != f) {
				((AbstractPersister<?>) ps).setFlusher(f);
			}
		}
		if (null != clazz && Reloadable.class.isAssignableFrom(clazz)) {
			// 若对象实现Reloadable接口则开启重加载功能
			ps.setReloadEnabled(true);
		}
		PersisterSet set = getPersisters();
		if (null != set) {
			set.regsiter(ps);
		}
		return ps;
	}

	/* 执行创建方法 */
	protected abstract <E extends Persistent> Persister<E> doCreatePersister(Class<E> clazz, ObjectMapper<E> mapper);

	@Override
	public <E extends Persistent> E get(String id) {
		return m_PersisterSet.get(id);
	}

	@Override
	public PersisterSet getPersisters() {
		return m_PersisterSet;
	}
}

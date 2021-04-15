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

import cn.weforward.data.persister.OfflineSupplier;
import cn.weforward.data.persister.OfflineSupplierFactory;
import cn.weforward.data.persister.PersisterSet;
import cn.weforward.data.util.FieldMapper;
import cn.weforward.data.util.Flusher;
import cn.weforward.data.util.MethodMapper;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.ext.ObjectMapperSet;

/**
 * 抽象持久工厂类
 * 
 * @author daibo
 *
 */
public abstract class AbstractOfflineSupplierFactory implements OfflineSupplierFactory {
	/**
	 * 日志记录器
	 */
	public final static org.slf4j.Logger _Logger = LoggerFactory.getLogger(AbstractOfflineSupplierFactory.class);
	/** 映射方式-属性 */
	private static final String MAPPER_FIELD = "field";
	/** 映射方式-方法 */
	private static final String MAPPER_METHOD = "method";
	/** 映射方式-全部 */
	private static final List<String> MAPPER_ALL = Arrays.asList(MAPPER_FIELD, MAPPER_METHOD);
	/** 刷写器 */
	protected Flusher m_Flusher;
	/** 服务器ID */
	protected String m_ServerId;
	/** 映射方式 */
	protected String m_MapperType = MAPPER_FIELD;
	/** 对象映射器集合 */
	protected ObjectMapperSet m_Mappers;

	public AbstractOfflineSupplierFactory() {
	}

	public AbstractOfflineSupplierFactory(PersisterSet ps) {
		this(ps.getMappers());
	}

	public AbstractOfflineSupplierFactory(ObjectMapperSet ms) {
		m_Mappers = ms;
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

	@Override
	public <E> OfflineSupplier<E> createOfflineSupplier(Class<E> clazz) {
		ObjectMapper<E> mapper;
		String type = m_MapperType;
		switch (type) {
		case MAPPER_FIELD:
			mapper = FieldMapper.valueOf(clazz, m_Mappers);
			break;
		case MAPPER_METHOD:
			mapper = MethodMapper.valueOf(clazz, m_Mappers);
			break;
		default:
			throw new UnsupportedOperationException("不支持的映射方式:" + type);
		}
		return createOfflineSupplier(clazz, mapper);
	}

	@Override
	public <E> OfflineSupplier<E> createOfflineSupplier(Class<E> clazz, ObjectMapper<E> mapper) {
		return createOfflineSupplier(clazz, mapper, mapper.getName());
	}

	@Override
	public <E> OfflineSupplier<E> createOfflineSupplier(Class<E> clazz, ObjectMapper<E> mapper, String name) {
		OfflineSupplier<E> offline = doCreateOfflineSupplier(clazz, mapper, name);
		if (offline instanceof AbstractOfflineSupplier) {
			((AbstractOfflineSupplier<E>) offline).setFlusher(m_Flusher);
			((AbstractOfflineSupplier<E>) offline).setServerId(m_ServerId);
		}
		return offline;
	}

	/* 执行创建方法 */
	protected abstract <E> OfflineSupplier<E> doCreateOfflineSupplier(Class<E> clazz, ObjectMapper<E> mapper,
			String name);

}

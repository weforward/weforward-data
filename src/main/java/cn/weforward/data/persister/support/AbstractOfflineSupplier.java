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

import java.io.IOException;

import cn.weforward.data.persister.ObjectWithVersion;
import cn.weforward.data.persister.OfflineSupplier;
import cn.weforward.data.util.Flushable;
import cn.weforward.data.util.Flusher;

/**
 * 抽象离线缓存实现
 * 
 * @author daibo
 *
 * @param <E> 业务类
 */
public abstract class AbstractOfflineSupplier<E> implements OfflineSupplier<E> {
	/** 刷写器 */
	protected Flusher m_Flusher;
	/** 服务器ID */
	protected String m_ServerId;

	public AbstractOfflineSupplier() {

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

	public ObjectWithVersion<E> get(String id) {
		return doGet(id);
	}

	@Override
	public String update(String id, E obj) {
		if (null == m_Flusher) {
			return doUpdate(id, obj);
		} else {
			m_Flusher.flush(new FlushableImpl(id, obj));
			return null;
		}
	}

	@Override
	public boolean remove(String id) {
		return doRemove(id);
	}

	/* 执行获取 */
	protected abstract ObjectWithVersion<E> doGet(String id);

	/* 执行更新 */
	protected abstract String doUpdate(String id, E obj);

	/* 执行移除 */
	protected abstract boolean doRemove(String id);

	/**
	 * 可刷写实现
	 * 
	 * @author daibo
	 *
	 */
	class FlushableImpl implements Flushable {
		private String id;
		private E obj;

		public FlushableImpl(String id, E obj) {
			this.id = id;
			this.obj = obj;
		}

		@Override
		public void flush() throws IOException {
			doUpdate(id, obj);
		}

	}
}

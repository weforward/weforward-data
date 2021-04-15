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
package cn.weforward.data.mongodb.util;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import cn.weforward.common.ResultPage;

/**
 * mongodb结果集
 * 
 * @author daibo
 *
 */
public abstract class MongodbResultPage<E> implements ResultPage<E>, Closeable {
	/** 链接 */
	protected MongoCollection<Document> m_Connnection;
	/** 过滤条件 */
	protected Bson m_Filter;
	/** 排序 */
	protected Bson m_Sort;
	/** 投影条件 */
	protected BsonDocument m_Projection;
	/** 限制条件 */
	protected int m_Limit;
	/** 遍历条件 */
	protected MongoCursor<Document> m_It;
	/** 文档缓存 */
	protected List<Document> m_Caches;
	/** 总大小 */
	int m_Count = -1;
	/** 页大小 */
	int m_PageSize = 200;
	/** 当前页 */
	int m_Page;

	public MongodbResultPage(MongoCollection<Document> c, Bson filter) {
		m_Connnection = c;
		m_Filter = filter;
	}

	public MongodbResultPage(MongoCollection<Document> c, Bson filter, Bson sort) {
		m_Connnection = c;
		m_Filter = filter;
		m_Sort = sort;
	}

	public void setProjection(BsonDocument projection) {
		m_Projection = projection;
	}

	@Override
	public int getCount() {
		if (m_Count < 0) {
			long c;
			if (null == m_Filter) {
				c = m_Connnection.countDocuments();
			} else {
				c = m_Connnection.countDocuments(m_Filter);
			}
			if (c > Integer.MAX_VALUE) {
				m_Count = Integer.MAX_VALUE;
			} else {
				m_Count = (int) c;
			}
		}
		return m_Count;
	}

	@Override
	public int getPageCount() {
		int count = getCount();
		int size = getPageSize();
		return count / size + (count % size == 0 ? 0 : 1);
	}

	@Override
	public int getPageSize() {
		return m_PageSize;
	}

	@Override
	public void setPageSize(int size) {
		m_PageSize = size;
	}

	@Override
	public void setPage(int page) {
		m_Page = page;
		gotoPage(page);
	}

	@Override
	public int getPage() {
		return m_Page;
	}

	@Override
	public boolean gotoPage(int page) {
		if (page <= 0 || page > getPageCount()) {
			return false;
		}
		int start = (page - 1) * getPageSize();
		FindIterable<Document> it;
		if (null == m_Filter) {
			it = m_Connnection.find();
		} else {
			it = m_Connnection.find(m_Filter);
		}
		if (null != m_Projection) {
			it = it.projection(m_Projection);
		}
		if (null != m_Sort) {
			it = it.sort(m_Sort);
		}
		int size = getPageSize();
		if (m_Limit > 0) {
			size = Math.min(m_Limit, size);
		}
		m_It = it.skip(start).limit(size).batchSize(size).iterator();
		m_Page = page;
		m_Caches = null;
		return true;
	}

	@Override
	public E prev() {
		if (null == m_Caches || m_Caches.isEmpty()) {
			return null;
		}
		int last = m_Caches.size() - 1;
		Document v = m_Caches.remove(last);
		return to(v);
	}

	@Override
	public boolean hasPrev() {
		return null != m_Caches && !m_Caches.isEmpty();
	}

	@Override
	public E next() {
		if (null == m_It) {
			return null;
		}
		Document doc = m_It.next();
		if (null == m_Caches) {
			m_Caches = new ArrayList<>();
		}
		m_Caches.add(doc);
		return to(doc);
	}

	@Override
	public boolean hasNext() {
		return null == m_It ? false : m_It.hasNext();
	}

	@Override
	public Iterator<E> iterator() {
		return this;
	}

	@Override
	public E move(int pos) {
		if (null != m_Caches && m_Caches.size() > pos) {
			Document v = m_Caches.get(pos);
			for (int i = pos + 1; i < m_Caches.size(); i++) {
				m_Caches.remove(i);
			}
			return to(v);
		}
		int current = null == m_Caches ? 0 : m_Caches.size();
		while (hasNext()) {
			E v = next();
			if (current == pos) {
				return v;
			}
			current++;
		}
		return null;
	}

	@Override
	public void close() {
		if (null != m_It) {
			m_It.close();
		}
		m_Caches = null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	protected abstract E to(Document doc);

}

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
import java.util.Iterator;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

/**
 * mongodb迭代器
 * 
 * @author daibo
 *
 */
public abstract class MongodbIterator<E> implements Iterator<E>, Closeable {
	/** 链接 */
	MongoCollection<Document> m_Connnection;
	/** 指针 */
	MongoCursor<Document> m_It;
	/** 过滤器 */
	Bson m_Filter;
	/** 排序 */
	Bson m_Sort;
	/** 偏移 */
	int m_Offset;
	/** 限制 */
	int m_Limit;
	/** 投影条件 */
	protected BsonDocument m_Projection;

	public MongodbIterator(MongoCollection<Document> c, Bson filter) {
		this(c, filter, null, 0, 0);
	}

	public MongodbIterator(MongoCollection<Document> c, Bson filter, Bson sort, int offset, int limit) {
		m_Connnection = c;
		m_Filter = filter;
		m_Sort = sort;
		m_Offset = offset;
		m_Limit = limit;
	}

	public void setProjection(BsonDocument projection) {
		m_Projection = projection;
	}

	private MongoCursor<Document> getIt() {
		if (null == m_It) {
			FindIterable<Document> it;
			if (null == m_Filter) {
				it = m_Connnection.find();
			} else {
				it = m_Connnection.find(m_Filter);
			}
			if (null != m_Sort) {
				it = it.sort(m_Sort);
			}
			if (m_Offset > 0) {
				it = it.skip(m_Offset);
			}
			if (m_Limit > 0) {
				it = it.limit(m_Limit);
			}
			if (null != m_Projection) {
				it = it.projection(m_Projection);
			}
			m_It = it.iterator();
		}
		return m_It;
	}

	@Override
	public boolean hasNext() {
		return getIt().hasNext();
	}

	@Override
	public E next() {
		Document doc = getIt().next();
		if (null == doc) {
			return null;
		}
		return to(doc);
	}

	protected abstract E to(Document doc);

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
		if (null != m_It) {
			m_It.close();
		}
	}

}

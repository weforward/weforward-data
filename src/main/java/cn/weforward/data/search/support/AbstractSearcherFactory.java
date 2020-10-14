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
package cn.weforward.data.search.support;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.weforward.data.search.Searcher;
import cn.weforward.data.search.SearcherFactory;

/**
 * 抽象搜索工厂
 * 
 * @author daibo
 *
 */
public abstract class AbstractSearcherFactory implements SearcherFactory {
	/** 数据项 */
	private ConcurrentMap<String, Searcher> m_Items;

	public AbstractSearcherFactory() {
		m_Items = new ConcurrentHashMap<>();
	}

	@Override
	public Iterator<Searcher> iterator() {
		return m_Items.values().iterator();
	}

	@Override
	public Searcher openSearcher(String name) {
		Searcher c = m_Items.get(name);
		if (null != c) {
			return c;
		}
		c = doCreateSearcher(name);
		Searcher old = m_Items.putIfAbsent(name, c);
		return null == old ? c : old;
	}

	@Override
	public Searcher createSearcher(String name) {
		Searcher c = m_Items.get(name);
		if (null != c) {
			throw new IllegalArgumentException("已存在同名的搜索器[" + name + "]");
		}
		c = doCreateSearcher(name);
		if (null == m_Items.putIfAbsent(name, c)) {
			return c;
		} else {
			throw new IllegalArgumentException("已存在同名的搜索器[" + name + "]");
		}
	}

	@Override
	public Searcher getSearcher(String name) {
		return m_Items.get(name);
	}

	/* 创建计数器 */
	protected abstract Searcher doCreateSearcher(String name);
}

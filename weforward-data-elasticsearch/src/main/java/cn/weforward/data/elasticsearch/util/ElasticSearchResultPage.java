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
package cn.weforward.data.elasticsearch.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.ResultPage;
import cn.weforward.data.exception.DataAccessException;

/**
 * ElasticSearch分页
 * 
 * @author daibo
 *
 */
public abstract class ElasticSearchResultPage<E> implements ResultPage<E> {
	/** 日志 */
	protected static final Logger _Logger = LoggerFactory.getLogger(ElasticSearchResultPage.class);
	/** 空数组 */
	protected static final JSONArray EMPTY = new JSONArray();
	protected static final JSONObject ASC = new JSONObject(Collections.singletonMap("order", "asc"));
	protected static final JSONObject DESC = new JSONObject(Collections.singletonMap("order", "desc"));
	/** 客户端 */
	protected RestClient m_Client;
	/** 名称 */
	protected String m_Name;
	/** 查询语句 */
	protected JSONObject m_Query;
	/** 排序语句 */
	protected JSONArray m_Sort;
	/** 返回的属性 */
	protected JSONArray m_Source;
	/** 是否优化输出 */
	protected boolean m_Pretty;
	/** 文档缓存 */
	protected JSONArray m_Caches;
	/** 索引位置 */
	protected int m_CachesIndex;
	/** 总大小 */
	protected int m_Count = -1;
	/** 页大小 */
	protected int m_PageSize = 200;
	/** 当前页 */
	protected int m_Page;

	public ElasticSearchResultPage(RestClient client, String name, JSONObject query) {
		m_Client = client;
		m_Name = name;
		m_Query = query;
		m_Sort = new JSONArray();
		m_Source = new JSONArray();
	}

	public RestClient getClient() {
		return m_Client;
	}

	public void setPretty(boolean pretty) {
		m_Pretty = pretty;
	}

	public boolean isPretty() {
		return m_Pretty;
	}

	public void addSort(JSONObject sort) {
		m_Sort.put(sort);
	}

	public void addSource(String source) {
		m_Source.put(source);
	}

	protected void exe(int from, int size) {
		Request request = new Request("GET", "/" + m_Name + "/_search");
		request.addParameter("size", String.valueOf(size));
		request.addParameter("from", String.valueOf(from));
		if (m_Pretty) {
			request.addParameter("pretty", "true");
		}
		JSONObject jsoncontent = new JSONObject();
		jsoncontent.put("query", m_Query);
		if (!m_Sort.isEmpty()) {
			jsoncontent.put("sort", m_Sort);
		}
		if (!m_Source.isEmpty()) {
			jsoncontent.put("_source", m_Source);
		}
		String content = jsoncontent.toString();
		Response response = null;
		try {
			if (_Logger.isTraceEnabled()) {
				_Logger.trace(request.toString());
				_Logger.trace(content);
			}
			request.setJsonEntity(content);
			response = getClient().performRequest(request);
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() == 202) {
				throw new DataAccessException("接口返回异常:" + status);
			}
			String back = EntityUtils.toString(response.getEntity());
			if (_Logger.isTraceEnabled()) {
				_Logger.trace(status.toString());
				_Logger.trace(back);
			}
			JSONObject json = new JSONObject(back);
			JSONObject hits = json.getJSONObject("hits");
			JSONObject total = hits.getJSONObject("total");
			m_Count = total.getInt("value");
			m_Caches = hits.getJSONArray("hits");
			return;
		} catch (ResponseException e) {
			response = e.getResponse();
			StatusLine status = response.getStatusLine();
			if ((status.getStatusCode() == 404)) {
				m_Count = 0;
				m_Caches = EMPTY;
			} else {
				throw new DataAccessException("搜索数据异常", e);
			}
		} catch (IOException e) {
			throw new DataAccessException("搜索数据异常", e);
		} finally {
			if (null != response) {
				try {
					EntityUtils.consume(response.getEntity());
				} catch (IOException e) {
					_Logger.warn("忽略关闭异常", e);
				}
			}
		}
	}

	@Override
	public int getCount() {
		if (m_Count < 0) {
			exe(0, 0);
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
		int size = getPageSize();
		exe(start, size);
		m_Page = page;
		return true;
	}

	@Override
	public E prev() {
		if (null == m_Caches || m_Caches.isEmpty() || m_CachesIndex <= 0) {
			return null;
		}
		JSONObject v = m_Caches.getJSONObject(--m_CachesIndex);
		return to(v);
	}

	@Override
	public boolean hasPrev() {
		return null != m_Caches && !m_Caches.isEmpty();
	}

	@Override
	public E next() {
		if (null == m_Caches || m_Caches.isEmpty() || m_CachesIndex > (m_Caches.length() - 1)) {
			return null;
		}
		JSONObject v = m_Caches.getJSONObject(m_CachesIndex++);
		return to(v);
	}

	@Override
	public boolean hasNext() {
		return m_CachesIndex < m_Caches.length();
	}

	@Override
	public Iterator<E> iterator() {
		return this;
	}

	@Override
	public E move(int pos) {
		if (null == m_Caches || m_Caches.isEmpty() || pos < 0 || pos >= m_Caches.length()) {
			return null;
		}
		m_CachesIndex = pos;
		return to(m_Caches.getJSONObject(m_CachesIndex));
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	protected abstract E to(JSONObject doc);

}

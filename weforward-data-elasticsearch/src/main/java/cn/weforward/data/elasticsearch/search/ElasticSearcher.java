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
package cn.weforward.data.elasticsearch.search;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

import cn.weforward.common.util.ListUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.exception.DataAccessException;
import cn.weforward.data.search.IndexAttribute;
import cn.weforward.data.search.IndexElement;
import cn.weforward.data.search.IndexKeyword;
import cn.weforward.data.search.IndexRange;
import cn.weforward.data.search.IndexResults;
import cn.weforward.data.search.SearchOption;
import cn.weforward.data.search.support.AbstractSearcher;

/**
 * 基于Elasticsearcher的搜索器
 * 
 * @author daibo
 *
 */
public class ElasticSearcher extends AbstractSearcher {
	/** 日志 */
	protected static final Logger _Logger = LoggerFactory.getLogger(ElasticSearcher.class);
	private static final JSONObject TYPE_KEYWORD = new JSONObject(Collections.singletonMap("type", "keyword"));
	private static final JSONObject TYPE_INTEGER = new JSONObject(Collections.singletonMap("type", "integer"));
	/** 搜索工厂 */
	protected ElasticSearcherFactory m_Factory;
	/** id属性 */
	final static String ID = "id";
	/** 关键字属性 */
	final static String KEYWROD = "k";
	/** 索引项标题 */
	final static String CAPTION = "c";
	/** 索引项摘要 */
	final static String SUMMARY = "s";
	/** 属性（主要用于排序） */
	final static String ATTRIBUTES = "a";
	/** 关键字属性-值 */
	final static String KEYWROD_VALUE = "v";
	/** 关键字属性-匹配率 */
	final static String KEYWROD_RATE = "r";
	/** 关键字属性-值属性名 */
	final static String KEYWORD_VALUE_INDEX = KEYWROD + "." + KEYWROD_VALUE;
	/** 关键字属性-匹配率属性名 */
	final static String KEYWORD_VALUE_RATE = KEYWROD + "." + KEYWROD_RATE;
	/** 服务器id */
	final static String SERVERID = "_serverid";

	/** 是否已初始化 */
	protected AtomicBoolean m_Init = new AtomicBoolean();

	public ElasticSearcher(ElasticSearcherFactory factory, String name) {
		super(name.toLowerCase());
		m_Factory = factory;
	}

	@Override
	public void updateElement(IndexElement element, List<? extends IndexKeyword> keywords) {
		if (null == keywords || keywords.isEmpty()) {
			removeElement(element.getKey());
			return;
		}
		StringBuilder items = new StringBuilder();
		items.append('{');
		if (!StringUtil.isEmpty(element.getCaption())) {
			append(items, CAPTION, element.getCaption());
			items.append(',');
		}
		if (!StringUtil.isEmpty(element.getSummary())) {
			append(items, SUMMARY, element.getSummary());
			items.append(',');
		}
		if (!StringUtil.isEmpty(m_Factory.getServerId())) {
			append(items, SERVERID, m_Factory.getServerId());
			items.append(',');
		}
		List<IndexAttribute> attrs = element.getAttributes();
		if (null != attrs) {
			StringBuilder attrsItems = new StringBuilder();
			attrsItems.append('{');
			int i = 0;
			IndexAttribute pair = attrs.get(i);
			append(attrsItems, pair.getKey(), StringUtil.toString(pair.getValue()));
			for (i = 1; i < attrs.size(); i++) {
				attrsItems.append(',');
				pair = attrs.get(i);
				append(attrsItems, pair.getKey(), StringUtil.toString(pair.getValue()));
			}
			attrsItems.append('}');
			items.append("\"").append(ATTRIBUTES).append("\":");
			items.append(attrsItems.toString());
			items.append(',');
		}
		StringBuilder array = new StringBuilder();
		array.append('[');
		int i = 0;
		IndexKeyword kw = keywords.get(i);
		array.append('{');
		array.append("\"").append(KEYWROD_VALUE).append("\":\"").append(kw.getKeyword()).append("\"");
		array.append(',');
		array.append("\"").append(KEYWROD_RATE).append("\":").append(kw.getRate()).append("");
		array.append('}');
		for (i = 1; i < keywords.size(); i++) {
			kw = keywords.get(i);
			array.append(',');
			array.append('{');
			array.append("\"").append(KEYWROD_VALUE).append("\":\"").append(kw.getKeyword()).append("\"");
			array.append(',');
			array.append("\"").append(KEYWROD_RATE).append("\":").append(kw.getRate()).append("");
			array.append('}');
		}
		array.append(']');
		items.append("\"");
		items.append(KEYWROD);
		items.append("\":");
		items.append(array.toString());
		String id = element.getKey();
		items.append(",\"");
		items.append(ID);
		items.append("\":\"");
		items.append(id);
		items.append("\"}");
		Request request = new Request("post", "/" + getName() + "/_doc/" + id);
		String entity = items.toString();
		request.setJsonEntity(entity);
		addParameter(request);
		Response response = null;
		try {
			if (_Logger.isTraceEnabled()) {
				_Logger.trace(request.toString());
				_Logger.trace(entity);
			}
			response = getClient().performRequest(request);
			StatusLine status = response.getStatusLine();
			if (_Logger.isTraceEnabled()) {
				_Logger.trace(status.toString());
				_Logger.trace(EntityUtils.toString(response.getEntity()));
			}
			if (status.getStatusCode() != 200 && status.getStatusCode() != 201) {
				throw new DataAccessException("接口返回异常:" + status);
			}
		} catch (ResponseException e) {
			response = e.getResponse();
			StatusLine status = response.getStatusLine();
			if (_Logger.isTraceEnabled()) {
				_Logger.trace(status.toString());
				try {
					_Logger.trace(EntityUtils.toString(response.getEntity()));
				} catch (IOException ee) {
					_Logger.warn("忽略异常", ee);
				}
			}
			throw new DataAccessException("插入数据异常", e);
		} catch (IOException e) {
			throw new DataAccessException("插入数据异常", e);
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

	private void append(StringBuilder items, String key, String value) {
		items.append("\"").append(key).append("\":\"").append(value).append("\"");

	}

	protected void addParameter(Request request) {
		if (m_Factory.getPretty()) {
			request.addParameter("pretty", "true");
		}
	}

	@Override
	public boolean removeElement(String elementKey) {
		Request request = new Request("delete", "/" + getName() + "/_doc/" + elementKey);
		addParameter(request);
		Response response = null;
		try {
			if (_Logger.isTraceEnabled()) {
				_Logger.trace(request.toString());
			}
			response = getClient().performRequest(request);
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() == 202) {
				throw new DataAccessException("接口返回异常:" + status);
			}
			if (_Logger.isTraceEnabled()) {
				_Logger.trace(status.toString());
				_Logger.trace(EntityUtils.toString(response.getEntity()));
			}
			return true;
		} catch (ResponseException e) {
			response = e.getResponse();
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() == 404) {
				return false;
			} else {
				throw new DataAccessException("删除数据异常", e);
			}
		} catch (IOException e) {
			throw new DataAccessException("删除数据异常", e);
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
	public IndexResults searchAll(List<? extends IndexRange> andRanges, List<? extends IndexRange> orRanges,
			List<? extends IndexKeyword> andKeywords, List<? extends IndexKeyword> orKeywords, SearchOption options) {
		// JSONObject must = new JSONObject();
		// must.put("match_all", new JSONObject());
		// bool.put("must", must);
		JSONArray should = new JSONArray();
		JSONArray filter = new JSONArray();
		if (ListUtil.isEmpty(andKeywords)) {
			// 不用处理
		} else if (andKeywords.size() == 1) {
			IndexKeyword ik = andKeywords.get(0);
			put(filter, ik);
		} else {
			for (IndexKeyword ik : andKeywords) {
				put(filter, ik);
			}

		}
		if (ListUtil.isEmpty(orKeywords)) {
			// 不用处理
		} else if (orKeywords.size() == 1) {
			IndexKeyword ik = orKeywords.get(0);
			put(filter, ik);
		} else {
			JSONObject terms = new JSONObject();
			JSONArray arr = new JSONArray();
			for (IndexKeyword ik : orKeywords) {
				if (ik.getRate() == IndexKeyword.RATE_PREFIX) {
					putPrefix(filter, ik);
				} else {
					arr.put(ik.getKeyword());
				}
			}
			terms.put(KEYWORD_VALUE_INDEX, arr);
			filter.put(new JSONObject(Collections.singletonMap("terms", terms)));
		}

		if (ListUtil.isEmpty(andRanges)) {
			// 不用处理
		} else if (andRanges.size() == 1) {
			JSONObject range = new JSONObject();
			IndexRange r = andRanges.get(0);
			JSONObject rangeDesc = new JSONObject();
			rangeDesc.put("gte", r.getBegin());
			rangeDesc.put("lt", r.getEnd());
			range.put(KEYWORD_VALUE_INDEX, rangeDesc);
			filter.put(new JSONObject(Collections.singletonMap("range", range)));
		} else {
			for (IndexRange r : andRanges) {
				JSONObject range = new JSONObject();
				JSONObject rangeDesc = new JSONObject();
				rangeDesc.put("gte", r.getBegin());
				rangeDesc.put("lt", r.getEnd());
				range.put(KEYWORD_VALUE_INDEX, rangeDesc);
				filter.put(new JSONObject(Collections.singletonMap("range", range)));
			}
		}
		if (ListUtil.isEmpty(orRanges)) {
			// 不用处理
		} else if (orRanges.size() == 1) {
			JSONObject range = new JSONObject();
			IndexRange r = orRanges.get(0);
			JSONObject rangeDesc = new JSONObject();
			rangeDesc.put("gte", r.getBegin());
			rangeDesc.put("lt", r.getEnd());
			range.put(KEYWORD_VALUE_INDEX, rangeDesc);
			filter.put(new JSONObject(Collections.singletonMap("range", range)));
		} else {
			for (IndexRange r : orRanges) {
				JSONObject range = new JSONObject();
				JSONObject rangeDesc = new JSONObject();
				rangeDesc.put("gte", r.getBegin());
				rangeDesc.put("lt", r.getEnd());
				range.put(KEYWORD_VALUE_INDEX, rangeDesc);
				should.put(new JSONObject(Collections.singletonMap("range", range)));
			}
		}
		if (null != options) {
			if (options.isOption(SearchOption.OPTION_RATE_LEAST)) {
				JSONObject range = new JSONObject();
				JSONObject rangeDesc = new JSONObject();
				rangeDesc.put("gte", options.getRate());
				range.put(KEYWORD_VALUE_RATE, rangeDesc);
				filter.put(new JSONObject(Collections.singletonMap("range", range)));
			} else if (options.isOption(SearchOption.OPTION_RANGE_LIMIT)) {
				JSONObject range = new JSONObject();
				JSONObject rangeDesc = new JSONObject();
				rangeDesc.put("gte", options.getStartRate());
				rangeDesc.put("lte", options.getEndRate());
				range.put(KEYWORD_VALUE_RATE, rangeDesc);
				filter.put(new JSONObject(Collections.singletonMap("range", range)));
			}
		}
		JSONObject bool = new JSONObject();
		if (!filter.isEmpty()) {
			bool.put("filter", filter);
		}
		if (!should.isEmpty()) {
			bool.put("should", should);
		}
		JSONObject query = new JSONObject();
		query.put("bool", bool);
		ElasticIndexResults irs = new ElasticIndexResults(getClient(), getName(), query);
		if (null != options) {
			if (options.isOption(SearchOption.OPTION_RESULT_DETAIL)) {
				irs.setNeedDetail(true);
			}
			if (options.isOption(SearchOption.OPTION_RATE_SORT)) {
				irs.addSort(new JSONObject(
						Collections.singletonMap(KEYWORD_VALUE_RATE, IndexResults.OPTION_ORDER_BY_DESC)));
			}
		}
		irs.setPretty(m_Factory.getPretty());
		return irs;
	}

	private void put(JSONArray filter, IndexKeyword ik) {
		if (ik.getRate() == IndexKeyword.RATE_PREFIX) {
			putPrefix(filter, ik);
		} else {
			putTerm(filter, ik);
		}
	}

	private void putTerm(JSONArray filter, IndexKeyword ik) {
		JSONObject term = new JSONObject();
		term.put(KEYWORD_VALUE_INDEX, ik.getKeyword());
		filter.put(new JSONObject(Collections.singletonMap("term", term)));
	}

	private void putPrefix(JSONArray filter, IndexKeyword ik) {
		JSONObject prefix = new JSONObject();
		prefix.put(ID, new JSONObject(Collections.singletonMap("value", ik.getKeyword())));
		filter.put(new JSONObject(Collections.singletonMap("prefix", prefix)));

	}

	private RestClient getClient() {
		if (m_Init.get()) {
			return m_Factory.getClient();
		} else {
			synchronized (m_Init) {
				if (m_Init.get()) {
					return m_Factory.getClient();
				}
				init();
				m_Init.getAndSet(true);
			}
			return m_Factory.getClient();
		}

	}

	private void init() {
		JSONObject entity = new JSONObject();
		JSONObject mappings = new JSONObject();
		JSONObject properties = new JSONObject();
		properties.put(ID, TYPE_KEYWORD);
		properties.put(KEYWORD_VALUE_INDEX, TYPE_KEYWORD);
		properties.put(KEYWORD_VALUE_RATE, TYPE_INTEGER);
		mappings.put("properties", properties);
		entity.put("mappings", mappings);
		Request request = new Request("PUT", "/" + getName());
		addParameter(request);
		Response response = null;
		try {
			String entityString = entity.toString();
			if (_Logger.isTraceEnabled()) {
				_Logger.trace(request.toString());
				_Logger.trace(entityString);
			}
			request.setJsonEntity(entityString);
			response = m_Factory.getClient().performRequest(request);
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() == 202) {
				throw new DataAccessException("接口返回异常:" + status);
			}
			if (_Logger.isTraceEnabled()) {
				_Logger.trace(status.toString());
				_Logger.trace(EntityUtils.toString(response.getEntity()));
			}
		} catch (ResponseException e) {
			response = e.getResponse();
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() != 400) {
				throw new DataAccessException("更新属性异常", e);
			}
		} catch (IOException e) {
			throw new DataAccessException("更新属性异常", e);
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

}

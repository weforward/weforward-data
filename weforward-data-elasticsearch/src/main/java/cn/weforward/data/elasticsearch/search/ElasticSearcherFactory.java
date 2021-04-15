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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpHost;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;

import cn.weforward.common.util.StringUtil;
import cn.weforward.data.search.Searcher;
import cn.weforward.data.search.support.AbstractSearcherFactory;

/**
 * 基于Elasticsearcher的搜索工厂
 * 
 * @author daibo
 *
 */
public class ElasticSearcherFactory extends AbstractSearcherFactory {
	/** 客户端 */
	protected RestClient m_Client;
	/** 服务器id */
	protected String m_ServerId;
	/** 是否美化返回报文 */
	protected boolean m_Pretty;

	public ElasticSearcherFactory(String url, String serverId) {
		this(Arrays.asList(url.split(";")), serverId);
	}

	public ElasticSearcherFactory(List<String> urls, String serverId) {
		List<HttpHost> hosts = new ArrayList<>(urls.size());
		for (int i = 0; i < urls.size(); i++) {
			String url = urls.get(i);
			if (StringUtil.isEmpty(url)) {
				continue;
			}
			hosts.add(HttpHost.create(url));
		}
		Node[] nodes = new Node[hosts.size()];
		for (int i = 0; i < hosts.size(); i++) {
			nodes[i] = new Node(hosts.get(i));
		}
		m_Client = RestClient.builder(nodes).build();
		m_ServerId = serverId;
	}

	public String getServerId() {
		return m_ServerId;
	}

	@Override
	protected Searcher doCreateSearcher(String name) {
		return new ElasticSearcher(this, name);
	}

	protected RestClient getClient() {
		return m_Client;

	}

	public void setPretty(boolean pretty) {
		m_Pretty = pretty;
	}

	public boolean getPretty() {
		return m_Pretty;
	}

}

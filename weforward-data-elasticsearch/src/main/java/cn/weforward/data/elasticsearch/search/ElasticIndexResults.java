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
import java.util.List;

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
import org.elasticsearch.client.RestClient;
import org.json.JSONObject;

import cn.weforward.common.util.StringUtil;
import cn.weforward.data.elasticsearch.util.ElasticSearchResultPage;
import cn.weforward.data.search.IndexAttribute;
import cn.weforward.data.search.IndexResult;
import cn.weforward.data.search.IndexResults;
import cn.weforward.data.search.util.IndexAttributeHelper;
import cn.weforward.data.search.util.IndexElementHelper;
import cn.weforward.data.search.vo.IndexElementVo;
import cn.weforward.data.search.vo.IndexResultVo;

/**
 * Elastic结果集
 * 
 * @author daibo
 *
 */
public class ElasticIndexResults extends ElasticSearchResultPage<IndexResult> implements IndexResults {

	protected boolean m_NeedDetail;

	public ElasticIndexResults(RestClient client, String name, JSONObject query) {
		super(client, name, query);
		addSource(ElasticSearcher.ID);
	}

	public void setNeedDetail(boolean need) {
		m_NeedDetail = need;
		if (need) {
			addSource(ElasticSearcher.CAPTION);
			addSource(ElasticSearcher.SUMMARY);
			addSource(ElasticSearcher.ATTRIBUTES);
		}
	}

	@Override
	public void sort(String attribut, int option) {
		JSONObject sort = new JSONObject();
		if (option == OPTION_ORDER_BY_ASC) {
			sort.put(ElasticSearcher.ATTRIBUTES + "." + attribut, ASC);
		} else if (option == OPTION_ORDER_BY_DESC) {
			sort.put(ElasticSearcher.ATTRIBUTES + "." + attribut, DESC);
		}
		addSort(sort);
	}

	@Override
	public IndexResults snapshot() {
		return new ElasticIndexResults(m_Client, m_Name, m_Query);
	}

	@Override
	protected IndexResult to(JSONObject doc) {
		String key = doc.getString("_id");
		if (m_NeedDetail) {
			JSONObject source = doc.getJSONObject("_source");
			String caption = source.optString(ElasticSearcher.CAPTION);
			String summary = source.optString(ElasticSearcher.SUMMARY);
			JSONObject attr = source.optJSONObject(ElasticSearcher.ATTRIBUTES);
			List<IndexAttribute> attributes = new ArrayList<>();
			if (null != attr) {
				for (String k : attr.keySet()) {
					Object v = attr.opt(k);
					if (v instanceof Number) {
						attributes.add(IndexAttributeHelper.newAttribute(k, (Number) v));
					} else {
						attributes.add(IndexAttributeHelper.newAttribute(k, StringUtil.toString(v)));
					}
				}
			}
			return new IndexResultVo(IndexElementHelper.newElement(key, caption, summary, attributes));
		} else {
			return new IndexResultVo(new IndexElementVo(key));
		}
	}

}

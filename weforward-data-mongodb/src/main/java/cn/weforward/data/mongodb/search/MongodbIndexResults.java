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
package cn.weforward.data.mongodb.search;

import java.util.ArrayList;
import java.util.List;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import cn.weforward.data.mongodb.util.MongodbResultPage;
import cn.weforward.data.search.IndexAttribute;
import cn.weforward.data.search.IndexElement;
import cn.weforward.data.search.IndexResult;
import cn.weforward.data.search.IndexResults;
import cn.weforward.data.search.SearchOption;
import cn.weforward.data.search.util.IndexElementHelper;
import cn.weforward.data.search.vo.IndexAttributeVo;

/**
 * mongodb迭代器
 * 
 * @author daibo
 *
 */
class MongodbIndexResults extends MongodbResultPage<IndexResult> implements IndexResults {

	/** 搜索条件 */
	SearchOption m_Options;
	/** 是否需要属性 */
	private static final BsonInt32 NEED = new BsonInt32(1);

	MongodbIndexResults(MongoCollection<Document> c, Bson filter, SearchOption options) {
		super(c, filter);
		m_Connnection = c;
		m_Filter = filter;
		m_Options = options;
		if (isOptions(SearchOption.OPTION_RATE_RANGE, m_Options)) {
			Bson b = Filters.gte(MongodbSearcher.KEYWORD_VALUE_RATE, m_Options.getStartRate());
			Bson s = Filters.lte(MongodbSearcher.KEYWORD_VALUE_RATE, m_Options.getEndRate());
			if (null == m_Filter) {
				m_Filter = b;
			} else {
				m_Filter = Filters.and(m_Filter, b, s);
			}
		} else if (isOptions(SearchOption.OPTION_RATE_LEAST, m_Options)) {
			Bson b = Filters.gte(MongodbSearcher.KEYWORD_VALUE_RATE, m_Options.getRate());
			if (null == m_Filter) {
				m_Filter = b;
			} else {
				m_Filter = Filters.and(m_Filter, b);
			}
		}
		if (isOptions(SearchOption.OPTION_RATE_SORT, m_Options)) {
			m_Sort = Filters.eq(MongodbSearcher.KEYWORD_VALUE_RATE, -1);
		}
		if (isOptions(SearchOption.OPTION_RANGE_LIMIT, m_Options)) {
			m_Limit = m_Options.getLimit();
		}
		BsonDocument projection = new BsonDocument();

		if (isOptions(SearchOption.OPTION_RESULT_DETAIL, m_Options)) {
			projection.put(MongodbSearcher.CAPTION, NEED);
			projection.put(MongodbSearcher.SUMMARY, NEED);
			projection.put(MongodbSearcher.ATTRIBUTES, NEED);
		}
		m_Projection = projection;
	}

	@Override
	public void sort(String attribut, int option) {
		int index = 0;
		if (option == IndexResults.OPTION_ORDER_BY_ASC) {
			index = 1;
		} else if (option == IndexResults.OPTION_ORDER_BY_DESC) {
			index = -1;
		}
		m_Sort = Filters.eq(MongodbSearcher.ATTRIBUTES + "." + attribut, index);
		m_Projection.put(MongodbSearcher.ATTRIBUTES, new BsonInt32(1));
	}

	@Override
	public IndexResults snapshot() {
		return new MongodbIndexResults(m_Connnection, m_Filter, m_Options);
	}

	@Override
	protected IndexResult to(Document doc) {
		return new IndexResultImpl(doc);
	}

	private static boolean isOptions(int option, SearchOption options) {
		if (null == options) {
			return false;
		}
		return options.isOption(option);
	}

	class IndexResultImpl implements IndexResult {
		String m_Key;
		String m_Caption;
		String m_Summary;
		List<IndexAttribute> m_Attributes;

		IndexResultImpl(Document doc) {
			m_Key = doc.getString(MongodbSearcher.ID);
			if (isOptions(SearchOption.OPTION_RESULT_DETAIL, m_Options)) {
				m_Caption = doc.getString(MongodbSearcher.CAPTION);
				m_Summary = doc.getString(MongodbSearcher.SUMMARY);
				Object value = doc.get(MongodbSearcher.ATTRIBUTES);
				if (value instanceof Document) {
					m_Attributes = new ArrayList<>();
					Document vdoc = ((Document) value);
					for (String key : vdoc.keySet()) {
						m_Attributes.add(new IndexAttributeVo(key, vdoc.get(key)));
					}
				}
			}
		}

		@Override
		public String getKey() {
			return m_Key;
		}

		@Override
		public IndexElement getElement() {
			return IndexElementHelper.newElement(m_Key, m_Caption, m_Summary, m_Attributes);
		}

		@Override
		public String toString() {
			return getKey();
		}

	}

}

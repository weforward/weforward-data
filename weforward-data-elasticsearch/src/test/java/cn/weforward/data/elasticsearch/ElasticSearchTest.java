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
package cn.weforward.data.elasticsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.data.elasticsearch.search.ElasticSearcher;
import cn.weforward.data.elasticsearch.search.ElasticSearcherFactory;
import cn.weforward.data.elasticsearch.util.ElasticSearchResultPage;
import cn.weforward.data.search.IndexAttribute;
import cn.weforward.data.search.IndexElement;
import cn.weforward.data.search.IndexKeyword;
import cn.weforward.data.search.IndexResult;
import cn.weforward.data.search.IndexResults;
import cn.weforward.data.search.SearchOption;
import cn.weforward.data.search.Searcher;
import cn.weforward.data.search.util.IndexAttributeHelper;
import cn.weforward.data.search.util.IndexElementHelper;
import cn.weforward.data.search.util.IndexKeywordHelper;

public class ElasticSearchTest {

	@Test
	public void testApp() {
		Logger l = (Logger) LoggerFactory.getLogger(ElasticSearcher.class);
		l.setLevel(Level.TRACE);
		l = (Logger) LoggerFactory.getLogger(ElasticSearchResultPage.class);
		l.setLevel(Level.TRACE);
		ElasticSearcherFactory factory = new ElasticSearcherFactory("http://127.0.0.1:9200", "x00ff");
		factory.setPretty(true);
		Searcher s = factory.createSearcher("mytest4");
		{
			List<IndexAttribute> attr = Arrays.asList(IndexAttributeHelper.newAttribute("序号", 10000));
			IndexElement element = IndexElementHelper.newElement("User$00001", "描述", "总结", attr);
			IndexKeyword k1 = IndexKeywordHelper.newKeyword("小天", 1);
			IndexKeyword k2 = IndexKeywordHelper.newKeyword("20210101", 200);
			IndexKeyword k3 = IndexKeywordHelper.newKeyword("level-10", 10);
			List<? extends IndexKeyword> keywords = Arrays.asList(k1, k2, k3);
			s.updateElement(element, keywords);
		}
		{
			List<IndexAttribute> attr = Arrays.asList(IndexAttributeHelper.newAttribute("序号", 20000));
			IndexElement element = IndexElementHelper.newElement("User$00002", attr);
			IndexKeyword k1 = IndexKeywordHelper.newKeyword("小地", 1);
			IndexKeyword k2 = IndexKeywordHelper.newKeyword("20210102", 100);
			IndexKeyword k3 = IndexKeywordHelper.newKeyword("level-20", 20);
			List<? extends IndexKeyword> keywords = Arrays.asList(k1, k2, k3);
			s.updateElement(element, keywords);
		}
		SearchOption options = SearchOption.valueOf(0);
		List<IndexKeyword> keywords = new ArrayList<>();
		keywords.add((IndexKeyword) IndexKeywordHelper.prefixKeyword("User$"));
//		List<IndexRange> ranges = new ArrayList<>();
//		ranges.add(new RangeVo("20210101", "202101019"));
//		ranges.add(new RangeVo("level-20", "level-209"));
//		IndexResults irs = s.searchAll(null, ranges, null, null, options);
		IndexResults irs = s.search(keywords, options);
		// IndexResults irs = s.search(options, "小地");
		irs.sort("序号", IndexResults.OPTION_ORDER_BY_DESC);
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println(ir.getKey() + "," + ir.getElement().getAttributes());
		}

//		{
//			IndexElement element = IndexElementHelper.newElement("User$00002");
//			IndexKeyword k1 = IndexKeywordHelper.newKeyword("me", 2);
//			List<? extends IndexKeyword> keywords = Arrays.asList(k1);
//			s.updateElement(element, keywords);
//		}
	}
}

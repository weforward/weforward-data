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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.data.UniteId;
import cn.weforward.data.search.IndexKeyword;
import cn.weforward.data.search.IndexRange;
import cn.weforward.data.search.IndexResult;
import cn.weforward.data.search.IndexResults;
import cn.weforward.data.search.SearchOption;
import cn.weforward.data.search.Searcher;
import cn.weforward.data.search.util.IndexAttributeHelper;
import cn.weforward.data.search.util.IndexElementHelper;
import cn.weforward.data.search.util.IndexKeywordHelper;

public class SearchDemo {

	public static void main(String[] args) {
		MongodbSearcherFactory factory = new MongodbSearcherFactory("mongodb://localhost:27017/", "search");
		IndexResults irs;
		// 场景1 单关键字匹配
		Searcher s1 = factory.openSearcher("test1");
		s1.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$1")),
				Arrays.asList(IndexKeywordHelper.newKeyword("我的")));
		s1.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$2")),
				Arrays.asList(IndexKeywordHelper.newKeyword("他的")));
		irs = s1.search(null, "我的");
		// 结果为order1
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println("场景1:" + ir.getKey());
		}

		// 场景2 多关键字匹配
		Searcher s2 = factory.openSearcher("test2");
		s2.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$1")),
				Arrays.asList(IndexKeywordHelper.newKeyword("我的"), IndexKeywordHelper.newKeyword("我们的")));
		s2.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$2")),
				Arrays.asList(IndexKeywordHelper.newKeyword("他的"), IndexKeywordHelper.newKeyword("我们的")));
		irs = s2.search(null, "我的", "我们的");// 默认为与关系
		// 结果为order1
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println("场景2:" + ir.getKey());
		}

		// 场景3 任意关键字匹配
		Searcher s3 = factory.openSearcher("test3");
		s3.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$1")),
				Arrays.asList(IndexKeywordHelper.newKeyword("我的"), IndexKeywordHelper.newKeyword("我们的")));
		s3.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$2")),
				Arrays.asList(IndexKeywordHelper.newKeyword("他的"), IndexKeywordHelper.newKeyword("我们的")));
		irs = s3.union(Arrays.asList(IndexKeywordHelper.newKeyword("我的"), IndexKeywordHelper.newKeyword("他的")), null);
		// 结果为order1,order2
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println("场景3:" + ir.getKey());
		}

		// 场景4 范围匹配
		Searcher s4 = factory.openSearcher("test4");
		s4.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$1")),
				Arrays.asList(IndexKeywordHelper.newKeyword("D:20200524"), IndexKeywordHelper.newKeyword("K:我们的")));
		s4.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$2")),
				Arrays.asList(IndexKeywordHelper.newKeyword("D:20200525"), IndexKeywordHelper.newKeyword("K:我们的")));
		irs = s4.searchRange("D:20200524", "D:202005249", null);// 加上属性前端(如D:)后可精确匹配到属性
		// 结果为order1
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println("场景4:" + ir.getKey());
		}
		// 场景5 多范围匹配
		Searcher s5 = factory.openSearcher("test5");
		s5.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$1")),
				Arrays.asList(IndexKeywordHelper.newKeyword("D:20200524"), IndexKeywordHelper.newKeyword("C:20200524"),
						IndexKeywordHelper.newKeyword("K:我们的")));
		s5.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$2")),
				Arrays.asList(IndexKeywordHelper.newKeyword("D:20200525"), IndexKeywordHelper.newKeyword("K:我们的")));
		List<? extends IndexRange> ranges = Arrays.asList(IndexKeywordHelper.newRange("D:20200524", "D:202005249"),
				IndexKeywordHelper.newRange("C:20200524", "C:202005249"));
		List<? extends IndexKeyword> keywords = Collections.emptyList();
		irs = s5.searchRange(ranges, keywords, null);
		// 结果为order1
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println("场景5:" + ir.getKey());
		}

		// 场景6 任意范围匹配
		Searcher s6 = factory.openSearcher("test6");
		s6.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$1")),
				Arrays.asList(IndexKeywordHelper.newKeyword("D:20200524"), IndexKeywordHelper.newKeyword("C:20200524"),
						IndexKeywordHelper.newKeyword("K:我们的")));
		s6.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$2")),
				Arrays.asList(IndexKeywordHelper.newKeyword("D:20200525"), IndexKeywordHelper.newKeyword("K:我们的")));
		ranges = Arrays.asList(IndexKeywordHelper.newRange("D:20200525", "D:202005259"),
				IndexKeywordHelper.newRange("C:20200524", "C:202005249"));
		keywords = Collections.emptyList();
		irs = s5.unionRange(ranges, keywords, null);
		// 结果为order1，order2
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println("场景6:" + ir.getKey());
		}

		// 场景7 匹配率排序
		Searcher s7 = factory.openSearcher("test7");
		s7.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$1")),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的", 1)));
		s7.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$2")),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的", 2)));
		irs = s7.search(SearchOption.valueOf(SearchOption.OPTION_RATE_SORT), "我们的");
		// 结果为order2，order1的顺序
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println("场景7:" + ir.getKey());
		}

		// 场景8 匹配率过滤
		Searcher s8 = factory.openSearcher("test8");
		s8.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$1")),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的", 1)));
		s8.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$2")),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的", 2)));
		s8.updateElement(IndexElementHelper.newElement(UniteId.valueOf("Order$3")),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的", 3)));
		irs = s8.search(SearchOption.valueOf(SearchOption.OPTION_RATE_LEAST).setRate(2), "我们的");
		// 结果为order2,order3
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println("场景8-1:" + ir.getKey());
		}
		irs = s8.search(SearchOption.valueOf(SearchOption.OPTION_RATE_RANGE).setStartRate(1).setEndRate(2), "我们的");
		// 结果为order1,order2
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println("场景8-2:" + ir.getKey());
		}

		// 场景9 按属性排序
		Searcher s9 = factory.openSearcher("test9");
		s9.updateElement(
				IndexElementHelper.newElement(UniteId.valueOf("Order$1"),
						Arrays.asList(IndexAttributeHelper.newAttribute("level", "1"))),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的")));
		s9.updateElement(
				IndexElementHelper.newElement(UniteId.valueOf("Order$2"),
						Arrays.asList(IndexAttributeHelper.newAttribute("level", "2"))),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的")));
		s9.updateElement(
				IndexElementHelper.newElement(UniteId.valueOf("Order$3"),
						Arrays.asList(IndexAttributeHelper.newAttribute("level", "3"))),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的")));
		irs = s9.search(null, "我们的");
		irs.sort("level", IndexResults.OPTION_ORDER_BY_DESC);
		// 结果为order3，order2，order1的顺序
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println("场景7:" + ir.getKey());
		}

		// 场景10 多属性排序
		Searcher s10 = factory.openSearcher("test10");
		s10.updateElement(
				IndexElementHelper.newElement(UniteId.valueOf("Order$1"),
						Arrays.asList(IndexAttributeHelper.newAttribute("level", "1"))),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的")));
		s10.updateElement(
				IndexElementHelper.newElement(UniteId.valueOf("Order$2"),
						Arrays.asList(IndexAttributeHelper.newAttribute("level", "100"),
								IndexAttributeHelper.newAttribute("w", "1000"))),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的")));
		s10.updateElement(
				IndexElementHelper.newElement(UniteId.valueOf("Order$3"),
						Arrays.asList(IndexAttributeHelper.newAttribute("level", "100"),
								IndexAttributeHelper.newAttribute("w", "100"))),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的")));
		irs = s10.search(null, "我们的");
		irs.sort("level", IndexResults.OPTION_ORDER_BY_DESC);
		irs.sort("w", IndexResults.OPTION_ORDER_BY_DESC);
		// 结果为order2，order3，order1的顺序
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println("场景10:" + ir.getKey());
		}

	}
}

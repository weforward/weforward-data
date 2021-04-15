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

import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.data.UniteId;
import cn.weforward.data.search.IndexResult;
import cn.weforward.data.search.IndexResults;
import cn.weforward.data.search.Searcher;
import cn.weforward.data.search.util.IndexAttributeHelper;
import cn.weforward.data.search.util.IndexElementHelper;
import cn.weforward.data.search.util.IndexKeywordHelper;

public class SortTest {

	public static void main(String[] args) {
		MongodbSearcherFactory factory = new MongodbSearcherFactory("mongodb://localhost:27017/", "search");
		Searcher s9 = factory.openSearcher("test");
		s9.updateElement(
				IndexElementHelper.newElement(UniteId.valueOf("Order$1"),
						Arrays.asList(IndexAttributeHelper.newAttribute("level", 1000))),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的")));
		s9.updateElement(
				IndexElementHelper.newElement(UniteId.valueOf("Order$2"),
						Arrays.asList(IndexAttributeHelper.newAttribute("level", 20))),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的")));
		s9.updateElement(
				IndexElementHelper.newElement(UniteId.valueOf("Order$3"),
						Arrays.asList(IndexAttributeHelper.newAttribute("level", 3))),
				Arrays.asList(IndexKeywordHelper.newKeyword("我们的")));
		IndexResults irs = s9.search(null, "我们的");
		irs.sort("level", IndexResults.OPTION_ORDER_BY_DESC);
		for (IndexResult ir : ResultPageHelper.toForeach(irs)) {
			System.out.println(ir.getKey());
		}

	}
}

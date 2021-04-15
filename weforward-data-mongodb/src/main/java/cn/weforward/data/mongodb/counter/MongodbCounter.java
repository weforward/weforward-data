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
package cn.weforward.data.mongodb.counter;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.counter.support.DbCounter;
import cn.weforward.data.mongodb.util.MongodbResultPage;
import cn.weforward.data.mongodb.util.MongodbUtil;

/**
 * 在Mongodb下的计数器实现
 * 
 * @author liangyi
 *
 */
public class MongodbCounter extends DbCounter {
	MongoCollection<Document> m_Collection;

	public MongodbCounter(String name, MongodbCounterFactory factory) {
		super(name, factory);
		m_Collection = factory.m_Db.getCollection(getLableName().toLowerCase());
	}

	protected MongoCollection<Document> getCollection() {
		return m_Collection;
	}

	@Override
	public ResultPage<String> searchRange(String first, String last) {
		return new Result(Filters.and(Filters.gte(MongodbUtil.ID, first), Filters.lte(MongodbUtil.ID, last)));
	}

	@Override
	public ResultPage<String> startsWith(String prefix) {
		if (StringUtil.isEmpty(prefix)) {
			return new Result(null);
		}
		return searchRange(prefix, prefix + StringUtil.UNICODE_REPLACEMENT_STRING);
	}

	/**
	 * 查询结果封装
	 */
	class Result extends MongodbResultPage<String> {

		public Result(Bson filter) {
			super(getCollection(), filter, null);
		}

		@Override
		protected String to(Document doc) {
			return null == doc ? null : doc.getString(MongodbUtil.ID);
		}
	}
}

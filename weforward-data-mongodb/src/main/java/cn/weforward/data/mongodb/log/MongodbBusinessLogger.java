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
package cn.weforward.data.mongodb.log;

import java.util.Date;

import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.log.BusinessLog;
import cn.weforward.data.log.support.AbstractBusinessLogger;
import cn.weforward.data.log.vo.BusinessLogVo;
import cn.weforward.data.mongodb.util.MongodbResultPage;
import cn.weforward.data.mongodb.util.MongodbUtil;

/**
 * Mongodb日志记录器，日志记录存储于“日志器名称_log”的集合里
 * 
 * @author liangyi
 *
 */
public class MongodbBusinessLogger extends AbstractBusinessLogger {
	MongodbBusinessLoggerFactory m_Factory;
	MongoCollection<Document> m_Collection;

	public MongodbBusinessLogger(MongodbBusinessLoggerFactory factory, String name) {
		super(name);
		m_Factory = factory;
		m_Collection = factory.m_Db.getCollection(getCollectionName());
	}

	private String getCollectionName() {
		String name = getName();
		if (name.endsWith("_log")) {
			return name.toLowerCase();
		}
		return name.toLowerCase() + "_log";
	}

	@Override
	public void writeLog(BusinessLog log) {
		Document insert = new Document();
		insert.append(MongodbUtil.ID, log.getId());
		insert.append("ac", toString(log.getAction()));
		insert.append("a", toString(log.getAuthor()));
		insert.append("n", toString(log.getNote()));
		insert.append("w", toString(log.getWhat()));
		m_Collection.insertOne(insert);
	}

	private static BsonString toString(String v) {
		return null == v ? null : new BsonString(v);
	}

	@Override
	public ResultPage<BusinessLog> searchLogs(String id, Date begin, Date end) {
		if (StringUtil.isEmpty(id)) {
			return ResultPageHelper.empty();
		}
		Bson first = Filters.gte(MongodbUtil.ID, toId(id, null == begin ? 0 : begin.getTime()));
		Bson last = Filters.lte(MongodbUtil.ID, toId(id, null == end ? Long.MAX_VALUE : end.getTime()));
		return new Result(Filters.and(first, last));
	}

	@Override
	public String getServerId() {
		return m_Factory.getServerId();
	}

	/**
	 * 查询结果封装
	 */
	class Result extends MongodbResultPage<BusinessLog> {
		public Result(Bson filter) {
			super(m_Collection, filter, null);
		}

		@Override
		protected BusinessLog to(Document doc) {
			if (null == doc) {
				return null;
			}
			BusinessLogVo vo = createVoById(doc.getString(MongodbUtil.ID));
			vo.setAction(doc.getString("ac"));
			vo.setAuthor(doc.getString("a"));
			vo.setNote(doc.getString("n"));
			vo.setWhat(doc.getString("w"));
			return vo;
		}
	}
}

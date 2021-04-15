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

import com.mongodb.client.MongoDatabase;

import cn.weforward.data.log.BusinessLogger;
import cn.weforward.data.log.support.AbstractBusinessLoggerFactory;
import cn.weforward.data.mongodb.util.MongodbUtil;

/**
 * 基于Mongodb的日志工厂
 * 
 * @author liangyi
 *
 */
public class MongodbBusinessLoggerFactory extends AbstractBusinessLoggerFactory {
	protected MongoDatabase m_Db;

	public MongodbBusinessLoggerFactory(String serverId, MongoDatabase db) {
		super(serverId);
		m_Db = db;
	}

	public MongodbBusinessLoggerFactory(String serverId, String connection, String dbname) {
		this(serverId, MongodbUtil.create(connection).getDatabase(dbname));
	}

	@Override
	protected BusinessLogger doCreateLogger(String name) {
		return new MongodbBusinessLogger(this, name);
	}

}

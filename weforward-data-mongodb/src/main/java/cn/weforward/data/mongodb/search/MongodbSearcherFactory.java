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

import com.mongodb.client.MongoDatabase;

import cn.weforward.data.mongodb.util.MongodbUtil;
import cn.weforward.data.search.Searcher;
import cn.weforward.data.search.support.AbstractSearcherFactory;

/**
 * mongodb查找工厂
 * 
 * @author daibo
 *
 */
public class MongodbSearcherFactory extends AbstractSearcherFactory {
	/** 数据库 */
	protected MongoDatabase m_Db;
	/** 服务器id */
	protected String m_ServerId;

	public MongodbSearcherFactory(String connection, String dbname) {
		this(MongodbUtil.create(connection).getDatabase(dbname));
	}

	public MongodbSearcherFactory(MongoDatabase db) {
		m_Db = db;
	}

	/**
	 * 设置服务器id
	 * 
	 * @param sid 服务器id
	 */
	public void setServerId(String sid) {
		m_ServerId = sid;
	}

	@Override
	protected Searcher doCreateSearcher(String name) {
		return new MongodbSearcher(m_Db, name, m_ServerId);
	}

}

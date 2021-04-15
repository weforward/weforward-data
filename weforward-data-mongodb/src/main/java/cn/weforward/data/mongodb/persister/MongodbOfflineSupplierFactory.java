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
package cn.weforward.data.mongodb.persister;

import com.mongodb.client.MongoDatabase;

import cn.weforward.data.mongodb.util.MongodbUtil;
import cn.weforward.data.persister.OfflineSupplier;
import cn.weforward.data.persister.support.AbstractOfflineSupplierFactory;
import cn.weforward.protocol.ext.ObjectMapper;

/**
 * 基于mongodb为远端对象提供脱机支持工厂
 * 
 * @author daibo
 *
 */
public class MongodbOfflineSupplierFactory extends AbstractOfflineSupplierFactory {
	/** 数据库 */
	protected MongoDatabase m_Db;

	public MongodbOfflineSupplierFactory(String connection, String dbname) {
		this(MongodbUtil.create(connection).getDatabase(dbname));
	}

	public MongodbOfflineSupplierFactory(MongoDatabase db) {
		m_Db = db;
	}

	@Override
	protected <E> OfflineSupplier<E> doCreateOfflineSupplier(Class<E> clazz, ObjectMapper<E> mapper, String name) {
		return new MongodbOfflineSupplier<E>(m_Db, mapper, name);
	}
}

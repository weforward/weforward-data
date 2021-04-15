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
package cn.weforward.data.mysql.persister;

import cn.weforward.data.jdbc.DataProvider;
import cn.weforward.data.mysql.EntityWatcher;
import cn.weforward.data.mysql.MysqlConst;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.persister.PersisterSet;
import cn.weforward.data.persister.support.AbstractPersisterFactory;
import cn.weforward.protocol.ext.ObjectMapper;

/**
 * 基于mysql的存储工厂，MySQL版本需要支持JSON格式，版本为 5.7.7以上
 * 
 * @author daibo
 *
 */
public class MysqlPersisterFactory extends AbstractPersisterFactory {
	/** 数据提供者 */
	protected DataProvider m_DataProvider;
	/** 实体监控 */
	protected EntityWatcher m_Watcher;
	/** 默认字符长度 */
	protected int m_DefaultStringLength = MysqlConst.DEFAULT_STRING_LENGTH;

	public MysqlPersisterFactory(String connectionString) {
		this(connectionString, MysqlConst.DEFAULT_POOL_MAX_SIZE);
	}

	public MysqlPersisterFactory(String connectionString, int maxSize) {
		this(MysqlConst.DEFAULT_DRICER_CLASS, connectionString, maxSize);
	}

	public MysqlPersisterFactory(String driverClass, String connectionString, int maxSize) {
		m_DataProvider = new DataProvider(driverClass, connectionString, maxSize);
	}

	public MysqlPersisterFactory(PersisterSet ps, String connectionString) {
		this(ps, connectionString, MysqlConst.DEFAULT_POOL_MAX_SIZE);
	}

	public MysqlPersisterFactory(PersisterSet ps, String connectionString, int maxSize) {
		this(ps, MysqlConst.DEFAULT_DRICER_CLASS, connectionString, maxSize);
	}

	public MysqlPersisterFactory(PersisterSet ps, String driverClass, String connectionString, int maxSize) {
		super(ps);
		m_DataProvider = new DataProvider(driverClass, connectionString, maxSize);
	}

	public void setDefaultStringLength(int length) {
		m_DefaultStringLength = length;
	}

	public void setWatcher(EntityWatcher watcher) {
		m_Watcher = watcher;
	}

	@Override
	protected <E extends Persistent> Persister<E> doCreatePersister(Class<E> clazz, ObjectMapper<E> mapper) {
		MysqlPersister<E> ps = new MysqlPersister<E>(m_DataProvider, mapper, m_DefaultStringLength);
		ps.setWatcher(m_Watcher);
		return ps;
	}

}

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
package cn.weforward.data.jdbc;

import cn.weforward.common.Destroyable;

/**
 * 基于连接池及JDBC数据库事务接口的数据提供者
 * 
 * 其提供的事务在同一线程内共享（即使用同一个数据库连接）
 * 
 * @author liangyi
 * 
 */
public class DataProvider implements Destroyable {
	// 数据库连接池
	ConnectionPool m_ConnectionPool;
	// 以线程为基础的事务集
	TransactionThread m_OnThread = new TransactionThread() {
		// 默认事务集
		private ThreadLocal<TemplateJdbc> m_Transtacions = new ThreadLocal<TemplateJdbc>();

		public Transaction get(Object provider) {
			return m_Transtacions.get();
		}

		public void set(Transaction trans) {
			m_Transtacions.set((TemplateJdbc) trans);
		}
	};

	/**
	 * 默认（线程共享）的事务
	 */
	public static final int TYPE_DEFAULT = 0;
	/**
	 * 孤立的事务
	 */
	public static final int TYPE_ISOLATION = 1;
	/**
	 * 仅取得当前（线程共享）已启动的事务
	 */
	public static final int TYPE_GET = 2;

	protected String m_Database;

	/**
	 * 构建基于单数据库连接池的
	 * 
	 * @param driverClass      数据库驱动类名
	 * @param connectionString 连接串
	 * @param maxSize          连接池最大项
	 */
	public DataProvider(String driverClass, String connectionString, int maxSize) {
		m_Database = findDataBasse(connectionString);
		if (1 == maxSize) {
			// 只需要一个连接，使用假连接池:)
			m_ConnectionPool = new FakeConnectionPool(driverClass, connectionString);
		} else {
			m_ConnectionPool = new ConnectionPoolSingle(driverClass, connectionString, maxSize, 1 * 60);
		}
	}

	public static String findDataBasse(String connectionString) {
		int end = connectionString.indexOf('?');
		int start = connectionString.lastIndexOf('/', end);
		if (start > 0 && end > 0 && start < end) {
			return connectionString.substring(start + 1, end);
		}
		return null;
	}

	/**
	 * 数据库名称
	 * 
	 * @return 数据库名称
	 */
	public String getDatabase() {
		return m_Database;
	}

	/**
	 * 构造基于主从数据库连接池的
	 * 
	 * @param driverClass            数据库驱动类名
	 * @param masterConnectionString 主数据库连接串
	 * @param slaveConnectionString  从数据库连接串
	 * @param maxSize                连接池最大项
	 */
	public DataProvider(String driverClass, String masterConnectionString, String slaveConnectionString, int maxSize) {
		m_ConnectionPool = new ConnectionPoolHotSpare(driverClass, masterConnectionString, slaveConnectionString,
				maxSize, 1 * 60);
	}

	/**
	 * 基于已有连接池构造
	 * 
	 * @param pool 已有连接池
	 */
	public DataProvider(ConnectionPool pool) {
		m_ConnectionPool = pool;
	}

	// public DataProvider() {
	// Shutdown.register(this);
	// }

	public void setTransactionManager(TransactionThread tm) {
		if (null == tm) {
			throw new NullPointerException("tm is null!");
		}
		m_OnThread = tm;
	}

	/*
	 * public void close() { if (null != m_ConnectionPool) {
	 * m_ConnectionPool.freeAllConnections(); } }
	 */
	public void destroy() {
		if (null != m_ConnectionPool) {
			synchronized (this) {
				m_ConnectionPool.freeAllConnections();
				m_ConnectionPool = null;
			}
		}
	}

	/**
	 * 启动/取得当前线程中的数据库事务模板
	 * 
	 * @return 事务模板
	 */
	public TemplateJdbc getTranstacion() {
		return getTranstacion(TYPE_DEFAULT);
	}

	/**
	 * 根据类型启动/取得当前线程的事务
	 * 
	 * @param type 事务类型 TYPE_*
	 * @return 事务模板
	 */
	public TemplateJdbc getTranstacion(int type) throws TransactionException {
		if (TYPE_DEFAULT == type) {
			TemplateJdbc t;
			t = (TemplateJdbc) m_OnThread.get(m_ConnectionPool);
			if (null == t) {
				t = new TemplateJdbc(m_ConnectionPool);
				m_OnThread.set(t);
			}
			return t;
		}
		if (TYPE_ISOLATION == type) {
			return new TemplateJdbc(m_ConnectionPool);
		}
		if (TYPE_GET == type) {
			return (TemplateJdbc) m_OnThread.get(m_ConnectionPool);
		}
		throw new TransactionException("Unknown type: " + type);
	}

	/**
	 * 启动/取得当前线程中的数据库事务模板且开始事务
	 * 
	 * @return jdbc模板
	 */
	public TemplateJdbc beginTranstacion() {
		TemplateJdbc t = getTranstacion(TYPE_DEFAULT);
		t.begin();
		return t;
	}
}

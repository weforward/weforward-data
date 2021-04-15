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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import cn.weforward.common.execption.BusyException;
import cn.weforward.common.sys.StackTracer;

/**
 * 兼容只需要一个连接情况下的虚连接池
 * 
 * @author liangyi
 * 
 */
public class FakeConnectionPool implements ConnectionPool {
	String m_ConnectionString; // 数据库连接串
	ConnectionWraper m_Connection;// 当前数据库连接
	Thread m_Worker; // 当前数据库连接使用者

	/* 实现Pool的接口 ****/
	protected Connection onPoolNewElement() throws SQLException {
		if (null == m_Connection) {
			ConnectionWraper conn = new ConnectionWraper(DriverManager.getConnection(m_ConnectionString));
			m_Connection = conn;
		}
		return m_Connection;
	}

	protected void onPoolDeleteElement(Connection element) {
		// 删除项时清除项
		try {
			element.close();
		} catch (SQLException e) {
			ConnectionPool._Logger.warn(StackTracer.printStackTrace(e, null).toString());
		}
	}

	protected boolean onPoolOvertimeElement(Connection element, long overtime) {
		// 处理超时的项(参数overtime指示超时毫秒),如果返回true将在使用列表中清除,返回否保留在列表中
		// //暂时不作处理
		// /return false;
		// 如果需要的话,强行关闭连接(不要在这强行关闭,在free的时候如果是丢失的会执行OnPoolDeleteElement)
		// /OnPoolDeleteElement(element);
		return true;
	}

	protected boolean onPoolCheckElement(Connection element, long idle) {
		// 执行连接测试,如果返回false将由连接池删除此连接
		return checkConnection(element);
	}

	/* 实现ConnectionPool接口 ****/
	public void freeConnection(Connection conn) {
		if (conn != m_Connection) {
			throw new IllegalArgumentException(conn + "不是受托管的连接" + m_Connection);
		}
		synchronized (this) {
			m_Worker = null;
			// 通知等待连接的线程
			this.notifyAll();
		}
		// 自动释放相关的Statement
		if (ConnectionWraper.class.isInstance(conn)) {
			((ConnectionWraper) conn).freeStatements();
		} else {
			ConnectionPool._Logger.warn("The is not ConnectionWraper: " + conn);
		}
		// free(con, false);
	}

	synchronized public Connection getConnection() throws SQLException {
		Thread requestor = Thread.currentThread();
		if (null != m_Worker && m_Worker != requestor) {
			// 连接还在使用中，只好等待
			try {
				this.wait();
			} catch (InterruptedException e) {
				throw new BusyException(e);
			}
		}
		// 获取连接
		if (null == m_Connection) {
			ConnectionWraper conn = new ConnectionWraper(DriverManager.getConnection(m_ConnectionString));
			m_Connection = conn;
		}
		return m_Connection;
	}

	public void freeConnectionAtException(Connection conn) {
		synchronized (this) {
			if (conn != m_Connection) {
				throw new IllegalArgumentException(conn + "不是受托管的连接" + m_Connection);
			}
			m_Connection = null;
			m_Worker = null;
			// 通知等待连接的线程
			this.notifyAll();
		}
		try {
			conn.rollback();
			conn.close();
		} catch (SQLException e) {
			ConnectionPool._Logger.error(StackTracer.printStackTrace(e, null).toString());
		}
	}

	public void freeAllConnections() {
		// if (ConnectionPool._InfoEnabled) {
		// ConnectionPool._Logger.info(this.hashCode() +
		// " freeAllConnections.");
		// }
		Connection conn;
		synchronized (this) {
			conn = m_Connection;
			m_Connection = null;
			m_Worker = null;
			// 通知等待连接的线程
			this.notifyAll();
		}
		if (null != conn) {
			try {
				conn.rollback();
				conn.close();
			} catch (SQLException e) {
				ConnectionPool._Logger.error(StackTracer.printStackTrace(e, null).toString());
			}
		}
	}

	public String getConnectionDetail(Connection conn) {
		// 获取连接的详情
		Thread thread = m_Worker;
		if (conn == m_Connection && null != thread) {
			// 显示连接的使用（线程）者的调用堆栈，方便跟踪处理
			return StackTracer.printStackTrace(thread, null).toString();
		}
		return "The connection is lost!";
	}

	/**
	 * 构造连接池
	 * 
	 * @param driverClass      JDBC驱动，如：com.mysql.jdbc.Driver
	 * @param connectionString 连接串，如：jdbc:mysql://localhost:3306/数据库名?characterEncoding=UTF-8&amp;useUnicode=true&amp;user=用户名&amp;password=密码
	 */
	public FakeConnectionPool(String driverClass, String connectionString) {
		super();
		if (null != driverClass && driverClass.length() > 0) {
			setDriverClassName(driverClass);
		}
		m_ConnectionString = connectionString;
	}

	/**
	 * 注册JDBC驱动器
	 * 
	 * @param driverClass JDBC驱动，如：com.mysql.jdbc.Driver
	 */
	public void setDriverClassName(String driverClass) {
		try {
			DriverManager.registerDriver((java.sql.Driver) (Class.forName(driverClass).newInstance()));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 测试连接是否有断开，执行一个查询若出错则表示连接有问题
	 * 
	 * @param conn 要测试的数据库连接
	 * @return 是否有断开
	 */
	static public boolean checkConnection(Connection conn) {
		try {
			Statement stm = conn.createStatement();
			stm.executeQuery("select 1");
			stm.close();
		} catch (SQLException e) {
			ConnectionPool._Logger.warn(StackTracer.printStackTrace(e, null).toString());
			return false;
		}
		return true;
	}
}

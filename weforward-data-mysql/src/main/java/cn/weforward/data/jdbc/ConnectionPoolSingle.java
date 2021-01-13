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

import cn.weforward.common.sys.StackTracer;

/**
 * 单一连接源的数据库连接池
 * 
 * @author liangyi
 * 
 */
public class ConnectionPoolSingle extends Pool<Connection> implements ConnectionPool {
	protected int m_MaxSize;
	protected String m_ConnectionString;

	/**** 实现Pool的接口 ****/
	protected Connection onPoolNewElement() throws SQLException {
		// 创建新的项
		ConnectionWraper conn = null;
		conn = new ConnectionWraper(DriverManager.getConnection(m_ConnectionString));
		return conn;
	}

	protected void onPoolDeleteElement(Connection element) {
		// 删除项时清除项
		try {
			element.close();
		} catch (SQLException e) {
			ConnectionPool._Logger.warn("SQL异常", e);
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

	/**** 实现ConnectionPool接口 ****/
	public void freeConnection(Connection con) {
		// 自动释放相关的Statement
		if (ConnectionWraper.class.isInstance(con)) {
			((ConnectionWraper) con).freeStatements();
		} else {
			ConnectionPool._Logger.warn("The is not ConnectionWraper: " + con);
		}
		free(con, false);
	}

	public Connection getConnection() throws SQLException {
		try {
			// 获取连接
			return (Connection) allocate(30 * 1000);
		} catch (Exception e) {
			if (SQLException.class.isInstance(e)) {
				throw (SQLException) e;
			}
			ConnectionPool._Logger.warn("SQL异常", e);
		}
		return null;
	}

	public void freeConnectionAtException(Connection conn) {
		try {
			conn.rollback();
		} catch (SQLException e) {
			ConnectionPool._Logger.warn("SQL异常", e);
		}
		free(conn, true);
	}

	public void freeAllConnections() {
		if (ConnectionPool._InfoEnabled) {
			ConnectionPool._Logger.info(this.hashCode() + " freeAllConnections.");
		}
		close();
	}

	public String getConnectionDetail(Connection conn) {
		// 获取连接的详情
		PoolElement element = this.getUsing(conn);
		if (null != element) {
			// 显示连接的使用（线程）者的调用堆栈，方便跟踪处理
			return StackTracer.printStackTrace(element.getOwner(), null).toString();
		}
		return "The connection is lost!";
	}

	/**
	 * 构造连接池
	 * 
	 * @param driverClass      JDBC驱动，如：com.mysql.jdbc.Driver
	 * @param connectionString 连接串，如：jdbc:mysql://localhost:3306/数据库名?characterEncoding=UTF-8&amp;useUnicode=true&amp;user=用户名&amp;password=密码
	 * @param maxSize          最大连接数
	 * @param overtime         连接占用（不释放）超时值（秒）
	 */
	public ConnectionPoolSingle(String driverClass, String connectionString, int maxSize, int overtime) {
		super();
		if (null != driverClass && driverClass.length() > 0) {
			setDriverClassName(driverClass);
		}
		m_ConnectionString = connectionString;
		// 连接存活检测为每分钟检查一次
		create(maxSize, overtime * 1000, 60 * 1000);
	}

	/**
	 * 直接使用连接串构造（假设需要的JDBC驱动已在系统注册），最大连接数为60，超时值为5分钟
	 * 
	 * @param connectionString 连接串，如：jdbc:mysql://localhost:3306/数据库名?characterEncoding=UTF-8&amp;useUnicode=true&amp;user=用户名&amp;password=密码
	 */
	public ConnectionPoolSingle(String connectionString) {
		this(null, connectionString, 60, 5 * 60);
	}

	//
	// public ConnectionPoolSingle() {
	// }

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
			ConnectionPool._Logger.warn("SQL异常", e);
			return false;
		}
		return true;
	}
}

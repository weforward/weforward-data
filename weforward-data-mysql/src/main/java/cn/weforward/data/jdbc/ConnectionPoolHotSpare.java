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

/**
 * （在单一连接源的基础上增加）支持主从结构数据库的两个连接源的数据库连接池，在主数据库连接失败时切换至从数据库
 * 
 * @author liangyi
 * 
 */
public class ConnectionPoolHotSpare extends ConnectionPoolSingle {
	// 主数据库连接
	private String m_MasterConnectionString;
	// 从数据库连接
	private String m_SlaveConnectionString;
	// 最后切换时间
	private long m_LastSwitch;

	/**
	 * 构造连接池
	 * 
	 * @param driverClass           JDBC驱动，如：com.mysql.jdbc.Driver
	 * @param connectionString      主数据库连接串，如：jdbc:mysql://localhost:3306/数据库名?characterEncoding=UTF-8
	 *                              &amp;useUnicode=true&amp;user=用户名&amp;password=密码
	 * @param slaveConnectionString 从数据库连接串
	 * @param maxSize               最大连接数
	 * @param overtime              连接占用（不释放）超时值（秒）
	 */
	public ConnectionPoolHotSpare(String driverClass, String connectionString, String slaveConnectionString,
			int maxSize, int overtime) {
		super(driverClass, connectionString, maxSize, overtime);
		m_MasterConnectionString = m_ConnectionString;
		m_SlaveConnectionString = slaveConnectionString;
	}

	/********** 实现Pool的接口 **********/
	protected Connection onPoolNewElement() throws SQLException {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(m_ConnectionString);
			return new ConnectionWraper(conn);
		} catch (SQLException e) {
			// if (e.getCause() instanceof java.net.ConnectException) {
			if (e.getCause() instanceof java.io.IOException) {
				ConnectionPool._Logger.warn("连接失败(将尝试切换):" + m_ConnectionString, e);
				// 尝试切换到另一个（主/从）服务器，再连接
				if (!switchTo()) {
					// 若切换失败，throw出错误
					throw e;
				}
			} else {
				throw e;
			}
		}
		conn = DriverManager.getConnection(m_ConnectionString);
		return new ConnectionWraper(conn);
	}

	@Override
	protected boolean onPoolCheckElement(Connection element, long idle) {
		Connection conn = (java.sql.Connection) element;
		return checkConnection(conn);
	}

	// 若需要更及时的知道数据库失效，打开以下方法的注释（但会影响性能）
	/*
	 * @Override public Connection getConnection() throws SQLException { Connection
	 * conn = super.getConnection(); // 取得连接后检查连接状态 if (!checkConnection(conn)) { //
	 * 若连接是断的，检查连接源并尝试切换 checkAndSwitch(); conn = super.getConnection(); } return
	 * conn; }
	 */

	@Override
	public void freeConnectionAtException(Connection conn) {
		try {
			conn.rollback();
		} catch (SQLException e) {
			ConnectionPool._Logger.warn("SQL异常", e);
		}
		// 放回池中，且检查连接情况
		if (!free(conn, true)) {
			// 若连接是断的，检查连接源并尝试切换
			checkAndSwitch();
		}
	}

	/**
	 * 切换到从数据库
	 * 
	 * @return 成功返回true
	 */
	public boolean toSlave() {
		synchronized (m_Lock) {
			if (null == m_SlaveConnectionString) {
				// 没指定从数据库
				return false;
			}
			if (!m_ConnectionString.equals(m_SlaveConnectionString)) {
				if (null == m_MasterConnectionString) {
					// m_ConnectionString保存至m_MasterConnectionString
					m_MasterConnectionString = m_ConnectionString;
				}
				// 切换到从数据库
				m_ConnectionString = m_SlaveConnectionString;
				// 清洗连接池
				clear();
				m_LastSwitch = System.currentTimeMillis();
				ConnectionPool._Logger.warn("已切换至从数据库：" + m_ConnectionString);
				return true;
			}
			return true;
		}
	}

	/**
	 * 切换回主数据库
	 * 
	 * @return 成功则返回true
	 */
	public boolean toMaster() {
		synchronized (m_Lock) {
			if (null != m_MasterConnectionString && !m_ConnectionString.equals(m_MasterConnectionString)) {
				// 切换到从数据库
				m_ConnectionString = m_MasterConnectionString;
				// 清洗连接池
				clear();
				m_LastSwitch = System.currentTimeMillis();
				ConnectionPool._Logger.warn("已切换至主数据库：" + m_ConnectionString);
				return true;
			}
			return true;
		}
	}

	/**
	 * 在主/从间切换
	 * 
	 * @return 切换成功返回true
	 */
	protected boolean switchTo() {
		synchronized (m_Lock) {
			long now = System.currentTimeMillis();
			if ((m_LastSwitch + (5 * 60 * 1000)) > now) {
				// 不允许在5分钟内切换第二次
				return false;
			}
			if (m_ConnectionString.equals(m_SlaveConnectionString)) {
				// 现时使用的是从数据库，切换到主
				return toMaster();
			}
			if (m_ConnectionString.equals(m_MasterConnectionString) || null == m_MasterConnectionString) {
				// 现时使用的是主数据库，切换到从
				return toSlave();
			}
			return false;
		}
	}

	/**
	 * 检查当前连接源，且在无法连接时尝试切换
	 * 
	 * @return 返回true表示连接源有效或在无效时已成功连接
	 */
	protected boolean checkAndSwitch() {
		try {
			// 创建一个新连接以确认是数据库不能连接
			Connection conn = DriverManager.getConnection(m_ConnectionString);
			// 连接成功，删除创建的新连接
			onPoolDeleteElement(conn);
			return true;
		} catch (SQLException e) {
			// 应该是数据库连接不了，切换到另一个（主/从）服务器
			if (e.getCause() instanceof java.io.IOException) {
				ConnectionPool._Logger.error("连接失败(将尝试切换):" + m_ConnectionString, e);
				return switchTo();
			}
		}
		return false;
	}
}

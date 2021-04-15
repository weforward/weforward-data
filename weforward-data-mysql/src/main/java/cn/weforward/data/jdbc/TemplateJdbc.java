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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import cn.weforward.common.sys.StackTracer;

/**
 * JDBC的事务接口，同时封装了部分JDBC操作
 * 
 * @author liangyi
 * 
 */
public class TemplateJdbc extends SimpleTransaction {
	// 数据库连接池
	protected ConnectionPool m_ConnectionPool;
	// 最后执行update语句的statement缓存
	protected Statement m_SqlUpdateStatement;
	// 由连接池取得的连接
	protected Connection m_SqlConnection;
	// 最后执行的SQL语句
	protected String m_SqlLastString;

	TransactionDeliver m_Deliver = new TransactionDeliver() {
		public void doCommit() throws TransactionException {
			if (null == m_SqlConnection) {
				if (ConnectionPool._TraceEnabled)
					ConnectionPool._Logger.trace("#" + this.hashCode() + " commit false(transaction not begin)!");
				return;
			}
			try {
				m_SqlConnection.commit();
			} catch (SQLException e) {
				freeConnectionAtException();
				throw new TransactionException(e.getMessage(), e);
			}
			if (ConnectionPool._TraceEnabled) {
				ConnectionPool._Logger.trace("#" + this.hashCode() + " JDBC transaction commit.");
			}
			freeConnection();
			return;
		}

		public void doRollback() throws TransactionException {
			if (null == m_SqlConnection) {
				if (ConnectionPool._TraceEnabled)
					ConnectionPool._Logger
							.trace("#" + this.hashCode() + " JDBC transaction rollback false(transaction not begin)!");
				return;
			}
			try {
				m_SqlConnection.rollback();
			} catch (SQLException e) {
				// /ConnectionPool._Logger.error("#" + this.hashCode() +
				// " rollback failed!",e);
				freeConnectionAtException();
				return;
			}
			if (ConnectionPool._TraceEnabled) {
				ConnectionPool._Logger.trace("#" + this.hashCode() + " JDBC transaction rollback.");
			}
			freeConnection();
		}

		public String getDetail() {
			if (null != m_SqlConnection) {
				return m_ConnectionPool.getConnectionDetail(m_SqlConnection);
			}
			return "Not connection.";
		}
	};

	@Override
	public void begin() {
		/*
		 * try { // 先创建连接 establishConnection(); } catch (SQLException e) {
		 * freeConnectionAtException(); throw new TransactionException(e); }
		 */
		if (null == m_SqlConnection) {
			// 先创建连接
			try {
				m_SqlConnection = m_ConnectionPool.getConnection();
				if (null == m_SqlConnection) {
					throw new TransactionException(
							"#" + this.hashCode() + " JDBC transaction get SQL connection fail at pool!");
				}
				// 启用数据库连接事务支持
				m_SqlConnection.setAutoCommit(false);
				// if (ConnectionPool._TraceEnabled) {
				// ConnectionPool._Logger.trace("#" + this.hashCode()
				// + " JDBC transaction establishConnection.");
				// }
			} catch (SQLException e) {
				freeConnectionAtException();
				throw new TransactionException(e);
			}
		}
		super.begin();
	}

	public void notifyException(Exception e) {
		if (SQLException.class.isInstance(e)) {
			// 如果是SQL异常
			StringBuilder sb = new StringBuilder();
			if (null != m_SqlLastString) {
				sb.append("SQL exception: ").append(m_SqlLastString).append('\n');
			}
			StackTracer.printStackTrace(e, sb);
			ConnectionPool._Logger.error(sb.toString());
			if (null != m_SqlConnection) {
				// rollback the process
				freeConnectionAtException();
				rollback();
			}
			return;
		}
		// throw new TransactionException(e.getMessage(),e);
	}

	public TransactionDeliver getDeliver() {
		return m_Deliver;
	}

	public Object getProvider() {
		return m_ConnectionPool;
	}

	public TemplateJdbc(ConnectionPool dbPool) {
		m_ConnectionPool = dbPool;
	}

	public ConnectionPool getConnectionPool() {
		return m_ConnectionPool;
	}

	public void setConnectionPool(ConnectionPool pool) {
		m_ConnectionPool = pool;
	}

	public Connection sqlGetConnection() {
		return m_SqlConnection;
	}

	/**
	 * 执行一条UPDATE/INSERT/DELETE语句
	 * 
	 * @param sql 数据库语句
	 * @return 影响行数
	 * @throws SQLException 数据库异常
	 */
	public int sqlExecuteUpdate(String sql) throws SQLException {
		m_SqlLastString = sql;
		if (null == m_SqlUpdateStatement) {
			try {
				m_SqlUpdateStatement = sqlGetConnection().createStatement();
			} catch (SQLException e) {
				notifyException(e);
				throw e;
			}
		}
		int ret = m_SqlUpdateStatement.executeUpdate(sql);
		return ret;
	}

	/**
	 * 创建PrepareStatement
	 * 
	 * @param sql 数据库语句
	 * @return 声明
	 * @throws SQLException 数据库异常
	 */
	public PreparedStatement sqlPrepareStatement(String sql) throws SQLException {
		m_SqlLastString = sql;
		try {
			return sqlGetConnection().prepareStatement(sql);
		} catch (SQLException e) {
			notifyException(e);
			throw e;
		}
	}

	/**
	 * 执行一条查询语句
	 * 
	 * @param sql 数据库语句
	 * @return 结果集
	 * @throws SQLException 数据库异常
	 */
	public ResultSet sqlExecuteQuery(String sql) throws SQLException {
		m_SqlLastString = sql;
		Statement stm;
		try {
			stm = sqlGetConnection().createStatement();
		} catch (SQLException e) {
			notifyException(e);
			throw e;
		}
		return stm.executeQuery(sql);
	}

	/**
	 * 查询结果数
	 * 
	 * @param sql 数据库语句
	 * @return 条数
	 * @throws SQLException 数据库异常
	 */
	public int sqlCount(String sql) throws SQLException {
		// // 去除order by子句
		// int idx = sql.toLowerCase().lastIndexOf("order by");
		// if (-1 != idx) {
		// sql = sql.substring(0, idx);
		// }
		ResultSet rs = sqlExecuteQuery("SELECT count(*) AS cc FROM (" + sql + ")T");
		int count = 0;
		if (rs.next()) {
			count = rs.getInt(1);
		}
		rs.close();
		return count;
	}

	/**
	 * 把连接归还连接池
	 */
	protected void freeConnection() {
		m_SqlUpdateStatement = null;
		m_SqlLastString = null;
		if (null != m_SqlConnection) {
			m_ConnectionPool.freeConnection(m_SqlConnection);
			m_SqlConnection = null;
		}
	}

	/**
	 * SQLException时归还连接池
	 */
	protected void freeConnectionAtException() {
		m_SqlUpdateStatement = null;
		m_SqlLastString = null;
		if (null != m_SqlConnection) {
			m_ConnectionPool.freeConnectionAtException(m_SqlConnection);
			m_SqlConnection = null;
			// rollback();
		}
	}
}

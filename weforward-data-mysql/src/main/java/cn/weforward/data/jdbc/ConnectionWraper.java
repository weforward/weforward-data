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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import cn.weforward.common.sys.StackTracer;

/**
 * 封装java.sql.Connection进入连接池 ，避免连接在使用过程中创建的Statement及ResultSet忘记关闭
 * 
 * @author liangyi
 * 
 */
public class ConnectionWraper implements java.sql.Connection {
	java.sql.Connection m_Host;
	ArrayList<Statement> m_Statements;

	public ConnectionWraper(java.sql.Connection conn) {
		m_Host = conn;
		m_Statements = new ArrayList<Statement>();
	}

	/**
	 * 关闭所有当前相关Statement
	 */
	public void freeStatements() {
		int size = m_Statements.size();
		Statement stm;
		for (int i = 0; i < size; i++) {
			stm = m_Statements.get(i);
			try {
				stm.close();
			} catch (SQLException e) {
				Pool._Logger.error(StackTracer.printStackTrace(e, null).toString());
			}
		}
		stm = null;
		m_Statements.clear();
	}

	public void clearWarnings() throws SQLException {
		m_Host.clearWarnings();
	}

	public void close() throws SQLException {
		freeStatements();
		m_Host.close();
	}

	public void commit() throws SQLException {
		freeStatements();
		m_Host.commit();
	}

	public Statement createStatement() throws SQLException {
		Statement stm = m_Host.createStatement();
		m_Statements.add(stm);
		return stm;
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		Statement stm = m_Host.createStatement(resultSetType, resultSetConcurrency);
		m_Statements.add(stm);
		return stm;
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		Statement stm = m_Host.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		m_Statements.add(stm);
		return stm;
	}

	public boolean getAutoCommit() throws SQLException {
		return m_Host.getAutoCommit();
	}

	public String getCatalog() throws SQLException {
		return m_Host.getCatalog();
	}

	public int getHoldability() throws SQLException {
		return m_Host.getHoldability();
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		return m_Host.getMetaData();
	}

	public int getTransactionIsolation() throws SQLException {
		return m_Host.getTransactionIsolation();
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return m_Host.getTypeMap();
	}

	public SQLWarning getWarnings() throws SQLException {
		return m_Host.getWarnings();
	}

	public boolean isClosed() throws SQLException {
		return m_Host.isClosed();
	}

	public boolean isReadOnly() throws SQLException {
		return m_Host.isReadOnly();
	}

	public String nativeSQL(String sql) throws SQLException {
		return m_Host.nativeSQL(sql);
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		CallableStatement stm = m_Host.prepareCall(sql);
		m_Statements.add(stm);
		return stm;
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		CallableStatement stm = m_Host.prepareCall(sql, resultSetType, resultSetConcurrency);
		m_Statements.add(stm);
		return stm;
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		CallableStatement stm = m_Host.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		m_Statements.add(stm);
		return stm;
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		PreparedStatement stm = m_Host.prepareStatement(sql);
		m_Statements.add(stm);
		return stm;
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		PreparedStatement stm = m_Host.prepareStatement(sql, autoGeneratedKeys);
		m_Statements.add(stm);
		return stm;
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		PreparedStatement stm = m_Host.prepareStatement(sql, columnIndexes);
		m_Statements.add(stm);
		return stm;
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		PreparedStatement stm = m_Host.prepareStatement(sql, columnNames);
		m_Statements.add(stm);
		return stm;
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		PreparedStatement stm = m_Host.prepareStatement(sql, resultSetType, resultSetConcurrency);
		m_Statements.add(stm);
		return stm;
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		PreparedStatement stm = m_Host.prepareStatement(sql, resultSetType, resultSetConcurrency);
		m_Statements.add(stm);
		return stm;
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		m_Host.releaseSavepoint(savepoint);
	}

	public void rollback() throws SQLException {
		freeStatements();
		m_Host.rollback();
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		// freeStatements();
		m_Host.rollback(savepoint);
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		m_Host.setAutoCommit(autoCommit);
	}

	public void setCatalog(String catalog) throws SQLException {
		m_Host.setCatalog(catalog);
	}

	public void setHoldability(int holdability) throws SQLException {
		m_Host.setHoldability(holdability);
	}

	public void setReadOnly(boolean readOnly) throws SQLException {
		m_Host.setReadOnly(readOnly);
	}

	public Savepoint setSavepoint() throws SQLException {
		return m_Host.setSavepoint();
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		return m_Host.setSavepoint(name);
	}

	public void setTransactionIsolation(int level) throws SQLException {
		m_Host.setTransactionIsolation(level);
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		m_Host.setTypeMap(map);
	}

	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return m_Host.createArrayOf(typeName, elements);
	}

	public Blob createBlob() throws SQLException {
		return m_Host.createBlob();
	}

	public Clob createClob() throws SQLException {
		return m_Host.createClob();
	}

	public NClob createNClob() throws SQLException {
		return m_Host.createNClob();
	}

	public SQLXML createSQLXML() throws SQLException {
		return m_Host.createSQLXML();
	}

	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return m_Host.createStruct(typeName, attributes);
	}

	public Properties getClientInfo() throws SQLException {
		return m_Host.getClientInfo();
	}

	public String getClientInfo(String name) throws SQLException {
		return m_Host.getClientInfo(name);
	}

	public boolean isValid(int timeout) throws SQLException {
		return m_Host.isValid(timeout);
	}

	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		m_Host.setClientInfo(properties);

	}

	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		m_Host.setClientInfo(name, value);

	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return m_Host.isWrapperFor(iface);
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return m_Host.unwrap(iface);
	}

	public void abort(Executor arg0) throws SQLException {
		// TODO Auto-generated method stub

	}

	public int getNetworkTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getSchema() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setNetworkTimeout(Executor arg0, int arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void setSchema(String arg0) throws SQLException {
		// TODO Auto-generated method stub
	}
}

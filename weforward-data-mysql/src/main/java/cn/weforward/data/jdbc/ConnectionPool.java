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
import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.LoggerFactory;

/**
 * JDBC数据库连接池接口
 * 
 * @author liangyi
 * 
 */
public interface ConnectionPool {
	/**
	 * 日志记录器
	 */
	public final static org.slf4j.Logger _Logger = LoggerFactory.getLogger(ConnectionPool.class);
	/**
	 * 是否允许trace，通常写法
	 * 
	 * <pre>
	 * if(Pool._TraceEnabled){
	 * 	Pool._Logger.trace(...);
	 * }
	 * </pre>
	 */
	public final static boolean _TraceEnabled = _Logger.isTraceEnabled();
	public final static boolean _DebugEnabled = _Logger.isDebugEnabled();
	public final static boolean _InfoEnabled = _Logger.isInfoEnabled();
	public final static boolean _WarnEnabled = _Logger.isWarnEnabled();

	/**
	 * 由池获取连接
	 * 
	 * @return 数据库连接
	 * @throws SQLException 数据库异常
	 */
	Connection getConnection() throws SQLException;

	/**
	 * 释放由池取得的数据库连接回池
	 * 
	 * @param conn 由池取得的数据库连接
	 */
	void freeConnection(Connection conn);

	/**
	 * 在非业务性异常发生时，使用其代替freeConnection释放由池取得的数据库连接回池，以检查连接的可靠性
	 * 
	 * @param conn 由池取得的数据库连接
	 */
	void freeConnectionAtException(Connection conn);

	/**
	 * 关闭连接池中所有连接（清空连接池）
	 */
	void freeAllConnections();

	/**
	 * 获取连接的详情
	 * 
	 * @param conn 数据库连接
	 * @return 关于此连接的状态描述
	 */
	String getConnectionDetail(Connection conn);
}

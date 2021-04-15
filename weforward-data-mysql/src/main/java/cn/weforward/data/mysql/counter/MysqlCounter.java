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
package cn.weforward.data.mysql.counter;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.counter.support.DbCounter;
import cn.weforward.data.jdbc.DataProvider;
import cn.weforward.data.jdbc.SqlString;
import cn.weforward.data.mysql.util.MysqlResultPage;

/**
 * 在mysql下的计数器实现
 * 
 * @author liangyi
 *
 */
public class MysqlCounter extends DbCounter {
	/** SQL串编码后的表名 */
	protected String m_TableName;
	private boolean m_Init;

	public MysqlCounter(String name, MysqlCounterFactory factory) {
		super(name, factory);
		m_TableName = SqlString.escape(super.getLableName());
		m_Init = false;
	}

	protected DataProvider getDataProvider() {
		init();
		return ((MysqlCounterFactory) m_Factory).m_DataProvider;
	}

	public String getLableName() {
		return m_TableName;
	}

	private void init() {
		if (m_Init) {
			return;
		}
		synchronized (this) {
			if (!m_Init) {
				((MysqlCounterFactory) m_Factory).init(m_TableName);
				m_Init = true;
			}
		}
	}

	@Override
	public ResultPage<String> searchRange(String first, String last) {
		String where;
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			if (!StringUtil.isEmpty(first)) {
				builder.append("id>='");
				SqlString.escape(first, builder).append("'");
			}
			if (!StringUtil.isEmpty(last)) {
				if (builder.length() > 0) {
					builder.append(" AND ");
				}
				builder.append("id<='");
				SqlString.escape(last, builder).append("'");
			}
			where = builder.toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
		return new Result(getDataProvider(), getLableName(), where);
	}

	@Override
	public ResultPage<String> startsWith(String prefix) {
		if (StringUtil.isEmpty(prefix)) {
			return new Result(getDataProvider(), getLableName(), null);
		}
		String where;
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			builder.append("id>='");
			SqlString.escape(prefix, builder).append("' AND ");
			builder.append("id<='");
			SqlString.escape(prefix, builder).append(StringUtil.UNICODE_REPLACEMENT_STRING).append("'");
			where = builder.toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
		return new Result(getDataProvider(), getLableName(), where);
	}

	/**
	 * 查询结果封装
	 */
	static class Result extends MysqlResultPage<String> {

		public Result(DataProvider provider, String tabelName, String where) {
			super(provider, tabelName, "id", where, null);
		}

		@Override
		protected String to(ResultSet rs) throws SQLException {
			return rs.getString(1);
		}

	}
}

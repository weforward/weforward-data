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
package cn.weforward.data.mysql.util;

import java.io.Closeable;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.ResultPage;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.exception.DataAccessException;
import cn.weforward.data.jdbc.DataProvider;
import cn.weforward.data.jdbc.TemplateJdbc;

/**
 * mysql结果页
 * 
 * @author daibo
 *
 * @param <E> 参数
 */
public abstract class MysqlResultPage<E> implements ResultPage<E>, Closeable {
	/** 日志 */
	private static final Logger _Logger = LoggerFactory.getLogger(MysqlResultPage.class);
	/** 总大小 */
	int m_Count = -1;
	/** 页大小 */
	int m_PageSize = 200;
	/** 当前页 */
	int m_Page;
	/** 数据供应商 */
	protected DataProvider m_Provider;
	/** 数据缓存 */
	protected List<E> m_Caches;
	/** 当前位置 */
	protected int m_Current;
	/** 限制条数 */
	protected int m_Limit;

	protected String m_TabelName;

	protected String m_Field;

	protected String m_Where;

	protected String m_OrderBy;

	public MysqlResultPage(DataProvider provider, String tabelName, String field, String where, String orderBy) {
		m_Provider = provider;
		m_TabelName = tabelName;
		m_Field = field;
		m_Where = where;
		m_OrderBy = orderBy;
	}

	@Override
	public int getCount() {
		if (m_Count < 0) {
			TemplateJdbc jdbc = null;
			ResultSet rs = null;
			try {
				jdbc = m_Provider.beginTranstacion();
				String sql = getCountSql();
				if (_Logger.isTraceEnabled()) {
					_Logger.trace("exe " + sql);
				}
				rs = jdbc.sqlExecuteQuery(sql);
				long c = 0;
				while (rs.next()) {
					c = rs.getLong(1);
				}
				if (c > Integer.MAX_VALUE) {
					m_Count = Integer.MAX_VALUE;
				} else {
					m_Count = (int) c;
				}
				jdbc.commit();
			} catch (SQLException e) {
				throw new DataAccessException("查询异常", e);
			} finally {
				if (null != rs) {
					try {
						rs.close();
					} catch (SQLException e) {
						_Logger.warn("忽略关键异常");
					}
				}
				if (null != jdbc && !jdbc.isCompleted()) {
					jdbc.rollback();
				}
			}
		}
		return m_Count;
	}

	@Override
	public int getPageCount() {
		int count = getCount();
		int size = getPageSize();
		return count / size + (count % size == 0 ? 0 : 1);
	}

	@Override
	public int getPageSize() {
		return m_PageSize;
	}

	@Override
	public void setPageSize(int size) {
		m_PageSize = size;
	}

	@Override
	public void setPage(int page) {
		m_Page = page;
		gotoPage(page);
	}

	@Override
	public int getPage() {
		return m_Page;
	}

	@Override
	public boolean gotoPage(int page) {
		if (page <= 0 || page > getPageCount()) {
			return false;
		}
		int start = (page - 1) * getPageSize();
		int size = getPageSize();
		if (m_Limit > 0) {
			size = Math.min(m_Limit, size);
		}
		String sql = getQuerySql(start, size);
		TemplateJdbc jdbc = null;
		ResultSet rs = null;
		try {
			jdbc = m_Provider.beginTranstacion();
			rs = jdbc.sqlExecuteQuery(sql);
			if (_Logger.isTraceEnabled()) {
				_Logger.trace("exe " + sql);
			}
			List<E> list = new ArrayList<>();
			while (rs.next()) {
				list.add(to(rs));
			}
			jdbc.commit();
			m_Page = page;
			m_Caches = list;
			m_Current = 0;
			return true;
		} catch (SQLException e) {
			throw new DataAccessException("查询异常", e);
		} finally {
			if (null != rs) {
				try {
					rs.close();
				} catch (SQLException e) {
					_Logger.warn("忽略关键异常");
				}
			}
			if (null != jdbc && !jdbc.isCompleted()) {
				jdbc.rollback();
			}
		}

	}

	@Override
	public E prev() {
		if (null == m_Caches || 0 <= m_Current) {
			return null;
		}
		return m_Caches.get(--m_Current);
	}

	@Override
	public boolean hasPrev() {
		return null != m_Caches && !m_Caches.isEmpty() && m_Current > 0;
	}

	@Override
	public E next() {
		if (null == m_Caches || m_Current >= m_Caches.size()) {
			return null;
		}
		return m_Caches.get(m_Current++);
	}

	@Override
	public boolean hasNext() {
		return null == m_Caches ? false : m_Current < m_Caches.size();
	}

	@Override
	public E move(int pos) {
		if (null == m_Caches || pos < 0 || pos >= m_Caches.size()) {
			return null;
		}
		m_Current = pos;
		return m_Caches.get(m_Current);
	}

	@Override
	public Iterator<E> iterator() {
		return null == m_Caches ? Collections.emptyIterator() : m_Caches.iterator();
	}

	@Override
	public void close() throws IOException {
		m_Caches = null;
	}

	protected String getQuerySql(int start, int size) {
		return "SELECT " + (StringUtil.isEmpty(m_Field) ? "*" : m_Field) + " FROM " + m_TabelName
				+ (StringUtil.isEmpty(m_Where) ? "" : " WHERE " + m_Where)
				+ (StringUtil.isEmpty(m_OrderBy) ? "" : " ORDER BY " + m_OrderBy) + " LIMIT " + start + "," + size;
	}

	protected String getCountSql() {
		return "SELECT count(*) FROM " + m_TabelName + (StringUtil.isEmpty(m_Where) ? "" : " WHERE " + m_Where);
	}

	protected abstract E to(ResultSet rs) throws SQLException;

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}

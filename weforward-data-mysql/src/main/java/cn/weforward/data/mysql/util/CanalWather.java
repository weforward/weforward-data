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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry.Column;
import com.alibaba.otter.canal.protocol.CanalEntry.Entry;
import com.alibaba.otter.canal.protocol.CanalEntry.EntryType;
import com.alibaba.otter.canal.protocol.CanalEntry.EventType;
import com.alibaba.otter.canal.protocol.CanalEntry.RowChange;
import com.alibaba.otter.canal.protocol.CanalEntry.RowData;
import com.alibaba.otter.canal.protocol.Message;

import cn.weforward.common.util.StringUtil;
import cn.weforward.data.mysql.EntityListener;
import cn.weforward.data.mysql.EntityListener.ChangeEntity;
import cn.weforward.data.mysql.EntityWatcher;

public class CanalWather implements Runnable, EntityWatcher {
	/** 监控异常 */
	protected static final Logger _Logger = LoggerFactory.getLogger(CanalWather.class);
	protected CanalConnector m_Connector;
	protected SocketAddress m_Address;
	protected String m_Username;
	protected String m_Password;
	protected String m_Destination;
	protected String m_Filter = ".*\\..*";;
	List<EntityListener> m_Listeners;
	/* 线程 */
	Thread m_Thread;
	/* 错误数 */
	int m_ErrorNum;

	public CanalWather(String hostname, int port, String destination, String username, String password) {
		m_Listeners = new ArrayList<>();
		m_Address = new InetSocketAddress(hostname, port);
		m_Destination = destination;
		m_Username = username;
		m_Password = password;
	}

	public void setDatabase(String db) {
		setFilter(db + "\\..*");
	}

	public void setFilter(String filter) {
		m_Filter = filter;
	}

	public void start() {
		if (null != m_Thread) {
			return;
		}
		m_Thread = new Thread(this, "canalwatcher-" + m_Filter);
		m_Thread.start();
	}

	public void stop() {
		if (null != m_Connector) {
			m_Connector.stopRunning();
			m_Connector = null;
		}
		Thread old = m_Thread;
		m_Thread = null;
		old.interrupt();

	}

	@Override
	public void run() {
		while (null != m_Thread) {
			try {
				doLoop();
			} catch (InterruptedException e) {
				break;
			} catch (Throwable e) {
				_Logger.error("监控程序异常," + (++m_ErrorNum) + "秒后重试", e);
				synchronized (this) {
					try {
						this.wait(m_ErrorNum * 1000);
					} catch (InterruptedException ee) {
						break;
					}
				}
			}
		}
	}

	private void doLoop() throws InterruptedException {
		while (null != m_Thread) {
			m_Connector = CanalConnectors.newSingleConnector(m_Address, m_Destination, m_Username, m_Password);
			try {
				m_Connector.connect();
				// connector.subscribe(".*\\..*");
				// m_Connector.subscribe(m_Database + "." + m_TableName);
				m_Connector.subscribe(m_Filter);
				m_Connector.rollback();
				while (null != m_Thread) {
					Message message = m_Connector.get(1, 10l, TimeUnit.SECONDS);
					for (Entry entry : message.getEntries()) {
						if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN
								|| entry.getEntryType() == EntryType.TRANSACTIONEND) {
							continue;
						}
						String schemaName = entry.getHeader().getSchemaName();
						String tableName = entry.getHeader().getTableName();
						RowChange rowChage = null;
						try {
							rowChage = RowChange.parseFrom(entry.getStoreValue());
						} catch (Exception e) {
							_Logger.warn("忽略解析异常 data:" + entry.toString(), e);
							continue;
						}
						EventType eventType = rowChage.getEventType();

						if (eventType == EventType.DELETE) {
							for (RowData rowData : rowChage.getRowDatasList()) {
								onChange(schemaName, tableName,
										changeEntiy(EntityListener.DELETE, rowData.getBeforeColumnsList()));
							}
						} else if (eventType == EventType.INSERT) {
							for (RowData rowData : rowChage.getRowDatasList()) {
								onChange(schemaName, tableName,
										changeEntiy(EntityListener.INSERT, rowData.getAfterColumnsList()));
							}
						} else if (eventType == EventType.UPDATE) {
							for (RowData rowData : rowChage.getRowDatasList()) {
								onChange(schemaName, tableName,
										changeEntiy(EntityListener.UPDATE, rowData.getAfterColumnsList()));
							}
						}
					}
				}
			} catch (Throwable e) {
				_Logger.warn("监控异常", e);
			} finally {
				m_Connector.disconnect();
				m_Connector = null;
			}
		}

	}

	private void onChange(String database, String tabelName, ChangeEntity entity) {
		List<EntityListener> ls = m_Listeners;
		for (EntityListener l : ls) {
			if (null == l) {
				continue;
			}
			if (!StringUtil.isEmpty(l.getDatabase()) && !StringUtil.eq(l.getDatabase(), database)) {
				continue;
			}
			if (!StringUtil.isEmpty(l.getTabelName()) && !StringUtil.eq(l.getTabelName(), tabelName)) {
				continue;
			}
			try {
				l.onChange(entity);
			} catch (Throwable e) {
				_Logger.warn("忽略通知" + l + "出错", e);
			}
		}

	}

	private ChangeEntity changeEntiy(int type, List<Column> list) {
		Map<String, String> map = new HashMap<String, String>(list.size());
		for (Column col : list) {
			map.put(col.getName(), col.getValue());
		}
		return new ChangeEntity(type, map);
	}

	@Override
	public synchronized void register(EntityListener l) {
		if (null == l) {
			return;
		}
		List<EntityListener> ls = m_Listeners;
		List<EntityListener> result = new ArrayList<>(ls.size() + 1);
		for (EntityListener e : ls) {
			if (e.equals(l)) {
				return;
			}
			result.add(e);
		}
		result.add(l);
		m_Listeners = result;
		start();
	}

	@Override
	public synchronized void unRegister(EntityListener l) {
		if (null == l) {
			return;
		}
		List<EntityListener> ls = m_Listeners;
		List<EntityListener> result = new ArrayList<>(ls.size() - 1);
		boolean find = false;
		for (EntityListener e : ls) {
			if (e.equals(l)) {
				find = true;
				continue;
			}
			result.add(e);
		}
		if (find) {
			m_Listeners = result;
			if (result.isEmpty()) {
				stop();
			}
		}

	}
}

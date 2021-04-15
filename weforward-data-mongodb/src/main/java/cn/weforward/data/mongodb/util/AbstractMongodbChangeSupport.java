package cn.weforward.data.mongodb.util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoCommandException;
import com.mongodb.MongoInterruptedException;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;

/**
 * mongodb监控
 * 
 * @author daibo
 *
 */
public abstract class AbstractMongodbChangeSupport implements Runnable {
	/** 日志 */
	private static final Logger _Logger = LoggerFactory.getLogger(AbstractMongodbChangeSupport.class);
	/** 线程 */
	private Thread m_Thread;
	/** 错误次数 */
	private int m_ErrorNum;
	/** 最大等待时间 */
	private long m_MaxAwaitTime = 60 * 1000;
	/** db数据库 */
	private MongoDatabase m_Db;

	private MongoCursor<ChangeStreamDocument<Document>> m_Cursor;

	private static final AtomicInteger INC = new AtomicInteger();

	private long m_LastActivity;

	public AbstractMongodbChangeSupport(MongoDatabase db) {
		m_Db = db;
		m_LastActivity = System.currentTimeMillis();
	}

	public void setMaxAwaitTime(long v) {
		m_MaxAwaitTime = v;
	}

	/**
	 * 关闭监控
	 */
	public void stop() {
		if (null != m_Thread) {
			_Logger.info("stop " + m_Thread.getName());
			m_Thread = null;
		}
		MongoCursor<ChangeStreamDocument<Document>> c = m_Cursor;
		if (null != c) {
			try {
				c.close();
				m_Cursor = null;
			} catch (Throwable e) {
				_Logger.warn("忽略关闭异常", e);
			}
		}
	}

	protected synchronized void start() {
		if (null != m_Thread) {
			return;
		}
		m_Thread = new Thread(this, "mongodbwatcher-" + m_Db.getName() + "-" + INC.incrementAndGet());
		m_Thread.setDaemon(true);
		m_Thread.start();
		MongodbWatcherChecker.checkMe(this);
		_Logger.info("start " + m_Thread.getName());
	}

	public void restart() {
		if (null != m_Thread) {
			m_Thread.interrupt();
		}

	}

	public boolean isDead() {
		if (null == m_Thread || null == m_Cursor) {
			return false;// 没启动怎么死。。。
		}
		return System.currentTimeMillis() - m_LastActivity > (m_MaxAwaitTime + (10 * 1000));
	}

	@Override
	public void run() {
		while (null != m_Thread) {
			synchronized (this) {
				try {
					this.wait(3 * 1000);// 3秒后再启动，等系统完全跑起来
				} catch (InterruptedException ee) {
					break;
				}
			}
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
		m_Thread = null;
	}

	private void doLoop() throws InterruptedException, IOException {
		try {
			ChangeStreamIterable<Document> it = m_Db.watch();
			long maxAwaitTime = m_MaxAwaitTime;
			it = it.maxAwaitTime(maxAwaitTime, TimeUnit.MILLISECONDS);
			it = it.fullDocument(FullDocument.UPDATE_LOOKUP);
			m_Cursor = it.iterator();
			if (_Logger.isTraceEnabled()) {
				_Logger.trace("启动监听，" + "MaxAwaitTime:" + maxAwaitTime / 1000d);
			}
			while (null != m_Thread) {
				m_LastActivity = System.currentTimeMillis();
				ChangeStreamDocument<Document> doc;
				try {
					doc = m_Cursor.tryNext();
				} catch (MongoInterruptedException e) {
					_Logger.info("线程中断,重新获取", e);
					continue;
				}
				if (null != doc) {
					if (_Logger.isTraceEnabled()) {
						_Logger.trace(doc.getNamespaceDocument() + "," + doc.getFullDocument().getString("_id"));
					}
					try {
						onChange(doc);
					} catch (Throwable e) {
						_Logger.error("变化通知异常", e);
					}
				} else {
					if (_Logger.isTraceEnabled()) {
						_Logger.trace((maxAwaitTime / 1000d) + "s无变化");
					}
					synchronized (this) {
						this.wait(10);
					}
				}
			}
		} catch (MongoCommandException e) {
			if (e.getMessage().contains("The $changeStream stage is only supported on replica sets")) {
				_Logger.error("非副本集数据库无法支持变化监听,将导致Reload功能失效");
				throw new InterruptedException("无法支持变化监听,正常中止");
			} else {
				throw e;
			}
		} catch (RuntimeException e) {
			throw e;
		} finally {
			if (null != m_Cursor) {
				try {
					m_Cursor.close();
					m_Cursor = null;
				} catch (Throwable e) {
					_Logger.warn("忽略关闭异常", e);
				}
			}
		}

	}

	protected abstract void onChange(ChangeStreamDocument<Document> doc);

}

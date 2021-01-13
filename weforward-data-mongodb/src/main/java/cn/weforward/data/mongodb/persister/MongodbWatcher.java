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
package cn.weforward.data.mongodb.persister;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoCommandException;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;

import cn.weforward.common.Nameable;

/**
 * mongodb监控
 * 
 * @author daibo
 *
 */
public class MongodbWatcher implements Runnable {
	/** 日志 */
	private static final Logger _Logger = LoggerFactory.getLogger(MongodbWatcher.class);
	/** 变化通知 */
	DocumentChange m_Change;
	/** 线程 */
	Thread m_Thread;

	int m_ErrorNum;

	MongoCursor<ChangeStreamDocument<Document>> m_Cursor;

	public MongodbWatcher(DocumentChange change) {
		m_Change = change;
		start();
	}

	private void start() {
		if (null != m_Thread) {
			return;
		}
		m_Thread = new Thread(this, "mongodbwatcher-" + m_Change.getName());
		m_Thread.start();
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
		try {
			ChangeStreamIterable<Document> it = m_Change.getCollection().watch();
			it.fullDocument(FullDocument.UPDATE_LOOKUP);
			m_Cursor = it.iterator();
			while (m_Cursor.hasNext()) {
				try {
					ChangeStreamDocument<Document> doc = m_Cursor.next();
					if (null != m_Change) {
						m_Change.onChange(doc);
					}
				} catch (Throwable e) {
					_Logger.error("变化通知异常", e);
				}
			}
		} catch (MongoCommandException e) {
			if (e.getMessage().contains("The $changeStream stage is only supported on replica sets")) {
				_Logger.error("非副本集数据库无法支持变化监听,将导致[" + m_Change.getName() + "]Reload功能失效");
				throw new InterruptedException("无法支持变化监听,正常中止");
			} else {
				throw e;
			}
		} catch (RuntimeException e) {
			throw e;
		} finally {
			if (null != m_Cursor) {
				m_Cursor.close();
				m_Cursor = null;
			}
		}

	}

	/**
	 * 关闭监控
	 */
	public void close() {
		m_Thread = null;
		if (null != m_Cursor) {
			m_Cursor.close();
		}

	}

	/**
	 * 文档变化
	 * 
	 * @author daibo
	 *
	 */
	public static interface DocumentChange extends Nameable {
		/**
		 * 变化事件
		 * 
		 * @param doc 变化文档
		 */
		void onChange(ChangeStreamDocument<Document> doc);

		/**
		 * 获取集合
		 * 
		 * @return 集合
		 */
		MongoCollection<Document> getCollection();
	}

}

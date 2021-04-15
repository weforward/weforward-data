package cn.weforward.data.mongodb.util;

import java.util.Collections;
import java.util.List;

import cn.weforward.common.util.FreezedList;

/**
 * 检查watcher的情况，防止出现挂死
 * 
 * @author daibo
 *
 */
public class MongodbWatcherChecker implements Runnable {

	private static MongodbWatcherChecker CHECKER = new MongodbWatcherChecker();

	private List<AbstractMongodbChangeSupport> m_Watchers;

	private Thread m_Thread;

	private int m_Interval = 10 * 1000;

	public static void checkMe(AbstractMongodbChangeSupport wather) {
		CHECKER.add(wather);
	}

	private MongodbWatcherChecker() {
		m_Watchers = Collections.emptyList();
	}

	private synchronized void add(AbstractMongodbChangeSupport wathcer) {
		if (m_Watchers.isEmpty()) {
			start();
		}
		List<AbstractMongodbChangeSupport> list = m_Watchers;
		m_Watchers = FreezedList.addToFreezed(list, list.size(), wathcer);
	}

	private synchronized void start() {
		if (null != m_Thread) {
			return;
		}
		m_Thread = new Thread(this, "mongodbwatcherchecker");
		m_Thread.setDaemon(true);
		m_Thread.start();
	}

	@Override
	public void run() {
		while (null != m_Thread) {
			synchronized (this) {
				try {
					this.wait(m_Interval);
				} catch (InterruptedException e) {
					break;
				}
			}
			List<AbstractMongodbChangeSupport> list = m_Watchers;
			for (AbstractMongodbChangeSupport w : list) {
				if (w.isDead()) {
					w.restart();
				}
			}

		}

	}

}

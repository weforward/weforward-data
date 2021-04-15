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
package cn.weforward.data.util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.DestroyableExt;
import cn.weforward.common.crypto.Hex;
import cn.weforward.common.sys.GcCleaner;
import cn.weforward.common.sys.Shutdown;
import cn.weforward.common.sys.StackTracer;
import cn.weforward.common.util.SinglyLinked;
import cn.weforward.data.exception.WrapToDataAccessException;

/**
 * 延时及周期执行刷写动作的刷写器
 * 
 * @author liangyi
 * 
 */
public class DelayFlusher implements Flusher, DestroyableExt {
	/** 日志记录器 */
	public final static Logger _Logger = LoggerFactory.getLogger(Flusher.class);

	/** 刷写线程已关闭 */
	protected static final int STATE_FLUSH_CLOSED = 0x2000;
	/** 重索引线程已关闭 */
	protected static final int STATE_REINDEX_CLOSED = 0x4000;
	/** 刷写器已关闭 */
	protected static final int STATE_CLOSED = STATE_FLUSH_CLOSED | STATE_REINDEX_CLOSED;
	/** 停止刷写器任务 */
	protected static final int STATE_STOP = 0x00100;
	/** 刷写线程启动中 */
	protected static final int STATE_FLUSH_STARTING = 0x00010;
	/** 执行周期刷写 */
	protected static final int STATE_PERIOD = 0x00001;

	/** 正在执行周期刷写 */
	protected static final int BUSY_PERIOD = 0x01000000;
	/** 正在执行延时刷写 */
	protected static final int BUSY_DELAY = 0x02000000;
	/** 正在执行重索引 */
	protected static final int BUSY_REINDEX = 0x04000000;
	/** 正在执行失败重试刷写 */
	protected static final int BUSY_FAIL = 0x08000000;
	/** 标识忙的位 */
	protected static final int BUSY_MASK_STATE = 0xFF000000;
	/** 标识数量的位 */
	protected static final int BUSY_MASK_COUNT = 0x00FFFFFF;

	/** 名称 */
	protected String m_Name;
	/** 延时刷写项链表 */
	final protected SinglyLinked<Flushable> m_DelayList = new SinglyLinked<Flushable>();
	/** 延时刷写出错项链表 */
	final protected SinglyLinked<Flushable> m_FailList = new SinglyLinked<Flushable>();

	/** 锁 */
	final protected ReentrantLock m_Lock = new ReentrantLock();
	/** 等待刷写任务条件 */
	final protected Condition m_WaitWriteTask = m_Lock.newCondition();

	/** 每次后台刷写的最小间隔（毫秒） */
	final protected int m_FlushInterval;
	/** 最多等待刷写的累积项数（0表示不控制） */
	protected int m_MaxSuspend;

	/** 状态 STATE_xxx */
	protected volatile int m_State;
	/** 最后延时刷写时间戳 */
	protected volatile long m_LastDelayFlush;

	/** 刷写的后台工作线程 */
	protected Thread m_ThreadFlush;
	/** 是否正在刷写或处理中（只是作简单标识，操作非原子） */
	protected volatile int m_Busy;
	/** 执行刷写中的表 */
	protected SinglyLinked.SinglyLinkedNode<Flushable> m_FlushPending;

	/** 刷写周期（毫秒） */
	final protected int m_FlushPeriod;
	/** 最后周期刷写时间戳 */
	protected volatile long m_LastPeriodFlush;

	/**
	 * 构造刷写器，5秒延时
	 */
	public DelayFlusher() {
		this(5);
	}

	/**
	 * 构造刷写器
	 * 
	 * @param interval
	 *            后台刷写的最小间隔（秒）
	 */
	public DelayFlusher(int interval) {
		m_FlushInterval = interval * 1000;
		m_FlushPeriod = m_FlushInterval * 10; // 10倍interval
		m_LastPeriodFlush = System.currentTimeMillis();
		m_Name = Hex.toHex(hashCode());
		// 注册在shutdown时关闭
		Shutdown.register(this);
	}

	public void setName(String name) {
		m_Name = name;
	}

	public String getName() {
		return m_Name;
	}

	/**
	 * 最多等待刷写的累积项数
	 * 
	 * @param limit
	 *            要控制的项数，0表示不控制
	 */
	public void setMaxSuspend(int limit) {
		m_MaxSuspend = limit;
	}

	public int getMaxSuspend() {
		return m_MaxSuspend;
	}

	/**
	 * 标记刷写出错项
	 * 
	 * @param flushing
	 *            待刷写项
	 */
	protected void markOnFail(Flushable flushing) {
		m_Lock.lock();
		try {
			m_FailList.addIfAbsent(flushing);
		} finally {
			m_Lock.unlock();
		}
	}

	public void mark(Flushable flushing) {
		m_Lock.lock();
		try {
			if (0 == ((STATE_FLUSH_CLOSED) & m_State)) {
				startFlushingThread();
				// 加到列表中
				if (m_DelayList.addIfAbsent(flushing)) {
					// if (0 == m_LastDelayFlush) {
					if (1 == m_DelayList.size()) {
						// 这次首先进入的项，标记最后刷为当前时间，强迫其至少在N秒后再刷写
						m_LastDelayFlush = System.currentTimeMillis();
						if (_Logger.isDebugEnabled()) {
							_Logger.debug("mark m_LastDelayFlush.");
						}
					}
					// 给等待的条件一个讯号
					m_WaitWriteTask.signal();
					if (_Logger.isDebugEnabled()) {
						_Logger.debug("#" + getName() + " signal DelayFlush(" + m_DelayList.size()
								+ "):" + flushing);
					}
				} else if (_Logger.isDebugEnabled()) {
					_Logger.debug("#" + getName() + " same flushing:" + flushing);
				}
				return;
			}
		} finally {
			m_Lock.unlock();
		}

		// 刷写器已是结束的
		_Logger.warn(StackTracer.printStackTrace(Thread.currentThread(),
				(new StringBuilder("刷写器已关闭，直接刷写项：")).append(flushing)).toString());
		try {
			flushing.flush();
		} catch (IOException e) {
			throw new WrapToDataAccessException("直接刷写失败：" + flushing, e);
		}
		return;
	}

	public void flush(Flushable flushable) {
		// XXX 暂时与mark一样，延时去刷写
		mark(flushable);
	}

	public void flush() {
		signal();
	}

	/**
	 * 给刷写线程信号
	 */
	private boolean signal() {
		boolean ret;
		m_Lock.lock();
		try {
			ret = (0 != (BUSY_MASK_STATE & m_Busy) || !m_DelayList.isEmpty()
					|| !m_FailList.isEmpty());
			// 强置最后刷写时间在周期前
			m_LastPeriodFlush = System.currentTimeMillis() - (1000 + m_FlushPeriod);
			// 强置最后延时刷写在周期前
			m_LastDelayFlush = System.currentTimeMillis() - (1000 + m_FlushInterval);
			// 给等待的条件一个讯号
			m_WaitWriteTask.signal();
		} finally {
			m_Lock.unlock();
		}
		return ret;
	}

	@Override
	public boolean destroySignal() {
		String info = toString();
		boolean ret = signal();
		_Logger.info(info);
		return ret;
	}

	public void destroy() {
		// 不在destroy里停止刷写，因为要确保刷写完成
		// if (0 != (BUSY_MASK_STATE & m_Busy)) {
		if (signal()) {
			try {
				// 它说在忙，等待一会吧
				_Logger.warn(toString());
				Thread.sleep(5 * 1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		_Logger.info(toString());
	}

	/**
	 * 关闭刷写器
	 */
	public void close() {
		if (STATE_CLOSED == (STATE_CLOSED & m_State) || 0 == m_State) {
			// 已是结束的
			return;
		}
		m_Lock.lock();
		try {
			if (STATE_CLOSED == (STATE_CLOSED & m_State)) {
				// 已是结束的
				return;
			}
			m_State |= STATE_STOP;
			m_WaitWriteTask.signal();
		} finally {
			m_Lock.unlock();
		}

		if (_Logger.isTraceEnabled()) {
			_Logger.trace("destroy at wait stop... #" + getName());
		}
		try {
			Thread threadFlush;
			synchronized (this) {
				// 先等1秒给点时间停止
				wait(1 * 1000);
				threadFlush = m_ThreadFlush;
			}
			// 等待工作线程结束
			if (null != threadFlush) {
				threadFlush.join(10 * 60 * 1000);
			}
		} catch (InterruptedException e) {
			_Logger.warn(e.getMessage(), e);
			Thread.currentThread().interrupt();
		}
		_Logger.info("flusher closed{n:" + getName() + ",dl:" + m_DelayList.size() + ",");
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	/**
	 * 执行延时队列的刷写
	 * 
	 * @return 是否刷写中
	 */
	public boolean flushing() {
		if (m_DelayList.isEmpty()) {
			// m_LastDelayFlush = System.currentTimeMillis();
			return false;
		}
		SinglyLinked.SinglyLinkedNode<Flushable> first;
		int size;
		m_Lock.lock();
		try {
			// 使用异步的刷写（刷写过程不影响缓存的操作，把m_First取走就行了）
			size = m_DelayList.size();
			first = m_DelayList.detach();
			if (null == first) {
				// 没有需要刷写的项
				return false;
			}
			m_Busy |= (BUSY_MASK_COUNT & size);
			m_FlushPending = first;
		} finally {
			m_Lock.unlock();
		}
		if (_Logger.isDebugEnabled()) {
			_Logger.debug("delayFlushing... " + size);
		}
		// 历遍（链表中）的项执行刷写
		while (null != first) {
			try {
				// 执行刷写
				first.value.flush();
				if (_Logger.isDebugEnabled()) {
					_Logger.debug("flushing:" + first.value);
				}
			} catch (Throwable e) {
				if (e instanceof OutOfMemoryError) {
					// 内存爆掉:<
					// // 执行下GC
					// GcCleaner.gc();
					// 等5秒再试吧
					GcCleaner.waitFor(5 * 1000);
				} else {
					_Logger.error("flushing fail：" + first.value, e);
				}
				// 把出错项标记回待刷写列表
				markOnFail(first.value);
			}
			first = first.getNext();
			m_LastDelayFlush = System.currentTimeMillis();
		}
		m_LastDelayFlush = System.currentTimeMillis();
		m_FlushPending = null;
		return true;
	}

	/**
	 * 执行刷写中的表
	 * 
	 * @return 链表
	 */
	public SinglyLinked.SinglyLinkedNode<Flushable> getFlushPending() {
		return m_FlushPending;
	}

	/**
	 * 重试刷写错误的项
	 * 
	 * @return true/false
	 */
	public boolean tryFailFlushing() {
		if (m_FailList.isEmpty()) {
			return false;
		}
		SinglyLinked.Node<Flushable> first;
		int size;
		m_Lock.lock();
		try {
			// 使用异步的刷写（刷写过程不影响缓存的操作，把m_First取走就行了）
			size = m_FailList.size();
			first = m_FailList.detach();
			if (null == first) {
				// 没有需要更新的项
				return false;
			}
			// 标记正在忙
			m_Busy |= (BUSY_MASK_COUNT & size);
		} finally {
			m_Lock.unlock();
		}

		_Logger.warn("try flushing... " + size);
		while (null != first) {
			try {
				first.value.flush();
				if (_Logger.isDebugEnabled()) {
					_Logger.debug("try flushing:" + first.value);
				}
			} catch (Throwable e) {
				if (e instanceof OutOfMemoryError) {
					// 内存爆掉:<
					// 执行下GC
					GcCleaner.gc();
					// 等10秒再试吧
					GcCleaner.waitFor(10 * 1000);
				} else {
					_Logger.error("try flushing fail：" + first.value, e);
				}
				// 把出错项标记继续回列表
				markOnFail(first.value);
			}
			first = first.getNext();
		}
		return true;
	}

	/**
	 * 执行刷写任务
	 */
	public void daemonFlushing() {
		int state = 0;
		do {
			state = 0;
			m_Lock.lock();
			try {
				// 等待处理条件
				while (true) {
					if (STATE_STOP == (STATE_STOP & m_State)) {
						// 关闭刷写器
						state |= STATE_PERIOD | STATE_STOP;
						_Logger.info("flushing stop signalled.");
						break;
					}
					int interval = (int) ((m_LastPeriodFlush + m_FlushPeriod)
							- System.currentTimeMillis());
					if (interval <= 0) {
						// 已到刷写周期，跳出等待进行处理
						state |= STATE_PERIOD;
						if (_Logger.isDebugEnabled()) {
							_Logger.debug("periodFlushing condition." + interval);
						}
						break;
					}
					if (m_DelayList.isEmpty()) {
						// // 队列是空的
						// m_WaitEmpty.signal();
					} else if (m_MaxSuspend > 0 && m_DelayList.size() >= m_MaxSuspend) {
						// 到达最大累积项，刷吧
						if (_Logger.isInfoEnabled()) {
							_Logger.info("delayFlushing over list: " + m_MaxSuspend + "/"
									+ m_DelayList.size());
						}
						break;
					} else {
						// 若延时队列不为空计算与上次刷写的间隔
						interval = (int) (System.currentTimeMillis() - m_LastDelayFlush);
						if (interval > m_FlushInterval) {
							// 若刷写间隔合适，跳出等待进行处理
							if (_Logger.isDebugEnabled()) {
								_Logger.debug("delayFlushing condition: " + interval + "ms "
										+ m_DelayList.size());
							}
							break;
						}
						// 等待一小会吧
						interval = (m_FlushInterval + 1000) - interval;
					}
					// 没到要处理的条件，等等（唤醒或到时间）吧
					try {
						if (m_WaitWriteTask.await(interval, TimeUnit.MILLISECONDS)) {
							// 被唤醒了
							if (_Logger.isDebugEnabled()) {
								_Logger.debug(
										"await signalled of (ms)" + interval + "/" + m_FlushPeriod);
							}
						}
					} catch (InterruptedException e) {
						_Logger.error("await of (ms)" + interval + "/" + m_FlushPeriod, e);
						Thread.currentThread().interrupt();
						return;
					}
				}
			} finally {
				m_Lock.unlock();
			}

			// XXX 这里的同步块只为数据可见性，并不需要互斥（希望不会被JIT优化掉吧）
			synchronized (m_DelayList) {
				// 执行延时刷写
				m_Busy |= BUSY_DELAY;
				flushing();

				// 执行周期重试刷写失败的项
				if (STATE_PERIOD == (STATE_PERIOD & state)) {
					m_Busy |= BUSY_FAIL;
					tryFailFlushing();
					m_LastPeriodFlush = System.currentTimeMillis();
				}
				m_Busy &= ~(BUSY_DELAY | BUSY_PERIOD | BUSY_FAIL | BUSY_MASK_COUNT);
			}
		} while (STATE_STOP != (STATE_STOP & state));

		m_Lock.lock();
		try {
			m_State |= STATE_FLUSH_CLOSED;
		} finally {
			m_Lock.unlock();
		}
		_Logger.info("Flusher close... #" + getName() + " dl:" + m_DelayList.size());
		// 刷写器关闭前完成刷写任务
		synchronized (m_DelayList) {
			m_Busy |= BUSY_DELAY;
			while (flushing()) {
			}
			// 重试失败的项
			m_Busy |= BUSY_FAIL;
			tryFailFlushing();
			m_Busy &= ~(BUSY_DELAY | BUSY_PERIOD | BUSY_FAIL | BUSY_MASK_COUNT);
		}
	}

	/**
	 * 启动刷写线程
	 */
	private void startFlushingThread() {
		if (STATE_FLUSH_STARTING == (STATE_FLUSH_STARTING & m_State) || null != m_ThreadFlush) {
			return;
		}
		Thread thread = new Thread("Flusher-" + getName()) {
			@Override
			public void run() {
				_Logger.info("Flusher running...");
				m_ThreadFlush = this;
				m_Lock.lock();
				try {
					m_State &= ~STATE_FLUSH_STARTING;
				} finally {
					m_Lock.unlock();
				}
				try {
					daemonFlushing();
				} finally {
					m_ThreadFlush = null;
				}
			}
		};
		thread.setDaemon(true);
		// 先标识已启动
		m_State |= STATE_FLUSH_STARTING;
		thread.start();
	}

	@Override
	public String toString() {
		return "{n:" + getName() + ",busy:x" + Hex.toHex(m_Busy) + ",queue:" + m_DelayList.size()
				+ ",fail:" + m_FailList.size() + ",thread:" + m_ThreadFlush + ",period:"
				+ m_FlushPeriod + ",interval:" + m_FlushInterval + ",ms:" + m_MaxSuspend + "}";
	}
}

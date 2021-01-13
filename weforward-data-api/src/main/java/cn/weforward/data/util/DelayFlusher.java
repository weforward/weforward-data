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
	/** 刷写线程是否已启动 */
	protected static final int STATE_FLUSH_READY = 0x00010;
	/** 重索引线程是否已启动 */
	protected static final int STATE_REINDEX_READY = 0x00020;
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

	// /** 等待任务列表空 */
	// final protected Condition m_WaitEmpty = m_Lock.newCondition();

	/** 刷写的后台工作线程 */
	protected Thread m_ThreadFlush;
	// /** 重索引的后台工作线程 */
	// protected Thread m_ThreadReindex;
	/** 是否正在刷写或处理中（只是作简单标识，操作不是原子的） */
	protected volatile int m_Busy;
	/** 执行刷写中的表 */
	protected SinglyLinked.SinglyLinkedNode<Flushable> m_FlushPending;

	// /** 周期刷写项链表 */
	// final protected SinglyLinkedLifo<Flushable> m_PeriodList = new
	// SinglyLinkedLifo<Flushable>();
	// /** 延时索引出错项链表 */
	// final protected SinglyLinked<Searchable> m_IndexFailList = new
	// SinglyLinked<Searchable>();
	/** 刷写周期（毫秒） */
	final protected int m_FlushPeriod;
	/** 最后周期刷写时间戳 */
	protected volatile long m_LastPeriodFlush;
	// /** 重索引项链表 */
	// final protected SinglyLinked<Searchable> m_IndexList = new
	// SinglyLinked<Searchable>();
	// /** 等待重索引任务条件 */
	// final protected Condition m_WaitReindexTask = m_Lock.newCondition();

	/**
	 * 构造刷写器，5秒延时
	 */
	public DelayFlusher() {
		this(5);
	}

	/**
	 * 构造刷写器
	 * 
	 * @param interval 后台刷写的最小间隔（秒）
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
	 * @param limit 要控制的项数，0表示不控制
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
	 * @param flushing 待刷写项
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
			// if (0 != ((STATE_CLOSED | STATE_STOP) & m_State)) {
			// // 刷写器已是结束的
			// _Logger.warn(Misc.printStackTrace(Thread.currentThread(),
			// (new
			// StringBuilder("刷写器已关闭，直接刷写项：")).append(flushing)).toString());
			// flushing.flush();
			// return;
			// }
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
						_Logger.debug("#" + getName() + " signal DelayFlush(" + m_DelayList.size() + "):" + flushing);
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
		_Logger.warn(StackTracer
				.printStackTrace(Thread.currentThread(), (new StringBuilder("刷写器已关闭，直接刷写项：")).append(flushing))
				.toString());
		try {
			flushing.flush();
		} catch (IOException e) {
			throw new WrapToDataAccessException("直接刷写失败：" + flushing, e);
		}
		return;
	}

	// @Override
	// public void mark(Searchable searchable) {
	// m_Lock.lock();
	// try {
	// // if (0 != ((STATE_CLOSED | STATE_STOP) & m_State)) {
	// // // 刷写器已是结束的
	// // _Logger.warn(Misc.printStackTrace(Thread.currentThread(),
	// // (new
	// // StringBuilder("刷写器已关闭，直接重索引项：")).append(searchable)).toString());
	// // searchable.reindex();
	// // return;
	// // }
	// if (0 == ((STATE_REINDEX_CLOSED) & m_State)) {
	// startIndexingThread();
	//
	// // 加到列表中
	// if (m_IndexList.addIfAbsent(searchable)) {
	// // 给等待的条件一个讯号
	// m_WaitReindexTask.signal();
	// if (_Logger.isDebugEnabled()) {
	// _Logger.debug("signal daemon indexing.");
	// }
	// }
	// return;
	// }
	// } finally {
	// m_Lock.unlock();
	// }
	//
	// // 刷写器已是结束的
	// _Logger.warn(
	// StackTracer
	// .printStackTrace(Thread.currentThread(),
	// (new StringBuilder("刷写器已关闭，直接重索引项：")).append(searchable))
	// .toString());
	// searchable.reindex();
	// return;
	// }

	// /**
	// * 标记索引出错项
	// *
	// * @param searchable
	// * 待索引项
	// */
	// protected void markOnFail(Searchable searchable) {
	// m_Lock.lock();
	// try {
	// m_IndexFailList.addIfAbsent(searchable);
	// } finally {
	// m_Lock.unlock();
	// }
	// }

	public void flush(Flushable flushable) {
		// XXX 暂时与mark一样，延时去刷写
		mark(flushable);
	}

	// public void join(Flushable flushable) {
	// m_Lock.lock();
	// try {
	// startFlushingThread();
	// m_PeriodList.addIfAbsent(flushable);
	// } finally {
	// m_Lock.unlock();
	// }
	// }

	// public void unjoin(Flushable flushable) {
	// m_Lock.lock();
	// try {
	// m_PeriodList.remove(flushable);
	// } finally {
	// m_Lock.unlock();
	// }
	// }

	public void flush() {
		m_Lock.lock();
		try {
			// // 强置最后刷写时间在周期前
			// m_LastPeriodFlush = System.currentTimeMillis() - (1000 +
			// m_FlushPeriod);
			// 强置最后延时刷写在周期前
			m_LastDelayFlush = System.currentTimeMillis() - (1000 + m_FlushInterval);
			// 给等待的条件一个讯号
			m_WaitWriteTask.signal();
			// m_WaitReindexTask.signal();
		} finally {
			m_Lock.unlock();
		}
	}

	@Override
	public boolean destroySignal() {
		boolean ret;
		String info;
		// 通知马上执行刷写
		m_Lock.lock();
		try {
			info = toString();
			ret = (0 != (BUSY_MASK_STATE & m_Busy) || !m_DelayList.isEmpty() || !m_FailList.isEmpty());
			// || !m_IndexList.isEmpty()) || !m_IndexFailList.isEmpty());
			// 强置最后刷写时间在周期前
			m_LastPeriodFlush = System.currentTimeMillis() - (1000 + m_FlushPeriod);
			// 强置最后延时刷写在周期前
			m_LastDelayFlush = System.currentTimeMillis() - (1000 + m_FlushInterval);
			// 给等待的条件一个讯号
			m_WaitWriteTask.signal();
			// m_WaitReindexTask.signal();
		} finally {
			m_Lock.unlock();
		}
		_Logger.info(info);
		return ret;
	}

	public void destroy() {
		// XXX 在destroy停止刷写绝对是错误的做法，应该要确保刷写完成
		m_Lock.lock();
		try {
			// 强置最后刷写时间在周期前
			m_LastPeriodFlush = System.currentTimeMillis() - (1000 + m_FlushPeriod);
			m_LastDelayFlush = System.currentTimeMillis() - (1000 + m_FlushInterval);
			// 给等待的一个讯号
			m_WaitWriteTask.signal();
			// m_WaitReindexTask.signal();
		} finally {
			m_Lock.unlock();
		}
		if (0 != (BUSY_MASK_STATE & m_Busy)) {
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
		// m_PeriodTask.cancel();
		m_Lock.lock();
		try {
			if (STATE_CLOSED == (STATE_CLOSED & m_State)) {
				// 已是结束的
				return;
			}
			m_State |= STATE_STOP;
			m_WaitWriteTask.signal();
			// m_WaitReindexTask.signal();
		} finally {
			m_Lock.unlock();
		}

		if (_Logger.isTraceEnabled()) {
			_Logger.trace("destroy at wait stop... #" + getName());
		}
		try {
			Thread threadFlush;
			// Thread threadIndex;
			synchronized (this) {
				// 先等1秒给点时间停止
				wait(1 * 1000);
				threadFlush = m_ThreadFlush;
				// threadIndex = m_ThreadReindex;
			}
			// 等待工作线程结束
			if (null != threadFlush) {
				threadFlush.join(10 * 60 * 1000);
				// thread.join(0);
			}
			// if (null != threadIndex) {
			// threadIndex.join(10 * 60 * 1000);
			// // thread.join(0);
			// }
		} catch (InterruptedException e) {
			_Logger.warn(e.getMessage(), e);
			Thread.currentThread().interrupt();
		}
		_Logger.info("flusher closed{n:" + getName() + ",dl:" + m_DelayList.size() + ",");
		// _Logger.info("flusher closed. #" + getName() + " dl:" +
		// m_DelayList.size() + " il:"
		// + m_IndexList.size());
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	// /**
	// * 执行周期列表的刷写
	// */
	// public boolean period() {
	// // 使用异步的刷写（刷写过程不影响链表的操作，不需要加锁，取得链表首就行了）
	// SinglyLinked.SinglyLinkedNode<Flushable> first;
	// first = m_PeriodList.getHead();
	// int size = m_PeriodList.size();
	// if (null == first) {
	// // 没有需要刷写的项
	// m_LastPeriodFlush = System.currentTimeMillis();
	// return false;
	// }
	// m_Busy |= (BUSY_MASK_COUNT & size);
	// if (_Logger.isTraceEnabled()) {
	// _Logger.trace("periodFlushing... " + size);
	// }
	// // 历遍（链表中）的项执行刷写
	// while (null != first) {
	// // m_Busy |= (BUSY_PERIOD | (BUSY_MASK_COUNT & size));
	// try {
	// // 执行刷写
	// first.value.flush();
	// if (_Logger.isDebugEnabled()) {
	// _Logger.debug("periodFlushing:" + first.value);
	// }
	// } catch (Throwable e) {
	// // 出错了:(
	// // StringBuilder sb = new StringBuilder();
	// // sb.append("periodFlush failed：").append(first);
	// // _Logger.error(Misc.printStackTrace(e, sb).toString());
	// _Logger.error("periodFlush failed：" + first.value, e);
	// }
	// first = first.getNext();
	// // --size;
	// }
	// // m_Busy &= ~(BUSY_PERIOD | BUSY_MASK_COUNT);
	// m_LastPeriodFlush = System.currentTimeMillis();
	// return true;
	// }

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

	// /**
	// * 重试索引错误的项
	// */
	// public boolean tryFailIndexing() {
	// if (m_IndexFailList.isEmpty()) {
	// return false;
	// }
	// SinglyLinked.Node<Searchable> first;
	// int size;
	// m_Lock.lock();
	// try {
	// // 使用异步的刷写（刷写过程不影响缓存的操作，把m_First取走就行了）
	// size = m_IndexFailList.size();
	// first = m_IndexFailList.detach();
	// if (null == first) {
	// // 没有需要更新的项
	// return false;
	// }
	// // 标记正在忙
	// m_Busy |= (BUSY_MASK_COUNT & size);
	// } finally {
	// m_Lock.unlock();
	// }
	//
	// _Logger.warn("try indexing... " + size);
	// while (null != first) {
	// try {
	// first.value.reindex();
	// if (_Logger.isDebugEnabled()) {
	// _Logger.debug("try indexing:" + first.value);
	// }
	// } catch (Throwable e) {
	// if (e instanceof OutOfMemoryError) {
	// // 内存爆掉:<
	// // 执行下GC
	// GcCleaner.gc();
	// // 等10秒再试吧
	// GcCleaner.waitFor(10 * 1000);
	// } else {
	// _Logger.error("try indexing fail：" + first.value, e);
	// }
	// // 把出错项标记继续回列表
	// markOnFail(first.value);
	// }
	// first = first.getNext();
	// }
	// return true;
	// }

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
						// return;
					}
					int interval = (int) ((m_LastPeriodFlush + m_FlushPeriod) - System.currentTimeMillis());
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
							_Logger.info("delayFlushing over list: " + m_MaxSuspend + "/" + m_DelayList.size());
						}
						break;
					} else {
						// 若延时队列不为空计算与上次刷写的间隔
						interval = (int) (System.currentTimeMillis() - m_LastDelayFlush);
						if (interval > m_FlushInterval) {
							// 若刷写间隔合适，跳出等待进行处理
							if (_Logger.isDebugEnabled()) {
								_Logger.debug("delayFlushing condition: " + interval + "ms " + m_DelayList.size());
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
								_Logger.debug("await signalled of (ms)" + interval + "/" + m_FlushPeriod);
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

				// 执行周期刷写
				if (STATE_PERIOD == (STATE_PERIOD & state)) {
					// m_Busy |= BUSY_PERIOD;
					// period();

					// 重试失败的项
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
			// // 记录周期刷写的时间点
			// m_Busy |= BUSY_PERIOD;
			// period();
			m_Busy |= BUSY_DELAY;
			while (flushing()) {
			}
			// 重试失败的项
			m_Busy |= BUSY_FAIL;
			tryFailFlushing();
			m_Busy &= ~(BUSY_DELAY | BUSY_PERIOD | BUSY_FAIL | BUSY_MASK_COUNT);
		}
	}

	// /**
	// * 执行重索引任务
	// */
	// public void daemonIndexing() {
	// int state;
	// do {
	// state = 0;
	// m_Lock.lock();
	// try {
	// // 等待处理条件
	// while (true) {
	// if (STATE_STOP == (STATE_STOP & m_State)) {
	// // 关闭刷写器
	// state |= STATE_PERIOD | STATE_STOP;
	// _Logger.info("indexing stop signalled.");
	// break;
	// }
	// if (!m_IndexList.isEmpty()) {
	// break;
	// }
	// // 没有任务，等唤醒吧
	// try {
	// if (!m_WaitReindexTask.await(m_FlushPeriod, TimeUnit.MILLISECONDS)
	// && !m_IndexFailList.isEmpty()) {
	// // 等待刷写周期时间超时若索引失败列表不为空，处理失败列表
	// state |= STATE_PERIOD;
	// break;
	// }
	// } catch (InterruptedException e) {
	// _Logger.error(StackTracer.printStackTrace(e, null).toString());
	// Thread.currentThread().interrupt();
	// return;
	// }
	// }
	// } finally {
	// m_Lock.unlock();
	// }
	//
	// m_Busy |= BUSY_REINDEX;
	// indexing();
	// if (STATE_PERIOD == (STATE_PERIOD & state)) {
	// tryFailIndexing();
	// }
	// m_Busy &= ~(BUSY_REINDEX | BUSY_MASK_COUNT);
	// } while (STATE_STOP != (STATE_STOP & state));
	//
	// m_Lock.lock();
	// try {
	// m_State |= STATE_REINDEX_CLOSED;
	// } finally {
	// m_Lock.unlock();
	// }
	// _Logger.info("Indexing close... #" + getName() + " il:" +
	// m_IndexList.size());
	// // 关闭前完成索引任务
	// m_Busy |= BUSY_REINDEX;
	// while (indexing()) {
	// }
	// // 重试失败的项
	// tryFailIndexing();
	// m_Busy &= ~(BUSY_REINDEX | BUSY_MASK_COUNT);
	// }

	// /**
	// * 历遍（链表中）的项执行重索引
	// *
	// * @param head
	// * 链表首项
	// */
	// public boolean indexing() {
	// int size;
	// SinglyLinked.Node<Searchable> head;
	// m_Lock.lock();
	// try {
	// size = m_IndexList.size();
	// head = m_IndexList.detach();
	// if (null == head) {
	// // // 没有需要索引的项
	// return false;
	// }
	// m_Busy |= (BUSY_MASK_COUNT & size);
	// if (_Logger.isDebugEnabled()) {
	// _Logger.debug("indexing... " + size);
	// }
	// } finally {
	// m_Lock.unlock();
	// }
	// for (; null != head; head = head.getNext()) {
	// try {
	// head.value.reindex();
	// if (_Logger.isDebugEnabled()) {
	// _Logger.debug("indexing:" + head.value);
	// }
	// } catch (Throwable e) {
	// // 出错了:(
	// // StringBuilder sb = new StringBuilder();
	// // sb.append("reindex失败：").append(head);
	// // _Logger.error(Misc.printStackTrace(e, sb).toString());
	// _Logger.error("indexing失败：" + head.hashCode(), e);
	// // 标记到出错项待索引表
	// markOnFail(head.value);
	// }
	// }
	// return true;
	// }

	/**
	 * 启动刷写线程
	 */
	private void startFlushingThread() {
		if (STATE_FLUSH_READY == (STATE_FLUSH_READY & m_State)) {
			return;
		}
		Thread thread = new Thread("Flusher-" + getName()) {
			@Override
			public void run() {
				_Logger.info("Flusher running...");
				m_ThreadFlush = this;
				daemonFlushing();
				m_ThreadFlush = null;
			}
		};
		thread.setDaemon(true);
		thread.start();
		// 只要线程start就加上标识
		m_State |= STATE_FLUSH_READY;
	}

	// /**
	// * 启动重索引线程
	// */
	// private void startIndexingThread() {
	// if (STATE_REINDEX_READY == (STATE_REINDEX_READY & m_State)) {
	// return;
	// }
	// // 启动重索引线程
	// Thread thread = new Thread("Indexing-" + getName()) {
	// @Override
	// public void run() {
	// _Logger.info("indexing running...");
	// m_ThreadReindex = this;
	// daemonIndexing();
	// m_ThreadReindex = null;
	// }
	// };
	// thread.setDaemon(true);
	// thread.start();
	// m_State |= STATE_REINDEX_READY;
	// }

	@Override
	public String toString() {
		return "{n:" + getName() + ",busy:x" + Hex.toHex(m_Busy) + ",queue:" + m_DelayList.size() + ",fail:"
				+ m_FailList.size() + ",thread:" + m_ThreadFlush + ",period:" + m_FlushPeriod + ",interval:"
				+ m_FlushInterval + ",ms:" + m_MaxSuspend + "}";
	}

}

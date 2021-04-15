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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.LoggerFactory;

import cn.weforward.common.sys.StackTracer;
import cn.weforward.common.util.TimeUtil;

/**
 * 池管理器，使用一个m_Frees数组存储空闲的连接，一个m_Usings存储使用中的连接
 * <p>
 * 当由池提取时，将由m_Frees提取（或其为空时且未超出最大项数的条件下新创建一个，并置入m_Usings）
 * <p>
 * 当项使用完毕回收时，将其置入m_Frees，并由m_Usings中删除
 * <p>
 * 池会启用一个后台线程，对m_Usings进行扫描，删除由于各种异常原因导致没能正常回收的项，且日志输出提取项的调用堆栈
 * 
 * @version V1.0
 * @author liangyi
 */
public abstract class Pool<E extends Object> {
	/**
	 * 用于检查连接的定时器
	 */
	protected final static Timer _Timer = new Timer("Pool-Timer", true);

	/**
	 * 日志记录器
	 */
	public final static org.slf4j.Logger _Logger = LoggerFactory.getLogger(Pool.class);
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

	private int m_MaxSize; // 所允许的最大size
	private int m_PeakSize; // 峰值size
	private List<PoolElement> m_Frees; // 存储空闲的项
	private Map<E, PoolElement> m_Usings; // 存储使用中的项
	private Map<E, PoolElement> m_Freeze; // 暂时冻结超时的连接
	private long m_LiveCheckPoint; // 空闲连接是否生存检查点时间(毫秒)
	private volatile long m_Overtime; // 由连接池分配出开始计算,连接使用的过期时间(毫秒)
	private LiveCheckTask m_Task;

	protected Object m_Lock = new Object(); // 用于加锁的对象

	/**
	 * 创建新的项置入池
	 * 
	 * @return 新建项
	 * @throws Exception 异常
	 */
	protected abstract E onPoolNewElement() throws Exception;

	/**
	 * 在池中去除项时调用清除
	 * 
	 * @param element 被清除的项
	 */
	protected abstract void onPoolDeleteElement(E element);

	/**
	 * 处理超时的项(参数overtime指示超时毫秒),
	 * 
	 * @param element  超时项
	 * @param overtime 超时值（毫秒）
	 * @return 返回true将在使用列表中清除,false保留在列表中
	 */
	protected abstract boolean onPoolOvertimeElement(E element, long overtime);

	/**
	 * 处理检查点的项(参数idle指示空闲时间)
	 * 
	 * @param element 检查项
	 * @param idle    空闲时间（毫秒
	 * @return 返回false将在使用列表中清除，ture则保留在列表中
	 */
	protected abstract boolean onPoolCheckElement(E element, long idle);

	/**
	 * 构造
	 */
	public Pool() {
	}

	/**
	 * 构造且创建池
	 * 
	 * @param maxSize        池最大项数
	 * @param overtime       使用超时时间（毫秒）
	 * @param liveCheckPoint 检查时间间隔（毫秒）
	 */
	public Pool(int maxSize, int overtime, int liveCheckPoint) {
		create(maxSize, overtime, liveCheckPoint);
	}

	/**
	 * 指定最大项数创建池，使用超时时间为1分钟，每5分钟检查一次
	 * 
	 * @param maxSize 池最大项数
	 */
	public void create(int maxSize) {
		create(maxSize, 1 * 60 * 1000, 5 * 60 * 1000);
	}

	/**
	 * 创建池
	 * 
	 * @param maxSize        池最大项数
	 * @param overtime       使用超时时间（毫秒）
	 * @param liveCheckPoint 检查时间间隔（毫秒）
	 */
	public void create(int maxSize, long overtime, int liveCheckPoint) {
		/*
		 * synchronized (this) { if (m_Overtime > 0) close(); }
		 */
		close();

		// 检查间隔不能小于10秒
		if (liveCheckPoint < 10 * 1000)
			m_LiveCheckPoint = 10 * 1000;
		else
			m_LiveCheckPoint = liveCheckPoint;

		m_MaxSize = maxSize;
		m_Frees = new ArrayList<PoolElement>(m_MaxSize);
		m_Usings = new HashMap<E, PoolElement>(m_MaxSize);
		m_Freeze = new HashMap<E, PoolElement>(m_MaxSize);
		m_Overtime = overtime;

		// 加入定时器
		m_Task = new LiveCheckTask();
		_Timer.schedule(m_Task, m_LiveCheckPoint, m_LiveCheckPoint);
		// _Logger.info("#" + this.hashCode() + " Pool.start...");
	}

	// 返回可用项数
	public int getFreeCount() {
		return m_Frees.size();
	}

	// 返回已经分配项数
	public int getUsingCount() {
		return m_Usings.size();
	}

	// 取得使用中的项
	public PoolElement getUsing(E item) {
		return m_Usings.get(item);
	}

	// 由池中分配
	public E allocate(long timeout) throws Exception {
		// 从数据库连接池获取有效连接
		PoolElement item = null;
		synchronized (m_Lock) {
			while (null == item) {
				// 如果空闲连接池中存在空闲连接
				if (m_Frees.size() > 0) {
					// 获取一个空闲连接
					item = m_Frees.remove(m_Frees.size() - 1);
				} else {
					// 如果当前的连接数小于最大连接数，或者配置的最大连接数为0，则创建新的连接
					if (m_MaxSize == 0 || m_Usings.size() < m_MaxSize) {
						E element = onPoolNewElement();
						if (_TraceEnabled) {
							_Logger.trace("#" + this.hashCode() + " new: " + m_Usings.size() + " peak size: "
									+ m_PeakSize + '/' + m_MaxSize);
						}
						if (null == element) {
							_Logger.warn("#" + this.hashCode() + " OnPoolNewElement() return is null!");
							return null;
						}
						item = new PoolElement(element);
					}
				}
				if (null == item) {
					// 已经超时，退出
					if (-1 == timeout) {
						_Logger.warn("#" + this.hashCode() + " WARNING timeout waiting for free element!");
						return null;
					}

					// 没有可用的空连接，等待timeout值
					try {
						m_Lock.wait(timeout);
					} catch (Exception e) {
						timeout = -1;
					}
					continue;
				}
			}
			// /m_Usings.put(element, new PoolElement(element, m_DebugMode));
			item.active();
			m_Usings.put(item.getElement(), item);
			if (m_Usings.size() > m_PeakSize) {
				m_PeakSize = m_Usings.size();
				if (_TraceEnabled) {
					_Logger.trace("#" + this.hashCode() + " peak size: " + m_PeakSize);
				}
			}
		}
		return item.getElement();
	}

	// 使用完归还池(checkLive指示返回之前检查连接是否可用)
	public boolean free(E element, boolean checkLive) {
		if (null == element) {
			_Logger.warn("ERROR free null element!");
			return false;
		}
		synchronized (m_Lock) {
			// 由使用列表中删除
			PoolElement item = m_Usings.remove(element);

			// 如果在使用列表中没找到,不承认这个连接
			if (null == item) {
				_Logger.warn("#" + this.hashCode() + " ERROR element no in m_Usings! " + element.hashCode());

				// 还要看看是否在被冻结的列表中（是的话，给它解冻啦）,解冻后后台线程会把它放回空闲池
				item = m_Freeze.remove(element);
				if (null == item) {
					// 这是丢失了的连接,执行OnPoolDeleteElement
					_Logger.warn("#" + this.hashCode() + " Element is lost! " + element.hashCode());
					onPoolDeleteElement(element);
				}
				return false;
			}
			if (checkLive && !onPoolCheckElement(element, ((new Date()).getTime() - item.getLastActive()))) {
				// 检查返回连接不活,删除
				onPoolDeleteElement(element);
				_Logger.warn("Free and OnPoolCheckElement is failed, remove element!");
				return false;
			}

			// 释放一个空闲的项回池
			// /item.m_LastActive=(new Date()).getTime();
			item.free();
			m_Frees.add(item);

			// Trace("Free element: " + element.hashCode() + ",
			// usings:"+m_Usings.size()+", free="+m_Frees.size());

			// 通知等待的线程可以提取连接
			m_Lock.notifyAll();
			return true;
		}
	}

	// 关闭池
	public void close() {
		synchronized (m_Lock) {
			if (null != m_Task) {
				m_Task.cancel();
			}
			// m_Overtime = 0;
		}
		clear();
	}

	// 清除池中的数据
	protected void clear() {
		if (null == m_Frees) {
			return;
		}
		synchronized (m_Lock) {
			// 删除所有空闲连接
			for (int i = m_Frees.size() - 1; i >= 0; i--) {
				try {
					PoolElement element = m_Frees.remove(i);
					if (null != element)
						onPoolDeleteElement(element.getElement());
				} catch (Exception e) {
					_Logger.error(StackTracer.printStackTrace(e, null).toString());
				}
			}
			// 删除所有占用中的连接
			Iterator<PoolElement> it = m_Usings.values().iterator();
			while (it.hasNext()) {
				PoolElement element = it.next();
				try {
					onPoolDeleteElement(element.getElement());
				} catch (Exception e) {
					_Logger.error("onPoolDeleteElement --->", e);
				}
			}
			m_Usings.clear();

			// 删除所有冻结的连接
			it = m_Freeze.values().iterator();
			while (it.hasNext()) {
				PoolElement element = it.next();
				try {
					onPoolDeleteElement(element.getElement());
				} catch (Exception e) {
					_Logger.error("onPoolDeleteElement --->", e);
				}
			}
			m_Freeze.clear();
		}
		_Logger.info("#" + this.hashCode() + " Pool.close");
	}

	// 池中的条目
	protected class PoolElement {
		E m_Element; // 条目
		long m_LastActive; // 最后活动时间
		// String m_StackTrace; // 创建时的调用堆栈
		Thread m_Owner; // 创建线程
		int m_State; // 状态

		final public static int STATE_NEW = 0; // 新建状态
		final public static int STATE_FREE = 1; // 空闲状态
		final public static int STATE_USING = 2; // 使用状态
		final public static int STATE_OVERTIME = 3; // 超时状态
		final public static int STATE_CLOSE = -1; // 关闭状态

		private PoolElement(E element) {
			m_Element = element;
			m_LastActive = (new Date()).getTime();
			// /m_CreateTime=m_LastActive;
			m_State = STATE_NEW;
		}

		// 刷新项为使用状态
		public void active() {
			m_LastActive = (new Date()).getTime();
			m_State = STATE_USING;
			m_Owner = Thread.currentThread();
			/*
			 * if (debugMode) { // 如果是调试模式,保存现时调用堆栈情况 Throwable throwable = new Throwable();
			 * StackTraceElement[] trace = throwable.getStackTrace(); m_StackTrace = new
			 * String(); for (int i = 2; i < trace.length; i++) { m_StackTrace +=
			 * trace[i].toString(); m_StackTrace += "\n"; } }
			 */
		}

		// 标志项为空闲状态
		public void free() {
			active();
			m_State = STATE_FREE;
		}

		// 取后更新状态时间
		public long getLastActive() {
			return m_LastActive;
		}

		public E getElement() {
			return m_Element;
		}

		/*
		 * // 取得进入使用状态时的调用堆栈（通常用于跟踪丢失的项） public String getStackTrace(boolean isClear) {
		 * if (isClear) { String s = m_StackTrace; m_StackTrace = null; return s; }
		 * return m_StackTrace; }
		 */
		public Thread getOwner() {
			return m_Owner;
		}
	}

	// 检查并断开超时不活动的连接
	class LiveCheckTask extends TimerTask {
		// 超时项
		ArrayList<PoolElement> overtimes = new ArrayList<PoolElement>(m_MaxSize);

		public void run() {
			// _Logger.info("#" + this.hashCode() + " Pool.run...");
			int i;
			long ti = System.currentTimeMillis();

			// 检查m_Usings超时不释放的项，置入overtimes
			synchronized (m_Lock) {
				Iterator<PoolElement> it;
				it = m_Usings.values().iterator();
				while (it.hasNext()) {
					PoolElement element = it.next();
					if (ti > (m_Overtime + element.getLastActive())) {
						// 超时了
						it.remove();
						m_Usings.remove(element.getElement());
						// 标志为STATE_OVERTIME状态，放入冻结列表
						element.m_State = PoolElement.STATE_OVERTIME;
						m_Freeze.put(element.getElement(), element);
						overtimes.add(element);

						StringBuilder sb = new StringBuilder(512);
						sb.append("#").append(this.hashCode()).append(" element '")
								.append(element.getElement().hashCode()).append("' overtime(")
								.append((ti - element.getLastActive()) / 1000).append("s) begin at ")
								.append(TimeUtil.formatDateTime(new Date(element.getLastActive())));
						Thread owner = element.getOwner();
						if (null != owner) {
							sb.append("\ttrace stack--->\n");
							StackTracer.printStackTrace(owner, sb);
						}
						_Logger.warn(sb.toString());
						// if (m_DebugMode) {
						// // 显示由池分配此项的调用堆栈
						// _Logger.warn(element.getStackTrace(true));
						// }
					}
				}
			}

			// 对超时的项overtimes进行处理，onOvertimeElement返回true的话置丢弃
			boolean isRemove;
			for (i = overtimes.size() - 1; i >= 0; i--) {
				PoolElement element = overtimes.get(i);
				isRemove = false;
				try {
					// 调用onOvertimeElement处理
					isRemove = onPoolOvertimeElement(element.getElement(), ti - element.getLastActive());
				} catch (Exception e) {
					_Logger.error(StackTracer.printStackTrace(e, null).toString());
					continue;
				}
				synchronized (m_Lock) {
					if (isRemove) {
						// 标志为STATE_CLOSE状态
						element.m_State = PoolElement.STATE_CLOSE;
						if (null != m_Freeze.remove(element.getElement())) {
							_Logger.warn("#" + this.hashCode() + " The element is remove! "
									+ element.getElement().hashCode());
						} else {
							// 如果onOvertimeElement返回false，得把连接放回m_Usings（如果它还在冻结列表的话）
							if (null != m_Freeze.remove(element.getElement())) {
								m_Usings.put(element.getElement(), element);
								_Logger.warn("#" + this.hashCode()
										+ " The element retrieve m_Usings, be OnPoolOvertimeElement is false! "
										+ element.getElement().hashCode());
							} else {
								// 不在的话，放回m_Frees
								m_Frees.add(element);
								// 通知等待的线程可以提取连接
								m_Lock.notifyAll();
								_Logger.warn("#" + this.hashCode()
										+ " The element retrieve m_Frees, be OnPoolOvertimeElement is false! "
										+ element.getElement().hashCode());
							}
						}
					}
				}
			}
			overtimes.clear();
			synchronized (m_Lock) {
				if (m_Freeze.size() > 0) {
					_Logger.warn("#" + this.hashCode() + " m_Freeze size: " + m_Freeze.size());
					m_Freeze.clear();
				}
			}

			// 检查空闲连接(调用OnPoolCheckElement)是否还能用
			synchronized (m_Lock) {
				ti = System.currentTimeMillis();
				for (i = m_Frees.size() - 1; i >= 0; i--) {
					PoolElement element = m_Frees.get(i);
					long idle = ti - element.getLastActive();
					if (idle > m_LiveCheckPoint) {
						if (_TraceEnabled) {
							_Logger.trace(
									"#" + this.hashCode() + " Element live check: " + element.getElement().hashCode()
											+ " " + TimeUtil.formatDateTime(new Date(element.getLastActive())) + "("
											+ (idle / 1000) + "s)");
						}
						// 生存检测
						if (onPoolCheckElement(element.getElement(), idle)) {
							// 返回true表示活着的,保留在列表
							element.active();
						} else {
							// 返回false表示死掉了,删掉它
							m_Frees.remove(i);
							_Logger.warn("#" + this.hashCode() + " OnPoolCheckElement report the element is death: "
									+ element.getElement().hashCode());
							onPoolDeleteElement(element.getElement());
						}
					}
				}
			}
			// _Logger.info("#" + this.hashCode() + " Pool.run end");
		}
	}
}

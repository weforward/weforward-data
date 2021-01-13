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
package cn.weforward.data.persister.remote;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.execption.AbortException;
import cn.weforward.common.sys.ClockTick;
import cn.weforward.common.sys.Shutdown;
import cn.weforward.common.sys.StackTracer;
import cn.weforward.common.util.DelayRunner;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.UniteId;
import cn.weforward.data.exception.DataAccessException;
import cn.weforward.data.exception.WrapToDataAccessException;
import cn.weforward.data.persister.BusinessDi;
import cn.weforward.data.persister.ObjectWithVersion;
import cn.weforward.data.persister.support.AbstractPersistent;

/**
 * 远程 持久业务对象基类
 * 
 * @author daibo
 *
 */
public abstract class AbstractRemotePersistent<E extends BusinessDi, V> extends AbstractPersistent<E> {
	/** 日志记录器 */
	protected final static Logger _Logger = LoggerFactory.getLogger(AbstractRemotePersistent.class);
	/** 缓存刷新策略- 标记为强过期（必须重新加载VO） */
	protected static final int POLICY_FORCE_EXPIRED = 0x00;
	/** 缓存刷新策略- 异步 */
	protected static final int POLICY_ASYNC = 0x01;
	/** 缓存刷新策略- 等待 */
	protected static final int POLICY_WAITTING = 0x02;
	/** 缓存刷新策略- 标记为弱过期（若加载失败重用旧VO） */
	protected static final int POLICY_WEAK_EXPIRED = 0x08;

	/** 默认缓存刷新策略 */
	protected static final int POLICY_DEFAULT = POLICY_ASYNC;

	/** 等待超时值- 默认（死等） */
	protected final static int TIMEOUT_DEFAULT = 0;
	/** 用时间戳标记VO缓存已过期 */
	protected final static int TIMESTAMP_EXPIRED = Integer.MIN_VALUE + 1;
	/** 用时间戳标记VO缓存已弱过期 */
	protected final static int TIMESTAMP_WEAK_EXPIRED = Integer.MIN_VALUE + 2;

	/** 时钟（0.1秒的周期） */
	private final static ClockTick _Tick = ClockTick.getInstance(0.1);

	/** 值vo */
	protected V m_Vo;
	/** 版本 */
	protected String m_Version;
	/** VO最后刷新的时间戳（毫秒） */
	protected volatile long m_Timestamp;
	/** 延迟加载器 */
	protected DelayLoader m_DelayLoader;

	/**
	 * 以依赖接口构造对象
	 * 
	 * @param di 依赖接口
	 */
	protected AbstractRemotePersistent(E di) {
		super(di, true);
	}

	/**
	 * 以依赖接口构造对象，且指定是否持久
	 * 
	 * @param di         依赖接口
	 * @param persistent 是否持久
	 */
	protected AbstractRemotePersistent(E di, boolean persistent) {
		super(di, persistent);
	}

	/**
	 * 取得VO（可能会阻塞及触发异步刷新处理）
	 * 
	 * @return VO
	 */
	protected V getVo() {
		V v = null;
		if (TIMESTAMP_EXPIRED != m_Timestamp) {
			// VO没有标记强过期
			v = m_Vo;
			if (TIMESTAMP_WEAK_EXPIRED != m_Timestamp && null != v) {
				// 若有VO且没有标记弱过期，由checkExpiry决定是否reload
				if (!checkExpiry()) {
					return v;
				}
			}
		}
		// 若显式标记过期或VO为null，重新加载VO
		try {
			reload(0);
		} catch (Throwable e) {
			if (null == v) {
				// 只有VO为null的情况下直接抛出加载异常（也就是会略过VO过期时重加载的异常）
				throw new WrapToDataAccessException(e);
			}
			// VO还有值的情况下更新时间戳避免雪崩加载重试
			m_Timestamp = _Tick.getMills();
			StringBuilder sb = new StringBuilder(512);
			sb.append("[").append(m_Id).append("]刷新失败继续使用旧VO\t");
			StackTracer.printStackTrace(e, sb);
			_Logger.warn(sb.toString());
		}
		v = m_Vo;
		if (null == v) {
			// reload后还是null，有问题
			throw new DataAccessException("reload后VO依然为[null] " + m_Id);
		}
		return v;
	}

	/**
	 * 只是简单直接返回VO
	 * 
	 * @return VO
	 */
	protected V getVoFast() {
		return m_Vo;
	}

	/**
	 * 检查VO缓存是否过期，且根据di的配置值触发软刷新（异步刷新）
	 * 
	 * @return 若返回true表马上过期迫使getVo阻塞调用reload加载VO，否则返回false（若软过期则put入异步队列刷新VO）
	 */
	protected boolean checkExpiry() {
		if (isPersistenceUpdating()) {
			// 脏数据，直接返回未过期
			return false;
		}
		int expiry = getVoExpiry();
		if (expiry == 0) {
			// 0为永不过期
			return false;
		}
		boolean block = false;
		if (expiry < 0) {
			// expiry小于0，过期阻塞加载
			block = true;
			expiry = -expiry;
		}
		long ts = m_Timestamp;
		long tk = _Tick.getMills();
		// 计算过期值
		if (tk <= (ts + expiry)) {
			// 未过期，直接返回就好了
			return false;
		}
		// VO已过期
		if (block) {
			// 阻塞加载，返回true
			return true;
		}
		// 异步刷新
		try {
			refreshPersistent(POLICY_ASYNC);
			if (_Logger.isDebugEnabled()) {
				_Logger.debug("缓存过期[" + tk + "-" + ts + "=" + (tk - ts) + "]：" + m_Id + "/" + m_Version);
			}
		} catch (Exception e) {
			_Logger.error("异步更新失败：" + m_Id, e);
		}
		return false;
	}

	/**
	 * 获取加载器
	 * 
	 * @return 延迟加载器
	 */
	@SuppressWarnings("unchecked")
	protected DelayLoader getDelayLoader() {
		DelayLoader loader = m_DelayLoader;
		if (null != loader) {
			return loader;
		}
		synchronized (this) {
			loader = m_DelayLoader;
			if (null != loader) {
				return loader;
			}
			AbstractRemotePersister<?, V> p = (AbstractRemotePersister<?, V>) getPersister();
			if (p.isShareDelayLoader()) {
				loader = SHARE;
			} else {
				// 创建独立的加载器
				loader = new IsolateDelayLoader();
			}
			m_DelayLoader = loader;
		}
		return loader;
	}

	/**
	 * 刷新持久
	 * 
	 * @param policy 刷新策略 POLICY_XXX
	 * @throws InterruptedException 中断异常
	 * @throws IOException          IO异常
	 */
	public void refreshPersistent(int policy) throws InterruptedException, IOException {
		if (isPersistenceUpdating()) {
			_Logger.warn("更改未刷写 " + this);
			return;
		}
		if (POLICY_FORCE_EXPIRED == policy) {
			// 标记缓存过期
			expirePersistent();
			return;
		}
		if (POLICY_WEAK_EXPIRED == policy) {
			// 弱过期
			weakExpirePersistent();
			return;
		}
		long tk = _Tick.getMills();
		// 马上更新时间戳，避免getVo又触发刷新
		m_Timestamp = tk;
		reload(policy);
	}

	/* 获取vo过期时间 */
	private int getVoExpiry() {
		@SuppressWarnings("unchecked")
		AbstractRemotePersister<?, V> p = (AbstractRemotePersister<?, V>) getPersister();
		return p.getExpiry();
	}

	/*
	 * 重新加载对象
	 * 
	 * @param policy 刷新策略 POLICY_XXX
	 * 
	 */
	private void reload(int policy) {
		if (policy == POLICY_ASYNC) {
			getDelayLoader().delayLoad(this);
		} else {
			doReload();
		}
	}

	/* 执行重新加载对象 */
	private void doReload() {
		@SuppressWarnings("unchecked")
		AbstractRemotePersister<?, V> p = (AbstractRemotePersister<?, V>) getPersister();
		ObjectWithVersion<V> vo = p.remoteLoad(getPersistenceId().getOrdinal(), m_Version);
		if (null == vo) {
			// 杯具？对象在远程已被干掉
			UniteId uid = getPersistenceId();
			p.remove(uid);// 自己删除自己..
			throw new DataAccessException("对象已被删除" + String.valueOf(uid));
		} else {
			updateVo(vo.getObject(), vo.getVersion());
		}
	}

	/**
	 * 标记为过期
	 */
	synchronized public void expirePersistent() {
		if (!isPersistenceUpdating()) {
			// 标记缓存过期
			m_Timestamp = TIMESTAMP_EXPIRED;
		} else {
			_Logger.warn("由于更改未刷写，不能过期VO " + this);
		}
	}

	/**
	 * 标记为弱过期
	 */
	synchronized public void weakExpirePersistent() {
		if (!isPersistenceUpdating()) {
			// 标记缓存过期
			m_Timestamp = TIMESTAMP_WEAK_EXPIRED;
		} else {
			_Logger.warn("由于更改未刷写，不能弱过期VO " + this);
		}
	}

	/**
	 * 更新vo
	 * 
	 * @param vo      VO
	 * @param version 版本
	 */
	public void updateVo(V vo, String version) {
		if (!StringUtil.isEmpty(m_Version) && StringUtil.eq(m_Version, version)) {
			return;// 同版本
		}
		m_Vo = vo;
		@SuppressWarnings("unchecked")
		AbstractRemotePersister<?, V> p = (AbstractRemotePersister<?, V>) getPersister();
		p.updateOffline(getPersistenceId().getId(), m_Vo);

	}

	/**
	 * 获取vo
	 * 
	 * @return VO
	 */
	public V acceptVo() {
		return getVoFast();
	}

	/**
	 * 脱机对象的延迟加载器
	 * 
	 * @author daibo
	 * 
	 */
	public interface DelayLoader {
		/**
		 * 延迟加载对象
		 * 
		 * @param proxy 代理对象
		 */
		public void delayLoad(AbstractRemotePersistent<?, ?> proxy);
	}

	/** 共享加载器 */
	static final DelayLoader SHARE = new ShareDelayLoader();

	/**
	 * 共用的加载器
	 * 
	 * @author daibo
	 * 
	 */
	static public class ShareDelayLoader implements DelayLoader {
		final DelayRunner<AbstractRemotePersistent<?, ?>> m_DelayRunner;

		public ShareDelayLoader() {
			m_DelayRunner = new DelayRunner<AbstractRemotePersistent<?, ?>>("delayloader", 1) {
				@Override
				protected void execute(AbstractRemotePersistent<?, ?> task) throws Exception {
					if (isStop()) {
						throw new AbortException("及时响应停止");
					}
					task.doReload();
				}
			};
			Shutdown.register(m_DelayRunner);
			m_DelayRunner.start();
		}

		@Override
		public void delayLoad(AbstractRemotePersistent<?, ?> task) {
			m_DelayRunner.submit(task, true);
		}

		@Override
		public String toString() {
			return m_DelayRunner.toString();
		}
	}

	/**
	 * 独立的加载器
	 * 
	 * @author daibo
	 * 
	 */
	protected class IsolateDelayLoader implements DelayLoader {
		final DelayRunner<AbstractRemotePersistent<?, ?>> m_DelayRunner;

		IsolateDelayLoader() {
			m_DelayRunner = new DelayRunner<AbstractRemotePersistent<?, ?>>(AbstractRemotePersistent.this.toString(),
					1) {
				@Override
				protected void execute(AbstractRemotePersistent<?, ?> task) throws Exception {
					if (isStop()) {
						throw new AbortException("及时响应停止");
					}
					task.doReload();
				}
			};
			Shutdown.register(m_DelayRunner);
			m_DelayRunner.start();
		}

		@Override
		public void delayLoad(AbstractRemotePersistent<?, ?> proxy) {
			m_DelayRunner.submit(proxy, true);
		}

		@Override
		public String toString() {
			return m_DelayRunner.toString();
		}
	}
}

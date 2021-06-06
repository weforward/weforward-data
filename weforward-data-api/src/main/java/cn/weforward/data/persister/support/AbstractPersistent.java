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
package cn.weforward.data.persister.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.sys.IdGenerator;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.UniteId;
import cn.weforward.data.persister.BusinessDi;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.PersistentListener;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.search.Searchable;

/**
 * 持久业务对象基类
 * 
 * @author liangyi
 * 
 * @param <E> 业务依赖接口
 */
public abstract class AbstractPersistent<E extends BusinessDi>
		implements Persistent, PersistentListener, cn.weforward.common.DistributedObject {
	/** 日志记录器 */
	private final static Logger _Logger = LoggerFactory.getLogger(AbstractPersistent.class);

	/** 清除所有标记 */
	public static final int PERSISTENCE_CLEAR = 0;
	/** 标记对象是新创建的 */
	public static final int PERSISTENCE_NEW = 0x10000000;
	/** 标记对象处于不持久的新建状态 */
	public static final int PERSISTENCE_TRANSIENT = 0x20000000;
	/** 标记对象处于删除状态 */
	public static final int PERSISTENCE_DELETE = 0x40000000;
	/** 标记对象需要重索引 */
	public static final int PERSISTENCE_REINDEX = 0x00010000;

	/** 对象依赖接口 */
	private final E m_BusinessDi;
	/** 对象持久标记（只影响0x7FFF0000标志位，0xFFFF位保留作业务类用途） */
	private volatile int m_PersistenceMark;
	/** 对象联合ID */
	protected UniteId m_Id;

	/** 控制的实例 */
	protected String m_DriveIt;

	/**
	 * 以业务依赖接口构造对象
	 * 
	 * @param di 业务依赖接口
	 */
	protected AbstractPersistent(E di) {
		this(di, true);
	}

	/**
	 * 以依赖接口构造对象，且指定是否持久
	 * 
	 * @param di         依赖接口
	 * @param persistent 是否持久
	 */
	protected AbstractPersistent(E di, boolean persistent) {
		m_BusinessDi = di;
		m_PersistenceMark = persistent ? PERSISTENCE_NEW : (PERSISTENCE_NEW | PERSISTENCE_TRANSIENT);
	}

	/**
	 * 以依赖接口及ID构造对象
	 * 
	 * @param di 业务依赖接口
	 * @param id 指定对象的ID，若为null则忽略，若为空字串（不是null，是长度为0）则自动生成
	 */
	protected AbstractPersistent(E di, String id) {
		this(di, id, true);
	}

	/**
	 * 以依赖接口及ID构造对象，且指定是否持久
	 * 
	 * @param di         业务依赖接口
	 * @param id         指定对象的ID，若为null则忽略，若为空字串（不是null，是长度为0）则自动生成
	 * @param persistent 是否持久
	 */
	protected AbstractPersistent(E di, String id, boolean persistent) {
		m_BusinessDi = di;
		if (null != id) {
			if (id.length() == 0) {
				// 自动产生对象ID
				m_Id = getPersister().getNewId();
				_Logger.trace("自动生成ID:" + m_Id);
			} else {
				m_Id = UniteId.valueOf(id).changeType(getClass());
				if (null != m_BusinessDi && m_Id.getOrdinal().length() == 0) {
					// 传入的是无序号ID，只好生成新的
					_Logger.warn("忽略构造传入的无效ID:" + id);
					m_Id = getPersister().getNewId();
				} else {
					_Logger.trace("使用指定ID:" + id);
				}
			}
		}
		m_PersistenceMark = persistent ? PERSISTENCE_NEW : (PERSISTENCE_NEW | PERSISTENCE_TRANSIENT);
	}

	@Override
	public UniteId getPersistenceId() {
		return m_Id;
	}

	// @Override
	public String getPersistenceVersion() {
		return getPersister().getVersion(getPersistenceId());
	}

	/**
	 * 业务依赖接口
	 * 
	 * @return 依赖接口
	 */
	protected E getBusinessDi() {
		return m_BusinessDi;
	}

	protected void enablDelete() {
		setPersistenceMark(PERSISTENCE_DELETE);
	}

	/**
	 * 把对象非持久状态转变为持久（且标记对象需要刷写）
	 */
	protected void enablePersistent() {
		if (PERSISTENCE_TRANSIENT == (PERSISTENCE_TRANSIENT & m_PersistenceMark)) {
			m_PersistenceMark &= (~PERSISTENCE_TRANSIENT);
			markPersistenceUpdate();
		}
	}

	/**
	 * 对象是否持久的（没有PERSISTENCE_TRANSIENT标记）
	 * 
	 * @return true/false
	 */
	protected boolean isPersistent() {
		return (PERSISTENCE_TRANSIENT != (PERSISTENCE_TRANSIENT & m_PersistenceMark));
	}

	/**
	 * 标记对象变化需要刷写
	 */
	protected void markPersistenceUpdate() {
		if (isPersistenceMark(PERSISTENCE_DELETE)) {
			throw new IllegalStateException("已删除的持久对象");
		}
		if (null == m_BusinessDi) {
			// 没指定依赖接口，忽略
			return;
		}
		Persister<Persistent> persister = getPersister();
		if (isPersistent()) {
			// 若对象持久，更新
			persister.update(this);
		} else if (persister instanceof AbstractPersister<?>) {
			// 若对象不持久，且持久器带有缓存，则进入缓存
			((AbstractPersister<Persistent>) persister).putOfCache(this);
		}
	}

	/**
	 * 标记对象变化需要持久，与markPersistenceUpdate方法的差别在于此方法立刻写数据
	 */
	protected void persistenceUpdateNow() {
		if (isPersistenceMark(PERSISTENCE_DELETE)) {
			throw new IllegalStateException("已删除的持久对象");
		}
		if (null == m_BusinessDi) {
			// 没指定依赖接口，忽略
			return;
		}
		Persister<Persistent> persister = getPersister();
		if (isPersistent()) {
			// 若对象持久，更新
			persister.persist(this);
		} else if (persister instanceof AbstractPersister<?>) {
			// 若对象不持久，且持久器带有缓存，则进入缓存
			((AbstractPersister<Persistent>) persister).putOfCache(this);
		}
	}

	/**
	 * 标记对象变化需要刷写且指定持久标记
	 * 
	 * @param marks 标记
	 */
	protected void markPersistenceUpdate(int marks) {
		setPersistenceMark(marks);
		markPersistenceUpdate();
	}

	/**
	 * 对象是否标记为更新但未持久化完成
	 * 
	 * @return true/false
	 */
	protected boolean isPersistenceUpdating() {
		return getPersister().isDirty(this);
	}

	/**
	 * 标示/去除持久标记
	 * 
	 * @param marks 状态PERSISTENCE_*，-PERSISTENCE_*表示去除，PERSISTENCE_NORMAL表示清除全部
	 */
	protected void setPersistenceMark(int marks) {
		if (marks > 0) {
			m_PersistenceMark |= marks;
		} else if (marks < 0) {
			m_PersistenceMark &= (~(-marks));
		} else {
			m_PersistenceMark = 0;
		}
	}

	/**
	 * 是否有指定持久标记位
	 * 
	 * @param marks 标记位 PERSISTENCE_*
	 * @return 有则返回true
	 */
	public boolean isPersistenceMark(int marks) {
		return marks == (marks & m_PersistenceMark);
	}

	/**
	 * 构造一个带持久器标识的id
	 * 
	 * @param id 唯一id
	 * @return 联合id
	 */
	protected UniteId withPersistenceId(String id) {
		Persister<?> ps = getPersister();
		String pid = ps.getPersisterId();
		if (StringUtil.isEmpty(pid)) {
			return UniteId.valueOf(id, ps.getName(), null);
		} else {
			return UniteId.valueOf(id + "-" + pid, ps.getName(), null);
		}
	}

	/**
	 * 生成新的ID
	 */
	protected void genPersistenceId() {
		if (!UniteId.isEmpty(m_Id)) {
			throw new UnsupportedOperationException("不能重复初始化ID：" + m_Id);
		}
		m_Id = getPersister().getNewId();
	}

	/**
	 * 生成新的ID
	 * 
	 * @param prefix 前缀
	 */
	protected void genPersistenceId(String prefix) {
		if (!UniteId.isEmpty(m_Id)) {
			throw new UnsupportedOperationException("不能重复初始化ID：" + m_Id);
		}
		m_Id = getPersister().getNewId(prefix);
	}

	/**
	 * 是否属于当前服务器产生的持久对象
	 * 
	 * @return 属于返回true
	 */
	public boolean isPersistenceOfOwner() {
		return getPersister().isOwner(this);
	}

	@Override
	public String tryDriveIt() {
		String old = m_DriveIt;
		String driveIt = getPersister().getPersisterId();
		if (StringUtil.eq(old, driveIt)) {
			return old;
		}
		m_DriveIt = driveIt;
		persistenceUpdateNow();
		onDriveIt(old);
		return m_DriveIt;
	}

	@Override
	public boolean iDo() {
		if (getPersister().isForOwnerEnabled()) {
			if (StringUtil.isEmpty(m_DriveIt)) {
				return isPersistenceOfOwner();
			} else {
				return StringUtil.eq(m_DriveIt, getPersister().getPersisterId());
			}
		} else {
			return true;
		}
	}

	@Override
	public String getDriveIt() {
		if (StringUtil.isEmpty(m_DriveIt)) {
			return IdGenerator.getServerId(getPersistenceId().getOrdinal());
		} else {
			return m_DriveIt;
		}
	}

	/**
	 * 取当前对象的持久器
	 * 
	 * @param <T> 对象类型
	 * @return 对应的持久器
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Persistent> Persister<T> getPersister() {
		if (null == m_BusinessDi) {
			return null;
		}
		return (Persister<T>) m_BusinessDi.getPersister(getClass());
	}

	/**
	 * 当控制实例时
	 * 
	 * @param oldDriveIt 旧的控制实例对象
	 */
	protected void onDriveIt(String oldDriveIt) {

	}

	@Override
	public void onAfterPersistence(Persister<? extends Persistent> persister, String version) {
		if (isPersistenceMark(PERSISTENCE_REINDEX) && this instanceof Searchable) {
			// 若对象标记了需要重索引且有实现Searchable接口，执行reindex
			Searchable searchable = ((Searchable) this);
			// 直接重索引
			try {
				searchable.reindex();
			} catch (Throwable e) {
				// catch重索引错误，不要影响刷写过程
				_Logger.error(e.getMessage(), e);
			}
		}
		// 持久后清除标记
		setPersistenceMark(PERSISTENCE_CLEAR);
	}

	@Override
	public void onAfterReflect(Persister<? extends Persistent> persister, UniteId id, String version, String driveIt) {
		// 反射后去除不持久的标记
		setPersistenceMark(-PERSISTENCE_TRANSIENT);
		// m_PersistenceMark &= (~PERSISTENCE_TRANSIENT);
		if (UniteId.isEmpty(m_Id)) {
			// 恢复ID
			m_Id = id;
		}
		if (StringUtil.isEmpty(m_DriveIt)) {
			m_DriveIt = driveIt;
		}
	}

	@Override
	public void onBeforePersistence(Persister<? extends Persistent> persister) {
		if (UniteId.isEmpty(m_Id)) {
			// 持久前必须生成对象ID
			genPersistenceId();
		}
	}

}

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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import cn.weforward.common.ResultPage;
import cn.weforward.data.UniteId;
import cn.weforward.data.exception.IdDuplicateException;
import cn.weforward.data.persister.BusinessDi;
import cn.weforward.data.persister.ChangeListener;
import cn.weforward.data.persister.Condition;
import cn.weforward.data.persister.OrderBy;
import cn.weforward.data.persister.Persistent;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.persister.PersisterFactory;
import cn.weforward.data.persister.PersisterSet;
import cn.weforward.protocol.ext.ObjectMapper;

/**
 * 多个持久工厂集合,一般用来作数据过滤,同时保存多份数据
 * 
 * @author daibo
 *
 */
public class MultiPersisterFactory implements PersisterFactory {
	/** 持久器集 */
	protected final PersisterSet m_PersisterSet;
	/** 工厂集合 */
	protected List<PersisterFactory> m_Factorys;
	/** 主工厂 */
	protected int m_Main;

	public MultiPersisterFactory(List<PersisterFactory> factorys, int main) {
		this(factorys, main, new SimplePersisterSet());
	}

	public MultiPersisterFactory(List<PersisterFactory> factorys, int main, PersisterSet ps) {
		m_PersisterSet = ps;
		m_Factorys = factorys;
		m_Main = main;
	}

	@Override
	public <E extends Persistent> Persister<E> createPersister(Class<E> clazz, ObjectMapper<E> mapper) {
		List<Persister<E>> list = new ArrayList<>();
		for (PersisterFactory ps : m_Factorys) {
			Persister<E> p = ps.createPersister(clazz, mapper);
			list.add(p);
		}
		MultiPersistent<E> ps = new MultiPersistent<E>(list);
		m_PersisterSet.regsiter(ps);
		return ps;
	}

	@Override
	public <E extends Persistent> Persister<E> createPersister(Class<E> clazz, BusinessDi di) {
		List<Persister<E>> list = new ArrayList<>();
		for (PersisterFactory ps : m_Factorys) {
			Persister<E> p = ps.createPersister(clazz, di);
			list.add(p);
		}
		MultiPersistent<E> ps = new MultiPersistent<E>(list);
		m_PersisterSet.regsiter(ps);
		return ps;
	}

	@Override
	public <E extends Persistent> Persister<E> getPersister(Class<E> clazz) {
		return getPersisters().getPersister(clazz);
	}

	@Override
	public <E extends Persistent> E get(String id) {
		return getPersisters().get(id);
	}

	@Override
	public PersisterSet getPersisters() {
		return m_PersisterSet;
	}

	public class MultiPersistent<E extends Persistent> implements Persister<E> {
		List<Persister<E>> m_List;

		MultiPersistent(List<Persister<E>> list) {
			m_List = list;
		}

		private Persister<E> getMain() {
			return m_List.get(m_Main);
		}

		@Override
		public String getName() {
			return getMain().getName();
		}

		@Override
		public UniteId getNewId() throws IdDuplicateException {
			return getMain().getNewId();
		}

		@Override
		public UniteId getNewId(String prefix) throws IdDuplicateException {
			return getMain().getNewId(prefix);
		}

		@Override
		public E get(UniteId id) {
			return getMain().get(id);
		}

		@Override
		public E get(String id) {
			return getMain().get(id);
		}

		@Override
		public boolean remove(UniteId id) {
			for (Persister<E> p : m_List) {
				if (!p.remove(id)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean remove(String id) {
			for (Persister<E> p : m_List) {
				if (!p.remove(id)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public void update(E object) {
			for (Persister<E> p : m_List) {
				p.update(object);
			}
		}

		@Override
		public boolean isDirty(E object) {
			return getMain().isDirty(object);
		}

		@Override
		public void flush(E object) {
			for (Persister<E> p : m_List) {
				p.flush(object);
			}
		}

		@Override
		public void persist(E object) {
			for (Persister<E> p : m_List) {
				p.persist(object);
			}
		}

		@Override
		public void cleanup() {
			for (Persister<E> p : m_List) {
				p.cleanup();
			}
		}

		@Override
		public ResultPage<E> search(Date begin, Date end) {
			return getMain().search(begin, end);
		}

		@Override
		public ResultPage<E> startsWith(String prefix) {
			return getMain().startsWith(prefix);
		}

		@Override
		public String getVersion(UniteId id) {
			return getMain().getVersion(id);
		}

		@Override
		public ResultPage<E> searchRange(String from, String to) {
			return getMain().searchRange(from, to);
		}

		@Override
		public String getPersisterId() {
			return getMain().getPersisterId();
		}

		@Override
		public boolean isOwner(E obj) {
			return getMain().isOwner(obj);
		}

		@Override
		public boolean isReloadEnabled() {
			return getMain().isReloadEnabled();
		}

		@Override
		public boolean setReloadEnabled(boolean enabled) {
			for (Persister<E> p : m_List) {
				if (!p.setReloadEnabled(enabled)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public ResultPage<String> searchOfId(Date begin, Date end) {
			return getMain().searchOfId(begin, end);
		}

		@Override
		public ResultPage<String> searchRangeOfId(String from, String to) {
			return getMain().searchRangeOfId(from, to);
		}

		@Override
		public boolean isForOwnerEnabled() {
			return getMain().isForOwnerEnabled();
		}

		@Override
		public boolean setForOwnerEnabled(boolean enabled) {
			return getMain().setForOwnerEnabled(enabled);
		}

		@Override
		public ResultPage<String> startsWithOfId(String prefix) {
			return getMain().startsWithOfId(prefix);
		}

		@Override
		public Iterator<E> search(String serverId, Date begin, Date end) {
			return getMain().search(serverId, begin, end);
		}

		@Override
		public Iterator<String> searchOfId(String serverId, Date begin, Date end) {
			return getMain().searchOfId(serverId, begin, end);
		}

		@Override
		public Iterator<E> searchRange(String serverId, String from, String to) {
			return getMain().searchRange(serverId, from, to);
		}

		@Override
		public Iterator<String> searchRangeOfId(String serverId, String from, String to) {
			return getMain().searchRangeOfId(serverId, from, to);
		}

		@Override
		public ResultPage<E> search(Condition condition) {
			return getMain().search(condition);
		}

		@Override
		public ResultPage<String> searchOfId(Condition condition) {
			return getMain().searchOfId(condition);
		}

		@Override
		public ResultPage<E> search(Condition condition, OrderBy orderBy) {
			return getMain().search(condition, orderBy);
		}

		@Override
		public ResultPage<String> searchOfId(Condition condition, OrderBy orderBy) {
			return getMain().searchOfId(condition, orderBy);
		}

		@Override
		public void addListener(ChangeListener<E> l) {
			for (Persister<E> p : m_List) {
				p.addListener(l);
			}

		}

		@Override
		public void removeListener(ChangeListener<E> l) {
			for (Persister<E> p : m_List) {
				p.removeListener(l);
			}
		}

	}

}

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
import java.util.List;

/**
 * 以线程存储为基础管理多个同线程的事务管理器，同时提供事务组合功能
 * 
 * @author liangyi
 * 
 */
public class TransactionManager implements TransactionThread, TransactionGroup {
	// 线程存储
	ThreadLocal<Element> m_Elements = new ThreadLocal<Element>();

	public Transaction get(Object provider) {
		Element element = m_Elements.get();
		if (null == element) {
			return null;
		}
		return element.get(provider);
	}

	public void set(Transaction trans) {
		Element element = m_Elements.get();
		if (null == element) {
			element = new Element();
			m_Elements.set(element);
		}
		trans.setGroup(this);
		element.put(trans);
	}

	/**
	 * 用于清理事务，用于检查是否有忘记关闭的事务
	 */
	public void cleanup() {
		Element element = m_Elements.get();
		if (null != element && !element.isCompleted()) {
			// 有事务未关闭，先取得事务信息
			String detail = element.getDetail();
			// 然后回滚所有子事务
			element.rollback();
			m_Elements.remove();
			// 抛出错误
			throw new TransactionException(detail);
		}
		m_Elements.remove();
	}

	public void onBegin(Transaction trans) {
		Element element = m_Elements.get();
		if (null == element) {
			throw new TransactionException("此事务组已关闭！");
		}
		element.begin();
		// 可能是新事务，要把事务置入管理中
		element.put(trans);
	}

	public void onCommit(Transaction trans) {
		Element element = m_Elements.get();
		if (null == element || element.isCompleted()) {
			throw new TransactionException("此事务组已提交或回滚！");
		}
		element.commit();
	}

	public void onRollback(Transaction trans) {
		Element element = m_Elements.get();
		if (null == element || element.isCompleted()) {
			throw new TransactionException("此事务组已提交或回滚！");
		}
		element.rollback();
	}

	/**
	 * 当前线程下的事务组项
	 * 
	 * @author liangyi
	 * 
	 */
	static class Element {
		// 事务计数器（在begin时+1，在commit时-1，且为0时执行真正的commit）
		protected int m_Counter;
		// 当前事务组下的事务项
		protected List<Transaction> m_Transactions = new ArrayList<Transaction>(4);

		/**
		 * 由提供商取得事务
		 * 
		 * @param provider
		 *            事务提供者
		 * @return 相关的事务
		 */
		public Transaction get(Object provider) {
			for (int i = m_Transactions.size() - 1; i >= 0; i--) {
				Transaction t = m_Transactions.get(i);
				if (t.getProvider() == provider) {
					return t;
				}
			}
			return null;
		}

		/**
		 * 置入事务
		 * 
		 * @param trans
		 *            要置入的事务
		 */
		public void put(Transaction trans) {
			Object provider = trans.getProvider();
			for (int i = m_Transactions.size() - 1; i >= 0; i--) {
				Transaction t = m_Transactions.get(i);
				if (t == trans) {
					// 是同一实例
					return;
				}
				if (t.getProvider() == provider) {
					// 替换掉相同提供商的实例
					m_Transactions.set(i, trans);
					return;
				}
			}
			// 新增
			m_Transactions.add(trans);
		}

		/**
		 * 开始事务
		 */
		public void begin() {
			++m_Counter;
		}

		/**
		 * 提交事务
		 */
		public void commit() {
			if (m_Counter <= 0) {
				throw new TransactionException("此事务已提交或回滚！");
			}
			--m_Counter;
			if (0 == m_Counter) {
				// 提交所有子事务（TODO 这里暂时不考虑某个子事务提交失败的情况）
				for (int i = m_Transactions.size() - 1; i >= 0; i--) {
					Transaction t = m_Transactions.get(i);
					t.getDeliver().doCommit();
				}
				m_Transactions.clear();
			}
		}

		/**
		 * 回滚
		 */
		public void rollback() {
			if (m_Counter <= 0) {
				throw new TransactionException("此事务已提交或回滚！");
			}
			m_Counter = 0;

			// 回滚所有事务
			for (int i = m_Transactions.size() - 1; i >= 0; i--) {
				Transaction t = m_Transactions.get(i);
				t.getDeliver().doRollback();
			}
			m_Transactions.clear();
		}

		public boolean isCompleted() {
			return (0 == m_Counter);
		}

		/**
		 * 取事务详细信息
		 * 
		 * @return 详细信息
		 */
		public String getDetail() {
			StringBuilder detail = new StringBuilder();
			for (int i = m_Transactions.size() - 1; i >= 0; i--) {
				Transaction t = m_Transactions.get(i);
				detail.append(t.toString());
				if (t.isCompleted()) {
					detail.append(" completed.");
				} else {
					detail.append(" pending ---> \n").append(t.getDeliver().getDetail()).append(
							'\n');
				}
			}
			return detail.toString();
		}
	}

}

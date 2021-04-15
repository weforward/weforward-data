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

/**
 * 以计数简单方式实现的事务基础类
 * 
 * @author liangyi
 * 
 */
abstract class SimpleTransaction implements Transaction {
	protected int m_Counter; // 事务计数器（在begin时+1，在commit时-1，且为0时执行真正的commit）
	protected TransactionGroup m_Group; // 事务组

	public void begin() {
		// 开始事务计数器加一
		++m_Counter;

		// 通知事务组
		if (null != m_Group) {
			m_Group.onBegin(this);
		}
	}

	public void commit() {
		if (m_Counter <= 0) {
			throw new TransactionException("此事务已提交或回滚！");
		}
		// 提交事务计数器减一
		--m_Counter;
		if (null != m_Group) {
			// 通知事务组
			m_Group.onCommit(this);
		} else if (0 == m_Counter) {
			// 当计数为0时执行
			getDeliver().doCommit();
		}
	}

	public void rollback() {
		if (m_Counter <= 0) {
			throw new TransactionException("此事务已提交或回滚！");
		}
		// 回滚操作直接计数置0
		m_Counter = 0;
		if (null != m_Group) {
			// 通知事务组
			m_Group.onRollback(this);
		} else {
			getDeliver().doRollback();
		}
	}

	public boolean isCompleted() {
		return (0 == m_Counter);
	}

	public void setGroup(TransactionGroup group) {
		m_Group = group;
	}
}

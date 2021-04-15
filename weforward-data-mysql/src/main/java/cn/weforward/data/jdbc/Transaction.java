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
 * 事务接口
 * 
 * 以begin/commit成对出现为基本使用基本，同样rollback可以在commit前，begin后任何地方出现
 * 
 * @author liangyi
 * 
 */
public interface Transaction {

	/**
	 * 开始事务
	 */
	void begin();

	/**
	 * 提交事务
	 */
	void commit();

	/**
	 * 回滚事务
	 */
	public void rollback();

	/**
	 * 事务是否完成
	 * 
	 * @return 完成则返回true
	 */
	boolean isCompleted();

	/**
	 * 通知事务在业务过程中发生异常
	 * 
	 * @param e
	 *            所发生的异常
	 */
	void notifyException(Exception e);

	/**
	 * 设置组合事务
	 * 
	 * @param group
	 *            事务组
	 */
	void setGroup(TransactionGroup group);

	/**
	 * 取事务实现者
	 * 
	 * @return 事务器
	 */
	TransactionDeliver getDeliver();

	/**
	 * 取事务提供者
	 * 
	 * @return 事务提供者
	 */
	Object getProvider();
}

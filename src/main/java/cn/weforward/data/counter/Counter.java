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
package cn.weforward.data.counter;

import org.slf4j.LoggerFactory;

import cn.weforward.common.Nameable;
import cn.weforward.common.ResultPage;

/**
 * 计数器，系统可能会分布部署，能运行在三种方式下：
 * <p>
 * 1.独立运行（单机）
 * <p>
 * 2.两个对等节点
 * <p>
 * 3.一个主节点与多个（32个以内）从节点
 * <p>
 * 注：分布模式下的计数器只会最终一致，所以不能作为依赖一致性逻辑的实现
 * 
 * @author liangyi
 * 
 */
public interface Counter extends Nameable {
	/**
	 * 日志记录器
	 */
	org.slf4j.Logger _Logger = LoggerFactory.getLogger(Counter.class);
	/**
	 * 是否允许trace，通常写法
	 * 
	 * <pre>
	 * if(Counter._TraceEnabled){
	 * 	Counter._Logger.trace(...);
	 * }
	 * </pre>
	 */
	boolean _TraceEnabled = _Logger.isTraceEnabled();
	boolean _DebugEnabled = _Logger.isDebugEnabled();
	boolean _InfoEnabled = _Logger.isInfoEnabled();
	boolean _WarnEnabled = _Logger.isWarnEnabled();

	/**
	 * 计数器名（标识）
	 * 
	 * @return 计数器名（标识）
	 */
	String getName();

	/**
	 * 取得计数项的值
	 * 
	 * @param id 计数项
	 * @return 计数值
	 */
	long get(String id);

	/**
	 * 计数加1
	 * 
	 * @param id 计数项
	 * @return 加一后的计数值
	 */
	long inc(String id);

	/**
	 * 指定ID的计数值加step
	 * 
	 * @param id   计数项
	 * @param step 增加的值（负数为减小）
	 * @return 增加或减小step后的计数值
	 */
	long inc(String id, int step);

	/**
	 * 计数减1
	 * 
	 * @param id 计数项
	 * @return 减一后的计数值
	 */
	long dec(String id);

	/**
	 * 设置计数项的值
	 * 
	 * @param id    计数项
	 * @param value 计数值
	 * @return 设置前的计数值
	 */
	long set(String id, long value);

	/**
	 * 比较并改变计数器的值
	 * 
	 * @param id     计数器
	 * @param expect 改变时预期的值
	 * @param value  改变到的值
	 * @return 成功则返回true，否则意味着值不是expect
	 */
	boolean compareAndSet(String id, long expect, long value);

	/**
	 * 删除计数项
	 * 
	 * @param id 计数项
	 * @return true/false
	 */
	boolean remove(String id);

	/**
	 * 删除所有计数器项
	 */
	void removeAll();

	/**
	 * 查询以指定前缀开始的项
	 * 
	 * @param prefix 计数项ID前缀
	 * @return 计数项页结果
	 */
	ResultPage<String> startsWith(String prefix);

	/**
	 * 获取ID区间（ID&gt;=first且ID&lt;=last）内的计数项名
	 * 
	 * @param first 计数项名区间的开始，可以为null
	 * @param last  计数项名区间的结束，可以为null
	 * @return 计数项页结果
	 */
	ResultPage<String> searchRange(String first, String last);
}

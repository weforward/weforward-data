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
package cn.weforward.data.persister;

import java.util.Date;
import java.util.Iterator;

import cn.weforward.common.Nameable;
import cn.weforward.common.ResultPage;
import cn.weforward.data.UniteId;
import cn.weforward.data.exception.IdDuplicateException;
import cn.weforward.data.persister.ext.ConditionUtil;
import cn.weforward.data.persister.ext.OrderByUtil;

/**
 * 对象持久器接口
 * 
 * @author daibo
 * 
 * @param <E> 持久对象
 */
public interface Persister<E extends Persistent> extends Nameable {
	/**
	 * 取持久器名称，通常为类名
	 * 
	 * @return 持久器名称
	 */
	String getName();

	/**
	 * 由ID取得持久对象
	 * 
	 * @param id 持久对象ID
	 * @return 相应的持久对象
	 */
	E get(UniteId id);

	/**
	 * 由ID取得持久对象
	 * 
	 * @param id 持久对象ID
	 * @return 相应的持久对象
	 */
	E get(String id);

	/**
	 * 由持久器中删除指定ID的持久对象
	 * 
	 * @param id 持久对象ID
	 * @return false表示没有对应的持久对象
	 */
	boolean remove(UniteId id);

	/**
	 * 由持久器中删除指定ID的持久对象
	 * 
	 * @param id 持久对象ID
	 * @return false表示没有对应的持久对象
	 */
	boolean remove(String id);

	/**
	 * 标示对象状态已变化，由持久器选择合适的时候刷写
	 * 
	 * @param object 持久对象
	 */
	void update(E object);

	/**
	 * 立刻保存对象
	 * 
	 * @param object 对象
	 */
	void persist(E object);

	/**
	 * 检查缓存中的持久对象是否还处理待刷写状态
	 * 
	 * @param object 要检查的对象
	 * @return 有在缓存且是待刷写状态返回true
	 */
	boolean isDirty(E object);

	/**
	 * 对象是否属于当前服务器（根据对象持久ID确定）
	 * 
	 * @param obj 持久对象
	 * @return 是属于当前服务器（标识）则返回true，否则false
	 */
	boolean isOwner(E obj);

	/**
	 * 若对象标示为更新的，立刻持久化对象
	 * 
	 * @param object 持久对象
	 */
	void flush(E object);

	/**
	 * 清理持久器，立刻持久所有标记更新的持久对象
	 */
	void cleanup();

	/**
	 * 取得持久器的服务器标识
	 * 
	 * @return 标识
	 */
	String getPersisterId();

	/**
	 * 生成新ID供新建对象使用
	 * 
	 * @return 新的ID
	 * @throws IdDuplicateException id重复异常
	 */
	UniteId getNewId() throws IdDuplicateException;

	/**
	 * 生成新ID并加上指定前缀
	 * 
	 * @param prefix ID前缀
	 * @return 新的ID
	 * @throws IdDuplicateException id重复异常
	 */
	UniteId getNewId(String prefix) throws IdDuplicateException;

	/**
	 * 取得持久对象持久化后的版本号
	 * 
	 * @param id 持久对象ID
	 * @return 返回版本号，若不找这ID的对象则返回null，若不支持版本号则返回空字串（""）
	 */
	String getVersion(UniteId id);

	/**
	 * 是否已经启用对象重载功能，还需要业务对象实现以下接口才能收到通知
	 * 
	 * @see Reloadable
	 * 
	 * @return 启用返回true
	 */
	boolean isReloadEnabled();

	/**
	 * 启用/关闭对象重载功能
	 * 
	 * @param enabled 启用/关闭
	 * @return 返回true表示支持此功能的开启，不支持则返回false
	 */
	boolean setReloadEnabled(boolean enabled);

	/**
	 * 只处理当前服务器持久类
	 * 
	 * @return 只处理返回true
	 */
	boolean isForOwnerEnabled();

	/**
	 * 启用/关闭只处理当前服务器持久类
	 * 
	 * @param enabled 启用/关闭
	 * @return 返回true表示支持此功能的开启，不支持则返回false
	 */
	boolean setForOwnerEnabled(boolean enabled);

	/**
	 * 查找ID为指定前缀的对象
	 * 
	 * @param prefix ID前缀
	 * @return 对象结果页
	 */
	ResultPage<E> startsWith(String prefix);

	/**
	 * 查找ID为指定前缀的对象
	 * 
	 * @param prefix ID前缀
	 * @return 对象结果页
	 */
	ResultPage<String> startsWithOfId(String prefix);

	/**
	 * 查找在指定时间段内变化（持久化）过的对象 (begin,end]
	 * 
	 * @param begin 开始时间，若=null则不限制开始时间
	 * @param end   结束时间，若=null则为当前时间
	 * @return 对象结果页
	 */
	ResultPage<E> search(Date begin, Date end);

	/**
	 * 查找在指定时间段内变化（持久化）过的对象 (begin,end]
	 * 
	 * @param begin 开始时间，若=null则不限制开始时间
	 * @param end   结束时间，若=null则为当前时间
	 * @return 对象ID形式的结果页
	 */
	ResultPage<String> searchOfId(Date begin, Date end);

	/**
	 * 查找ID在指定区间内的对象，id&gt;=from and id&lt;=to
	 * 
	 * @param from ID开始点，若为null则不限制开始（但to不能为null）
	 * @param to   ID结束点，若为null则不限制结束（但from不能为null）
	 * @return 对象结果页
	 */
	ResultPage<E> searchRange(String from, String to);

	/**
	 * 查找ID在指定区间内的对象，id&gt;=from and id&lt;=to
	 * 
	 * @param from ID开始点，若为null则不限制开始（但to不能为null）
	 * @param to   ID结束点，若为null则不限制结束（但from不能为null）
	 * @return 对象ID形式的结果页
	 */
	ResultPage<String> searchRangeOfId(String from, String to);

	/**
	 * 查找在指定时间段内变化（持久化）过的对象
	 * 
	 * @param serverId 服务器标识（1~255）
	 * @param begin    开始时间，若=null则不限制开始时间
	 * @param end      结束时间，若=null则为当前时间
	 * @return 结果集迭代器
	 */
	Iterator<E> search(String serverId, Date begin, Date end);

	/**
	 * 查找在指定时间段内变化（持久化）过的对象
	 * 
	 * @param serverId 服务器标识（1~255）
	 * @param begin    开始时间，若=null则不限制开始时间
	 * @param end      结束时间，若=null则为当前时间
	 * @return 结果集迭代器
	 */
	Iterator<String> searchOfId(String serverId, Date begin, Date end);

	/**
	 * 查找ID在指定区间内的对象，id&gt;=from and id&lt;=to
	 * 
	 * @param serverId 服务器标识
	 * 
	 * @param from     ID开始点，若为null则不限制开始（但to不能为null）
	 * @param to       ID结束点，若为null则不限制结束（但from不能为null）
	 * @return 结果集迭代器
	 */
	Iterator<E> searchRange(String serverId, String from, String to);

	/**
	 * 查找ID在指定区间内的对象，id&gt;=from and id&lt;=to
	 * 
	 * @param serverId 服务器标识
	 * 
	 * @param from     ID开始点，若为null则不限制开始（但to不能为null）
	 * @param to       ID结束点，若为null则不限制结束（但from不能为null）
	 * @return 结果集迭代器
	 */
	Iterator<String> searchRangeOfId(String serverId, String from, String to);

	/**
	 * 条件查询
	 * 
	 * @param condition 条件 {@link ConditionUtil}
	 * @return 对象结果页
	 */
	ResultPage<E> search(Condition condition);

	/**
	 * 条件查询
	 * 
	 * @param condition 条件 {@link ConditionUtil}
	 * @return 对象结果页
	 */
	ResultPage<String> searchOfId(Condition condition);

	/**
	 * 条件查询
	 * 
	 * @param condition 条件 {@link ConditionUtil}
	 * @param orderBy   排序 {@link OrderByUtil}
	 * @return 对象结果页
	 */
	ResultPage<E> search(Condition condition, OrderBy orderBy);

	/**
	 * 条件查询
	 * 
	 * @param condition 条件 {@link ConditionUtil}
	 * @param orderBy   排序 {@link OrderByUtil}
	 * @return 对象结果页
	 */
	ResultPage<String> searchOfId(Condition condition, OrderBy orderBy);

	/**
	 * 添加监听
	 * 
	 * @param l 监听对象
	 */
	void addListener(ChangeListener<E> l);

	/**
	 * 移除监听
	 * 
	 * @param l 监听对象
	 */
	void removeListener(ChangeListener<E> l);

}

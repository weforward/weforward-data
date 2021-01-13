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
package cn.weforward.data.log;

import java.util.Date;

import cn.weforward.common.Nameable;
import cn.weforward.common.ResultPage;

/**
 * 日志记录器提供对业务日志记录、检索及标记的功能，不适合在高要求的环境下使用（对同记录目标每秒100条日志以上）
 * 
 * @author liangyi
 * 
 */
public interface BusinessLogger extends Nameable {
	/**
	 * 日志记录器名（标识）
	 * 
	 * @return 日志记录器（标识）
	 */
	String getName();

	/**
	 * 写日志
	 * 
	 * @param id     对象ID
	 * @param author 操作者
	 * @param action 动作
	 * @param what   什么
	 */
	void writeLog(String id, String author, String action, String what);

	/**
	 * 写日志
	 * 
	 * @param id     对象ID
	 * @param author 操作者
	 * @param action 动作
	 * @param what   什么
	 * @param note   用户描述
	 */
	void writeLog(String id, String author, String action, String what, String note);

	/**
	 * 取得对象的相关日志
	 * 
	 * @param id 对象ID
	 * @return 与之相关的倒序日志页结果
	 */
	ResultPage<BusinessLog> getLogs(String id);

	/**
	 * 查询指定时间段的日志
	 * 
	 * @param id    对象ID
	 * @param begin 开始时间，可为null表示由最早
	 * @param end   结束时间，可为null表示到最后
	 * @return 与之相关的顺序日志页结果
	 */
	ResultPage<BusinessLog> searchLogs(String id, Date begin, Date end);
}

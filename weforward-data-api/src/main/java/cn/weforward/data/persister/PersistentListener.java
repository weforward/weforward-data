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

import cn.weforward.data.UniteId;

/**
 * 监听对象持久事件
 * 
 * @author liangyi
 * 
 * @version 1.0
 * 
 */
public interface PersistentListener {
	/**
	 * 发生在对象持久化前
	 * 
	 * @param persister 持久器
	 */
	void onBeforePersistence(Persister<? extends Persistent> persister);

	/**
	 * 发生在对象持久化后
	 * 
	 * @param persister 持久器
	 * @param version   持久化后的版本号
	 */
	void onAfterPersistence(Persister<? extends Persistent> persister, String version);

	/**
	 * 发生在对象由持久数据反射后
	 * 
	 * @param persister     持久器
	 * @param persistenceId 持久化ID
	 * @param version       持久化的版本号
	 * @param driveIt       控制实例标识
	 */
	void onAfterReflect(Persister<? extends Persistent> persister, UniteId persistenceId, String version,
			String driveIt);

}

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

import cn.weforward.common.ResultPage;

/**
 * 为远端对象提供脱机支持
 * 
 * @author liangyi
 * 
 */
public interface OfflineSupplier<E> {

	/**
	 * 由脱机数据加载
	 * 
	 * @param id ID
	 * @return 加载相应的项，没有则返回null
	 */
	ObjectWithVersion<E> get(String id);

	/**
	 * 更新脱机项
	 * 
	 * @param id  ID
	 * @param obj 要更新到脱机中的对象
	 * @return 更新后的版本号，异步写入或不支持则返回null
	 */
	String update(String id, E obj);

	/**
	 * 由脱机数据删除
	 * 
	 * @param id ID
	 * @return 是否删除成功
	 */
	boolean remove(String id);

	/**
	 * 清空脱机数据
	 */
	void removeAll();

	/**
	 * 清理缓存
	 */
	void cleanup();

	/**
	 * 按ID区间查询脱机缓存对象
	 * 
	 * @param first 开始ID，若为null则表示由第一项开始
	 * @param last  结束ID，若为null则表示到最后一项
	 * @return 页结果
	 */
	ResultPage<E> searchRange(String first, String last);
}

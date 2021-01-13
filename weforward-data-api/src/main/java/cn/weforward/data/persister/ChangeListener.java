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

import java.util.function.Supplier;

import cn.weforward.common.NameItem;

/**
 * 变化监听
 * 
 * @author daibo
 *
 */
public interface ChangeListener<E extends Persistent> {
	/** 变化类型-未知 */
	NameItem TYPE_UNKNOW = NameItem.valueOf("未知", -1);
	/** 变化类型-新增 */
	NameItem TYPE_NEW = NameItem.valueOf("新增", 1);
	/** 变化类型-更新 */
	NameItem TYPE_UPDATE = NameItem.valueOf("更新", 2);
	/** 变化类型-删除 */
	NameItem TYPE_DELETE = NameItem.valueOf("删除", 3);

	/**
	 * 变化监听
	 * 
	 * @param type         类型
	 * @param id           唯一标识
	 * @param supplierdata 数据
	 */
	void onChange(NameItem type, String id, Supplier<E> supplierdata);
}

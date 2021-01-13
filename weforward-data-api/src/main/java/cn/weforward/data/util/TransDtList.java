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
package cn.weforward.data.util;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;

import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtList;

/**
 * 转换dtlist为 list
 * 
 * @author daibo
 *
 * @param <E> 对象
 */
public abstract class TransDtList<E> extends AbstractList<E> {
	/** dt列表 */
	private DtList m_List;

	public TransDtList(DtList list) {
		m_List = list == null ? DtList.EMPTY : list;
	}

	@Override
	public E get(int index) {
		return trans(m_List.getItem(index));
	}

	/* 转换 */
	protected abstract E trans(DtBase item);

	@Override
	public int size() {
		return m_List.size();
	}

	/**
	 * 构造list
	 * 
	 * @param <E>   对象
	 * @param list  列表
	 * @param trans 转换函数
	 * @return 转换后列表
	 */
	public static <E> List<E> valueOf(DtList list, Function<DtBase, E> trans) {
		return new TransDtList<E>(list) {

			@Override
			protected E trans(DtBase item) {
				return trans.apply(item);
			}
		};
	}

}

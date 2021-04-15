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
package cn.weforward.data.search.util;

import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.data.search.IndexResult;
import cn.weforward.data.search.IndexResults;

/**
 * IndexResults的工具类
 * 
 * @author daibo
 *
 */
public class IndexResultsHelper {
	/** 空对象 */
	private static final IndexResults _nil = new Empty();

	private IndexResultsHelper() {

	}

	/**
	 * 空对象
	 * 
	 * @return 结果集
	 */
	public static IndexResults empty() {
		return _nil;
	}

	/**
	 * 快速封装（构造）单结果项IndexResults
	 * 
	 * @param element 结果项
	 * @return 封装后的ResultPage
	 */
	static public IndexResults singleton(IndexResult element) {
		return new Singleton(element);
	}

	/**
	 * 当结果只有一项时，能使用其作简单快捷的封装
	 * 
	 * @author daibo
	 * 
	 */
	public static class Singleton extends ResultPageHelper.Singleton<IndexResult> implements IndexResults {

		public Singleton(IndexResult element) {
			super(element);
		}

		@Override
		public void sort(String attribut, int option) {

		}

		@Override
		public IndexResults snapshot() {
			return new Singleton(element);
		}

	}

	/**
	 * 空对象
	 * 
	 * @author daibo
	 *
	 */
	public static class Empty extends ResultPageHelper.Empty<IndexResult> implements IndexResults {

		@Override
		public void sort(String attribut, int option) {

		}

		@Override
		public IndexResults snapshot() {
			return new Empty();
		}

	}
}

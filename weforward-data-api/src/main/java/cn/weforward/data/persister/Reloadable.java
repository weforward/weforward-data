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

/**
 * 实现此接口的持久对象表示支持由另一实例重装信息
 * 
 * consistent
 * 
 * @author liangyi
 * 
 */
public interface Reloadable<E extends Persistent> {
	/**
	 * 在持久对象的另一个实例发生变化（通常是多服务器环境下）时被调用，若使用另一个实例的对象信息，则实现复制的过程且返回true，否则直接返回false
	 * 
	 * @param persister 持久器
	 * @param other     同标识下另一个实例的持久对象，若为null表示对象被删除
	 * @return 若允许另一个实例的信息则复制后返回true
	 */
	boolean onReloadAccepted(Persister<E> persister, E other);
}

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

/**
 * 日志记录器集
 * 
 * @author liangyi
 * 
 */
public interface BusinessLoggerFactory extends Iterable<BusinessLogger> {
	/**
	 * 创建日志记录器
	 * 
	 * @param name 记录器名
	 * @return 新建的记录器
	 * @exception IllegalArgumentException 若已有同名日志记录器或需要的存储环境不适合时抛出异常
	 */
	BusinessLogger createLogger(String name);

	/**
	 * 取指定名称的日志记录器
	 * 
	 * @param name 名称（标识）
	 * @return 没有则返回null
	 */
	BusinessLogger getLogger(String name);

	/**
	 * 打开定名称的日志记录器
	 * 
	 * @param name 名称（标识）
	 * @return 相应的日志记录器
	 */
	BusinessLogger openLogger(String name);
}

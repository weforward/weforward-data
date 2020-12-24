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

/**
 * 日志是以时间为顺序进行记录的信息，记录的内容为“什么时间”“谁”对“什么物件”“做了什么”“为什么”，它由四部分组成：
 * <p>
 * 一、日志记录的对象
 * <p>
 * 二、系统描述（一种格式化供系统检索及识别的文本）
 * <p>
 * 三、用户描述（由使用者输入的内容，基于文本，但可以是HTML或XML格式化的内容）
 * <p>
 * 四、操作者
 * 
 */
public interface BusinessLog {

	/**
	 * 日志ID，由所记录对象的ID、时间及所属服务器标识组成
	 * 
	 * @return ID
	 * 
	 */
	String getId();

	/**
	 * 日志时间
	 * 
	 * @return 时间
	 */
	Date getTime();

	/**
	 * 记录的对象ID
	 * 
	 * @return ID
	 */
	String getTarget();

	/**
	 * 作者
	 * 
	 * @return 作者
	 */
	String getAuthor();

	/**
	 * 取动作（部分）
	 * 
	 * @return 动作
	 */
	String getAction();

	/**
	 * 取什么（部分）
	 * 
	 * @return 什么
	 */
	String getWhat();

	/**
	 * 备注
	 * 
	 * @return 备注
	 */
	String getNote();

}

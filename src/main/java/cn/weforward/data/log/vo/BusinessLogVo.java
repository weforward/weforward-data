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
package cn.weforward.data.log.vo;

import java.util.Date;

import cn.weforward.data.log.BusinessLog;

/**
 * 日志vo
 * 
 * @author daibo
 *
 */
public class BusinessLogVo implements BusinessLog {
	/** 唯一id */
	protected String m_Id;
	/** 时间 */
	protected Date m_Time;
	/** 记录的对象ID */
	protected String m_Target;
	/** 作者 */
	protected String m_Author;
	/** 动作 */
	protected String m_Action;
	/** 什么 */
	protected String m_What;
	/** 备注 */
	protected String m_Note;

	public BusinessLogVo() {

	}

	public BusinessLogVo(String id, Date time, String target, String author, String action,
			String what, String note) {
		m_Id = id;
		m_Time = time;
		m_Target = target;
		m_Author = author;
		m_Action = action;
		m_What = what;
		m_Note = note;
	}

	/**
	 * 构造对象
	 * 
	 * @param log
	 *            日志对象
	 * @return 日志VO对象
	 */
	public static BusinessLogVo valueOf(BusinessLog log) {
		if (null == log) {
			return null;
		}
		if (log instanceof BusinessLogVo) {
			return (BusinessLogVo) log;
		} else {
			return new BusinessLogVo(log.getId(), log.getTime(), log.getTarget(), log.getAuthor(),
					log.getAction(), log.getWhat(), log.getNote());
		}
	}

	public void setId(String id) {
		m_Id = id;
	}

	@Override
	public String getId() {
		return m_Id;
	}

	public void setTime(Date time) {
		m_Time = time;
	}

	@Override
	public Date getTime() {
		return m_Time;
	}

	public void setTarget(String target) {
		m_Target = target;
	}

	@Override
	public String getTarget() {
		return m_Target;
	}

	public void setAuthor(String author) {
		m_Author = author;
	}

	@Override
	public String getAuthor() {
		return m_Author;
	}

	public void setAction(String action) {
		m_Action = action;
	}

	@Override
	public String getAction() {
		return m_Action;
	}

	public void setWhat(String what) {
		m_What = what;
	}

	@Override
	public String getWhat() {
		return m_What;
	}

	public void setNote(String note) {
		m_Note = note;
	}

	@Override
	public String getNote() {
		return m_Note;
	}

	@Override
	public String toString() {
		return "{id:" + m_Id + ",time:" + m_Time + ",target:" + m_Target + ",author:" + m_Author
				+ ",action:" + m_Action + ",what:" + m_What + ",note:" + m_Note + "}";
	}
}

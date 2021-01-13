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
 * 包含版本的对象
 * 
 * @author daibo
 *
 * @param <E> 对象
 */
public class ObjectWithVersion<E> {
	/** 对象 */
	protected E m_Object;
	/** 版本号 */
	protected String m_Version;
	/** 控制实例标识 */
	protected String m_DriveIt;

	public ObjectWithVersion(E obj, String ver, String driveIt) {
		m_Object = obj;
		m_Version = ver;
		m_DriveIt = driveIt;
	}

	/**
	 * 对象
	 * 
	 * @return 对象
	 */
	public E getObject() {
		return m_Object;
	}

	/**
	 * 版本号
	 * 
	 * @return 版本
	 */
	public String getVersion() {
		return m_Version;
	}

	/**
	 * 控制实例标识
	 * 
	 * @return 标识
	 */
	public String getDriveIt() {
		return m_DriveIt;
	}

}

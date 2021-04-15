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
package cn.weforward.data.mongodb.persister;

import javax.annotation.Resource;

import cn.weforward.data.annotation.Index;
import cn.weforward.data.persister.support.AbstractPersistent;

/**
 * 索引
 * 
 * @author daibo
 *
 */
public class MyIndex extends AbstractPersistent<DataDi> {
	@Index
	@Resource
	protected String m_Name;
	@Resource
	protected int m_Age;
	@Resource
	protected Vo m_Vo;

	protected MyIndex(DataDi di) {
		super(di);
	}

	public MyIndex(DataDi di, String name, int age, String date) {
		super(di);
		genPersistenceId();
		m_Name = name;
		m_Age = age;
		m_Vo = new Vo();
		m_Vo.myString = date;
		markPersistenceUpdate();
	}

	public void setName(String name) {
		m_Name = name;
		markPersistenceUpdate();
	}

}

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
package cn.weforward.data.mysql;

import java.util.Date;

import javax.annotation.Resource;

import cn.weforward.data.UniteId;
import cn.weforward.data.persister.Persister;
import cn.weforward.data.persister.Reloadable;
import cn.weforward.data.persister.support.AbstractPersistent;

public class MyData extends AbstractPersistent<TestDi> implements Reloadable<MyData> {
	@Resource
	protected int m_Num;
	@Resource
	protected String m_Value;
	@Resource
	protected Date m_Time;
	@Resource
	protected SimpleVo m_Vo;

	protected MyData(TestDi di) {
		super(di);
	}

	public MyData(TestDi di, String id, int num, String value, Date time, SimpleVo vo) {
		super(di);
		m_Id = UniteId.valueOf(id, getClass());
		m_Num = num;
		m_Value = value;
		m_Time = time;
		m_Vo = vo;
		persistenceUpdateNow();
	}

	@Override
	public boolean onReloadAccepted(Persister<MyData> persister, MyData other) {
		System.out.println("onReload:" + other);
		return true;
	}

}

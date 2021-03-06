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
package cn.weforward.data.mongodb.array;

import java.util.List;

import javax.annotation.Resource;

import cn.weforward.data.annotation.Inherited;
import cn.weforward.data.annotation.ResourceExt;
import cn.weforward.data.array.LabelElement;

@Inherited
public class MyData extends AbstractData implements LabelElement {
	@Resource
	private Vo m_MyVo;
	@Resource
	private Vo[] m_MyVos;
	@ResourceExt(component = Vo.class)
	private List<Vo> m_MyVoList;
	@Resource
	protected String m_Id;

	public MyData() {

	}

	public MyData(String id) {
		m_Id = id;
	}

	public void setVo(Vo vo) {
		m_MyVo = vo;
	}

	public Vo getVo() {
		return m_MyVo;
	}

	public void setVos(Vo[] vos) {
		m_MyVos = vos;
	}

	public Vo[] getVos() {
		return m_MyVos;
	}

	public void setVoList(List<Vo> list) {
		m_MyVoList = list;
	}

	public List<Vo> getVoList() {
		return m_MyVoList;
	}

	@Override
	public String getIdForLabel() {
		return m_Id;
	}

}

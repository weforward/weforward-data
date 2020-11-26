package cn.weforward.data;

import javax.annotation.Resource;

import cn.weforward.data.annotation.Index;

public class IndexData {
	@Index
	@Resource
	protected String m_Value;
	@Resource
	public IndexData m_Child;

	@Index
	public String getValue() {
		return m_Value;
	}

	public IndexData getChild() {
		return m_Child;
	}
}

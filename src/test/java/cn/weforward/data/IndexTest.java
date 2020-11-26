package cn.weforward.data;

import java.util.Enumeration;

import org.junit.Test;

import cn.weforward.data.util.MethodMapper;

public class IndexTest {
	@Test
	public void test() {
		Enumeration<String> indexs = ((MethodMapper<?>) MethodMapper.valueOf(IndexData.class)).getIndexAttributeNames(3);
		while (indexs.hasMoreElements()) {
			System.out.println(indexs.nextElement());
		}
	}
}

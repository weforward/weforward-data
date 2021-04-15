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
package cn.weforward.data;

import java.util.List;

import org.junit.Test;

import cn.weforward.data.util.TransDtList;
import cn.weforward.data.util.VersionTags;
import cn.weforward.protocol.datatype.DtString;
import cn.weforward.protocol.support.datatype.SimpleDtList;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

public class UtilTest {

	public void test() {
		SimpleDtList list = new SimpleDtList();
		SimpleDtObject dt = new SimpleDtObject();
		dt.put("list", list);
		list.add("111");
		list.add("222");
		List<Integer> l = TransDtList.valueOf(list, (v) -> Integer.valueOf(((DtString) v).value()));
		l.stream().forEach((v) -> System.out.println(v));
	}

	@Test
	public void testVers() {
		System.out.println(VersionTags.next("x00ff", "x00ff+1", false));
		System.out.println(VersionTags.next("x00ff", "x00ff:1", false));
		System.out.println(VersionTags.next("x00ff",
				"0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz:1",
				false));
	}
}

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
package cn.weforward.data.mongodb.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import org.bson.Document;

import cn.weforward.common.KvPair;
import cn.weforward.common.util.SimpleKvPair;
import cn.weforward.protocol.datatype.DataType;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.support.datatype.AbstractDtObject;

/**
 * 基于document的dtobject实现
 * 
 * @author daibo
 *
 */
public class DocumentDtObject extends AbstractDtObject implements DtObject {

	protected Document m_Doc;

	private final static Document EMPTY = new Document();

	public static DocumentDtObject valueOf(Document doc) {
		return new DocumentDtObject(null == doc ? EMPTY : doc);
	}

	protected DocumentDtObject(Document doc) {
		m_Doc = doc;
	}

	@Override
	public DataType type() {
		return DataType.OBJECT;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(m_Doc.keySet());
	}

	@Override
	public Enumeration<KvPair<String, DtBase>> getAttributes() {
		Iterator<Map.Entry<String, Object>> it = m_Doc.entrySet().iterator();
		return new Enumeration<KvPair<String, DtBase>>() {

			@Override
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			@Override
			public KvPair<String, DtBase> nextElement() {
				Map.Entry<String, Object> e = it.next();
				DtBase base = MongodbUtil.change(e.getValue());
				return SimpleKvPair.valueOf(e.getKey(), base);
			}
		};
	}

	@Override
	public int getAttributeSize() {
		return m_Doc.size();
	}

	@Override
	protected DtBase getAttributeInner(String name) {
		Object v = m_Doc.get(name);
		return MongodbUtil.change(v);
	}

}

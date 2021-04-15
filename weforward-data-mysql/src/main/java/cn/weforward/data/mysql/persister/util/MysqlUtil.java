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
package cn.weforward.data.mysql.persister.util;

import java.io.IOException;
import java.util.Enumeration;

import cn.weforward.common.KvPair;
import cn.weforward.common.execption.InvalidFormatException;
import cn.weforward.common.json.JsonUtil;
import cn.weforward.protocol.datatype.DataType;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtBoolean;
import cn.weforward.protocol.datatype.DtList;
import cn.weforward.protocol.datatype.DtNumber;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.datatype.DtString;
import cn.weforward.protocol.serial.JsonDtList;
import cn.weforward.protocol.serial.JsonDtObject;

public class MysqlUtil {

	public static void formatObject(DtObject object, Appendable output) throws IOException {
		if (object instanceof JsonDtObject) {
			String json = ((JsonDtObject) object).getJsonString();
			if (null != json) {
				output.append(json);
				return;
			}
		}
		output.append('{');
		boolean first = true;
		Enumeration<KvPair<String, DtBase>> atts = object.getAttributes();
		while (atts.hasMoreElements()) {
			KvPair<String, DtBase> att = atts.nextElement();
			if (first) {
				first = false;
			} else {
				output.append(',');
			}
			output.append('"');
			JsonUtil.escape(att.getKey(), output);
			output.append('"');
			output.append(':');
			formatValue(att.getValue(), output);
		}
		output.append('}');
	}

	public static void formatList(DtList list, Appendable output) throws IOException {
		if (list instanceof JsonDtList) {
			String json = ((JsonDtList) list).getJsonString();
			if (null != json) {
				output.append(json);
				return;
			}
		}
		output.append('[');
		boolean first = true;
		Enumeration<DtBase> values = list.items();
		while (values.hasMoreElements()) {
			DtBase value = values.nextElement();
			if (first) {
				first = false;
			} else {
				output.append(',');
			}
			formatValue(value, output);
		}
		output.append(']');
	}

	public static void formatValue(DtBase value, Appendable output) throws IOException {
		if (null == value) {
			output.append("null");
			return;
		}
		if (DataType.STRING == value.type() || DataType.DATE == value.type()) {
			DtString v = (DtString) value;
			output.append('"');
			JsonUtil.escape(v.value(), output);
			output.append('"');
			return;
		}
		if (DataType.NUMBER == value.type()) {
			DtNumber v = (DtNumber) value;
			output.append(v.valueNumber().toString());
			return;
		}
		if (DataType.BOOLEAN == value.type()) {
			DtBoolean v = (DtBoolean) value;
			if (v.value()) {
				output.append("true");
			} else {
				output.append("false");
			}
			return;
		}
		if (DataType.OBJECT == value.type()) {
			formatObject((DtObject) value, output);
			return;
		}
		if (DataType.LIST == value.type()) {
			formatList((DtList) value, output);
			return;
		}
		throw new InvalidFormatException("JSON值类型不支持" + value);
	}
}

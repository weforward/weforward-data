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
package cn.weforward.data.jdbc;

import cn.weforward.common.util.StringBuilderPool;

/**
 * SQL字串辅助
 * 
 * @author liangyi
 * 
 */
public final class SqlString {
	private StringBuilder m_Builder;

	public SqlString(CharSequence str) {
		m_Builder = new StringBuilder(str);
	}

	public SqlString() {
		m_Builder = new StringBuilder(64);
	}

	public final void clear() {
		m_Builder.setLength(0);
	}

	public String toString() {
		return m_Builder.toString();
	}

	public final SqlString append(CharSequence str) {
		m_Builder.append(str);
		return this;
	}

	public final SqlString append(char ch) {
		m_Builder.append(ch);
		return this;
	}

	public final SqlString append(char str[]) {
		m_Builder.append(str);
		return this;
	}

	public final SqlString append(int val) {
		m_Builder.append(val);
		return this;
	}

	public final SqlString append(long val) {
		m_Builder.append(val);
		return this;
	}

	public final SqlString append(double val) {
		m_Builder.append(val);
		return this;
	}

	public final SqlString appendEscape(CharSequence str) {
		int len = str.length();
		for (int i = 0; i < len; i++) {
			appendEscape(str.charAt(i), m_Builder);
		}
		return this;
	}

	public final SqlString appendEscape(char str[]) {
		int len = str.length;
		for (int i = 0; i < len; i++) {
			appendEscape(str[i], m_Builder);
		}
		return this;
	}

	public final SqlString appendEscape(char ch) {
		appendEscape(ch, m_Builder);
		return this;
	}

	static final void appendEscape(char ch, StringBuilder builder) {
		if ('\'' == ch) {
			builder.append("''");
			// } else if ('%' == ch) {
			// builder.append("[%]");
			// } else if ('_' == ch) {
			// builder.append("[_]");
			// } else if ('[' == ch) {
			// builder.append("[[]");
			// } else if ('\\' == ch) {
			// builder.append("\\\\");
		} else {
			builder.append(ch);
		}
	}

	static public final String escape(CharSequence sqlValue) {
		if (null == sqlValue || 0 == sqlValue.length()) {
			return "";
		}

		// StringBuilder builder = new StringBuilder(sqlValue.length());
		StringBuilder builder = StringBuilderPool._8k.poll();
		try {
			int len = sqlValue.length();
			for (int i = 0; i < len; i++) {
				appendEscape(sqlValue.charAt(i), builder);
			}
			return builder.toString();
		} finally {
			StringBuilderPool._8k.offer(builder);
		}
	}

	static public final StringBuilder escape(CharSequence sqlValue, StringBuilder builder) {
		if (null == sqlValue || 0 == sqlValue.length()) {
			return builder;
		}

		int len = sqlValue.length();
		for (int i = 0; i < len; i++) {
			appendEscape(sqlValue.charAt(i), builder);
		}
		return builder;
	}
}

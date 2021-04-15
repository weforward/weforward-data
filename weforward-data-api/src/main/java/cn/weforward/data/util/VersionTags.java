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
package cn.weforward.data.util;

import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.common.util.StringUtil;

/**
 * 把版本号封装为标记组，版本号间以分号分隔，如：“9a+1;ae+1f”
 * 
 * @author liangyi
 * 
 */
public class VersionTags {
	/** 同一节点下版本合并数 */
	public static final int MAX_SAME_ORDIAL = 5;

	public final String version;
	int m_Postions;
	int m_End;

	public VersionTags(String version) {
		if (null == version) {
			version = "";
		}
		this.version = version;
		m_End = this.version.length();
	}

	/**
	 * 取最后的版本标记
	 * 
	 * @return 版本标记
	 */
	public VersionTag last() {
		m_Postions = this.version.lastIndexOf(VersionTag.VERSION_SPEARATOR);
		return VersionTag.valueOf(
				(-1 == m_Postions) ? this.version : this.version.substring(m_Postions + 1));
	}

	/**
	 * 取得上一个版本标记
	 * 
	 * @return 版本标记
	 */
	public VersionTag previous() {
		if (m_Postions < 1) {
			return null;
		}
		m_End = m_Postions;
		m_Postions = this.version.lastIndexOf(VersionTag.VERSION_SPEARATOR, m_End - 1);
		return VersionTag
				.valueOf(this.version.substring(((-1 == m_Postions) ? 0 : m_Postions + 1), m_End));
	}

	@Override
	public String toString() {
		return this.version;
	}

	/**
	 * 替换最后的版本
	 * 
	 * @param vt
	 *            替换的版本
	 * @return 替换后的版本标记组
	 */
	public VersionTags replaceLast(VersionTag vt) {
		int idx = this.version.lastIndexOf(VersionTag.VERSION_SPEARATOR);
		if (-1 == idx) {
			return new VersionTags(vt.toString());
		}
		return new VersionTags(this.version.substring(0, idx + 1) + vt.toString());
	}

	/**
	 * 版本号是否被删除的标记（最后的版本序号是负数）
	 * 
	 * @param version
	 *            版本号
	 * @return 是则返回true
	 */
	public static final boolean isRemoved(String version) {
		if (StringUtil.isEmpty(version)) {
			return false;
		}
		int idx = version.lastIndexOf(VersionTag.VERSION_SPEARATOR);
		return VersionTag.isRemoved((-1 == idx) ? version : version.substring(idx + 1));
	}

	/**
	 * 取得下一个版本标记
	 * 
	 * @param node
	 *            节点ID
	 * @param ver
	 *            当前版本号
	 * @param isRemoved
	 *            是否为删除
	 * @return 下一个版本的标记串
	 */
	public static final String next(String node, String ver, boolean isRemoved) {
		if (StringUtil.isEmpty(ver)) {
			// 还没有版本号，那就由1开始
			ver = VersionTag.format(node, isRemoved ? -1 : 1);
			return ver;
		}

		// 使用版本标签组枚举，看是追加版本标签还是保留最后版本标签
		VersionTags vts = new VersionTags(ver);
		VersionTag last = vts.last();
		if (0 == last.ordinal) {
			// 版本序号是0,直接由1开始
			ver = VersionTag.format(node, isRemoved ? -1 : 1);
			return ver;
		}
		int i = 0;
		VersionTag vt = last;
		// 检查连续节点版本号是否有MAX_SAME_ORDIAL个或以上
		while (null != vt && vt.node.equals(node) && (++i) < MAX_SAME_ORDIAL) {
			vt = vts.previous();
		}
		if (i >= MAX_SAME_ORDIAL) {
			// 由最后开始相同节点版本标签已经超过MAX_SAME_ORDIAL，只需要保留最后版本，防止版本号过长
			ver = last.next(isRemoved).toString();
			return ver;
		}

		// 追加版本标签
		last = last.next(node, isRemoved);
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			builder.append(ver).append(VersionTag.VERSION_SPEARATOR);
			VersionTag.format(last.node, last.ordinal, builder);
			ver = builder.toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
		return ver;
	}

}

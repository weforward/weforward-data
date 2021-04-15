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

import cn.weforward.common.crypto.Hex;
import cn.weforward.common.util.StringBuilderPool;

/**
 * 版本号标记（完整版本号中的一段）
 * 
 * <pre>
 * 分布的存储使用版本号来辅助合作及识别冲突，版本号可能由多个标记组成，每个标记由三部分组成：
 * - 存储节点（服务器）ID，由数字及字母组成，一般为两字符的16进制服务器ID，如“fe”
 * - 一个字符修改标识（也作为节点ID与序号的分隔符）：“+”表示修改，“-”表示删除
 * - 修改序号以十六进制字符表示，如“1f”
 * 多标记的版本号以分号分隔，如：“fe+1;fe+1f;fe-1f”
 * </pre>
 * 
 * @author liangyi
 * 
 */
public class VersionTag {
	/** 版本相同 */
	public final static int VERSION_SAME = 0x00;
	/** 版本比较低 */
	public final static int VERSION_LOW = -0x1;
	/** 版本比较高 */
	public final static int VERSION_HIGH = 0x1;
	/** 版本冲突 */
	public final static int VERSION_CONFLICT = -0x3;
	/** 版本异常 */
	public final static int VERSION_ERROR = -0x10;

	/** 版本号段分隔号 */
	public static final char VERSION_SPEARATOR = ';';
	/** 版本标识符- 修改 */
	public static final char VERSION_MARK_CHANGE = '+';
	/** 版本标识符- 删除 */
	public static final char VERSION_MARK_REMOVE = '-';
	/** 版本标识符- 未有版本 */
	public static final String UNVERSION = "~";

	/** 空版本号 */
	private static final VersionTag _nil = new VersionTag();

	/** 节点ID */
	public final String node;
	/** 序号 */
	public final int ordinal;

	/**
	 * 空版本标记
	 */
	private VersionTag() {
		this.node = "";
		this.ordinal = 0;
	}

	private VersionTag(String versionTag) {
		int len = versionTag.length();
		if (len < 2) {
			// XXX 不应该有这种情况哦
			this.node = versionTag;
			this.ordinal = 0;
			return;
		}
		// 控制节点名称不能超过64字符
		if (len > 64) {
			len = 64;
		}
		char mark;
		for (int i = 0; i < len; i++) {
			mark = versionTag.charAt(i);
			if (VERSION_MARK_CHANGE == mark || VERSION_MARK_REMOVE == mark) {
				// 找到标识符了
				this.node = versionTag.substring(0, i);
				int ord;
				if (i < versionTag.length()) {
					String hex = versionTag.substring(i + 1);
					ord = Hex.parseHex(hex, hex.length(), 0);
				} else {
					ord = 0;
				}
				this.ordinal = (VERSION_MARK_REMOVE == mark) ? -ord : ord;
				return;
			}
		}

		// 直接标没有版本
		this.node = UNVERSION;
		this.ordinal = 0;
	}

	/**
	 * 检查标记版本是否有删除标记（序号小于0）
	 * 
	 * @param versionTag
	 *            版本，如 “fa+1”，“fa-1”
	 * @return 是则返回true
	 */
	public static final boolean isRemoved(String versionTag) {
		if (null == versionTag) {
			return false;
		}
		int len = versionTag.length();
		// 找到标识符位置，所在点不太应该超过5
		if (len > 5) {
			len = 5;
		}
		char mark;
		for (int i = 0; i < len; i++) {
			mark = versionTag.charAt(i);
			if (VERSION_MARK_REMOVE == mark) {
				// 找到标识符了
				if ((i + 1) < len && VERSION_MARK_REMOVE == versionTag.charAt(i + 1)) {
					// XXX 兼容旧的“--xx”版本号
					++i;
					continue;
				}
				return true;
			} else if (VERSION_MARK_CHANGE == mark) {
				return false;
			}
		}

		// XXX 尝试对旧存储项的版本格式的支持
		if (versionTag.length() < 2) {
			return false;
		}
		return ('-' == versionTag.charAt(1)
				&& (versionTag.length() < 3 || '-' != versionTag.charAt(0)));
	}

	/**
	 * 创建版本号标记
	 * 
	 * @param node
	 *            节点ID
	 * @param ordinal
	 *            版本序号（由1开始）
	 */
	public VersionTag(String node, int ordinal) {
		this.node = node;
		this.ordinal = ordinal;
	}

	/**
	 * 取得正版本号
	 * 
	 * @return 正整数
	 */
	public int getOrdinalAbs() {
		return (this.ordinal < 0) ? (-this.ordinal) : this.ordinal;
	}

	public String getNode() {
		return node;
	}

	public int getOrdinal() {
		return ordinal;
	}

	/**
	 * 是否已删除的版本标记
	 * 
	 * @return 是删除版本则返回true
	 */
	public boolean isRemoved() {
		// 若版本号小于0为删除
		return (ordinal < 0);
	}

	/**
	 * 返回下一个版本标记
	 * 
	 * @return 版本标记
	 */
	public VersionTag next() {
		return new VersionTag(this.node, this.getOrdinalAbs() + 1);
	}

	/**
	 * 返回下一个版本标记
	 * 
	 * @param isRemoved
	 *            是否标记为删除的
	 * @return 版本标记
	 */
	public VersionTag next(boolean isRemoved) {
		return next(this.node, isRemoved);
	}

	/**
	 * 返回下一个版本标记
	 * 
	 * @param node
	 *            新指定的节点ID
	 * @param isRemoved
	 *            是否标记为删除的
	 * 
	 * @return 版本标记
	 */
	public VersionTag next(String node, boolean isRemoved) {
		if (isRemoved) {
			return new VersionTag(node, -(this.getOrdinalAbs() + 1));
		}
		return new VersionTag(node, this.getOrdinalAbs() + 1);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof VersionTag) {
			VersionTag vt = (VersionTag) obj;
			return (this.getOrdinalAbs() == vt.getOrdinalAbs() && this.node.equals(vt.node));
		}
		return toString().equals(obj.toString());
	}

	@Override
	public String toString() {
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			return format(node, ordinal, builder).toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
	}

	static public String format(String node, int ordinal) {
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			return format(node, ordinal, builder).toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
	}

	static public StringBuilder format(String node, int ordinal, StringBuilder builder) {
		builder.append(node);
		if (ordinal < 0) {
			// 负数则为删除
			builder.append(VERSION_MARK_REMOVE);
			Hex.toHex(-ordinal, builder);
		} else {
			builder.append(VERSION_MARK_CHANGE);
			Hex.toHex(ordinal, builder);
		}
		return builder;
	}

	public final static VersionTag valueOf(String versionTag) {
		if (null == versionTag || versionTag.length() == 0) {
			return _nil;
		}
		return new VersionTag(versionTag);
	}

	// /**
	// * 比较版本ver1与ver2：相同、高、低、冲突
	// *
	// * @param ver1
	// * 被比较版本
	// * @param ver2
	// * 参照版本
	// * @return ver1==ver2返回VERSION_SAME，ver1>ver2返回VERSION_HIGH，否则VERSION_LOW
	// */
	// public static int compareVersion(String ver1, String ver2) {
	// int ret = compareVersion_(ver1, ver2);
	// if (Storage._TraceEnabled) {
	// Storage._Logger.trace(ver1 + " ∩ " + ver2 + " => " + ret);
	// }
	// return ret;
	// }

	/**
	 * 比较版本ver1与ver2：相同、高、低、冲突
	 * 
	 * @param ver1
	 *            被比较版本
	 * @param ver2
	 *            参照版本
	 * @return ver1==ver2返回VERSION_SAME，ver1&gt;ver2返回VERSION_HIGH，否则VERSION_LOW
	 */
	public static int compareVersion(String ver1, String ver2) {
		if (null == ver1 || 0 == ver1.length()) {
			return (null == ver2 || 0 == ver2.length()) ? VersionTag.VERSION_SAME
					: VersionTag.VERSION_LOW;
		}
		if (null == ver2 || 0 == ver2.length()) {
			return (null == ver1 || 0 == ver1.length()) ? VersionTag.VERSION_SAME
					: VersionTag.VERSION_HIGH;
		}

		// 首先取得最后版本号段
		int idx1 = ver1.lastIndexOf(VersionTag.VERSION_SPEARATOR);
		String last1 = (-1 == idx1) ? ver1
				: ((ver1.length() > (idx1 + 1)) ? ver1.substring(idx1 + 1)
						: ver1.substring(0, idx1));
		int idx2 = ver2.lastIndexOf(VersionTag.VERSION_SPEARATOR);
		String last2 = (-1 == idx2) ? ver2
				: ((ver2.length() > (1 + idx2)) ? ver2.substring(idx2 + 1)
						: ver2.substring(0, idx2));
		if (last1.equals(last2)) {
			// 是相同版本
			return VersionTag.VERSION_SAME;
		}
		VersionTag tag1 = VersionTag.valueOf(last1);
		VersionTag theyTag = VersionTag.valueOf(last2);
		if ((-1 == idx2 || -1 == idx1)) {
			if (tag1.node.equals(theyTag.node)) {
				// 若其中一个版本号只有一个版本标记，且节点ID相同，直接比较序号即可
				int v = tag1.getOrdinalAbs() - theyTag.getOrdinalAbs();
				if (0 == v) {
					return VersionTag.VERSION_SAME;
				}
				return ((v > 0) ? VersionTag.VERSION_HIGH : VersionTag.VERSION_LOW);
			}
			// 不是相同节点ID，版本号只有一个版本标记，比较版本序号
			if (-1 == idx2 && -1 == idx1) {
				if (tag1.getOrdinalAbs() > theyTag.getOrdinalAbs()) {
					return VersionTag.VERSION_HIGH;
				} else if (tag1.getOrdinalAbs() < theyTag.getOrdinalAbs()) {
					return VersionTag.VERSION_LOW;
				}
				// 相同就是冲突啦
				return VersionTag.VERSION_CONFLICT;
			}
		}
		// 有多个，由高向低比较
		if (tag1.getOrdinalAbs() > theyTag.getOrdinalAbs()) {
			// 当前是高版本 Ai1;Be2;Cc3 Ai1;Be2
			int pos = idx1;
			while (pos > 1) {
				idx1 = ver1.lastIndexOf(VersionTag.VERSION_SPEARATOR, pos - 1);
				tag1 = valueOf(ver1.substring(idx1 + 1, pos));
				pos = idx1;
				if (tag1.getOrdinalAbs() < theyTag.getOrdinalAbs()) {
					// 当前版本低，不可能匹配
					// break;
					return VersionTag.VERSION_CONFLICT;
				}
				if (theyTag.getOrdinalAbs() == tag1.getOrdinalAbs()) {
					// 匹配到共同的起点版本
					if (tag1.node.equals(theyTag.node)) {
						// 确实是高版本:)
						return VersionTag.VERSION_HIGH;
					}
					// 但节点不相同，冲突
					return VersionTag.VERSION_CONFLICT;
				}
			}
			// 没有冲突就认为是高版本
			return VersionTag.VERSION_HIGH;
		}

		if (tag1.getOrdinalAbs() < theyTag.getOrdinalAbs()) {
			// 当前是低版本
			int pos = idx2;
			while (pos > 1) {
				idx2 = ver2.lastIndexOf(VersionTag.VERSION_SPEARATOR, pos - 1);
				theyTag = VersionTag.valueOf(ver2.substring(idx2 + 1, pos));
				pos = idx2;
				if (theyTag.getOrdinalAbs() < tag1.getOrdinalAbs()) {
					// 当前版本高，不可能匹配，冲突
					// break;
					return VersionTag.VERSION_CONFLICT;
				}
				if (theyTag.getOrdinalAbs() == tag1.getOrdinalAbs()) {
					// 匹配到共同的起点版本
					if (theyTag.node.equals(tag1.node)) {
						// 确实是低版本:)
						return VersionTag.VERSION_LOW;
					}
					// 但节点不相同，冲突
					return VersionTag.VERSION_CONFLICT;
				}
			}
			// 没有冲突就认为是低版本
			return VersionTag.VERSION_LOW;
		}

		// 相同的版本号
		if (theyTag.node.equals(tag1.node)) {
			// if (!theyTag.node.equals(tag1.node)) {
			// // 不是相同的节点，冲突
			// return VersionTag.VERSION_CONFLICT;
			// }
			// 节点相同，若上个版本（源版本）一致则为同版本
			int pos;
			if (-1 != idx2) {
				pos = idx2;
				idx2 = ver2.lastIndexOf(VersionTag.VERSION_SPEARATOR, pos - 1);
				theyTag = valueOf(ver2.substring(idx2 + 1, pos));
			}
			if (-1 != idx1) {
				pos = idx1;
				idx1 = ver1.lastIndexOf(VersionTag.VERSION_SPEARATOR, pos - 1);
				tag1 = valueOf(ver1.substring(idx1 + 1, pos));
			}
			if (theyTag.equals(tag1)) {
				// 相同的版本号
				return VersionTag.VERSION_SAME;
			}
			// // 不相同则为冲突
			// return VersionTag.VERSION_CONFLICT;
		}
		// 相同的版本号，不同的节点，当然是冲突啦
		return VersionTag.VERSION_CONFLICT;
	}

	/**
	 * 取得版本号串中最后的版本标签
	 * 
	 * @param version
	 *            版本号串
	 * @return 最后的版本标签串
	 */
	public static final String getLastTag(String version) {
		int idx = version.lastIndexOf(VERSION_SPEARATOR);
		return (-1 == idx) ? version : (version.substring(idx + 1));
	}
}

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
package cn.weforward.data.log.label;

import java.util.Date;

import cn.weforward.common.crypto.Hex;
import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.TimeUtil;
import cn.weforward.data.UniteId;
import cn.weforward.data.array.LabelElement;
import cn.weforward.data.log.BusinessLog;
import cn.weforward.data.log.vo.BusinessLogVo;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.exception.ObjectMappingException;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.support.AbstractObjectMapper;
import cn.weforward.protocol.support.datatype.FriendlyObject;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 日志元素
 * 
 * @author daibo
 *
 */
public class LogItem implements LabelElement, BusinessLog {
	/** 原日志对象 */
	protected BusinessLog m_Log;
	/** 所属服务器标识（0x01~0xFF） */
	protected String m_ServerId;
	/** 日志在集合中的序号 */
	protected int m_Ordinal;

	public LogItem(BusinessLog l, String serverId, int ordinal) {
		m_Log = l;
		m_ServerId = serverId;
		m_Ordinal = ordinal;
	}

	/**
	 * 生成日志ID
	 * 
	 * @param timestamp 时间
	 * @param ordinal   序号
	 * @param serverId  服务器ID（0~255）
	 * @return ID字串
	 */
	public static String genId(long timestamp, int ordinal, String serverId) {
		StringBuilder sb = new StringBuilder(1 + 19);
		Hex.toHex(timestamp, sb);
		sb.append(UniteId.PREFIX_SPEARATOR);
		if (ordinal > 0) {
			Hex.toHex(ordinal, sb);
		}
		if (!StringUtil.isEmpty(serverId)) {
			sb.append(UniteId.PREFIX_SPEARATOR);
			sb.append(serverId);
		}
		return sb.toString();
	}

	@Override
	public String getIdForLabel() {
		String id = getLog().getId();
		return StringUtil.isEmpty(id) ? genId(getTime().getTime(), m_Ordinal, m_ServerId) : id;
	}

	public BusinessLog getLog() {
		return m_Log;
	}

	@Override
	public String getId() {
		return getIdForLabel();
	}

	@Override
	public Date getTime() {
		return getLog().getTime();
	}

	@Override
	public String getTarget() {
		return getLog().getTarget();
	}

	@Override
	public String getAuthor() {
		return getLog().getAuthor();
	}

	@Override
	public String getAction() {
		return getLog().getAction();
	}

	@Override
	public String getWhat() {
		return getLog().getWhat();
	}

	@Override
	public String getNote() {
		return getLog().getNote();
	}

	/** 映射表 */
	static final ObjectMapper<LogItem> _MAPPER = new AbstractObjectMapper<LogItem>() {

		@Override
		public String getName() {
			return UniteId.getSimpleName(LogItem.class);
		}

		@Override
		public DtObject toDtObject(LogItem object) throws ObjectMappingException {
			SimpleDtObject o = new SimpleDtObject();
			o.put("serverId", object.m_ServerId);
			o.put("ordinal", object.m_Ordinal);
			BusinessLog l = object.getLog();
			o.put("id", l.getId());
			o.put("time", object.getTime());
			o.put("target", l.getTarget());
			o.put("author", object.getAuthor());
			o.put("action", object.getAction());
			o.put("what", object.getWhat());
			o.put("note", object.getNote());
			return o;
		}

		@Override
		public LogItem fromDtObject(DtObject obj) throws ObjectMappingException {
			FriendlyObject v = FriendlyObject.valueOf(obj);
			Date date = v.getDate("time");
			if (null == date) {
				date = TimeUtil.parseDate(v.getString("time"));
			}
			BusinessLog l = new BusinessLogVo(v.getString("id"), date, v.getString("target"), v.getString("author"),
					v.getString("action"), v.getString("what"), v.getString("note"));
			String serverId = v.getString("serverId");
			int ordinal = v.getInt("ordinal", 0);
			return new LogItem(l, serverId, ordinal);
		}
	};

}

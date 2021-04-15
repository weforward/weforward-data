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
package cn.weforward.data.log.support;

import java.util.Date;

import cn.weforward.common.ResultPage;
import cn.weforward.common.crypto.Hex;
import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.UniteId;
import cn.weforward.data.log.BusinessLog;
import cn.weforward.data.log.BusinessLogger;
import cn.weforward.data.log.vo.BusinessLogVo;

/**
 * 抽象日志记录器实现 日志的ID为：“对象ID_十六进时间戳_服务器标识”，时间戳由0~7位的补充序数及8~59位（自1970后的）毫秒数
 * 
 * @author daibo
 *
 */
public abstract class AbstractBusinessLogger implements BusinessLogger {
	/** 名称 */
	protected String m_Name;
	/** 最后生成的ID时间 */
	protected long m_LastTime;
	/** 最后生成的ID补充序数 */
	protected int m_LastOrdinal;

	public AbstractBusinessLogger(String name) {
		m_Name = name;
	}

	@Override
	public String getName() {
		return m_Name;
	}

	@Override
	public void writeLog(String id, String author, String action, String what) {
		writeLog(id, author, action, what, null);
	}

	@Override
	public void writeLog(String id, String author, String action, String what, String note) {
		long t = nextTimestamp();
		Date time = new Date(t >> 8);
		writeLog(new BusinessLogVo(genId(id, t), time, id, author, action, what, note));
	}

	static public BusinessLogVo createVoById(String id) {
		BusinessLogVo vo = new BusinessLogVo();
		vo.setId(id);
		int idxTimestamp = id.lastIndexOf(UniteId.PREFIX_SPEARATOR);
		int idxTarget;
		idxTarget = id.lastIndexOf(UniteId.PREFIX_SPEARATOR, idxTimestamp - 1);
		vo.setTarget(id.substring(0, idxTarget));
		++idxTarget;
		if (idxTimestamp < idxTarget) {
			idxTimestamp = id.length();
			// } else {
			// vo.setServerId(id.substring(idxTimestamp + 1));
		}
		long ts = Long.parseLong(id.substring(idxTarget, idxTimestamp), 16);
		ts >>= 8;
		vo.setTime(new Date(ts));
		return vo;
	}

	public String genId(String objectId, long timestamp) {
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			builder.append(objectId).append(UniteId.PREFIX_SPEARATOR);
			Hex.toHexFixed((Long.MAX_VALUE & timestamp), builder);
			String serverId = getServerId();
			if (!StringUtil.isEmpty(serverId)) {
				builder.append(UniteId.PREFIX_SPEARATOR).append(serverId);
			}
			return builder.toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
	}

	public String toId(String objectId, long time) {
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			builder.append(objectId).append(UniteId.PREFIX_SPEARATOR);
			Hex.toHexFixed(time << 8, builder);
			return builder.toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
	}

	synchronized public long nextTimestamp() {
		long time = System.currentTimeMillis();
		if (time > m_LastTime) {
			m_LastTime = time;
			m_LastOrdinal = 0;
			time <<= 8;
		} else {
			++m_LastOrdinal;
			time = time << 8 | m_LastOrdinal;
		}
		return time;
	}

	public abstract String getServerId();

	/**
	 * 写日志
	 * 
	 * @param log 日志
	 */
	protected abstract void writeLog(BusinessLog log);

	@Override
	public ResultPage<BusinessLog> getLogs(String target) {
		return searchLogs(target, null, null);
	}

}

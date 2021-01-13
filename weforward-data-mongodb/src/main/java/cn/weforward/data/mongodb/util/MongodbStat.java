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

import java.io.File;
import java.io.IOException;
import java.net.UnknownServiceException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import cn.weforward.common.io.CachedInputStream;
import cn.weforward.common.json.JsonObject;
import cn.weforward.common.json.JsonPair;
import cn.weforward.common.json.JsonUtil;
import cn.weforward.common.json.StringInput;
import cn.weforward.common.util.NumberUtil;
import cn.weforward.common.util.StringUtil;

/**
 * mongodb统计
 * 
 * @author daibo
 *
 */
public class MongodbStat {
	/** mongo目录 */
	protected String m_Home;
	/** mongo主机 */
	protected String m_Host;
	/** mongo端口 */
	protected int m_Port;
	/** 用户名 */
	protected String m_Username;
	/** 密码 */
	protected String m_Password;
	/** 编码集 */
	protected Charset m_Charset = Charset.forName("utf-8");

	public MongodbStat(String home, String host, int port) {
		this(home, host, port, null, null);
	}

	public MongodbStat(String home, String host, int port, String username, String password) {
		if (StringUtil.isEmpty(home)) {
			throw new NullPointerException("home不能为空");
		}
		if (!home.endsWith(File.separator)) {
			m_Home = home + File.separator;
		} else {
			m_Home = home;
		}
		m_Host = host;
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("端口范围必须在1~65535之间");
		}
		m_Port = port;
		m_Username = username;
		m_Password = password;
	}

	/** @return mongo主机 */
	public String getHost() {
		return m_Host;
	}

	/** @return mongo端口 */
	public int getPort() {
		return m_Port;
	}

	/**
	 * 统计
	 * 
	 * @return 统计结果
	 * @throws IOException IO异常
	 */
	public Stat stat() throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(m_Home);
		sb.append("bin/mongostat --json");
		if (!StringUtil.isEmpty(m_Host)) {
			sb.append(" --host ").append(m_Host);
		}
		if (m_Port > 1 && m_Port < 65535) {
			sb.append(" --port ").append(m_Port);
		}
		if (!StringUtil.isEmpty(m_Username)) {
			sb.append(" -u ").append(m_Username);
		}
		if (!StringUtil.isEmpty(m_Password)) {
			sb.append(" -p ").append(m_Password);
		}
		if (!StringUtil.isEmpty(m_Username) || !StringUtil.isEmpty(m_Password)) {
			sb.append(" --authenticationDatabase=admin");
		}
		sb.append(" -n 1");
		String cmd = sb.toString();
		String result = exec(cmd);
		JsonObject m = (JsonObject) JsonUtil.parse(new StringInput(result), null);
		for (JsonPair pair : m.items()) {
			return new Stat(pair.getKey(), (JsonObject) pair.getValue());
		}
		return null;

	}

	/**
	 * 资源
	 * 
	 * @return 资源列表
	 * @throws IOException IO异常
	 */
	public List<Top> top() throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(m_Home);
		sb.append("bin/mongotop ");
		sb.append("--json");
		if (!StringUtil.isEmpty(m_Host)) {
			sb.append(" --host ").append(m_Host);
		}
		if (m_Port > 1 && m_Port < 65535) {
			sb.append(" --port ").append(m_Port);
		}
		if (!StringUtil.isEmpty(m_Username)) {
			sb.append(" -u ").append(m_Username);
		}
		if (!StringUtil.isEmpty(m_Password)) {
			sb.append(" -p ").append(m_Password);
		}
		if (!StringUtil.isEmpty(m_Username) || !StringUtil.isEmpty(m_Password)) {
			sb.append(" --authenticationDatabase=admin");
		}
		sb.append(" -n 1");
		String cmd = sb.toString();
		String result = exec(cmd);
		List<Top> tops = new ArrayList<>();
		JsonObject json = (JsonObject) JsonUtil.parse(new StringInput(result), null);
		for (JsonPair pair : json.items()) {
			if (StringUtil.eq(pair.getKey(), "totals")) {
				JsonObject totals = (JsonObject) pair.getValue();
				for (JsonPair p : totals.items()) {
					tops.add(new Top(p.getKey(), (JsonObject) p.getValue()));
				}
				break;
			}
		}
		return tops;
	}

	/* 执行方式 */
	private String exec(String cmd) throws IOException {
		Process proecess = null;
		try {
			proecess = Runtime.getRuntime().exec(cmd);
			String result = CachedInputStream.readString(proecess.getInputStream(), 1024, m_Charset.name());
			String err = CachedInputStream.readString(proecess.getErrorStream(), 1024, m_Charset.name());
			if (!StringUtil.isEmpty(err)) {
				if (err.contains("no reachable servers")) {
					throw new UnknownServiceException(err);
				}
				throw new IOException(err);
			}
			return result;
		} finally {
			if (null != proecess) {
				proecess.destroy();
			}
		}
	}

	/**
	 * 统计
	 * 
	 * @author daibo
	 *
	 */
	public static class Stat {
		@Resource
		private String host;
		@Resource
		private String insert;
		@Resource
		private String query;
		@Resource
		private String update;
		@Resource
		private String delete;
		@Resource
		private String getmore;
		@Resource
		private String command;
		@Resource
		private String dirty;
		@Resource
		private String used;
		@Resource
		private String flushes;
		@Resource
		private String vsize;
		@Resource
		private String res;
		@Resource
		private String qrw;
		@Resource
		private String arw;
		@Resource
		private String net_in;
		@Resource
		private String net_out;
		@Resource
		private String conn;
		@Resource
		private String time;

		protected Stat() {

		}

		public Stat(String host, JsonObject json) {
			this.host = host;
			for (JsonPair pair : json.items()) {
				String key = pair.getKey();
				if (StringUtil.eq(key, "insert")) {
					insert = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "query")) {
					query = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "update")) {
					update = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "delete")) {
					delete = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "getmore")) {
					getmore = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "command")) {
					command = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "dirty")) {
					dirty = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "used")) {
					used = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "flushes")) {
					flushes = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "vsize")) {
					vsize = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "res")) {
					res = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "qrw")) {
					qrw = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "arw")) {
					arw = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "net_in")) {
					net_in = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "net_out")) {
					net_out = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "conn")) {
					conn = StringUtil.toString(pair.getValue());
				} else if (StringUtil.eq(key, "time")) {
					time = StringUtil.toString(pair.getValue());
				}
			}
		}

		/** @return 主机 */
		public String getHost() {
			return host;
		}

		/** @return 一秒内的插入数 */
		public String getinsert() {
			return insert;
		}

		/** @return 一秒内的查询数 */
		public String getQuery() {
			return query;
		}

		/** @return 一秒内的更新数 */
		public String getUpdate() {
			return update;
		}

		/** @return 一秒内的删除数 */
		public String getDelete() {
			return delete;
		}

		/** @return 一秒内的getmore(游标批处理)数） */
		public String getGetmore() {
			return getmore;
		}

		/** @return 每秒的命令数，比以上插入、查找、更新、删除的综合还多，还统计了别的命令 */
		public String getCommand() {
			return command;
		}

		/** @return 脏字节比例 */
		public String getDirty() {
			return dirty;
		}

		/** @return 已使用比例 */
		public String getUsed() {
			return used;
		}

		/** @return 一秒内flush的次数 */
		public String getflushes() {
			return flushes;
		}

		/** @return 虚拟内存使用量，单位MB */
		public String getVsize() {
			return vsize;
		}

		/** @return 物理内存使用量，单位MB */
		public String getRes() {
			return res;
		}

		public String getQrw() {
			return qrw;
		}

		public String getArw() {
			return arw;
		}

		/** @return 入网流量 */
		public String getNetIn() {
			return net_in;
		}

		/** @return 出网流量 */
		public String getNetOut() {
			return net_out;
		}

		/** @return 当前连接数 */
		public String getConn() {
			return conn;
		}

		/** @return 时间戳 */
		public String getTime() {
			return time;
		}

		@Override
		public String toString() {
			return host + "|" + insert + "|" + query + "|" + update + "|" + delete + "|" + getmore + "|(" + command
					+ ")|" + dirty + "|" + used + "|" + flushes + "|" + vsize + "|" + res + "|(" + qrw + ")|(" + arw
					+ ")|" + net_in + "|" + net_out + "|" + conn + "|" + time;
		}
	}

	/**
	 * 资源
	 * 
	 * @author daibo
	 *
	 */
	public static class Top {
		@Resource
		protected String name;
		@Resource
		protected TimeAndCount total;
		@Resource
		protected TimeAndCount write;
		@Resource
		protected TimeAndCount read;

		protected Top() {

		}

		public Top(String name, JsonObject object) {
			this.name = name;
			for (JsonPair pair : object.items()) {
				String key = pair.getKey();
				if (StringUtil.eq(key, "total")) {
					total = new TimeAndCount((JsonObject) pair.getValue());
				} else if (StringUtil.eq(key, "write")) {
					write = new TimeAndCount((JsonObject) pair.getValue());
				} else if (StringUtil.eq(key, "read")) {
					read = new TimeAndCount((JsonObject) pair.getValue());
				}
			}
		}

		public String getName() {
			return name;
		}

		public TimeAndCount getTotal() {
			return total;
		}

		public TimeAndCount getWrite() {
			return write;
		}

		public TimeAndCount getRead() {
			return read;
		}

		@Override
		public String toString() {
			return name + "|" + total + "|" + write + "|" + read;
		}
	}

	public static class TimeAndCount {
		@Resource
		private int time;
		@Resource
		private int count;

		protected TimeAndCount() {

		}

		public TimeAndCount(JsonObject m) {
			for (JsonPair pair : m.items()) {
				String key = pair.getKey();
				if (StringUtil.eq(key, "time")) {
					time = NumberUtil.toInt(StringUtil.toString(pair.getValue()), 0);
				} else if (StringUtil.eq(key, "count")) {
					count = NumberUtil.toInt(StringUtil.toString(pair.getValue()), 0);
				}

			}
		}

		public int getTime() {
			return time;
		}

		public int getCount() {
			return count;
		}

		@Override
		public String toString() {
			return time + "|" + count;
		}
	}

	@Override
	public String toString() {
		return m_Host + ":" + m_Port;
	}

	public static void main(String[] args) {
		String mhome = "/home/daibo/mongo/mongodb/";
		MongodbStat w = new MongodbStat(mhome, args[0], Integer.parseInt(args[1]), args[2], args[3]);
		try {
			String head = "host		|insert|query|update|delete|getmore|command|dirty|used|flushes|vsize|res|qrw|arw|net_in|net_out|conn|time";
			System.out.println(head);
			while (true) {
				System.out.println(w.stat());
				synchronized (w) {
					w.wait(1 * 1000);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

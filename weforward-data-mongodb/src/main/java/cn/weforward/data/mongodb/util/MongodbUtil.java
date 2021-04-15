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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.SocketSettings;

import cn.weforward.common.KvPair;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.UniteId;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtBoolean;
import cn.weforward.protocol.datatype.DtDate;
import cn.weforward.protocol.datatype.DtList;
import cn.weforward.protocol.datatype.DtNumber;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.datatype.DtString;
import cn.weforward.protocol.support.datatype.SimpleDtBoolean;
import cn.weforward.protocol.support.datatype.SimpleDtDate;
import cn.weforward.protocol.support.datatype.SimpleDtList;
import cn.weforward.protocol.support.datatype.SimpleDtNumber;
import cn.weforward.protocol.support.datatype.SimpleDtObject;
import cn.weforward.protocol.support.datatype.SimpleDtString;

/**
 * 工具类
 * 
 * @author daibo
 *
 */
public class MongodbUtil {
	/** 链接超时时间 */
	public final static int MONGOCLIENT_DEFAULT_CONNECTTIMEOUT_MS = 1 * 1000;
	/** 读取超时时间 */
	public final static int MONGOCLIENT_DEFAULT_READTIMEOUT_MS = 60 * 1000;
	/** 服务超时时间 */
	public final static int MONGOCLIENT_DEFAULT_SERVERSELECTIONTIMEOUT_MS = 3 * 1000;
	/** 池连接的最大空闲时间 */
	protected static final long MONGOCLIENT_DEFAULT_MAXCONNECTIONIDLETIME = 6 * 60 * 1000;
	/** 在Document中的主键（ObjectId） */
	public final static String ID = "_id";

	private MongodbUtil() {

	}

	/**
	 * 创建客户端
	 * 
	 * @param connectionString
	 * 
	 *                         链接 格式
	 *                         mongodb+srv://[username:password@]host[/[database][?options]]
	 *                         如
	 *                         mongodb://admin:123@127.0.0.1:21000/?serverselectiontimeoutms=3000
	 * 
	 *                         options说明
	 *                         <p>
	 *                         serverselectiontimeoutms:服务选择超时毫秒数，当集群没有可用的链接时客户端等待超时的时间，默认{@link #MONGOCLIENT_DEFAULT_SERVERSELECTIONTIMEOUT_MS}
	 *                         <p>
	 *                         connecttimeoutms:与mongodb服务器网络交互的链接超时毫秒数，默认{@link #MONGOCLIENT_DEFAULT_CONNECTTIMEOUT_MS}
	 *                         <p>
	 *                         sockettimeoutms:与mongodb服务器网络交互的读取超时毫秒数，默认{@link #MONGOCLIENT_DEFAULT_READTIMEOUT_MS}
	 *                         <p>
	 *                         minPoolSize:链接池最小数，默认0
	 *                         <p>
	 *                         maxpoolsize:链接池最大数，默认100
	 *                         <p>
	 *                         waitqueuemultiple:限制链接池等待队列个数，默认5，等待链接池的大小等于waitqueuemultiple*maxpoolsize
	 *                         <p>
	 *                         waitqueuetimeoutms:当链接池满后等待链接队列的超时时间，默认2 * 60 * 1000
	 * @return mongo客户端
	 */
	public static MongoClient create(String connectionString) {
		MongoClientSettings.Builder settionsBuilder = MongoClientSettings.builder();
		settionsBuilder.applyToSocketSettings(new Block<SocketSettings.Builder>() {

			@Override
			public void apply(SocketSettings.Builder t) {
				SocketSettings socketSettings = SocketSettings.builder()
						.connectTimeout(MONGOCLIENT_DEFAULT_CONNECTTIMEOUT_MS, TimeUnit.MILLISECONDS)
						.readTimeout(MONGOCLIENT_DEFAULT_READTIMEOUT_MS, TimeUnit.MILLISECONDS).build();
				t.applySettings(socketSettings);

			}
		});
		// settionsBuilder.applyToConnectionPoolSettings(new
		// Block<ConnectionPoolSettings.Builder>() {
		//
		// @Override
		// public void apply(ConnectionPoolSettings.Builder t) {
		// }
		// });
		settionsBuilder.applyToClusterSettings(new Block<ClusterSettings.Builder>() {

			@Override
			public void apply(ClusterSettings.Builder t) {
				ClusterSettings clusterSettings = ClusterSettings.builder()
						.serverSelectionTimeout(MONGOCLIENT_DEFAULT_SERVERSELECTIONTIMEOUT_MS, TimeUnit.MILLISECONDS)
						.build();
				t.applySettings(clusterSettings);

			}
		});
		settionsBuilder.applyToConnectionPoolSettings(new Block<ConnectionPoolSettings.Builder>() {

			@Override
			public void apply(ConnectionPoolSettings.Builder t) {
				ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings.builder()
						.maxConnectionIdleTime(MONGOCLIENT_DEFAULT_MAXCONNECTIONIDLETIME, TimeUnit.MILLISECONDS)
						.build();
				t.applySettings(connectionPoolSettings);
			}
		});
		// settionsBuilder.applyToServerSettings(new
		// Block<ServerSettings.Builder>() {
		//
		// @Override
		// public void apply(ServerSettings.Builder t) {
		//
		// }
		// });
		if (!StringUtil.isEmpty(connectionString)) {
			settionsBuilder.applyConnectionString(new ConnectionString(connectionString));
		}
		return MongoClients.create(settionsBuilder.build());
	}

	/**
	 * 是否重复主键异常
	 * 
	 * @param e mongo异常
	 * @return 是否重复主键异常
	 */
	public static boolean isDuplicateKeyError(MongoException e) {
		// com.mongodb.MongoWriteException: E11000 duplicate key error
		// collection:
		return e.getMessage().contains("duplicate key error");
	}

	/**
	 * dt数据转换成doc文档
	 * 
	 * @param doc 文档对象
	 * @param dt  数据对象
	 * @return 文档对象
	 */
	public static Document dtToDoc(Document doc, DtObject dt) {
		if (null == doc) {
			doc = new Document();
		}
		if (null == dt) {
			return doc;
		}
		Enumeration<KvPair<String, DtBase>> it = dt.getAttributes();
		while (it.hasMoreElements()) {
			KvPair<String, DtBase> pair = it.nextElement();
			String key = pair.getKey();
			DtBase value = pair.getValue();
			if (null == value) {
				continue;
			}
			doc.put(key, change(value));
		}
		return doc;
	}

	/**
	 * doc文档转换成dt数据
	 * 
	 * @param doc 文档对象
	 * @param dt  数据对象
	 * @return 文档对象
	 */
	public static SimpleDtObject docToDt(SimpleDtObject dt, Document doc) {
		if (null == dt) {
			dt = new SimpleDtObject();
		}
		if (null == doc) {
			return dt;
		}
		Set<Entry<String, Object>> es = doc.entrySet();
		for (Entry<String, Object> e : es) {
			dt.put(e.getKey(), change(e.getValue()));
		}
		return dt;
	}

	/**
	 * object转成dtbast
	 * 
	 * @param value 对象
	 * @return 数据对象
	 */
	public static DtBase change(Object value) {
		if (null == value) {
			return null;
		}
		if (value.getClass() == boolean.class || value instanceof Boolean) {
			return new SimpleDtBoolean((Boolean) value);
		} else if (value.getClass() == int.class || value instanceof Integer) {
			return new SimpleDtNumber((Integer) value);
		} else if (value.getClass() == long.class || value instanceof Long) {
			return new SimpleDtNumber((Long) value);
		} else if (value.getClass() == double.class || value instanceof Double) {
			return new SimpleDtNumber((Double) value);
		} else if (value instanceof Date) {
			return new SimpleDtDate((Date) value);
		} else if (value instanceof String) {
			return new SimpleDtString((String) value);
		} else if (value instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) value;
			List<DtBase> dtlist = new ArrayList<>(list.size());
			for (Object o : list) {
				dtlist.add(change(o));
			}
			return SimpleDtList.valueOf(dtlist);
		} else if (value.getClass().isArray()) {
			int l = Array.getLength(value);
			List<DtBase> dtlist = new ArrayList<>(l);
			for (int i = 0; i < l; i++) {
				dtlist.add(change(Array.get(value, i)));
			}
			return SimpleDtList.valueOf(dtlist);
		} else if (value instanceof Document) {
			return docToDt(null, (Document) value);
		} else {
			throw new UnsupportedOperationException("不支持的类型:" + UniteId.getSimpleName(value.getClass()));
		}
	}

	/**
	 * dtbase转换成object
	 * 
	 * @param value 数据对象
	 * @return 对象
	 */
	public static Object change(DtBase value) {
		if (null == value) {
			return null;
		}
		if (value instanceof DtBoolean) {
			return ((DtBoolean) value).value();
		} else if (value instanceof DtNumber) {
			DtNumber n = (DtNumber) value;
			if (n.isInt()) {
				return n.valueInt();
			} else if (n.isLong()) {
				return n.valueLong();
			} else if (n.isDouble()) {
				return n.valueDouble();
			} else {
				return n.valueDouble();
			}
		} else if (value instanceof DtDate) {
			return ((DtDate) value).value();
		} else if (value instanceof DtString) {
			return ((DtString) value).value();
		} else if (value instanceof DtList) {
			List<Object> list = new ArrayList<>();
			DtList dtlist = (DtList) value;
			for (int i = 0; i < dtlist.size(); i++) {
				list.add(change(dtlist.getItem(i)));
			}
			return list;
		} else if (value instanceof DtObject) {
			DtObject dt = (DtObject) value;
			return dtToDoc(null, dt);
		} else {
			throw new UnsupportedOperationException("不支持的类型:" + UniteId.getSimpleName(value.getClass()));
		}
	}

}

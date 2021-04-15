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
package cn.weforward.data.mongodb.array;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import cn.weforward.common.GcCleanable;
import cn.weforward.common.ResultPage;
import cn.weforward.common.util.LruCache;
import cn.weforward.common.util.ResultPageHelper;
import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.TransResultPage;
import cn.weforward.data.array.Label;
import cn.weforward.data.array.LabelElement;
import cn.weforward.data.array.support.AbstractLabelSet;
import cn.weforward.data.mongodb.util.MongodbResultPage;
import cn.weforward.data.mongodb.util.MongodbUtil;
import cn.weforward.protocol.ext.ObjectMapper;

/**
 * 基于mongodb的label集合
 * 
 * @author daibo
 *
 */
public class MongodbLabelSet<E extends LabelElement> extends AbstractLabelSet<E> implements GcCleanable {
	/** 数据库 */
	protected MongoDatabase m_Db;
	/** 服务器id */
	protected String m_Serverid;
	/** 数据项 */
	protected LruCache<String, MongodbLabel<E>> m_Items;
	/** 映射表 */
	final ObjectMapper<E> m_Mapper;
	/** hash集合的大小 */
	final int m_HashSize;

	public MongodbLabelSet(MongoDatabase db, ObjectMapper<E> mapper, String serverid, int hashSize) {
		super(db.getName());
		m_Db = db;
		m_Serverid = serverid;
		m_Mapper = mapper;
		m_HashSize = hashSize;
		m_Items = new LruCache<String, MongodbLabel<E>>(getName() + "-cache");
	}

	private MongoCollection<Document> getLabelCollection() {
		if (m_HashSize > 0) {
			return m_Db.getCollection("__labelname");
		} else {
			return null;
		}
	}

	@Override
	public ResultPage<Label<E>> startsWith(String prefix) {
		ResultPage<String> namerp = getLabelNames(prefix);
		return new TransResultPage<Label<E>, String>(namerp) {

			@Override
			protected Label<E> trans(String src) {
				return openLabel(src);
			}
		};
	}

	private ResultPage<String> getLabelNames(String prefix) {
		prefix = fixLabel(prefix);
		ResultPage<String> namerp;
		MongoCollection<Document> collections = getLabelCollection();
		if (null != collections) {
			Bson filter = StringUtil.isEmpty(prefix) ? null
					: MongodbLabel.toBson(prefix, prefix + StringUtil.UNICODE_REPLACEMENT_CHAR);
			namerp = new MongodbResultPage<String>(collections, filter) {

				@Override
				protected String to(Document doc) {
					return doc.getString(MongodbLabel.ID);
				}
			};
		} else {
			List<String> names = new ArrayList<>();
			if (StringUtil.isEmpty(prefix)) {
				for (String v : m_Db.listCollectionNames()) {
					names.add(v);
				}
			} else {
				for (String v : m_Db.listCollectionNames()) {
					if (v.startsWith(prefix)) {
						names.add(v);
					}
				}
			}
			namerp = ResultPageHelper.toResultPage(names);
		}
		return namerp;
	}

	@Override
	public ResultPage<Label<E>> searchRange(String first, String last) {
		List<String> names = new ArrayList<>();
		first = null == first ? "" : fixLabel(first);
		last = null == last ? "" : fixLabel(last);
		MongoCollection<Document> collections = getLabelCollection();
		if (null != collections) {
			FindIterable<Document> it;
			if (null == first && null == last) {
				it = collections.find();
			} else {
				Bson filter = MongodbLabel.toBson(first, last);
				it = collections.find(filter);
			}
			for (Document doc : it) {
				names.add(doc.getString(MongodbLabel.ID));
			}
		} else {
			if (null == first && null == last) {
				for (String v : m_Db.listCollectionNames()) {
					names.add(v);
				}
			} else {
				for (String v : m_Db.listCollectionNames()) {
					if (v.compareTo(first) >= 0 && v.compareTo(last) <= 0) {
						names.add(v);
					}
				}
			}
		}
		ResultPage<String> rp = ResultPageHelper.toResultPage(names);
		return new TransResultPage<Label<E>, String>(rp) {

			@Override
			protected Label<E> trans(String src) {
				return getLabel(src);
			}
		};
	}

	@Override
	public Label<E> getLabel(String label) {
		label = fixLabel(label);
		Label<E> labels = m_Items.get(label);
		if (null == labels) {
			for (String v : ResultPageHelper.toForeach(getLabelNames(label))) {
				if (StringUtil.eq(v, label)) {
					labels = openLabel(label);
					break;
				}
			}
		}
		return labels;
	}

	@Override
	public boolean remove(String label) {
		label = fixLabel(label);
		MongodbLabel<E> l = (MongodbLabel<E>) getLabel(label);
		if (null == l) {
			return false;
		}
		l.removeAll();
		m_Items.remove(label);
		MongoCollection<Document> c = getLabelCollection();
		if (null != c) {
			c.deleteOne(Filters.eq(MongodbLabel.ID, label));
		}
		return true;
	}

	@Override
	public void removeAll() {
		m_Db.drop();
		m_Items.clear();
	}

	@Override
	public Label<E> openLabel(String label) {
		label = fixLabel(label);
		MongodbLabel<E> labels = m_Items.get(label);
		if (null == labels) {
			labels = new MongodbLabel<E>(m_Db, m_Mapper, label, m_Serverid, m_HashSize);
			MongoCollection<Document> c = getLabelCollection();
			if (null != c) {
				Document document = new Document();
				document.put(MongodbLabel.ID, label);
				try {
					c.insertOne(document);
				} catch (MongoWriteException e) {
					if (MongodbUtil.isDuplicateKeyError(e)) {
						// 忽略重复异常
					} else {
						throw e;
					}
				}
			}
			MongodbLabel<E> old = m_Items.putIfAbsent(label, labels);
			if (null != old) {
				labels = old;// 被人抢先了，用别人的
			}
		}
		return labels;
	}

	/* 修复标签名称 */
	private static String fixLabel(String name) {
		return StringUtil.isEmpty(name) ? "" : name.toLowerCase().replaceAll("\\$", "_");
	}

	@Override
	public String toString() {
		return "mongod:" + m_Db.getName();
	}

	@Override
	public void onGcCleanup(int policy) {
		m_Items.onGcCleanup(policy);
	}

}

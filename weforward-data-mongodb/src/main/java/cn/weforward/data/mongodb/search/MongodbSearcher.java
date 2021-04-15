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
package cn.weforward.data.mongodb.search;

import java.util.ArrayList;
import java.util.List;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import cn.weforward.common.util.ListUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.mongodb.persister.MongodbPersister;
import cn.weforward.data.search.IndexAttribute;
import cn.weforward.data.search.IndexElement;
import cn.weforward.data.search.IndexKeyword;
import cn.weforward.data.search.IndexRange;
import cn.weforward.data.search.IndexResults;
import cn.weforward.data.search.SearchOption;
import cn.weforward.data.search.support.AbstractSearcher;
import cn.weforward.data.search.util.IndexResultsHelper;

/**
 * 基于mongodb的搜索器
 * 
 * @author daibo
 *
 */
public class MongodbSearcher extends AbstractSearcher {
	/** 日志 */
	final static Logger _Logger = LoggerFactory.getLogger(MongodbSearcher.class);
	/** 替换条件 */
	private static final ReplaceOptions REPLACE_OPTIONS = new ReplaceOptions().upsert(true);
	/** id属性 */
	final static String ID = MongodbPersister.ID;
	/** 关键字属性 */
	final static String KEYWROD = "k";
	/** 索引项标题 */
	final static String CAPTION = "c";
	/** 索引项摘要 */
	final static String SUMMARY = "s";
	/** 属性（主要用于排序） */
	final static String ATTRIBUTES = "a";
	/** 关键字属性-值 */
	final static String KEYWROD_VALUE = "v";
	/** 关键字属性-匹配率 */
	final static String KEYWROD_RATE = "r";
	/** 关键字属性-值属性名 */
	final static String KEYWORD_VALUE_INDEX = KEYWROD + "." + KEYWROD_VALUE;
	/** 关键字属性-匹配率属性名 */
	final static String KEYWORD_VALUE_RATE = KEYWROD + "." + KEYWROD_RATE;
	/** 服务器id */
	final static String SERVERID = "_serverid";
	/** 数据库 */
	final MongoDatabase m_Db;
	/** 服务器id */
	protected String m_Serverid;
	/** 链接 */
	private MongoCollection<Document> m_Collection;

	public MongodbSearcher(MongoDatabase db, String name, String serverid) {
		super(name);
		m_Db = db;
		m_Serverid = serverid;
	}

	private String getCollectionName() {
		String name = getName();
		if (name.endsWith("_doc")) {
			return name.toLowerCase();
		}
		return name.toLowerCase() + "_doc";
	}

	@Override
	public void updateElement(IndexElement element, List<? extends IndexKeyword> keywords) {
		if (null == keywords || keywords.size() == 0) {
			removeElement(element.getKey());
			return;
		}
		Document doc = toDoc(element);
		List<Document> ks = toDocs(keywords);
		doc.put(KEYWROD, ks);
		doUpdate(element.getKey(), doc);
	}

	@Override
	public boolean removeElement(String elementKey) {
		MongoCollection<Document> c = getCollection();
		Bson filter = Filters.eq(ID, elementKey);
		DeleteResult r = c.deleteOne(filter);
		return r.getDeletedCount() > 0;
	}

	@Override
	public IndexResults searchAll(List<? extends IndexRange> andRanges, List<? extends IndexRange> orRanges,
			List<? extends IndexKeyword> andKeywords, List<? extends IndexKeyword> orKeywords, SearchOption options) {
		List<Bson> and = new ArrayList<>();
		if (!ListUtil.isEmpty(andRanges)) {
			for (IndexRange r : andRanges) {
				and.add(toBson(r.getBegin(), r.getEnd()));
			}
		}
		if (!ListUtil.isEmpty(andKeywords)) {
			and.addAll(toBson(andKeywords));
		}
		List<Bson> or = new ArrayList<>();
		if (!ListUtil.isEmpty(orRanges)) {
			for (IndexRange r : orRanges) {
				or.add(toBson(r.getBegin(), r.getEnd()));
			}
		}
		if (!ListUtil.isEmpty(orKeywords)) {
			or.addAll(toBson(orKeywords));
		}
		if (ListUtil.isEmpty(and) && ListUtil.isEmpty(or)) {
			return IndexResultsHelper.empty();
		} else if (ListUtil.isEmpty(and)) {
			return new MongodbIndexResults(getCollection(), Filters.or(or), options);
		} else if (ListUtil.isEmpty(or)) {
			return new MongodbIndexResults(getCollection(), Filters.and(and), options);
		} else {
			and.add(Filters.or(or));
			return new MongodbIndexResults(getCollection(), Filters.and(and), options);
		}
	}

	private MongoCollection<Document> getCollection() {
		if (null == m_Collection) {
			synchronized (this) {
				if (null == m_Collection) {
					MongoCollection<Document> c = m_Db.getCollection(getCollectionName());
					MongoCursor<Document> it = c.listIndexes().iterator();
					String kvalue = KEYWORD_VALUE_INDEX;
					boolean hasKVaIndex = false;
					while (it.hasNext()) {
						Document doc = it.next();
						if (StringUtil.eq(doc.getString("name"), kvalue)) {
							hasKVaIndex = true;
						}
					}
					if (!hasKVaIndex) {
						IndexOptions options = new IndexOptions();
						options.name(kvalue);
						c.createIndex(Filters.eq(kvalue, 1), options);
					}
					m_Collection = c;
				}
			}
		}
		return m_Collection;
	}

	private synchronized void doUpdate(String id, Document doc) {
		MongoCollection<Document> c = getCollection();
		Bson filter = Filters.eq(ID, id);
		UpdateResult result = c.replaceOne(filter, doc, REPLACE_OPTIONS);
		if (_Logger.isDebugEnabled()) {
			_Logger.debug("matchedCount:" + result.getMatchedCount());
			_Logger.debug("modifiedCount:" + result.getModifiedCount());
			_Logger.debug("upsertedId:" + result.getUpsertedId());
		}
	}

	private Document toDoc(IndexElement element) {
		Document doc = new Document();
		doc.put(ID, element.getKey());
		doc.put(CAPTION, element.getCaption());
		doc.put(SUMMARY, element.getSummary());
		if (!StringUtil.isEmpty(m_Serverid)) {
			doc.put(SERVERID, m_Serverid);
		}
		List<IndexAttribute> attrs = element.getAttributes();
		if (null != attrs) {
			Document adoc = new Document();
			for (IndexAttribute pair : attrs) {
				adoc.put(pair.getKey(), pair.getValue());
			}
			doc.put(ATTRIBUTES, adoc);
		}
		return doc;
	}

	private static List<Bson> toBson(List<? extends IndexKeyword> keywords) {
		List<Bson> filters = new ArrayList<>();
		for (IndexKeyword k : keywords) {
			if (k.getRate() == IndexKeyword.RATE_PREFIX) {
				BsonDocument between = new BsonDocument();
				between.put("$gte", new BsonString(k.getKeyword()));
				between.put("$lt", new BsonString(k.getKeyword() + StringUtil.UNICODE_REPLACEMENT_CHAR));
				BsonDocument v = new BsonDocument();
				v.put(ID, between);
				filters.add(v);
			} else {
				filters.add(Filters.eq(KEYWORD_VALUE_INDEX, k.getKeyword()));
			}
		}
		return filters;
	}

	private static Bson toBson(String begin, String to) {
		BsonDocument doc = new BsonDocument();
		BsonDocument between = new BsonDocument();
		between.put("$gte", new BsonString(begin));
		between.put("$lt", new BsonString(to));
		BsonDocument v = new BsonDocument();
		v.put(KEYWROD_VALUE, between);
		BsonDocument elemMatch = new BsonDocument();
		elemMatch.put("$elemMatch", v);
		doc.put(KEYWROD, elemMatch);
		return doc;
	}

	private static List<Document> toDocs(List<? extends IndexKeyword> keywords) {
		List<Document> list = new ArrayList<>();
		Long r = null;
		for (IndexKeyword k : keywords) {
			if (null == r) {
				r = k.getRate();
			} else if (r != k.getRate()) {
				throw new UnsupportedOperationException("多个关键字匹配率需一致");
			}

			Document doc = new Document();
			doc.put(KEYWROD_VALUE, k.getKeyword());
			doc.put(KEYWROD_RATE, k.getRate());
			list.add(doc);
		}
		return list;
	}

}

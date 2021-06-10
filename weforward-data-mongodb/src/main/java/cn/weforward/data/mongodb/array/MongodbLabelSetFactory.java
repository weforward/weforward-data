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

import com.mongodb.client.MongoClient;

import cn.weforward.common.GcCleanable;
import cn.weforward.common.sys.GcCleaner;
import cn.weforward.common.util.StringUtil;
import cn.weforward.data.array.LabelElement;
import cn.weforward.data.array.LabelSet;
import cn.weforward.data.array.support.AbstractLabelSetFactory;
import cn.weforward.data.mongodb.util.MongodbUtil;
import cn.weforward.protocol.ext.ObjectMapper;

/**
 * 基于mongodb的label集合工厂
 * 
 * @author daibo
 *
 */
public class MongodbLabelSetFactory extends AbstractLabelSetFactory implements GcCleanable {
	/** mongo客户端 */
	protected MongoClient m_Clients;
	/** 服务器id */
	protected String m_ServerId;
	/** hash集合的大小 */
	protected int m_HashSize = 128;
	/** 名称前缀 */
	protected String m_NamePrefix;

	/**
	 * 构造
	 * 
	 * @param url 数据库链接
	 */
	public MongodbLabelSetFactory(String url) {
		m_Clients = MongodbUtil.create(url);
		GcCleaner.register(this);
	}

	/**
	 * 设置服务器id
	 * 
	 * @param sid 服务器id
	 */
	public void setServerId(String sid) {
		m_ServerId = sid;
	}

	/**
	 * 
	 * @param size hash集合的大小
	 */
	public void setHashSize(int size) {
		m_HashSize = size;
	}

	/**
	 * 设置lableset的名称前缀
	 * 
	 * @param prefix 名称前缀
	 */
	public void setNamePrefix(String prefix) {
		m_NamePrefix = prefix;
	}

	private String fixName(String name) {
		name = name.toLowerCase().replace('$', '_');
		if (StringUtil.isEmpty(m_NamePrefix)) {
			return name;
		}
		return m_NamePrefix + name;
	}

	@Override
	protected <E extends LabelElement> LabelSet<E> doCreateLabelSet(String name, ObjectMapper<E> mapper) {
		return new MongodbLabelSet<E>(m_Clients.getDatabase(fixName(name)), mapper, m_ServerId, m_HashSize);
	}

	@Override
	public void onGcCleanup(int policy) {
		for (LabelSet<?> labelset : this) {
			if (labelset instanceof GcCleanable) {
				((GcCleanable) labelset).onGcCleanup(policy);
			}
		}

	}

}

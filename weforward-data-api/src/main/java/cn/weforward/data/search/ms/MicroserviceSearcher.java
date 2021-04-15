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
package cn.weforward.data.search.ms;

import java.util.ArrayList;
import java.util.List;

import cn.weforward.common.util.StringUtil;
import cn.weforward.data.search.IndexAttribute;
import cn.weforward.data.search.IndexElement;
import cn.weforward.data.search.IndexKeyword;
import cn.weforward.data.search.IndexRange;
import cn.weforward.data.search.IndexResults;
import cn.weforward.data.search.SearchOption;
import cn.weforward.data.search.support.AbstractSearcher;
import cn.weforward.data.search.util.IndexResultsHelper;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.client.execption.GatewayException;
import cn.weforward.protocol.client.execption.MicroserviceException;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.support.datatype.SimpleDtList;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 基于微服务的搜索器,暂时只实现了保存功能
 * 
 * @author daibo
 *
 */
public class MicroserviceSearcher extends AbstractSearcher {
	/** id属性 */
	final static String ID = "_id";
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
	/** 服务器id */
	final static String SERVERID = "_serverid";

	protected MicroserviceSearcherFactory m_Factory;

	public MicroserviceSearcher(MicroserviceSearcherFactory factory, String name) {
		super(name);
		m_Factory = factory;
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
		SimpleDtObject content = new SimpleDtObject();
		content.put(ID, element.getKey());
		content.put(CAPTION, element.getCaption());
		content.put(SUMMARY, element.getSummary());
		String serverid = m_Factory.getServiceId();
		if (!StringUtil.isEmpty(serverid)) {
			content.put(SERVERID, serverid);
		}
		List<IndexAttribute> attrs = element.getAttributes();
		if (null != attrs) {
			SimpleDtObject adoc = new SimpleDtObject();
			for (IndexAttribute pair : attrs) {
				adoc.put(pair.getKey(), StringUtil.toString(pair.getValue()));
			}
			content.put(ATTRIBUTES, adoc);
		}
		if (null != keywords) {
			List<DtBase> list = new ArrayList<>();
			for (IndexKeyword k : keywords) {
				SimpleDtObject obj = new SimpleDtObject();
				obj.put(KEYWROD_VALUE, k.getKeyword());
				obj.put(KEYWROD_RATE, k.getRate());
				list.add(obj);
			}
			content.put(KEYWROD, SimpleDtList.valueOf(list));
		}

		ServiceInvoker invoker = m_Factory.getInvoker();
		String method = m_Factory.getMethodGroup() + "save";
		SimpleDtObject params = new SimpleDtObject();
		params.put("dbName", m_Factory.getDbName());
		params.put("collection", getCollectionName());
		params.put("content", content);
		Response response = invoker.invoke(method, params);
		GatewayException.checkException(response);
		DtObject serviceResult = response.getServiceResult();
		MicroserviceException.checkException(serviceResult);
	}

	@Override
	public boolean removeElement(String elementKey) {
		return false;
	}

	@Override
	public IndexResults searchAll(List<? extends IndexRange> andRanges, List<? extends IndexRange> orRanges,
			List<? extends IndexKeyword> andKeywords, List<? extends IndexKeyword> orKeywords, SearchOption options) {
		return IndexResultsHelper.empty();
	}

}

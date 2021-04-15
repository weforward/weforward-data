package cn.weforward.data.mongodb.util;

import java.io.IOException;

import org.bson.Document;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;

import cn.weforward.common.util.StringUtil;
import cn.weforward.data.util.Flushable;
import cn.weforward.data.util.Flusher;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.client.ServiceInvokerFactory;
import cn.weforward.protocol.client.execption.GatewayException;
import cn.weforward.protocol.client.execption.MicroserviceException;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * mongodb数据同步到微服务
 * 
 * @author daibo
 *
 */
public class MongodbSyncToMicroservice extends AbstractMongodbChangeSupport {
	/** 目标数据库 */
	protected String m_TargetDbName;
	/** 调整器 */
	protected ServiceInvoker m_Invoker;
	/** 数据库名 */
	protected String m_DbName;
	/** 方法组 */
	protected String m_MethodGroup = "";

	protected Flusher m_Fusher;

	public MongodbSyncToMicroservice(String url, String dbname, String targetDbName, String preUrl, String accessId,
			String accessKey, String serviceName) {
		super(MongodbUtil.create(url).getDatabase(dbname));
		if (!StringUtil.isEmpty(preUrl) && !StringUtil.isEmpty(accessId) && !StringUtil.isEmpty(accessKey)) {
			m_Invoker = ServiceInvokerFactory.create(serviceName, preUrl, accessId, accessKey);
		}
		m_TargetDbName = targetDbName;
		start();
	}

	public void setFusher(Flusher fusher) {
		m_Fusher = fusher;
	}

	public void setMethodGroup(String v) {
		m_MethodGroup = StringUtil.toString(v);
	}

	public ServiceInvoker getInvoker() {
		return m_Invoker;
	}

	public String getMethodGroup() {
		return m_MethodGroup;
	}

	@Override
	protected void onChange(ChangeStreamDocument<Document> doc) {
		OperationType op = doc.getOperationType();
		if (op == OperationType.INSERT || op == OperationType.UPDATE || op == OperationType.REPLACE
				|| op == OperationType.DELETE) {
			SimpleDtObject params = new SimpleDtObject();
			params.put("dbName", m_TargetDbName);
			params.put("collection", doc.getNamespace().getCollectionName());
			params.put("content", MongodbUtil.docToDt(null, doc.getFullDocument()));
			if (null == m_Fusher) {
				doSave(params);
			} else {
				m_Fusher.flush(new SaveTask(params));
			}
		}
	}

	private class SaveTask implements Flushable {

		SimpleDtObject m_Params;

		public SaveTask(SimpleDtObject params) {
			m_Params = params;
		}

		@Override
		public void flush() throws IOException {
			doSave(m_Params);
		}

	}

	private void doSave(SimpleDtObject params) {
		ServiceInvoker invoker = getInvoker();
		if (null == invoker) {
			return;
		}
		String method = getMethodGroup() + "save";
		Response response = invoker.invoke(method, params);
		GatewayException.checkException(response);
		DtObject serviceResult = response.getServiceResult();
		MicroserviceException.checkException(serviceResult);

	}
}

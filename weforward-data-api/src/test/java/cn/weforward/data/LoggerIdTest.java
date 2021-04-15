package cn.weforward.data;

import org.junit.Test;

import cn.weforward.common.util.TimeUtil;
import cn.weforward.data.log.support.AbstractBusinessLogger;
import cn.weforward.data.log.vo.BusinessLogVo;

public class LoggerIdTest {
	@Test
	public void test() {
		BusinessLogVo vo = AbstractBusinessLogger
				.createVoById("SimpleRunning01762b3cab5900-x00bc_0001772819644f00_x00bc");
		System.out.println(vo.getTarget());
		System.out.println(TimeUtil.formatDateTime(vo.getTime()));
	}
}

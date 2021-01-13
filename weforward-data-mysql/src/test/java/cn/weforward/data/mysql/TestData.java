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
package cn.weforward.data.mysql;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import cn.weforward.data.annotation.Index;
import cn.weforward.data.annotation.ResourceExt;
import cn.weforward.data.persister.support.AbstractPersistent;

public class TestData extends AbstractPersistent<TestDi> {
	@Resource
	protected boolean m_BooleanInit;
	@Resource
	protected boolean m_BooleanTrue;
	@Resource
	protected boolean m_BooleanFalse;
	@Resource
	protected Boolean m_BooleanInitObject;
	@Resource
	protected Boolean m_BooleanTrueObject;
	@Resource
	protected Boolean m_BooleanFalseObject;

	@Resource
	protected byte m_ByteInit;
	@Resource
	protected byte m_ByteMax;
	@Resource
	protected byte m_ByteMin;
	@Resource
	protected byte m_ByteZero;

	@Resource
	protected Byte m_ByteInitObject;
	@Resource
	protected Byte m_ByteMaxObject;
	@Resource
	protected Byte m_ByteMinObject;
	@Resource
	protected Byte m_ByteZeroObject;

	@Resource
	protected short m_ShortInit;
	@Resource
	protected short m_ShortMax;
	@Resource
	protected short m_ShortMin;
	@Resource
	protected short m_ShortZero;

	@Resource
	protected Short m_ShortInitObject;
	@Resource
	protected Short m_ShortMaxObject;
	@Resource
	protected Short m_ShortMinObject;
	@Resource
	protected Short m_ShortZeroObject;

	@Resource
	protected int m_IntInit;
	@Resource
	protected int m_IntMax;
	@Resource
	protected int m_IntMin;
	@Resource
	protected int m_IntZero;

	@Resource
	protected Integer m_IntInitObject;
	@Resource
	protected Integer m_IntMaxObject;
	@Resource
	protected Integer m_IntMinObject;
	@Resource
	protected Integer m_IntZeroObject;

	@Resource
	protected long m_LongInit;
	@Resource
	protected long m_LongMax;
	@Resource
	protected long m_LongMin;
	@Resource
	protected long m_LongZero;

	@Resource
	protected Long m_LongInitObject;
	@Resource
	protected Long m_LongMaxObject;
	@Resource
	protected Long m_LongMinObject;
	@Resource
	protected Long m_LongZeroObject;

	@Resource
	protected float m_FloatInit;
	@Resource
	protected float m_FloatMax;
	@Resource
	protected float m_FloatMin;
	@Resource
	protected float m_FloatZero;

	@Resource
	protected Float m_FloatInitObject;
	@Resource
	protected Float m_FloatMaxObject;
	@Resource
	protected Float m_FloatMinObject;
	@Resource
	protected Float m_FloatZeroObject;

	@Resource
	protected double m_DoubleInit;
	@Resource
	protected double m_DoubleMax;
	@Resource
	protected double m_DoubleMin;
	@Resource
	protected double m_DoubleZero;

	@Resource
	protected Double m_DoubleInitObject;
	@Resource
	protected Double m_DoubleMaxObject;
	@Resource
	protected Double m_DoubleMinObject;
	@Resource
	protected Double m_DoubleZeroObject;
	@Resource
	protected String m_StringInit;
	@Resource
	protected String m_StringEmpty;
	@Resource
	protected String m_StringValue;

	@Resource
	protected Date m_DateInit;
	@Resource
	protected Date m_DateMin;
	@Index
	@Resource
	protected Date m_DateMax;
	@Resource
	protected DataVo m_VoInit;
	@Resource
	protected DataVo m_VoObject;

	@Resource
	protected List<Object> m_ListInit;
	@Resource
	protected List<Object> m_ListEmpty;
	@Resource
	protected List<Integer> m_ListInteger;
	@Resource
	protected List<Double> m_ListDouble;
	@Resource
	protected List<String> m_ListString;
	@ResourceExt(component = DataVo.class)
	protected List<DataVo> m_ListVos;

	protected TestData(TestDi di) {
		super(di);
	}

	public TestData(TestDi di, String value) {
		super(di);
		genPersistenceId();
		init(value);
		persistenceUpdateNow();
	}

	static final Date MAX;
	static {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 9999);
		cal.set(Calendar.MONTH, 11);
		cal.set(Calendar.DATE, 31);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		MAX = cal.getTime();
	}

	private void init(String value) {
		m_BooleanTrue = true;
		m_BooleanFalse = false;

		m_BooleanTrueObject = Boolean.TRUE;
		m_BooleanFalseObject = Boolean.FALSE;

		m_ByteMax = Byte.MAX_VALUE;
		m_ByteMin = Byte.MIN_VALUE;
		m_ByteZero = 0;

		m_ByteMaxObject = Byte.MAX_VALUE;
		m_ByteMinObject = Byte.MIN_VALUE;
		m_ByteZeroObject = new Byte((byte) 0);

		m_ShortMax = Short.MAX_VALUE;
		m_ShortMin = Short.MIN_VALUE;
		m_ShortZero = 0;

		m_ShortMaxObject = Short.MAX_VALUE;
		m_ShortMinObject = Short.MIN_VALUE;
		m_ShortZeroObject = new Short((short) 0);

		m_IntMax = Integer.MAX_VALUE;
		m_IntMin = Integer.MIN_VALUE;
		m_IntZero = 0;

		m_IntMaxObject = Integer.MAX_VALUE;
		m_IntMinObject = Integer.MIN_VALUE;
		m_IntZeroObject = new Integer(0);

		m_LongMax = Long.MAX_VALUE;
		m_LongMin = Long.MIN_VALUE;
		m_LongZero = new Long(0l);

		m_LongMaxObject = Long.MAX_VALUE;
		m_LongMinObject = Long.MIN_VALUE;
		m_LongZeroObject = new Long(0l);

		m_FloatMax = Float.MAX_VALUE;
		m_FloatMin = Float.MIN_VALUE;
		m_FloatZero = 0.0f;

		m_FloatMaxObject = Float.MAX_VALUE;
		m_FloatMinObject = Float.MIN_VALUE;
		m_FloatZeroObject = new Float(0.0f);

		m_DoubleMax = Double.MAX_VALUE;
		m_DoubleMin = Double.MIN_VALUE;
		m_DoubleZero = 0.0;

		m_DoubleMaxObject = Double.MAX_VALUE;
		m_DoubleMinObject = Double.MIN_VALUE;
		m_DoubleZeroObject = new Double(0.0);
		m_StringEmpty = "";
		m_StringValue = value;

		m_DateMin = new Date(0);
		m_DateMax = MAX;

		m_VoObject = new DataVo(value);

		m_ListEmpty = Collections.emptyList();
		m_ListInteger = Arrays.asList(1, 2, 3);
		m_ListDouble = Arrays.asList(1.1, 2.2, 3.3);
		m_ListString = Arrays.asList("Hello", "World");
		m_ListVos = Arrays.asList(new DataVo("Hello"), new DataVo("World"));
	}

}

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
package cn.weforward.data;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;

import cn.weforward.common.util.TimeUtil;
import cn.weforward.data.annotation.ResourceExt;

public class MyData {

	@Resource
	public byte myByte;
	@Resource
	public short myShort;
	@Resource
	public int myInt;
	@Resource
	public long myLong;
	@Resource
	public float myFloat;
	@Resource
	public double myDouble;
	@Resource
	public boolean myBoolean;
	@Resource
	public Byte myByteObject;
	@Resource
	public Short myShortObject;
	@Resource
	public Integer myIntegerObject;
	@Resource
	public Long myLongObject;
	@Resource
	public Float myFloatObject;
	@Resource
	public Double myDoubleObject;
	@Resource
	public Boolean myBooleanObject;
	@Resource
	public String myString;
	@Resource
	public Date myDate;

	@Resource
	public byte[] myBytes;
	@Resource
	public Byte[] myByteObjects;
	@ResourceExt(component = Byte.class)
	public List<Byte> myByteList;

	@Resource
	public short[] myShorts;
	@Resource
	public Short[] myShortObjects;
	@ResourceExt(component = Short.class)
	public List<Short> myShortList;

	@Resource
	public int[] myInts;
	@Resource
	public Integer[] myIntegerObjects;
	@ResourceExt(component = Integer.class)
	public List<Integer> myIntegerList;

	@Resource
	public long[] myLongs;
	@Resource
	public Long[] myLongObjects;
	@ResourceExt(component = Long.class)
	public List<Long> myLongList;

	@Resource
	public float[] myFloats;
	@Resource
	public Float[] myFloatObjects;
	@ResourceExt(component = Float.class)
	public List<Float> myFloatList;

	@Resource
	public double[] myDoubles;
	@Resource
	public Double[] myDoubleObjects;
	@ResourceExt(component = Double.class)
	public List<Double> myDoubleList;

	@Resource
	public boolean[] myBooleans;
	@Resource
	public Boolean[] myBooleanObjects;
	@ResourceExt(component = Boolean.class)
	public List<Boolean> myBooleanList;
	@Resource
	public String[] myStrings;
	@ResourceExt(component = String.class)
	public List<String> myStringList;
	@Resource
	public Date[] myDates;
	@ResourceExt(component = Date.class)
	public ArrayList<Date> myDateList;

	@ResourceExt(components = { String.class, Date.class })
	public Map<String, Date> myMap;

	@Resource
	public String[][] myArrayArray;

	@ResourceExt(components = { List.class, String.class })
	public List<List<String>> myListList;

	@ResourceExt(components = { String.class, List.class, Date.class })
	public Map<String, List<Date>> myMapList;

	@Resource(type = String.class)
	public UniteId myId;
	@Resource
	public BigInteger myBigInteger;
	@Resource
	public BigDecimal myBigDecimal;

	public void init() {
		Random r = new Random();
		myByte = (byte) r.nextInt(100);
		myShort = (short) r.nextInt(200);
		myInt = r.nextInt();
		myLong = r.nextLong();
		myFloat = r.nextFloat();
		myDouble = r.nextDouble();
		myBoolean = r.nextInt() % 2 == 0;
		myByteObject = new Byte((byte) r.nextInt(100));
		myShortObject = new Short((short) r.nextInt(200));
		myIntegerObject = new Integer(r.nextInt());
		myLongObject = new Long(r.nextLong());
		myFloatObject = new Float(r.nextFloat());
		myDoubleObject = new Double(r.nextDouble());
		myBooleanObject = r.nextInt() % 2 == 0;
		myString = "HelloWorld!";
		myDate = new Date();
		myBytes = new byte[] { (byte) r.nextInt(100), (byte) r.nextInt(100), (byte) r.nextInt(100) };
		myByteObjects = new Byte[] { (byte) r.nextInt(100), (byte) r.nextInt(100), (byte) r.nextInt(100) };
		myByteList = Arrays.asList((byte) r.nextInt(100), (byte) r.nextInt(100), (byte) r.nextInt(100));

		myShorts = new short[] { (short) r.nextInt(100), (short) r.nextInt(100), (short) r.nextInt(100) };
		myShortObjects = new Short[] { (short) r.nextInt(100), (short) r.nextInt(100), (short) r.nextInt(100) };
		myShortList = Arrays.asList((short) r.nextInt(100), (short) r.nextInt(100), (short) r.nextInt(100));

		myInts = new int[] { r.nextInt(), r.nextInt(), r.nextInt() };
		myIntegerObjects = new Integer[] { r.nextInt(), r.nextInt(), r.nextInt() };
		myIntegerList = Arrays.asList(r.nextInt(), r.nextInt(), r.nextInt());

		myLongs = new long[] { r.nextLong(), r.nextLong(), r.nextLong() };
		myLongObjects = new Long[] { r.nextLong(), r.nextLong(), r.nextLong() };
		myLongList = Arrays.asList(r.nextLong(), r.nextLong(), r.nextLong());

		myFloats = new float[] { r.nextFloat(), r.nextFloat(), r.nextFloat() };
		myFloatObjects = new Float[] { r.nextFloat(), r.nextFloat(), r.nextFloat() };
		myFloatList = Arrays.asList(r.nextFloat(), r.nextFloat(), r.nextFloat());

		myDoubles = new double[] { r.nextDouble(), r.nextDouble(), r.nextDouble() };
		myDoubleObjects = new Double[] { r.nextDouble(), r.nextDouble(), r.nextDouble() };
		myDoubleList = Arrays.asList(r.nextDouble(), r.nextDouble(), r.nextDouble());

		myBooleans = new boolean[] { r.nextBoolean(), r.nextBoolean(), r.nextBoolean() };
		myBooleanObjects = new Boolean[] { r.nextBoolean(), r.nextBoolean(), r.nextBoolean() };
		myBooleanList = Arrays.asList(r.nextBoolean(), r.nextBoolean(), r.nextBoolean());

		myStrings = new String[] { "Hello", "World", "!" };
		myStringList = Arrays.asList("Hello", "HelloWorld", "HelloWorld!");

		myDates = new Date[] { new Date(System.currentTimeMillis() - r.nextInt(10000)),
				new Date(System.currentTimeMillis() - r.nextInt(10000)) };
		ArrayList<Date> list = new ArrayList<>();
		list.add(new Date(System.currentTimeMillis() - r.nextInt(10000)));
		list.add(new Date(System.currentTimeMillis() - r.nextInt(10000)));
		myDateList = list;

		HashMap<String, Date> map = new HashMap<String, Date>();
		map.put("111", new Date());
		map.put("222", new Date());
		myMap = map;
		myArrayArray = new String[2][3];
		for (int i = 0; i < myArrayArray.length; i++) {
			for (int j = 0; j < myArrayArray[i].length; j++) {
				myArrayArray[i][j] = "HelloWorld!";
			}
		}
		myListList = new ArrayList<>();
		myListList.add(Arrays.asList("111", "222"));
		myListList.add(Arrays.asList("1111", "2222"));

		HashMap<String, List<Date>> maplist = new HashMap<String, List<Date>>();
		maplist.put("111", Arrays.asList(new Date(), new Date()));
		maplist.put("222", Arrays.asList(new Date(), new Date()));
		myMapList = maplist;

		myId = UniteId.valueOf("Order$123");

		myBigInteger = new BigInteger(String.valueOf(r.nextInt()));
		myBigDecimal = new BigDecimal(String.valueOf(r.nextDouble()));
	}

	public void print(PrintStream out) {
		out.println("myByte=" + myByte);
		out.println("myShort=" + myShort);
		out.println("myInt=" + myInt);
		out.println("myLong=" + myLong);
		out.println("myFloat=" + myFloat);
		out.println("myDouble=" + myDouble);
		out.println("myBoolean=" + myBoolean);

		out.println("myByteObject=" + myByteObject);
		out.println("myShortObject=" + myShortObject);
		out.println("myIntegerObject=" + myIntegerObject);
		out.println("myLongObject=" + myLongObject);
		out.println("myFloatObject=" + myFloatObject);
		out.println("myDoubleObject=" + myDoubleObject);
		out.println("myBooleanObject=" + myBooleanObject);
		out.println("myString=" + myString);
		out.println("myDate=" + (null == myDate ? "null" : TimeUtil.formatDateTime(myDate)));

		out.println("myBytes=" + (null == myBytes ? "null" : Arrays.toString(myBytes)));
		out.println("myByteObjects=" + (null == myByteObjects ? "null" : Arrays.toString(myByteObjects)));
		out.println("myByteList=" + (null == myByteList ? "null" : myByteList.toString()));

		out.println("myShorts=" + (null == myShorts ? "null" : Arrays.toString(myShorts)));
		out.println("myShortObjects=" + (null == myShortObjects ? "null" : Arrays.toString(myShortObjects)));
		out.println("myShortList=" + (null == myShortList ? "null" : myShortList.toString()));

		out.println("myInts=" + (null == myInts ? "null" : Arrays.toString(myInts)));
		out.println("myIntegerObjects=" + (null == myIntegerObjects ? "null" : Arrays.toString(myIntegerObjects)));
		out.println("myIntegerList=" + (null == myIntegerList ? "null" : myIntegerList.toString()));

		out.println("myLongs=" + (null == myLongs ? "null" : Arrays.toString(myLongs)));
		out.println("myLongObjects=" + (null == myLongObjects ? "null" : Arrays.toString(myLongObjects)));
		out.println("myLongList=" + (null == myLongList ? "null" : myLongList.toString()));

		out.println("myFloats=" + (null == myFloats ? "null" : Arrays.toString(myFloats)));
		out.println("myFloatObjects=" + (null == myFloatObjects ? "null" : Arrays.toString(myFloatObjects)));
		out.println("myFloatList=" + (null == myFloatList ? "null" : myFloatList.toString()));

		out.println("myDoubles=" + (null == myDoubles ? "null" : Arrays.toString(myDoubles)));
		out.println("myDoubleObjects=" + (null == myDoubleObjects ? "null" : Arrays.toString(myDoubleObjects)));
		out.println("myDoubleList=" + (null == myDoubleList ? "null" : myDoubleList.toString()));

		out.println("myBooleans=" + (null == myBooleans ? "null" : Arrays.toString(myBooleans)));
		out.println("myBooleanObjects=" + (null == myBooleanObjects ? "null" : Arrays.toString(myBooleanObjects)));
		out.println("myBooleanList=" + (null == myBooleanList ? "null" : myBooleanList.toString()));

		out.println("myStrings=" + (null == myStrings ? "null" : Arrays.toString(myStrings)));
		out.println("myStringList=" + (null == myStringList ? "null" : myStringList.toString()));

		out.println("myDates=" + (null == myDates ? "null" : Arrays.toString(myDates)));
		out.println("myDateList=" + (null == myDateList ? "null" : myDateList.toString()));

		out.println("myBigInteger=" + (null == myBigInteger ? "null" : myBigInteger.toString()));
		out.println("myBigDecimal=" + (null == myBigDecimal ? "null" : myBigDecimal.toString()));
	}

}

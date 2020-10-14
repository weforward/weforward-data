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
package cn.weforward.data.persister;

import java.util.List;

/**
 * 搜索条件
 * 
 * @author daibo
 *
 */
public interface Condition {
	/** 子属性分隔号 */
	char FIELD_SPEARATOR = '.';
	/** 特殊属性名-id */
	String ID = "_id";
	/** 特殊属性名-最后修改时间 */
	String LASTMODIFIED = "_lastmodified";
	/** 特殊属性名-版本 */
	String VERSION = "_version";
	/** 特殊属性名-服务器id */
	String SERVERID = "_serverid";
	/** 特殊属性名-控制实例id */
	String DRIVEIT = "_driveit";
	/** 相等 = */
	short TYPE_EQ = 1;
	/** 不相等 != */
	short TYPE_NE = 2;
	/** 小于 &lt; */
	short TYPE_LT = 3;
	/** 大于 &gt; */
	short TYPE_GT = 4;
	/** 小于等于 &lt;= */
	short TYPE_LTE = 5;
	/** 大于等于 &gt;= */
	short TYPE_GTE = 6;

	/** &amp; 与条件 (组合条件) */
	short TYPE_AND = 1000;
	/** | 或条件 (组合条件) */
	short TYPE_OR = 2000;

	/**
	 * 条件类型
	 * 
	 * @return 类型
	 */
	short getType();

	/**
	 * 属性名，不是组合条件时返回
	 * 
	 * @return 属性名
	 */
	String getName();

	/**
	 * 属性值 ，不是组合条件时返回
	 * 
	 * @return 属性值
	 */
	Object getValue();

	/**
	 * 条件项，组合条件时返回
	 * 
	 * @return 条件项
	 */
	List<Condition> getItems();
}

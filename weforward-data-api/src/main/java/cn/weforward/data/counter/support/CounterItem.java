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
package cn.weforward.data.counter.support;

import cn.weforward.data.UniteId;
import cn.weforward.data.array.LabelElement;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.exception.ObjectMappingException;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.support.AbstractObjectMapper;
import cn.weforward.protocol.support.datatype.FriendlyObject;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 计数器项，主要由id及value组成， 但由于计数器系统可能分布部署
 * 
 * value 只表示当前（服务器/节点）的变化的值
 * 
 * hold 表示其它节点的总值减去当前值的结果
 * 
 * 所以（准）总值=value+hold
 * 
 * @author liangyi
 * 
 */
public class CounterItem implements LabelElement {
	/** 统计ID */
	public final String id;
	/** 当前（变化中的）计数 */
	public volatile long value;
	/** 同步后（中心服务器）返回的总值减去当前值的结果 */
	public volatile long hold;

	/**
	 * 构造初始值为0的项
	 * 
	 * @param id 计数项ID
	 */
	public CounterItem(String id) {
		this.id = id;
	}

	/**
	 * 构造
	 * 
	 * @param id    计数项ID
	 * @param value 初始值
	 */
	public CounterItem(String id, long value) {
		this.id = id;
		this.value = value;
	}

	/**
	 * 计数项的（合并）值
	 * 
	 * @return 数值
	 */
	public long getTotal() {
		return (value + hold);
	}

	/**
	 * 当前（节点下的）值
	 * 
	 * @return 值
	 */
	public long getValue() {
		return value;
	}

	/**
	 * 加一
	 * 
	 * @return 累加后的值
	 */
	public long inc() {
		return ((++value) + hold);
	}

	/**
	 * 减一
	 * 
	 * @return 减少后的值
	 */
	public long dec() {
		return ((--value) + hold);
	}

	/**
	 * 累加指定值（为负数则是减）
	 * 
	 * @param add 累加值（负数则为减）
	 * @return 累加后的值
	 */
	public long addAndGet(int add) {
		value += add;
		return (value + hold);
	}

	/**
	 * 指定绝对值
	 * 
	 * @param v 绝对值
	 */
	public void set(long v) {
		value = v - hold;
	}

	/**
	 * 由其它节点/主节点同步（影响hold的值）
	 * 
	 * @param total 统计总值
	 * @return 总值没变化返回false
	 */
	public boolean syncTotal(long total) {
		if (getTotal() == total) {
			return false;
		}
		this.hold = total - this.value;
		return true;
	}

	public int getIntValue() {
		return long2int(this.value);
	}

	/**
	 * 为兼容，强制把long转为int
	 * 
	 * @param v long数值
	 * @return int数值
	 */
	static public final int long2int(long v) {
		return (int) (0xFFFFFFFFL & v);
	}

	@Override
	public String getIdForLabel() {
		return this.id;
	}

	@Override
	public String toString() {
		return "{id:" + id + ",v:" + value + ",vv:" + getTotal() + "}";
	}

	/** 映射器 */
	public static ObjectMapper<CounterItem> _Mapper = new AbstractObjectMapper<CounterItem>() {

		@Override
		public String getName() {
			return UniteId.getSimpleName(CounterItem.class);
		}

		@Override
		public DtObject toDtObject(CounterItem object) throws ObjectMappingException {
			SimpleDtObject dt = new SimpleDtObject();
			dt.put("id", object.id);
			dt.put("v", object.value);
			return dt;
		}

		@Override
		public CounterItem fromDtObject(DtObject obj) throws ObjectMappingException {
			FriendlyObject v = FriendlyObject.valueOf(obj);
			return new CounterItem(v.getString("id"), v.getLong("v"));
		}
	};
}

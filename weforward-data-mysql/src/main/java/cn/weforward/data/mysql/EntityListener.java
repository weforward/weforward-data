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

import java.util.Map;

/**
 * 实体变化监听
 * 
 * @author daibo
 *
 */
public interface EntityListener {
	/** INSERT事件 */
	int INSERT = 1;
	/** UPDATE事件 */
	int UPDATE = 2;
	/** DELETE事件 */
	int DELETE = 3;

	/**
	 * 要监听的数据库
	 * 
	 * @return 数据库名
	 */
	String getDatabase();

	/**
	 * 要监听的表
	 * 
	 * @return 表名
	 */
	String getTabelName();

	/**
	 * 变化事件
	 * 
	 * @param entity 变化实体
	 */
	void onChange(ChangeEntity entity);

	/**
	 * 变化实体
	 * 
	 * @author daibo
	 *
	 */
	public static class ChangeEntity {
		/** 类型 */
		protected int m_Type;
		/** 值 */
		protected Map<String, String> m_Map;

		public ChangeEntity(int type, Map<String, String> map) {
			m_Map = map;
			m_Type = type;
		}

		/**
		 * 类型
		 * 
		 * @return 类型
		 */
		public int getType() {
			return m_Type;
		}

		/**
		 * 获取值
		 * 
		 * @param key 键
		 * @return 对应值
		 */
		public String getString(String key) {
			return m_Map.get(key);
		}

	}
}

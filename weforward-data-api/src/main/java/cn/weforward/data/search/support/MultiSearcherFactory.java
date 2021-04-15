package cn.weforward.data.search.support;

import java.util.ArrayList;
import java.util.List;

import cn.weforward.data.search.IndexElement;
import cn.weforward.data.search.IndexKeyword;
import cn.weforward.data.search.IndexRange;
import cn.weforward.data.search.IndexResults;
import cn.weforward.data.search.SearchOption;
import cn.weforward.data.search.Searcher;
import cn.weforward.data.search.SearcherFactory;

/**
 * 多个搜索工厂集合,一般用来作数据过滤,同时建立多个索引
 * 
 * @author daibo
 *
 */
public class MultiSearcherFactory extends AbstractSearcherFactory {
	/** 工厂集合 */
	protected List<SearcherFactory> m_Factorys;
	/** 主工厂 */
	protected int m_Main;

	public MultiSearcherFactory(List<SearcherFactory> factorys, int main) {
		m_Factorys = factorys;
		m_Main = main;
	}

	@Override
	protected Searcher doCreateSearcher(String name) {
		List<Searcher> finders = new ArrayList<>(m_Factorys.size());
		for (int i = 0; i < m_Factorys.size(); i++) {
			finders.add(m_Factorys.get(i).createSearcher(name));
		}
		return new MultiSearcher(finders);
	}

	class MultiSearcher implements Searcher {
		List<Searcher> m_Finders;

		MultiSearcher(List<Searcher> list) {
			m_Finders = list;
		}

		private Searcher getMain() {
			return m_Finders.get(m_Main);
		}

		@Override
		public String getName() {
			return getMain().getName();
		}

		@Override
		public void updateElement(IndexElement element, List<? extends IndexKeyword> keywords) {
			for (Searcher f : m_Finders) {
				f.updateElement(element, keywords);
			}
		}

		@Override
		public void updateElement(IndexElement element, String... keyword) {
			for (Searcher f : m_Finders) {
				f.updateElement(element, keyword);
			}
		}

		@Override
		public boolean removeElement(String elementKey) {
			boolean isok = false;
			for (Searcher f : m_Finders) {
				if (f.removeElement(elementKey)) {
					isok = true;
				}
			}
			// 一个成功则成功
			return isok;
		}

		@Override
		public IndexResults search(SearchOption options, String... keyword) {
			return getMain().search(options, keyword);
		}

		@Override
		public IndexResults search(List<? extends IndexKeyword> keywords, SearchOption options) {
			return getMain().search(keywords, options);
		}

		@Override
		public IndexResults searchRange(String begin, String to, SearchOption options) {
			return getMain().searchRange(begin, to, options);
		}

		@Override
		public IndexResults searchRange(String begin, String to, List<? extends IndexKeyword> keywords,
				SearchOption options) {
			return getMain().searchRange(begin, to, keywords, options);
		}

		@Override
		public IndexResults searchRange(List<? extends IndexRange> ranges, List<? extends IndexKeyword> keywords,
				SearchOption options) {
			return getMain().searchRange(ranges, keywords, options);
		}

		@Override
		public IndexResults union(SearchOption options, String... keyword) {
			return getMain().union(options, keyword);
		}

		@Override
		public IndexResults union(List<? extends IndexKeyword> keywords, SearchOption options) {
			return getMain().union(keywords, options);
		}

		@Override
		public IndexResults unionRange(List<? extends IndexRange> ranges, List<? extends IndexKeyword> keywords,
				SearchOption options) {
			return getMain().unionRange(ranges, keywords, options);
		}

		@Override
		public IndexResults searchAll(List<? extends IndexRange> andRanges, List<? extends IndexRange> orRanges,
				List<? extends IndexKeyword> andKeywords, List<? extends IndexKeyword> orKeywords,
				SearchOption options) {
			return getMain().searchAll(andRanges, orRanges, andKeywords, orKeywords, options);
		}
	}

}

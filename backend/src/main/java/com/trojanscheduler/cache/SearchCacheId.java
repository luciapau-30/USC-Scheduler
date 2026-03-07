package com.trojanscheduler.cache;

import java.io.Serializable;
import java.util.Objects;

public class SearchCacheId implements Serializable {

	private String termCode;
	private String searchKey;

	public SearchCacheId() {}

	public SearchCacheId(String termCode, String searchKey) {
		this.termCode = termCode;
		this.searchKey = searchKey;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SearchCacheId that = (SearchCacheId) o;
		return Objects.equals(termCode, that.termCode) && Objects.equals(searchKey, that.searchKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(termCode, searchKey);
	}
}

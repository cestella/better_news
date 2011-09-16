package com.caseystella.news.interfaces;

public class IdentityCategoryMapper implements ICategoryMapper {

	@Override
	public String[] getCategories(String[] oldCategories) {
		return oldCategories;
	}

	@Override
	public String map(String pString) {
		return pString;
	}

}

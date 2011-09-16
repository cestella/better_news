package com.caseystella.news.interfaces;

public interface ICategoryMapper 
{
	String map(String pString);
	String[] getCategories(String[] oldCategories);
}

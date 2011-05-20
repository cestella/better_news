package com.caseystella.news.interfaces;

import java.io.Serializable;

public interface IPreprocessor extends Serializable
{
	public String transform(String pData);
}

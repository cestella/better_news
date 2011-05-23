package com.caseystella.news.nlp.preprocessor;

import com.caseystella.news.interfaces.IPreprocessor;

public class NoopPreprocessor implements IPreprocessor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8873298960216015092L;

	@Override
	public String transform(String pData) {
		return pData;
	}

}

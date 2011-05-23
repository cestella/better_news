package com.caseystella.news.nlp.preprocessor;

import java.util.ArrayList;

import com.caseystella.news.interfaces.IPreprocessor;

public class CompositionPreprocessor implements IPreprocessor {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1626794227792310714L;
	
	ArrayList<IPreprocessor> underlyingPreprocessors;
	
	public CompositionPreprocessor(IPreprocessor... preprocessors)
	{
		underlyingPreprocessors = new ArrayList<IPreprocessor>();
		for(IPreprocessor preprocessor : preprocessors)
		{
			underlyingPreprocessors.add(preprocessor);
		}
	}
	
	@Override
	public String transform(String pData) {
		
		String output = pData;
		for(int i = 0
			;output != null && i < underlyingPreprocessors.size()
			;output = underlyingPreprocessors.get(i++).transform(output)
			)
			;
		return output;
	}

}

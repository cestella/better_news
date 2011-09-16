package com.caseystella.news.nlp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import com.caseystella.news.interfaces.ICategoryMapper;
import com.caseystella.news.interfaces.IClassifier;
import com.caseystella.news.interfaces.IPreprocessor;
import com.caseystella.news.nlp.preprocessor.NoopPreprocessor;
import com.sun.tools.javac.util.Pair;

public abstract class AbstractClassifier implements IClassifier, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4277017201469946637L;
	protected IPreprocessor preprocessor;
	public AbstractClassifier(IPreprocessor pPreprocessor)
	{
		preprocessor = pPreprocessor;
	}
	
	

	@Override
	public void persist(File pClassifierFile) throws Exception {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(pClassifierFile));
		oos.writeObject(this);
	}

	
	
	public void train(List<Pair<BufferedReader, String>> pTrainingData, boolean pUsePreprocessor, ICategoryMapper pMapper) throws Exception
	{
		train(pTrainingData
			 , pUsePreprocessor?preprocessor:new NoopPreprocessor()
			 , pMapper
			 );
	}

}

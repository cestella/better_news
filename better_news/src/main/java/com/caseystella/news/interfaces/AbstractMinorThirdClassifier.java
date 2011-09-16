package com.caseystella.news.interfaces;

import java.io.BufferedReader;
import java.util.List;
import java.util.StringTokenizer;

import com.caseystella.news.nlp.util.AbstractClassifier;
import com.caseystella.news.nlp.util.NLPUtils;
import com.sun.tools.javac.util.Pair;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;

public abstract class AbstractMinorThirdClassifier extends AbstractClassifier
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1197442497374735296L;
	
	protected Classifier classifier;
	
	public AbstractMinorThirdClassifier(IPreprocessor pPreprocessor) {
		super(pPreprocessor);
		
	}
	
	
	
	protected Instance makeInstance(String pString, IPreprocessor pPreprocessor)
	{
		String data = pPreprocessor.transform(pString);
		StringTokenizer tokenizer = new StringTokenizer(data);
		MutableInstance instance = new MutableInstance();
		while(tokenizer.hasMoreTokens())
		{
			instance.addBinary(new Feature(tokenizer.nextToken()));
		}
		return instance;
	}
	
	public abstract ClassifierLearner getLearner();
	
	
	@Override
	public void train( List<Pair<BufferedReader, String>> pTrainingData
					 , IPreprocessor pPreprocessor
					 , ICategoryMapper pMapper
					 ) throws Exception 
	{
		ClassifierLearner learner = getLearner();
		learner.setSchema(new ExampleSchema(pMapper.getCategories(getCategories())));
		System.out.println("TRAINING ON " + pTrainingData.size() + " SAMPLES...");
		for(Pair<BufferedReader, String> datum : pTrainingData)
		{
			String strDat = NLPUtils.toString(datum.fst);
			
			Example example = new Example(makeInstance(strDat, pPreprocessor), new ClassLabel(pMapper.map(datum.snd)));
			learner.addExample(example);
		}
		//learner.completeTraining();
		classifier = learner.getClassifier();
	}
	

}

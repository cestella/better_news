package com.caseystella.news.interfaces;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.StringTokenizer;

import com.google.common.io.Files;
import com.sun.tools.javac.util.Pair;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;

public abstract class AbstractMinorThirdClassifier extends AbstractNewsClassifier
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1197442497374735296L;
	
	Classifier classifier;
	
	public AbstractMinorThirdClassifier(IPreprocessor pPreprocessor) {
		super(pPreprocessor);
		
	}
	
	@Override
	public Affiliations classify(String pInputData) throws IOException,
			Exception {
		ClassLabel label = classifier.classification(makeInstance(pInputData, preprocessor));
		if(label.bestClassName() == null)
			return Affiliations.MILDLY_LIBERAL;
		return Affiliations.nameToEnum(label.bestClassName());
	}
	
	private Instance makeInstance(String pString, IPreprocessor pPreprocessor)
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
	public void train(List<Pair<File, Integer>> pTrainingData,
			IPreprocessor pPreprocessor) throws Exception {
		ClassifierLearner learner = getLearner();
		learner.setSchema(new ExampleSchema(getCategories()));
		System.out.println("TRAINING ON " + pTrainingData.size() + " SAMPLES...");
		for(Pair<File, Integer> datum : pTrainingData)
		{
			String strDat = Files.toString(datum.fst, Charset.defaultCharset());
			
			Example example = new Example(makeInstance(strDat, pPreprocessor), new ClassLabel(Affiliations.codeToName(datum.snd).getName()));
			learner.addExample(example);
		}
		//learner.completeTraining();
		classifier = learner.getClassifier();
	}
	

}

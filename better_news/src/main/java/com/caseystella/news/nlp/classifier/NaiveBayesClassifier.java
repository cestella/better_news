package com.caseystella.news.nlp.classifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import com.aliasi.classify.Classification;
import com.aliasi.classify.Classified;
import com.aliasi.classify.DynamicLMClassifier;
import com.aliasi.lm.NGramProcessLM;
import com.caseystella.news.interfaces.Affiliations;
import com.caseystella.news.interfaces.IPreprocessor;
import com.caseystella.news.nlp.util.AbstractClassifier;
import com.caseystella.news.nlp.util.NLPUtils;
import com.sun.tools.javac.util.Pair;

public class NaiveBayesClassifier extends AbstractClassifier<Affiliations> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5907349564699559539L;
	public static final int NUM_NGRAMS = 8;
	private DynamicLMClassifier<NGramProcessLM> mClassifier;
	
	public NaiveBayesClassifier(IPreprocessor pPreprocessor)
	{
		super(pPreprocessor);
		
	}
	
	@Override
	public String[] getCategories() {
		return Affiliations.getCategories();
	}
	
	@Override
	public Affiliations classify(String pInputData) throws IOException,
			Exception {
		return Affiliations.nameToEnum(mClassifier.classify(preprocessor.transform(pInputData)).bestCategory());
	}

	
	@Override
	public void train(List<Pair<BufferedReader, String>> pTrainingData, IPreprocessor pPreprocessor) throws Exception {

		mClassifier = DynamicLMClassifier.createNGramProcess(getCategories(), NUM_NGRAMS);
		System.out.println("TRAINING UNDERLYING CLASSIFIER");
		for(Pair<BufferedReader, String> datum : pTrainingData)
		{
			Classification classification
            = new Classification(datum.snd);
			 String data = pPreprocessor.transform(NLPUtils.toString(datum.fst));
			 
             Classified<CharSequence> classified
                 = new Classified<CharSequence>(data,classification);
             mClassifier.handle(classified);
		}

	}

}

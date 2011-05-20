package com.caseystella.news.nlp.classifier;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.aliasi.classify.Classification;
import com.aliasi.classify.Classified;
import com.aliasi.classify.DynamicLMClassifier;
import com.aliasi.lm.NGramProcessLM;
import com.aliasi.util.Files;
import com.caseystella.news.interfaces.AbstractNewsClassifier;
import com.caseystella.news.interfaces.Affiliations;
import com.caseystella.news.interfaces.IPreprocessor;
import com.sun.tools.javac.util.Pair;

public class NaiveBayesClassifier extends AbstractNewsClassifier {

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
	public Affiliations classify(String pInputData) throws IOException,
			Exception {
		return Affiliations.nameToEnum(mClassifier.classify(preprocessor.transform(pInputData)).bestCategory());
	}

	
	@Override
	public void train(List<Pair<File, Integer>> pTrainingData, IPreprocessor pPreprocessor) throws Exception {

		mClassifier = DynamicLMClassifier.createNGramProcess(getCategories(), NUM_NGRAMS);
		System.out.println("TRAINING UNDERLYING CLASSIFIER");
		for(Pair<File, Integer> datum : pTrainingData)
		{
			Classification classification
            = new Classification(Affiliations.codeToName(datum.snd).getName());
			 String data = pPreprocessor.transform(Files.readFromFile(datum.fst,"ISO-8859-1"));
			 
             Classified<CharSequence> classified
                 = new Classified<CharSequence>(data,classification);
             mClassifier.handle(classified);
		}

	}

}

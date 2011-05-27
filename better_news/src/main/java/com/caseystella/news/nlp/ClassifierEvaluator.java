package com.caseystella.news.nlp;

import java.io.BufferedReader;
import java.util.List;

import com.aliasi.classify.BaseClassifierEvaluator;
import com.aliasi.classify.Classification;
import com.caseystella.news.interfaces.IClassifier;
import com.caseystella.news.nlp.util.NLPUtils;
import com.sun.tools.javac.util.Pair;

public class ClassifierEvaluator<T extends Enum<T>>
{
	
	
	public void evaluate(IClassifier<T> pClassifier, List<Pair<BufferedReader, String>> pTestingData) throws Exception
	{
		boolean storeInstances = false;
        BaseClassifierEvaluator<CharSequence> evaluator
            = new BaseClassifierEvaluator<CharSequence>(null,getCategories(pClassifier),storeInstances);
        for(Pair<BufferedReader, String> datum : pTestingData)
        {
        	
        	T classification = pClassifier.classify(NLPUtils.toString(datum.fst));
        	
        	evaluator.addClassification(transform(datum.snd), new Classification(transform(classification.toString())), null);
        }
        System.out.println(evaluator.toString());
	}
	
	public String[] getCategories(IClassifier<T> pClassifier)
	{
		return pClassifier.getCategories();
	}
	
	public String transform(String pInstance)
	{
		return pInstance.toString();
	}
}

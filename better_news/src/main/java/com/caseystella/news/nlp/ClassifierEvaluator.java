package com.caseystella.news.nlp;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aliasi.classify.BaseClassifierEvaluator;
import com.aliasi.classify.Classification;
import com.caseystella.news.interfaces.IClassifier;
import com.caseystella.news.nlp.util.NLPUtils;
import com.sun.tools.javac.util.Pair;

public class ClassifierEvaluator
{
	
	
	public void evaluate(IClassifier pClassifier, List<Pair<BufferedReader, String>> pTestingData) throws Exception
	{
		boolean storeInstances = false;
        BaseClassifierEvaluator<CharSequence> evaluator
            = new BaseClassifierEvaluator<CharSequence>(null,getCategories(pClassifier),storeInstances);
        dumpCategoryCounts(pTestingData);
        for(Pair<BufferedReader, String> datum : pTestingData)
        {
        	
        	String classification = pClassifier.classify(NLPUtils.toString(datum.fst));
        	String referenceCategory = transform(datum.snd);
        	evaluator.addClassification(referenceCategory, new Classification(transform(classification.toString())), null);
        	
        }
        
        System.out.println(evaluator.toString());
	}
	
	public void dumpCategoryCounts(List<Pair<BufferedReader, String>> pTestingData)
	{
		Map<String, Integer> categoryCount = new HashMap<String, Integer>();
		for(Pair<BufferedReader, String> datum : pTestingData)
        {
			String referenceCategory = transform(datum.snd);
			if(!categoryCount.containsKey(referenceCategory))
        	{
        		categoryCount.put(referenceCategory, 1);
        	}
        	else
        	{
        		categoryCount.put(referenceCategory, categoryCount.get(referenceCategory) + 1);
        	}
        }
		for(Map.Entry<String, Integer> entry : categoryCount.entrySet())
        {
        	System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
	}
	
	public String[] getCategories(IClassifier pClassifier)
	{
		return pClassifier.getCategories();
	}
	
	public String transform(String pInstance)
	{
		return pInstance.toString();
	}
}

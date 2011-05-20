package com.caseystella.news.nlp;

import java.io.File;
import java.util.List;

import com.aliasi.classify.BaseClassifierEvaluator;
import com.aliasi.classify.Classification;
import com.aliasi.util.Files;
import com.caseystella.news.interfaces.Affiliations;
import com.caseystella.news.interfaces.IClassifier;
import com.sun.tools.javac.util.Pair;

public class ClassifierEvaluator 
{
	public static void evaluate(IClassifier pClassifier, List<Pair<File, Integer>> pTestingData) throws Exception
	{
		boolean storeInstances = false;
        BaseClassifierEvaluator<CharSequence> evaluator
            = new BaseClassifierEvaluator<CharSequence>(null,pClassifier.getCategories(),storeInstances);
        for(Pair<File, Integer> datum : pTestingData)
        {
        	
        	Affiliations affiliation = pClassifier.classify(Files.readFromFile(datum.fst, "ISO-8859-1"));
        	
        	evaluator.addClassification(Affiliations.codeToName(datum.snd).getName(), new Classification(affiliation.getName()), null);
        }
        System.out.println(evaluator.toString());
	}
}

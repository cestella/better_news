package com.caseystella.news;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import com.caseystella.news.interfaces.Affiliations;
import com.caseystella.news.interfaces.IClassifier;
import com.sun.tools.javac.util.Pair;

public class VerifyAgainstReddit {

	private static void add(List<Pair<BufferedReader, String>> pCollection, File pDir, String pLabel, int limit) throws Exception
	{
		int cnt = 0;
		for(File child : pDir.listFiles())
		{
			if(cnt == limit)
				break;
			pCollection.add(new Pair<BufferedReader, String>(new BufferedReader(new FileReader(child)), pLabel));
			++cnt;
		}
		System.out.println("Added " + cnt + " " + pLabel + " examples");
	}
	
	public static void verify(IClassifier classifier) throws Exception
	{
		List<Pair<BufferedReader, String>> testingSet = new ArrayList<Pair<BufferedReader, String>>();
		File baseDir = new File(Resource.baseDataPath, "reddit_data");
		File liberalDir = new File(baseDir, "liberal");
		File conservativeDir = new File(baseDir, "conservative");
		int max = (int)Math.min(liberalDir.list().length, conservativeDir.list().length);
		add(testingSet, new File(baseDir, "liberal"), "LIBERAL", max);

		add(testingSet, new File(baseDir, "conservative"), "CONSERVATIVE", max);
		
		
		App.CLASSIFIER_EVALUATOR
	        .evaluate(classifier, testingSet);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		Resource.baseDataPath = new File(args[0]);
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(Resource.getModelsDirectory(), "news-classifier.model")));
        IClassifier classifier = (IClassifier)ois.readObject();
        verify(classifier);
	}

}

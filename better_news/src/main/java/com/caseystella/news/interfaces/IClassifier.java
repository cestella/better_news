package com.caseystella.news.interfaces;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import com.sun.tools.javac.util.Pair;

public interface IClassifier extends Serializable
{
	public Affiliations classify(String pInputData) throws IOException, Exception;
	public void train(List<Pair<File, Integer>> pTrainingData, IPreprocessor pPreprocessor) throws Exception;
	public void persist(File pClassifierFile) throws Exception;
	public String[] getCategories();
}

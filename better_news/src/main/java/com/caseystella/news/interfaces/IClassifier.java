package com.caseystella.news.interfaces;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import com.sun.tools.javac.util.Pair;

public interface IClassifier extends Serializable
{
	public String classify(String pInputData) throws IOException, Exception;
	public void train(List<Pair<BufferedReader, String>> pTrainingData, IPreprocessor pPreprocessor, ICategoryMapper pMapper) throws Exception;
	public void persist(File pClassifierFile) throws Exception;
	public String[] getCategories();
}

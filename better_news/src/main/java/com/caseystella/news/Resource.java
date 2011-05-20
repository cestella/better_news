package com.caseystella.news;

import java.io.File;

public class Resource {
	public static File baseDataPath = null;
	
	public static File getClassifierDataDirectory()
	{
		return new File(baseDataPath, "classifier_data");
	}
	
	public static File getSubjectivityDataDirectory()
	{
		return new File(baseDataPath, "subjectivity_data");
	}
	
	public static File getIdealPointsFile()
	{
		return new File(baseDataPath, "ideal_points.csv");
	}
	
	public static File getModelsDirectory()
	{
		return new File(baseDataPath, "models");
	}
	
	public static File getStopwords()
	{
		return new File(baseDataPath, "stopwords.txt");
	}
}

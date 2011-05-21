package com.caseystella.news.nlp.classifier;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.caseystella.news.interfaces.AbstractNewsClassifier;
import com.caseystella.news.interfaces.Affiliations;
import com.caseystella.news.interfaces.IPreprocessor;
import com.caseystella.news.nlp.TopicInferencer;
import com.caseystella.news.statistics.util.MahalanobisDistance;
import com.google.common.io.Files;
import com.sun.tools.javac.util.Pair;

public class TopicInferencerClassifier extends AbstractNewsClassifier {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3181407350563113087L;

	private EnumMap<Affiliations, MahalanobisDistance> affiliationToDistanceMetric;
	private TopicInferencer inferencer;
	
	public TopicInferencerClassifier(TopicInferencer pInferencer, IPreprocessor pPreprocessor) 
	{
		super(pPreprocessor);	
		inferencer = pInferencer;
	}

	@Override
	public Affiliations classify(String pInputData) throws IOException,	Exception 
	{
		SortedSet<Pair<Double, Affiliations> > distances 
			= new TreeSet<Pair<Double, Affiliations>>( new Comparator<Pair<Double, Affiliations>>() 
														{
															@Override
															public int compare(
																	Pair<Double, Affiliations> o1,
																	Pair<Double, Affiliations> o2
																			  ) 
															{
																return o1.fst.compareTo(o2.fst);
															}
														}
													 );
		double[] vec = inferencer.getVector(pInputData);
		for(Affiliations affiliation : Affiliations.values())
		{
			MahalanobisDistance distance = affiliationToDistanceMetric.get(affiliation);
			distances.add(new Pair<Double, Affiliations>(distance.distance(vec), affiliation));
		}
		return distances.first().snd;
	}

	@Override
	public void train( List<Pair<File, Integer>> pTrainingData
					 , IPreprocessor pPreprocessor
					 ) 
	throws Exception 
	{
		affiliationToDistanceMetric = new EnumMap<Affiliations, MahalanobisDistance>(Affiliations.class);
		for(Affiliations affiliation : Affiliations.values())
		{
			affiliationToDistanceMetric.put(affiliation, new MahalanobisDistance(inferencer.getDimension()));
		}
		
		for(Pair<File, Integer> pair : pTrainingData)
		{
			MahalanobisDistance distance = affiliationToDistanceMetric.get(Affiliations.codeToName(pair.snd));
			double[] vec = inferencer.getVector(Files.toString(pair.fst, Charset.defaultCharset()));
			distance.add(vec);
		}
		for(Affiliations affiliation : Affiliations.values())
		{
			affiliationToDistanceMetric.get(affiliation).finalizeDistance();
		}
		
	}

}

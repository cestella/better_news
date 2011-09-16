package com.caseystella.news.nlp.classifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.caseystella.news.interfaces.Affiliations;
import com.caseystella.news.interfaces.ICategoryMapper;
import com.caseystella.news.interfaces.IPreprocessor;
import com.caseystella.news.nlp.TopicInferencer;
import com.caseystella.news.nlp.util.AbstractClassifier;
import com.caseystella.news.nlp.util.MahalanobisDistance;
import com.caseystella.news.nlp.util.NLPUtils;
import com.sun.tools.javac.util.Pair;

public class TopicInferencerClassifier extends AbstractClassifier {

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
	public String classify(String pInputData) throws IOException,	Exception 
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
			double mDistance = distance.distance(vec);
			//System.out.println(affiliation + " -> " + mDistance);
			distances.add(new Pair<Double, Affiliations>(mDistance, affiliation));
		}
		return distances.first().snd.toString();
	}
	@Override
	public String[] getCategories() {
		return Affiliations.getCategories();
	}
	
	
	
	@Override
	public void train( List<Pair<BufferedReader, String>> pTrainingData
					 , IPreprocessor pPreprocessor
					 , ICategoryMapper pMapper
					 ) 
	throws Exception 
	{
		affiliationToDistanceMetric = new EnumMap<Affiliations, MahalanobisDistance>(Affiliations.class);
		for(Affiliations affiliation : Affiliations.values())
		{
			affiliationToDistanceMetric.put(affiliation, new MahalanobisDistance(inferencer.getDimension()));
		}
		
		for(Pair<BufferedReader, String> pair : pTrainingData)
		{
			MahalanobisDistance distance = affiliationToDistanceMetric.get(Affiliations.nameToEnum(pair.snd));
			double[] vec = inferencer.getVector(NLPUtils.toString(pair.fst));
			distance.add(vec);
		}
		for(Affiliations affiliation : Affiliations.values())
		{
			affiliationToDistanceMetric.get(affiliation).finalizeDistance();
		}
		
	}
	public List<List<String>> getTopics(String pDoc, int numTopics) throws Exception
	{
		return inferencer.getTopics(pDoc, numTopics);
	}
}

package com.caseystella.news.nlp.preprocessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.caseystella.news.interfaces.AbstractMinorThirdClassifier;
import com.caseystella.news.interfaces.IPreprocessor;
import com.caseystella.news.nlp.TopicInferencer;
import com.caseystella.news.nlp.util.AbstractComponentComparator;
import com.caseystella.news.nlp.util.MahalanobisDistance;
import com.caseystella.news.nlp.util.NLPUtils;
import com.sun.tools.javac.util.Pair;

import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.ManyVsRestLearner;
import edu.cmu.minorthird.ui.Recommended;

public class SubjectivityPreprocessor extends AbstractMinorThirdClassifier<SubjectivityPreprocessor.SubjectivityCategories> 
									  implements IPreprocessor
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1072441849198987921L;
	private static double CUTOFF_PERCENTAGE = .5;
    
   public static enum SubjectivityCategories
   {
	    OBJECTIVE("OBJECTIVE")
	  , SUBJECTIVE("SUBJECTIVE");
	   private static String[] categories = new String[] { "OBJECTIVE", "SUBJECTIVE" };
	   String name;
	   
	   private SubjectivityCategories(String pName)
	   {
		   name = pName;
	   }
	   
	   public static String[] getCategories()
	   {
		   return categories;
	   }
   }
   
   private TopicInferencer inferencer;
   private MahalanobisDistance distance;
   private double cutoffPoint;
   
   public SubjectivityPreprocessor(TopicInferencer pInferencer)
   {
	   super(new NoopPreprocessor());
	   inferencer = pInferencer;
	   distance = new MahalanobisDistance(inferencer.getDimension());
   }
   @Override
	public ClassifierLearner getLearner() {
		
			return new ManyVsRestLearner(new Recommended.BoostedStumpLearner());
	
	} 
   
	@Override
	public String transform(String pData) {
		StringBuffer buff = new StringBuffer();
		try
		{
			List<String> sentences = NLPUtils.extractSentences(pData);
			for(String sentence : sentences)
			{
				if(classify(sentence) == SubjectivityCategories.SUBJECTIVE){
					buff.append(sentence + "\n");
				}
			}
			String ret = buff.toString().trim();
			if(ret.length() == 0)
				return pData;
			return buff.toString();
		}
		catch(Exception ex)
		{
			System.err.println("Unable to transform..just passing through");
			ex.printStackTrace(System.err);
			return pData;
		}
		
	}
	
	@Override
	public SubjectivityCategories classify(String pInputData) throws IOException,
			Exception {
		return distance.distance(inferencer.getVector(pInputData)) < cutoffPoint?SubjectivityCategories.OBJECTIVE:SubjectivityCategories.SUBJECTIVE;
//		ClassLabel label = classifier.classification(makeInstance(pInputData, preprocessor));
//		if(label.bestClassName() == null)
//			return SubjectivityCategories.OBJECTIVE;
//		return SubjectivityCategories.valueOf(label.bestClassName());
	}
	
	@Override
	public String[] getCategories() {
		return SubjectivityCategories.getCategories();
	}
	
	
	@Override
	public void train(List<Pair<BufferedReader, String>> pTrainingData,
			IPreprocessor pPreprocessor) throws Exception 
	{
		
		System.out.println("TRAINING OBJECTIVITY CLASSIFIER");
       
        SortedSet<Pair<String, Double>> sentenceSet 
        	= new TreeSet<Pair<String, Double>>(new AbstractComponentComparator<Pair<String, Double>, Double>() 
        											{
        												protected Double getComponent(Pair<String, Double> pUnderlyingObject) 
        												{
        													return pUnderlyingObject.snd;
        												}	
        											}
        									   );
        List<String> sentenceList = new ArrayList<String>();
        for(Pair<BufferedReader, String> trainingFile : pTrainingData)
        {
        	List<String> sentences = NLPUtils.extractSentences(NLPUtils.toString(trainingFile.fst));
        	for(String sentence : sentences)
        	{
        		distance.add(inferencer.getVector(sentence));
        		sentenceList.add(sentence);
        	}
        }
        distance.finalizeDistance();
        
        for(String sentence : sentenceList)
        {
        	sentenceSet.add(new Pair<String, Double> (sentence, distance.distance(inferencer.getVector(sentence))));
        }
        
        int breakPoint = (int)(CUTOFF_PERCENTAGE*sentenceList.size());
        
        List<Pair<BufferedReader, String>> trainingSet = new ArrayList<Pair<BufferedReader,String>>();
        int i = 0;
        for(Pair<String, Double> sentence : sentenceSet)
        {
        	SubjectivityCategories category = i < breakPoint?SubjectivityCategories.OBJECTIVE:SubjectivityCategories.SUBJECTIVE;
        	if(i == breakPoint)
        	{
        		cutoffPoint = sentence.snd;
        	}
        	BufferedReader sentenceReader = new BufferedReader(new StringReader(sentence.fst));
        	trainingSet.add(new Pair<BufferedReader, String>(sentenceReader, category.toString()));
        	if(i++ >= 2*breakPoint)
        		break;
        	
        }
        //super.train(trainingSet, pPreprocessor);
	}

	
	
}

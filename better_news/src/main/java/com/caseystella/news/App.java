package com.caseystella.news;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.caseystella.news.interfaces.AbstractMinorThirdNewsClassifier;
import com.caseystella.news.interfaces.Affiliations;
import com.caseystella.news.interfaces.IPreprocessor;
import com.caseystella.news.nlp.ClassifierEvaluator;
import com.caseystella.news.nlp.TopicInferencer;
import com.caseystella.news.nlp.preprocessor.CompositionPreprocessor;
import com.caseystella.news.nlp.preprocessor.PorterStemmerPreprocessor;
import com.caseystella.news.nlp.preprocessor.SubjectivityPreprocessor;
import com.caseystella.news.nlp.util.AbstractClassifier;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ListMultimap;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.sun.tools.javac.util.Pair;

import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.ManyVsRestLearner;
import edu.cmu.minorthird.classify.MostFrequentFirstLearner;
import edu.cmu.minorthird.classify.TweakedLearner;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.ui.Recommended;



public class App 
{
	public static final float TRAINING_PARTITION = 0.7f;
	
	public static final int STRONGLY_LIBERAL = 0;
	public static final int MILDLY_LIBERAL = 1;
	public static final int MILDLY_CONSERVATIVE = 2;
	public static final int STRONGLY_CONSERVATIVE = 3;
	
	public static void copyLDA(List<Pair<File, Integer>> testingSet, List<Pair<File, Integer>> trainingSet) throws Exception
	{
		for(Pair<File, Integer> testingPair : testingSet)
		{
			File resultingDir = new File(new File(Resource.getLDADirectory(), "testing"), Affiliations.codeToName(testingPair.snd).getName());
			Files.copy(testingPair.fst, new File(resultingDir, testingPair.fst.getName()));
		}
		for(Pair<File, Integer> trainingPair : trainingSet)
		{
			File resultingDir = new File(new File(Resource.getLDADirectory(), "training"), Affiliations.codeToName(trainingPair.snd).getName());
			Files.copy(trainingPair.fst, new File(resultingDir, trainingPair.fst.getName()));
		}
	}
	
    public static void main( String[] args ) throws Exception
    {
    	Resource.baseDataPath = new File(args[0]);
    	File fixedPointFile = Resource.getIdealPointsFile();
    	File dataDirectory = Resource.getClassifierDataDirectory();
    	
    	File subjectivityFile = new File(Resource.getModelsDirectory(), "subjectivity.model");
    	
    	LineProcessor<List<Pair<String, Float>>> preprocessor =
    		new LineProcessor<List<Pair<String, Float>>>() 
    		{
				List<Pair<String, Float>> data = new ArrayList<Pair<String,Float>>();
				@Override
				public boolean processLine(String arg0) throws IOException {
					String[] tokens = arg0.split(",");
					String name = null;
					Float position = null;
					try
					{
						position = Float.parseFloat(tokens[1]);
					}
					catch(Exception ex)
					{
						return true;
					}
					
					name = tokens[0].replaceAll("\"", "").split(" ")[0];
					data.add( new Pair<String, Float>(name, position));
					return true;
				}
				
				@Override
				public List<Pair<String, Float>> getResult() {
					return data;
				}
    		};
    	List<Pair<String, Float>> data;
        Files.readLines(fixedPointFile, Charset.defaultCharset(), preprocessor);
        data = preprocessor.getResult();
        
        Comparator<Pair<String, Float>> naturalOrder = new Comparator<Pair<String, Float>>() {
        	@Override
        	public int compare(Pair<String, Float> o1, Pair<String, Float> o2) {
        		if(o1.snd == o2.snd) return 0;
        		return o1.snd < o2.snd?-1:1;
        	}
        	
		};
        
        Collections.sort(data, naturalOrder);
       
        //find the zero point
        int zeroPoint = Math.abs(Collections.binarySearch(data
        										, new Pair<String, Float>(null, new Float(0))
        										, naturalOrder
        										)
        						);
        if(!(data.get(zeroPoint).snd < 0 && data.get(zeroPoint+1).snd > 0))
        {
        	zeroPoint--;
        }
        Map<String, Integer> politicianToAffiliation = new HashMap<String, Integer>();
        ListMultimap<Integer, File> affiliationToDataSet = ArrayListMultimap.create();
        
        //partition the space by segmenting the liberal and conservatives in half by density.
        
        Map<Integer, Set<String>> partitionMap = new HashMap<Integer, Set<String>>();
        int[] affiliationCounts = new int[STRONGLY_CONSERVATIVE+1];
        
        {
	        int[] partitionPoints = { 0, zeroPoint/2, zeroPoint, zeroPoint + (data.size() - zeroPoint)/2, data.size() - 1};
	        
	        for(int i = 1;i < partitionPoints.length;i++)
	        {
	        	final float leftEndpoint = data.get(partitionPoints[i - 1]).snd;
	        	final float rightEndpoint = data.get(partitionPoints[i]).snd + ((i < partitionPoints.length - 1)?0:1);
	        	Collection<Pair<String, Float>> filteredCollection = new HashSet<Pair<String, Float>>(
	        		Collections2.filter(data
						   , new Predicate<Pair<String, Float>>() {
					@Override
					public boolean apply(
							Pair<String, Float> arg0
									    )
					{
						if(arg0.snd < rightEndpoint && arg0.snd >= leftEndpoint)
						{
							return true;
						}
						return false;
					}
				}
			   ));
	        	Collection<String> transformedCollection = 
	        	Collections2.transform( filteredCollection
	        						  , new Function<Pair<String, Float>, String>() 
	        						  {
	        							@Override
	        							public String apply(Pair<String, Float> arg0) {
	        								return arg0.fst;
	        							}
	        						  }
	        					      );
	        	partitionMap.put(i - 1
	        					, new HashSet<String>(transformedCollection)
	        					);
	        }
	        
	        for(Pair<String, Float> datum : data)
	        {
	        	boolean assigned = false;
	        	for(int affiliation = STRONGLY_LIBERAL;affiliation <= STRONGLY_CONSERVATIVE && !assigned;++affiliation)
	        	{
	        		//this should just make a copy for us...
	        		Set<String> partition = partitionMap.get(affiliation);
	        		if(partition.contains(datum.fst))
	        		{
	        			politicianToAffiliation.put(datum.fst, affiliation);
	        			assigned = true;
	        		}
	        	}
	        	if(!assigned)
	        		System.err.println("Unable to assign " + datum.fst);
	        }
	        
	        File[] dataFiles = dataDirectory.listFiles();
	
	        
	        for(File dataFile : dataFiles)
	        {
	        	String lastName = dataFile.getName().split("_")[0].split("-")[1].toUpperCase().trim();
	        	Integer affiliation = politicianToAffiliation.get(lastName);
	        	if(affiliation == null)
	        	{
	        		System.err.println("Unable to map: " + lastName + " to a politician...");
	        		continue;
	        	}
	        	affiliationToDataSet.put(affiliation, dataFile);
	        	affiliationCounts[affiliation]++;
	        }
	        Arrays.sort(affiliationCounts);
        }
        List<Pair<BufferedReader, String>> trainingSet = new ArrayList<Pair<BufferedReader, String>>();
        List<Pair<BufferedReader, String>> testingSet = new ArrayList<Pair<BufferedReader, String>>();
        {
	        int trimPoint = affiliationCounts[0];
	        int trainingPoint = (int)(TRAINING_PARTITION*trimPoint);
	        
	        
	        for(int affiliation = STRONGLY_LIBERAL;affiliation <= STRONGLY_CONSERVATIVE;++affiliation)
	    	{
	        	List<File> affiliatedDataFiles = affiliationToDataSet.get(affiliation);
	        	Collections.shuffle(affiliatedDataFiles);
	        	for(int i = 0; i < trimPoint;++i)
	        	{
	        		Pair<BufferedReader, String> datum = new Pair<BufferedReader, String>(new BufferedReader(new FileReader(affiliatedDataFiles.get(i))), Affiliations.codeToName(affiliation).toString());
	        		
	        		if(i < trainingPoint)
	        		{
	        			trainingSet.add(datum);
	        			
	        		}
	        		else
	        		{
	        			testingSet.add(datum);
	        		}
	        	}
	        	
	    	}
	        Collections.shuffle(trainingSet, new Random(0));
	        Collections.shuffle(testingSet, new Random(0));
        }
        SubjectivityPreprocessor subjectivityPreprocessor = null;
        if(subjectivityFile.exists())
        {
        	ObjectInputStream ios = new ObjectInputStream(new FileInputStream(subjectivityFile));
        	subjectivityPreprocessor = (SubjectivityPreprocessor)ios.readObject();
        }
        else
        {
        	subjectivityPreprocessor = new SubjectivityPreprocessor(new TopicInferencer( new File(Resource.getLDADirectory(), "inferencer-250.inferencer")
        														   , new File(Resource.getModelsDirectory(), "news.mallet")
        														   , 250
        														   )
        														   );
        	subjectivityPreprocessor.train(trainingSet, false);
        	subjectivityPreprocessor.persist(subjectivityFile);
        														
        }
        

        
        IPreprocessor pipePreprocessor = new CompositionPreprocessor( new PorterStemmerPreprocessor());
  
        
        //AbstractClassifier<Affiliations> classifier = new BoostedStumpClassifier(pipePreprocessor);
        
        //AbstractClassifier<Affiliations> classifier = new NaiveBayesClassifier(pipePreprocessor);
        
        AbstractClassifier<Affiliations> classifier = new AbstractMinorThirdNewsClassifier(pipePreprocessor) {
			
			/**
			 * 
			 */
			private static final long serialVersionUID = -5803102867652466430L;

			@Override
			public ClassifierLearner getLearner() {
				return new ManyVsRestLearner
						   ( 
								   new AdaBoost( new edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner(10,6)
						   			     , 100
								   		 )
								
						   );
			}
		};
        classifier.train(trainingSet, true);
        System.out.println();
        new ClassifierEvaluator<Affiliations>()
        .evaluate(classifier, testingSet);
    }
}

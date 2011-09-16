package com.caseystella.news;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.URL;
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
import com.caseystella.news.interfaces.ICategoryMapper;
import com.caseystella.news.interfaces.IClassifier;
import com.caseystella.news.interfaces.IPreprocessor;
import com.caseystella.news.interfaces.IdentityCategoryMapper;
import com.caseystella.news.nlp.ClassifierEvaluator;
import com.caseystella.news.nlp.TopicInferencer;
import com.caseystella.news.nlp.classifier.polarity.PolarityClassifier;
import com.caseystella.news.nlp.preprocessor.CompositionPreprocessor;
import com.caseystella.news.nlp.preprocessor.NoopPreprocessor;
import com.caseystella.news.nlp.preprocessor.PorterStemmerPreprocessor;
import com.caseystella.news.nlp.preprocessor.SubjectivityPreprocessor;
import com.caseystella.news.nlp.util.AbstractClassifier;
import com.caseystella.news.nlp.util.NLPUtils;
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
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner;



public class App 
{
	private static final String NON_BIASED = "MILD";

	public static final float TRAINING_PARTITION = 0.7f;
	
	public static final int STRONGLY_LIBERAL = 0;
	public static final int MILDLY_LIBERAL = 1;
	public static final int MILDLY_CONSERVATIVE = 2;
	public static final int STRONGLY_CONSERVATIVE = 3;
	
	public static ClassifierEvaluator CLASSIFIER_EVALUATOR =
		new ClassifierEvaluator()
		{
			public String transform(String pInstance) {
				if(pInstance.contains("MILDLY") || pInstance.equals(NON_BIASED))
				{
					return NON_BIASED;
				}
				if(pInstance.equals("LIBERAL") || pInstance.equals("CONSERVATIVE"))
					return pInstance;
				if(pInstance.equals(Affiliations.MILDLY_LIBERAL.toString()) || pInstance.equals(Affiliations.STRONGLY_LIBERAL.toString()) )
					return "LIBERAL";
				else
					return "CONSERVATIVE";
			}
			
			public String[] getCategories(com.caseystella.news.interfaces.IClassifier pClassifier) 
			{
				return new String[] { "LIBERAL", "CONSERVATIVE", NON_BIASED };
			}
		};
	
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
	
	public static void getTrainingAndTestingSet( List<Pair<BufferedReader, String>> trainingSet
											   , List<Pair<BufferedReader, String>> testingSet
											   , String[] categories
											   ) throws Exception
	{
		File fixedPointFile = Resource.getIdealPointsFile();
    	File dataDirectory = Resource.getClassifierDataDirectory();
    	
    	
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
        	double length = 5.0/8;
        	int[] partitionPoints = { 0, (int)((1-length)*zeroPoint), zeroPoint, zeroPoint + (int)(length*(data.size() - zeroPoint)), data.size() - 1};

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
        Map<String, Double> categoryToProbability = new HashMap<String, Double>();
        {
        	Map<String, Integer> categoryCounts = new HashMap<String, Integer>();
        	int trimPoint = affiliationCounts[0];
        	for(int affiliation = STRONGLY_LIBERAL;affiliation <= STRONGLY_CONSERVATIVE;++affiliation)
	    	{
        		for(int i = 0; i < trimPoint;++i)
	        	{
        			String referenceCategory = CLASSIFIER_EVALUATOR.transform(Affiliations.codeToName(affiliation).toString());
        			int count = 1;
        			if(categoryCounts.containsKey(referenceCategory))
        			{
        				count = categoryCounts.get(referenceCategory) + 1;
        			}
        			categoryCounts.put(referenceCategory, count);
	        	}
        		int minimumCount = Integer.MAX_VALUE;
        		for(Map.Entry<String, Integer> countEntry : categoryCounts.entrySet())
        		{
        			if(countEntry.getValue() < minimumCount)
        			{
        				minimumCount = countEntry.getValue();
        			}
        		}
        		double targetCount = (TRAINING_PARTITION*minimumCount);
        		for(Map.Entry<String, Integer> countEntry : categoryCounts.entrySet())
        		{
        			categoryToProbability.put(countEntry.getKey(), targetCount/countEntry.getValue());
        		}
	    	}
        }
        {
	        int trimPoint = affiliationCounts[0];
	        int trainingPoint = (int)(TRAINING_PARTITION*trimPoint);
	        Random rng = new Random(0);
	        for(int affiliation = STRONGLY_LIBERAL;affiliation <= STRONGLY_CONSERVATIVE;++affiliation)
	    	{
	        	List<File> affiliatedDataFiles = affiliationToDataSet.get(affiliation);
	        	Collections.shuffle(affiliatedDataFiles);
	        	for(int i = 0; i < trimPoint;++i)
	        	{
	        		Pair<BufferedReader, String> datum = new Pair<BufferedReader, String>(new BufferedReader(new FileReader(affiliatedDataFiles.get(i))), Affiliations.codeToName(affiliation).toString());
	        		String referenceCategory = CLASSIFIER_EVALUATOR.transform(Affiliations.codeToName(affiliation).toString());
        			double categoryProbability = categoryToProbability.get(referenceCategory);
	        		if(rng.nextDouble() < categoryProbability)
	        		{
	        			//training
	        			trainingSet.add(datum);
	        		}
	        		else
	        		{
	        			//testing
	        			testingSet.add(datum);
	        		}
//	        		if(i < trainingPoint)
//	        		{
//	        			trainingSet.add(datum);
//
//	        		}
//	        		else
//	        		{
//	        			testingSet.add(datum);
//	        		}
	        	}

	    	}
	        Collections.shuffle(trainingSet, new Random(0));
	        Collections.shuffle(testingSet, new Random(0));
	        System.out.println("Dumping category counts for training set: ");
	        CLASSIFIER_EVALUATOR.dumpCategoryCounts(trainingSet);
	        System.out.println("Dumping category counts for testing set: " );
	        CLASSIFIER_EVALUATOR.dumpCategoryCounts(testingSet);
        }
	}
	
	public static IClassifier train(boolean pPersist) throws Exception
	{
		List<Pair<BufferedReader, String>> trainingSet = new ArrayList<Pair<BufferedReader, String>>();
        List<Pair<BufferedReader, String>> testingSet = new ArrayList<Pair<BufferedReader, String>>();
        //getTrainingAndTestingSet(trainingSet, testingSet);
      
        

        
        
        
        TopicInferencer inferencer = new TopicInferencer(new File(Resource.getLDADirectory(), "reddit-inferencer-250.inferencer")
	 	  												, new File(Resource.getModelsDirectory(), "reddit.mallet")
		  												, 250
		  												);
        
        
        SubjectivityPreprocessor subjPrep = new SubjectivityPreprocessor(inferencer);
        
        for(File file : new File(Resource.baseDataPath, "reddit_data/conservative").listFiles())
        {
        	trainingSet.add(new Pair<BufferedReader, String>(new BufferedReader(new FileReader(file)), SubjectivityPreprocessor.SubjectivityCategories.SUBJECTIVE.toString()));
        	
        }
        for(File file : new File(Resource.baseDataPath, "reddit_data/liberal").listFiles())
        {
        	trainingSet.add(new Pair<BufferedReader, String>(new BufferedReader(new FileReader(file)), SubjectivityPreprocessor.SubjectivityCategories.SUBJECTIVE.toString()));
        	
        }
        subjPrep.train(trainingSet, false, new IdentityCategoryMapper());
        trainingSet.clear();
        
        IPreprocessor pipePreprocessor = new CompositionPreprocessor(  new  PorterStemmerPreprocessor());
        
        //AbstractClassifier<Affiliations> classifier = new TopicInferencerClassifier(inferencer, pipePreprocessor);
        
        //AbstractClassifier<Affiliations> classifier = new BoostedDecisionTreeClassifier(pipePreprocessor);
        //AbstractClassifier<Affiliations> classifier = new BoostedStumpClassifier(pipePreprocessor);
        
        //AbstractClassifier<Affiliations> classifier = new NaiveBayesClassifier(pipePreprocessor);
        //AbstractClassifier<Affiliations> classifier = new NewsClassifier(pipePreprocessor);
//        ClassifierTrainer<? extends Classifier> trainer = 
//        	new MaxEntTrainer();
//        	
//        	//new AdaBoostTrainer( new DecisionTreeTrainer(6), 150);
//        AbstractClassifier<Affiliations> classifier = new MalletNewsClassifier(pipePreprocessor
//        																	  ,trainer 
//        																	  );
        
        AbstractClassifier classifier = new AbstractMinorThirdNewsClassifier(pipePreprocessor) {
			
			
			private static final long serialVersionUID = -5803102867652466430L;

			@Override
			public ClassifierLearner getLearner() {
				return new ManyVsRestLearner( new AdaBoost.L(new DecisionTreeLearner(10,10), 200) );
				//return new Recommended.SVMLearner();
				//return new Recommended.NaiveBayes();
			}
		};
		
        trainingSet.clear();
        testingSet.clear();
        getTrainingAndTestingSet(trainingSet, testingSet, CLASSIFIER_EVALUATOR.getCategories(classifier));
        classifier.train( trainingSet
        				, true
        				, new ICategoryMapper() {
							
							@Override
							public String map(String pString) {
								if(pString.contains("MILDLY"))
								{
									return NON_BIASED;
								}
								if(pString.contains("LIBERAL"))
								{
									return "LIBERAL";
								}
								else
								{
									return "CONSERVATIVE";
								}
							}
							
							@Override
							public String[] getCategories(String[] oldCategories) {
								
								return new String[]{"LIBERAL", "CONSERVATIVE", NON_BIASED};
							}
						}
        				);
        System.out.println();
        
        CLASSIFIER_EVALUATOR.evaluate(classifier, testingSet);
        if(pPersist)
        {
        	classifier.persist(new File(Resource.getModelsDirectory(), "news-classifier.model"));
        }
        return classifier;
	}
	
	@SuppressWarnings("unchecked")
	public static void evaluate() throws Exception
	{
		List<Pair<BufferedReader, String>> trainingSet = new ArrayList<Pair<BufferedReader, String>>();
        List<Pair<BufferedReader, String>> testingSet = new ArrayList<Pair<BufferedReader, String>>();
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(Resource.getModelsDirectory(), "news-classifier.model")));
        IClassifier classifier = (IClassifier)ois.readObject();
        getTrainingAndTestingSet(trainingSet, testingSet, CLASSIFIER_EVALUATOR.getCategories(classifier));
        
        CLASSIFIER_EVALUATOR
		 .evaluate(classifier, testingSet);
	    TopicInferencer inferencer = new TopicInferencer(new File(Resource.getLDADirectory(), "inferencer-250.inferencer")
	 	  , new File(Resource.getModelsDirectory(), "news.mallet")
		  , 250
		  );
		PolarityClassifier polarityClassifier = new PolarityClassifier(inferencer
																	  );
		List<Pair<BufferedReader, String>> universe = new ArrayList<Pair<BufferedReader,String>>();
		universe.addAll(trainingSet);
		universe.addAll(testingSet);
		polarityClassifier.train(universe, new NoopPreprocessor(), new IdentityCategoryMapper());
		String line = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while( (line = br.readLine()) != null)
		{
			URL retrieveUrl = new URL(line);
			System.out.println("Retrieving and classifying " + line + "...");
			String text = NLPUtils.getText(retrieveUrl);
			System.out.println(text);
			System.out.println("---------------------------");
			System.out.println("Is this political? " + polarityClassifier.classify(text));
			System.out.println("Result: " + classifier.classify(text));
			List<List<String>> topics = inferencer.getTopics(text, 3);
			for( List<String> topic : topics)
			{
				String topicStr = new String();
				for(String topicToken : topic)
				{
					topicStr += topicToken + ", ";
				}
				System.out.println("\t" + topicStr);
			}
			
		}
	}
	
    @SuppressWarnings("unchecked")
	public static void main( String[] args ) throws Exception
    {
    	Resource.baseDataPath = new File(args[0]);
    	if(args[1].equalsIgnoreCase("train"))
    	{
    		IClassifier classifier = null;
    		try
    		{
    		 classifier = train(true);
    		}
    		catch(Exception ex)
    		{
    			ex.printStackTrace();
    		}
//    		System.out.println("\n\nVERIFYING AGAINST REDDIT...\n\n");
//    		VerifyAgainstReddit.verify(classifier);
    	}
    	else if(args[1].equalsIgnoreCase("get"))
    	{
    		evaluate();
    	}
    }
}

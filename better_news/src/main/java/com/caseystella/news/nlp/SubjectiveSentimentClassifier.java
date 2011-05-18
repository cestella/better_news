package com.caseystella.news.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import com.aliasi.classify.BaseClassifierEvaluator;
import com.aliasi.classify.Classification;
import com.aliasi.classify.Classified;
import com.aliasi.classify.ConditionalClassification;
import com.aliasi.classify.DynamicLMClassifier;
import com.aliasi.lm.NGramProcessLM;
import com.aliasi.util.BoundedPriorityQueue;
import com.aliasi.util.Files;
import com.aliasi.util.ScoredObject;
import com.caseystella.news.interfaces.Affiliations;
import com.caseystella.news.interfaces.IClassifier;
import com.sun.tools.javac.util.Pair;

public class SubjectiveSentimentClassifier implements IClassifier, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1824545446719759311L;

	public static final int NUM_NGRAMS = 8;
	
	String[] mCategories = new String[] { Affiliations.STRONGLY_LIBERAL.getName()
			   , Affiliations.MILDLY_LIBERAL.getName()
			   , Affiliations.MILDLY_CONSERVATIVE.getName()
			   , Affiliations.STRONGLY_CONSERVATIVE.getName()
			   };
	private DynamicLMClassifier<NGramProcessLM> mClassifier;
    private DynamicLMClassifier<NGramProcessLM> mSubjectivityClassifier;
	
    public SubjectiveSentimentClassifier()
	{
		
		
	}
    
    public SubjectiveSentimentClassifier(DynamicLMClassifier<NGramProcessLM> classifier
    									,DynamicLMClassifier<NGramProcessLM> subjectivityClassifier
    									)
    {
    	mClassifier = classifier;
    	mSubjectivityClassifier = subjectivityClassifier;
    	
    }
	
    @SuppressWarnings("unchecked")
	public SubjectiveSentimentClassifier(File pClassifier, File pSubjectivityClassifier) throws Exception
    {
    	{
	    	FileInputStream fileIn = new FileInputStream(pClassifier);
	        ObjectInputStream objIn = new ObjectInputStream(fileIn);
	        mClassifier = (DynamicLMClassifier<NGramProcessLM>)objIn.readObject();
	    }
    	{
	    	FileInputStream fileIn = new FileInputStream(pSubjectivityClassifier);
	        ObjectInputStream objIn = new ObjectInputStream(fileIn);
	        mSubjectivityClassifier = (DynamicLMClassifier<NGramProcessLM>)objIn.readObject();
	    }
    }
    
    public void trainSubjectivity(File polarityDir) throws IOException {
        int numTrainingChars = 0;
        System.out.println("TRAINING SUBJECTIVITY CLASSIFIER");
        String[] categories = new String[] { "plot", "quote" };
        mSubjectivityClassifier = DynamicLMClassifier
        .createNGramProcess(categories, NUM_NGRAMS);
        
        System.out.println("\nSubjectivity Training.");
        for (int i = 0; i < categories.length; ++i) {
            String category = categories[i];
            Classification classification
                = new Classification(category);
            File file = new File(polarityDir,
            		categories[i] + ".tok.gt9.5000");
            String data = Files.readFromFile(file,"ISO-8859-1");
            String[] sentences = data.split("\n");
            System.out.println("# Sentences " + category + "=" + sentences.length);
            int numTraining = (sentences.length * 9) / 10;
            for (int j = 0; j < numTraining; ++j) {
                String sentence = sentences[j];
                numTrainingChars += sentence.length();
                Classified<CharSequence> classified
                    = new Classified<CharSequence>(sentence,classification);
                mSubjectivityClassifier.handle(classified);
            }
        }
       

        System.out.println("  # Training Cases=" + 9000);
        System.out.println("  # Training Chars=" + numTrainingChars);
    }
    
	public void train(List<Pair<File, Integer>> pTrainingData) throws Exception
	{
		mClassifier = DynamicLMClassifier.createNGramProcess(mCategories, NUM_NGRAMS);
		System.out.println("TRAINING UNDERLYING CLASSIFIER");
		for(Pair<File, Integer> datum : pTrainingData)
		{
			Classification classification
            = new Classification(Affiliations.codeToName(datum.snd).getName());
			 String data = Files.readFromFile(datum.fst,"ISO-8859-1");
			 data = subjectiveSentences(data);
            
             Classified<CharSequence> classified
                 = new Classified<CharSequence>(data,classification);
             mClassifier.handle(classified);
		}
	}
	
	public void evaluate(List<Pair<File, Integer>> pTestingData) throws Exception
	{
		BaseClassifierEvaluator<CharSequence> evaluator = new BaseClassifierEvaluator<CharSequence>(null,mCategories,false);
		for(Pair<File, Integer> datum : pTestingData)
		{
			String data = subjectiveSentences(Files.readFromFile(datum.fst, "ISO-8859-1"));
			Classification classification = mClassifier.classify(data);
			evaluator.addClassification(Affiliations.codeToName(datum.snd).getName(), classification, null);
		}
		
		System.out.println();
		System.out.println(evaluator.toString());
	}
	
	public void persist(File pClassifierFile, File pSubjectivityClassifierFile) throws Exception
	{
		com.aliasi.util.AbstractExternalizable.compileTo(mClassifier, pClassifierFile);
		com.aliasi.util.AbstractExternalizable.compileTo(mSubjectivityClassifier, pSubjectivityClassifierFile);
	}
	
	String subjectiveSentences(String review) {
        String[] sentences = review.split("\n");
        BoundedPriorityQueue<ScoredObject<String>> pQueue 
            = new BoundedPriorityQueue<ScoredObject<String>>(ScoredObject.comparator(),
                                                             MAX_SENTS);
        for (int i = 0; i < sentences.length; ++i) {
            String sentence = sentences[i];
            ConditionalClassification subjClassification
                = (ConditionalClassification) 
                mSubjectivityClassifier.classify(sentences[i]);
            double subjProb;
            if (subjClassification.category(0).equals("quote"))
                subjProb = subjClassification.conditionalProbability(0);
            else
                subjProb = subjClassification.conditionalProbability(1);
            pQueue.offer(new ScoredObject<String>(sentence,subjProb));
        }
        StringBuilder reviewBuf = new StringBuilder();
        Iterator<ScoredObject<String>> it = pQueue.iterator();
        for (int i = 0; it.hasNext(); ++i) {
            ScoredObject<String> so = it.next();
            if (so.score() < .5 && i >= MIN_SENTS) break;
            reviewBuf.append(so.getObject() + "\n");
        }
        String result = reviewBuf.toString().trim();
        return result;
    }

    static int MIN_SENTS = 5;
    static int MAX_SENTS = 25;
	
	@Override
	public Affiliations classify(BufferedReader pInputData) throws Exception
	{
		StringBuffer data = new StringBuffer();
		for(String line = null; (line = pInputData.readLine()) != null;)
		{
			data.append(line + "\n");
		}
		Classification classification = mClassifier.classify(subjectiveSentences(data.toString()));
		return Affiliations.nameToEnum(classification.bestCategory());
		
	}
}

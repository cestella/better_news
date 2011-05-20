package com.caseystella.news.nlp.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;

import com.aliasi.classify.Classification;
import com.aliasi.classify.Classified;
import com.aliasi.classify.ConditionalClassification;
import com.aliasi.classify.DynamicLMClassifier;
import com.aliasi.classify.LMClassifier;
import com.aliasi.util.BoundedPriorityQueue;
import com.aliasi.util.Files;
import com.aliasi.util.ScoredObject;
import com.caseystella.news.interfaces.IPreprocessor;

public class SubjectivityPreprocessor implements IPreprocessor {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1072441849198987921L;
	private static int MIN_SENTS = 5;
    private static int MAX_SENTS = 25;
    public static String SUBJECTIVITY_MODEL = "polarity.model-subjectivity.model";
    public static final int NUM_NGRAMS = 8;
    
    private LMClassifier mSubjectivityClassifier;
    
    @SuppressWarnings("unchecked")
	public SubjectivityPreprocessor(InputStream pInputStream) throws IOException, ClassNotFoundException
    {
    	ObjectInputStream objIn = new ObjectInputStream(pInputStream);
    	mSubjectivityClassifier = (LMClassifier)objIn.readObject();
    }
    
    public SubjectivityPreprocessor()
    {
    	
    }
    
	@Override
	public String transform(String pData) {
		String[] sentences = pData.split("\n");
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
                ((DynamicLMClassifier)mSubjectivityClassifier).handle(classified);
            }
        }
       

        System.out.println("  # Training Cases=" + 9000);
        System.out.println("  # Training Chars=" + numTrainingChars);
    }
	
	public void persist( File pSubjectivityClassifierFile) throws Exception
	{
		if(mSubjectivityClassifier instanceof DynamicLMClassifier)
		{
			com.aliasi.util.AbstractExternalizable.compileTo((DynamicLMClassifier)mSubjectivityClassifier, pSubjectivityClassifierFile);
		}
	}
}

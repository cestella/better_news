package com.caseystella.news.interfaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

import com.caseystella.news.nlp.util.AbstractClassifier;
import com.caseystella.news.nlp.util.NLPUtils;
import com.sun.tools.javac.util.Pair;

public class MalletNewsClassifier extends AbstractClassifier
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 960636295749059155L;

	private Classifier classifier;
	private transient ClassifierTrainer<? extends Classifier> trainer;
	private Pipe pipe;
	
	public MalletNewsClassifier( IPreprocessor pPreprocessor
									   , ClassifierTrainer<? extends Classifier> trainer
										) 
	{
		super(pPreprocessor);
		pipe = buildPipe();
		this.trainer = trainer;
	}

	@Override
	public String classify(String pInputData) throws IOException, Exception {
		InstanceList list = new InstanceList(pipe);
		String data = preprocessor.transform(pInputData);
		Instance instance = new Instance(data, "LIBERAL", UUID.randomUUID().toString(), null);
		list.addThruPipe(instance);
		Classification classification = classifier.classify(list.get(0));
		return classification.getLabeling().getBestLabel().getEntry().toString();
	}
	
	@Override
	public String[] getCategories() {
		return Affiliations.getCategories();
	}

	
	@Override
	public void train(List<Pair<BufferedReader, String>> pTrainingData,
			IPreprocessor pPreprocessor
			, ICategoryMapper pMapper) throws Exception 
	{
		
		InstanceList instances = new InstanceList(pipe);
		for(Pair<BufferedReader, String> pair : pTrainingData)
		{
			String strDat = NLPUtils.toString(pair.fst);
			strDat = pPreprocessor.transform(strDat);
			if(strDat == null || strDat.trim().isEmpty() )
				continue;
			Instance instance = new Instance(strDat, pMapper.map(pair.snd), UUID.randomUUID().toString(), null);
			instances.addThruPipe(instance);
		}
		trainer.train(instances);
		classifier = trainer.getClassifier();
	}
	
	 public Pipe buildPipe() {
	        ArrayList pipeList = new ArrayList();

	        // Read data from File objects
	        pipeList.add(new Input2CharSequence("UTF-8"));

	        // Regular expression for what constitutes a token.
	        //  This pattern includes Unicode letters, Unicode numbers, 
	        //   and the underscore character. Alternatives:
	        //    "\\S+"   (anything not whitespace)
	        //    "\\w+"    ( A-Z, a-z, 0-9, _ )
	        //    "[\\p{L}\\p{N}_]+|[\\p{P}]+"   (a group of only letters and numbers OR
	        //                                    a group of only punctuation marks)
	        Pattern tokenPattern =
	            Pattern.compile("[\\p{L}\\p{N}_]+");

	        // Tokenize raw strings
	        pipeList.add(new CharSequence2TokenSequence(tokenPattern));

	        // Normalize all tokens to all lowercase
	        pipeList.add(new TokenSequenceLowercase());

	        // Remove stopwords from a standard English stoplist.
	        //  options: [case sensitive] [mark deletions]
	        pipeList.add(new TokenSequenceRemoveStopwords(false, false));
	        
	        

	        // Rather than storing tokens as strings, convert 
	        //  them to integers by looking them up in an alphabet.
	        pipeList.add(new TokenSequence2FeatureSequence());

	        // Do the same thing for the "target" field: 
	        //  convert a class label string to a Label object,
	        //  which has an index in a Label alphabet.
	        pipeList.add(new Target2Label());

	        // Now convert the sequence of features to a sparse vector,
	        //  mapping feature IDs to counts.
	        pipeList.add(new FeatureSequence2FeatureVector());

	        return new SerialPipes(pipeList);
	    }
	
}

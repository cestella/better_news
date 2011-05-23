package com.caseystella.news.nlp.classifier;

import com.caseystella.news.interfaces.AbstractMinorThirdNewsClassifier;
import com.caseystella.news.interfaces.IPreprocessor;

import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.ManyVsRestLearner;
import edu.cmu.minorthird.ui.Recommended;

public class BoostedStumpClassifier extends AbstractMinorThirdNewsClassifier {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3632310679474078949L;

	public BoostedStumpClassifier(IPreprocessor pPreprocessor) {
		super(pPreprocessor);
		
	}
	
	

	@Override
	public ClassifierLearner getLearner() {
		
			return new ManyVsRestLearner(new Recommended.BoostedStumpLearner());
	
	}

}

package com.caseystella.news.nlp.classifier;

import com.caseystella.news.interfaces.AbstractMinorThirdNewsClassifier;
import com.caseystella.news.interfaces.IPreprocessor;

import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.ManyVsRestLearner;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;

public class BoostedDecisionTreeClassifier extends
		AbstractMinorThirdNewsClassifier {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1352463130803704970L;

	public BoostedDecisionTreeClassifier(IPreprocessor pPreprocessor) {
		super(pPreprocessor);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ClassifierLearner getLearner() {
		//8,100 worked
		return new ManyVsRestLearner
		   ( 
				   new AdaBoost.L( new edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner(15,8)
		   			     , 150
				   		 )
				
		   );
	}

}

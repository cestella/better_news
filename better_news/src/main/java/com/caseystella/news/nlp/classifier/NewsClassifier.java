package com.caseystella.news.nlp.classifier;

import com.caseystella.news.interfaces.AbstractMinorThirdNewsClassifier;
import com.caseystella.news.interfaces.IPreprocessor;

import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.ManyVsRestLearner;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;

public class NewsClassifier extends AbstractMinorThirdNewsClassifier {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9219110429289285271L;

	public NewsClassifier(IPreprocessor pPreprocessor) {
		super(pPreprocessor);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ClassifierLearner getLearner() {
		//return  new MaxEntLearner("maxIters 25 mForHessians 5 doScaling true");
		
		return new ManyVsRestLearner
		   ( 
				   new AdaBoost( new edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner(8,8)
		   			     , 200
				   		 )
				
		   );
	}

}

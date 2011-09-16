package com.caseystella.news.interfaces;

import java.io.IOException;

import edu.cmu.minorthird.classify.ClassLabel;

public abstract class AbstractMinorThirdNewsClassifier extends
		AbstractMinorThirdClassifier {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5822371802013667242L;

	public AbstractMinorThirdNewsClassifier(IPreprocessor pPreprocessor) {
		super(pPreprocessor);
		// TODO Auto-generated constructor stub
	}

	

	@Override
	public String classify(String pInputData) throws IOException, Exception {
		ClassLabel label = classifier.classification(makeInstance(pInputData, preprocessor));
		if(label.bestClassName() == null)
			return Affiliations.STRONGLY_LIBERAL.toString();
		return label.bestClassName();
	}

	@Override
	public String[] getCategories() {
		// TODO Auto-generated method stub
		return Affiliations.getCategories();
	}

}

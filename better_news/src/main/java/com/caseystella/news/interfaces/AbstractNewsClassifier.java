package com.caseystella.news.interfaces;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import com.sun.tools.javac.util.Pair;

public abstract class AbstractNewsClassifier implements IClassifier {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3962914938094082543L;
	protected String[] mCategories = new String[] { Affiliations.STRONGLY_LIBERAL.getName()
			   , Affiliations.MILDLY_LIBERAL.getName()
			   , Affiliations.MILDLY_CONSERVATIVE.getName()
			   , Affiliations.STRONGLY_CONSERVATIVE.getName()
			   };
	protected IPreprocessor preprocessor;
	public AbstractNewsClassifier(IPreprocessor pPreprocessor)
	{
		preprocessor = pPreprocessor;
	}
	
	

	@Override
	public String[] getCategories() {
		// TODO Auto-generated method stub
		return mCategories;
	}

	@Override
	public void persist(File pClassifierFile) throws Exception {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(pClassifierFile));
		oos.writeObject(this);
	}

	
	public void train(List<Pair<File, Integer>> pTrainingData, boolean pUsePreprocessor) throws Exception
	{
		train(pTrainingData
			 , pUsePreprocessor?preprocessor:new IPreprocessor() {
			
				
				/**
				 * 
				 */
				private static final long serialVersionUID = 6299516734250462357L;

				@Override
				public String transform(String pData) {
					return pData;
				}
			}
			 );
	}

}

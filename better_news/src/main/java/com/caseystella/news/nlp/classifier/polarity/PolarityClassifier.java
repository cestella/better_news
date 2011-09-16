package com.caseystella.news.nlp.classifier.polarity;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.caseystella.news.interfaces.ICategoryMapper;
import com.caseystella.news.interfaces.IPreprocessor;
import com.caseystella.news.nlp.TopicInferencer;
import com.caseystella.news.nlp.preprocessor.NoopPreprocessor;
import com.caseystella.news.nlp.util.AbstractClassifier;
import com.caseystella.news.nlp.util.MahalanobisDistance;
import com.caseystella.news.nlp.util.NLPUtils;
import com.sun.tools.javac.util.Pair;

public class PolarityClassifier extends AbstractClassifier {
	
	 /**
	 * 
	 */
	private static final long serialVersionUID = 8012272984350798793L;
	private TopicInferencer inferencer;
	private MahalanobisDistance distance;
	double cutoffPoint = 0; 
	double THRESHOLD = .7;
	
	public PolarityClassifier( TopicInferencer pInferencer) {
		super(new NoopPreprocessor());
		   inferencer = pInferencer;
		   distance = new MahalanobisDistance(inferencer.getDimension());
	}




	public static enum PoliticalPolarity
	{
		APOLITICAL,
		POLITICAL;
		
		private static String[] categories = {"APOLITICAL", "POLITICAL"};
		public static String[] getCategories() { return categories;}
	}

	@Override
	public String classify(String pInputData) throws IOException,
			Exception {
		double distancePt = distance.distance(inferencer.getVector(pInputData));
		System.out.println("Distance = " + distancePt);
		if(distancePt > cutoffPoint) return PoliticalPolarity.APOLITICAL.toString();
		else return PoliticalPolarity.POLITICAL.toString();
	}

	@Override
	public String[] getCategories() {
		return PoliticalPolarity.getCategories();
	}

	@Override
	public void train(List<Pair<BufferedReader, String>> pTrainingData,
			IPreprocessor pPreprocessor, ICategoryMapper pMapper) throws Exception 
	{
		
		System.out.println("TRAINING OBJECTIVITY CLASSIFIER");
       
        
        List<String> sentenceList = new ArrayList<String>();
        for(Pair<BufferedReader, String> trainingFile : pTrainingData)
        {
        	String data = NLPUtils.toString(trainingFile.fst);
        	distance.add(inferencer.getVector( data));
        	
        		sentenceList.add(data);
        	
        }
        distance.finalizeDistance();
        cutoffPoint = 15;
//        List<Double> distanceVec = new ArrayList<Double>();
//        File pointsFile = new File(Resource.getModelsDirectory(), "polarity.dat");
//        PrintWriter pw = new PrintWriter(pointsFile);
        
//        for(String sentence : sentenceList)
//        {
//        	double distancePt = distance.distance(inferencer.getVector(sentence));
//        	pw.println(distancePt);
//        	distanceVec.add(distancePt);
//        }
        //pw.close();
        //Collections.sort(distanceVec);
        //cutoffPoint = distanceVec.get((int)(distanceVec.size()*THRESHOLD) - 1);
        
	}
}

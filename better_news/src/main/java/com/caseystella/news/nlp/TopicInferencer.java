package com.caseystella.news.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;

import com.caseystella.news.nlp.util.AbstractComponentComparator;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.sun.tools.javac.util.Pair;


public class TopicInferencer implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7903051611323143584L;
	private cc.mallet.topics.TopicInferencer underlyingInferencer;
	private Pipe pipe = null;
	private int dimension = 0;
	Map<Integer, List<String>> topics = null; ;
	
	
	public TopicInferencer(File pInferencer, File pModel, int pDimension) throws IOException
	{
		try
		{
			ObjectInputStream ois = new ObjectInputStream( new FileInputStream(pInferencer)) ;
			underlyingInferencer = (cc.mallet.topics.TopicInferencer)ois.readObject();
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		dimension = pDimension;
		InstanceList list = InstanceList.load(pModel);
	
		pipe = new SerialPipes(new Pipe[] {list.getPipe(), new Noop(list.getDataAlphabet(), null) });
		
		topics = Files.readLines(new File(pInferencer.getParentFile(), "reddit-topics-" + pDimension + ".dat")
					   , Charset.defaultCharset()
					   , new LineProcessor<Map<Integer, List<String>>>() 
					   	{
			Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();
			@Override
			public Map<Integer, List<String>> getResult() {
				return map;
			}
			@Override
			public boolean processLine(String line)
					throws IOException 
			{
				List<String> topic = new ArrayList<String>();
				String tokens[] = line.split(" ");
				for(int i = 2;i < tokens.length;++i)
				{
					topic.add(tokens[i]);
				}
				map.put(Integer.parseInt(tokens[0]), topic);
				return true;
			}
					   	}
					   );
		
	}
	public int getDimension()
	{
		return dimension;
	}
	
	private InstanceList getInstances(String pDoc)
	{
		
		
		InstanceList instanceList = new InstanceList(pipe)
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = -4485336881095115065L;

			@Override
			public Alphabet getTargetAlphabet() {
				return null;
			}
		};
		
		instanceList.addThruPipe(new StringArrayIterator(new String[] { pDoc}));
		return instanceList;
	}
	
	public double[] getVector( String pDoc) throws Exception
	{
		
		int numIterations = 100;
		int thinning = 10;
		int burnIn = 10;
		
		
		InstanceList instanceList = getInstances(pDoc);
		
		
		return underlyingInferencer.getSampledDistribution(instanceList.get(0), numIterations, thinning, burnIn);
	}
	
	public List<List<String>> getTopics(String pDoc, int numTopics) throws Exception
	{
		double[] vector = getVector(pDoc);
		ArrayList<Pair<Integer, Double>> zippedCollection = new ArrayList<Pair<Integer, Double>>();
		for(int i = 0;i < vector.length;++i)
		{
			zippedCollection.add(new Pair<Integer, Double>(i, vector[i]));
		}
		Collections.sort(zippedCollection
						, new AbstractComponentComparator<Pair<Integer, Double>, Double>() 
						{
							@Override
							protected Double getComponent(
									Pair<Integer, Double> pUnderlyingObject) {
								return pUnderlyingObject.snd;
							}
						}
						);
		List<List<String>> topicList = new ArrayList<List<String>>();
		for(int i = 0;i < numTopics;++i)
		{
			
			List<String> topic = topics.get(zippedCollection.get(zippedCollection.size() - 1 - i).fst);
			topicList.add(topic);
		}
		return topicList;
	}
	
}

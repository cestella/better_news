package com.caseystella.news.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;


public class TopicInferencer 
{
	private cc.mallet.topics.TopicInferencer underlyingInferencer;
	private Pipe pipe = null;
	private int dimension = 0;
	
	
	
	public TopicInferencer(File pInferencer, File pModel, int pDimension)
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
		
	}
	public int getDimension()
	{
		return dimension;
	}
	
	public double[] getVector( String pDoc) throws Exception
	{
		
		int numIterations = 100;
		int thinning = 10;
		int burnIn = 10;
		
		
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
		
		
		return underlyingInferencer.getSampledDistribution(instanceList.get(0), numIterations, thinning, burnIn);
	}
	
}

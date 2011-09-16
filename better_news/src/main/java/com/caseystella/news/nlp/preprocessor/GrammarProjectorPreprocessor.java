package com.caseystella.news.nlp.preprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.Streams;
import com.caseystella.news.Resource;
import com.caseystella.news.interfaces.IPreprocessor;
import com.caseystella.news.nlp.util.NLPUtils;

public class GrammarProjectorPreprocessor implements IPreprocessor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3075205809470868154L;
	static TokenizerFactory TOKENIZER_FACTORY = IndoEuropeanTokenizerFactory.INSTANCE;
	private transient HmmDecoder decoder;
	
	@Override
	public String transform(String pData) {
		if(decoder == null)
		{
			try
			{
				FileInputStream fileIn = new FileInputStream(new File(Resource.getModelsDirectory(), "brown.model"));
				ObjectInputStream objIn = new ObjectInputStream(fileIn);
				HiddenMarkovModel hmm = (HiddenMarkovModel) objIn.readObject();
				Streams.closeInputStream(objIn);
				decoder = new HmmDecoder(hmm);
			}
			catch(Exception ex)
			{
				throw new RuntimeException(ex);
			}
		}
		pData = pData.replace('-', ' ');
		
		List<String> sentences = NLPUtils.extractSentences(pData);
		StringBuffer transformedDocument = new StringBuffer();
		for(String sentence : sentences)
		{
		
			char[] cs = sentence.toCharArray();
			Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(cs, 0, cs.length);
			String[] tmpWords = tokenizer.tokenize();
			List<String> tokens = Arrays.asList(tmpWords);
			Tagging<String> tagging = decoder.tag(tokens);
			
			for(int i = 0;i < tagging.size();++i)
			{
				if(tagging.tag(i).startsWith("nn") || tagging.tag(i).startsWith("v"))
				{
					transformedDocument.append(tagging.token(i) + " ");
				}
			}
		}
		return transformedDocument.toString().trim();
	}

}

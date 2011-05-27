package com.caseystella.news.nlp.util;

import java.io.BufferedReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;

public class NLPUtils {
	static final TokenizerFactory TOKENIZER_FACTORY = IndoEuropeanTokenizerFactory.INSTANCE;
	static final SentenceModel SENTENCE_MODEL = new IndoEuropeanSentenceModel();
	static final SentenceChunker SENTENCE_CHUNKER = new SentenceChunker(
			TOKENIZER_FACTORY, SENTENCE_MODEL);

	public static List<String> extractSentences(String pStrings) {
		List<String> retSentences = new ArrayList<String>();
		Chunking chunking = SENTENCE_CHUNKER.chunk(pStrings.toCharArray(), 0, pStrings
				.length());
		Set<Chunk> sentences = chunking.chunkSet();
		if (sentences.size() < 1) {
			System.out.println("No sentence chunks found.");
			return retSentences;
		}
		String slice = chunking.charSequence().toString();
	
		for (Iterator<Chunk> it = sentences.iterator(); it.hasNext();) {
			Chunk sentence = it.next();
			int start = sentence.start();
			int end = sentence.end();
			retSentences.add(slice.substring(start, end));
		}

		return retSentences;
	}
	
	public static String toString(BufferedReader pReader) throws Exception
	{
		StringBuffer buff = new StringBuffer();
		for(String line = null; (line = pReader.readLine()) != null;)
		{
			buff.append(line + "\n");
		}
		return buff.toString().trim();
	}
	
	public static String getText(URL pUrl) throws BoilerpipeProcessingException
	{
		return ArticleExtractor.INSTANCE.getText(pUrl);
		
	}
}

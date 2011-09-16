package com.caseystella.news;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.charset.Charset;

import com.caseystella.news.nlp.util.NLPUtils;
import com.google.common.io.Files;

public class LinkGrabber 
{
	public static void main(String[] pArgs) throws Exception
	{
		File linkFile = new File(pArgs[0]);
		File outputDir = new File(pArgs[1]);
		
		BufferedReader br = new BufferedReader(new FileReader(linkFile));
		int fileNum = 0;
		for(String line = null; (line = br.readLine()) != null;++fileNum)
		{
			try
			{
				URL url = new URL(line);
				File outFile = new File(outputDir, fileNum + ".txt");
				Files.write(NLPUtils.getText(url), outFile, Charset.defaultCharset());
			}
			
			catch(Exception ex)
			{
				System.err.println("Skipping " + line + " ... ");
				ex.printStackTrace();
			}
		}
	}
}

package com.caseystella.news.interfaces;

import java.io.BufferedReader;
import java.io.IOException;

public interface IClassifier {
	public Affiliations classify(BufferedReader pInputData) throws IOException, Exception;
}

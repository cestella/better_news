package com.caseystella.news.interfaces;

import com.caseystella.news.App;

public enum Affiliations {
	STRONGLY_LIBERAL(App.STRONGLY_LIBERAL, "STRONGLY_LIBERAL"),
	MILDLY_LIBERAL(App.MILDLY_LIBERAL, "MILDLY_LIBERAL"),
	MILDLY_CONSERVATIVE(App.MILDLY_CONSERVATIVE, "MILDLY_CONSERVATIVE"),
	STRONGLY_CONSERVATIVE(App.STRONGLY_CONSERVATIVE, "STRONGLY_CONSERVATIVE");
	
	private int affiliationCode;
	private String affiliationName;
	private static String[] mCategories = new String[] { Affiliations.STRONGLY_LIBERAL.getName()
			   , Affiliations.MILDLY_LIBERAL.getName()
			   , Affiliations.MILDLY_CONSERVATIVE.getName()
			   , Affiliations.STRONGLY_CONSERVATIVE.getName()
			   };
	
	Affiliations(int pAffiliationCode, String pAffiliationName)
	{
		affiliationCode = pAffiliationCode;
		affiliationName = pAffiliationName;
	}
	
	public static String[] getCategories()
	{
		return mCategories;
	}
	public int getCode()
	{
		return affiliationCode;
	}
	
	public String getName()
	{
		return affiliationName;
	}
	
	public static Affiliations codeToName(int pCode) throws Exception
	{
		if(pCode == STRONGLY_LIBERAL.getCode())	
			return STRONGLY_LIBERAL;
		else if(pCode == MILDLY_LIBERAL.getCode())
			return MILDLY_LIBERAL;
		else if(pCode == MILDLY_CONSERVATIVE.getCode())
			return MILDLY_CONSERVATIVE;
		else if( pCode == STRONGLY_CONSERVATIVE.getCode())
			return STRONGLY_CONSERVATIVE;
		else
			throw new Exception("Unable to find Affiliations");
		
	}
	
	public static Affiliations nameToEnum(String pName) throws Exception
	{
		if(pName.equalsIgnoreCase(STRONGLY_LIBERAL.getName()))
			return STRONGLY_LIBERAL;
		else if(pName.equalsIgnoreCase(MILDLY_LIBERAL.getName()))
			return MILDLY_LIBERAL;
		else if(pName.equalsIgnoreCase(MILDLY_CONSERVATIVE.getName()))
			return MILDLY_CONSERVATIVE;
		else if(pName.equalsIgnoreCase(STRONGLY_CONSERVATIVE.getName()))
			return STRONGLY_CONSERVATIVE;
		else
			throw new Exception("Unable to find Affiliations");
	}
}

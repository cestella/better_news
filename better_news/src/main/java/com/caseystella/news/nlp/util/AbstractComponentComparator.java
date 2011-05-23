package com.caseystella.news.nlp.util;

import java.util.Comparator;

public abstract class AbstractComponentComparator<T , COMPONENT_T extends Comparable<COMPONENT_T>> implements Comparator<T>
{
	protected abstract COMPONENT_T getComponent(T pUnderlyingObject);
	
	public int compare(T o1, T o2) 
	{
		COMPONENT_T o1Component = getComponent(o1);
		COMPONENT_T o2Component = getComponent(o2);
		if(o1Component == null && o2Component != null)
		{
			return 1;
		}
		else if(o1Component == null && o2Component == null)
		{
			return 0;
			
		}
		else if(o1Component != null && o2Component == null)
		{
			return -1;
		}
		else
		{
			//they're both not null
			return o1Component.compareTo(o2Component);
		}
	}
}

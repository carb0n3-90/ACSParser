package com.x.acs.utility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ListComparator implements Comparator<Map<String,String>> {

	@Override
	public int compare(Map<String, String> map1, Map<String, String> map2) {
		List<String> lst1 = new ArrayList<>(map1.values());
		List<String> lst2 = new ArrayList<>( map2.values());
		
		for (int i = 0; i < lst1.size(); i++) {
			if (!lst1.get(i).equals(lst2.get(i)))
				return lst1.get(i).compareTo(lst2.get(i));
		}
		return lst1.get(0).compareTo(lst2.get(0));
	}
}

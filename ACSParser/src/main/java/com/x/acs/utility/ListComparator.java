package com.x.acs.utility;

import java.util.Comparator;
import java.util.List;

public class ListComparator implements Comparator<List<String>> {
	public int compare(List<String> lst1, List<String> lst2) {
		for (int i = 0; i < lst1.size(); i++) {
			if (!lst1.get(i).equals(lst2.get(i)))
				return lst1.get(i).compareTo(lst2.get(i));
		}
		return lst1.get(0).compareTo(lst2.get(0));
	}
}

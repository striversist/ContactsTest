package com.example.contactstest;

import java.util.List;

public class CloudContactUtils {

	public static String joinWhere(String field, List<?> valueList) {
		if (field == null || valueList == null)
			return null;
		
		if (valueList.isEmpty())
			return "";
		
		String where = "";
		if (valueList.size() == 1) {
			if (valueList.get(0) instanceof String) {
				where = field + "=" + "'" + valueList.get(0) + "'";
			} else {
				where = field + "=" + valueList.get(0);
			}
		} else {
			for (int i=0; i<valueList.size(); ++i) {
				if (valueList.get(i) instanceof String) {
					where += field + "=" + "'" + String.valueOf(valueList.get(i)) + "'";
				} else {
					where += field + "=" + String.valueOf(valueList.get(i));
				}
				if (i != valueList.size() - 1) {	// Not the last
					where += " OR ";
				}
			}
		}
		return where;
	}
}

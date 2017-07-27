package com.x.acs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.x.acs.utility.Utility;

public final class ACSParserConstants {

	static Map<String, String> secParMap = new HashMap<>();
	static List<String> secList;
	static String sourceLoc;
	static String destinationLoc;
	static String dLimiter;
	static Map<String, String> updateDataTagMap = new HashMap<>();
	static Map<String, List<String>> uniqueIDMap = new HashMap<>();
	static Map<String, List<String>> attrMap = new LinkedHashMap<>();

	public static final String SRC_FILE_PREFIX = "SOURCE_FILE_PREFIX";
	public static final String OUT_FILE_PREFIX = "OUT_FILE_PREFIX";
	public static final String SRC_FILE_EXT = "SOURCE_FILE_EXTENSION";
	public static final String OUT_FILE_EXT = "OUT_FILE_EXTENSION";
	public static final String SEC_TAG = "TAG_TO_EACH_SECTION";
	public static final String SEC_UNIQ_ID = "UNIQUE_IDENTIFIER_FOR_EACH_SEC";
	public static final String SRC_LOC = "SOURCE_FILE_PATH";
	public static final String DST_LOC = "DESTINATION_PATH";
	public static final String PAR_TAG_SEC = "PARENT_TAG_FOR_SECTION";
	public static final String DELIMITKEY = "OUT_FILE_DELIMITER";
	public static final String CFG_FILE = "config.properties";
	public static final String TMP_PRFIX = "TEMP_";
	public static final String PAR_KEY = "PARENT_KEY_EACH_REC";
	public static final String PARENTKEY = "Parent_Item";
	public static final String PROCESS_DIR = "DIR_PATH_FOR_SUCCESSFUL_XML";
	public static final String FAIL_DIR = "DIR_PATH_FOR_FAILED_XML";
	public static final String PASS = "PASS";
	public static final String FAIL = "FAIL";
	public static final String JOB_FAIL = "JOB_FAIL";
	public static final String JOB_FAIL_SUB = "JOB_FAIL_SUBJECT";
	public static final String JOB_FAIL_BODY = "JOB_FAIL_CONTENT";
	public static final String FILE_FAIL_SUB = "FILE_FAIL_SUBJECT";
	public static final String FILE_FAIL_BODY = "FILE_FAIL_CONTENT";
	public static final String JOB_FAIL_TO = "JOB_FAIL_TO";
	public static final String FILE_FAIL_TO = "FILE_FAIL_TO";
	public static final String JOB_FAIL_CC = "JOB_FAIL_CC";
	public static final String FILE_FAIL_CC = "FILE_FAIL_CC";
	public static final String EMAIL_FROM = "EMAIL_FROM";
	public static final String EMAIL_HOST = "mail.host";
	public static final String DATA_UPDATE_STRING = "TAGS_FOR_DATA_MANIPULATION";
	public static final String CHG_NUM_KEY = "Change_Number";
	public static final String CHG_TYP_LIST = "CHANGE_ORDERS_TYPE_LIST";

	public static Map<String, String> getSecParMap() {
		return secParMap;
	}

	public static List<String> getSecList() {
		return secList;
	}

	public static String getSourceLoc() {
		return sourceLoc;
	}

	public static String getDestinationLoc() {
		return destinationLoc;
	}

	public static String getdLimiter() {
		return dLimiter;
	}

	public static Map<String, String> getUpdateDataTagMap() {
		return updateDataTagMap;
	}

	public static Map<String, List<String>> getUniqueIDMap() {
		return uniqueIDMap;
	}

	public static Map<String, List<String>> getAttrMap() {
		return attrMap;
	}

	public static String getPass() {
		return PASS;
	}

	public static String getFail() {
		return FAIL;
	}

	public static String getJobFail() {
		return JOB_FAIL;
	}

	private ACSParserConstants() {
	}

	public static void loadConstatnts(Properties prop) {
		dLimiter = prop.getProperty(DELIMITKEY);

		secList = Arrays.asList(prop.getProperty(SEC_TAG).split(","));
		List<String> secAttSet;
		for (String sec : secList) {
			if (!Utility.isEmptyStr(sec)) {
				secAttSet = Arrays.asList(prop.getProperty(sec.toUpperCase() + "_ATTRIBUTE_MAPPING").split(","));
				ACSParserConstants.attrMap.put(sec, secAttSet);
			}
		}

		String[] uniIDArray = prop.getProperty(SEC_UNIQ_ID).split(";");
		for (String uniID : uniIDArray) {
			if (!Utility.isEmptyStr(uniID))
				ACSParserConstants.uniqueIDMap.put(uniID.split(":")[0], Arrays.asList(uniID.split(":")[1].split(",")));
		}

		String[] secParMapping = prop.getProperty(PAR_TAG_SEC).split(";");
		for (String secParmap : secParMapping) {
			if (!Utility.isEmptyStr(secParmap))
				ACSParserConstants.secParMap.put(secParmap.split(":")[0], secParmap.split(":")[1]);
		}

		String[] tagsForDataUpdates = prop.getProperty(DATA_UPDATE_STRING).split(";");
		for (String strTag : tagsForDataUpdates) {
			if (!Utility.isEmptyStr(strTag))
				ACSParserConstants.updateDataTagMap.put(strTag.split(":")[0], strTag.split(":")[1]);
		}
		sourceLoc = prop.getProperty(SRC_LOC);
		destinationLoc = prop.getProperty(DST_LOC);

	}
}

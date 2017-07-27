package com.x.acs;

import static com.x.acs.ACSParserConstants.CFG_FILE;
import static com.x.acs.ACSParserConstants.CHG_NUM_KEY;
import static com.x.acs.ACSParserConstants.CHG_TYP_LIST;
import static com.x.acs.ACSParserConstants.EMAIL_FROM;
import static com.x.acs.ACSParserConstants.FAIL;
import static com.x.acs.ACSParserConstants.FAIL_DIR;
import static com.x.acs.ACSParserConstants.FILE_FAIL_BODY;
import static com.x.acs.ACSParserConstants.FILE_FAIL_CC;
import static com.x.acs.ACSParserConstants.FILE_FAIL_SUB;
import static com.x.acs.ACSParserConstants.FILE_FAIL_TO;
import static com.x.acs.ACSParserConstants.JOB_FAIL;
import static com.x.acs.ACSParserConstants.JOB_FAIL_BODY;
import static com.x.acs.ACSParserConstants.JOB_FAIL_CC;
import static com.x.acs.ACSParserConstants.JOB_FAIL_SUB;
import static com.x.acs.ACSParserConstants.JOB_FAIL_TO;
import static com.x.acs.ACSParserConstants.OUT_FILE_EXT;
import static com.x.acs.ACSParserConstants.OUT_FILE_PREFIX;
import static com.x.acs.ACSParserConstants.PARENTKEY;
import static com.x.acs.ACSParserConstants.PAR_KEY;
import static com.x.acs.ACSParserConstants.PASS;
import static com.x.acs.ACSParserConstants.PROCESS_DIR;
import static com.x.acs.ACSParserConstants.SEC_TAG;
import static com.x.acs.ACSParserConstants.SRC_FILE_EXT;
import static com.x.acs.ACSParserConstants.SRC_FILE_PREFIX;
import static com.x.acs.ACSParserConstants.TMP_PRFIX;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.x.acs.utility.ListComparator;
import com.x.acs.utility.SendMail;
import com.x.acs.utility.Utility;

public class ParserAcsToTxt {
	static Logger logger = Logger.getLogger(ParserAcsToTxt.class);

	public static Logger getLogger() {
		return logger;
	}

	Properties prop = null;
	String changeNum = "";

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd: HH.mm.ss");

	public void init() throws IOException {
		loadPropertyFile();
		ACSParserConstants.loadConstatnts(prop);
		sdf = new SimpleDateFormat(prop.getProperty("DATE_FORMAT"));
		new File(ACSParserConstants.destinationLoc).mkdirs();
	}

	public void loadPropertyFile() throws IOException {
		prop = new Properties();
		String propFileName = CFG_FILE;
		try (FileInputStream file = new FileInputStream(propFileName)) {
			prop.load(file);
			PropertyConfigurator.configure(prop);
			logger.info("config File initialized");
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	public void process() {
		logger.info("ACS Parser Job kicks off @" + (sdf.format(new Date())));
		try {
			List<Path> inputFiles = readInputFiles();
			Path tmpDir = null;
			try {
				tmpDir = Files.createTempDirectory(TMP_PRFIX);
				for (Path file : inputFiles) {
					processEachFile(tmpDir, file);
				}
			} finally {
				deleteTempFiles(tmpDir);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			sendNotificationEmail(JOB_FAIL, "");
		}
		logger.info("ACS Parser Job completed successfully @" + (sdf.format(new Date())));
	}

	private List<Path> readInputFiles() throws IOException {
		List<Path> inputFiles;
		String fileExt = prop.getProperty(SRC_FILE_EXT) == null ? "" : prop.getProperty(SRC_FILE_EXT);
		String filePreFix = prop.getProperty(SRC_FILE_PREFIX) == null ? "" : prop.getProperty(SRC_FILE_PREFIX);
		StringBuilder fileNameExp = new StringBuilder();
		fileNameExp.append(filePreFix);
		fileNameExp.append("*");
		fileNameExp.append(".");
		fileNameExp.append(fileExt);
		logger.trace("Input file criteria:" + fileNameExp);
		inputFiles = Utility.readFilesFromDir(ACSParserConstants.getSourceLoc(), fileNameExp.toString());
		logger.trace("All Input Files: " + inputFiles.size() + "\n" + inputFiles);
		return inputFiles;
	}

	private void processEachFile(Path tmpDir, Path file) {
		logger.trace("Processing File -> " + file.getFileName().toString());
		try {
			Utility.unzip(file.toString(), tmpDir.toAbsolutePath().toString());

			Map<String, Object> xmlInMap = readXmlToMap(tmpDir, file.getFileName().toString());
			logger.trace("Full ATO Map:\n" + xmlInMap);
			
			Map<String, Object> finalMap = getProcessedMap(xmlInMap);
			logger.trace("Processed data Map:\n" + finalMap);
			
			generateOutFile(file.getFileName().toString(), finalMap);
			//moveInputFile(PASS, file);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			logger.error(e.getMessage(), e);
			moveInputFile(FAIL, file);
			sendNotificationEmail(FAIL, file.getFileName().toString());
		}
	}

	private Map<String, Object> readXmlToMap(Path tmpDir, String xmlFileName)
			throws ParserConfigurationException, SAXException, IOException {
		Map<String, Object> finaMap = new HashMap<>();
		File fXmlFile = tmpDir.resolve(xmlFileName).toFile();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();

		Element nl = doc.getDocumentElement();
		getAtoChangeNum(nl);
				
		for (String parTag : prop.getProperty(SEC_TAG).split(",")) {
			NodeList partsList = nl.getElementsByTagName(ACSParserConstants.secParMap.get(parTag));
			HashMap<String, Object> attrDataMap = new HashMap<>();
			for (int i = 0; i < partsList.getLength(); i++) {
				Node n = partsList.item(i);
				String parentItemNum = "";
				if (n instanceof Element)
					parentItemNum = ((Element) n).getElementsByTagName(prop.getProperty(PAR_KEY)).item(0)
							.getTextContent();
				getChildNode(n, attrDataMap, parentItemNum);

			}
			finaMap.put(ACSParserConstants.secParMap.get(parTag), attrDataMap);
		}
		Gson gson = new Gson();
		String json = gson.toJson(finaMap);
		logger.debug(json);
		return finaMap;
	}

	private void getAtoChangeNum(Element nl) {
		String[] changeOrderArray = prop.getProperty(CHG_TYP_LIST).split(",");
		for (String chgType : changeOrderArray) {
			NodeList chgOrderNodeList = nl.getElementsByTagName(chgType.split(":")[0]);
			if (chgOrderNodeList != null) {
				Node coNode = chgOrderNodeList.item(0);
				if (coNode.getNodeType() == Node.ELEMENT_NODE) {
					try {
						changeNum = ((Element) coNode).getElementsByTagName(chgType.split(":")[1]).item(0)
								.getTextContent();
						break;
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
				
			}
		}
	}

	public void getChildNode(Node node, Map<String, Object> attrMap, String parentItemNum) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element ele = ((Element) node);
			logger.trace("getting details for parent Item:" + parentItemNum + "-->" + node.getNodeName());
			if (meetCriteria((Element) node)) {
				if (hasChildElements((Element) node)) {
					HashMap<String, Object> newMap = new HashMap<>();
					NodeList nl = node.getChildNodes();
					for (int i = 0; i < nl.getLength(); i++) {
						getChildNode(nl.item(i), newMap, parentItemNum);
					}
					String chldKey = getChildKey(node);
					attrMap.put(chldKey, newMap);
				} else {
					String tagName = ele.getTagName();
					String tagVal = ele.getTextContent().replaceAll("[\n\r]", " ");

					if (ACSParserConstants.updateDataTagMap.containsKey(tagName)) {
						String tagSplitter = ACSParserConstants.updateDataTagMap.get(tagName);
						String tagDataDelimiter = tagSplitter.split("@")[0];
						String idx = tagSplitter.split("@")[1];
						try {
							tagVal = tagVal.replaceAll(tagDataDelimiter + "+", tagDataDelimiter)
									.split(tagDataDelimiter)[Integer.parseInt(idx)];
						} catch (NumberFormatException e) {
							logger.error(tagName + " index should be a number @ TAGS_FOR_DATA_MANIPULATION property.");
						}
					}
					if (attrMap.containsKey(tagName) && attrMap.get(tagName) != null
							&& attrMap.get(tagName) instanceof String) {
						String newTagVal = attrMap.get(tagName).toString().concat(", ").concat(tagVal);
						attrMap.put(tagName, newTagVal);
					} else {
						attrMap.put(tagName, tagVal);
						attrMap.put(PARENTKEY, parentItemNum);
						attrMap.put(CHG_NUM_KEY, changeNum);
					}

				}
			}
		}
	}

	private boolean meetCriteria(Element eleNode) {
		boolean meetCriteria = true;
		if(!"Parts".equalsIgnoreCase(eleNode.getNodeName())) return true;
		String [] criteria  =prop.getProperty("CRITERIA_TO_FILTER_PARTS").split(";");
		logger.trace("..."+criteria[0]);
		NodeList crNl = eleNode.getElementsByTagName(criteria[0].split(":")[0]);
		if(crNl != null) {
			logger.trace("nodelist not null....");
			Node crNode = crNl.item(0);
			if(crNode != null && crNode.getNodeType() == Node.ELEMENT_NODE) {
				String crNodeVal = ((Element)crNode).getTextContent();
				logger.trace("...--> "+crNode.getNodeName()+": "+crNodeVal);
				String crVal = criteria[0].split(":")[1];
				Pattern pattern = Pattern.compile(crVal, Pattern.CASE_INSENSITIVE);
		        Matcher matcher = pattern.matcher(crNodeVal);
		        meetCriteria = !matcher.lookingAt();
				/*switch (criteria[0].split(":")[1]) {
				case	"equals":
					meetCriteria = crVal.equalsIgnoreCase(crNodeVal);break;
				case	"not equals":
					meetCriteria = !crVal.equalsIgnoreCase(crNodeVal);break;
				case	"like":
					meetCriteria = crNodeVal.indexOf(crVal) > -1;break;
				case	"not like":
					meetCriteria = crNodeVal.indexOf(crVal) == -1;break;
				default: 
						logger.error("Invalid criteria in property file..."+criteria[0]);
				}*/
			}
		}
		logger.trace("meetCriteria? "+meetCriteria);
		return meetCriteria;
	}

	public boolean hasChildElements(Element el) {
		NodeList children = el.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE)
				return true;
		}
		return false;
	}

	private String getChildKey(Node node) {
		String chldKey = node.getNodeName();
		if (ACSParserConstants.uniqueIDMap.containsKey(node.getNodeName())) {
			String uniKey = "";
			for (String uIDTag : ACSParserConstants.uniqueIDMap.get(node.getNodeName())) {
				NodeList uIDList = ((Element) node).getElementsByTagName(uIDTag);
				if (uIDList != null && uIDList.getLength() > 0) {
					Node uIdNode = uIDList.item(0);
					uniKey = uniKey.concat(uIdNode != null ? uIdNode.getTextContent() : "");
				}
			}
			if (!Utility.isEmptyStr(uniKey))
				chldKey = chldKey.concat(":").concat(uniKey);
		}
		return chldKey;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getProcessedMap(Map<String, Object> attrDataMap) {
		Map<String, Object> secMap = new HashMap<>();
		for (String sec : ACSParserConstants.getSecList()) {
			List<List<String>> rows = new ArrayList<>();
			traverseChildMap((Map<String, Object>) attrDataMap.get(ACSParserConstants.secParMap.get(sec)), rows, sec);
			Collections.sort(rows, new ListComparator());
			secMap.put(sec, rows);
		}
		Gson gson = new Gson();
		String json = gson.toJson(secMap);
		logger.debug(json);
		return secMap;
	}

	@SuppressWarnings("unchecked")
	private void traverseChildMap(Map<String, Object> attrDataMap, List<List<String>> rows, String attKey) {
		for (Entry<String, Object> entry : attrDataMap.entrySet()) {
			String key = entry.getKey();
			if (key.indexOf(":") > -1) {
				logger.trace("Processing item ---> " + key.split(":")[1]);
			}
			if (attKey.equalsIgnoreCase(key.split(":")[0])) {
				List<String> cols = getColData(attKey, entry.getValue());
				if (!cols.isEmpty()) {
					rows.add(cols);
				}
			} else {
				if (entry.getValue() instanceof Map<?, ?>)
					traverseChildMap((Map<String, Object>) entry.getValue(), rows, attKey);
			}
		}
	}

	private List<String> getColData(String attKey, Object valObj) {
		List<String> cols = new ArrayList<>();
		if (ACSParserConstants.attrMap.containsKey(attKey)) {
			for (String att : ACSParserConstants.attrMap.get(attKey)) {
				cols.add(getAttValue(att, valObj));
			}
		}
		return cols;
	}

	@SuppressWarnings("unchecked")
	private String getAttValue(String attName, Object attData) {
		String attVal = "";
		if (attData instanceof String)
			attVal = attData.toString();
		else if (attData instanceof Map<?, ?>) {
			Map<String, Object> valMap = (Map<String, Object>) attData;
			if (attName.indexOf("->") > -1) {
				if (valMap.containsKey(attName.split("->")[0])) {
					Object valChldObj = valMap.get(attName.split("->")[0]);
					if (valChldObj != null && valChldObj instanceof Map<?, ?>)
						valMap = (Map<String, Object>) valMap.get(attName.split("->")[0]);
				}
			} else {
				if (valMap.containsKey(attName)) {
					attVal = valMap.get(attName).toString();
				}
			}
			if (attVal.isEmpty()) {
				attVal = drillDownMap(attName, valMap);
			}
		}
		return attVal;
	}

	private String drillDownMap(String attName, Map<String, Object> valMap) {
		String attVal = "";
		for (String attPath : attName.split("->")) {
			for (Entry<String, Object> entry : valMap.entrySet()) {
				if (entry.getKey().split(":")[0].equals(attPath)) {
					Object val = valMap.get(entry.getKey());

					if (val instanceof String) {
						attVal = val.toString();
					} else {
						attVal = getAttValue(attName, val);
					}
				}
			}
		}
		return attVal;
	}

	private void generateOutFile(String inFileName, Map<String, Object> finalMap) throws IOException {

		String outFileName = inFileName.replace(prop.getProperty(SRC_FILE_PREFIX), prop.getProperty(OUT_FILE_PREFIX))
				.replace(prop.getProperty(SRC_FILE_EXT), prop.getProperty(OUT_FILE_EXT));
		Path outFile = Paths.get(ACSParserConstants.getDestinationLoc()).resolve(outFileName);
		logger.trace("Printing out file: " + outFileName);
		Files.write(outFile, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		for (String sec : ACSParserConstants.getSecList()) {
			if (isSecHasData(finalMap.get(sec))) {
				String secHeader = prop.getProperty("OUT_FILE_" + sec.toUpperCase() + "_HEADER");
				secHeader = secHeader.replace("<newline>", System.getProperty("line.separator"));
				Utility.writeInFile(outFile.toString(), secHeader);
				appendSecData(outFile, finalMap.get(sec));
			}
		}

	}

	private void appendSecData(Path outFile, Object dataObj) throws IOException {
		String sRowData = "";
		if (dataObj instanceof List) {
			List<?> dataList = (List<?>) dataObj;
			for (Object row : dataList) {
				sRowData = getRowDataFromMap(row);
				sRowData = sRowData.endsWith(ACSParserConstants.getdLimiter())
						? sRowData.substring(0, sRowData.lastIndexOf(ACSParserConstants.getdLimiter()))
						: sRowData;
				if (!sRowData.isEmpty())
					Utility.writeInFile(outFile.toString(), sRowData);
			}
		}
	}

	private String getRowDataFromMap(Object row) {
		StringBuilder rowData = new StringBuilder();
		if (row instanceof List) {
			List<?> cols = (List<?>) row;
			for (Object col : cols) {
				rowData.append(col.toString()).append(ACSParserConstants.getdLimiter());
			}
		} else {
			rowData.append(row.toString()).append(ACSParserConstants.getdLimiter());
		}
		return rowData.toString();
	}

	private boolean isSecHasData(Object secDatObject) {
		return secDatObject != null && secDatObject instanceof List<?> && !((List<?>) secDatObject).isEmpty();
	}

	private void deleteTempFiles(Path tmpDir) {
		try {
			Utility.recursiveDeleteOnExit(tmpDir);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void moveInputFile(String status, Path file) {
		String dstDir;
		if (PASS.equals(status)) {
			dstDir = prop.getProperty(PROCESS_DIR);
		} else {
			dstDir = prop.getProperty(FAIL_DIR);
		}
		try {
			Utility.moveFile(file.toAbsolutePath().toString(), dstDir);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void sendNotificationEmail(String failType, String fileName) {
		SendMail mailObj = new SendMail(prop);
		String to;
		String cc;
		String sub;
		String content;
		String from = prop.getProperty(EMAIL_FROM);

		if (failType.equals(FAIL)) {
			sub = String.format(prop.getProperty(FILE_FAIL_SUB), fileName);
			cc = prop.getProperty(FILE_FAIL_CC);
			to = prop.getProperty(FILE_FAIL_TO);
			content = String.format(prop.getProperty(FILE_FAIL_BODY), sdf.format(new Date()), fileName);
		} else {
			sub = prop.getProperty(JOB_FAIL_SUB);
			cc = prop.getProperty(JOB_FAIL_CC);
			to = prop.getProperty(JOB_FAIL_TO);
			content = String.format(prop.getProperty(JOB_FAIL_BODY), sdf.format(new Date()));
		}
		try {
			mailObj.sendEmail(sub, content, "", to, cc, from);
		} catch (MessagingException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
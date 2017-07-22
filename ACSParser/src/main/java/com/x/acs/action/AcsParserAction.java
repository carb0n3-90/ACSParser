package com.x.acs.action;

import com.x.acs.ParserAcsToTxt;

public class AcsParserAction {
	public static void main(String[] argv) {
		try {
			ParserAcsToTxt parObj = new ParserAcsToTxt();
			parObj.init();
			parObj.process();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

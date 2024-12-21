package com.pipasoft.overdrivemp3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * parses the .odm file into usable pieces
 * 
 * @author bpipa
 *
 */
public class OdmParserXpath {

	private XPathFactory xpathFactory = XPathFactory.newInstance();
	private String odmSourceString;

	public OdmParserXpath(File odmFile) throws IOException {
		super();
		odmSourceString = FileUtils.readFileToString(odmFile);
	}

	private String getTitle() {
		return odmSourceString.split("<Title>")[1].split("</Title>")[0];	
	}	

	private String getAuthor() {
		return odmSourceString.split("<Creator role=\"Author\" file-as=")[1].split("</Creator>")[0].split(">")[1];	
	}
	
	public String getCoverUrl() {
		return odmSourceString.split("<CoverUrl>")[1].split("</CoverUrl>")[0];			
	}
	
	public String getTitleAuthorString() throws XPathExpressionException, FileNotFoundException {
		String title = getTitle();
		String author= getAuthor();
			
		return (title+"-by-"+author).replaceAll(" ", "_");		
	}
	
	private InputSource getSource() throws FileNotFoundException {
		return new InputSource(new StringReader(odmSourceString));
	}

	public String getMediaId() throws XPathExpressionException, FileNotFoundException {
		XPath xpath = xpathFactory.newXPath();
		return (String) xpath.evaluate("/OverDriveMedia/@id", getSource(), XPathConstants.STRING);
	}

	public String getBaseUrl() throws XPathExpressionException, FileNotFoundException {
		XPath xpath = xpathFactory.newXPath();
		String path =  (String) xpath.evaluate("/OverDriveMedia/Formats/Format/Protocols/Protocol/@baseurl", getSource(), XPathConstants.STRING);
		if (!path.endsWith("/")) {
			path += "/";
		}
		return path;
	}

	public String getAcquisitionUrl() throws XPathExpressionException, FileNotFoundException {
		XPath xpath = xpathFactory.newXPath();
		return (String) xpath.evaluate("/OverDriveMedia/License/AcquisitionUrl", getSource(), XPathConstants.STRING);
	}

	public List<String> getPartsPaths() throws XPathExpressionException, FileNotFoundException {
		List<String> list = new ArrayList<>();
		XPath xpath = xpathFactory.newXPath();
		NodeList parts = (NodeList) xpath.evaluate("/OverDriveMedia/Formats/Format/Parts/Part", getSource(), XPathConstants.NODESET);
		for (int i = 0; i < parts.getLength(); i++) {
			Node node = parts.item(i);
			Element e = (Element) node;
			String filename = e.getAttribute("filename");
			filename = filename.replace("{", "%7B");
			filename = filename.replace("}", "%7D");
			list.add(filename);
		}
		return list;
	}

}

package com.pipasoft.overdrivemp3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class OdmParserXpath {

	private XPathFactory xpathFactory = XPathFactory.newInstance();
	private File odmFile;

	public OdmParserXpath(File odmFile) {
		super();
		this.odmFile = odmFile;
	}

	private InputSource getSource() throws FileNotFoundException {
		//there's no way to reuse this?
		return new InputSource(new FileInputStream(odmFile));
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

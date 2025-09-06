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

	/**
	 * Cosntructor
	 * 
	 * @param odmFile local ODM file
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	public OdmParserXpath(File odmFile) throws IOException {
		super();
		odmSourceString = FileUtils.readFileToString(odmFile);
	}

	/**
	 * gets the URL to the cover
	 *  
	 * @return the book's cover URL
	 */	
	public String getCoverUrl() {
		return odmSourceString.split("<CoverUrl>")[1].split("</CoverUrl>")[0];			
	}
		
	/**
	 * gets an InputSource to the local odm file
	 * 
	 * @return an InputSource to the local odm file
	 * @throws FileNotFoundException
	 */
	private InputSource getSource() throws FileNotFoundException {
		return new InputSource(new StringReader(odmSourceString));
	}

	/**
	 * gets the media id - used to ge the license
	 * 
	 * @return the media id
	 * @throws XPathExpressionException
	 * @throws FileNotFoundException
	 */
	public String getMediaId() throws XPathExpressionException, FileNotFoundException {
		XPath xpath = xpathFactory.newXPath();
		return (String) xpath.evaluate("/OverDriveMedia/@id", getSource(), XPathConstants.STRING);
	}

	/**
	 * gets the base URL for downloading the MP3s
	 * 
	 * @return the base URL
	 * @throws XPathExpressionException
	 * @throws FileNotFoundException
	 */
	public String getBaseUrl() throws XPathExpressionException, FileNotFoundException {
		XPath xpath = xpathFactory.newXPath();
		String path =  (String) xpath.evaluate("/OverDriveMedia/Formats/Format/Protocols/Protocol/@baseurl", getSource(), XPathConstants.STRING);
		if (!path.endsWith("/")) {
			path += "/";
		}
		return path;
	}

	/**
	 * gets the URL to use to get the license
	 * 
	 * @return the base URL to get the license
	 * @throws XPathExpressionException
	 * @throws FileNotFoundException
	 */
	public String getAcquisitionUrl() throws XPathExpressionException, FileNotFoundException {
		XPath xpath = xpathFactory.newXPath();
		return (String) xpath.evaluate("/OverDriveMedia/License/AcquisitionUrl", getSource(), XPathConstants.STRING);
	}

	/**
	 * returns a list of the paths to each MP3
	 * 
	 * @return a List of the paths to each MP3
	 * @throws XPathExpressionException
	 * @throws FileNotFoundException
	 */
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

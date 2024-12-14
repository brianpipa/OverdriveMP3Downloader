package com.pipasoft.overdrivemp3;

import java.io.BufferedReader;
import java.io.File;
import java.io.*;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

public class OverdriveMp3 {

	public static final String OMC = "1.2.0";
	public static final String OS = "10.11.6";
	public static final String USER_AGENT = "OverDrive Media Console"; 
	public static final String BACKWARDS_SECRET = "ELOSNOC*AIDEM*EVIRDREVO";

	private static String licenseContents;
	private static String clientId;
	private static OdmParserXpath xpathParser;
	
	public static void main(String[] args) throws Exception {
		String odmFileName = "Dust_9798212197700_9253919.odm";
		String prefix = odmFileName.replace(".odm", "");
		String licenseFile = odmFileName+".license";
		
		File odmFile = new File("./"+odmFileName);
		System.out.println("Using ODM file: "+odmFile.getCanonicalPath());
		
		xpathParser = new OdmParserXpath(odmFile);

		String downloadBaseUrl = xpathParser.getBaseUrl();
		
		if (!new File(licenseFile).exists()) {
			String clientId = UUID.randomUUID().toString().toUpperCase();		
			String acquisitionUrl = xpathParser.getAcquisitionUrl();			
			String mediaId = xpathParser.getMediaId();			
			String rawHash = String.join("|", clientId, OMC, OS, BACKWARDS_SECRET);			
			String hash = Base64.getEncoder().encodeToString(DigestUtils.sha(rawHash.getBytes("UTF-16LE")));			
			String fullAcquisitionUrl =  acquisitionUrl+"?mediaID="+mediaId+"&ClientID="+clientId+"&OMC="+OMC+"&OS="+OS+"&Hash="+hash;
			
			boolean successGetLicense = acquireLicense(licenseFile, fullAcquisitionUrl); 			
			System.out.println("License acquisition: "+(successGetLicense ? "SUCCESS" : "FAIL"));			
		}
		
		if (new File(licenseFile).exists()) {
			download(prefix, downloadBaseUrl, licenseFile);
		} else {
			System.err.println("License file doesn't exist at "+licenseFile);
		}		
		System.out.println("DONE");
		
	}	
	
	private static boolean acquireLicense(String licenseFile, String fullAcquisitionUrl) throws Exception {
		//make the GET call.
		//set useragent to USER_AGENT
		//follow redirects
		//--compressed    Request compressed response
		
		URL url = new URL(fullAcquisitionUrl);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent",USER_AGENT);
		con.setInstanceFollowRedirects(true);
		
		int status = con.getResponseCode();

		Reader streamReader = null;

		boolean isError = false;
		if (status > 299) {
		    streamReader = new InputStreamReader(con.getErrorStream());
		    isError = true;
		} else {
		    streamReader = new InputStreamReader(con.getInputStream());
		}		

		BufferedReader in = new BufferedReader(streamReader);
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
		    content.append(inputLine);
		}
		in.close();		
		
		con.disconnect();
		
		System.out.println(content.toString());
		
		if (isError) {
			FileUtils.writeStringToFile(new File(licenseFile+".error"), content.toString(), "UTF-8");
			return false;
		} else {
			FileUtils.writeStringToFile(new File(licenseFile), content.toString(), "UTF-8");
			return true;
		}		
	}
	
	private static boolean download(String prefix, String baseUrl, String licenseFile) throws Exception {
		licenseContents = FileUtils.readFileToString(new File(licenseFile), "UTF-8");

		//this is messy but it works
		clientId = licenseContents.split("<ClientID>")[1].split("</ClientID>")[0];;

		File outputDir = new File(".", prefix);
		if (!outputDir.exists()) {
			outputDir.mkdir();
		}
		System.out.println("Output Folder: "+outputDir.getCanonicalPath());

		for (String path : xpathParser.getPartsPaths()) {
			String urlToFile = baseUrl + path;
			downloadOne(urlToFile, outputDir.getCanonicalPath());
		}
		return true;
	}
	
	private static void downloadOne(String urlToDownload, String outputFolder) throws Exception {
		
		String localFilename = urlToDownload.substring(urlToDownload.lastIndexOf("-")+1, urlToDownload.length());

		String fullpathToLocalFile = outputFolder+"/"+localFilename;
		
		if (!(new File(fullpathToLocalFile).exists())) {
			URL url = new URL(urlToDownload);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent",USER_AGENT);
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty("License", licenseContents);
			con.setRequestProperty("ClientID", clientId);
			
			int status = con.getResponseCode();
			if (status == HttpURLConnection.HTTP_OK) {
				InputStream input = con.getInputStream();
				byte[] buffer = new byte[4096];
				int n;

				OutputStream output = new FileOutputStream(fullpathToLocalFile);
				while ((n = input.read(buffer)) != -1) {
				    output.write(buffer, 0, n);
				}
				output.close();
				
				System.out.println("Saved "+fullpathToLocalFile);				
			} else {
				System.err.println("Failed to download "+ urlToDownload+" : HTTP status code = "+status);
			}
			
		} else {
			System.out.println("Already exists, not re-downloading: "+fullpathToLocalFile);
		}		
	}	
}

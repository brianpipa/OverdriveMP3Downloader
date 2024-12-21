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
		printHeader();
		
		if (args.length == 0 || !args[0].endsWith("odm")) {
			System.err.println("Run this with the path to the .odm file after it like java -jar path/to/file.odm");
			System.err.println("Aborting");
			System.exit(1);
		}
		
		String fullOdmFileName = args[0];
		File odmFile = new File(fullOdmFileName);
		if (!odmFile.exists()) {
			System.err.println(fullOdmFileName+" does not exist. Aborting.");
			System.exit(1);			
		}
		
		System.out.println("Using ODM file: "+odmFile.getCanonicalPath());
		
		//create a directory next to the ODM file to put the files into
		String shortOdmName = odmFile.getName();
		String outputDir = odmFile.getCanonicalPath().replace(".odm", "");
		File outputDirFile = new File(outputDir);
		if (!outputDirFile.exists()) {
			outputDirFile.mkdir();
		}
		
		String prefix = shortOdmName.replace(".odm", "");
		String fullPathToLicenseFile = odmFile.getCanonicalPath()+".license";
		
		xpathParser = new OdmParserXpath(odmFile);

		String downloadBaseUrl = xpathParser.getBaseUrl();
		
		//get the license if we need to. Put it next to the .odm file
		if (!new File(fullPathToLicenseFile).exists()) {
			String clientId = UUID.randomUUID().toString().toUpperCase();		
			String acquisitionUrl = xpathParser.getAcquisitionUrl();			
			String mediaId = xpathParser.getMediaId();			
			String rawHash = String.join("|", clientId, OMC, OS, BACKWARDS_SECRET);			
			String hash = Base64.getEncoder().encodeToString(DigestUtils.sha(rawHash.getBytes("UTF-16LE")));			
			String fullAcquisitionUrl =  acquisitionUrl+"?mediaID="+mediaId+"&ClientID="+clientId+"&OMC="+OMC+"&OS="+OS+"&Hash="+hash;
			
			boolean successGetLicense = acquireLicense(fullPathToLicenseFile, fullAcquisitionUrl); 			
			System.out.println("License acquisition: "+(successGetLicense ? "SUCCESS" : "FAIL"));		
			if (!successGetLicense) {
				System.err.println("Could not acquire license. Aborting");
				System.exit(1);							
			}
		}
		
		//at this point we should have the license.
		
		String titleAuthor = xpathParser.getTitleAuthorString(); 
		downloadMp3s(titleAuthor, outputDir, prefix, downloadBaseUrl, fullPathToLicenseFile);		
		downloadCover(titleAuthor, xpathParser.getCoverUrl(), outputDir);
				
		System.out.println("DONE! Files are in "+outputDir);		
	}	
	
	private static void printHeader() {
		System.out.println();
		String line = "***************************************************";
		System.out.println(line);
		System.out.println("Overdrive MP3 Downloader");
		System.out.println("https://github.com/brianpipa/OverdriveMP3Downloader");		
		System.out.println(line);
		System.out.println();
	}
	
	private static boolean acquireLicense(String licenseFile, String fullAcquisitionUrl) throws Exception {		
		URL url = new URL(fullAcquisitionUrl);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent",USER_AGENT);
		con.setInstanceFollowRedirects(true);
		

		Reader streamReader = null;
		boolean isError = false;
		int status = con.getResponseCode();
		
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
		
		if (isError) {
			FileUtils.writeStringToFile(new File(licenseFile+".error"), content.toString(), "UTF-8");
			return false;
		} else {
			FileUtils.writeStringToFile(new File(licenseFile), content.toString(), "UTF-8");
			return true;
		}		
	}
	
	private static boolean downloadMp3s(String titleAuthorString, String outputDir, String prefix, String baseUrl, String licenseFile) throws Exception {
		licenseContents = FileUtils.readFileToString(new File(licenseFile), "UTF-8");

		//this is messy but it works
		clientId = licenseContents.split("<ClientID>")[1].split("</ClientID>")[0];;

		for (String path : xpathParser.getPartsPaths()) {
			String urlToFile = baseUrl + path;
			downloadOneMp3(titleAuthorString, urlToFile, outputDir);
		}
		return true;
	}
	
	private static void downloadOneMp3(String titleAuthorString, String urlToDownload, String outputFolder) throws Exception {
		String localFilename = urlToDownload.substring(urlToDownload.lastIndexOf("-")+1, urlToDownload.length());
		localFilename = localFilename.replace(".mp3", "-"+titleAuthorString+".mp3");
		String fullpathToLocalFile = outputFolder+File.separator+localFilename;		
		downloadFile(urlToDownload, fullpathToLocalFile);		
	}
	
	private static void downloadCover(String titleAuthor, String urlToDownload, String outputFolder) throws Exception {
		String localFilename = titleAuthor+".jpg";
		String fullpathToLocalFile = outputFolder+File.separator+localFilename;
		
		downloadFile(urlToDownload, fullpathToLocalFile);
	}	
	
	private static void downloadFile(String urlToDownload, String fullpathToLocalFile) throws Exception {
				
		if (!(new File(fullpathToLocalFile).exists())) {
			System.out.print("Downloading to "+fullpathToLocalFile);
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
				System.out.println("  DONE");				
			} else {
				System.err.println("Failed to download "+ urlToDownload+" : HTTP status code = "+status);
			}
			
		} else {
			System.out.println("Already exists, not re-downloading: "+fullpathToLocalFile);
		}		
	}	
	
}

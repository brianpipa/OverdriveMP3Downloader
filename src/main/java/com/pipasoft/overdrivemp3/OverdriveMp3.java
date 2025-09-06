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

/**
 * Main class for the application
 * 
 * UPDATE: As of February 1, 2025 (or so), this no longer works. Overdrive has removed the 
 * ability to download the ODM file (it returns a 404 error now). I'm leaving this project 
 * online in case the code helps anyone.
 * 
 * @author bpipa
 *
 */
public class OverdriveMp3 {

	public static final String OMC = "1.2.0";
	public static final String OS = "10.11.6";
	public static final String USER_AGENT = "OverDrive Media Console"; 
	public static final String BACKWARDS_SECRET = "ELOSNOC*AIDEM*EVIRDREVO";

	private static String licenseContents;
	private static String clientId;
	private static OdmParserXpath xpathParser;
	
	/**
	 * Main entry point into the application
	 * 
	 * @param args command lien args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		printHeader();
		
		if (args.length == 0 || !args[0].endsWith("odm")) {
			System.err.println("Run this with the path to the .odm file after it like java -jar path/to/jar/file.jar path/to/file.odm");
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
		String outputDir = odmFile.getCanonicalPath().replace(".odm", "");
		File outputDirFile = new File(outputDir);
		if (!outputDirFile.exists()) {
			outputDirFile.mkdir();
		}
		
		String fullPathToLicenseFile = odmFile.getCanonicalPath()+".license";
		
		xpathParser = new OdmParserXpath(odmFile);

		String downloadBaseUrl = xpathParser.getBaseUrl();
		
		//get the license if we need to. Put it next to the .odm file
		//we can only get the license file ONCE. 
		if (!new File(fullPathToLicenseFile).exists()) {
			String clientId = UUID.randomUUID().toString().toUpperCase();		
			String acquisitionUrl = xpathParser.getAcquisitionUrl();			
			String mediaId = xpathParser.getMediaId();			
			String rawHash = String.join("|", clientId, OMC, OS, BACKWARDS_SECRET);			
			@SuppressWarnings("deprecation")
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
	
		downloadMp3s(outputDir, downloadBaseUrl, fullPathToLicenseFile);		
		downloadCover(xpathParser.getCoverUrl(), outputDir);
				
		System.out.println("DONE! Files are in "+outputDir);		
	}	
	
	/**
	 * prints the opening into header to System.out
	 */
	private static void printHeader() {
		System.out.println();
		String line = "***************************************************";
		System.out.println(line);
		System.out.println("Overdrive MP3 Downloader");
		System.out.println("https://github.com/brianpipa/OverdriveMP3Downloader");		
		System.out.println(line);
		System.out.println();
	}
	
	/**
	 * gets the license file, using the .odm file and write it next to the .odm file
	 * 
	 * @param licenseFile the path to where it should be written
	 * @param fullAcquisitionUrl the URL to get the lcinse from
	 * @return indicates whether or not the license was successfully acquired or not
	 * @throws Exception
	 */
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
			//if it failed, write the error to a file - helpful for debugging
			FileUtils.writeStringToFile(new File(licenseFile+".error"), content.toString(), "UTF-8");
			return false;
		} else {
			//all good, write it
			FileUtils.writeStringToFile(new File(licenseFile), content.toString(), "UTF-8");
			return true;
		}		
	}
	
	/**
	 * Downloads the MP3 files
	 * 
	 * @param outputDir directory to put the files into. There is no check to verify the dir is writable
	 * @param baseUrl teh base URL from the odm file
	 * @param licenseFile path to the local license file that should already be present
	 * @return boolean indicating success
	 * @throws Exception
	 */
	private static boolean downloadMp3s(String outputDir, String baseUrl, String licenseFile) throws Exception {
		licenseContents = FileUtils.readFileToString(new File(licenseFile), "UTF-8");

		//this is messy but it works
		clientId = licenseContents.split("<ClientID>")[1].split("</ClientID>")[0];;

		for (String path : xpathParser.getPartsPaths()) {
			String urlToFile = baseUrl + path;
			downloadOneMp3(urlToFile, outputDir);
		}
		return true;
	}
	
	/**
	 * downloads a single MP3 file
	 * 
	 * @param urlToDownload the URL to download the file from
	 * @param outputDir the directory top write the file
	 * @throws Exception
	 */
	private static void downloadOneMp3(String urlToDownload, String outputDir) throws Exception {
		String localFilename = urlToDownload.substring(urlToDownload.lastIndexOf("-")+1, urlToDownload.length());
		String fullpathToLocalFile = outputDir+File.separator+localFilename;		
		downloadFile(urlToDownload, fullpathToLocalFile);		
	}
	
	/**
	 * downloads the MP3 audiobook cover and writes it locally as cover.jpg
	 *  
	 * @param urlToDownload the URL to the cover
	 * @param outputFolder the folder to write the cover to
	 * @throws Exception
	 */
	private static void downloadCover(String urlToDownload, String outputFolder) throws Exception {
		String localFilename = "cover.jpg";
		String fullpathToLocalFile = outputFolder+File.separator+localFilename;		
		downloadFile(urlToDownload, fullpathToLocalFile);
	}	
	
	/**
	 * Downloads a file from a url and writes it locally
	 *  
	 * @param urlToDownload the URL to download the file from 
	 * @param fullpathToLocalFile the local path to write teh file to
	 * @throws Exception
	 */
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

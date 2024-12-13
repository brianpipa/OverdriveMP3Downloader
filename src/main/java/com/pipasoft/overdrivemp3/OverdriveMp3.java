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
	
	public static void main(String[] args) throws Exception {
		String odmFile = "TheIcarusTwin_9780593912430_10290373.odm";
		String prefix = odmFile.replace(".odm", "");
		String licenseFile = odmFile+".license";
		

		//read in odm as XML so we can parse it			
		String odmContents = FileUtils.readFileToString(new File("./"+odmFile), "UTF-8");
		
		//TODO quick verification on it
		
		String downloadBaseUrl = odmContents.split("baseurl=\"")[1].split("\" />")[0]+"/";
		
		if (!new File(licenseFile).exists()) {
			String clientId = UUID.randomUUID().toString().toUpperCase();
			System.out.println(clientId);		
			
			/// OverDriveMedia/License/AcquisitionUrl/text()
			String acquisitionUrl = odmContents.split("<AcquisitionUrl>")[1].split("</AcquisitionUrl>")[0];
			
			// string(/OverDriveMedia/@id)
			String mediaId = odmContents.split("<OverDriveMedia id=\"")[1].split("\" ODMVersion")[0];
			
			//+ RawHash='59DF6871-D542-4AE3-8A4B-95B03F196511|1.2.0|10.11.6|ELOSNOC*AIDEM*EVIRDREVO'
			String rawHash = String.join("|", clientId, OMC, OS, BACKWARDS_SECRET);
			System.out.println("rawHash: "+rawHash);
			
			String hash = Base64.getEncoder().encodeToString(DigestUtils.sha(rawHash.getBytes("UTF-16LE")));
			System.out.println("hash: "+hash);
			
			//http_code=$(curl "${CURLOPTS[@]}" -o "$2" -w '%{http_code}' "$AcquisitionUrl?MediaID=$MediaID&ClientID=$ClientID&OMC=$OMC&OS=$OS&Hash=$Hash")
			String fullAcquisitionUrl =  acquisitionUrl+"?mediaID="+mediaId+"&ClientID="+clientId+"&OMC="+OMC+"&OS="+OS+"&Hash="+hash;
			System.out.println("fullAcquisitionUrl: "+fullAcquisitionUrl);
			
			boolean successGetLicense = acquireLicense(licenseFile, fullAcquisitionUrl); 
			
			System.out.println(successGetLicense ? "SUCCESS" : "FAIL");			
		}
		
		
		if (new File(licenseFile).exists()) {
			download(prefix, downloadBaseUrl, licenseFile, odmContents);
		}
		
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
	
	private static boolean download(String prefix, String baseUrl, String licenseFile, String odmContents) throws Exception {
		licenseContents = FileUtils.readFileToString(new File(licenseFile), "UTF-8");
		
		clientId = licenseContents.split("<ClientID>")[1].split("</ClientID>")[0];
		System.out.println(clientId);
		
		File outputDir = new File(".", prefix);
		if (!outputDir.exists()) {
			outputDir.mkdir();
		}
		
		String parts = odmContents.split("<Parts")[1].split("</Parts>")[0];
		System.out.println("parts: "+parts);
		
		String[] paths = parts.split("\" duration=");
		
		for (String pathPiece : paths ) {
			if (pathPiece.contains("filename=")) {
				String pathToDownload = pathPiece.split("filename=\"")[1];
				pathToDownload = pathToDownload.replace("{", "%7B");
				pathToDownload = pathToDownload.replace("}", "%7D");				
				String urlToFile = baseUrl+pathToDownload;
				System.out.println("urlToFile: "+urlToFile);
				downloadOne(urlToFile, outputDir.getAbsolutePath());
			}
		}
		//get parts to grab and make URLs
		//in filenames, convert:
		// { -> %7B
		// } -> %7D
		
		return true;
	}
	
	private static void downloadOne(String urlToDownload, String outputFolder) throws Exception {
		System.out.println("url: "+urlToDownload);
		System.out.println("outputFolder: "+outputFolder);
		
		String localFilename = urlToDownload.substring(urlToDownload.lastIndexOf("-")+1, urlToDownload.length());
		System.out.println("localFilename: "+localFilename);

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
			
			InputStream input = con.getInputStream();
			byte[] buffer = new byte[4096];
			int n;

			OutputStream output = new FileOutputStream(fullpathToLocalFile);
			while ((n = input.read(buffer)) != -1) 
			{
			    output.write(buffer, 0, n);
			}
			output.close();			
		} else {
			System.out.println(fullpathToLocalFile+" already exists");
		}
		
		
		
	}
	
}

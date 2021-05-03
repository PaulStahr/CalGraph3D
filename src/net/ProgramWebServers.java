/*******************************************************************************
 * Copyright (c) 2019 Paul Stahr
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import io.StreamUtil;
import util.ArrayTools;
import util.EmptyList;
import util.StringUtils;

public class ProgramWebServers {
	private static final ArrayList<ProgramWebServer> servers = new ArrayList<ProgramWebServer>();
	private static final Logger logger = LoggerFactory.getLogger(ProgramWebServers.class);
	
	static{
		try {
			InputStream inStream = DataHandler.getResource("servers.txt").openStream();
			InputStreamReader reader = new InputStreamReader(inStream);
			BufferedReader inBuf = new BufferedReader(reader);
			String line;
			while ((line = inBuf.readLine())!= null){
				try {			
					servers.add(new ProgramWebServer(new URL(line)));
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			inBuf.close();
			reader.close();
			inStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static final void readAll(){
		for (int i=0;i<servers.size();i++){
			final ProgramWebServer tmp = servers.get(i);
			if (!tmp.read){
				DataHandler.runnableRunner.run(new Runnable() {				
					@Override
					public void run() {
						if (!tmp.read())
							logger.error("Can't download informations from server:" + tmp.url.toString());
					}
				}, "Network");
			}	
		}
	}
	
	public static final void readAllAndWait() throws InterruptedException{
		readAll();
		Thread.sleep(5);
		waitForReading();
	}
	
	public static final void waitForReading() throws InterruptedException{
		for (int i=0;i<servers.size();i++){
			ProgramWebServer server = servers.get(i);
			while (server.isReading)
				Thread.sleep(1);
		}		
	}
	
	public static final ProgramDownload[] getAviableDownloads() throws InterruptedException{
		ArrayList<ProgramDownload> downloads = new ArrayList<ProgramDownload>();
		readAllAndWait();
		for (int i=0;i<servers.size();i++){
			ProgramWebServer server = servers.get(i);
			if (!server.read)
				continue;
			List<ProgramDownload> l = server.getAviableProgramDownloads();
			for (int j=0;j<l.size();j++){
				add:{
					ProgramDownload toAdd = l.get(j);
					for (int k=0;k<downloads.size();k++){
						ProgramDownload inList = downloads.get(k);
						if (toAdd.fullName.equals(inList.fullName)){
							if (inList.versionNumber>toAdd.versionNumber)
								downloads.set(j, toAdd);
							break add;
						}
					}
					downloads.add(toAdd);
				}
			}
		}
		return downloads.toArray(new ProgramDownload[downloads.size()]);
	}

	public static final Changelog getChangelog() throws InterruptedException{
		readAll();
		Changelog selected = null;
		for (int i=0;i<servers.size();i++){
			Changelog chLog = servers.get(i).changelog;
			if (chLog != null && (selected == null || chLog.versionNumber > selected.versionNumber))
				selected = chLog;
		}
		if (selected == null){
			waitForReading();
			for (int i=0;i<servers.size();i++){
				Changelog chLog = servers.get(i).changelog;
				if (chLog != null && (selected == null || chLog.versionNumber > selected.versionNumber))
					selected = chLog;
			}
		}
		return selected;
	}
	
	public static final class ProgramWebServer{
		private static final List<ProgramDownload> EMPTY_PROGRAM_DOWNLOAD_LIST = new EmptyList<ProgramWebServers.ProgramDownload>();
		private final URL url;
		private Document doc = null;
		private String lastUpdateTime = null;
		private List<ProgramDownload> aviableDownloads = EMPTY_PROGRAM_DOWNLOAD_LIST;
		private boolean read = false;
		private boolean isReading = false;
		private Changelog changelog;
		private ProgramWebServer(URL url){
			this.url = url;
		}
		
		public final boolean read(){
			isReading = true;
	        try {
	        	doc = new SAXBuilder().build(url);   
	        	Element elemRoot = doc.getRootElement();
	        	Element elemLastUpdate = elemRoot.getChild("last_update");
	        	if (elemLastUpdate!=null)
	        		lastUpdateTime = elemLastUpdate.getValue();
	        	Element elemDownload = elemRoot.getChild("program_downloads");
	        	if (elemDownload != null){
	        		List<Element> downloadElems = elemDownload.getChildren();
	        		ArrayList<ProgramDownload> al = new ArrayList<ProgramDownload>();
		        	for (int i=0;i<downloadElems.size();i++){
		        		List<Element> programVersionElem = downloadElems.get(i).getChildren();
		        		for (int j=0;j<programVersionElem.size();j++){
		        			ArrayList<FileHash> hashes = new ArrayList<FileHash>(0);
		        			Element elem = programVersionElem.get(j);
		        			Element elemHash = elem.getChild("hash");
		        			if (elemHash != null){
			        			List<Element> elemHashes = elemHash.getChildren();
			        			for (int k=0;k<elemHashes.size();k++){
			        				Element elemHashItem = elemHashes.get(k);
			        				try{
			        					hashes.add(new FileHash(elemHashItem.getName(), new BigInteger(elemHashItem.getValue(), 16)));
			        				}catch(NumberFormatException e){}
			        			}
		        			}
			        		al.add(new ProgramDownload(elem.getAttributeValue("location"), elem.getAttributeValue("platform"), elem.getAttributeValue("version"), elem.getAttributeValue("name"), hashes.toArray(new FileHash[hashes.size()])));
		        		}
		        	}
		        	aviableDownloads = ArrayTools.unmodifiableList(al.toArray(new ProgramDownload[al.size()]));
	        	}
	        	Element changelogElem = elemRoot.getChild("changelog");
	        	if (changelogElem != null)
	        		changelog = new Changelog(changelogElem.getAttributeValue("location"), changelogElem.getAttributeValue("version"));
	            read = true;
	 		} catch (Exception e) {}
			isReading = false;
			return read;
		}
		
		public final boolean isReading(){
			return isReading;
		}

		public final String getUpdateTime(){
			return lastUpdateTime;
		}
		
		public List<ProgramDownload> getAviableProgramDownloads(){
			return aviableDownloads;
		}		
	}
	
	public static final class Changelog{
		public final String location;
		public final long versionNumber;
		public final String version;
		private String stringData;
		
		public Changelog(String location, String version){
			this.location = location;
			this.version = version;
			this.versionNumber = StringUtils.getLongOfVersion(version);
		}
		
		public final String read(){
			read:{
				if (stringData != null)
					break read;
				synchronized(this){
					if (stringData != null)
						break read;
					try {
						InputStream stream = new URL(location).openStream();
						stringData = StreamUtil.readStreamToString(stream);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return stringData;
		}
	}
	
	public static final class ProgramDownload{
		public final String location;
		public final String system;
		public final String version;
		public final long versionNumber;
		public final String fullName;
		public final List<FileHash> hashes;
		
		private ProgramDownload(String location, String system, String version, String fullName, FileHash fileHash[]){
			this.location = location;
			this.system = system;
			this.fullName = fullName;
			this.versionNumber = StringUtils.getLongOfVersion(this.version = version);
			this.hashes = ArrayTools.unmodifiableList(fileHash);
		}
		
		public final BigInteger getHash(String type){
			for (int i=0;i<hashes.size();i++){
				FileHash hash = hashes.get(i);
				if (hash.hashType.equals(type))
					return hash.hash;
			}
			return null;
		}
	}
	
	public static final class FileHash{
		public final String hashType;
		public final BigInteger hash;
		
		public FileHash(String hashType, BigInteger hash){
			this.hashType = hashType;
			this.hash = hash;
		}
		
	}
}

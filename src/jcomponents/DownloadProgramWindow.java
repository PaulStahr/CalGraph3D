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
package jcomponents;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.JFrameUtils;
import net.ProgramWebServers;
import net.ProgramWebServers.ProgramDownload;
import data.DataHandler;
import data.ProgramIcons;
import jcomponents.util.JComponentSingletonInstantiator;

public class DownloadProgramWindow extends JFrame{
	private static final long serialVersionUID = 7466229601504265740L;
	private static final Logger logger = LoggerFactory.getLogger(DownloadProgramWindow.class);
	private JButton downloadButtons[];
	private final JButton buttonClose = new JButton ("Schliessen");
	private final JFileChooser fileChooser = new JFileChooser();
	private static final JComponentSingletonInstantiator<JFrame> instantiator = new JComponentSingletonInstantiator<JFrame>(DownloadProgramWindow.class);
	
	public DownloadProgramWindow(){
		setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
		DataHandler.runnableRunner.run(
			new Runnable(){
				@Override
				public void run(){
					try {
						ProgramDownload pd[] = ProgramWebServers.getAviableDownloads();
						downloadButtons = new JButton[pd.length];
						for (int i=0;i<pd.length;i++){
							ProgramDownload tmp = pd[i];
							JButton button = (downloadButtons[i] = new JButton(tmp.fullName));
							button.setToolTipText("<html>Platform:" + tmp.system + "<br />Version:" + tmp.version + "</html>");
							BigInteger hash = tmp.getHash("md5");
							button.addActionListener(new DownloadListener(tmp.location, button, hash, "md5"));
							add(button);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}					
				}
			}
		,"Download Program versions");

		buttonClose.addActionListener(JFrameUtils.closeParentWindowListener);		
		add(buttonClose);
		pack();
		setSize(200,downloadButtons.length * 25 + 25);
		setIconImage(ProgramIcons.webIcon.getImage());
		setMinimumSize(getPreferredSize());
		setResizable(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
    
	private class DownloadListener implements ActionListener{
		private final String downloadLocation;
		private final JButton button;
		private final BigInteger hash;
		private final String hashFunction;
		private DownloadListener(String downloadLocation, JButton button, BigInteger hash, String hashfunction){
			this.downloadLocation = downloadLocation;
			this.button = button;
			this.hash = hash;
			this.hashFunction = hashfunction;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fileChooser.setSelectedFile(new File(downloadLocation));
            if(fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
            	return;
            final File file= fileChooser.getSelectedFile();
            DataHandler.runnableRunner.run(new Runnable(){
        		@Override
				public void run(){
        			button.setEnabled(false);
            		try {
        	    		if (file.exists())
        	    			file.delete();
        	    		file.createNewFile();
        				final FileOutputStream outStream = new FileOutputStream(file);
        				final URL url =  new URL(downloadLocation);
        				final InputStream inStream = url.openStream();
        				final byte data[] = new byte[8196];
        				final String text = button.getText();
        				int read, progress = 0, progressPercent = -1;
        				final StringBuilder strBuilder = new StringBuilder();
        				final int length = url.openConnection().getContentLength(), pastePos = strBuilder.append(text).append(' ').length();
        				MessageDigest md = null;
        				try {
        					 md = MessageDigest.getInstance(hashFunction);
        				} catch (NoSuchAlgorithmException e) {
        					logger.error("Can't use hash to control Downloaded file", DownloadProgramWindow.class);
        				}
        				while ((read = inStream.read(data)) > 0){
        					if (md != null)
        						md.update(data, 0, read);
        					outStream.write(data, 0, read);
        					strBuilder.setLength(pastePos);
        					if (progressPercent != (progress += read) * 100 / length)
        						button.setText(strBuilder.append(progressPercent = progress * 100 / length).append('%').toString());
        				}
              				
        				button.setText(text);
        				button.setSelected(false);
        				inStream.close();
        				outStream.close();
        				BigInteger calculatedHash = new BigInteger(1, md.digest());
        				if (md != null && !calculatedHash.equals(hash) && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "<html>Der Hashcode des Servers stimmt nicht mit der heruntergeladenen Datei \u00FCberein.<br />Dies kann auf einen Fehler beim Herunteladen hindeuten.<br />Soll die Datei gel\u00F6scht werden?</html>","Fehler", JOptionPane.YES_NO_OPTION))
        					file.delete();
               			} catch (IOException e) {
        				logger.error("Can't download new Version:", e);
        			}
        			button.setEnabled(true);
        		}            	
            }, "Download Program");                    
		}
	}

	public static ActionListener getOpenWindowListener() {
		return instantiator;
	}	
}

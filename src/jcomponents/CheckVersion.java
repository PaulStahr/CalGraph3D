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

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import data.DataHandler;
import data.ProgramIcons;
import data.ProgrammData;
import jcomponents.util.JComponentSingletonInstantiator;
import net.ProgramWebServers;
import net.ProgramWebServers.Changelog;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public class CheckVersion extends JFrame implements Runnable
{
	private static final long serialVersionUID = -6479080661365866948L;
    private static final JComponentSingletonInstantiator<CharacterTable> instantiator = new JComponentSingletonInstantiator<CharacterTable>(CharacterTable.class);
	private final JLabel labelOwnVersion 		= new JLabel("Ihre Version");
    private final JLabel versionOwnVersion 		= new JLabel(ProgrammData.getVersion());
    private final JLabel labelActualVersion 	= new JLabel("Aktuelle Version:");
    private final JLabel versionActualVersion 	= new JLabel("L\u00E4d");
    private final JEditorPane editorPaneChangelog= new JEditorPane();
    private final JScrollPane scrollPaneChangelog= new JScrollPane(editorPaneChangelog);
    private final JButton buttonDownload 		= new JButton("Herunterladen");    
    boolean isDownloading = false;
    @Override
	public void run (){
        try {
	    	Changelog chLog = ProgramWebServers.getChangelog();
	    	final boolean isNewer = chLog != null && ProgrammData.isNewer(chLog.version);
	        versionActualVersion.setText(isNewer ? chLog.version.concat(" (aktueller)") : chLog.version);
			editorPaneChangelog.setText(chLog.read());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        isDownloading = false;
     }

    public static final ActionListener getOpenWindowListener()
    {
    	return instantiator;
    }
    
    public static final synchronized JFrame getInstance(){
    	return instantiator.get();
    }
    
    public CheckVersion(){
        final GroupLayout layout = new GroupLayout(getContentPane());
        setLayout(layout);
        buttonDownload.addActionListener(DownloadProgramWindow.getOpenWindowListener());
         
        layout.setHorizontalGroup(layout.createSequentialGroup()
        	.addGap(5).addGroup(layout.createParallelGroup()
	        	.addGroup(layout.createSequentialGroup()
	        		.addGroup(layout.createParallelGroup()
	        			.addComponent(labelOwnVersion)
	        			.addComponent(labelActualVersion)
	        		).addGap(5).addGroup(layout.createParallelGroup()
	        			.addComponent(versionOwnVersion)
	        			.addComponent(versionActualVersion)
	        		).addGap(5).addComponent(buttonDownload)
	        	).addComponent(scrollPaneChangelog)
        	).addGap(5)
        );
       
        layout.setVerticalGroup(layout.createSequentialGroup()
        	.addGap(5).addGroup(layout.createParallelGroup()
        		.addComponent(labelOwnVersion, Alignment.CENTER)
        		.addComponent(versionOwnVersion, Alignment.CENTER)
        	).addGap(5).addGroup(layout.createParallelGroup()
        		.addComponent(labelActualVersion, Alignment.CENTER)
        		.addComponent(versionActualVersion, Alignment.CENTER)
        		.addComponent(buttonDownload, Alignment.CENTER)
        	).addGap(5).addComponent(scrollPaneChangelog, 0, 300, 10000).addGap(5)
        );

        editorPaneChangelog.setContentType("text/html"); 
        editorPaneChangelog.setText("<html><body>L\u00E4d</body></html>");
        editorPaneChangelog.setEditable(false);

        setTitle("Version");
        setResizable(true);      

		setIconImage(ProgramIcons.webIcon.getImage());
        setMinimumSize(new Dimension(500, 200));
        pack();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        DataHandler.addToUpdateTree(this);
    }
    
    
	@Override
	public void setVisible(boolean vis){
    	if (vis && !isDownloading){
    		isDownloading = true;
    		DataHandler.runnableRunner.run(this, "Download Info");
    	}
    	super.setVisible(vis);
    }
}

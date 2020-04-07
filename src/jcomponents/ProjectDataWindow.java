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

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import data.DataHandler;
import util.JFrameUtils;
import data.Project;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class ProjectDataWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = -8104574201311985756L;
	private final JLabel labelName           = new JLabel("Name:");
    private final JTextField textFieldName   = new JTextField();
    private final JLabel labelAuthor         = new JLabel("Autor:");
    private final JTextField textFieldAuthor = new JTextField();
    private final JLabel labelDescription    = new JLabel("Beschreibung:");
    private final JTextArea textAreaDescription = new JTextArea();
    private final JScrollPane scrollPaneDescription = new JScrollPane(textAreaDescription);
    private final JButton buttonOk           = new JButton("OK");
    private final JButton buttonAccept       = new JButton("\u00DCbernehmen");
    private final JButton buttonCancel       = new JButton("Abbrechen");
	private final Project project;

    public void actionPerformed(ActionEvent e){
    	Object source = e.getSource();
    	if (source == buttonOk)
    	{
    		applyChanges();
    		dispose();
    	}
    	else if (source == buttonAccept)
    	{
    		applyChanges();
    	}
    		
    }
    
    public ProjectDataWindow(Project project){
    	this.project = project;
        GroupLayout layout = new GroupLayout(getContentPane());
        setLayout(layout);

        textAreaDescription.setWrapStyleWord(true);
        textAreaDescription.setLineWrap(true);

        buttonOk.addActionListener(this);
        buttonAccept.addActionListener(this);

        buttonCancel.addActionListener(JFrameUtils.closeParentWindowListener);
 
        layout.setHorizontalGroup(
        	layout.createParallelGroup()
        	.addGroup(layout.createSequentialGroup()
        		.addGroup(layout.createParallelGroup()
        			.addComponent(labelName)
        			.addComponent(labelAuthor)
        			.addComponent(labelDescription)
        		).addGroup(layout.createParallelGroup()
        			.addComponent(textFieldName, 10, 20, 1000)
        			.addComponent(textFieldAuthor, 10, 20, 1000)
        			.addComponent(scrollPaneDescription, 10, 20, 1000)
        		)		
        	).addGroup(layout.createSequentialGroup()
        		.addComponent(buttonOk, buttonOk.getPreferredSize().width, buttonOk.getPreferredSize().width, 1000)
        		.addComponent(buttonAccept, buttonAccept.getPreferredSize().width, buttonAccept.getPreferredSize().width, 1000)
        		.addComponent(buttonCancel, buttonCancel.getPreferredSize().width, buttonCancel.getPreferredSize().width, 1000)
        	)
        );
        
        layout.setVerticalGroup(
        	layout.createSequentialGroup()
        	.addGroup(layout.createParallelGroup()
        		.addComponent(labelName, Alignment.CENTER)
        		.addComponent(textFieldName, Alignment.CENTER, 25, 25, 25)
        	).addGroup(layout.createParallelGroup()
            	.addComponent(labelAuthor, Alignment.CENTER)
            	.addComponent(textFieldAuthor, Alignment.CENTER, 25, 25, 25)
            ).addGroup(layout.createParallelGroup()
            	.addComponent(labelDescription, Alignment.CENTER)
            	.addComponent(scrollPaneDescription, 50, 100, 10000)
            ).addGroup(layout.createParallelGroup()
            	.addComponent(buttonOk)
            	.addComponent(buttonAccept)
            	.addComponent(buttonCancel)
            )
        );

        pack();
        setBounds(10,10, 305, 230);
        setTitle("Projekteinstellungen");
        setResizable(true);
        setMinimumSize(getPreferredSize());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        DataHandler.addToUpdateTree(this);
    }

    /**
     * \u00D6ffnet das Fenster
     */
    
	@Override
	public void setVisible(boolean vis){
    	if (vis){
	        textFieldName.setText(project.name);
	        textFieldAuthor.setText(project.author);
	        textAreaDescription.setText(project.description);
    	}
        super.setVisible(vis);
    }

    private void applyChanges(){
    	project.name =			textFieldName.getText();
    	project.author =		textFieldAuthor.getText();
    	project.description =	textAreaDescription.getText();
    }
}

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import jcomponents.util.JComponentSingletonInstantiator;
import util.JFrameUtils;

import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;

/**
 * Write a description of class CharakterTable here.
 * 
 * @author Paul Stahr
 * @version 04.02.2012
 */
public class CharacterTable extends JFrame implements ActionListener, KeyListener
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -906479873322525801L;
	private static final Logger logger = LoggerFactory.getLogger(CharacterTable.class);
	private static final JComponentSingletonInstantiator<CharacterTable> instantiator = new JComponentSingletonInstantiator<CharacterTable>(CharacterTable.class);
	private final JPanel panelCharacterButtons = new JPanel(JFrameUtils.LEFT_FLOW_LAYOUT);
    private final JLabel textFieldDescription = new JLabel();
    private final JButton buttonCopy = new JButton("Kopieren");
    private char currentCharacter;
    
    public static final ActionListener getOpenWindowListener()
    {
    	return instantiator;
    }
    
    @Override
	public void actionPerformed(ActionEvent ae){
    	Object source = ae.getSource();
    	if (source == buttonCopy)
    	{
    		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(String.valueOf(currentCharacter)), null);
    	}
    	else
    	{
	        final JCharacterToggleButton button = (JCharacterToggleButton)source;
	        currentCharacter = button.c;
	        textFieldDescription.setText(button.getDescription());
    	}
    }
    @Override
	public void keyPressed(KeyEvent ke) {
		if (ke.isControlDown() && ke.getKeyCode() == KeyEvent.VK_C)
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(String.valueOf(currentCharacter)), null);
	}

	@Override
	public void keyReleased(KeyEvent arg0) {}

	@Override
	public void keyTyped(KeyEvent arg0) {}
    public CharacterTable(){
        final ButtonGroup characterButtonGroup = new ButtonGroup();
    	addKeyListener(this);
    	
        setLayout(new GridBagLayout());
        try{
        	final InputStream stream = DataHandler.getResourceAsStream("/characters.txt");
        	final InputStreamReader reader = new InputStreamReader(stream);
            final BufferedReader inBuf = new BufferedReader(reader);
            String line;
            final Dimension dim = new Dimension(50, 30);
            while (null!=(line = inBuf.readLine())){
                try{
                    final int index = line.indexOf(':');
                    final char c = parseCharacter(line.substring(line.indexOf('\'')+1, line.lastIndexOf('\'', index)));
                    final JCharacterToggleButton characterToggleButton = new JCharacterToggleButton(String.valueOf(c), c);                    
                    characterToggleButton.setDescription(line.substring(line.indexOf('"', index) + 1, line.lastIndexOf('"')));
                    characterToggleButton.addActionListener(this);
                    characterToggleButton.setPreferredSize(dim);
                    characterButtonGroup.add(characterToggleButton);
                    panelCharacterButtons.add(characterToggleButton);
                }catch (Exception e){
                    logger.error("Can't read character", e);
                }
            }
            inBuf.close();
            reader.close();
            stream.close();
        }catch (MalformedURLException e){
        	logger.error("Problems reading characters", e);
        }catch (ConnectException e){
        	logger.error("Problems reading characters", e);
        }catch (IOException e){
        	logger.error("Problems reading characters", e);
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx =1;
        gbc.weighty =1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(panelCharacterButtons, gbc);

        gbc = new GridBagConstraints();
        gbc.weightx =1;
        gbc.weighty =0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(textFieldDescription, gbc);

        buttonCopy.addActionListener(this);
        gbc = new GridBagConstraints();
        gbc.weightx =0;
        gbc.weighty =0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        add (buttonCopy, gbc);
        pack();
        setBounds (100, 100, 350, 310);
        setTitle("Zeichentabelle");
        setResizable(false);      

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        DataHandler.addToUpdateTree(this);
    }
    
    public static final synchronized JFrame getInstance(){
    	return instantiator.get();
    }
    
    public static char parseCharacter(String str){
    	if (str.length() == 1)
    		return str.charAt(0);
        if (str.length() == 6 && str.startsWith("\\u")){
        	int erg = 0;
        	for (int i=2;i<6;i++){
        		final char c = str.charAt(i);
        		final int nextChar;
        		if (c >= '0' && c <= '9')
        			nextChar = c-'0';
        		else if (c >= 'A' && c <= 'F')
        			nextChar = c-('A'-10);
        		else if (c >= 'a' && c <= 'f')
        			nextChar = c-('a'-10);
        		else
        	        throw new NumberFormatException("Can't read char:" + c);    
        		erg = (erg << 4) | nextChar;
        	}
        	return (char)erg;
        }
        throw new NumberFormatException();
    }

    private static class JCharacterToggleButton extends JToggleButton
    {
		private static final long serialVersionUID = -1706788315844600779L;
		private String description;
		private final char c;
		
        public JCharacterToggleButton (String text, char c){super(text);this.c = c;}

        public void setDescription(String description){
            this.description = description;
        }

        public final String getDescription(){
            return description;
        }
    }
}

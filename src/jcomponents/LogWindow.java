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

import java.awt.*;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import data.DataHandler;
import jcomponents.util.JComponentSingletonInstantiator;
import logging.InterfaceLogger;
import util.JFrameUtils;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class LogWindow extends JFrame implements InterfaceLogger.LoggingListener
{
	public static final long serialVersionUID = 1L;
	private static final SimpleAttributeSet SET_OUT = getStyle(Color.BLACK);
	private static final SimpleAttributeSet SET_WARN= getStyle(Color.ORANGE);
	private static final SimpleAttributeSet SET_ERR = getStyle(Color.RED);
	private static final Logger logger = LoggerFactory.getLogger(LogWindow.class);
	private static final JComponentSingletonInstantiator<JFrame> instantiator = new JComponentSingletonInstantiator<JFrame>(LogWindow.class);
	
	private final JTextPane textPaneLog = new JTextPane();
	private final Document textDocument = textPaneLog.getDocument();
	
    public static final ActionListener getOpenWindowListener()
    {
    	return instantiator;
    }
	
	@Override
	public void append(ILoggingEvent ile) {
		final SimpleAttributeSet set;
		switch (ile.getLevel().levelInt){
			case Level.INFO_INT:	set = SET_OUT;break;
			case Level.WARN_INT:	set = SET_WARN;break;
			case Level.ERROR_INT:	set = SET_ERR;break;
			default: set = SET_OUT;
		}
	    try {
	    	textDocument.insertString(textDocument.getLength(), ile.toString() + '\n', set);
	    }catch (BadLocationException e) {
    		logger.error("Error at appending log line",e);
    	}
	}
	
	
	private static final SimpleAttributeSet getStyle(Color c){
		SimpleAttributeSet set = new SimpleAttributeSet();
		StyleConstants.setForeground(set, c);
   		StyleConstants.setFontFamily(set, "Helvetica");
   		StyleConstants.setFontSize(set, 12);
   		StyleConstants.setBold(set, true);
   		return set;
	}
    
	public LogWindow(){
		final JScrollPane scrollPaneLog = new JScrollPane(textPaneLog);
		setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
		add(scrollPaneLog);
		setBounds(100, 100, 500, 200);
		setResizable(true);
		
   		textPaneLog.setEditable(false);
   		InterfaceLogger.addLoggingListener(this);
   		DataHandler.addToUpdateTree(this);
	}
}

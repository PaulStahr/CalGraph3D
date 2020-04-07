package jcomponents;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JMenuItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;

public class LicenceMenuItem extends JMenuItem implements ActionListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6253856974563168526L;
	private static final Logger logger = LoggerFactory.getLogger(LicenceMenuItem.class);
	final String file;
	
	public LicenceMenuItem(String headline, String file)
	{
		super(headline);
		this.file = file;
		addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			LicenceWindow.getInstance(getText(), DataHandler.getResourceAsString('/' + "license" + '/' + file)).setVisible(true);
		} catch (IOException ex) {
			logger.error("Can't read licence", ex);
		}
	}
}


package jcomponents;

import java.io.IOException;

import javax.swing.JMenu;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import util.StringUtils;

public class LicenseMenu extends JMenu{
	private static final long serialVersionUID = -9113695438548902687L;
	private static final Logger logger = LoggerFactory.getLogger(LicenseMenu.class);
    
    public LicenseMenu(){
    	this(StringUtils.EMPTY);
    }
    
   public LicenseMenu(String title){
    	super(title);
    	try {
			Document doc = new SAXBuilder().build(DataHandler.getResourceAsStream("license.xml"));
			Element elem = doc.getRootElement();
			for (Element licence : elem.getChildren()){
				try{
					final String headline = licence.getChildText("headline");
					final String file = licence.getChildText("file");
					
					final LicenceMenuItem menuItem = new LicenceMenuItem(headline, file);
					add(menuItem);
				}catch(Exception e){
					logger.error("Error at initialising Licence-Menu", e);
				}
			}
    	} catch (JDOMException | IOException e) {
			logger.error("Error at initialising Licence-Menu", e);
		}
    }
}

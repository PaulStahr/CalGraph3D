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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigInteger;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.JFrameUtils;
import data.DataHandler;
import data.Options;
import jcomponents.util.JComponentSingletonInstantiator;

public class ActivateWindow extends JFrame implements ActionListener, WindowListener {
	/** 
	* @author  Paul Stahr
	* @version 26.02.2012
	*/	
	private static final long serialVersionUID = 862576726569880338L;
	private static final Logger logger = LoggerFactory.getLogger(ActivateWindow.class);
	private static final JComponentSingletonInstantiator<ActivateWindow> instantiator = new JComponentSingletonInstantiator<ActivateWindow>(ActivateWindow.class);
	private final JTextPane editorPaneText = new JTextPane();
	private final JLabel labelHashCode = new JLabel("Ihre Id:");
	private final JTextField textFieldHashCode = new JTextField();
	private final JLabel labelKey = new JLabel("Schl\u00FCssel:");
	private final JTextField textFieldKey = new JTextField();
	private final JCheckBox showAtStartup = new JCheckBox("Beim Start anzeigen");
	private final JButton buttonOk = new JButton("Akzeptieren");
	private final JButton buttonCancel = new JButton("Abbrechen");
	
	private BigInteger key = null;
	
    public static final synchronized JFrame getInstance(){
    	return instantiator.get();
    }
    
    public static final ActionListener getOpenWindowListener()
    {
    	return instantiator;
    }
    
	@Override
   public void actionPerformed(ActionEvent ae){
		Options.set("product_key", key);
		Options.triggerUpdates();
		logger.info("Programm is activated now");
		dispose();
	}
		
	public ActivateWindow(){
		OperatingSystemMXBean bean =  ManagementFactory.getOperatingSystemMXBean();
		final BigInteger THOUSAND = BigInteger.valueOf(1000);
		final BigInteger userHashCode = BigInteger.valueOf(System.getProperty("user.name").hashCode())
				.multiply(THOUSAND).add(BigInteger.valueOf(bean.getAvailableProcessors()))
				.add(BigInteger.valueOf(bean.getArch().hashCode()))
				.multiply(THOUSAND)
				.add(BigInteger.valueOf(bean.getVersion().hashCode()))
				.multiply(THOUSAND)
				.add(BigInteger.valueOf(System.getProperty("user.home").hashCode()))
				.multiply(THOUSAND)
				.add(BigInteger.valueOf(bean.getName().hashCode())).abs();
		
		GroupLayout layout = new GroupLayout(getContentPane());
		setLayout(layout);
		
		editorPaneText.setContentType("text/html"); 
		editorPaneText.setEditable(false);
		editorPaneText.setText("<html><body>Ihr Produkt ist nicht aktiviert.<br />Die Aktivierung ist notwendig, damit Funktionen wie die Speicher-, Lade- und Exportfunktionen genutzt werden k\u00F6nnen.<br />Um ihr Produkt zu aktivieren senden sie bitte eine E-mail an:<a href=\"mailto:paul.stahr@gmxy.de\">\"paul.stahr@gmx.de\"</a><br />Zu der E-mail f\u00FCgen sie den unten stehenden Code bei.<br />Die Aktivierung ist Kostenlos und es werden auch keine Werbe-E-mails oder sonstiges an sie versendet.<br />Wenn sie bereits den im Besitz des Schl\u00FCssels f\u00FCr diesen Pc sind tragen sie diesen bitte in dem unteren Eingabefeld ein.</body></html>");
		
		textFieldHashCode.setEditable(false);
		textFieldHashCode.setText(userHashCode.toString(16));
		
		textFieldKey.getDocument().addDocumentListener(
			new JFrameUtils.DocumentChangeListener() {
				
				
				@Override
				public void update(DocumentEvent de) {
					try{
						key = new BigInteger(textFieldKey.getText(), 16);
						buttonOk.setEnabled(userHashCode.equals(key.modPow(new BigInteger("21f3076c0b61cb3a1a5e71609a3297b11c7750cc9c1b6a30a89b22aa3925b0f495c49a6a0411de8dcb801c8459966496150d77f80173567cda5ba057f210744a3e6bbfe1f98c581cfda68f0cc8076473563d3bfc25937ba445b7a898d3b3acfd2f76e4ad94640b47b522be2cd317a0faf12c1113162adf7f0d2b8277f2f203cb71f071b69bb2345195eec5627bffdc147157b047c0bb223af0cec1545138a199297c948b413fecfe64e6d6f872499506473b99b895d4e2dfd98247a426e1a11aecbf402a3b44d147786561eadc057bc8df85d717e42f8b68afccfac3a036599459d495c874afab1590d0091853fe1357", 16), new BigInteger("4852dabb7b4fb62a131811b43fd58143fa4087cd04cadd1f24827f0d76083807d4bd691c4200fce6e73713b40291f2715cfd202723956ee8e47ab4852861d31def18561c128b22c80e333432a96cebd98c5c32820a39a86a5dd6cedbca527aba875299b60b3219faec366ec6af884e1801b864ac4fb3cec7d116381231641115be48decb59ca5f1bdf8b5014a2e96edb15ad1e5731e546ad29914efcf7e40f2474309dbddd2d9202d0a007698e791c7e1919110d9091b1201d32e7d98c26088c74d25886da4117d83cc1fb6237d941a0b8e2df1332683912ec0623e15f0ef98571dd4daeceb2fc82949681454373f9cb998c8369b1324ed4b7c94815e2f03e7", 16))));
					}catch(Exception e){}
				}
			}		
		);
		
		buttonOk.setEnabled(false);
		buttonOk.addActionListener(this);
		
		buttonCancel.addActionListener(JFrameUtils.closeParentWindowListener);

		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(editorPaneText)
			.addGroup(layout.createParallelGroup()
				.addComponent(labelHashCode, 25, 25, 25)
				.addComponent(textFieldHashCode, 25, 25, 25)
			).addGap(5).addGroup(layout.createParallelGroup()
				.addComponent(labelKey, 25, 25, 25)
				.addComponent(textFieldKey, 25, 25, 25)
			).addGap(5).addComponent(showAtStartup)
			.addGap(5).addGroup(layout.createParallelGroup()
				.addComponent(buttonOk, 25, 25, 25)
				.addComponent(buttonCancel, 25, 25, 25)
			)
		);
		
		layout.setHorizontalGroup(layout.createParallelGroup()
			.addComponent(editorPaneText)
			.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
					.addComponent(labelHashCode)
					.addComponent(labelKey)
				).addGroup(layout.createParallelGroup()
					.addComponent(textFieldHashCode)
					.addComponent(textFieldKey)
				)
			).addComponent(showAtStartup)
			.addGroup(layout.createSequentialGroup()
				.addComponent(buttonOk)
				.addComponent(buttonCancel)
			)
		);	
		setTitle("Aktivierung");
		setResizable(false);
		pack();
	
		addWindowListener(this);
		DataHandler.addToUpdateTree(this);
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		showAtStartup.setSelected(Options.getBoolean("show_actiavation_at_start", true));
	}

	
	@Override
	public void windowClosed(WindowEvent arg0) {
		Options.set("show_actiavation_at_start", showAtStartup.isSelected());
		Options.triggerUpdates();
	}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowClosing(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}
}

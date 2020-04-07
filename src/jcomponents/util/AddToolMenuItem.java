package jcomponents.util;

import javax.swing.JMenuItem;

public class AddToolMenuItem extends JMenuItem {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8238920490664701658L;
	public final byte panel;
	public AddToolMenuItem(String name, final byte panel){
		super(name);
		this.panel = panel;
	}
}

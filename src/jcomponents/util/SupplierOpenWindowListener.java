package jcomponents.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Supplier;

import javax.swing.JFrame;

public class SupplierOpenWindowListener implements ActionListener{
	private final Supplier<JFrame> sup;
	
	public SupplierOpenWindowListener(Supplier<JFrame> sup) {
		this.sup = sup;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		sup.get().setVisible(true);
	}

}

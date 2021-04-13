package jcomponents;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class ColorEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
	private static final long serialVersionUID = -2260187722110350710L;
	private Color currentColor;
	private final JButton button = new JButton();
	//protected static final String EDIT = "edit";
	
	public ColorEditor() {
		//button.setActionCommand(EDIT);
		button.addActionListener(this);
		button.setBorderPainted(false);
	}
		
	@Override
    public void actionPerformed(ActionEvent e) {
		JColorChooser colorChooser = new JColorChooser();
		Object source = e.getSource();
		if (source == button) {
			button.setBackground(currentColor);
			colorChooser.setColor(currentColor);
			JDialog dialog = JColorChooser.createDialog(button,"Pick a Color",true,colorChooser,this,null);
			dialog.setVisible(true);
			fireEditingStopped();
		} else {
			currentColor = colorChooser.getColor();
		}
	}
		
	@Override
    public Object getCellEditorValue() {return currentColor;}
	
	@Override
    public Component getTableCellEditorComponent(JTable table, Object value,boolean isSelected,int row,int column) {
		currentColor = (Color)value;
		return button;
	}
}
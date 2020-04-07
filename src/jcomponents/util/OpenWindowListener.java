package jcomponents.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;

import util.data.UniqueObjects;

public class OpenWindowListener implements ActionListener{
	private final Class<?> cl;
	public OpenWindowListener(Class<?> cl)
	{
		this.cl = cl;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			((JFrame)(cl.getMethod("getInstance", UniqueObjects.EMPTY_CLASS_ARRAY).invoke(null, UniqueObjects.EMPTY_OJECT_ARRAY))).setVisible(true);
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvocationTargetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}

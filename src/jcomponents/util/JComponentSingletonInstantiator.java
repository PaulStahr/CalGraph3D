package jcomponents.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

import javax.swing.JFrame;

import util.JFrameUtils;
import util.RunnableSupplier;
import util.data.UniqueObjects;

public class JComponentSingletonInstantiator<T> implements Supplier<T>, ActionListener{
	private WeakReference<T> ref;
	private final Class<?> cl;
	
	public JComponentSingletonInstantiator(Class<?> cl)
	{
		this.cl = cl;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final T get()
	{
		if (ref != null)
		{
			T o = ref.get();
			if (o != null)
			{
				return o;
			}
		}
		RunnableSupplier<T> sup = new RunnableSupplier<T>() {
			@Override
			public void run()
			{
				try {
					set((T)cl.getConstructor(UniqueObjects.EMPTY_CLASS_ARRAY).newInstance(UniqueObjects.EMPTY_OJECT_ARRAY));
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
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		JFrameUtils.runByDispatcherAndWait(sup);
		T o = sup.get();
		ref = new WeakReference<T>(o);
    	return o;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			((JFrame)get()).setVisible(true);
			//((JFrame)(cl.getMethod("getInstance", DataHandler.EMPTY_CLASS_ARRAY).invoke(null, DataHandler.EMPTY_OJECT_ARRAY))).setVisible(true);
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} /*catch (IllegalAccessException e1) {
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
		}*/
	}
}

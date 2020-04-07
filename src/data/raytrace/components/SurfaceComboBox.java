package data.raytrace.components;

import java.awt.EventQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import data.raytrace.GuiOpticalSurfaceObject;
import data.raytrace.OpticalObject;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.RaytraceScene;

public class SurfaceComboBox extends JComboBox<OpticalObject>implements GuiOpticalSurfaceObject.OpticalSurfaceObjectChangeListener, Runnable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6036914930497606789L;
	private final RaytraceScene scene;
	boolean light;
	private final AtomicBoolean isQueued = new AtomicBoolean();

	public SurfaceComboBox(RaytraceScene scene, boolean light) {
		this.scene = scene;
		this.light = light;
		scene.addObjectChangeListener(this);
		
	}

	@Override
	public void valueChanged(GuiOpticalSurfaceObject object, SCENE_OBJECT_COLUMN_TYPE ct) {
		if (ct == SCENE_OBJECT_COLUMN_TYPE.ACTIVE && isQueued.compareAndSet(false, true))
		{
			EventQueue.invokeLater(this);
		}
	}
	
	public void run()
	{
		isQueued.set(false);
		Object current = getSelectedItem();
		setModel(new DefaultComboBoxModel<OpticalObject>(light ? scene.cloneActiveLights() : scene.getActiveSurfaces()));
		setSelectedItem(current);
	}
}

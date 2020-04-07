package jcomponents.raytrace;

import javax.swing.JComboBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import data.raytrace.OpticalSurfaceObject;
import data.raytrace.RaytraceScene;
import data.raytrace.RaytraceScene.SceneChangeListener;

public class JOpticalSurfaceListComboBox extends JComboBox<OpticalSurfaceObject> implements ChangeListener, SceneChangeListener{
	private static final long serialVersionUID = 91603719843072767L;
	RaytraceScene scene;
	
	public JOpticalSurfaceListComboBox(RaytraceScene scene)
	{
		this.scene = scene;
		scene.add(this);
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void valueChanged(byte ct, Object o) {
		if (ct == RaytraceScene.OBJECT_ADD)
		{
			
		}
	}
	
}

package data.raytrace.components;

import java.awt.EventQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import data.raytrace.GuiTextureObject;
import data.raytrace.RaytraceScene;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;

public class TextureComboBox extends JComboBox<GuiTextureObject>implements GuiTextureObject.TextureObjectChangeListener, Runnable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3006151928755696087L;
	private final RaytraceScene scene;
	private final AtomicBoolean isQueued = new AtomicBoolean();

	public TextureComboBox(RaytraceScene scene) {
		this.scene = scene;
		scene.addObjectChangeListener(this);
	}
	
	@Override
	public void valueChanged(GuiTextureObject object, SCENE_OBJECT_COLUMN_TYPE ct) {
		if (ct == SCENE_OBJECT_COLUMN_TYPE.ACTIVE && isQueued.compareAndSet(false, true))
		{
			EventQueue.invokeLater(this);
		}
	}

	@Override
	public void run()
	{
		isQueued.set(false);
		Object current = getSelectedItem();
		setModel(new DefaultComboBoxModel<GuiTextureObject>(scene.textureObjectList.toArray(new GuiTextureObject[scene.textureObjectList.size()])));
		setSelectedItem(current);
	}
}

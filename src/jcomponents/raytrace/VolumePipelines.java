package jcomponents.raytrace;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;

import data.raytrace.RaytraceScene;
import util.JFrameUtils;

public class VolumePipelines extends JFrame implements ActionListener{
	private static final long serialVersionUID = -1840132779570616194L;
	private final JTabbedPane tapPane = new JTabbedPane();
	private final JMenuBar menuBar = new JMenuBar();
	private final JMenuItem itemAdd = new JMenuItem("Add");
	private final JMenuItem itemRemove = new JMenuItem("Remove");
	private final RaytraceScene scene;
	
	public VolumePipelines(RaytraceScene scene)
	{
		this.scene = scene;
		setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
		add(tapPane);
		menuBar.add(itemAdd);
		menuBar.add(itemRemove);
		itemAdd.addActionListener(this);
		itemRemove.addActionListener(this);
		setJMenuBar(menuBar);
		setSize(400, 200);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		if (source == itemAdd)
		{
			addPipeline();
		}
		else if (source == itemRemove)
		{
			tapPane.remove(tapPane.getSelectedIndex());
		}
	}

	public VolumePipelinePanel addPipeline() {
		VolumePipelinePanel vp = new VolumePipelinePanel(scene);
		scene.add(vp.pipeline);
		tapPane.add("Pipe", vp);
		return vp;
	}

	public void removeAllPipelines() {
		tapPane.removeAll();
		tapPane.revalidate();
	}
	
	public VolumePipelinePanel[] getPipelines() {
		VolumePipelinePanel vp[] = new VolumePipelinePanel[tapPane.getComponentCount()];
		for (int i = 0; i < vp.length; ++i)
		{
			vp[i] = (VolumePipelinePanel)tapPane.getComponent(i);
		}
		return vp;
	}
}

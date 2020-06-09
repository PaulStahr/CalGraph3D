package jcomponents.raytrace;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import data.Options;
import jcomponents.InterfaceOptions.OptionPanel;
import jcomponents.util.JComponentSingletonInstantiator;
import util.JFrameUtils;
public class RaytraceOptions extends OptionPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6597654543082438900L;
	private final JLabel labelInvisibleAlphaValue = new JLabel("Invisible Alpha");
	private final JSlider sliderInvisibleAlphaValue = new JSlider(0, 255);
	private final JLabel labelRayAlphaValue = new JLabel("Ray Alpha");
	private final JSlider sliderRayAlphaValue = new JSlider(0, 255);
	private final JLabel labelDrawAnchor = new JLabel("Draw Anchor");
	private final JCheckBox checkBoxDrawAnchor = new JCheckBox();
	private final JLabel labelDrawDirection = new JLabel("Draw Direction");
	private final JCheckBox checkBoxDrawDirection = new JCheckBox();
	private final JLabel labelDrawFocalpoints = new JLabel("Draw Focalpoints");
	private final JCheckBox checkBoxDrawFocalpoint = new JCheckBox();
	private final JLabel labelDrawMesure = new JLabel("Draw Mesure");
	private final JCheckBox checkBoxDrawMesure = new JCheckBox();
	private final JLabel label3dScale = new JLabel("3D scale");
	private final JTextField textField3dScale = new JTextField();
	private final JLabel labelBlocksize = new JLabel("Blocksize");
	private final JTextField textFieldBlocksize = new JTextField();
	private final JLabel labelRaytraceMessageLevel = new JLabel("Raytrace Loglevel");
	private final JTextField textFieldRaytraceLoglevel = new JTextField();
	private final JLabel labelRaytraceWriteInstance = new JLabel("Raytrace Write Instance");
	private final JCheckBox checkBoxRaytraceWriteInstance = new JCheckBox();
	
	public static final JComponentSingletonInstantiator<RaytraceOptions> instantiator = new JComponentSingletonInstantiator<RaytraceOptions>(RaytraceOptions.class);

	
	public RaytraceOptions()
	{
		setLayout(JFrameUtils.DOUBLE_COLUMN_LAUYOUT);
		add(labelInvisibleAlphaValue);
		add(sliderInvisibleAlphaValue);
		add(labelRayAlphaValue);
		add(sliderRayAlphaValue);
		add(labelDrawAnchor);
		add(checkBoxDrawAnchor);
		add(labelDrawDirection);
		add(checkBoxDrawDirection);
		add(labelDrawFocalpoints);
		add(checkBoxDrawFocalpoint);
		add(labelDrawMesure);
		add(checkBoxDrawMesure);
		add(label3dScale);
		add(textField3dScale);
		add(labelBlocksize);
		add(textFieldBlocksize);
		add(labelRaytraceMessageLevel);
		add(textFieldRaytraceLoglevel);
		add(labelRaytraceWriteInstance);
		add(checkBoxRaytraceWriteInstance);
	}
	
	@Override
	public void save()
	{
		Options.OptionTreeInnerNode raytrace = Options.getInnerNode("raytrace");
		Options.OptionTreeInnerNode visible = Options.getInnerNode(raytrace, "visible");
		Options.set(raytrace, "invisible_alpha", sliderInvisibleAlphaValue.getValue());
		Options.set(raytrace, "ray_alpha", sliderRayAlphaValue.getValue());
		Options.set(visible, "anchor", checkBoxDrawAnchor.isSelected());
		Options.set(visible, "direction", checkBoxDrawDirection.isSelected());
		Options.set(visible, "focalpoint", checkBoxDrawFocalpoint.isSelected());
		Options.set(visible, "measure", checkBoxDrawMesure.isSelected());
		Options.set(raytrace, "dscale", Float.parseFloat(textField3dScale.getText()));
		Options.set(raytrace, "blocksize", Integer.parseInt(textFieldBlocksize.getText()));
		Options.set(raytrace, "loglevel", Integer.parseInt(textFieldRaytraceLoglevel.getText()));
		Options.set(raytrace, "writeinstance", checkBoxRaytraceWriteInstance.isSelected());
		Options.triggerUpdates();
	}
	
	@Override
	public void load()
	{
		Options.OptionTreeInnerNode raytrace = Options.getInnerNode("raytrace");
		Options.OptionTreeInnerNode visible = Options.getInnerNode(raytrace, "visible");
		sliderInvisibleAlphaValue.setValue(Options.getInteger(raytrace, "invisible_alpha", 64));
		sliderRayAlphaValue.setValue(Options.getInteger(raytrace, "ray_alpha", 64));
		checkBoxDrawAnchor.setSelected(Options.getBoolean(visible, "anchor"));
		checkBoxDrawDirection.setSelected(Options.getBoolean(visible, "direction"));
		checkBoxDrawFocalpoint.setSelected(Options.getBoolean(visible, "focalpoint"));
		checkBoxDrawMesure.setSelected(Options.getBoolean(visible, "measure"));
		textField3dScale.setText(Float.toString(Options.getFloat(raytrace, "dscale")));
		textFieldBlocksize.setText(Integer.toString(Options.getInteger(raytrace, "blocksize")));
		textFieldRaytraceLoglevel.setText(Integer.toString(Options.getInteger(raytrace, "loglevel")));
		checkBoxRaytraceWriteInstance.setSelected(Options.getBoolean(raytrace, "writeinstance"));
	}
}
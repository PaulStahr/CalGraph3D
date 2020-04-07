package jcomponents.raytrace;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import data.raytrace.RaytraceScene;
import jcomponents.util.JMathTextField;
import util.JFrameUtils;

public class ProjectOptionFrame extends JFrame implements ActionListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1420596131507238310L;
	private final JLabel labelAuthor = new JLabel("Author");
	private final JTextField textFieldAuthor = new JTextField();
	private final JLabel labelEpsilon = new JLabel("Epsilon");
	private final JMathTextField textFieldEpsilon = new JMathTextField();
	private final JLabel labelBounds = new JLabel("Bounds");
	private final JTextField textFieldBounds = new JTextField();
	private final JButton buttonOk = new JButton("Ok");
	private final JButton buttonCancel = new JButton("Cancel");
	private final RaytraceScene scene;
	
	public ProjectOptionFrame(RaytraceScene scene)
	{
		this.scene = scene;
		setLayout(JFrameUtils.DOUBLE_COLUMN_LAUYOUT);
		add(labelAuthor);
		add(textFieldAuthor);
		textFieldAuthor.setText(scene.author);
		add(labelEpsilon);
		add(textFieldEpsilon);
		add(labelBounds);
		add(textFieldBounds);
		textFieldEpsilon.setText(Double.toString(scene.epsilon));
		add(buttonOk);
		add(buttonCancel);
		
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		if (source == buttonOk)
		{
			scene.author = textFieldAuthor.getText();
			scene.epsilon = textFieldEpsilon.get().calculate(null, null).doubleValue();
		}
		dispose();
	}
}

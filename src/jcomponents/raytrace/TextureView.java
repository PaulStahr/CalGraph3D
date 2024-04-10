package jcomponents.raytrace;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.raytrace.TextureMapping;
import geometry.Matrix3d;
import geometry.Matrix4d;
import geometry.Vector2d;
import geometry.Vector3d;
import jcomponents.RecentFileList;
import jcomponents.util.ImageUtil;
import jcomponents.util.JMathTextField;
import maths.Controller;
import maths.Operation;
import maths.algorithm.Calculate;
import maths.data.ArrayOperation;
import maths.variable.Variable;
import maths.variable.VariableStack;
import util.ArrayTools;
import util.JFrameUtils;
import util.OperationGeometry;


public class TextureView extends JFrame implements ActionListener, ItemListener{
	/**
	 *
	 */
	private static final long serialVersionUID = -6300404180378123140L;
	private ImageComponent imagePanel = new ImageComponent();
	private final JMenu menuFile = new JMenu("File");
	private final JMenuItem menuItemOpen = new JMenuItem("Open");
	private final JMenuItem menuItemSave = new JMenuItem("Save");
	private final JMenuItem menuItemBackground = new JMenuItem("Background");
	private final JMenuItem menuItemFitScaling = new JMenuItem("Fit Scaling");
	private final JMenu menuMapping = new JMenu("Mapping");
	private final JMathTextField mappingInputTransformation = new JMathTextField();
	private final JCheckBoxMenuItem[] menuItemInputMapping = new JCheckBoxMenuItem[TextureMapping.size()];
	private final JMathTextField mappingCartTransformation = new JMathTextField();
	private final JCheckBoxMenuItem[] menuItemOutputMapping = new JCheckBoxMenuItem[TextureMapping.size()];
	private final JMathTextField mappingOutputTransformation = new JMathTextField();
	private TextureMapping inputTextureMapping = TextureMapping.FLAT;
	private TextureMapping outputTextureMapping = TextureMapping.FLAT;
	private final JMenuItem menuItemRecalculateMapping = new JMenuItem("Recalculate");
	private final JMenuItem menuItemSaveMappedImage = new JMenuItem("Save");
	private final JCheckBoxMenuItem menuItemDrawFullCircle = new JCheckBoxMenuItem("DrawFullCircle");
	private final JCheckBoxMenuItem menuItemDrawHalfCircle = new JCheckBoxMenuItem("DrawHalfCircle");
	private final JMathTextField mapColors = new JMathTextField("{r,g,b,a}");
	private final JMenuBar menuBar = new JMenuBar();
	private static final Logger logger = LoggerFactory.getLogger(TextureView.class);
	private BufferedImage image;

	public TextureView(BufferedImage image)
	{
		this.image = image;
		imagePanel = new ImageComponent(image);
		menuBar.add(menuFile);
		menuFile.add(menuItemOpen);
		menuFile.add(menuItemBackground);
		menuFile.add(menuItemSave);
		menuFile.add(menuItemFitScaling);
		menuFile.add(menuMapping);
		ButtonGroup buttonGroup = new ButtonGroup();
		menuMapping.add(mappingInputTransformation);
		for (int i = 0; i < TextureMapping.size(); ++i)
		{
			menuItemInputMapping[i] = new JCheckBoxMenuItem(TextureMapping.get(i).name);
			menuItemInputMapping[i].addActionListener(this);
			buttonGroup.add(menuItemInputMapping[i]);
			menuMapping.add(menuItemInputMapping[i]);
		}
		menuMapping.add(mappingCartTransformation);
		buttonGroup = new ButtonGroup();
		for (int i = 0; i < TextureMapping.size(); ++i)
		{
			menuItemOutputMapping[i] = new JCheckBoxMenuItem(TextureMapping.get(i).name);
			menuItemOutputMapping[i].addActionListener(this);
			buttonGroup.add(menuItemOutputMapping[i]);
			menuMapping.add(menuItemOutputMapping[i]);
		}
		menuMapping.add(mappingOutputTransformation);
		menuFile.add(menuItemRecalculateMapping);
		menuMapping.add(menuItemSaveMappedImage);

		JMenu menuMapColors = new JMenu("Color Mapping");
		menuFile.add(menuMapColors);
		menuMapping.add(menuMapColors);
		menuMapColors.add(mapColors);

		menuFile.add(menuItemDrawFullCircle);
		menuFile.add(menuItemDrawHalfCircle);

		menuItemOpen.addActionListener(this);
		menuItemSaveMappedImage.addActionListener(this);
		menuItemSave.addActionListener(this);
		menuItemFitScaling.addActionListener(this);
		menuItemBackground.addActionListener(this);

		menuItemDrawFullCircle.addItemListener(this);
		menuItemDrawHalfCircle.addItemListener(this);
		menuItemRecalculateMapping.addActionListener(this);

		setJMenuBar(menuBar);
        add(imagePanel);
        setSize(800, 400);
		setVisible(true);
	}

	public BufferedImage getImage()
	{
		return imagePanel.image;
	}

	public void setImage(BufferedImage image)
	{
		this.image = image;

		BufferedImage bi = image;

		int pixel[] = new int[4];
		if (inputTextureMapping != outputTextureMapping)
		{
			bi = new BufferedImage(outputTextureMapping.defaultAspect() * image.getHeight(), image.getHeight(), image.getType());
			WritableRaster outR = bi.getRaster();
			Vector2d v = new Vector2d();
			Vector3d v3 = new Vector3d();
			Raster r = image.getRaster();
			int tmp[] = new int[4];
			Matrix3d inputTransformation = null, outputTransformation = null;
			Matrix4d cartTransformation = null;
			int outWidth = bi.getWidth();
			int outHeight = bi.getHeight();
			double invOutHeight = 1. / outHeight;
			double invOutWidth = 1. / outWidth;
			maths.Operation op = mappingInputTransformation.get();

			if (op != null)
			{
				OperationGeometry.parseMatRowMajor(op.calculate(null, null), inputTransformation = new Matrix3d());
			}
			op = mappingCartTransformation.get();
			if (op != null)
			{
				OperationGeometry.parseMatRowMajor(op.calculate(null, null), cartTransformation = new Matrix4d());
			}
			op = mappingOutputTransformation.get();
			if (op != null)
			{
				OperationGeometry.parseMatRowMajor(op.calculate(null, null), outputTransformation = new Matrix3d());
			}
			for (int y = 0; y < outHeight; ++y)
			{
				for (int x = 0; x < outWidth; ++x)
				{
					double xd = x * invOutWidth;
					double yd = y * invOutHeight;
					if (outputTransformation != null)
					{
						double tmpx = outputTransformation.ldotAffineX(xd, yd);
						yd = outputTransformation.ldotAffineY(xd, yd);
						xd = tmpx;
					}
					outputTextureMapping.mapTexToCart(xd, yd, v3);
					if (cartTransformation != null)
					{
						cartTransformation.rdotAffine(v3);
					}
					inputTextureMapping.mapCartToTex(v3.x, v3.y, v3.z, v);
					if (inputTransformation != null)
					{
						inputTransformation.transformAffine(v);
					}
					v.x = Calculate.clamp(v.x, 0, 0.99999999);
					v.y = Calculate.clamp(v.y, 0, 0.99999999);

					ImageUtil.getSmoothedPixel(v.x * r.getWidth(), v.y * r.getHeight(), pixel, tmp, r);
					outR.setPixel(x, y, pixel);
				}
			}
		}
		Graphics2D graphics = (Graphics2D)bi.getGraphics();
		if (menuItemDrawFullCircle.isSelected())
		{
			if (outputTextureMapping == TextureMapping.FISHEYE_EQUIDISTANT)
			{
				graphics.drawArc(0, 0, image.getWidth(), image.getHeight(), 0, 360);
			}
		}
		if (menuItemDrawHalfCircle.isSelected())
		{
			if (outputTextureMapping == TextureMapping.FISHEYE_EQUIDISTANT)
			{
				graphics.drawArc(image.getWidth() / 4, image.getHeight() / 4, image.getWidth() / 2, image.getHeight() / 2, 0, 360);
			}
			else if (outputTextureMapping == TextureMapping.SPHERICAL)
			{
				graphics.drawLine(0, image.getHeight() / 2, image.getWidth(), image.getHeight() / 2);
			}
			else
			{
				graphics.drawArc(0, 0, image.getWidth(), image.getHeight(), 0, 360);
			}
		}
		Operation mapColorOp = mapColors.get();
		if (mapColorOp != null && !mapColors.getText().equals("{r,g,b,a}"))
		{
			if (bi == image)
			{
				bi = ImageUtil.deepCopy(image);
			}
			WritableRaster outR = bi.getRaster();
			VariableStack vs = new VariableStack();
			Variable red = new Variable("r");
			vs.add(red);
			Variable green = new Variable("g");
			vs.add(green);
			Variable blue = new Variable("b");
			vs.add(blue);
			Variable alpha = new Variable("a");
			vs.add(alpha);
			Controller controll = new Controller();
			controll.connectEmptyVariables(true);
			mapColorOp = mapColorOp.calculate(vs, controll);
			float pixelf[] = new float[4];
			for (int y = 0; y < outR.getHeight(); ++y)
            {
                for (int x = 0; x < outR.getWidth(); ++x)
                {
                    outR.getPixel(x, y, pixelf);
                    red.setValue(pixelf[0]);
                    green.setValue(pixelf[1]);
                    blue.setValue(pixelf[2]);
                    alpha.setValue(pixelf[3]);
                    Operation result = mapColorOp.calculate(vs, null);
                    if (result instanceof ArrayOperation)
                    {
                        pixelf[0] = (float)result.get(0).doubleValue();
                        pixelf[1] = (float)result.get(1).doubleValue();
                        pixelf[2] = (float)result.get(2).doubleValue();
                        pixelf[3] = (float)result.get(3).doubleValue();
                        outR.setPixel(x, y, pixelf);
                    }
                }
            }
			/*

			RealLongOperation numbers[] = new RealLongOperation[256];
			for (int i = 0; i < numbers.length; ++i)
			{
				numbers[i] = new RealLongOperation(i);
			}
			for (int y = 0; y < outR.getHeight(); ++y)
			{
				for (int x = 0; x < outR.getWidth(); ++x)
				{
					outR.getPixel(x, y, pixel);
					red.setValue(numbers[pixel[0]]);
					green.setValue(numbers[pixel[1]]);
					blue.setValue(numbers[pixel[2]]);
					alpha.setValue(numbers[pixel[3]]);
					Operation result = mapColorOp.calculate(vs, null);
					if (result instanceof ArrayOperation)
					{
						pixel[0] = (int)(result.get(0).longValue());
						pixel[1] = (int)(result.get(1).longValue());
						pixel[2] = (int)(result.get(2).longValue());
						pixel[3] = (int)(result.get(3).longValue());
						outR.setPixel(x, y, pixel);
					}
				}
			}*/
		}
		imagePanel.setImage(bi);
	}

	public void addMenu(JMenu menu)
	{
		menuBar.add(menu);
	}

	public Runnable getTextureChangeListener()
	{
		return imagePanel.tcl;
	}

	public ImageComponent getImageComponent() {
		return imagePanel;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		if (source == menuItemOpen)
		{
			JFileChooser fileChooser= new JFileChooser();
			fileChooser.setAccessory(new RecentFileList(fileChooser, DataHandler.getRecentFiles()));
			if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
	        {
				File selectedFile = fileChooser.getSelectedFile();
				String filepath = selectedFile.getAbsolutePath();
				DataHandler.addRecentFile(filepath);
				if (imagePanel.image != null)
				{
					try {
						setImage(ImageIO.read(selectedFile));
					} catch (IOException e) {
						logger.error("Can't read texture", e);
					}
				}
	        }
		}
		else if (source == menuItemSave || source == menuItemSaveMappedImage)
		{
			JFileChooser fileChooser= new JFileChooser();
			fileChooser.setAccessory(new RecentFileList(fileChooser, DataHandler.getRecentFiles()));
			if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
	        {
				File selectedFile = fileChooser.getSelectedFile();
				String filepath = selectedFile.getAbsolutePath();
				DataHandler.addRecentFile(filepath);
				BufferedImage img = menuItemSave == source ? image : imagePanel.image;
				if (imagePanel.image != null)
				{
					try {
						ImageIO.write(img, filepath.substring(filepath.lastIndexOf('.') + 1), selectedFile);
					} catch (IOException e) {
						logger.error("Can't save texture", e);
					}
				}
	        }
		}
		else if (source == menuItemBackground)
		{
			final Color c = JColorChooser.showDialog(null, "Choose Color", getBackground());
			setBackground(c);
			imagePanel.setBackground(c);
		}
		else if (source == menuItemFitScaling)
		{
			imagePanel.fitScaling();
		}
		else if (source == menuItemRecalculateMapping)
		{
			setImage(image);
		}

		if (ArrayTools.find(menuItemInputMapping, source) >= 0 || ArrayTools.find(menuItemOutputMapping, source) >= 0)
		{
			inputTextureMapping = TextureMapping.get(JFrameUtils.getFirstSelected(menuItemInputMapping));
			outputTextureMapping = TextureMapping.get(JFrameUtils.getFirstSelected(menuItemOutputMapping));
			setImage(image);
		}
	}

	protected void updatePreview() {}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
		updatePreview();
	}
}

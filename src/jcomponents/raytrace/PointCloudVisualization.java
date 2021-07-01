package jcomponents.raytrace;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import data.raytrace.FitCircle;
import data.raytrace.TextureMapping;
import geometry.Geometry;
import geometry.Vector2d;
import geometry.Vector3d;
import jcomponents.util.ImageUtil;
import jcomponents.util.JMathTextField;
import maths.Operation;
import util.ArrayUtil;
import util.JFrameUtils;
import util.StringUtils;
import util.data.DoubleArrayList;
import util.io.IOUtil;

public class PointCloudVisualization extends TextureView implements ItemListener
{
	private DoubleArrayList dal;
	private final JCheckBoxMenuItem menuItemTrajectory = new JCheckBoxMenuItem("Trajectory");
	private final JCheckBoxMenuItem parametrizations[] = new JCheckBoxMenuItem[TextureMapping.size()];
	private final JCheckBoxMenuItem menuItemDensityCorrection = new JCheckBoxMenuItem("SphericalDensityCorrection");
	private final JCheckBoxMenuItem menuItemNormalizeValues = new JCheckBoxMenuItem("NormalizeValues");
	private final JCheckBoxMenuItem menuItemMaxDensityLinear = new JCheckBoxMenuItem("Linear");
	private final JCheckBoxMenuItem menuItemMaxDensityQuadratic = new JCheckBoxMenuItem("Quadratic");
	private final JCheckBoxMenuItem menuItemMaxDensityGauss = new JCheckBoxMenuItem("Gauss");
	private final JCheckBoxMenuItem menuItemMaxDensitySurface = new JCheckBoxMenuItem("Surface");
	private final JCheckBoxMenuItem menuItemShowMaximumDensity = new JCheckBoxMenuItem("Show");
	private final JMathTextField textFieldResolution = new JMathTextField("Resolution");
	private final JMenuItem menuItemRecalculate = new JMenuItem("Recalculate");
	private final JMathTextField textFieldSigma = new JMathTextField(0.2);
	private final JMathTextField textFieldFraction = new JMathTextField(0.5);
	private TextureMapping mapping;
	private final Raster raster;

	@Override
    protected void updatePreview()
	{
		mapping = TextureMapping.get(JFrameUtils.getFirstSelected(parametrizations));
		boolean printTrajectory = menuItemTrajectory.isSelected() && dal != null;
		Operation op = textFieldResolution.get();
		int width = printTrajectory ? 2000 : 256;
		int height = width;
		if (this.raster != null)
		{
			width = this.raster.getWidth();
			height = this.raster.getHeight();
		}
		else
		{
			if (op != null && op.isIntegral())
			{
				width = (int)op.longValue();
			}
			height = width / mapping.defaultAspect();
		}
		int count[] = new int[width * height];
		Vector2d v0 = new Vector2d();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		WritableRaster outRaster = image.getRaster();
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, width, height);
		int color[] = new int[4];
		ImageUtil.fill(outRaster, color);

		if (dal != null)
		{
			if (printTrajectory)
			{
				Vector2d v1 = new Vector2d();
				Rectangle2D rect = new Rectangle2D.Double(0, 0, width, height);
				Color col = new Color(0, 255,0,64);
				for (int i = 0; i < dal.size() / 2; ++i)
				{
					double azimuth = dal.getD(i * 2);
					double elevation = dal.getD(i * 2 + 1);
					mapping.mapSphericalToTex(azimuth, elevation, v1);
					v1.x *= width;
					v1.y *= height;
					double xtmp = v1.x, ytmp = v1.y;
					if (Geometry.cropToRectangle(v0, v1, rect))
					{
						int red = (i * 512) / dal.size();
						int green = 255 - red;
						if (col.getRed() != red)
						{
							col = new Color(red, green,0,64);
						}
						graphics.setColor(col);
						graphics.drawLine((int)v0.x, (int)v0.y, (int)v1.x, (int)v1.y);
					}
					v0.set(xtmp, ytmp);
				}
			}
			else
			{
				for (int i = 0; i < dal.size() / 2; ++i)
				{
					double azimuth = dal.getD(i * 2);
					double elevation = dal.getD(i * 2 + 1);
					if (Double.isNaN(azimuth) || Double.isNaN(elevation))
					{
						continue;
					}
					double density = mapping.mapSphericalToTex(azimuth, elevation, v0);
					int add = menuItemDensityCorrection.isSelected() ? (int)(0x100 * density) : 0x100;
					ImageUtil.addToPixel(v0.x * width, v0.y * height, width, height, add, count);

				}
			}
		}
		else if(this.raster != null)
		{
			ImageUtil.getChannel(raster, (byte)0, count, color);
			//ArrayUtil.invert(count	, 255);
		}
		if (!printTrajectory)
		{
			if (menuItemNormalizeValues.isSelected())
			{
				ArrayUtil.normalizeTo(count, 0, count.length, 255);
			}
			ImageUtil.setChannel(outRaster,(byte)3, count, color);

		}

		if (menuItemShowMaximumDensity.isSelected())
		{
			FitCircle fc;
			if (dal != null)
			{
				fc = new FitCircle(dal);
			}
			else if (this.raster != null)
			{
				//Arrays.fill(count, 0);
				//count[width * (height / 2) + width - 1] = 100000000;
				fc = new FitCircle(width, height, count, mapping);
			}
			else
			{
				throw new NullPointerException();
			}

			fc.surfaceDist = menuItemMaxDensitySurface.isSelected();
			if     (menuItemMaxDensityLinear.isSelected())       {fc.method = FitCircle.LINEAR;}
			else if(menuItemMaxDensityQuadratic.isSelected())    {fc.method = FitCircle.QUADRATIC;}
			else if(menuItemMaxDensityGauss.isSelected())        {fc.method = FitCircle.GAUSS;}
			fc.sigma = textFieldSigma.get().doubleValue();

			fc.run();
			double azimuth = fc.getD(0), elevation =  fc.getD(1);
			System.out.println(azimuth + " " + elevation);
			//azimuth = 1;
			//elevation = Math.PI/4;
			mapping.mapSphericalToTex(azimuth,elevation, v0);
			v0.x *= width;
			v0.y *= height;
			graphics.setColor(Color.RED);
			//graphics.drawArc((int)v0.x - 5, (int)v0.y - 5, 10, 10, 0, 360);
			graphics.drawLine((int)v0.x - 10, (int)v0.y, (int)v0.x + 10, (int)v0.y);
			op = textFieldFraction.get();
			if (op != null && op.isRealFloatingNumber())
			{
				double circleSize = fc.getIncludingCircleSize(op.doubleValue());
				Vector3d v3 = new Vector3d();
				TextureMapping.SPHERICAL.mapSphericalToCart(azimuth, elevation, v3);
				double xp = v3.x, yp = v3.y, zp = v3.z;
				for (int y = 0; y < height; ++y)
				{
					for (int x = 0; x < width; ++x)
					{
						mapping.mapTexToCart((double)x / width, (double)y / height, v3);
						double dist = Math.acos(xp * v3.x + yp * v3.y + zp * v3.z);
						if (dist < circleSize)
						{
							outRaster.getPixel(x, y, color);
							//color[3] = 255;
							color[1] = 255;
							outRaster.setPixel(x, y, color);
						}
					}
				}
			}
		}
		setImage(image);
	}

	private void init()
	{
		JMenu menu = new JMenu("Edit");
		JMenu menuParametrization = new JMenu("Parametrization");
		JMenu menuMaxDensityPoint = new JMenu("Maximum Density");
		for (int i = 0; i < TextureMapping.size(); ++i)
		{
			parametrizations[i] = new JCheckBoxMenuItem(TextureMapping.get(i).name);
		}
		menuItemTrajectory.addItemListener(this);
		JFrameUtils.addItemListener(parametrizations, this);
		menuItemDensityCorrection.addItemListener(this);
		menuItemNormalizeValues.addItemListener(this);
		menuItemMaxDensityLinear.addItemListener(this);
		menuItemMaxDensityQuadratic.addItemListener(this);
		menuItemMaxDensityGauss.addItemListener(this);
		menuItemMaxDensitySurface.addItemListener(this);
		menuItemShowMaximumDensity.addItemListener(this);
		menuItemRecalculate.addActionListener(this);
		menu.add(menuItemTrajectory);
		JFrameUtils.add(menuParametrization, parametrizations);
		ButtonGroup group = new ButtonGroup();
		JFrameUtils.add(group, parametrizations);
		group = new ButtonGroup();
		group.add(menuItemMaxDensityLinear);
		group.add(menuItemMaxDensityQuadratic);
		group.add(menuItemMaxDensityGauss);
		menuMaxDensityPoint.add(menuItemMaxDensityLinear);
		menuMaxDensityPoint.add(menuItemMaxDensityQuadratic);
		menuMaxDensityPoint.add(menuItemMaxDensityGauss);
		menuMaxDensityPoint.add(menuItemMaxDensitySurface);
		menuMaxDensityPoint.add(textFieldSigma);
		menuMaxDensityPoint.add(textFieldFraction);
		menuMaxDensityPoint.add(menuItemShowMaximumDensity);
		menu.add(textFieldResolution);
		menu.add(menuParametrization);
		menu.add(menuMaxDensityPoint);
		menu.add(menuItemDensityCorrection);
		menu.add(menuItemNormalizeValues);
		menu.add(menuItemRecalculate);
		parametrizations[0].setSelected(true);
		menuItemDensityCorrection.setSelected(true);
		menuItemNormalizeValues.setSelected(true);
		menuItemMaxDensityLinear.setSelected(true);
		addMenu(menu);
		updatePreview();
		if (dal ==null)
		{
			menuItemTrajectory.setEnabled(false);
		}
	}

	public PointCloudVisualization(Raster r)
	{
		super(null);
		init();
		this.dal = null;
		this.raster = r;
	}

	public PointCloudVisualization(DoubleArrayList dal) {
		super(null);
		this.dal = dal;
		this.raster = null;
		init();
	}

	public PointCloudVisualization(File f) throws IOException {
		super(null);
		String type = StringUtils.getFileType(f.getName());
		String imageTypes[] = ImageIO.getReaderFormatNames();
		Arrays.sort(imageTypes);
		if (Arrays.binarySearch(imageTypes, type) >= 0)
		{
			BufferedImage img = ImageIO.read(f);
			this.raster = img.getRaster();
			init();
		}
		else
		{
			this.dal = IOUtil.readPositionFile(f.getAbsolutePath());
			this.raster = null;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		super.actionPerformed(e);
		Object source = e.getSource();
		if (source == menuItemRecalculate)
		{
			updatePreview();
		}
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 5514312621563618301L;
	@Override
	public void itemStateChanged(ItemEvent arg0) {
		updatePreview();
	}

}

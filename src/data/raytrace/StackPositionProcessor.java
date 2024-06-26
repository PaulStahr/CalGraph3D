package data.raytrace;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.Options;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.RaytraceScene.RaySimulationObject;
import data.raytrace.raygen.ArrayRayGenerator;
import data.raytrace.raygen.RayGenerator;
import geometry.Geometry;
import geometry.Matrix3d;
import geometry.Matrix4d;
import geometry.Vector2d;
import geometry.Vector3d;
import jcomponents.Interface;
import jcomponents.raytrace.RaySimulationData;
import jcomponents.util.ImageUtil;
import maths.Controller;
import maths.Operation;
import maths.OperationCompiler;
import maths.exception.OperationParseException;
import maths.variable.Variable;
import maths.variable.VariableStack;
import util.ArrayUtil;
import util.ImageSaver;
import util.JFrameUtils;
import util.StringUtils;
import util.ThreadPool;
import util.data.DoubleArrayList;
import util.io.IOUtil;

public class StackPositionProcessor {
	private static final Logger logger = LoggerFactory.getLogger(StackPositionProcessor.class);
	public static enum Mode
	{
		ARRAY, COMBINED_ARRAY, SINGLE, CAMERA_TRACK;
	}


	public volatile boolean isRunning = false;
	private int numIterations;
	private int progressMax = 0;
	private BufferedImage img;
	private float normalizationFactor;
	private class SingleThreadLocal
	{
		RayGenerator gen;
		RaySimulationData rsd;
		VariableStack variables;
		RaySimulationObject rso;
		float startpoints[];
		float startdirs[];
		float color[];
	}

	public float getNormalizationFactor()
	{
		return normalizationFactor;
	}

	public static Matrix4d getTransformation(double azimuth0, double elevation0, double distance0, double azimuth1, double elevation1, double distance1)
	{
		Matrix4d m = new Matrix4d();
		setTransformation(m, azimuth0, elevation0, distance0, azimuth1, elevation1, distance1);
		return m;
	}

	public static void setTransformation(Matrix4d m, double azimuth0, double elevation0, double distance0, double azimuth1, double elevation1, double distance1)
	{
		Vector3d pos0 = new Vector3d();
		Geometry.toCart(pos0, azimuth0, elevation0, distance0);
		Vector3d pos1 = new Vector3d();
		Geometry.toCart(pos1, azimuth1, elevation1, distance1);
		m.set(1,0,0,0,0,1,0,0,0,0,1,0,(pos0.x + pos1.x) * 0.5, (pos0.y + pos1.y) * 0.5, (pos0.z + pos1.z) * 0.5,0);
		pos1.sub(pos0);
		Matrix3d m3 = new Matrix3d();
		Vector3d e0 = new Vector3d(1,0,0);
		Geometry.getRotationFromTo(e0, pos1, m3);
		m.set(m3);
	}

	public void evaluate(
			final RaytraceScene scene,
			final double scale,
			String positionFile,
			final boolean surfaceCompensationMode,
			final String outputFolder,
			final AtomicInteger progress,
			Mode mode,
			GuiTextureObject gto,
			final SurfaceObject evaluationObject,
			String rangeBeginStr,
			String rangeEndStr,
			final Runnable updateProgressBarRunnable,
			final int numRays,
			Operation outputResolution,
			OpticalObject lightSource,
			String outputStr,
			final boolean backward,
			RaytraceSession session) throws OperationParseException
	{
		if (session != null)
		{
			StringBuilder strB = new StringBuilder();
			strB
			.append("stp").append(' ')
			.append('"').append(scene.getId()).append('"').append(' ')
			.append(scale).append(' ')
			.append('"').append(positionFile).append('"').append(' ')
			.append(surfaceCompensationMode).append(' ')
			.append('"').append(outputFolder).append('"').append(' ')
			.append(mode.toString()).append(' ')
			.append(gto.getId()).append(' ')
			.append(evaluationObject.getId()).append(' ')
			.append(rangeBeginStr).append(' ')
			.append(rangeEndStr).append(' ')
			.append(numRays).append(' ')
			.append(outputResolution.toString()).append(' ')
			.append(lightSource != null ? lightSource.id : -1).append(' ')
			.append('"').append(outputStr).append('"').append(' ')
			.append(backward).append(' ');
			session.commandExecuted(strB.toString());
		}
		Variable variableFrame = new Variable("frame");
		scene.vs.replaceAddGlobal(variableFrame);
		try {
			final DoubleArrayList dal = positionFile.length() == 0 ? null : IOUtil.readPositionFile(positionFile);
			Controller control = new Controller();

			int rangeBegin = 0;
			int rangeEnd = dal == null ? 0 : mode == Mode.CAMERA_TRACK ? dal.size() / 6 : dal.size() / 2;
			try
			{
				rangeBegin = (int)OperationCompiler.compile(rangeBeginStr).calculate(scene.vs, control).longValue();
				rangeEnd = (int)OperationCompiler.compile(rangeEndStr).calculate(scene.vs, control).longValue();
			}
			catch(Exception e)
			{}
			numIterations = rangeEnd - rangeBegin;
			progressMax = numIterations;
			if (updateProgressBarRunnable != null) {JFrameUtils.runByDispatcher(updateProgressBarRunnable);}

			if (mode == Mode.CAMERA_TRACK)
			{
				Options.OptionTreeInnerNode raytrace = Options.getInnerNode("raytrace");
				double dScale = Options.getFloat(raytrace, "dscale");
				Vector3d tmp = new Vector3d();
				for (int i = rangeBegin; i < rangeEnd && isRunning; ++i)
				{
					progress.set(i - rangeBegin);
					if (updateProgressBarRunnable != null) {JFrameUtils.runByDispatcher(updateProgressBarRunnable);}
					final int index = i * 6;
					final int num = i;
					if (dal != null)
					{
						tmp.set(dal, index);
						Interface.scene.cameraPosition.set(tmp);
						Interface.scene.cameraRotation.setRadians(dal, index + 3);
						tmp.multiply(dScale);
						scene.cameraViewRunnable.gen.position.set(tmp);
						scene.cameraViewRunnable.gen.rotation.setRadians(dal, index + 3);
					}
					//scene.cameraViewRunnable.run();
					scene.cameraViewRunnable.blockOnCalculation();
					synchronized(scene.cameraViewRunnable)
					{
						try
						{
							Thread.sleep(100);
							variableFrame.setValue(i);
							scene.cameraViewRunnable.gen.position.set(tmp);
							scene.cameraViewRunnable.gen.rotation.setRadians(dal, index + 3);
							scene.cameraViewRunnable.blockOnCalculation();
							System.out.println(Thread.currentThread().getId() + " woken " + scene.cameraViewRunnable);
						}catch(InterruptedException e) {}
					}

					DataHandler.runnableRunner.run(new ImageSaver(ImageUtil.deepCopy(gto.image), new File(new StringBuilder().append(outputFolder).append('/').append(num).append('.').append("png").toString())), "Image Saving");
				}
				return;
			}

			final RayGenerator gen = new RayGenerator();
			gen.threeDimensional = true;
			gen.setSource(lightSource);
			if (gto != null)
			{
				gto.load(scene.vs, new ParseUtil());
			}
			final Matrix4d mat = lightSource instanceof MeshObject ? ((MeshObject)lightSource).meshToGlobal : null;
			final ThreadPool.ThreadLocal<SingleThreadLocal> stl = DataHandler.runnableRunner.new ThreadLocal<>();

			if (outputResolution == null)
			{
				throw new NullPointerException("Invalid output Resolution");
			}
			outputResolution = outputResolution.calculate(scene.vs, control);
			final int trWidth = (int)(outputResolution.get(0).longValue()), trHeight = (int)(outputResolution.get(1).longValue());
			switch(mode)
			{
				case SINGLE:
				{
					final int countArray[] = new int[trWidth * trHeight];
					final float imageColorArray[] = new float[trWidth * trHeight * 5];
					img = new BufferedImage(trWidth, trHeight, BufferedImage.TYPE_4BYTE_ABGR);
					numIterations = rangeEnd - rangeBegin;
					final double avarage[] = new double[2 * (rangeEnd - rangeBegin)];
					System.out.println(rangeBegin + '-' + rangeEnd);
					DataHandler.runnableRunner.runParallel(new ThreadPool.ParallelRangeRunnable() {
						@Override
						public void run(int from, int to)
						{
							OpticalObject source = gen.getSource();
							SingleThreadLocal threadLocal = stl.get();
							boolean bidir = source instanceof OpticalSurfaceObject && ((OpticalSurfaceObject)source).bidirectional;
							if (threadLocal == null)
							{
								stl.set(threadLocal = new SingleThreadLocal());
								threadLocal.gen = new RayGenerator(gen);
								if (source instanceof MeshObject)
								{
									ParseUtil parser = new ParseUtil();
									threadLocal.gen.setSource(source.copy(scene.vs, parser));
								}
								threadLocal.rsd = new RaySimulationData(numRays, bidir);
								threadLocal.rso = new RaySimulationObject();
								threadLocal.variables = new VariableStack(scene.vs);
								threadLocal.color = new float[5];
								threadLocal.rso.readColorGen = true;
								threadLocal.rso.readColorBack = false;
								threadLocal.rso.readColorFront = false;
								threadLocal.rso.readColorGen = true;
							}
							RaySimulationData rsd = threadLocal.rsd;
							RaySimulationObject currentRay = threadLocal.rso;
							//TODO
							Matrix4d tmp = new Matrix4d();
							ParseUtil parser = new ParseUtil();
							for (int i = from; i < to && isRunning; ++i, progress.incrementAndGet())
							{
								int idx = i - from;
								if (source instanceof OpticalSurfaceObject)
								{
									if (dal != null)
									{
										double azimuth = dal.getD(i * 2);
										double elevation = dal.getD(i * 2 + 1);
										if (Double.isNaN(azimuth) || Double.isNaN(elevation))
										{
											continue;
										}
										gen.setArcs(elevation, azimuth);
									}
								}
								else if (source instanceof MeshObject)
								{
									MeshObject mesh = (MeshObject)source;
									mesh.meshToGlobal.set(mat);
									setTransformation(tmp, dal.getD(i * 6), dal.getD(i * 6 + 1), dal.getD(i * 6 + 2) * scale, dal.getD(i * 6 + 3), dal.getD(i * 6 + 4), dal.getD(i * 6 + 5) * scale);
									mesh.meshToGlobal.dotr(tmp);
									try {
										mesh.updateValue(SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, threadLocal.variables, parser);
									} catch (OperationParseException e) {
										logger.error("Can't update Transformation", e);
									}
								}
								scene.calculateRays(0, numRays, numRays, gen, 0, 0, null, null, rsd.endpoints, rsd.enddirs, rsd.endcolor, null, rsd.accepted, rsd.bounces, rsd.lastObject, 10, bidir, currentRay, RaytraceScene.UNACCEPTED_DELETE);
								int count = 0;
								float tt[] = new float[5];
								tt[4] = 1;
								for (int j = 0; j < numRays; ++j)
								{
									if (rsd.lastObject[j] == evaluationObject && rsd.accepted[j] == RaytraceScene.STATUS_ACCEPTED)
									{
										currentRay.position.set(rsd.endpoints, j * 3);
										currentRay.direction.set(rsd.enddirs, j * 3);
										evaluationObject.getTextureCoordinates(currentRay.position, currentRay.direction, currentRay.v3);
										++count;
										ImageUtil.addToPixel(currentRay.v3.x * trWidth, currentRay.v3.y * trHeight, trWidth, trHeight, 1, countArray);
										currentRay.v3.addTo(avarage, idx * 2);
										System.arraycopy(rsd.endcolor, j * 4, tt, 0, 4);
										ImageUtil.addToPixel(currentRay.v3.x * trWidth, currentRay.v3.y * trHeight, trWidth, trHeight, tt, 0, 5, 1, imageColorArray);
									}
								}
								ArrayUtil.mult(avarage, idx * 2, idx * 2 + 1, 1. / count);
								if (updateProgressBarRunnable != null) {JFrameUtils.runByDispatcher(updateProgressBarRunnable);}
							}
						}

						@Override
						public void finished() {}
					}, "StackPositionProcessor", null, rangeBegin, rangeEnd, 1000, true);
					if (outputStr.length() != 0)
					{
						StringUtils.writeTapSeperated(avarage, new File(outputStr), 2);
					}
					for (int i = 0; i < trWidth * trHeight; ++i)
					{
						if (imageColorArray[i * 5 + 4] != 0)
						{
							ArrayUtil.mult(imageColorArray, i * 5, i * 5 + 4, 1f/imageColorArray[i * 5 + 4]);
							imageColorArray[i * 5 + 4] = -imageColorArray[i * 5 + 4];
						}
					}
					//normalizationFactor = ArrayUtil.normalizeTo(imageColorArray, 0, imageColorArray.length, 255);
					for (int i = 0; i < trWidth * trHeight; ++i)
					{
						if (imageColorArray[i * 5 + 4] != 0)
						{
							imageColorArray[i * 5 + 3] = 255;
						}
					}
					img = new BufferedImage(trWidth, trHeight, BufferedImage.TYPE_4BYTE_ABGR);
					ImageUtil.setRGB(img.getRaster(), imageColorArray, new int[4], 4, 5);
					/*if (gto != null)
					{
						ImageUtil.setRGBAChannels(gto.raster, countArray, new int[4]);
						if (surfaceCompensationMode && evaluationObject instanceof GuiOpticalSurfaceObject)
						{
							((GuiOpticalSurfaceObject)evaluationObject).textureMapping.densityCompensation(gto.raster);
						}
						gto.modified();
						gto.triggerModificationEvents();
						new TextureView(gto.image).setVisible(true);
						try {
							ImageIO.write(gto.image, "png", new File(outputFolder));
						} catch (IOException e1) {
							logger.error("Input Output error", e1);
						}
					}*/
					break;
				}
				case ARRAY:
				{
					final long imageColorArray[] = new long[trWidth * trHeight * 5];

					//BufferedImage bi = gto.image;
					/*if (bi == null)
					{
						JFrameUtils.logErrorAndShow("No Input Image", null, logger);
					}*/
					Options.OptionTreeInnerNode raytrace = Options.getInnerNode("raytrace");
					final int blocksize = Options.getInteger(raytrace, "blocksize", 100000);
					if (logger.isDebugEnabled()) {logger.debug("Starting stack processing: Blocksize " + blocksize);}
					Matrix4d tmp = new Matrix4d();
					final StringBuilder strB = new StringBuilder();
					progress.set(0);
					progressMax = numRays * (rangeEnd - rangeBegin);
					for (int i = rangeBegin; i < rangeEnd; ++i)
					{
						variableFrame.setValue(i);
						scene.rayUpdateHandler.update();
						final int index = i;
						if (!isRunning)
						{
							break;
						}
						//gto.setImage(ImageUtil.deepCopy(bi));
						if (lightSource instanceof OpticalSurfaceObject)
						{
							if (dal != null)
							{
								double azimuth = dal.getD(i * 2);
								double elevation = dal.getD(i * 2 + 1);
								if (Double.isNaN(azimuth) || Double.isNaN(elevation))
								{
									continue;
								}
								gen.setArcs(elevation, azimuth);
							}
						}
						else if (lightSource instanceof MeshObject)
						{
							MeshObject mesh = (MeshObject)lightSource;
							mesh.meshToGlobal.set(mat);
							setTransformation(tmp, dal.getD(i * 6), dal.getD(i * 6 + 1), dal.getD(i * 6 + 2) * scale, dal.getD(i * 6 + 3), dal.getD(i * 6 + 4), dal.getD(i * 6 + 5) * scale);
							mesh.meshToGlobal.dotr(tmp);
							try {
								ParseUtil parser = new ParseUtil();
								mesh.updateValue(SCENE_OBJECT_COLUMN_TYPE.TRANSFORMATION, scene.vs, parser);
							} catch (OperationParseException e) {
								logger.error("Can't update Transformation", e);
							}
						}
						DataHandler.runnableRunner.runParallel(new ThreadPool.ParallelRangeRunnable() {
							@Override
							public void run(int from, int to) {
								if (!isRunning)
								{
									return;
								}
								SingleThreadLocal threadLocal = stl.get();
								OpticalObject source = gen.getSource();
								boolean bidir = source instanceof OpticalSurfaceObject && ((OpticalSurfaceObject)source).bidirectional;
								if (threadLocal == null)
								{
									threadLocal = new SingleThreadLocal();
									threadLocal.rso = new RaySimulationObject();
									//threadLocal.rso.textureDrawMode = TextureDrawMode.ALPHA_ADDITIVE;
									threadLocal.rso.readColorGen = true;
									threadLocal.rso.readColorBack = false;
									threadLocal.rso.readColorGen = true;
									threadLocal.rsd = new RaySimulationData(blocksize, bidir);
									threadLocal.startdirs = new float[threadLocal.rsd.enddirs.length];
									threadLocal.startpoints = new float[threadLocal.rsd.endpoints.length];
									threadLocal.color = new float[5];
									stl.set(threadLocal);
								}
								RaySimulationData rsd = threadLocal.rsd;
								RaySimulationObject currentRay = threadLocal.rso;
								currentRay.readColorFront = backward;
								OpticalObject lastObject[] = threadLocal.rsd.lastObject;

								if (rsd == null)
								{
									throw new NullPointerException();
								}
								int toCalculate = to - from;
								scene.calculateRays(0, toCalculate, numRays, gen, 0, 0, threadLocal.startpoints, threadLocal.startdirs, rsd.endpoints, rsd.enddirs, rsd.endcolor, null, rsd.accepted, rsd.bounces, lastObject, 10, bidir, currentRay, RaytraceScene.UNACCEPTED_DELETE);

								Vector2d v2 = currentRay.v3;
                                float color[] = threadLocal.color;
                                color[4] = 255;
                                for (int j = 0; j < toCalculate; ++j)
								{
									if (lastObject[j] == evaluationObject && rsd.accepted[j] == RaytraceScene.STATUS_ACCEPTED)
									{
										if (backward)
										{
										    //Read from endpoint, write to source
											System.arraycopy(rsd.endcolor, 4*j, color, 0, 4);
                                            currentRay.position.set(threadLocal.startpoints, j * 3);
                                            currentRay.direction.set(threadLocal.startdirs, j * 3);
                                            ((OpticalSurfaceObject)source).getTextureCoordinates(currentRay.position, currentRay.direction, v2);
											ImageUtil.addToPixel(v2.x * trWidth, v2.y * trHeight, trWidth, trHeight, color, 0, 5, 1f/255, imageColorArray);
										}
										else
										{
										    //Read from source, write to endpoint
											currentRay.position.set(rsd.endpoints, j * 3);
											currentRay.direction.set(rsd.enddirs, j * 3);
											System.arraycopy(rsd.endcolor, 4*j, color, 0, 4);
								            evaluationObject.getTextureCoordinates(currentRay.position, currentRay.direction, v2);
											ImageUtil.addToPixel(v2.x * trWidth, v2.y * trHeight, trWidth, trHeight, color, 0, 5, 1f/255, imageColorArray);
										}
									}
								}
								progress.addAndGet(toCalculate);
								if (updateProgressBarRunnable != null) {JFrameUtils.runByDispatcher(updateProgressBarRunnable);}
							}

							@Override
							public void finished() {}
						}, "StackPositionProcessor", null, 0, numRays, blocksize, true);
						strB.setLength(0);
						DataHandler.runnableRunner.run(new Runnable() {
							final long imageColorArrayCopy[] = imageColorArray.clone();
							final String filename = strB.append(outputFolder).append('/').append(index).append('c').append('.').append("png").toString();

							@Override
							public void run()
							{
							    if (surfaceCompensationMode)
							    {
							        evaluationObject.densityCompensation(trWidth, trHeight, imageColorArrayCopy, 5, 5);
							    }

							    if (backward)
							    {
							        ArrayUtil.normalizeTo(imageColorArrayCopy, 0, imageColorArrayCopy.length, 255);
							    }
							    else
							    {
					                for (int i = 0; i < trWidth * trHeight; ++i)
				                    {
				                        if (imageColorArray[i * 5 + 4] > 255)
				                        {
				                            ArrayUtil.divide(imageColorArrayCopy, i * 5, i * 5 + 4, imageColorArrayCopy[i * 5 + 3] / 255);
				                        }
				                    }
							    }

								BufferedImage img2 = new BufferedImage(trWidth, trHeight, BufferedImage.TYPE_4BYTE_ABGR);

								for (int i = 0; i < imageColorArrayCopy.length; i += 5)
								{
									if (imageColorArrayCopy[i + 4] != 0)
									{
										//imageColorArrayCopy[i + 3] = 255;
									}
								}
								ImageUtil.setRGB(img2.getRaster(), imageColorArrayCopy, new int[4], 4, 5);
								try {
									ImageIO.write(img2, "png", new File(filename));
								} catch (IOException e) {
									logger.error("Can't write image " + filename, e);
								}
							}
						}, "ImageSaving");
						//DataHandler.runnableRunner.run(new ImageSaver(ImageUtil.deepCopy(gto.image), new File(strB.append(outputFolder).append('/').append(index).append('.').append("png").toString())), "Image Saving");
						//strB.setLength(0);
					}
					break;
				}
				case COMBINED_ARRAY:
				{
					Options.OptionTreeInnerNode raytrace = Options.getInnerNode("raytrace");
					final int blocksize = Options.getInteger(raytrace, "blocksize", 100000);
					final float midPos[] = new float[numRays * 3];
					final float midDir[] = new float[numRays * 3];
					final float textureCoords[] = new float[numRays * 2];
					final float color[] = new float[numRays * 5];
					final OpticalObject centerObject = scene.getOpticalObject("AnteriorCornea"); //TODO
					Arrays.fill(midPos, Float.NaN);
					Arrays.fill(midDir, Float.NaN);
					Arrays.fill(textureCoords, Float.NaN);
					DataHandler.runnableRunner.runParallel(new ThreadPool.ParallelRangeRunnable() {
						@Override
						public void run(int from, int to) {
							if (!isRunning)
							{
								return;
							}
							SingleThreadLocal threadLocal = stl.get();
							OpticalObject source = gen.getSource();
							boolean bidir = source instanceof OpticalSurfaceObject && ((OpticalSurfaceObject)source).bidirectional;
							if (threadLocal == null)
							{
								threadLocal = new SingleThreadLocal();
								threadLocal.rso = new RaySimulationObject();
								//threadLocal.rso.textureDrawMode = TextureDrawMode.ALPHA_ADDITIVE;
								threadLocal.rso.readColorGen = true;
								threadLocal.rso.readColorBack = false;
								threadLocal.rso.readColorFront = false;
								threadLocal.rso.readColorGen = true;
								threadLocal.color = new float[5];
								threadLocal.rsd = new RaySimulationData(Math.min(blocksize, numRays), bidir);
								threadLocal.startdirs = new float[threadLocal.rsd.enddirs.length];
								threadLocal.startpoints = new float[threadLocal.rsd.endpoints.length];
								stl.set(threadLocal);
							}
							RaySimulationData rsd = threadLocal.rsd;
							RaySimulationObject currentRay = threadLocal.rso;
							OpticalObject lastObject[] = threadLocal.rsd.lastObject;

							if (rsd == null)
							{
								throw new NullPointerException();
							}
							int toCalculate = to - from;
							scene.calculateRays(0, toCalculate, numRays, gen, 0, 0, threadLocal.startpoints, threadLocal.startdirs, rsd.endpoints, rsd.enddirs, rsd.endcolor, null, rsd.accepted, rsd.bounces, lastObject, 10, bidir, currentRay, RaytraceScene.UNACCEPTED_DELETE);

							Vector2d v2 = currentRay.v3;
							for (int j = 0; j < toCalculate; ++j)
							{
								if (rsd.accepted[j] == RaytraceScene.STATUS_ACCEPTED && lastObject[j] == centerObject)
								{
									System.arraycopy(rsd.endpoints, j * 3, midPos, (from + j) * 3, 3);
									System.arraycopy(rsd.enddirs, j * 3, midDir, (from + j) * 3, 3);

									currentRay.position.set(threadLocal.startpoints, j * 3);
									currentRay.direction.write(threadLocal.startdirs, j * 3);
									((SurfaceObject)source).getTextureCoordinates(currentRay.position, currentRay.direction, v2);
									v2.write(textureCoords, (from + j) * 2);
								}
							}
							progress.addAndGet(toCalculate);
							if (updateProgressBarRunnable != null) {JFrameUtils.runByDispatcher(updateProgressBarRunnable);}
						}

						@Override
						public void finished() {}
					}, "StackPositionProcessor", null, 0, numRays, blocksize, true);
					int numAcceptedRays = 0;
					for (int read = 0; read < numRays; ++read)
					{
						if (!Float.isNaN(midPos[read * 3]))
						{
							System.arraycopy(midPos, read * 3, midPos, numAcceptedRays * 3, 3);
							System.arraycopy(midDir, read * 3, midDir, numAcceptedRays * 3, 3);
							System.arraycopy(textureCoords, read * 2, textureCoords, numAcceptedRays * 2, 2);
							++numAcceptedRays;
						}
					}
					System.out.println("num accepted:" + numAcceptedRays);
					final float midPos2[] = Arrays.copyOf(midPos, numAcceptedRays * 3);
					final float midDir2[] = Arrays.copyOf(midDir, numAcceptedRays * 3);
					final float textureCoords2[] = Arrays.copyOf(textureCoords, numAcceptedRays * 2);
					final ArrayRayGenerator arrayGen = new ArrayRayGenerator(midPos2, midDir2, textureCoords2);
					for (int i = rangeBegin; i < rangeEnd; ++i)
					{
						variableFrame.setValue(i);
						scene.rayUpdateHandler.update();
						//final int index = i;
						if (!isRunning)
						{
							break;
						}
						DataHandler.runnableRunner.runParallel(new ThreadPool.ParallelRangeRunnable() {
							@Override
							public void run(int from, int to) {
								if (!isRunning)
								{
									return;
								}
								SingleThreadLocal threadLocal = stl.get();
								OpticalObject source = gen.getSource();
								boolean bidir = source instanceof OpticalSurfaceObject && ((OpticalSurfaceObject)source).bidirectional;
								if (threadLocal == null)
								{
									threadLocal = new SingleThreadLocal();
									threadLocal.rso = new RaySimulationObject();
									//threadLocal.rso.textureDrawMode = TextureDrawMode.ALPHA_ADDITIVE;
									threadLocal.rso.readColorGen = true;
									threadLocal.rso.readColorBack = false;
									threadLocal.rso.readColorFront = false;
									threadLocal.rso.readColorGen = true;
									threadLocal.color = new float[5];
									threadLocal.rsd = new RaySimulationData(blocksize, bidir);
									threadLocal.startdirs = new float[threadLocal.rsd.enddirs.length];
									threadLocal.startpoints = new float[threadLocal.rsd.endpoints.length];
									stl.set(threadLocal);

								}
								RaySimulationData rsd = threadLocal.rsd;
								RaySimulationObject currentRay = threadLocal.rso;
								OpticalObject lastObject[] = threadLocal.rsd.lastObject;

								if (rsd == null)
								{
									throw new NullPointerException();
								}
								int toCalculate = to - from;
								scene.calculateRays(0, toCalculate, numRays, arrayGen, from, 0, threadLocal.startpoints, threadLocal.startdirs, rsd.endpoints, rsd.enddirs, rsd.endcolor, null, rsd.accepted, rsd.bounces, lastObject, 10, bidir, currentRay, RaytraceScene.UNACCEPTED_DELETE);

								Vector2d v2 = currentRay.v3;
								for (int j = 0; j < toCalculate; ++j)
								{
									//TODO strange accepted
									if (lastObject[j] == evaluationObject && rsd.accepted[j] == RaytraceScene.STATUS_ACCEPTED)
									{
										currentRay.position.set(rsd.endpoints, j * 3);
										currentRay.direction.set(rsd.enddirs, j * 3);
										RaytraceScene.readColor(evaluationObject, currentRay.position, currentRay.direction, v2, threadLocal.color);
										threadLocal.color[4] = 1;
										ArrayUtil.addTo(threadLocal.color, 0, 5, color, (j + from) * 5);
									}
								}
								progress.addAndGet(toCalculate);
								if (updateProgressBarRunnable != null) {JFrameUtils.runByDispatcher(updateProgressBarRunnable);}
							}

							@Override
							public void finished() {}
						}, "StackPositionProcessor", null, 0, numAcceptedRays, blocksize, true);


					}
					final float imageColorArray[] = new float[trWidth * trHeight * 5];
					for (int j = 0; j < numAcceptedRays; ++j)
					{
						ImageUtil.addToPixel(textureCoords2[j * 2] * trWidth, textureCoords2[j * 2 + 1] * trHeight, trWidth, trHeight, color, j * 5, j * 5 + 5, 1, imageColorArray);
					}
					for (int i = 0; i < trWidth * trHeight; ++i)
					{
						if (imageColorArray[i * 5 + 4] != 0)
						{
							ArrayUtil.mult(imageColorArray, i * 5, i * 5 + 4, 1f/imageColorArray[i * 5 + 4]);
							imageColorArray[i * 5 + 4] = -imageColorArray[i * 5 + 4];
						}
					}
					normalizationFactor = ArrayUtil.normalizeTo(imageColorArray, 0, imageColorArray.length, 255);
					for (int i = 0; i < trWidth * trHeight; ++i)
					{
						if (imageColorArray[i * 5 + 4] != 0)
						{
							imageColorArray[i * 5 + 3] = 255;
						}
					}
					img = new BufferedImage(trWidth, trHeight, BufferedImage.TYPE_4BYTE_ABGR);
					ImageUtil.setRGB(img.getRaster(), imageColorArray, new int[4], 4, 5);
				}
				break;
			case CAMERA_TRACK:
				break;
			default:
				break;
			}
			DataHandler.globalVariables.del(variableFrame);
		} catch (FileNotFoundException e1) {
			logger.error("File not found", e1);
		} catch (IOException e1) {
			logger.error("Input Output error", e1);
		} catch (OutOfMemoryError e1) {
			logger.error("Not enough memory", e1);
		}
	}

	public int getProgressMax() {
		return progressMax;
	}

	public BufferedImage getImg() {
		return img;
	}
}

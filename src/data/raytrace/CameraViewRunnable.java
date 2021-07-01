package data.raytrace;

import java.awt.image.WritableRaster;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import data.raytrace.RaytraceScene.RaySimulationObject;
import data.raytrace.raygen.ImageRayGenerator;
import util.ArrayUtil;
import util.ThreadPool;
import util.ThreadPool.RunnableObject;
import util.data.UniqueObjects;

public class CameraViewRunnable  extends RunnableObject{
	private static final Logger logger = LoggerFactory.getLogger(CameraViewRunnable.class);
	RaytraceScene scene;

	public CameraViewRunnable(RaytraceScene scene)
	{
		super("Scene View", null);
		scene.add(this);
		this.scene = scene;
	}
	private final ThreadPool.ThreadLocal<RaySimulationObject> rso = DataHandler.runnableRunner.new ThreadLocal<>();
		public final ImageRayGenerator gen = new ImageRayGenerator();

		float enddirs[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
		float endpoints[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
		byte accepted[] = UniqueObjects.EMPTY_BYTE_ARRAY;
	private float sceneEndpointColor[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
	private float sceneEndpointColorAdded[] = UniqueObjects.EMPTY_FLOAT_ARRAY;
	int bounces[] = UniqueObjects.EMPTY_INT_ARRAY;
	private OpticalObject lastObject[] = OpticalObject.EMPTY_ARRAY;
	int numPixels;
	int maxBounces = 10;
	public GuiTextureObject gto;
	public int passes = 1;
	private volatile boolean calculating = false;

	public boolean isRunning() {
		return calculating;
	}

	private final ThreadPool.ParallelRangeRunnable prr = new ThreadPool.ParallelRangeRunnable() {

		@Override
		public void run(int from, int to) {
			Arrays.fill(sceneEndpointColor, from * 4, to * 4, 0);
			WritableRaster raster = gto.raster;
			RaySimulationObject r = rso.get();
			if (r == null)
			{
				rso.set(r = new RaySimulationObject());
			}

			if (passes > 1)
			{
				Arrays.fill(sceneEndpointColorAdded, from * 4, to * 4, 0);
			}
			for (int i = 0; i < passes; ++i)
			{
				scene.calculateRays(from, to, numPixels, gen, from, from, null, null, endpoints, enddirs, sceneEndpointColor, null, accepted, bounces, lastObject, maxBounces, false, r, RaytraceScene.UNACCEPTED_MARK);
				if (passes > 1)
				{
				    ArrayUtil.add(sceneEndpointColor, from * 4, to * 4, sceneEndpointColorAdded, from * 4);
				}
			}
			if (passes > 1)
			{
			    ArrayUtil.mult(sceneEndpointColorAdded, from * 4, to * 4, sceneEndpointColor, from * 4, 1f/passes);
			}

			float pixel[] = r.color;
			pixel[3] = 1;
   			int width = raster.getWidth();
   			for (int i = from; i < to; ++i)
   			{
   				for (int j = 0; j < 3; ++j)
   				{
   					pixel[j] = sceneEndpointColor[i * 4 + j];
   				}
   				raster.setPixel(i % width, i / width, pixel);
   			}
   			gto.modified();
		}

		@Override
		public void finished() {CameraViewRunnable.this.finished();}
	};

		@Override
		public void run()
		{
			if (gto == null)
			{
				return;
			}
			synchronized(CameraViewRunnable.this)
			{
				calculating = true;
			}
			int width = gen.width = gto.image.getWidth();
			int height = gen.height = gto.image.getHeight();
			numPixels = width * height;
			if (enddirs.length != numPixels * 3 || bounces.length  != numPixels)
			{
				endpoints = new float[numPixels * 3];
				enddirs = new float[numPixels * 3];
				sceneEndpointColor = new float[numPixels * 4];
				accepted = new byte[numPixels];
				bounces = new int[numPixels];
				lastObject = new OpticalObject[numPixels];
			}
			if (passes > 1 && sceneEndpointColorAdded.length != numPixels * 4)
			{
				sceneEndpointColorAdded = new float[numPixels * 4];
			}
			DataHandler.runnableRunner.runParallel(prr, "Scene View", null, 0, numPixels, 200000, true);

			gto.triggerModificationEvents();
			synchronized(CameraViewRunnable.this)
			{
	   			calculating=false;
				CameraViewRunnable.this.notifyAll();
			}
		}

		public void finished() {}

		public void blockOnCalculation()
		{
			synchronized(this)
			{
				if (calculating)
				{
					try {
						wait();
					} catch (InterruptedException e) {
						logger.error("Unexpected Interrupt", e);
					}
				}
			}
		}
}

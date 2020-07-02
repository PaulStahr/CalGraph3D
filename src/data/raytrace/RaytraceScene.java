package data.raytrace;

import java.awt.image.WritableRaster;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import data.DataHandler;
import data.raytrace.GuiOpticalSurfaceObject.OpticalSurfaceObjectChangeListener;
import data.raytrace.GuiOpticalVolumeObject.OpticalVolumeObjectChangeListener;
import data.raytrace.GuiTextureObject.TextureObjectChangeListener;
import data.raytrace.MeshObject.MeshObjectChangeListener;
import data.raytrace.OpticalObject.SCENE_OBJECT_COLUMN_TYPE;
import data.raytrace.RaySimulation.AlphaCalculation;
import data.raytrace.RaySimulation.MaterialType;
import data.raytrace.raygen.AbstractRayGenerator;
import data.raytrace.raygen.ImageRayGenerator;
import geometry.Vector2d;
import geometry.Vector3d;
import maths.Controller;
import maths.Variable;
import maths.VariableStack;
import maths.VariableStack.VariableObserver.PendendList;
import maths.exception.OperationParseException;
import util.ArrayUtil;
import util.ListTools;
import util.RunnableRunner;
import util.RunnableRunner.RunnableObject;
import util.TimedUpdateHandler;
import util.data.UniqueObjects;

public class RaytraceScene {
	public final ArrayList<VolumePipeline> volumePipelines = new ArrayList<VolumePipeline>();
	public final ArrayList<GuiOpticalSurfaceObject> surfaceObjectList = new ArrayList<GuiOpticalSurfaceObject>();
	public final ArrayList<GuiOpticalVolumeObject> volumeObjectList = new ArrayList<GuiOpticalVolumeObject>();
	public final ArrayList<GuiTextureObject> textureObjectList = new ArrayList<GuiTextureObject>();
	public final ArrayList<MeshObject> meshObjectList = new ArrayList<MeshObject>();
	private GuiOpticalSurfaceObject activeSurfaces[] = GuiOpticalSurfaceObject.EMPTY_SURFACE_ARRAY;
	private GuiOpticalSurfaceObject activeEmissions[] = GuiOpticalSurfaceObject.EMPTY_SURFACE_ARRAY;
	private OpticalObject activeLights[] = OpticalObject.EMPTY_ARRAY;
	private GuiOpticalVolumeObject activeVolumes[] = GuiOpticalVolumeObject.EMPTY_VOLUME_ARRAY;
	private MeshObject activeMeshes[] = MeshObject.EMPTY_MESH_ARRAY;
	private OpticalObject activeObjects[] = OpticalObject.EMPTY_ARRAY;
	private GuiTextureObject activeTextures[] = GuiTextureObject.EMPTY_TEXTTURE_ARRAY;
	private ArrayList<GuiOpticalSurfaceObject> tmpSurfaceArrayList = new ArrayList<>();
	private ArrayList<GuiOpticalVolumeObject> tmpVolumeArrayList = new ArrayList<>();
	private ArrayList<MeshObject> tmpMeshArrayList = new ArrayList<>();
	private final ArrayList<OpticalSurfaceObjectChangeListener> surfaceChangeListenerList = new ArrayList<>();
	private final ArrayList<OpticalVolumeObjectChangeListener> volumeObjectChangeListenerList = new ArrayList<>();
	private final ArrayList<DataChangeListener> dataChangeListener = new ArrayList<>();
	private final ArrayList<TextureObjectChangeListener> textureObjectChangeListenerList = new ArrayList<>();
	private final ArrayList<MeshObjectChangeListener> meshObjectChangeListenerList = new ArrayList<>();
	private final ArrayList<WeakReference<CameraViewRunnable>> cameraViewRunnables = new ArrayList<>();
	private final ParseUtil parser = new ParseUtil();
	public Object forceStartpoint;
	public Object forceEndpoint;
	public String environmentTextureString;
	public String writableEnvironmentTextureString;
	public String renderToTextureString;
	public GuiTextureObject environmentTexture;
	public GuiTextureObject writableEnvironmentTexture;
	private final ArrayList<SceneChangeListener> sceneChangeListener = new ArrayList<>();
	public GuiTextureObject renderToTextureObject = null;
	private boolean verifyRefractionIndices = false;
	public final VariableStack vs = new VariableStack(DataHandler.globalVariables);
	public TextureMapping environment_mapping = TextureMapping.SPHERICAL;
	public static final ArrayList<WeakReference<RaytraceScene> > openedInstances = new ArrayList<>(); 
	private String id;
	
	public static final byte FORCE_STARTPOINT = 0;
	public static final byte FORCE_ENDPOINT = 1;
	public static final byte ENVIRONMENT_TEXTURE = 2;
	public static final byte WRITABLE_ENVIRONMENT_TEXTURE = 3;
	public static final byte RENDER_TO_TEXTURE = 4;
	public static final byte OBJECT_ADD= 5;
	public static final byte OBJECT_REMOVE= 6;
	public static final byte VERIFY_REFRACTION_INDICES = 7;
	public static final byte ENVIRONMENT_MAPPING = 8;
	
	public final TimedUpdateHandler rayUpdateHandler = new TimedUpdateHandler() {
    	
    	private final VariableStack.VariableObserver variableObserver= vs.createVaribleObserver();
    	private final VariableStack.VariableObserver globalVariableObserver= DataHandler.globalVariables.createVaribleObserver();
		private int modCount = -1;
		private final PendendList changedVariables = variableObserver.getPendentVariableList();
		private final PendendList globalChangedVariables = globalVariableObserver.getPendentVariableList();
		
		private final <E extends OpticalObject> void update(ArrayList<E> list)
		{
			for (int i = 0; i < list.size(); ++i)
			{
				list.get(i).update(changedVariables, vs, parser);
				list.get(i).update(globalChangedVariables, vs, parser);
			}
		}
		
		@Override
		public final synchronized void update() {
			int newModCount = vs.modCount();
			if (newModCount != modCount){
				modCount = newModCount;
				variableObserver.updateChanges();
				globalVariableObserver.updateChanges();
				update(surfaceObjectList);
				update(textureObjectList);
				update(volumeObjectList);
				update(meshObjectList);
			}
		}	
		
		@Override
		public final int getUpdateInterval() {
			return 10;
		}
	};

	private static final void cleanUp()
	{
		ListTools.clean(openedInstances);
	}
	
	public static RaytraceScene getScene(String string) {
		for (int i = 0; i < openedInstances.size(); ++i)
		{
			RaytraceScene scene = openedInstances.get(i).get(); 
			if (scene.id.equals(string))
			{
				return scene;
			}
		}
		return null;
	}
	
	public RaytraceScene(String id)
	{
		this.id = id;
		DataHandler.timedUpdater.add(rayUpdateHandler);
		cleanUp();
		openedInstances.add(new WeakReference<RaytraceScene>(this));
	}
	
	public static interface SceneChangeListener
	{
		public void valueChanged(byte ct, Object o);
	}
	
	public void add(SceneChangeListener scl)
	{
		sceneChangeListener.add(scl);
	}
	
	public void remove(SceneChangeListener scl)
	{
		sceneChangeListener.remove(scl);
	}
	
	public void valueChanged(byte sct, Object o)
	{
		++updateCount;
		for (int i = 0; i < sceneChangeListener.size(); ++i)
		{
			sceneChangeListener.get(i).valueChanged(sct, o);
		}
	}
	
	public class CameraViewRunnable extends RunnableObject
	{
		public CameraViewRunnable()
		{
			super("Scene View", null);
			cameraViewRunnables.add(new WeakReference<CameraViewRunnable>(this));
		}
		private final RunnableRunner.ThreadLocal<RaySimulationObject> rso = DataHandler.runnableRunner.new ThreadLocal<>();
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
		
		private final RunnableRunner.ParallelRangeRunnable prr = new RunnableRunner.ParallelRangeRunnable() {
			
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
					calculateRays(from, to, numPixels, gen, from, from, null, null, endpoints, enddirs, sceneEndpointColor, null, accepted, bounces, lastObject, maxBounces, false, r, UNACCEPTED_MARK);
					if (passes > 1)
					{
						for (int j = from * 4; j < to * 4; ++j)
						{
							sceneEndpointColorAdded[j] += sceneEndpointColor[j];
						}
					}
				}
				if (passes > 1)
				{
					for (int j = from * 4; j < to * 4; ++j)
					{
						sceneEndpointColor[j] = sceneEndpointColorAdded[j] / passes;
					}
				}
				
				int pixel[] = r.color;
				pixel[3] = 255;
	   			int width = raster.getWidth();
	   			for (int i = from; i < to; ++i)
	   			{
	   				for (int j = 0; j < 3; ++j)
	   				{
	   					pixel[j] = (int)(sceneEndpointColor[i * 4 + j] * 255);
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
   			int width = gto.image.getWidth();
   			int height = gto.image.getHeight();
   			gen.width = width;
   			gen.height = height;
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
   			
   			DataHandler.runnableRunner.runParallelAndWait(prr, "Scene View", null, 0, numPixels, 200000);
   			
   			gto.triggerModificationEvents();
   			synchronized(this)
   			{
   				notifyAll();
   			}
   		}
   		
   		public void finished() {}
	}
	
   	public final CameraViewRunnable cameraViewRunnable = new CameraViewRunnable();
	
	private int updateCount = 0;
	private int lastSceneUpdate = 0;
	private Object forceEndpointObject;
	private Object forceStartpointObject;
	
	private void getObjects(String ids[], double ior, ArrayList<GuiOpticalSurfaceObject> surfaces, ArrayList<GuiOpticalVolumeObject> volumes, ArrayList<MeshObject> meshes) {
		for (GuiOpticalSurfaceObject goo : activeSurfaces)
		{
			if ((ids == null || Arrays.binarySearch(ids, goo.id) >= 0) && (!verifyRefractionIndices || Double.isNaN(ior) || goo.ior1.doubleValue() == ior || goo.ior0.doubleValue() == ior))
			{
				tmpSurfaceArrayList.add(goo);
			}
		}
		for (GuiOpticalVolumeObject gov : activeVolumes)
		{	
			if (ids == null || Arrays.binarySearch(ids, gov.id) >= 0)
			{
				tmpVolumeArrayList.add(gov);
			}
		}
		for (MeshObject mo : activeMeshes)
		{
			if ((ids == null || Arrays.binarySearch(ids, mo.id) >= 0) && (!verifyRefractionIndices || Double.isNaN(ior) || mo.ior1.doubleValue() == ior || mo.ior0.doubleValue() == ior))
			{
				tmpMeshArrayList.add(mo);
			}
		}
	}
	
	private String cameraStartObjects[];
	
	public void setCameraStartObjects(String cameraStartObjects[])
	{
		this.cameraStartObjects = cameraStartObjects;
		++updateCount;
	}
	
	private void updateNeighbours(OpticalObject current)
	{
		double ior0 = Double.NaN, ior1 = Double.NaN;
		if (current instanceof OpticalSurfaceObject)
		{
			OpticalSurfaceObject surf = (OpticalSurfaceObject)current;
			ior0 = surf.ior0.doubleValue();
			ior1 = surf.ior1.doubleValue();
			surf.textureObject = getActiveTexture(surf.textureObjectStr);
		}
		
		if (current.successorArray != null || verifyRefractionIndices)
		{
			if (current.successorArray != null)
			{
				Arrays.sort(current.successorArray);
			}
			getObjects(current.successorArray, ior1,tmpSurfaceArrayList, tmpVolumeArrayList, tmpMeshArrayList);
			if (current.surfaceSuccessor == activeSurfaces || current.surfaceSuccessor.length != tmpSurfaceArrayList.size())
			{
				current.surfaceSuccessor = new GuiOpticalSurfaceObject[tmpSurfaceArrayList.size()];
			}
			current.surfaceSuccessor = tmpSurfaceArrayList.toArray(current.surfaceSuccessor);
			
			if (current.volumeSuccessor == activeVolumes || current.volumeSuccessor.length != tmpVolumeArrayList.size())
			{
				current.volumeSuccessor = new GuiOpticalVolumeObject[tmpVolumeArrayList.size()];
			}
			current.volumeSuccessor = tmpVolumeArrayList.toArray(current.volumeSuccessor);
			
			if (current.meshSuccessor == activeMeshes || current.meshSuccessor.length != tmpMeshArrayList.size())
			{
				current.meshSuccessor = new MeshObject[tmpMeshArrayList.size()];
			}
			current.meshSuccessor = tmpMeshArrayList.toArray(current.meshSuccessor);
			current.successor = concatenate(current.surfaceSuccessor, current.meshSuccessor, current.volumeSuccessor, current.successor);
			tmpMeshArrayList.clear();
			tmpSurfaceArrayList.clear();
			tmpVolumeArrayList.clear();
			volumePipelines.clear();
		}
		else
		{
			current.surfaceSuccessor = activeSurfaces;
			current.volumeSuccessor = activeVolumes;
			current.meshSuccessor = activeMeshes;
			current.successor = activeObjects;
		}
		if (current.predessorArray != null || verifyRefractionIndices)
		{				
			if (current.successorArray != null)
			{
				Arrays.sort(current.successorArray);
			}
			getObjects(current.successorArray, ior0,tmpSurfaceArrayList, tmpVolumeArrayList, tmpMeshArrayList);
			if (current.surfacePredessor == activeSurfaces || current.surfacePredessor.length != tmpSurfaceArrayList.size())
			{
				current.surfacePredessor = new GuiOpticalSurfaceObject[tmpSurfaceArrayList.size()];
			}
			current.surfacePredessor = tmpSurfaceArrayList.toArray(current.surfacePredessor);
			if (current.volumePredessor == activeVolumes || current.volumePredessor.length != tmpVolumeArrayList.size())
			{
				current.volumePredessor = new GuiOpticalVolumeObject[tmpVolumeArrayList.size()];
			}
			current.volumePredessor = tmpVolumeArrayList.toArray(current.volumePredessor);
			if (current.meshPredessor == activeMeshes || current.meshPredessor.length != tmpMeshArrayList.size())
			{
				current.meshPredessor = new MeshObject[tmpMeshArrayList.size()];
			}
			current.meshPredessor = tmpMeshArrayList.toArray(current.meshPredessor);
			current.predessor = concatenate(current.surfacePredessor, current.meshPredessor, current.volumePredessor, current.predessor);
			tmpMeshArrayList.clear();
			tmpSurfaceArrayList.clear();
			tmpVolumeArrayList.clear();
		}
		else
		{
			current.surfacePredessor = activeSurfaces;
			current.volumePredessor = activeVolumes;
			current.meshPredessor = activeMeshes;
			current.predessor = activeObjects;
		}
	}
	
	public static final OpticalObject[] concatenate(OpticalObject o0[], OpticalObject o1[], OpticalObject o2[], OpticalObject result[])
	{
		if (o0.length + o1.length + o2.length != result.length)
		{
			result = new OpticalObject[o0.length + o1.length + o2.length];
		}
		System.arraycopy(o0, 0, result, 0, o0.length);
		System.arraycopy(o1, 0, result, o0.length, o1.length);
		System.arraycopy(o2, 0, result, o0.length + o1.length, o2.length);
		return result;
	}
	
	public final void updateScene()
	{
		if (lastSceneUpdate != updateCount)
		{
			lastSceneUpdate = updateCount;
			activeSurfaces = getActiveSurfaces(activeSurfaces);
			activeEmissions = getActiveEmissions(activeEmissions);
			activeLights = getActiveLightSources(activeLights);
			activeVolumes = getActiveVolumes(false, activeVolumes);
			activeTextures = getActiveTextures(activeTextures);
			activeMeshes = getActiveMeshes(activeMeshes);
			activeObjects = concatenate(activeSurfaces, activeMeshes, activeVolumes, activeObjects);
	
			if (cameraStartObjects != null)
			{
				getObjects(cameraStartObjects, Double.NaN, tmpSurfaceArrayList, tmpVolumeArrayList, tmpMeshArrayList);
				if (cameraStartObjects != null)
				{
					Arrays.sort(cameraStartObjects);
				}
				getObjects(cameraStartObjects, Double.NaN,tmpSurfaceArrayList, tmpVolumeArrayList, tmpMeshArrayList);
				if (cameraViewRunnable.gen.surfaceSuccessor == activeSurfaces || cameraViewRunnable.gen.surfaceSuccessor.length != tmpSurfaceArrayList.size())
				{
					cameraViewRunnable.gen.surfaceSuccessor = new GuiOpticalSurfaceObject[tmpSurfaceArrayList.size()];
				}
				cameraViewRunnable.gen.surfaceSuccessor = tmpSurfaceArrayList.toArray(cameraViewRunnable.gen.surfaceSuccessor);
				if (cameraViewRunnable.gen.volumeSuccessor == activeVolumes || cameraViewRunnable.gen.volumeSuccessor.length != tmpVolumeArrayList.size())
				{
					cameraViewRunnable.gen.volumeSuccessor = new GuiOpticalVolumeObject[tmpVolumeArrayList.size()];
				}
				cameraViewRunnable.gen.volumeSuccessor = tmpVolumeArrayList.toArray(cameraViewRunnable.gen.volumeSuccessor);
				if (cameraViewRunnable.gen.meshSuccessor == activeMeshes || cameraViewRunnable.gen.meshSuccessor.length != tmpMeshArrayList.size())
				{
					cameraViewRunnable.gen.meshSuccessor = new MeshObject[tmpMeshArrayList.size()];
				}
				cameraViewRunnable.gen.meshSuccessor = tmpMeshArrayList.toArray(cameraViewRunnable.gen.meshSuccessor);
				tmpSurfaceArrayList.clear();
				tmpVolumeArrayList.clear();
				tmpMeshArrayList.clear();
			}
			else
			{
				cameraViewRunnable.gen.surfaceSuccessor = activeSurfaces;
				cameraViewRunnable.gen.volumeSuccessor = activeVolumes;
				cameraViewRunnable.gen.meshSuccessor = activeMeshes;
			}
			for (int i = cameraViewRunnables.size() - 1; i >= 0; --i)
			{
				CameraViewRunnable r = cameraViewRunnables.get(i).get();
				if (r == null)
				{
					cameraViewRunnables.remove(i);
					continue;
				}
				if (r == cameraViewRunnable)
				{
					continue;
				}
				r.gen.meshSuccessor = cameraViewRunnable.gen.meshSuccessor;
				r.gen.volumeSuccessor = cameraViewRunnable.gen.volumeSuccessor;
				r.gen.surfaceSuccessor = cameraViewRunnable.gen.surfaceSuccessor;
				
			}
			for (int i = 0; i < surfaceObjectList.size(); ++i)
			{
				updateNeighbours(surfaceObjectList.get(i));
			}
			for (int i = 0; i < meshObjectList.size(); ++i)
			{
				updateNeighbours(meshObjectList.get(i));
			}
			for (int i = 0; i < volumeObjectList.size(); ++i)
			{
				updateNeighbours(volumeObjectList.get(i));
				volumeObjectList.get(i).volumeSuccessor = OpticalVolumeObject.EMPTY_VOLUME_ARRAY;
			}
			forceEndpoint = getForceEndpointObject(forceEndpointObject);
			forceStartpoint = getForceEndpointObject(forceStartpointObject);
			environmentTexture = getActiveTexture(environmentTextureString);
			writableEnvironmentTexture = getActiveTexture(writableEnvironmentTextureString);
			renderToTextureObject = getActiveTexture(renderToTextureString);
		}
	}
	
	private GuiTextureObject getActiveTexture(String id) {
		int index = getIndex(id, activeTextures);
		if (index != -1)
		{
			return activeTextures[index];
		}
		return null;
	}

	private GuiTextureObject[] getActiveTextures(GuiTextureObject[] textures) {
		return getActive(textureObjectList, textures);
	}

	private Object getForceEndpointObject(Object o)
	{
		if (o instanceof OpticalObject)
		{
			return o;
		}
		if (o instanceof String)
		{
			String str = (String)o;
			if (str.equals("No"))
			{
				return null;
			}
			if (str.equals("Env"))
			{
				return this;
			}
			return getOpticalObject(str);
		}
		if (o == this)
		{
			return o;
		}
		if (o == null)
		{
			return null;
		}
		throw new IllegalArgumentException();
	}
	
	private String getForceEndpointStr(Object o)
	{
		if (o instanceof OpticalObject)
		{
			return ((OpticalObject)o).id;
		}
		if (o == null)
		{
			return "No";
		}
		if (o == this)
		{
			return "Env";
		}
		throw new IllegalArgumentException();
	}
	
	public String getForceStartpointStr()
	{
		return getForceEndpointStr(forceStartpoint);
	}
	
	public String getForceEndpointStr()
	{
		return getForceEndpointStr(forceEndpoint);
	}
	
	public void setForceEndpoint(Object o)
	{
		forceEndpointObject = o;
		valueChanged(FORCE_ENDPOINT, o);
	}
		
	public void setForceStartpoint(Object o)
	{
		forceStartpointObject = o;
		valueChanged(FORCE_STARTPOINT, o);
	}

	private static int getIndex(String id, ArrayList<? extends OpticalObject> oo)
	{
		if (id != null)
		{
			for (int i = 0; i < oo.size(); ++i)
			{
				if (id.equals(oo.get(i).id))
				{
					return i;
				}
			}
		}
		return -1;		
	}
	
	private static int getIndex(String id, OpticalObject oo[])
	{
		if (id != null)
		{
			for (int i = 0; i < oo.length; ++i)
			{
				if (id.equals(oo[i].id))
				{
					return i;
				}
			}
		}
		return -1;		
	}
	


	public GuiOpticalSurfaceObject getActiveEmissionObject(String id) {
		int index = getIndex(id, activeEmissions);
		return index == -1 ? null : activeEmissions[index];
	}
	
	public GuiOpticalSurfaceObject getActiveSurfaceObject(String id)
	{
		int index = getIndex(id, activeSurfaces);
		return index == -1 ? null : activeSurfaces[index];
	}
		
	public GuiOpticalVolumeObject getActiveVolumeObject(String id)
	{
		int index = getIndex(id, activeVolumes);
		return index == -1 ? null : activeVolumes[index];
	}
	
	
	public MeshObject getActiveMeshObject(String id)
	{
		int index = getIndex(id, activeMeshes);
		return index == -1 ? null : activeMeshes[index];
	}
		
	public GuiOpticalSurfaceObject getSurfaceObject(String id)
	{
		int index = getIndex(id, surfaceObjectList);
		return index == -1 ? null : surfaceObjectList.get(index);
	}
	
	public GuiOpticalVolumeObject getVolumeObject(String id)
	{
		int index = getIndex(id, volumeObjectList);
		return index == -1 ? null : volumeObjectList.get(index);
	}
	
	public GuiTextureObject getTexture(String id)
	{
		int index = getIndex(id, textureObjectList);
		return index == -1 ? null : textureObjectList.get(index);
	}
	
	private final OpticalSurfaceObjectChangeListener osoc = new OpticalSurfaceObjectChangeListener() {
		
		@Override
		public void valueChanged(GuiOpticalSurfaceObject object, SCENE_OBJECT_COLUMN_TYPE ct) {
			++updateCount;
			for (int i = 0; i < surfaceChangeListenerList.size(); ++i)
			{
				surfaceChangeListenerList.get(i).valueChanged(object, ct);
			}
		}
	};
	
	private final OpticalVolumeObjectChangeListener ovoc = new OpticalVolumeObjectChangeListener() {
		
		@Override
		public void valueChanged(GuiOpticalVolumeObject object, SCENE_OBJECT_COLUMN_TYPE ct) {
			++updateCount;
			for (int i = 0; i < volumeObjectChangeListenerList.size(); ++i)
			{
				volumeObjectChangeListenerList.get(i).valueChanged(object, ct);
			}
		}
	};
	
	private final DataChangeListener dcl = new DataChangeListener() {
		
		@Override
		public void dataChanged(OpticalObject source) {
			++updateCount;
			for (int i = 0; i < dataChangeListener.size(); ++i)
			{
				dataChangeListener.get(i).dataChanged(source);
			}
		}
	};
	
	private final TextureObjectChangeListener toc = new TextureObjectChangeListener() {
		
		@Override
		public void valueChanged(GuiTextureObject object, SCENE_OBJECT_COLUMN_TYPE ct) {
			++updateCount;
			for (int i = 0; i < textureObjectChangeListenerList.size(); ++i)
			{
				textureObjectChangeListenerList.get(i).valueChanged(object, ct);
			}
		}
	};
	
	private final MeshObjectChangeListener moc = new MeshObjectChangeListener() {
		
		@Override
		public void valueChanged(MeshObject object, SCENE_OBJECT_COLUMN_TYPE ct) {
			++updateCount;
			for (int i = 0; i < meshObjectChangeListenerList.size(); ++i)
			{
				meshObjectChangeListenerList.get(i).valueChanged(object, ct);
			}
		}
	};
	public String author = "";
	public double epsilon = 0.001;
	
	public void addObjectChangeListener(OpticalSurfaceObjectChangeListener ooc)
	{
		surfaceChangeListenerList.add(ooc);
	}
	
	public void addObjectChangeListener(OpticalVolumeObjectChangeListener ooc)
	{
		volumeObjectChangeListenerList.add(ooc);
	}
	
	public void addObjectChangeListener(TextureObjectChangeListener toc)
	{
		textureObjectChangeListenerList.add(toc);
	}
	
	public void addObjectChangeListener(MeshObjectChangeListener toc)
	{
		meshObjectChangeListenerList.add(toc);
	}
	
	public void removeObjectChangeListener(OpticalSurfaceObjectChangeListener ooc)
	{
		surfaceChangeListenerList.remove(ooc);
	}
	
	public void removeObjectChangeListener(TextureObjectChangeListener ooc)
	{
		textureObjectChangeListenerList.remove(ooc);
	}
	
	public void removeObjectChangeListener(OpticalVolumeObjectChangeListener ooc)
	{
		volumeObjectChangeListenerList.remove(ooc);
	}
	
	public void removeObjectChangeListener(MeshObjectChangeListener ooc)
	{
		meshObjectChangeListenerList.remove(ooc);
	}
	
	public void add(GuiOpticalSurfaceObject goo)
	{
		surfaceObjectList.add(goo);
		valueChanged(OBJECT_ADD, goo);
		goo.addChangeListener(osoc);
	}
	
	public void add(GuiOpticalVolumeObject ovo) {
		volumeObjectList.add(ovo);
		valueChanged(OBJECT_ADD, ovo);
		ovo.addChangeListener(ovoc);
		ovo.addDataChangeListener(dcl);
	}
	
	public void remove(GuiOpticalSurfaceObject goo)
	{
		/*try {
			goo.setValue(SURFACE_COLUMN_TYPE.DELETE, "delete");
		} catch (OperationParseException e) {}*/
		surfaceObjectList.remove(goo);
		goo.removeChangeListener(osoc);
		valueChanged(OBJECT_REMOVE, goo);
	}
	
	public void remove(GuiOpticalVolumeObject gov)
	{
		/*try {
			gov.setValue(VOLUME_COLUMN_TYPE.DELETE, "delete");
		} catch (OperationParseException e) {}*/
		volumeObjectList.remove(gov);
		gov.removeChangeListener(ovoc);
		gov.removeDataChangeListener(dcl);
		valueChanged(OBJECT_REMOVE, gov);
	}
	
	public void remove(GuiTextureObject gov)
	{
		/*try {
			gov.setValue(TEXTURE_COLUMN_TYPE.DELETE, "delete");
		} catch (OperationParseException e) {}*/
		textureObjectList.remove(gov);
		gov.removeChangeListener(toc);
		valueChanged(OBJECT_REMOVE, gov);
	}
	
	public void add(GuiTextureObject guiTextureObject) {
		textureObjectList.add(guiTextureObject);
		guiTextureObject.addChangeListener(toc);
		valueChanged(OBJECT_ADD, guiTextureObject);
	}
	
	public void remove(MeshObject gov)
	{
		try {
			gov.setValue(SCENE_OBJECT_COLUMN_TYPE.DELETE, "delete", vs, parser);
		} catch (OperationParseException e) {}
		meshObjectList.remove(gov);
		gov.removeChangeListener(moc);
		valueChanged(OBJECT_REMOVE, gov);
	}
	
	public void add(MeshObject meshObject) {
		meshObjectList.add(meshObject);
		meshObject.addDataChangeListener(moc);
		valueChanged(OBJECT_ADD, meshObject);
	}
	
	public void clear()
	{
		while (surfaceObjectList.size() != 0)
		{
			int index = surfaceObjectList.size() - 1;
			GuiOpticalSurfaceObject tmp = surfaceObjectList.get(index);
			tmp.removeChangeListener(osoc);
			surfaceObjectList.remove(index);
			valueChanged(OBJECT_REMOVE, tmp);
		}
		while (textureObjectList.size() != 0)
		{
			int index = textureObjectList.size() - 1;
			GuiTextureObject tmp = textureObjectList.get(index);
			tmp.removeChangeListener(toc);
			textureObjectList.remove(index);
			valueChanged(OBJECT_REMOVE, tmp);
		}
		while (volumeObjectList.size() != 0)
		{
			int index = volumeObjectList.size() - 1;
			GuiOpticalVolumeObject tmp = volumeObjectList.get(index);
			tmp.removeChangeListener(ovoc);
			volumeObjectList.remove(index);
			valueChanged(OBJECT_REMOVE, tmp);
		}
		while (meshObjectList.size() != 0)
		{
			int index = meshObjectList.size() - 1;
			MeshObject tmp = meshObjectList.get(index);
			tmp.removeChangeListener(moc);
			meshObjectList.remove(index);
			valueChanged(OBJECT_REMOVE, tmp);
		}

		setForceEndpoint(null);
		setForceStartpoint(null);
		setWritableEnvironmentTexture(null);
		setEnvironmentTexture(null);
		setRenderToTexture(null);
		vs.clear();
	}
	
	public final OpticalObject getOpticalObject(String id)
	{
		OpticalObject obj = getSurfaceObject(id);
		if (obj != null)
		{
			return obj;
		}
		return getVolumeObject(id);	
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends OpticalObject> T[] getActive(ArrayList<T> list, T res[])
	{
		int count = 0;
		for (int i = 0; i < list.size(); ++i)
		{
			if (list.get(i).active)
			{
				++count;
			}
		}
		if (res == null || res.length != count)
		{
			res =(T[]) Array.newInstance(res.getClass().getComponentType(), count);
		}
		count = 0;
		for (int i = 0; i < list.size(); ++i)
		{
			if (list.get(i).active)
			{
				res[count++] = list.get(i);
			}
		}
		return res;
	}

	public int getActiveSurfaceCount() 
	{
		return activeSurfaces.length;
	}
	
	public GuiOpticalSurfaceObject getActiveSurface(int index)
	{
		return activeSurfaces[index];
	}
	
	public int getActiveLightCount()
	{
		return activeLights.length;
	}
	
	public OpticalObject getActiveLight(int index)
	{
		return activeLights[index];
	}
	
	private OpticalObject[] getActiveLightSources(OpticalObject res[])
	{
		int count = 0;
		for (int i = 0; i < surfaceObjectList.size(); ++i)
		{
			if (surfaceObjectList.get(i).active && (surfaceObjectList.get(i).materialType == MaterialType.EMISSION))
			{
				++count;
			}
		}		
		for (int i = 0; i < meshObjectList.size(); ++i)
		{
			if (meshObjectList.get(i).active && (meshObjectList.get(i).materialType == MaterialType.EMISSION))
			{
				++count;
			}
		}
		if (res == null || res.length != count)
		{
			res = new OpticalObject[count];
		}
		count = 0;
		for (int i = 0; i < surfaceObjectList.size(); ++i)
		{
			if (surfaceObjectList.get(i).active && (surfaceObjectList.get(i).materialType == MaterialType.EMISSION))
			{
				res[count++] = surfaceObjectList.get(i);
			}
		}
		for (int i = 0; i < meshObjectList.size(); ++i)
		{
			if (meshObjectList.get(i).active && (meshObjectList.get(i).materialType == MaterialType.EMISSION))
			{
				res[count++] = meshObjectList.get(i);
			}
		}
		return res;
	}
	
    private MeshObject[] getActiveMeshes(MeshObject res[])
	{
		int count = 0;
		for (int i = 0; i < meshObjectList.size(); ++i)
		{
			if (meshObjectList.get(i).active && (meshObjectList.get(i).materialType != MaterialType.EMISSION))
			{
				++count;
			}
		}
		if (res == null || res.length != count)
		{
			res = new MeshObject[count];
		}
		count = 0;
		for (int i = 0; i < meshObjectList.size(); ++i)
		{
			if (meshObjectList.get(i).active && (meshObjectList.get(i).materialType != MaterialType.EMISSION))
			{
				res[count++] = meshObjectList.get(i);
			}
		}
		return res;
	}
	
    private GuiOpticalSurfaceObject[] getActiveSurfaces(GuiOpticalSurfaceObject res[])
	{
		int count = 0;
		for (int i = 0; i < surfaceObjectList.size(); ++i)
		{
			if (surfaceObjectList.get(i).active && (surfaceObjectList.get(i).materialType != MaterialType.EMISSION))
			{
				++count;
			}
		}
		if (res == null || res.length != count)
		{
			res = new GuiOpticalSurfaceObject[count];
		}
		count = 0;
		for (int i = 0; i < surfaceObjectList.size(); ++i)
		{
			if (surfaceObjectList.get(i).active && (surfaceObjectList.get(i).materialType != MaterialType.EMISSION))
			{
				res[count++] = surfaceObjectList.get(i);
			}
		}
		return res;
	}

    private GuiOpticalSurfaceObject[] getActiveEmissions(GuiOpticalSurfaceObject res[])
	{
		int count = 0;
		for (int i = 0; i < surfaceObjectList.size(); ++i)
		{
			if (surfaceObjectList.get(i).active && (surfaceObjectList.get(i).materialType == MaterialType.EMISSION))
			{
				++count;
			}
		}
		if (res == null || res.length != count)
		{
			res = new GuiOpticalSurfaceObject[count];
		}
		count = 0;
		for (int i = 0; i < surfaceObjectList.size(); ++i)
		{
			if (surfaceObjectList.get(i).active && (surfaceObjectList.get(i).materialType == MaterialType.EMISSION))
			{
				res[count++] = surfaceObjectList.get(i);
			}
		}
		return res;
	}

    public GuiOpticalVolumeObject[] getActiveVolumes(boolean lightSource, GuiOpticalVolumeObject res[])
	{
		int count = 0;
		for (int i = 0; i < volumeObjectList.size(); ++i)
		{
			if (volumeObjectList.get(i).active && (lightSource == (volumeObjectList.get(i).materialType == MaterialType.EMISSION)))
			{
				++count;
			}
		}
		if (res == null || res.length != count)
		{
			res = new GuiOpticalVolumeObject[count];
		}
		count = 0;
		for (int i = 0; i < volumeObjectList.size(); ++i)
		{
			if (volumeObjectList.get(i).active && (lightSource == (volumeObjectList.get(i).materialType == MaterialType.EMISSION)))
			{
				res[count++] = volumeObjectList.get(i);
			}
		}
		return res;
	}
   
	public static class RaySimulationObject{
		public final Vector3d position = new Vector3d();
		public final Vector3d direction = new Vector3d();
		private final Intersection nearest = new Intersection();
		public final Vector2d v3 = new Vector2d();
		public final int color[] = new int[4];
		public int numBounces = 0;
		public boolean readColorFront = true;
		public boolean readColorBack = false;
		public boolean readColorGen = true;
		public boolean readColorMiddle = false;
	}
	private static final float multColor = 1f / 0xFF;
	
	/*public static void setPixelColor(WritableRaster img, Vector3d direction, int result[], int tmp[], byte textureDrawMode)
	{
	    double theta = Math.atan2(Math.sqrt(direction.x * direction.x + direction.y * direction.y), direction.z);
	    double phi = Math.atan2(direction.y, direction.x) + Math.PI;
	    int x = (int)(phi * multX * img.getWidth());
	    int y = (int)(theta * multY * img.getHeight());
	    result[3] = 0xFF;
	    switch (textureDrawMode)
	    {
	    	case TextureDrawMode.OBSCURED: img.setPixel(x, y , result); break;
	    	case TextureDrawMode.ALPHA_ADDITIVE: 
	    		img.getPixel(x, y, tmp);
	    		int alpha = tmp[3];
	    		for (int i = 0; i < 3; ++i)
	    		{
	    			tmp[i] = (alpha + 2 * (alpha * tmp[i] + result[i])) / (alpha * 2 + 2);
	    		}
	    		tmp[3] = Math.min(alpha + 1, 0xFF);
	    		
	    		img.setPixel(x,y,tmp);
	    		break;
	    	case TextureDrawMode.ALPHA_INCREMENT:
	    		img.getPixel(x,y,tmp);
	    		tmp[3] = Math.min(tmp[3] + 1, 255);
	    		img.setPixel(x,y,tmp);
	    		break;
	    	default: throw new IllegalArgumentException();
	    }
	}*/
	
	/*public static void setPixelColor(WritableRaster img, Vector2d coord, int result[], int tmp[], byte textureDrawMode)
	{
	    int x = (int)(coord.x * img.getWidth());
	    int y = (int)(coord.y * img.getHeight());
	    result[3] = 0xFF;
	    switch (textureDrawMode)
	    {
	    	case TextureDrawMode.OBSCURED: img.setPixel(x, y , result); break;
	    	case TextureDrawMode.ALPHA_ADDITIVE: 
	    		img.getPixel(x, y, tmp);
	    		int alpha = tmp[3];
	    		for (int i = 0; i < 3; ++i)
	    		{
	    			tmp[i] = (alpha + 2 * (alpha * tmp[i] + result[i])) / (alpha * 2 + 2);
	    		}
	    		tmp[3] = Math.min(alpha + 1, 0xFF);
	    		
	    		img.setPixel(x,y,tmp);
	    		break;
	    	case TextureDrawMode.ALPHA_INCREMENT:
	    		img.getPixel(x,y,tmp);
	    		tmp[3] = Math.min(255, tmp[3] + 1);
	    		img.setPixel(x,y,tmp);
	    		break;
	    	default: throw new IllegalArgumentException("Draw Mode:" + textureDrawMode);
	    }
	}*/
	
	private boolean accept(Object res, Object forceEndpoint)
	{
		return (!(res instanceof OpticalSurfaceObject) || ((OpticalSurfaceObject)res).materialType != MaterialType.DELETION) && (forceEndpoint == res || (forceEndpoint == this && res == null) || forceEndpoint == null);
			
	}

	public static final byte UNACCEPTED_DELETE = 0, UNACCEPTED_RECALCULATE = 1, UNACCEPTED_MARK = 2;
	
	public static final void readColor(SurfaceObject surf, Vector3d position, Vector3d direction, Vector2d coord, int color[])
	{
		if (surf.textureObject != null && surf.textureObject.raster != null)
		{
			if (surf instanceof OpticalSurfaceObject)
			{
				((OpticalSurfaceObject) surf).getTextureCoordinates(position, direction, coord);
			}
			surf.textureObject.getColor(coord.x, coord.y, color);
		}
		else
		{
			color[0] = surf.color.getRed();
			color[1] = surf.color.getGreen();
			color[2] = surf.color.getBlue();
			color[3] = surf.color.getAlpha();
		}
		//throw new RuntimeException();
	}
	
	public static final void readColor(SurfaceObject surf, Vector3d position, Vector3d direction, Vector2d coord, float color[])
	{
		if (surf.textureObject != null && surf.textureObject.raster != null)
		{
			if (surf instanceof OpticalSurfaceObject)
			{
				((OpticalSurfaceObject) surf).getTextureCoordinates(position, direction, coord);
			}
			surf.textureObject.getColor(coord.x, coord.y, color);
		}
		else
		{
			color[0] = surf.color.getRed();
			color[1] = surf.color.getGreen();
			color[2] = surf.color.getBlue();
			color[3] = surf.color.getAlpha();
		}
	}
	
	private final void readColor(RaySimulationObject currentRay, OpticalObject res, int color[])
	{
		Vector2d coord = currentRay.v3;
		if (res instanceof SurfaceObject)
		{
			SurfaceObject surf = ((SurfaceObject)res);
			readColor(surf, currentRay.position, currentRay.direction, coord, color);
		}
		else
		{
			if (environmentTexture != null && environmentTexture.raster != null)
			{
				Vector3d dir = currentRay.direction;
				environment_mapping.mapCartToTex(dir.x, dir.y, dir.z, coord);
				environmentTexture.getColor(coord.x, coord.y, color);
			}
			else
			{
				Arrays.fill(color, 0);
			}
		}
	}
	
	public static final byte STATUS_REJECTED = 0;
	public static final byte STATUS_ACCEPTED = 1;
	public static final byte STATUS_VOLUME = 2;
	public static final byte STATUS_UNDEFINED = 3;

	private static final void writeColor(float[] array, int offset, int color[])
	{
		if (array != null)
		{
			for (int k = 0, wr = offset; k < 4; ++k, ++wr)
			{
				array[wr] = color[k] * multColor;
			}
		}
	}
	
	
	private static final void multColor(float[] sceneEndpointColor, int outIndex, int color[])
	{
		if (sceneEndpointColor != null)
		{
			for (int k = 0, wr = outIndex * 4; k < 4; ++k, ++wr)
			{
				sceneEndpointColor[wr] *= color[k] * multColor;
			}
		}
	}
	
	private static final void mixColor(float[] sceneEndpointColor, int outIndex, int color[])
	{
		float alpha = (1 - sceneEndpointColor[outIndex + 3]) * color[3] * (multColor * multColor);
		
		if (sceneEndpointColor != null)
		{
			for (int k = 0, wr = outIndex * 3; k < 3; ++k, ++wr)
			{
				sceneEndpointColor[wr] += color[k] * alpha;
			}
		}
	}
	
	public final int calculateRays(
			int beginRay,
			int endRay,
			int numRays,
			AbstractRayGenerator gen,
			int genBegnIndex,
			int outBeginIndex,
			float[] startpoints,
			float[] startdirs,
			Object endpoints,
			float[] enddirs,
			float[] sceneEndpointColor,
			double[] trajectory,
			byte[] accepted,
			int[] bounces,
			OpticalObject[] lastObject,
			int maxBounces,
			boolean bidir,
			RaySimulationObject currentRay,
			byte unacceptedBahavior)
	{
		updateScene();
		int color[] = currentRay.color;
		int notAcceptedCount = 0;
		Vector2d coord = currentRay.v3;
		double startpx = Double.NaN, startpy = Double.NaN, startpz = Double.NaN;
		double startdx = Double.NaN, startdy = Double.NaN, startdz = Double.NaN;
		Vector3d position = currentRay.position;
		Vector3d direction = currentRay.direction;
		
		OpticalVolumeObject ovo[] = null;
		int trajectoryStep = 3 * (maxBounces + 2);
		Arrays.fill(bounces, beginRay, endRay, 0);
		Arrays.fill(accepted, beginRay, endRay, STATUS_UNDEFINED);
		OpticalSurfaceObject successorSurfaces[] = gen.getSuccessorSurfaces();
		OpticalVolumeObject successorVolumes[] = gen.getSuccessorVolumes();
		MeshObject successorMeshes[] = gen.getSuccessorMeshes();
		OpticalObject successor[] = null;
		OpticalObject source = gen.getSource();
		Arrays.fill(lastObject, outBeginIndex, outBeginIndex + (endRay - beginRay) * (bidir ? 2 : 1), source);
		while (true)
		{
			for (int j = beginRay; j < endRay; ++j)
			{
				if (accepted[j] == STATUS_REJECTED || accepted[j] == STATUS_ACCEPTED || bounces[j] > maxBounces)
				{
					continue;
				}
				final int outIndex = outBeginIndex + (j - beginRay) * (bidir ? 2 : 1);
				final int trajectoryBeginIndex = outIndex * trajectoryStep;
				if (accepted[j] == STATUS_UNDEFINED)
				{
					gen.generate(j + genBegnIndex - beginRay, numRays, position, direction, coord, color);
					if (startpoints != null)
					{
						position.write(startpoints, outIndex * 3);
					}
					if (startdirs != null)
					{
						direction.write(startdirs, outIndex * 3);
					}
					successorSurfaces = gen.getSuccessorSurfaces();
					successorVolumes = gen.getSuccessorVolumes();
					successorMeshes = gen.getSuccessorMeshes();
					successor = null;
					if (successorSurfaces == null)
					{
						successorSurfaces = this.activeSurfaces;
					}
					if (successorVolumes == null)
					{
						successorVolumes = this.activeVolumes;
					}
					if (successorMeshes == null)
					{
						successorMeshes = this.activeMeshes;
					}
					if (successor == null)
					{
						successor = this.activeObjects;
					}
					startpx = position.x; startpy = position.y; startpz = position.z;
					startdx = position.x; startdy = position.y; startdz = position.z;
					if (currentRay.readColorGen)
					{
						writeColor(sceneEndpointColor, outIndex * 4, color);
					}	
				}
				else
				{
					position.set(endpoints, outIndex * 3);
					direction.set(enddirs, outIndex * 3);
					ArrayUtil.setTo(sceneEndpointColor, outIndex * 4, outIndex * 4 + 4, color, 0, 255);
				}
				currentRay.numBounces = bounces[j];
				if (lastObject[outIndex] != null)
				{
					successorSurfaces = lastObject[outIndex].surfaceSuccessor;
					successorVolumes = lastObject[outIndex].volumeSuccessor;
					successorMeshes = lastObject[outIndex].meshSuccessor;
					successor = lastObject[outIndex].successor;
					currentRay.nearest.object = lastObject[outIndex];
				}
				final int oldNumBounces = currentRay.numBounces;
				OpticalObject res = calculateRay(currentRay, maxBounces, trajectory, trajectoryBeginIndex, successorSurfaces, successorVolumes,  successorMeshes, successor, sceneEndpointColor, outIndex);
				if (lastObject != null)
				{
					lastObject[outIndex] = res;
				}
				bounces[j] = currentRay.numBounces;
				if (res instanceof OpticalVolumeObject && oldNumBounces + 1 != currentRay.numBounces)
				{
					if (ovo == null)
					{
						ovo = new OpticalVolumeObject[endRay - beginRay];
					}
					ovo[j - beginRay] = (OpticalVolumeObject)res;
					position.write(endpoints, outIndex * 3);
					direction.write(enddirs, outIndex * 3);
				}
				else if (accept(res, forceStartpoint))
				{
					if (currentRay.readColorFront)
					{
						readColor(currentRay, res, color);
					}
					position.write(endpoints, outIndex * 3);
					direction.write(enddirs, outIndex * 3);
					writeColor(sceneEndpointColor, outIndex * 4, color);
					if (bidir)
					{
						if (accepted[j] == STATUS_UNDEFINED)
						{
							position.set(startpx, startpy, startpz);
							direction.set(-startdx, -startdy, -startdz);
						}
						else
						{
							position.set(endpoints, outIndex * 3 + 3);
							direction.set(enddirs, outIndex * 3 + 3);
						}
						res = calculateRay(currentRay, maxBounces, trajectory, trajectoryBeginIndex + trajectoryStep, successorSurfaces, successorVolumes, successorMeshes, successor, sceneEndpointColor, outIndex);
						if (accept(res, forceEndpoint))
						{
							accepted[j] = STATUS_ACCEPTED;
							position.write(endpoints, outIndex * 3 + 3);
							direction.write(enddirs, outIndex * 3 + 3);
							writeColor(sceneEndpointColor, outIndex * 4 + 4, color);
						}
						else
						{
							switch(unacceptedBahavior)
							{
								case UNACCEPTED_RECALCULATE:accepted[j] = STATUS_UNDEFINED;--j;break;
								case UNACCEPTED_MARK:
								case UNACCEPTED_DELETE:accepted[j] = STATUS_REJECTED;break;
							}
							++notAcceptedCount;
						}
					}
					else
					{
						accepted[j] = STATUS_ACCEPTED;
					}
				}
				else
				{
					switch(unacceptedBahavior)
					{
						case UNACCEPTED_RECALCULATE:accepted[j] = STATUS_UNDEFINED;--j;break;
						case UNACCEPTED_MARK:
						case UNACCEPTED_DELETE:accepted[j] = STATUS_REJECTED;break;
					}
					++notAcceptedCount;
				}
			}
			if (ovo == null || ArrayUtil.allEqual(ovo, null))
			{
				break;
			}
			int i = 0;
			while (true)
			{
				i = ArrayUtil.firstUnequalIndex(ovo, i, ovo.length, null);
				if (i == ovo.length)
				{
					break;
				}
				OpticalVolumeObject current = ovo[i];
				float path[] = null;
				int endIteration[] = null;
				if (trajectory != null && current.numInnerTrajectoryPoints != 0)
				{
					path = new float[(endRay - beginRay) * current.maxSteps * 3];
					endIteration = new int[endRay - beginRay];
				}

				current.calculateRays(endpoints, enddirs, beginRay * 3, endRay * 3, ovo, 0, path, endIteration);
				for (int j = i; j < ovo.length; ++j)
				{
					if (ovo[j] == current)
					{
						if (path != null)
						{
							int numInnerSteps = current.numInnerTrajectoryPoints;
							for (int l = 0; l < numInnerSteps; ++l)
							{
								for (int k = 0; k < 3; ++k)
								{
									trajectory[outBeginIndex + (j - beginRay) * (bidir ? 2 : 1) * trajectoryStep + bounces[j] * 3 + k] = path[(j * current.maxSteps + l * endIteration[j] / numInnerSteps) * 3  + k];
								}
								++bounces[j];
							}
						}
						accepted[j + beginRay] = STATUS_VOLUME;
						ovo[j] = null;
					}
				}
			}
		}
		return notAcceptedCount;
	}

	public static final void getNextIntersection(
			Vector3d position,
			Vector3d direction,
			Intersection nearest,
			double epsilon,
			OpticalObject surfaceSuccessor[],
			OpticalObject volumeSuccessor[],
			OpticalObject meshSuccessor[])
	{
		for (int l = 0; l < surfaceSuccessor.length; ++l)
		{
			surfaceSuccessor[l].getIntersection(position, direction, nearest, epsilon, nearest.distance);
		}
		for (int l = 0; l < volumeSuccessor.length; ++l)
		{
			volumeSuccessor[l].getIntersection(position, direction, nearest, epsilon, nearest.distance);
		}
		for (int l = 0; l < meshSuccessor.length; ++l)
		{
			meshSuccessor[l].getIntersection(position, direction, nearest, epsilon, nearest.distance);
		}
	}
	
	public static final void getNextIntersection(
			Vector3d position,
			Vector3d direction,
			Intersection nearest,
			double epsilon,
			OpticalObject successor[])
	{
		for (int l = 0; l < successor.length; ++l)
		{
			successor[l].getIntersection(position, direction, nearest, epsilon, nearest.distance);
		}
	}
	
	public final OpticalObject calculateRay(
			RaySimulationObject ray,
			int bounces,
			double trajectory[],
			int trajectoryWriteIndex,
			OpticalSurfaceObject surfaceSuccessor[],
			OpticalVolumeObject volumeSuccessor[],
			MeshObject meshSuccessor[],
			OpticalObject successor[],
			float color[],
			int colorWriteIndex)
	{
		/*if (surfaceSuccessor == null)
		{
			surfaceSuccessor = activeSurfaces;
		}
		if (volumeSuccessor == null)
		{
			volumeSuccessor = activeVolumes;
		}
		if (meshSuccessor == null)
		{
			meshSuccessor = activeMeshes;
		}*/
		if (successor == null)
		{
			successor = activeObjects;
		}
		Intersection nearest = ray.nearest;
		OpticalObject res = nearest.object;
		Vector3d position = ray.position;
		Vector3d direction = ray.direction;
		if (trajectory != null)
		{
			position.write(trajectory, trajectoryWriteIndex + ray.numBounces * 3);
		}
		++ray.numBounces;
		for (; ray.numBounces < bounces +1; ++ ray.numBounces)
		{
			direction.normalize();
			nearest.object = null;
			nearest.distance = Double.POSITIVE_INFINITY;
			for (int l = 0; l < successor.length; ++l)
			{
				successor[l].getIntersection(position, direction, nearest, epsilon, nearest.distance);
			}
			/*for (int l = 0; l < surfaceSuccessor.length; ++l)
			{
				surfaceSuccessor[l].getIntersection(position, direction, nearest, epsilon, nearest.distance);
			}
			for (int l = 0; l < volumeSuccessor.length; ++l)
			{
				volumeSuccessor[l].getIntersection(position, direction, nearest, epsilon, nearest.distance);
			}
			for (int l = 0; l < meshSuccessor.length; ++l)
			{
				meshSuccessor[l].getIntersection(position, direction, nearest, epsilon, nearest.distance);
			}*/
			
			final int b = 3 * ray.numBounces;
			if (Double.isFinite(nearest.distance))
			{								
				//position.set(position, direction,nearest.distance);

				position.set(nearest.position);
				res = nearest.object;
				if (nearest.object instanceof OpticalVolumeObject)
				{
					if (trajectory != null)
					{
						position.write(trajectory, b + trajectoryWriteIndex);
					}
					++ ray.numBounces;
					return nearest.object;	
				}
				SurfaceObject obj = ((SurfaceObject)nearest.object);
				double c = direction.dot(nearest.normal);
				if (obj.materialType == null)
				{
					throw new NullPointerException("Object has no material " + obj.id);
				}
				switch (obj.materialType)
				{
					case ABSORBATION:
						if (ray.readColorMiddle && obj.color.getAlpha() != 255 && obj.alphaCalculation != AlphaCalculation.IGNORE)
						{
							switch(obj.alphaCalculation)
							{
								case MULT:
									readColor(ray, obj, ray.color);
									multColor(color, colorWriteIndex, ray.color);
									break;
								case MIX:
									readColor(ray, obj, ray.color);
									mixColor(color, colorWriteIndex, ray.color);
									break;
								default:
									break;
							}
							break;
						}
					case DELETION:
						if (trajectory != null)
						{
							position.write(trajectory, b + trajectoryWriteIndex);
						}
						++ ray.numBounces;
						return nearest.object;
					case REFRACTION:
						double normaldot = nearest.normal.dot();
						if (Double.isNaN(obj.iorq))
						{
							VariableStack vs = new VariableStack(this.vs);
							Variable x = new Variable("x", nearest.position.x);
							Variable y = new Variable("y", nearest.position.y);
							Variable z = new Variable("z", nearest.position.z);
							vs.addLocal(x);
							vs.addLocal(y);
							vs.addLocal(z);
							Controller control = new Controller();
							double ior0 = obj.ior0.calculate(vs, control).doubleValue();
							double ior1 = obj.ior1.calculate(vs, control).doubleValue();
							double ior = obj.invertNormal == c > 0 ? ior1 / ior0 : ior0 / ior1;
							double iorq = ior * ior - 1;
							double tmp = iorq * normaldot / (c * c) + 1;
							direction.add(nearest.normal,(tmp > 0 ? (Math.sqrt(tmp) - 1) : -2.) * c / normaldot);
						}
						else
						{
							double tmp = (c > 0 ? obj.iorq : obj.inviorq) * normaldot / (c * c) + 1;
							direction.add(nearest.normal,(tmp > 0 ? (Math.sqrt(tmp) - 1) : -2.) * c / normaldot);
						}
						break;
					case REFLECTION:direction.add(nearest.normal, -2 * c/nearest.normal.dot());break;
					case RANDOM:	direction.setAdd(nearest.normal, Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5);break;
					default:
						throw new IllegalArgumentException("Object with illegal material: " + obj.id);
				}
				if (c < 0)
				{
					//surfaceSuccessor = obj.surfaceSuccessor;
					//volumeSuccessor = obj.volumeSuccessor;
					//meshSuccessor = obj.meshSuccessor;
					successor = obj.successor;
				}
				else
				{
					//surfaceSuccessor = obj.surfacePredessor;
					//volumeSuccessor = obj.volumePredessor;	
					//meshSuccessor = obj.meshPredessor;
					successor =obj.predessor;
				}
				if (obj.diffuse != 0)
				{
					double dirnorm = direction.getLength() * obj.diffuse;
					direction.x += (Math.random() - 0.5) * dirnorm;
					direction.y += (Math.random() - 0.5) * dirnorm;
					direction.z += (Math.random() - 0.5) * dirnorm;
				}
				if (trajectory != null){position.write(trajectory, b + trajectoryWriteIndex);}
			}
			else
			{
				position.add(direction, 10000);
				if (trajectory != null){position.write(trajectory, b + trajectoryWriteIndex);}
				return res;
			}
		}
		return res;
	}
	
	public void setRenderToTexture(String text)
	{
		renderToTextureString = text;
		valueChanged(RENDER_TO_TEXTURE, text);
	}

	public void setEnvironmentTexture(String text) {
		environmentTextureString = text;
		valueChanged(ENVIRONMENT_TEXTURE, text);
	}
	
	public void setEnvironmentMapping(TextureMapping mapping)
	{
		environment_mapping = mapping;
		valueChanged(ENVIRONMENT_MAPPING, mapping);
	}
	
	public void setWritableEnvironmentTexture(String text)
	{
		writableEnvironmentTextureString = text;
		valueChanged(WRITABLE_ENVIRONMENT_TEXTURE, text);
	}

	public void setVerifyRefractionIndices(boolean selected) {
		verifyRefractionIndices = selected;
		valueChanged(VERIFY_REFRACTION_INDICES, selected);
	}

	public boolean isVerifyRefractionIndexActivated() {
		return verifyRefractionIndices;
	}

	public OpticalSurfaceObject[] copyActiveSurfaces() {
		return activeSurfaces.clone();
	}

	public OpticalObject[] cloneActiveLights() {
		return activeLights.clone();
	}
	
	public void getActiveLights(List<OpticalObject> list)
	{
		for (int i = 0; i < activeLights.length; ++i)
		{
			list.add(activeLights[i]);
		}
	}

	public OpticalObject[] getActiveSurfaces() {
		return activeSurfaces.clone();
	}

	public OpticalObject[] getActiveVolumes() {
		return activeVolumes.clone();
	}

	public OpticalObject[] getActiveMeshes() {
		return activeMeshes.clone();
	}

	public void setTextureMapping(TextureMapping selectedItem) {
		environment_mapping = selectedItem;
	}

	public final String getId() {
		return id;
	}
}

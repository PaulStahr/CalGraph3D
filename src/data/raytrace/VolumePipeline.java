package data.raytrace;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import data.DataHandler;
import jcomponents.raytrace.Volume;
import maths.Controller;
import maths.Operation;
import maths.OperationCompiler;
import maths.algorithm.OperationCalculate;
import maths.exception.OperationParseException;
import maths.variable.VariableStack;
import maths.variable.VariableStack.VariableObserver.PendendList;
import util.JFrameUtils;
import util.ListTools;
import util.ThreadPool;
import util.TimedUpdateHandler;
import util.data.SortedIntegerArrayList;

public class VolumePipeline implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(VolumePipeline.class);
	private Volume cachedSteps[] = Volume.EMPTY_VOLUME_ARRAY;
	private final ArrayList<WeakReference<Runnable> > updateListener = new ArrayList<>();
	public final ArrayList<CalculationStep> steps = new ArrayList<>();
	private volatile boolean calculating;
	public OpticalVolumeObject ovo;
	public final RaytraceScene scene;
	public boolean calcuteAtCreation;
	private boolean autoUpdate;
	public final VolumePipelineTimedUpdater updater;
	private final VolumeRunnable runnable = new VolumeRunnable();
	private volatile int begin = 0;
	public SortedIntegerArrayList vIds[] = SortedIntegerArrayList.EMPTY_SORTED_INTEGER_ARRAY_LIST_ARRAY;

	private class VolumePipelineTimedUpdater implements TimedUpdateHandler{
		private final VariableStack.VariableObserver observer;
		private final PendendList allChangedVariables;
		private int modCount = scene.vs.modCount();

		public VolumePipelineTimedUpdater(RaytraceScene scene)
		{
			observer = scene.vs.createVaribleObserver();
			allChangedVariables = observer.getPendentVariableList();
		}

		@Override
		public synchronized void update() {
			scene.rayUpdateHandler.update();
			if (scene.vs.modCount() != modCount){
				modCount = scene.vs.modCount();
				observer.updateChanges();
				for (int i = 0; i < vIds.length; ++i)
				{
					if (allChangedVariables.hasMatch(vIds[i]))
					{
						synchronized(this)
						{
							begin = Math.min(i, begin);
							if (!calculating)
							{
								calculating = true;
								DataHandler.runnableRunner.run(runnable, false);
							}
						}
						break;
					}
				}
			}
		}

		@Override
		public int getUpdateInterval() {
			return 10;
		}
	}

	public VolumePipeline(RaytraceScene scene)
	{
		this.scene = scene;
		updater = new VolumePipelineTimedUpdater(scene);
	}

	private final class VolumeRunnable extends ThreadPool.RunnableObject {
		public VolumeRunnable() {
			super("VolumePipeline", null);
		}
        @Override
		public void run(){
    		try{
    			pipe();
            }catch (Exception e){
            	logger.error("Exception at calculating Volume", e);
            }
        }
    }

	public static class CalculationStep{}

	public static class GenerationCalculationStep extends CalculationStep{
		public String size;
		public GenerationCalculationStep(String size)
		{
			this.size = size;
		}
	}

	public static class CalculationCalcuationStep extends CalculationStep{
		public String ior;
		public String translucency;
		public String givenValues;
		public String isGiven;

		public CalculationCalcuationStep(String ior, String translucency, String givenValues,String isGiven)
		{
			this.ior = ior;
			this.translucency = translucency;
			this.givenValues = givenValues;
			this.isGiven = isGiven;
		}
	}

	public void addListener(Runnable runnable)
	{
		ListTools.clean(updateListener);
		updateListener.add(new WeakReference<>(runnable));
	}

	public void removeListener(Runnable runnable){ListTools.removeAll(updateListener, runnable);}

	public boolean isCalculating(){return calculating;}

	public int getCurrentCalculatingStep(){return begin - 1;}

	public void updateState(){ListTools.run(updateListener);}

	public void updateVariableIds()
    {
    	if (vIds.length != steps.size())
    	{
    		vIds = Arrays.copyOf(vIds, steps.size());
    	}
		for (int i = 0; i < vIds.length; ++i)
		{
			if (vIds[i] == null)
			{
				vIds[i] = new SortedIntegerArrayList();
			}
			else
			{
				vIds[i].clear();
			}
		}
    	for (int i = 0; i < steps.size(); ++i)
		{
			CalculationStep ps = steps.get(i);
			try
			{
				if (ps instanceof CalculationCalcuationStep)
				{
					CalculationCalcuationStep cps = (CalculationCalcuationStep)ps;
					OperationCalculate.getVariables(OperationCompiler.compile(cps.givenValues, (Operation)null), vIds[i]);
					OperationCalculate.getVariables(OperationCompiler.compile(cps.ior,         (Operation)null), vIds[i]);
					OperationCalculate.getVariables(OperationCompiler.compile(cps.isGiven,     (Operation)null), vIds[i]);
					OperationCalculate.getVariables(OperationCompiler.compile(cps.translucency,(Operation)null), vIds[i]);
				}
				else if (ps instanceof GenerationCalculationStep)
				{
					GenerationCalculationStep gps = (GenerationCalculationStep)ps;
					OperationCalculate.getVariables(OperationCompiler.compile(gps.size), vIds[i]);
				}
			}catch(OperationParseException e)
			{

			}
		}
    }
	public synchronized void pipe()//TODO is this necessary?
	{
		updateState();
		if (cachedSteps.length != steps.size())
		{
			cachedSteps = Arrays.copyOf(cachedSteps, steps.size());
		}
		for (int i = 0; i < cachedSteps.length; ++i)
		{
			if (cachedSteps[i] == null)
			{
				cachedSteps[i] = new Volume(0,0,0);
			}
		}
		try
		{
			while (true)
			{
				int current;
				synchronized(this)
				{
					current = begin++;
					if (current == steps.size())
					{
						ovo.setVolume(cachedSteps[cachedSteps.length - 1]);
						ovo.triggerModificationEvents();
						calculating = false;
						notifyAll();
						break;
					}
				}
				updateState();
				CalculationStep ps = steps.get(current);
				if (ps instanceof CalculationCalcuationStep)
				{
					if (current != 0)
					{
						cachedSteps[current] = cachedSteps[current].readOrClone(cachedSteps[current - 1]);
					}
					else
					{
						cachedSteps[0].readOrClone(ovo.getVolume());
					}
					CalculationCalcuationStep cps = (CalculationCalcuationStep)ps;
					ovo.editValues(
					        scene.copyActiveSurfaces(),
					        OperationCompiler.compile(cps.ior, (Operation)null),
					        OperationCompiler.compile(cps.translucency, (Operation)null),
					        OperationCompiler.compile(cps.givenValues, (Operation)null),
					        OperationCompiler.compile(cps.isGiven, (Operation)null),
					        scene.vs, cachedSteps[current]);
				}
				else if (ps instanceof GenerationCalculationStep)
				{
					GenerationCalculationStep gps = (GenerationCalculationStep)ps;
					Controller control = new Controller();
					int values[] = OperationCalculate.toIntArray(OperationCompiler.compile(gps.size).calculate(scene.vs, control));
					cachedSteps[current] = new Volume(values[0], values[1], values[2]);
					ovo.setSize(values[0], values[1], values[2]);
				}
			}
		}
		catch(final Throwable ex)
		{
			JFrameUtils.logErrorAndShow("Can't pipe calculations", ex, logger);
			synchronized(this) {
				calculating = false;
				notifyAll();
			}
		}

		updateState();
	}

	@Override
	public void run() {
		synchronized(this)
		{
			begin = 0;
			if (!calculating)
			{
				calculating = true;
				DataHandler.runnableRunner.run(runnable, false);
			}
		}
	}

	public void setAutoUpdate(boolean selected) {
		if (this.autoUpdate != selected)
		{
			if (selected)
			{
				DataHandler.timedUpdater.add(updater);
				updateVariableIds();
			}
			else
			{
				DataHandler.timedUpdater.remove(updater);
			}
			this.autoUpdate = selected;
		}
	}

	public boolean getAutoUpdate() {
		return autoUpdate;
	}

	public void blockOnCalculation() {
		updater.update();
		synchronized(this)
		{
			if (calculating)
			{
				logger.debug("block");
				try {
					wait();
				} catch (InterruptedException e) {
					logger.error("Unexpected Interrupt", e);
				}
				logger.debug("unblock");
			}
			else
			{
				logger.debug("skipblock");
			}
			if (calculating)
			{
				throw new RuntimeException("Thread shoud only be woken at calculation-finish");
			}
		}
	}
}

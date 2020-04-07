package data.raytrace;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jcomponents.raytrace.Volume;
import maths.Controller;
import maths.Operation;
import maths.OperationCompiler;
import maths.algorithm.OperationCalculate;
import maths.exception.OperationParseException;
import util.JFrameUtils;
import util.ListTools;
import util.data.SortedIntegerArrayList;

public class VolumePipeline {
	private static final Logger logger = LoggerFactory.getLogger(VolumePipeline.class);
	private Volume cachedSteps[] = Volume.EMPTY_VOLUME_ARRAY;
	private final ArrayList<WeakReference<Runnable> > updateListener = new ArrayList<>();
	public final ArrayList<CalculationStep> steps = new ArrayList<>();
	boolean calculating;
	int currentCalculatingStep;
	public OpticalVolumeObject ovo;
	public RaytraceScene scene;
	public boolean calcuteAtCreation;
	public boolean autoUpdate;
	public SortedIntegerArrayList vIds[] = SortedIntegerArrayList.EMPTY_SORTED_INTEGER_ARRAY_LIST_ARRAY;

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
		updateListener.add(new WeakReference<Runnable>(runnable));
	}
	
	public void removeListener(Runnable runnable)
	{
		ListTools.removeAll(updateListener, runnable);
	}
	
	public boolean isCalculating()
	{
		return calculating;
	}
	
	public int getCurrentCalculatingStep()
	{
		return currentCalculatingStep;
	}
	
	public void updateState()
	{
		for (int i = 0; i < updateListener.size(); ++i)
		{
			Runnable current = updateListener.get(i).get();
			if (current != null)
			{
				current.run();
			}
		}
	}
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
					OperationCalculate.getVariables(OperationCompiler.compile(cps.ior, (Operation)null), vIds[i]);
					OperationCalculate.getVariables(OperationCompiler.compile(cps.isGiven, (Operation)null), vIds[i]);
					OperationCalculate.getVariables(OperationCompiler.compile(cps.translucency, (Operation)null), vIds[i]);
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
	public void pipe(int begin)
	{
		calculating = true;
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
			for (currentCalculatingStep = begin; currentCalculatingStep < steps.size(); ++currentCalculatingStep)
			{
				updateState();
				CalculationStep ps = steps.get(currentCalculatingStep);
				if (ps instanceof CalculationCalcuationStep)
				{
					if (currentCalculatingStep != 0)
					{
						cachedSteps[currentCalculatingStep] = cachedSteps[currentCalculatingStep].readOrClone(cachedSteps[currentCalculatingStep - 1]);
					}
					else
					{
						cachedSteps[0].readOrClone(ovo.getVolume());
					}
					CalculationCalcuationStep cps = (CalculationCalcuationStep)ps;
					ovo.editValues(scene.copyActiveSurfaces(), OperationCompiler.compile(cps.ior, (Operation)null), OperationCompiler.compile(cps.translucency, (Operation)null), OperationCompiler.compile(cps.givenValues, (Operation)null), OperationCompiler.compile(cps.isGiven, (Operation)null), scene.vs, cachedSteps[currentCalculatingStep]);
				}
				else if (ps instanceof GenerationCalculationStep)
				{
					GenerationCalculationStep gps = (GenerationCalculationStep)ps;
					Controller control = new Controller();
					int values[] = OperationCalculate.toIntArray(OperationCompiler.compile(gps.size).calculate(scene.vs, control));
					cachedSteps[currentCalculatingStep] = new Volume(values[0], values[1], values[2]);
				}
			}
			ovo.setVolume(cachedSteps[cachedSteps.length - 1]);
			ovo.triggerModificationEvents();			
		}
		catch(final Exception ex)
		{
			JFrameUtils.logErrorAndShow("Can't pipe calculations", ex, logger);
		}
		calculating = false;
		updateState();
	}
}

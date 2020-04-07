/*******************************************************************************
 * Copyright (c) 2019 Paul Stahr
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package maths;

import maths.Operation.CalculationController;
import maths.algorithm.OperationCalculate;
import maths.algorithm.OperationCalculate.OperationList;
import util.data.Stack;

public class Controller implements CalculationController{
	/**
	 * Stoppt die berechnung sobald wie moeglich.
	 * Wird nicht automatisch auf false gesetzt
	 */
	private boolean stop = false;
	public boolean saveMod = false;
	private boolean calculateRandom = false;
	private boolean calculateLoop = false;
	private boolean connectEmptyVariables = false;
	private final Stack<OperationCalculate.OperationList> cacheOperationList = new Stack<OperationCalculate.OperationList>();
	public VariableAmount va;
	
	public final void setStopFlag(boolean value){
		stop = value;
	}

	public final boolean getStopFlag(){
		return stop;
	}
	
	public final boolean calculateRandom(){
		return calculateRandom;
	}
	
	public final boolean calculateLoop(){
		return calculateLoop;
	}
	
	public final void calculateRandom(boolean value){
		calculateRandom = value;
	}
	
	public final void calculateLoop(boolean value){
		calculateLoop = value;
	}
	
	public final void connectEmptyVariables(boolean value)
	{
		connectEmptyVariables = value;
	}
	
	public void returnToChached(OperationList ol){
		ol.clear();
		cacheOperationList.push(ol);	
	}
	
	public OperationList getOperationList(){
		OperationCalculate.OperationList ol = cacheOperationList.pop();
		return ol == null ? new OperationList() : ol;
	}

	@Override
	public boolean connectEmptyVariables() {
		return connectEmptyVariables;
	}
	
	@Override
	public VariableAmount getVariables()
	{
		return va;
	}
}

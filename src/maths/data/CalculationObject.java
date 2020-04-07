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
package maths.data;

public class CalculationObject {
	public static final int MODE_REDUCE = 0;
	public static final int MODE_CALCULATE_SAVE = 1;
	public static final int MODE_CALCULATE_REEL = 2;
	public static final int MODE_CALCULATE_UNSAVE = 3;
	private boolean stopped;
	public final int mode;
	
	public CalculationObject(int mode){
		switch (mode){
			case MODE_REDUCE:
			case MODE_CALCULATE_REEL:
			case MODE_CALCULATE_SAVE:
			case MODE_CALCULATE_UNSAVE:
			break;
			default:
				throw new IllegalArgumentException();
		}
		this.mode = mode;
	}
	
	public final boolean isStopped(){
		return stopped;
	}
	
	public final void setStopped(){
		stopped = true;
	}
}

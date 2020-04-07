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
package geometry;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public class Vector3i implements Vectori{
	public int x, y, z;
	
	public Vector3i(final int x, final int y, final int z){
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3i() {}
	
	public final int getI(int index)
	{
		switch (index)
		{
			case 0: return x;
			case 1: return y;
			case 2: return z;
			default: throw new ArrayIndexOutOfBoundsException(index);
		}
	}
	
	public final void set(int x, int y, int z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public String toString()
	{
		return new StringBuilder().append('(').append('x').append(',').append('y').append(',').append('z').append(')').toString();
	}

	@Override
	public void setElem(int index, int value) {
		switch (index)
		{
		case 0: this.x = value;return;
		case 1: this.y = value;return;
		case 2: this.z = value;return;
		}
		throw new ArrayIndexOutOfBoundsException(index);
	}
	
    public final int size()
    {
    	return 3;
    }
}

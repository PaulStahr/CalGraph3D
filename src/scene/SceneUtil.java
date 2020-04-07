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
package scene;

import scene.object.SceneObject;
import scene.object.SceneObjectLine;
import scene.object.SceneObject.DrawType;

public class SceneUtil {
	public static final SceneObject createCoordinateCross()
	{
		SceneObjectLine coordinateCross = new SceneObjectLine();
        coordinateCross.setSize(12 * 201 + 3 * 42);
        coordinateCross.setDrawType(DrawType.LINE_STRICLES);
        int index = 0;
        for (int i=-100;i<=100;i++)
        {
       		coordinateCross.setVertex(index++,  i,    -0.1f,    0f);
       		coordinateCross.setVertex(index++,  i,     0.1f,    0f);
       		coordinateCross.setVertex(index++,  i,       0f, -0.1f);
       		coordinateCross.setVertex(index++,  i,       0f,  0.1f);
       		coordinateCross.setVertex(index++, -0.1f,     i,    0f);
       		coordinateCross.setVertex(index++,  0.1f,     i,    0f);
       		coordinateCross.setVertex(index++,    0f,     i, -0.1f);
       		coordinateCross.setVertex(index++,    0f,     i,  0.1f);
       		coordinateCross.setVertex(index++, -0.1f,    0f,     i);
       		coordinateCross.setVertex(index++,  0.1f,    0f,     i);
       		coordinateCross.setVertex(index++,    0f, -0.1f,     i);
       		coordinateCross.setVertex(index++,    0f,  0.1f,     i);
        }
        for (int j=0;j<3;j++)
        {
        	int i = -10000;
        	coordinateCross.setVertex(index++, j==0 ? i : 0, j==1 ? i : 0, j==2 ? i : 0);
        	i+=1000;
            for (;i<10000;i+=1000)
            {
            	coordinateCross.setVertex(index++, j==0 ? i : 0, j==1 ? i : 0, j==2 ? i : 0);       	
        		coordinateCross.setVertex(index++, j==0 ? i : 0, j==1 ? i : 0, j==2 ? i : 0);
            }
        	coordinateCross.setVertex(index++, j==0 ? i : 0, j==1 ? i : 0, j==2 ? i : 0);       	
        }
        return coordinateCross;
	}
}

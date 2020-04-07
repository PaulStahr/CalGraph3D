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
package opengl;

import java.awt.Color;
import java.util.AbstractList;
import data.Options;
import opengl.Light.Type;

public class LightGroup extends AbstractList<Light>{
	private final Light lights[];
	private boolean useLights = false;
	public LightGroup(int count){
		lights = Light.create(count);
	}
	
	public final Light get(int index){
		return lights[index];
	}
	
	public int size()
	{
		return lights.length;
	}
	
	public void useLights(boolean use){
		useLights = use;
	}
	
	public void loadFromOptions(){
		StringBuilder stringBuilder = new StringBuilder(2);
		stringBuilder.append('l');
		Options.OptionTreeInnerNode node = Options.getInnerNode("light");
		
        for (int i=0;i<8;i++){
        	final Light l = lights[i];
        	stringBuilder.setLength(1);
        	stringBuilder.append(i);
        	Options.OptionTreeInnerNode child = Options.getInnerNode(node, stringBuilder.toString());
            l.set(Options.getColor(child, "ambient", Color.BLACK), Type.AMBIENT);
            l.set(Options.getColor(child, "diffuse", Color.BLACK), Type.DIFFUSE);
            l.set(Options.getColor(child, "specular", Color.BLACK), Type.SPECULAR);
            float x = Options.getFloat(child, "xpos", 0f);
            float y = Options.getFloat(child, "ypos", 0f);
            float z = Options.getFloat(child, "zpos", 0f);
            l.enabled = useLights && Options.getBoolean(child, "activated", false);
            l.fallOff = 1.0f;
            l.set(x, y, z,1f,Type.POSITION);
        }
	}
}

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

public final class Position
{
    public final Vector3f vertex;
    public final Rotation3 rotation;
    
    /**
     * Erzeugt eine neue Position
     */
    public Position (){
        vertex = new Vector3f();
        rotation = new Rotation3();
    }

    /**
     * Erzeugt eine neue Position durch \u00FCbernehmen der Werte einer anderen. Es werden nur Werte, keine Zeiger \u00FCbernommen.
     * @param position die Position anhand derer die Werte gesetzt werden sollen
     */
    public Position (final Position position){
        vertex = new Vector3f(position.vertex);
        rotation = new Rotation3(position.rotation);
    }

    public Position(float x, float y, float z, float xr, float yr, float zr, boolean rad) {
		vertex = new Vector3f(x, y, z);
		rotation = new Rotation3(xr, yr, zr, rad);
	}
    
    public final void set(Position position){
    	vertex.set(position.vertex);
    	rotation.set(position.rotation);
    }

    public final void set(Vector3f position, Rotation3 rotation){
    	this.vertex.set(position);
    	this.rotation.set(rotation);
    }
}

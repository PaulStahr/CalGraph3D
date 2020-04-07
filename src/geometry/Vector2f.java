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


public final class Vector2f implements Vectorf
{
    public float x, y;

    /**
     * erzeugt einen neuen Vektor
     */
    public Vector2f(){}

    /**
     * erzeugt einen neuen Vektor
     * @param x L\u00E4nge des Vektors in x-Richtung
     * @param y L\u00E4nge des Vektors in y-Richtung
     * @param z L\u00E4nge des Vektors in z-Tichtung
     */
    public Vector2f(final float x,final float y){
        this.x=x;
        this.y=y;
    }

    /**
     * erzeugt einen neuen Vektor
     * @param vektor der Vektor dessen eigenschaften \u00FCbernommen werden
     */
    public Vector2f(final Vector2f vector){
       set(vector);
    }
    
    @Override
    public void setElem(int index, float value)
    {
    	switch (index)
    	{
	    	case 0: this.x = value; return;
	    	case 1: this.y = value; return;
	    	default: throw new ArrayIndexOutOfBoundsException(index);
    	}
    }


        
    /**
     * Subtrahiert den Vektor
     * @param vektor der Subtrahiert
     */
    public void sub(final Vector2f vektor){
        x -= vektor.x;
        y -= vektor.y;
    }

    /**
     * Setzt die L\u00E4nge des Vektors auf 1
     */
    public final void normalize(){
        final float len = 1/getLength();
        x *= len;
        y *= len;
    }

    /**
     * Multipliziert die L\u00E4nge des Vektors
     * @param mult Multiplikator
     */
    public final void multiply(final float mult){
        x*=mult;
        y*=mult;
    }

    /**
     * Setzt den Vektor auf eine Bestimmte L\u00E4nge
     * @param length die neue L\u00E4nge des Vektors
     */
    public final void setLength(double length){
        length /= getLength();
        x *= length;
        y *= length;
    }

    /**
     * Berechnet die aktuelle L\u00E4nge des Vektors
     * @return L\u00E4nge des Vektors
     */
    public final float getLength(){
        return (float)Math.sqrt(x * x + y * y);
    }
    
    /**
     * Rotiert den Vektor.
     * @param z die Rotation um die z-Achse
     */
    public final void rotateRadiansZ(double z){
        double sin = Math.sin(z);
        double cos = Math.cos(z);
        float tmp = (float)(cos*x+sin*y);
        y = (float)(cos*y-sin*x);
        x = tmp;
    }

    /**
     * Setzt den Vektor auf die bestimmte Werte.
     * @param x x-Wert des Vektors
     * @param y y-Wert des Vektors
     * @param z z-Wert des Vektors
     */
    public final void set(final float x, final float y){
        this.x=x;
        this.y=y;
    }

    /**
     * Setzt den Vektor auf bestimmte Werte.
     * @param vektor der Vektor auf den die Werte gesetzt werden sollen
     */
    public final void set(final Vector2f vektor){
        x = vektor.x;
        y = vektor.y;
    }

    /**
     * Addiert zum Vektor bestimmte Werte.
     * @param vektor der Vektor der addiert werden soll
     */
    public final void add(final Vector2f vektor){
        x += vektor.x;
        y += vektor.y;
    }

    /**
     * Addiert zum Vektor bestimmte Werte.
     * @param x x-Wert der Addiert wird
     * @param y y-Wert der Addiert wird
     * @param z z-Wert der Addiert wird
     */
    public final void add(final float x, final float y){
        this.x += x;
        this.y += y;
    }
    
    @Override
    public final int size()
    {
    	return 2;
    }
    
    @Override
    public double getD(int index)
    {
    	switch (index)
    	{
	    	case 0: return x;
	    	case 1: return y;
	    	default: throw new ArrayIndexOutOfBoundsException(index);
    	}
    }
    
    @Override
    public void setElem(int index, double value)
    {
    	switch (index)
    	{
	    	case 0: this.x = (float)value; return;
	    	case 1: this.y = (float)value; return;
	    	default: throw new ArrayIndexOutOfBoundsException(index);
    	}
    }
}

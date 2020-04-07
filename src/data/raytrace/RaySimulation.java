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
package data.raytrace;

public class RaySimulation {
	public static enum SurfaceType{
		FLAT("Flat"), SPHERICAL("Spherical"), PARABOLIC("Parabolic"), HYPERBOLIC("Hyperbolic"), CUSTOM("Custom"), CYLINDER("Cylinder");
		
		public final String name;
		private SurfaceType(String name)
		{
			this.name = name;
		}
		
		public String toString()
		{
			return name;
		}
		
		public static final SurfaceType getByName(String name)
		{
			for (SurfaceType current : st)
			{
				if (name.equals(current.name))
				{
					return current;
				}
			}
			return valueOf(name);
		}
		private static final SurfaceType st[] = SurfaceType.values();
		public static String[] names (){
			 String res[] = new String[st.length];
			 for (int i = 0; i < res.length; ++i)
			 {
				 res[i] = st[i].name;
			 }
			 return res;
		 }
	}
	
	
	public static enum MaterialType{
		ABSORBATION("Absorbation"), DELETION("Deletion"), REFRACTION("Refraction"), REFLECTION("Reflection"), EMISSION("Emission"), RANDOM("Random");
		
		public final String name;
		private MaterialType(String name)
		{
			this.name = name;
		}
		
		public String toString()
		{
			return name;
		}
		
		public static final MaterialType getByName(String name)
		{
			for (MaterialType current : mt)
			{
				if (name.equals(current.name))
				{
					return current;
				}
			}
			return valueOf(name);
		}
		
		public static final MaterialType get(Object o)
		{
			if (o instanceof String)
			{
				MaterialType tmp = MaterialType.getByName((String)o);
				if (tmp != null)
				{
					return tmp;
				}
			}
			else if (o instanceof MaterialType)
			{
				return (MaterialType)o;
			}
			throw new IllegalArgumentException("Class:" + o.getClass());
		}
		
		 private static final MaterialType mt[] = MaterialType.values();
		
		 public static String[] names (){
			 String res[] = new String[mt.length];
			 for (int i = 0; i < res.length; ++i)
			 {
				 res[i] = mt[i].name;
			 }
			 return res;
		 }
	}
	
	public static enum AlphaCalculation
	{
		MIX("Mix"), MULT("Mult"), IGNORE("Ignore");
		
		public final String name;
		private AlphaCalculation(String name)
		{
			this.name = name;
		}
		
		public static final AlphaCalculation ac[] = AlphaCalculation.values();
		
		public static String[] names (){
			 String res[] = new String[ac.length];
			 for (int i = 0; i < res.length; ++i)
			 {
				 res[i] = ac[i].name;
			 }
			 return res;
		 }
		
		public static final AlphaCalculation getByName(String name)
		{
			for (AlphaCalculation current : ac)
			{
				if (name.equals(current.name))
				{
					return current;
				}
			}
			return valueOf(name);
		}

		public String toString()
		{
			return name;
		}
	}
}

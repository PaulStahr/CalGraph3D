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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import util.EmptyList;
import util.data.UniqueObjects;

public class StringId {
	public static final StringIdObject EMPTY_ARRAY[] = new StringId.StringIdObject[0];
    public static final List<StringIdObject> EMPTY_LIST = new EmptyList<>();

	private static StringIdObject dataNameSorted[] = new StringIdObject[1];
	private static StringIdObject dataIdSorted[] = new StringIdObject[1];
	private static int length=0;
	
	public void clean()
	{
		
	}//TOOO implement
	
	public static final int[] getIds(StringIdObject data[])
	{
		if (data.length == 0)
		{
			return UniqueObjects.EMPTY_INT_ARRAY;
		}
		int res[] = new int[data.length];
		for (int i = 0; i < res.length; ++i)
		{
			res[i] = data[i].id;
		}
		return res;
	}
	
	public static final Comparator<StringIdObject> idComparator = new Comparator<StringIdObject>() {
		@Override
		public int compare(StringIdObject o1, StringIdObject o2) {
			return o1.id-o2.id;
		}
	};
	
	public static final Comparator<StringIdObject> literalComparator = new Comparator<StringId.StringIdObject>() {

		@Override
		public int compare(StringIdObject o1, StringIdObject o2) {
			return o1.nameSortedIndex - o2.nameSortedIndex;
		}
	};
	
	/**
	 * Needs O(1)
	 * @param stringId
	 * @return
	 */
	public static final StringIdObject getStringAndId(int stringId){
		return stringId >= 0 && stringId < length ? dataIdSorted[stringId] : null;
	}
	
	public static final StringIdObject[] getStringAndId(String str[]){
		if (str.length == 0)
		{
			return EMPTY_ARRAY;
		}
		StringIdObject[] erg = new StringIdObject[str.length];
		for (int i=0;i<erg.length;i++)
			erg[i] = getStringAndId(str[i]);
		return erg;
	}
	
	/**
	 * Adding new String needs O(n)
	 * Getting existing String needs O(log n)
	 * @param str
	 * @return 
	 */
	public static final StringIdObject getStringAndId(String str){
		int index = getIndex(str);
		if (index >= 0)
			return dataNameSorted[index];
		synchronized(StringId.class){
			index =  getIndex(str);
			if (index >= 0)
				return dataNameSorted[index];
			if (length >= dataNameSorted.length){
				dataNameSorted = Arrays.copyOf(dataNameSorted, dataNameSorted.length * 2);
				dataIdSorted = Arrays.copyOf(dataIdSorted, dataNameSorted.length * 2);
			}
			System.arraycopy(dataNameSorted, -index-1, dataNameSorted, -index, 1+length+index);
			StringIdObject obj =  dataIdSorted[length] = dataNameSorted[-index-1] = new StringIdObject(str, length);
			++length;
			for (int i=-index-1;i<length;++i){
				dataNameSorted[i].nameSortedIndex = i;
			}
			return obj;
		}
	}
	
	/**
	 * Adding new String needs O(n)
	 * Getting existing String needs O(log n)
	 * @param str
	 * @return 
	 */	public static final StringIdObject getStringAndId(String str, int begin, int end){
		int index = getIndex(str, begin, end);
		if (index >= 0)
			return dataNameSorted[index];
		synchronized(StringId.class){
			index =  getIndex(str, begin, end);
			if (index >= 0)
				return dataNameSorted[index];
			if (length >= dataNameSorted.length){
				dataNameSorted = Arrays.copyOf(dataNameSorted, dataNameSorted.length * 2);
				dataIdSorted = Arrays.copyOf(dataIdSorted, dataNameSorted.length * 2);
			}
			System.arraycopy(dataNameSorted, -index-1, dataNameSorted, -index, 1+length+index);
			StringIdObject obj =  dataIdSorted[length] = dataNameSorted[-index-1] = new StringIdObject(str.substring(begin, end), length);
			++length;
			for (int i=-index-1;i<length;++i){
				dataNameSorted[i].nameSortedIndex = i;
			}
			return obj;
		}
	}

	public static final int getIdIfExist(String name) {
		int index = getIndex(name);
		System.out.println(name + ' ' + (index >= 0 ? dataNameSorted[index].string : ""));
		return index >= 0 ? dataNameSorted[index].id : -1;
	}
	
	public static final StringIdObject getStringAndIdIfExist(String name) {
		int index = getIndex(name);
		return index >= 0 ? dataNameSorted[index] : null;
	}

	public static final StringIdObject getStringAndIdIfExist(String name, int begin, int end) {
		int index = getIndex(name, begin, end);
		return index >= 0 ? dataNameSorted[index] : null;
	}

	/**
	 * needs O(log n)
	 * @param name
	 * @return
	 */
    private static final int getIndex (String name){
		int low = 0, high = length-1;
		while (low<=high){
			final int middle=(low+high)/2;
			final int c =name.compareTo(dataNameSorted[middle].string);
			if (c>0)
				low = middle+1;
			else if (c==0)
				return middle;
			else
				high = middle-1;
		}
		return -1-low;
    }
	
    public static final int compareTo(String str0, int begin, int end, String str1) {
        int len1 = end - begin;
        int len2 = str1.length();
        int lim = Math.min(len1, len2);
        int k = 0;
        while (k < lim) {
            char c1 = str0.charAt(begin + k);
            char c2 = str1.charAt(k);
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }
    
    private static final int getIndex (String name, int begin, int end){
		int low = 0, high = length-1;
		while (low<=high){
			final int middle=(low+high)/2;
			final int c = compareTo(name, begin, end, dataNameSorted[middle].string);
			if (c>0)
				low = middle+1;
			else if (c==0)
				return middle;
			else
				high = middle-1;
		}
		return -1-low;
    }
	
	public static final class StringIdObject implements Comparable<StringId.StringIdObject>{
		public final String string;
		public final int id;
		private int nameSortedIndex;
		
		private StringIdObject(String str, int id){
			this.string = str;
			this.id = id;
		}
		
		public final int getStringCompareNumber(){
			return nameSortedIndex;
		}
		
		public final int compareStringTo(StringIdObject o)
		{
			return nameSortedIndex - o.nameSortedIndex;
		}

		@Override
		public final int compareTo(StringIdObject o) {
			return id - o.id;
		}
		
		@Override
		public final String toString(){
			return string;
		}
	}

}

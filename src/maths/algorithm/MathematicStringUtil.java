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
package maths.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import util.data.IntegerArrayList;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class MathematicStringUtil
{
	   public static final List<String> split(final String str, int begin, int end, char c, List<String> list){
	        if (begin == end)
	            return list;
	        int deth = 0, index = begin;
	        boolean isStr = false;
	        
	        for (int i=begin;i<end;i++){
	            final char charAt = str.charAt(i);
	            if (charAt == '\\')
	            	i++;
	            else if (charAt == '"')
	            	isStr = !isStr;
	            else if (!isStr){
	            	switch (charAt){
	            	case '(':
	            	case '{':
	            	case '[': deth++;break;
	            	case ')':
	            	case '}':
	            	case ']': deth--;break;
	            	default : 
	            		if (deth == 0 && charAt == c){
	        				list.add(str.substring(index, i));
	        				index = i + 1;
	        			}
	            	}
	            }
	        }
	        list.add(str.substring(index, end));
	        return list;
	    }

	   public static final IntegerArrayList split(final CharSequence str, int begin, int end, char c, IntegerArrayList list){
	        if (begin == end)
	            return list;
	        int deth = 0;
	        boolean isStr = false;
	        
	        for (int i=begin;i<end;i++){
	            final char charAt = str.charAt(i);
	            if (charAt == '\\')
	            	i++;
	            else if (charAt == '"')
	            	isStr = !isStr;
	            else if (!isStr){
	            	switch (charAt){
	            	case '(':
	            	case '{':
	            	case '[': deth++;break;
	            	case ')':
	            	case '}':
	            	case ']': deth--;break;
	            	default : 
	            		if (deth == 0 && charAt == c){
	        				list.add(i);
	        			}
	            	}
	            }
	        }
	        return list;
	    }

    public static final String[] split(final String str, int begin, int end, char c){
    	ArrayList<String> ll = new ArrayList<String>();
    	split(str, begin, end, c, ll);
    	return ll.toArray(new String[ll.size()]);
    }    
    
    public static int indexOf (CharSequence str, int begin, int end, char c){
        int deth = 0;
        boolean isStr = false;
        for (int i=begin;i<end;i++){
            final char charAt = str.charAt(i);
            if (charAt == '\\')
            	i++;
            else if (charAt == '"')
            	isStr = !isStr;
            else if (!isStr){
                switch (charAt){
                	case '(':
                	case '{':
                	case '[': deth++;break;
                	case ')':
                	case '}':
                	case ']': deth--;break;
                	default: if (deth == 0 && charAt == c) return i;
                }
            }
        }
        return -1;
    }

    public static int lastIndexOf (CharSequence str, int begin, int end, char c){
    	int deth = 0;
        boolean isStr = false;
        for (int i=end-1;i>=begin;i--){
            final char charAt = str.charAt(i);
            if (charAt == '\\')
            	i++;
            else if (charAt == '"')
            	isStr = !isStr;
            else if (!isStr){
                switch (charAt){
	            	case '(':
	            	case '{':
	            	case '[': deth++;break;
	            	case ')':
	            	case '}':
	            	case ']': deth--;break;
                }
                if (deth == 0 && charAt == c) return i;
            }
        }
        return -1;
    }

    public static int indexOf (CharSequence str, int begin, int end, char c[]){
        int deth = 0;
        boolean isStr = false;
         for (int i=begin;i<end;i++){
            final char charAt = str.charAt(i);
            if (charAt == '\\')
            	i++;
            else if (charAt == '"')
            	isStr = !isStr;
            else if (!isStr){
                switch(charAt){
		        	case '(':
		        	case '{':
		        	case '[': deth++;break;
		        	case ')':
		        	case '}':
		        	case ']': deth--;break;
                	default: if(deth == 0)
	                    for (int j=0;j<c.length;j++)
	                        if (charAt == c[j])
	                            return i;
                }
            }
        }
        return -1;	
    }   
    
    public static boolean isInteger(CharSequence s, int begin, int end){
    	if (s.length()==0)
    		return false;
    	final char firstChar = s.charAt(0);
    	if (firstChar != '-' && (firstChar <'0' || firstChar >'9'))
    		return false;
    	for (int i=1;i<s.length();i++){
    		final char c = s.charAt(i);
    		if (c<'0'||c>'9')
    			return false;
    	}    	
    	return true;
    }
    
    public static boolean isDouble(CharSequence s, int begin, int end){
    	if (begin == end)
    		return false;
    	final char firstChar = s.charAt(begin);
    	if (firstChar != '-' && (firstChar <'0' || firstChar >'9'))
    		return false;
    	for (int i=begin + 1;i<end;i++){
    		final char c = s.charAt(i);
    		if ((c<'0'||c>'9')&&c!='.'&&c!='E' && c != 'e'&&c!='-' && c != '+')
    			return false;
    	}
    	return true;
    }
    
    public static int lastIndexOf (CharSequence str, char c[]){
        int deth = 0;
        boolean isStr = false;
        char charAt=0;
        for (int i=str.length() - 1;i>-2;i--){
        	final char charOld = charAt;
            if (i!=-1 && (charAt = str.charAt(i)) == '\\')
            	i--;
            else if (charOld == '"')
            	isStr = !isStr;
            else if (!isStr){
            	switch (charOld){
		        	case '(':
		        	case '{':
		        	case '[': deth++;break;
		        	case ')':
		        	case '}':
		        	case ']': deth--;break;
    				default :
    					if (deth == 0)			
    						for (char ch :c)
    							if (charOld == ch)
    								return i+1;
            	}
            }
        }
        return -1;	
    }

    public static int lastIndexOf (CharSequence str, int begin, int end, char c[][]){
        int deth = 0;
        boolean isStr = false;
        char charAt=0;
        int indexFound = -1;
        int bestFound = c.length;
        for (int i=end - 1;i>begin-2;i--){
        	final char charOld = charAt;
            if (i!=-1 && (charAt = str.charAt(i)) == '\\')
            	i--;
            else if (charOld == '"')
            	isStr = !isStr;
            else if (!isStr){
            	switch (charOld){
		        	case '(':
		        	case '{':
		        	case '[': deth++;break;
		        	case ')':
		        	case '}':
		        	case ']': deth--;break;
    				default :{
    					if (deth == 0){
    						for (int j=0;j<bestFound;j++){
    							for (char ch :c[j]){
    								if (charOld == ch){
    									indexFound = i+1;
    									bestFound = j;
    									if (j==0)
    										return indexFound;
    								}
    							}
    						}    						
    					}
    				}
            	}
            }
        }
        return indexFound;	
    }

    public static int lastIndexOf (CharSequence str, int begin, int end, char c[], byte prirority[]){
        int deth = 0;
        boolean isStr = false;
        char charAt=0;
        int indexFound = -1;
        int bestFound = c.length;
        for (int i=end - 1;i>begin-2;i--){
        	final char charOld = charAt;
            if (i!=-1 && (charAt = str.charAt(i)) == '\\')
            	i--;
            else if (charOld == '"')
            	isStr = !isStr;
            else if (!isStr){
            	switch (charOld){
		        	case '(':
		        	case '{':
		        	case '[': deth++;break;
		        	case ')':
		        	case '}':
		        	case ']': deth--;break;
    				default :{
    					if (deth == 0){
    						final int index = Arrays.binarySearch(c, charOld);
							if (index >= 0 && prirority[index]<bestFound){
								indexFound = i+1;
								bestFound = prirority[index];
								if (bestFound==0)
									return indexFound;
    						}    						
    					}
    				}
            	}
            }
        }
        return indexFound;	
    }

    public static boolean onlyBrackets (CharSequence str, int begin, int end, char bracketOpen, char bracketClose){
    	final int lengthMinOne = end-1;
        if (end - begin < 2 || str.charAt(begin)!=bracketOpen || str.charAt(lengthMinOne) != bracketClose)
            return false;
        int deep = 0;
        boolean isStr = false;
        for (int i=begin + 1;i<lengthMinOne;i++){
            final char charAt = str.charAt(i);
            if (charAt == '\\')
            	i++;
            else if (charAt == '"')
            	isStr = !isStr;
            else if (!isStr){
	            if (charAt == bracketOpen)
	                deep ++;
	            else if (charAt == bracketClose){
	                deep --;
	                if (deep < 0)
	                    return false;
	            }
            }
        }
        return deep == 0;    
    }

    public static boolean justString(final CharSequence str, int begin, int end){
    	--end;
    	if (end - begin < 1 || str.charAt(begin)!= '"' || str.charAt(end)!='"')
    		return false;
    	for (int i=begin + 1;i<end;i++){
    		final char c = str.charAt(i);
    		if ((c == '\\' && ++i>=end)||c == '"')
    			return false;	
    	}
    	return true;
    }

    public static final boolean isFunction(CharSequence str, int begin, int end){
    	final int lengthMinOne = end-1;
        if (!(Character.isLetter(str.charAt(begin))|| str.charAt(begin)=='_')||str.charAt(lengthMinOne)!=')')
            return false;
        int i;
        for (i=begin + 1;Character.isLetterOrDigit(str.charAt(i)) || str.charAt(i) == '_';i++)
            if (i==lengthMinOne)
                return false;
        return str.charAt(i)=='(';
    }
}

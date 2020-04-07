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
package util;


/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public class SaveLineCreator {
	private final StringBuilder strBuilder = new StringBuilder();
	
	public SaveLineCreator(){}
		
	public final String getSaveLine(final String variable, final float value){
		strBuilder.setLength(0);
		return appendSaveLine(variable, value, strBuilder).toString();
	}
	
    public static final StringBuilder appendSaveLine(final String variable, final float value, final StringBuilder stringBuilder){
    	return stringBuilder.append(variable).append('=').append('"').append(value).append('"').append('\n');
    }
    
	public final String getSaveLine(final String variable, final int value){
		strBuilder.setLength(0);
		return appendSaveLine(variable, value, strBuilder).toString();
	}
	
    public static final StringBuilder appendSaveLine(final String variable, final int value, final StringBuilder stringBuilder){
    	return stringBuilder.append(variable).append('=').append('"').append(value).append('"').append('\n');
    }
    
	public final String getSaveLine(final String variable, final boolean value){
		strBuilder.setLength(0);
		return appendSaveLine(variable, value, strBuilder).toString();
	}
	
    public static final StringBuilder appendSaveLine(final String variable, final boolean value, final StringBuilder stringBuilder){
    	return stringBuilder.append(variable).append('=').append('"').append(value).append('"').append('\n');
    }
    
    public final String getSaveLine(final CharSequence variable, final CharSequence value){
		strBuilder.setLength(0);
        return appendSaveLine(variable, value, strBuilder).toString();
    }
    
    public static final StringBuilder appendSaveLine(final CharSequence variable, final CharSequence value, final StringBuilder stringBuilder){
        stringBuilder.append(variable).append('=').append('"');
        final int length = value.length();
        for (int i = 0;i<length;i++){
            final char c = value.charAt(i);
            switch (c){
        		case '"':	stringBuilder.append('\\').append('"');break;
        		case '\n':	stringBuilder.append('\\').append('n');break;
        		case '\\':	stringBuilder.append('\\').append('\\');break;
        		default:stringBuilder.append(c);break;
            }
        }
        return stringBuilder.append('"').append('\n');
    }
    public final SaveObject getSaveObject(final String saveLine){
    	return getSaveObject(saveLine, 0, saveLine.length());
    }
    
    public final SaveObject getSaveObject(final String saveLine, int from, int to){
    	final int indexEquals = saveLine.indexOf('=', from);
    	if (indexEquals == -1 || to < indexEquals + 1 || saveLine.charAt(indexEquals + 1) != '"')
    		return null;
		strBuilder.setLength(0);
    	for (int i = indexEquals + 2; i < to; i++){
    		final char c = saveLine.charAt(i);
    		if (c == '\\'){
    			if (++i > to)
    				return null;
    			final char appendChar;
    			switch(saveLine.charAt(i)){
    				case '"':	appendChar = '"';break;
    				case 'n':	appendChar = '\n';break;
    				case '\\':	appendChar = '\\';break;
    				case 't':	appendChar = '\t';break;
    				case 'u':{
    					if (to < i+4)
    						return null;
    					appendChar = getChar(saveLine, i);
    					i += 4;
    					break;
    				}
    				default : return null;
    			}
    			strBuilder.append(appendChar);
    		}else{
    			if (c == '"')
    				return new SaveObject(saveLine.substring(from, indexEquals), strBuilder.toString());
    			strBuilder.append(c);
    		}
    	}
       	return null;
    }
    
    public final String comment(final CharSequence str){
        final int length = str.length();
		strBuilder.setLength(0);
        for (int i = 0;i<length;i++){
            final char c = str.charAt(i);
            switch (c){
        		case '"':	strBuilder.append('\\').append('"');break;
        		case '\n':	strBuilder.append('\\').append('n');break;                                                       
        		case '\\':	strBuilder.append('\\').append('\\');break;
        		default:strBuilder.append(c);break;
            }
        }
        return strBuilder.toString();
    	
    }
    
    public final String uncomment(final String str){
		strBuilder.setLength(0);
    	for (int i = 0; i < str.length(); i++){
    		final char c = str.charAt(i);
    		if (c == '\\'){
    			if (++i > str.length())
    				return null;
    			final char appendChar;
    			switch(str.charAt(i)){
    				case '"':	appendChar = '"' ;break;
    				case 'n':	appendChar = '\n';break;
    				case '\\':	appendChar = '\\';break;
    				case 't':	appendChar = '\t';break;
    				case 'u':{
    					if (str.length() < i+4)
    						return null;
    					appendChar = getChar(str, i);
    					i += 4;
    					break;
    				}
    				default : return null;
    			}
    			strBuilder.append(appendChar);
    		}else{
    			strBuilder.append(c);
    		}
    	}
    	return strBuilder.toString();
    	
    }
        
    private static final char getChar (final CharSequence str, int begin){
    	int erg = 0;
    	for (int i=begin;i<begin + 4;i++){
    		final char c = str.charAt(i);
    		final int nextChar;
    		if (c >= '0' && c <= '9')
    			nextChar = c-'0';
    		else if (c >= 'A' && c <= 'F')
    			nextChar = c-('A'-10);
    		else if (c >= 'a' && c <= 'f')
    			nextChar = c-('a'-10);
    		else
    	        return 0;    
    		erg = (erg << 4) | nextChar;
    	}
    	return (char)erg;
    }

    public static final class SaveObject{
    	public final String variable, value;
    	public SaveObject (final String variable, final String value){
    		this.variable = variable;
    		this.value = value;
    	}
    }
}

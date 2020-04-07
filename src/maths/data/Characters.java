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


/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public abstract class Characters {
	public static final char MULT		='*';
	public static final char MULT_MAT	='\u2022';
	public static final char MULT_CROSS	='\u2716';
	public static final char MULT_SKAL	='\u2055';
	public static final char DIV		='/';
	public static final char ADD		='+';
	public static final char SUB		='-';
	public static final char MOD		='%';
	public static final char LOW		='<';
	public static final char HIGH		='>';
	public static final char END_COMMAND = ';';
	public static final char LOW_EQ		='\u2264';
	public static final char HIGH_EQ	='\u2265';
	public static final char EQ			='=';
	public static final char NOT_EQ		='\u2260';
	public static final char POW		='^';
	public static final char AND		='\u02C4';
	public static final char OR			='\u02C5';
	public static final char NOT		='\u00AC';
	public static final char PI			='\u03C0';
	public static final char EULER		='\u212F';
	public static final char ELEM_OF	='\u2208';
	public static final char NOT_ELEM_OF='\u2209';
	public static final char CONCAT		='\u25E6';
	public static final char SET		='\u2192';
	public static final char N			='\u2115';
	public static final char Z			='\u2124';
	public static final char Q			='\u211A';
	public static final char R			='\u211D';
	public static final char C			='\u2102';
	public static final char P			='\u2119';
	public static final char I			='\u2148';
	public static final char INFTY		='\u221E';
	public static final char SUBSET		='\u2286';
	public static final char NOT_SUBSET	='\u2288';
	public static final char SQRT		='\u221A';
	public static final char HIGH_ZERO	='\u2070';
	public static final char HIGH_ONE	='\u00B9';
	public static final char HIGH_TWO	='\u00B2';
	public static final char HIGH_THREE	='\u00B3';
	public static final char HIGH_FOUR	='\u2074';
	public static final char HIGH_FIVE	='\u2075';
	public static final char HIGH_SIX	='\u2076';
	public static final char HIGH_SEVEN	='\u2077';
	public static final char HIGH_EIGHT	='\u2078';
	public static final char HIGH_NINE	='\u2079';
	public static final char HIGH_T		='\u1D57';
	public static final char HIGH_SUB	='\u207B';
	public static final char HIGH_PLUS  ='\u207A';
	public static final char FIBUNACCI	='\u2131';
	
	public static final String STR_INFTY	= String.valueOf(INFTY);
	public static final String STR_NEG_INFTY= String.valueOf(new char[]{SUB,INFTY});
	public static final String STR_EULER	= String.valueOf(EULER);
	public static final String STR_PI		= String.valueOf(PI);
	
	private static final char HIGH_ARRAY[] = {HIGH_ZERO, HIGH_ONE, HIGH_TWO, HIGH_THREE, HIGH_FOUR, HIGH_FIVE, HIGH_SIX, HIGH_SEVEN, HIGH_EIGHT, HIGH_NINE};
	
	public static final int digit(char c, int radix)
	{
		for (int i = 0; i < radix; ++i)
		{
			if(HIGH_ARRAY[i] == c)
			{
				return i;
			}
		}
		return -1;
	}
	
	public static final String toHighString(final String number){
		final char erg[] = new char[number.length()];
		for (int i=0;i<erg.length;i++){
			final char charAt = number.charAt(i);
			if (charAt <= '9' && charAt >='0')
				erg[i] = HIGH_ARRAY[number.charAt(i)-'0'];
			else if (charAt == SUB)
				erg[i] = HIGH_SUB;
			else
				erg[i] = charAt;
		}
		return new String (erg);
	}
	
	public static final char[] toLowString(final String number, int begin, int end){
		final char erg[] = new char[end - begin];
		for (int i=0;i<erg.length;i++){
			final char charAt = number.charAt(i + begin);
			for (int j=0;j<HIGH_ARRAY.length;j++){
				if (HIGH_ARRAY[j]==charAt){
					erg[i] = (char)('0'+j);
					break;
				}
			}
			if (charAt == HIGH_SUB)
				erg[i] = SUB;
		}
		return erg;
	}
	
	public static final boolean isHighNumber(final char c){
		for (int i=0;i<HIGH_ARRAY.length;i++)
			if (HIGH_ARRAY[i]==c)
				return true;
		return false;
	}
}

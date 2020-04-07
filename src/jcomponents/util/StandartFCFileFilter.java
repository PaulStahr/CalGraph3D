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
package jcomponents.util;

import java.io.File;
import java.util.Arrays;

import javax.swing.filechooser.FileFilter;

public class StandartFCFileFilter extends FileFilter implements java.io.FileFilter{
	final Object accepted;
	final boolean acceptDir;
	final String description;
	final String fullDescription;
	
	public StandartFCFileFilter(String description, String accepted, boolean acceptDir){
		if ((this.description = description)==null)
			throw new NullPointerException();
		this.accepted = accepted;
		this.acceptDir = acceptDir;
		fullDescription = new StringBuilder().append(description).append('(').append('*').append('.').append(accepted).append(')').toString();
	}

	public StandartFCFileFilter(String description, String accepted[], boolean acceptDir){
		if ((this.description = description)==null)
			throw new NullPointerException();
		for (int i=0;i<accepted.length;i++)
			if (accepted[i] == null)
				throw new NullPointerException();
		String tmp[] = accepted.clone();
		if (tmp.length == 1)
		{
			this.accepted = tmp[0];
		}
		else
		{
			Arrays.sort(tmp);
			this.accepted = tmp;
		}
		this.acceptDir = acceptDir;
		if (tmp.length != 0){
			 StringBuilder strBuilder = new StringBuilder().append(description).append('(').append('*').append('.').append(tmp[0]);
			for (int i=1;i<tmp.length;i++)
				strBuilder.append(',').append('*').append('.').append(tmp[i]);
			strBuilder.append(')');
			fullDescription = strBuilder.toString();
		}
		else
		{
			fullDescription = "";
		}
	}
		
	
	@Override
	public boolean accept(File f) {
		if (f.isDirectory() && acceptDir)
			return true;
		String name = f.getName();
		final int index = name.lastIndexOf('.');
		if (index < 0)
		{
			return false;
		}
		if (accepted instanceof String[])
		{
			return Arrays.binarySearch((String[])accepted, name.substring(index+1).toLowerCase())>-1;
		}
		else if (accepted instanceof String)
		{
			return name.substring(index + 1).equalsIgnoreCase((String)accepted);
		}
		else
		{
			throw new RuntimeException();
		}
	}

	@Override
	public String getDescription() {
		return fullDescription;
	}
}

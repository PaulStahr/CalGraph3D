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

import java.util.zip.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/** 
* @author  Paul Stahr
* @version 04.02.2012
*/
public final class BufferedZipReader
{
    private final ZipInputStream inStream;
    private final InputStreamReader reader;
    private final BufferedReader inBuf;
    private ZipEntry entry;
    private Charset format;

    public BufferedZipReader (ZipInputStream inStream, String charset){
        format = Charset.forName(charset);
        reader = new InputStreamReader(this.inStream = inStream, format);
        inBuf = new BufferedReader(reader, 2048);
    }

    public final ZipEntry getNextEntry() throws IOException{
        entry = inStream.getNextEntry();
        if (entry == null)
            return null;        
        return entry;
    }

    public final String readLine () throws IOException{
    	final String line = inBuf.readLine();
    	return line == null ? null : prepareLine(line);
    }
   
    private static final String prepareLine(String line){
        if (line.length() == 0 || (line.length() == 1 && (line.charAt(0) == 65279 || line.charAt(0) == '\r')))
        	return StringUtils.EMPTY;
        final boolean removeStart = line.charAt(0) == 65279, removeEnd = line.charAt(line.length()-1)=='\r';
    	return removeStart || removeEnd ? line.substring(removeStart ? 1 : 0, removeEnd ? line.length() -1 : line.length()) : line;
    }
    
    public final void close() throws IOException {
    	inBuf.close();
    	reader.close();
    }
}

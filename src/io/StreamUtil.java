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
package io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

public class StreamUtil {
	/*public static final String readStreamToString(InputStream stream) throws IOException
	{
		StringBuilder strB = new StringBuilder(stream.available());
		InputStreamReader reader = new InputStreamReader(stream);
		char ch[] = new char[4096];
		for (int len; (len = reader.read(ch))!=-1; strB.append(ch, 0, len));
		reader.close();
		return strB.toString();
	}*/
	

    public static long copy(InputStream from, OutputStream to)
              throws IOException {
        byte[] buf = new byte[0x1000];
        long total = 0;
        while (true) {
          int r = from.read(buf);
          if (r == -1) {
            break;
          }
          to.write(buf, 0, r);
          total += r;
        }
        return total;
    }

	public static final String readStreamToString(InputStream stream) throws IOException
	{
		InputStreamReader reader = new InputStreamReader(stream);
		char ch[] = new char[4096];
		int len = 0;
		int read = 0;
		while ((read = reader.read(ch, len, ch.length - len))!=-1)
		{
			len += read;
			if (ch.length - len == 0)
			{
				ch = Arrays.copyOf(ch, ch.length * 2);
			}
		}
		reader.close();
		return new String(ch, 0, len);
	}
}

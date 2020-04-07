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
package jcomponents;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import util.JFrameUtils;
import util.ListTools;
import util.StringUtils;

public class LicenceWindow extends JFrame {
	private static final long serialVersionUID = 2420749614268249560L;
	private final String content;
	private static final ArrayList<WeakReference<LicenceWindow> > windows = new ArrayList<WeakReference<LicenceWindow> >();
	
	private LicenceWindow(String title, String content) {
		this.content = content;
		setTitle(title);
		final JLabel label = new JLabel(StringUtils.toHtml(content));
		final JScrollPane scrollPane = new JScrollPane(label);
		setLayout(JFrameUtils.SINGLE_COLUMN_LAYOUT);
		add(scrollPane);
		pack();
	}

	public static final synchronized LicenceWindow getInstance(String title, String content)
	{
		ListTools.clean(windows);
		for (int i = 0; i < windows.size(); ++i)
		{
			LicenceWindow lw = windows.get(i).get();
			if (lw != null && lw.getTitle().equals(title) && lw.content.equals(content))
			{
				return lw;
			}
		}
		LicenceWindow lw = new LicenceWindow(title, content);
		windows.add(new WeakReference<LicenceWindow>(lw));
		return lw;
	}
}

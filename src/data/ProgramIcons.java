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
package data;

import java.net.URL;

import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgramIcons {
	private static final Logger logger = LoggerFactory.getLogger(ProgramIcons.class);
	public static final String iconDirInterface = "interface/";
	public static final String iconDirInterfacePanels = iconDirInterface.concat("panels/");
	public static final String iconDirGraph = "graph/";
	private static final StringBuilder strB = new StringBuilder(DataHandler.getResourceFolder().concat("icons/"));
	private static final int len = strB.length();
	
	public static final ImageIcon webIcon			= loadImage(iconDirInterface,"web");
	public static final ImageIcon settingsIcon		= loadImage(iconDirInterface,"settings");
	public static final ImageIcon addIcon			= loadImage(iconDirInterface,"add");
	public static final ImageIcon subIcon			= loadImage(iconDirInterface,"sub");
	public static final ImageIcon logoIcon			= loadImage("","logo");
	public static final ImageIcon deletedIcon		= loadImage("","deleted");
	public static final ImageIcon newFileIcon		= loadImage(iconDirInterface,"new_file");
	public static final ImageIcon openFileIcon 		= loadImage(iconDirInterface,"open_file");
	public static final ImageIcon saveFileIcon 		= loadImage(iconDirInterface,"save_file");
	public static final ImageIcon saveFileAtIcon 	= loadImage(iconDirInterface,"save_file_at");
	public static final ImageIcon historyIcon		= loadImage(iconDirInterface,"history");
	public static final ImageIcon imgPlayForward	= loadImage(iconDirInterfacePanels,"play_forward");
	public static final ImageIcon imgStop			= loadImage(iconDirInterfacePanels,"stop");
	public static final ImageIcon imgPlayBackward	= loadImage(iconDirInterfacePanels,"play_backward");
	public static final ImageIcon iconCalculating	= loadImage(iconDirGraph,"calculating");
	public static final ImageIcon iconNotCalculating= loadImage(iconDirGraph,"not_calculating");
	public static final ImageIcon iconVisible      	= loadImage(iconDirGraph,"visible");
	public static final ImageIcon iconInvisible    	= loadImage(iconDirGraph,"invisible");
	public static final ImageIcon iconDots          = loadImage(iconDirGraph,"dots");
	public static final ImageIcon iconLines         = loadImage(iconDirGraph,"lines");
	public static final ImageIcon iconSolid         = loadImage(iconDirGraph,"solid");
	public static final ImageIcon iconSmooth        = loadImage(iconDirGraph,"smooth");
	public static final ImageIcon iconDown          = loadImage(iconDirGraph,"arrow_down");
	public static final ImageIcon iconUp            = loadImage(iconDirGraph,"arrow_up");
	public static final ImageIcon iconDelete        = loadImage(iconDirGraph,"delete");
	public static final ImageIcon iconHelp			= loadImage(iconDirInterface,"help_icon");
	public static final ImageIcon arrowRight       	= loadImage(iconDirInterfacePanels,"arrow_right");
	public static final ImageIcon arrowRightDown   	= loadImage(iconDirInterfacePanels,"arrow_right_down");
	public static final ImageIcon arrowDown        	= loadImage(iconDirInterfacePanels,"arrow_down");
	public static final ImageIcon triangleUp		= loadImage(iconDirInterfacePanels,"triangle_up");
	public static final ImageIcon triangleDown		= loadImage(iconDirInterfacePanels,"triangle_down");

	private static final ImageIcon loadImage(String location, String name){
		strB.append(location).append(name).append('.').append("png");
		final URL url = ProgramIcons.class.getResource(strB.toString());
		strB.setLength(len);
		if (url == null){
			logger.error("can't read image: \"" + location +'"');
			return null;
		}
		return new ImageIcon(url);
	}
}

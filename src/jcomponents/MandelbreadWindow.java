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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;
import org.lwjgl.opencl.Util;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.CLKernel;
import java.nio.FloatBuffer;
import java.util.List;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;

import util.Buffers;
import static org.lwjgl.opencl.CL10.*;

public class MandelbreadWindow extends JFrame{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2041446859067166716L;
	static final String source =
			"kernel void sum(global const float *real_input, global const float *imag_input, global float *real_answer, global float *imag_answer) { "
			+ "  unsigned int xid = get_global_id(0); "
			+ "  real_answer[xid] = real_input[xid] + imag_input[xid];}";
    // Data buffers to store the input and result data in
    final FloatBuffer realInput = Buffers.createFloatBuffer(new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
    final FloatBuffer imagInput = Buffers.createFloatBuffer(new float[]{9, 8, 7, 6, 5, 4, 3, 2, 1, 0});
    final FloatBuffer realOutput = BufferUtils.createFloatBuffer(realInput.capacity());
    final FloatBuffer imagOutput = BufferUtils.createFloatBuffer(realInput.capacity());
    final ImageComponent imComp = new ImageComponent();
    final BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
    
    public MandelbreadWindow(){
    	try{
	   		// Initialize OpenCL and create a context and command queue
	   		CL.create();
	   		CLPlatform platform = CLPlatform.getPlatforms().get(0);
	   		List<CLDevice> devices = platform.getDevices(CL_DEVICE_TYPE_GPU);
	   		CLContext context = CLContext.create(platform, devices, null, null, null);
	   		CLCommandQueue queue = clCreateCommandQueue(context, devices.get(0), CL_QUEUE_PROFILING_ENABLE, null);
	
	   		// Allocate memory for our two input buffers and our result buffer
	    	CLMem reakiMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, realInput, null);
			clEnqueueWriteBuffer(queue, reakiMem, 1, 0, realInput, null, null);
			CLMem imagiMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, imagInput, null);
			clEnqueueWriteBuffer(queue, imagiMem, 1, 0, imagInput, null, null);
			CLMem realaMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR, realOutput, null);
			clFinish(queue);
			CLMem imagaMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR, realOutput, null);
			clFinish(queue);
	
			// Create our program and kernel
			CLProgram program = clCreateProgramWithSource(context, source, null);
			Util.checkCLError(clBuildProgram(program, devices.get(0), "", null));
			// sum has to match a kernel method name in the OpenCL source
			CLKernel kernel = clCreateKernel(program, "sum", null);
				// Execution our kernel
			PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(1);
			kernel1DGlobalWorkSize.put(0, realInput.capacity());
			kernel.setArg(0, reakiMem);
			kernel.setArg(1, imagiMem);
			kernel.setArg(2, realaMem);
			kernel.setArg(3, imagaMem);
			clEnqueueNDRangeKernel(queue, kernel, 1, null, kernel1DGlobalWorkSize, null, null, null);
			// Read the results memory back into our result buffer
			clEnqueueReadBuffer(queue, realaMem, 1, 0, realOutput, null, null);
			clEnqueueReadBuffer(queue, imagaMem, 1, 0, imagOutput, null, null);
			clFinish(queue);
			// Print the result memory
			for (int i=0;i<image.getWidth();i++){
				image.setRGB(i, 0, (int)(realOutput.get(i)*10000));
			}
			// Clean up OpenCL resources
			clReleaseKernel(kernel);
			clReleaseProgram(program);
			clReleaseMemObject(reakiMem);
			clReleaseMemObject(imagiMem);
			clReleaseMemObject(imagaMem);
			clReleaseCommandQueue(queue);
			clReleaseContext(context);
			CL.destroy();
    	}catch(LWJGLException e){
    		e.printStackTrace();
    	}
    	
    	imComp.setImage(image);
   	}
    
    class ImageComponent extends JComponent 
    { 
      private static final long serialVersionUID = 8055865896136562197L; 
     
      private BufferedImage image; 
     
      public void setImage( BufferedImage image ) 
      { 
        this.image = image; 
        setPreferredSize( new Dimension(image.getWidth(), image.getHeight()) ); 
        repaint(); 
        invalidate(); 
      } 
     
       
      @Override
	protected void paintComponent( Graphics g ) 
      { 
        if ( image != null ) 
          g.drawImage( image, 0, 0, this ); 
      } 
    }
}

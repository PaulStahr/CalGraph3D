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

import java.io.*;
import java.math.BigInteger;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.SaveLineCreator;
/** 
* @author  Paul Stahr
* @version 04.02.2012
*/

public abstract class Options3
{	
    private static final HashMap<CharSequence,DataObject> data = new HashMap<CharSequence, DataObject> ();
    private static int modCount = 0;
    private static int lastSync = -1;
	private static final Logger logger = LoggerFactory.getLogger(Options3.class);
	private static final ArrayList<Runnable> oml = new ArrayList<Runnable>();
	private static Thread thread;
	
	public static class OptionSetList{
		private ArrayList <OptionSetObject> list = new ArrayList<OptionSetObject>();
		
		public final OptionSetList set(String key, Object value)
		{
			list.add(new OptionSetObject(key, value));
			return this;
		}
		
		public final void run()
		{
			Options3.set(list);
		}
	}
	
	public static final OptionSetList getSetList()
	{
		return new OptionSetList();
	}
	
	private static final void optionsUpdated(){
		++modCount;
		if (thread != null)
		{
			thread = new Thread()
			{
				public void run()
				{
					int modCount;
					do{
						modCount = Options3.modCount;
						synchronized(oml)
						{
							for (int i = 0; i < oml.size(); ++i)
							{
								oml.get(i).run();
							}
						}
					}while (modCount != Options3.modCount);
					thread = null;
				}
			};
			thread.start();
		}
	}
	
	public static class OptionSetObject{
		String key;
		String value;
		
		public OptionSetObject(String key, Object value) {
			this.key = key;
			this.value = String.valueOf(value);
		}
	}
	
	public static synchronized void set(List<OptionSetObject> list)
	{
		for (int i = 0; i < list.size(); ++i)
		{
			data.put(list.get(i).key, new DataObject(list.get(i).value));
		}
		optionsUpdated();
	}
	
	private static final Runnable shutdownRunnable = new Runnable(){
        @Override
		public void run() {
            if (modCount != 0)
                save();
        }
    };

    static{
    	try{
            load(DataHandler.getResourceAsStream("/options.txt"));
            final File file = new File(defaultDirectory().concat("/.graphs/options.txt"));
            if (!file.exists() || file.isDirectory())
                save();
            else
            	load(new FileInputStream(file));
        }catch (Exception e){
        	logger.error("Problems with loading standard Options", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownRunnable));
    }

    private Options3(){}
    
    public static final int modCount()
    {
        return modCount;
    }
    
    public synchronized static void addModificationListener(Runnable runnable)
    {
    	oml.add(runnable);
    	runnable.run();
    }
    
    private static final boolean save(){
    	if (lastSync == modCount)
    		return true;
        try{
            final File dir = new File(defaultDirectory() + "/.graphs");
            if (!dir.exists())
                dir.mkdir();
            if (!dir.isDirectory()){
                dir.delete();
                dir.mkdir();
            }
            final FileWriter writer = new FileWriter(dir + "/options.txt");
            final BufferedWriter outBuffer = new BufferedWriter(writer);
            @SuppressWarnings("unchecked")
			final Map.Entry<String,DataObject> entry[] = data.entrySet().toArray(new Map.Entry[0]);
            Arrays.sort(entry, new Comparator<Map.Entry<String, DataObject>>() {
				@Override
				public int compare(Entry<String, DataObject> o1, Entry<String, DataObject> o2) {
					return o1.getKey().compareToIgnoreCase(o2.getKey());
				}
			});
            SaveLineCreator saveLineCreator = new SaveLineCreator();
            for (Map.Entry<String,DataObject> elem : entry)
                outBuffer.write(saveLineCreator.getSaveLine(elem.getKey(), elem.getValue().sData));
            outBuffer.flush();
            outBuffer.close();
            writer.close();
        }catch (Exception e){
            logger.error("Can't save Options",e);
            return false;
        }
        lastSync = modCount;
        return true;
    }

    public static final Boolean getBoolean (final CharSequence name){
    	return getBoolean(name, null);
    }

    public static final Boolean getBoolean(final CharSequence name, Boolean standart){
        final DataObject value = data.get(name);
        if (value == null)
            return standart;
        return value.getBoolean(standart);
    }
    
    public static final Integer getInteger (final CharSequence name){
    	return getInteger(name, null);
    }

    public static final Integer getInteger(final CharSequence name, Integer standart){
        final DataObject value = data.get(name);
        if (value == null)
            return standart;
        return value.getInt(standart);
    }

    public static final Long getLong (final CharSequence name){
    	return getLong(name, null);
    }

    public static final Long getLong(final CharSequence name, Long standart){
        final DataObject value = data.get(name);
        if (value == null)
            return standart;
        return value.getLong(standart);
    }

    public static final BigInteger getBigInteger (final CharSequence name){
    	return getBigInteger (name, null);
    }

    public static final BigInteger getBigInteger(final CharSequence name, BigInteger standart){
        final DataObject value = data.get(name);
        if (value == null)
            return standart;
        return value.getBigInt(standart);
    }

    public static final Float getFloat (final CharSequence name){
    	return getFloat(name, null);
    }

    public static final Float getFloat(CharSequence name, Float standart){
        final DataObject value = data.get(name);
        if (value == null)
            return standart;
        return value.getFloat(standart);
    }
    
    public static final Double getDouble (final CharSequence name){
    	return getDouble(name, null);
    }

    public static final Double getDouble(CharSequence name, Double standart){
        final DataObject value = data.get(name);
        if (value == null)
            return standart;
        return value.getDouble(standart);
    }
    
    public static final Color getColor (final CharSequence name){
    	return getColor (name, null);
    }

    public static final Color getColor(CharSequence name, Color standart){
        final DataObject value = data.get(name);
        if (value == null)
            return standart;
        return value.getColor(standart);
    }
    
    public static final String getString(CharSequence name){
    	return getString(name, null);
    }    

    public static final String getString (final CharSequence name, String standart){
        final DataObject value = data.get(name);
        if (value == null)
            return standart;
        return value.sData == null ? standart : value.sData;
    }

    private static final boolean load(InputStream inStream){
    	boolean error = false;
        try{
            final InputStreamReader reader = new InputStreamReader(inStream);
            final BufferedReader inBuffer = new BufferedReader(reader);
            String line;
            SaveLineCreator saveLineCreator = new SaveLineCreator();
            while (null!=(line=inBuffer.readLine())){
            	try{
	            	final SaveLineCreator.SaveObject saveObject = saveLineCreator.getSaveObject(line);
	            	if (saveObject != null)
		                data.put(saveObject.variable, new DataObject(saveObject.value));
            	}catch(Exception e){
                    logger.error("Corrupt line in save File:\"" + line + '\"', e);
            		error = true;
            	}
            }
            inBuffer.close();
            reader.close();
        }catch (IOException e){
        	error = true;
            logger.info("IO Error");
        }
        if (error){
        	logger.info("At least one error occured, Replacing savefile");
            save();
            return false;
        }
        lastSync = modCount;
        return true;
    }

    public static final String defaultDirectory(){
        final String os = System.getProperty("os.name").toUpperCase();
        if (os.contains("WIN"))
            return System.getenv("APPDATA");
        if (os.contains("MAC"))
            return System.getProperty("user.home") + "/Library/Application Support";
        if (os.contains("NUX"))
            return System.getProperty("user.home");
        return System.getProperty("user.dir");
    }
    
    private static final class DataObject{
    	private final String sData;
    	private byte isLong = 0, isDouble = 0, isBoolean = 0;
    	private long lData = 0;
    	private double dData = 0;
    	private boolean bData = false;
    	private BigInteger biData = null;
    	private Color cData = null;
    	
    	public DataObject(String str){
    		this.sData = str;
    	}
    	
    	public final Integer getInt(Integer standart){
    		if (isLong == 1)
    			return (int)lData;
    		if (isLong == 0){
        		try{
        			lData = Long.parseLong(sData);
        			isLong = 1;
        			return (int)lData;
        		}catch(NumberFormatException e){
        			isLong = -1;
        		}    			
    		}
			return standart;
    	}

    	public final Long getLong(Long standart){
    		if (isLong == 1)
    			return lData;
    		if (isLong == 0){
        		try{
        			lData = Long.parseLong(sData);
        			isLong = 1;
        			return lData;
        		}catch(NumberFormatException e){
        			isLong = -1;
        		}    			
    		}
			return standart;
    	}

    	public final Double getDouble(Double standart){
    		if (isDouble == 1)
    			return dData;
    		if (isDouble == 0){
        		try{
        			dData = Double.parseDouble(sData);
        			isDouble = 1;
        			return dData;
        		}catch(NumberFormatException e){
        			isDouble = -1;
        		}    			
    		}
			return standart;
    	}
    	
        public final Color getColor(Color standart){
        	if (cData != null)
        		return cData;
            Integer value = getInt(null);
            if (value != null){
            	try{
            		return (cData = new Color(value, true));
            	}catch (Exception e){}
        	}
            return standart;
        }

    	public final Float getFloat(Float standart){
    		if (isDouble == 1)
    			return (float)dData;
    		if (isDouble == 0){
        		try{
        			dData = Double.parseDouble(sData);
        			isDouble = 1;
        			return (float)dData;
        		}catch(NumberFormatException e){
        			isDouble = -1;
        		}    			
    		}
			return standart;
    	}

    	public final BigInteger getBigInt(BigInteger standart){
    		try{
    			return biData == null ? biData = new BigInteger(sData) : biData;
    		}catch(NumberFormatException e){}
    		return standart;
    	}

    	public final Boolean getBoolean(Boolean standart){
    		if (isBoolean == 1)
    			return bData;
    		if (isBoolean == 0){
        		try{
        			bData = Boolean.parseBoolean(sData);
        			isBoolean = 1;
        			return bData;
        		}catch(NumberFormatException e){
        			isBoolean = -1;
        		}    			
    		}
			return standart;
    	}
    	
    	
		@Override
		public final int hashCode(){
    		return sData.hashCode();
    	}
    	
    	
		@Override
		public final boolean equals(Object o){
    		return o instanceof DataObject && ((DataObject)o).sData.equals(sData);
    	}
    }
}

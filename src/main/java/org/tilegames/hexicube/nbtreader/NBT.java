package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NBT
{
	public NBTCompound mainTag;

	/**
	 * Returns the NBT structure for the provided data.
	 *
	 * @param data - The data to parse
	 * @return The NBT structure in that data
	 * @throws IllegalArgumentException The data isn't valid
	 */
	public NBT(byte[] data) throws IllegalArgumentException
	{
		byte[] uncompressed;
		try
		{
			GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data));
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int res = 0;
			byte buf[] = new byte[1024];
			while (res >= 0) {
			    res = in.read(buf, 0, buf.length);
			    if (res > 0) {
			        out.write(buf, 0, res);
			    }
			}
			uncompressed = out.toByteArray();
		}
		catch (IOException e)
		{
			System.err.println("WARNING: GZIP failed, assuming uncompressed!");
			uncompressed = data;
		}
		ByteArrayInputStream s = new ByteArrayInputStream(uncompressed);
		if(s.read() != 10)
			throw new IllegalArgumentException("Invalid NBT data!");
		mainTag = new NBTCompound(s, true);
		if(s.available() > 0)
			System.err.println("WARNING: Data parse ended before end of file!");
	}
	
	/**
	 * Returns the NBT structure for the data inside a provided file.
	 *
	 * @param file - The file to read
	 * @return The NBT structure in that file
	 * @throws IllegalArgumentException The file is missing
	 * @throws IOException 
	 */
	public static NBT fromFile(File file) throws IllegalArgumentException, IOException
	{
		FileInputStream in = new FileInputStream(file);
		int pos = 0;
		int len = (int)file.length();
		byte[] data = new byte[len];
		while(pos < len)
		{
			int val = in.read(data, pos, len-pos);
			if(val == -1)
			{
				in.close();
				throw new IOException("File read error!");
			}
			pos += val;
		}
		in.close();
		return new NBT(data);
	}
	
	public byte[] getData() throws IOException
	{
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		mainTag.appendData(data, true);
		return data.toByteArray();
	}
	
	public void toFile(File file, boolean compress) throws IOException
	{
		byte[] data = getData();
		if(file.exists() && file.isFile())
		{
			if(!file.delete()) throw new IOException("Unable to delete existing file!");
		}
		//if(!file.getParentFile().mkdirs()) throw new IOException("Unable to make directories to file!");
		if(!file.exists() || !file.isFile())
		{
			if(!file.createNewFile()) throw new IOException("Unable to create new file!");
		}
		if(compress)
		{
			GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(file));
			out.write(data);
			out.flush();
			out.close();
		}
		else
		{
			FileOutputStream out = new FileOutputStream(file);
			out.write(data);
			out.flush();
			out.close();
		}
	}
	
	public static byte parseByte(ByteArrayInputStream data)
	{
		int val = data.read();
		if(val == -1) throw new IndexOutOfBoundsException("Stream ended!");
		return (byte)val;
	}
	
	public static void writeByte(ByteArrayOutputStream data, byte val)
	{
		data.write(val);
	}
	
	public static short parseShort(ByteArrayInputStream data) throws IndexOutOfBoundsException
	{
		int val1 = parseByte(data);
		if(val1 < 0) val1 += 256;
		int val2 = parseByte(data);
		if(val2 < 0) val2 += 256;
		return (short)((val1<<8)|val2);
	}
	
	public static void writeShort(ByteArrayOutputStream data, short val)
	{
		writeByte(data, (byte)(val>>8));
		writeByte(data, (byte)(val));
	}
	
	public static int parseInt(ByteArrayInputStream data) throws IndexOutOfBoundsException
	{
		int val1 = parseShort(data);
		if(val1 < 0) val1 += 65536;
		int val2 = parseShort(data);
		if(val2 < 0) val2 += 65536;
		return (int)((val1<<16)|val2);
	}
	
	public static void writeInt(ByteArrayOutputStream data, int val)
	{
		writeShort(data, (short)(val>>16));
		writeShort(data, (short)(val));
	}
	
	public static long parseLong(ByteArrayInputStream data) throws IndexOutOfBoundsException
	{
		long val1 = parseInt(data);
		if(val1 < 0) val1 += 4294967296L;
		long val2 = parseInt(data);
		if(val2 < 0) val2 += 4294967296L;
		return (val1<<32)|val2;
	}
	
	public static void writeLong(ByteArrayOutputStream data, long val)
	{
		writeInt(data, (int)(val>>32));
		writeInt(data, (int)(val));
	}
	
	public static float parseFloat(ByteArrayInputStream data) throws IndexOutOfBoundsException
	{
		return Float.intBitsToFloat(parseInt(data));
	}
	
	public static void writeFloat(ByteArrayOutputStream data, float val)
	{
		writeInt(data, Float.floatToIntBits(val));
	}
	
	public static double parseDouble(ByteArrayInputStream data) throws IndexOutOfBoundsException
	{
		return Double.longBitsToDouble(parseLong(data));
	}
	
	public static void writeDouble(ByteArrayOutputStream data, double val)
	{
		writeLong(data, Double.doubleToLongBits(val));
	}
	
	public static String parseString(ByteArrayInputStream data)
	{
		int len = parseShort(data);
		String str;
		try
		{
			byte[] data2 = new byte[len];
			for(int a = 0; a < len; a++) data2[a] = parseByte(data);
			str = new String(data2, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			System.err.println("An error occured when converting a string!");
			e.printStackTrace();
			str = "?????";
		}
		return str;
	}
	
	public static void writeString(ByteArrayOutputStream data, String val) throws IOException
	{
		byte[] str;
		try
		{
			str = val.getBytes("UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			throw new IOException("String format exception!");
		}
		writeShort(data, (short)str.length);
		data.write(str, 0, str.length);
	}
}
package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class NBTCompound extends NBTTag
{
	public ArrayList<NBTTag> tagList;
	
	/**
	 * Creates an empty compound tag with the given name.
	 *
	 * @param name - The name of the tag
	 * @return An empty compound tag
	 */
	public NBTCompound(String name)
	{
		this.name = name;
		tagList = new ArrayList<NBTTag>();
	}
	
	/**
	 * Creates a compound tag with the given name and tag list.
	 *
	 * @param name - The name of the tag
	 * @param tags - The tags to be added
	 * @return A compound tag containing the given tags
	 */
	public NBTCompound(String name, NBTTag[] tags)
	{
		this.name = name;
		tagList = new ArrayList<NBTTag>();
		for(int a = 0; a < tags.length; a++)
		{
			tagList.add(tags[a]);
		}
	}
	
	/**
	 * Creates a compound tag with the given name and tag list.
	 *
	 * @param name - The name of the tag
	 * @param tags - The tags to be added
	 * @return A compound tag containing the given tags
	 */
	public NBTCompound(String name, ArrayList<NBTTag> tags)
	{
		this.name = name;
		tagList = new ArrayList<NBTTag>();
		for(int a = 0; a < tags.size(); a++)
		{
			tagList.add(tags.get(a));
		}
	}
	
	/**
	 * Creates a compound tag with the given data stream.
	 * Avoid using this if possible, and use the other provided functions instead.
	 *
	 * @param data - The raw NBT data
	 * @param getName - Try to read the name at the start
	 * @return A compound tag
	 */
	public NBTCompound(ByteArrayInputStream data, boolean getName) throws IllegalArgumentException
	{
		tagList = new ArrayList<NBTTag>();
		if(getName)
		{
			this.name = NBT.parseString(data);
		}
		else this.name = "-";
		try
		{
			while(true)
			{
				int type = NBT.parseByte(data);
				if(type == 0)
				{
					return;
				}
				else if(type == 1)
				{
					String name = NBT.parseString(data);
					tagList.add(new NBTByte(name, NBT.parseByte(data)));
				}
				else if(type == 2)
				{
					String name = NBT.parseString(data);
					tagList.add(new NBTShort(name, NBT.parseShort(data)));
				}
				else if(type == 3)
				{
					String name = NBT.parseString(data);
					tagList.add(new NBTInt(name, NBT.parseInt(data)));
				}
				else if(type == 4)
				{
					String name = NBT.parseString(data);
					tagList.add(new NBTLong(name, NBT.parseLong(data)));
				}
				else if(type == 5)
				{
					String name = NBT.parseString(data);
					tagList.add(new NBTFloat(name, NBT.parseFloat(data)));
				}
				else if(type == 6)
				{
					String name = NBT.parseString(data);
					tagList.add(new NBTDouble(name, NBT.parseDouble(data)));
				}
				else if(type == 7)
				{
					String name = NBT.parseString(data);
					int dataLen = NBT.parseInt(data);
					byte[] data2 = new byte[dataLen];
					for(int a = 0; a < dataLen; a++)
					{
						data2[a] = NBT.parseByte(data);
					}
					tagList.add(new NBTByteArray(name, data2));
				}
				else if(type == 8)
				{
					String name = NBT.parseString(data);
					String name2 = NBT.parseString(data);
					tagList.add(new NBTString(name, name2));
				}
				else if(type == 9)
				{
					tagList.add(new NBTList(data, true));
				}
				else if(type == 10)
				{
					tagList.add(new NBTCompound(data, true));
				}
				else if(type == 11)
				{
					String name = NBT.parseString(data);
					int dataLen = NBT.parseInt(data);
					int[] data2 = new int[dataLen];
					for(int a = 0; a < dataLen; a++)
					{
						data2[a] = NBT.parseInt(data);
					}
					tagList.add(new NBTIntArray(name, data2));
				}
				else
				{
					throw new IllegalArgumentException("Unknown tag type: "+type);
				}
			}
		}
		catch(IndexOutOfBoundsException e)
		{
			throw new IllegalArgumentException("Data ended abruptly!");
		}
	}
	
	public void appendData(ByteArrayOutputStream data, boolean addName) throws IOException
	{
		if(addName)
		{
			NBT.writeByte(data, (byte)10);
			NBT.writeString(data, name);
		}
		for(int a = 0; a < tagList.size(); a++)
		{
			tagList.get(a).appendData(data, true);
		}
		NBT.writeByte(data, (byte)0);
	}
}
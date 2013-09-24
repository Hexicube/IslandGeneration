package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class NBTList extends NBTTag
{
	public ArrayList<NBTTag> tagList;
	
	/**
	 * Creates an empty list tag with the given name.
	 *
	 * @param name - The name of the tag
	 * @return An empty list tag
	 */
	public NBTList(String name)
	{
		this.name = name;
		tagList = new ArrayList<NBTTag>();
	}
	
	/**
	 * Creates a list tag with the given name and tag list.
	 *
	 * @param name - The name of the tag
	 * @param tags - The tags to be added
	 * @return A list tag containing the given tags
	 */
	public NBTList(String name, NBTTag[] tags)
	{
		this.name = name;
		tagList = new ArrayList<NBTTag>();
		for(int a = 0; a < tags.length; a++)
		{
			tagList.add(tags[a]);
		}
	}
	
	/**
	 * Creates a list tag with the given name and tag list.
	 *
	 * @param name - The name of the tag
	 * @param tags - The tags to be added
	 * @return A list tag containing the given tags
	 */
	public NBTList(String name, ArrayList<NBTTag> tags)
	{
		this.name = name;
		tagList = new ArrayList<NBTTag>();
		for(int a = 0; a < tags.size(); a++)
		{
			tagList.add(tags.get(a));
		}
	}
	
	/**
	 * Creates a list tag with the given data stream.
	 * Avoid using this if possible, and use the other provided functions instead.
	 *
	 * @param data - The raw NBT data
	 * @param getName - Try to read the name at the start
	 * @return A list tag
	 */
	public NBTList(ByteArrayInputStream data, boolean getName) throws IllegalArgumentException
	{
		tagList = new ArrayList<NBTTag>();
		if(getName)
		{
			this.name = NBT.parseString(data);
		}
		else this.name = "-";
		int type = NBT.parseByte(data);
		int count = NBT.parseInt(data);
		try
		{
			while(count > 0)
			{
				if(type == 0)
				{
					throw new IllegalArgumentException("End tag within a list!");
				}
				else if(type == 1)
				{
					tagList.add(new NBTByte("-", NBT.parseByte(data)));
				}
				else if(type == 2)
				{
					tagList.add(new NBTShort("-", NBT.parseShort(data)));
				}
				else if(type == 3)
				{
					tagList.add(new NBTInt("-", NBT.parseInt(data)));
				}
				else if(type == 4)
				{
					tagList.add(new NBTLong("-", NBT.parseLong(data)));
				}
				else if(type == 5)
				{
					tagList.add(new NBTFloat("-", NBT.parseFloat(data)));
				}
				else if(type == 6)
				{
					tagList.add(new NBTDouble("-", NBT.parseDouble(data)));
				}
				else if(type == 7)
				{
					int dataLen = NBT.parseInt(data);
					byte[] data2 = new byte[dataLen];
					for(int a = 0; a < dataLen; a++)
					{
						data2[a] = NBT.parseByte(data);
					}
					tagList.add(new NBTByteArray("-", data2));
				}
				else if(type == 8)
				{
					String name = NBT.parseString(data);
					tagList.add(new NBTString("-", name));
				}
				else if(type == 9)
				{
					tagList.add(new NBTList(data, false));
				}
				else if(type == 10)
				{
					tagList.add(new NBTCompound(data, false));
				}
				else if(type == 11)
				{
					int dataLen = NBT.parseInt(data);
					int[] data2 = new int[dataLen];
					for(int a = 0; a < dataLen; a++)
					{
						data2[a] = NBT.parseInt(data);
					}
					tagList.add(new NBTIntArray("-", data2));
				}
				else
				{
					throw new IllegalArgumentException("Unknown tag type: "+type);
				}
				count--;
			}
		}
		catch(IndexOutOfBoundsException e)
		{
			throw new IllegalArgumentException("Data ended abruptly!");
		}
	}
	
	public void appendData(ByteArrayOutputStream data, boolean addName) throws IOException
	{
		Class<? extends NBTTag> lastType = null;
		if(tagList.size() == 0) lastType = NBTByte.class;
		else lastType = tagList.get(0).getClass();
		if(addName)
		{
			NBT.writeByte(data, (byte)9);
			NBT.writeString(data, name);
		}
		byte b = 0;
		if(lastType == NBTByte.class) b = 1;
		else if(lastType == NBTShort.class) b = 2; 
		else if(lastType == NBTInt.class) b = 3;
		else if(lastType == NBTLong.class) b = 4;
		else if(lastType == NBTFloat.class) b = 5;
		else if(lastType == NBTDouble.class) b = 6;
		else if(lastType == NBTString.class) b = 8;
		else if(lastType == NBTList.class) b = 9;
		else if(lastType == NBTCompound.class) b = 10;
		else if(lastType == NBTByteArray.class) b = 7;
		else if(lastType == NBTIntArray.class) b = 11;
		else throw new IOException("Unknown type: "+lastType.getName());
		NBT.writeByte(data, b);
		NBT.writeInt(data, tagList.size());
		for(int a = 0; a < tagList.size(); a++)
		{
			NBTTag t = tagList.get(a);
			if(lastType != null)
			{
				if(!t.getClass().equals(lastType))
				{
					throw new IOException("Tag type mismatch in list!");
				}
			}
			lastType = t.getClass();
			t.appendData(data, false);
		}
	}
}
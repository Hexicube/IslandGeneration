package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTShort extends NBTTag
{
	public short val;

	/**
	 * Creates a short tag with the given name and value.
	 *
	 * @param name - The name of the tag
	 * @param val - The value to store
	 * @return A short tag
	 */
	public NBTShort(String name, short val)
	{
		this.name = name;
		this.val = val;
	}
	
	@Override
	public void appendData(ByteArrayOutputStream data, boolean addName) throws IOException
	{
		if(addName)
		{
			NBT.writeByte(data, (byte)2);
			NBT.writeString(data, name);
		}
		NBT.writeShort(data, val);
	}
}
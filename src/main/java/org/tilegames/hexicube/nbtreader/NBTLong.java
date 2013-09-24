package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTLong extends NBTTag
{
	public long val;

	/**
	 * Creates a long tag with the given name and value.
	 *
	 * @param name - The name of the tag
	 * @param val - The value to store
	 * @return A long tag
	 */
	public NBTLong(String name, long val)
	{
		this.name = name;
		this.val = val;
	}
	
	@Override
	public void appendData(ByteArrayOutputStream data, boolean addName) throws IOException
	{
		if(addName)
		{
			NBT.writeByte(data, (byte)4);
			NBT.writeString(data, name);
		}
		NBT.writeLong(data, val);
	}
}
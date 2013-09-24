package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTByte extends NBTTag
{
	public byte val;

	/**
	 * Creates a byte tag with the given name and value.
	 *
	 * @param name - The name of the tag
	 * @param val - The value to store
	 * @return A byte tag
	 */
	public NBTByte(String name, byte val)
	{
		this.name = name;
		this.val = val;
	}
	
	@Override
	public void appendData(ByteArrayOutputStream data, boolean addName) throws IOException
	{
		if(addName)
		{
			NBT.writeByte(data, (byte)1);
			NBT.writeString(data, name);
		}
		NBT.writeByte(data, val);
	}
}
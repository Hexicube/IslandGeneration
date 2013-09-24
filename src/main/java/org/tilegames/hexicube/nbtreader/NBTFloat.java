package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTFloat extends NBTTag
{
	public float val;

	/**
	 * Creates a float tag with the given name and value.
	 *
	 * @param name - The name of the tag
	 * @param val - The value to store
	 * @return A float tag
	 */
	public NBTFloat(String name, float val)
	{
		this.name = name;
		this.val = val;
	}
	
	@Override
	public void appendData(ByteArrayOutputStream data, boolean addName) throws IOException
	{
		if(addName)
		{
			NBT.writeByte(data, (byte)5);
			NBT.writeString(data, name);
		}
		NBT.writeFloat(data, val);
	}
}
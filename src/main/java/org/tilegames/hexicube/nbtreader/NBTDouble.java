package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTDouble extends NBTTag
{
	public double val;

	/**
	 * Creates a double tag with the given name and value.
	 *
	 * @param name - The name of the tag
	 * @param val - The value to store
	 * @return A double tag
	 */
	public NBTDouble(String name, double val)
	{
		this.name = name;
		this.val = val;
	}
	
	@Override
	public void appendData(ByteArrayOutputStream data, boolean addName) throws IOException
	{
		if(addName)
		{
			NBT.writeByte(data, (byte)6);
			NBT.writeString(data, name);
		}
		NBT.writeDouble(data, val);
	}
}
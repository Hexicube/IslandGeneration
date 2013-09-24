package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTInt extends NBTTag
{
	public int val;

	/**
	 * Creates an int tag with the given name and value.
	 *
	 * @param name - The name of the tag
	 * @param val - The value to store
	 * @return An int tag
	 */
	public NBTInt(String name, int val)
	{
		this.name = name;
		this.val = val;
	}
	
	@Override
	public void appendData(ByteArrayOutputStream data, boolean addName) throws IOException
	{
		if(addName)
		{
			NBT.writeByte(data, (byte)3);
			NBT.writeString(data, name);
		}
		NBT.writeInt(data, val);
	}
}
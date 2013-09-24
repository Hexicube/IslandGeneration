package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTString extends NBTTag
{
	public String val;

	/**
	 * Creates a string tag with the given name and value.
	 *
	 * @param name - The name of the tag
	 * @param val - The value to store
	 * @return A string tag
	 */
	public NBTString(String name, String val)
	{
		this.name = name;
		this.val = val;
	}
	
	@Override
	public void appendData(ByteArrayOutputStream data, boolean addName) throws IOException
	{
		if(addName)
		{
			NBT.writeByte(data, (byte)8);
			NBT.writeString(data, name);
		}
		NBT.writeString(data, val);
	}
}
package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTIntArray extends NBTTag
{
	public int[] data;

	/**
	 * Creates an int array tag with the given name and value.
	 *
	 * @param name - The name of the tag
	 * @param data - The int array to store
	 * @return An int array tag
	 */
	public NBTIntArray(String name, int[] data)
	{
		this.name = name;
		this.data = data;
	}
	
	@Override
	public void appendData(ByteArrayOutputStream data, boolean addName) throws IOException
	{
		if(addName)
		{
			NBT.writeByte(data, (byte)11);
			NBT.writeString(data, name);
		}
		NBT.writeInt(data, this.data.length);
		for(int a = 0; a < this.data.length; a++)
		{
			NBT.writeInt(data, this.data[a]);
		}
	}
}
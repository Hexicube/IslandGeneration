package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTByteArray extends NBTTag
{
	public byte[] data;

	/**
	 * Creates a byte array tag with the given name and value.
	 *
	 * @param name - The name of the tag
	 * @param data - The byte array to store
	 * @return A byte array tag
	 */
	public NBTByteArray(String name, byte[] data)
	{
		this.name = name;
		this.data = data;
	}
	
	@Override
	public void appendData(ByteArrayOutputStream data, boolean addName) throws IOException
	{
		if(addName)
		{
			NBT.writeByte(data, (byte)7);
			NBT.writeString(data, name);
		}
		NBT.writeInt(data, this.data.length);
		data.write(this.data, 0, this.data.length);
	}
}
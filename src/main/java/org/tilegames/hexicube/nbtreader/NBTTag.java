package org.tilegames.hexicube.nbtreader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class NBTTag
{
	public String name;
	
	public abstract void appendData(ByteArrayOutputStream data, boolean addName) throws IOException;
}
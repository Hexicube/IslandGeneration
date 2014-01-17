package org.tilegames.hexicube.bukkit.isle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.server.v1_7_R1.NBTTagCompound;
import net.minecraft.server.v1_7_R1.TileEntity;
import net.minecraft.server.v1_7_R1.TileEntityChest;

import org.bukkit.World;
import org.tilegames.hexicube.nbtreader.*;

public class Schematic
{
	public String name;
	public boolean valid;
	
	public short pathX, pathZ;
	public byte pathDir, pathWidth;
	
	public int width, height, length, weight, offset;
	public byte[] materialList, materialListData;
	public int[][][] structure;
	public ArrayList<TileEntity> blockStates;
	
	private IslePopulator populator;
	
	public Schematic(String name, File file) throws IllegalArgumentException, IOException
	{
		valid = false;
		this.name = name;
		NBT data = NBT.fromFile(file);
		convertData(data);
	}
	
	public Schematic(String name, byte[] NBTdata) throws IllegalArgumentException
	{
		valid = false;
		this.name = name;
		NBT data = new NBT(NBTdata);
		convertData(data);
	}
	
	private void convertData(NBT data) throws IllegalArgumentException
	{
		if(!data.mainTag.name.equalsIgnoreCase("parsed_schematic"))
		{
			System.out.println("WARNING: NBT file main tag not called \"parsed_schematic\" might be invalid!");
		}
		
		NBTInt i = (NBTInt)getTag(data.mainTag, "Weight", NBTInt.class);
		if(i == null)
			throw new IllegalArgumentException("NBT data missing \"Weight\"!");
		weight = i.val;
		i = (NBTInt)getTag(data.mainTag, "Offset", NBTInt.class);
		if(i == null)
			throw new IllegalArgumentException("NBT data missing \"Offset\"!");
		offset = i.val;
		
		NBTShort s = (NBTShort)getTag(data.mainTag, "PathX", NBTShort.class);
		if(s == null)
			throw new IllegalArgumentException("NBT data missing \"PathX\"!");
		pathX = s.val;
		s = (NBTShort)getTag(data.mainTag, "PathZ", NBTShort.class);
		if(s == null)
			throw new IllegalArgumentException("NBT data missing \"PathZ\"!");
		pathZ = s.val;
		NBTByte b = (NBTByte)getTag(data.mainTag, "PathDir", NBTByte.class);
		if(b == null)
			throw new IllegalArgumentException("NBT data missing \"PathDir\"!");
		pathDir = b.val;
		s = (NBTShort)getTag(data.mainTag, "Width", NBTShort.class);
		b = (NBTByte)getTag(data.mainTag, "PathWidth", NBTByte.class);
		if(b == null)
			throw new IllegalArgumentException("NBT data missing \"PathWidth\"!");
		pathWidth = b.val;
		s = (NBTShort)getTag(data.mainTag, "Width", NBTShort.class);
		if(s == null)
			throw new IllegalArgumentException("NBT data missing \"Width\"!");
		width = s.val;
		s = (NBTShort)getTag(data.mainTag, "Height", NBTShort.class);
		if(s == null)
			throw new IllegalArgumentException("NBT data missing \"Height\"!");
		height = s.val;
		s = (NBTShort)getTag(data.mainTag, "Length", NBTShort.class);
		if(s == null)
			throw new IllegalArgumentException("NBT data missing \"Length\"!");
		length = s.val;
		
		NBTString str = (NBTString)getTag(data.mainTag, "Materials", NBTString.class);
		if(str == null)
			throw new IllegalArgumentException("NBT data missing \"Materials\"!");
		if(!str.val.equals("Alpha"))
			throw new IllegalArgumentException("Unsupported materials type: "+str.val);
		
		ArrayList<byte[]> tileData = new ArrayList<byte[]>();
		structure = new int[width][length][height];
		NBTByteArray blocks = (NBTByteArray)getTag(data.mainTag, "Blocks", NBTByteArray.class);
		if(blocks == null)
			throw new IllegalArgumentException("NBT data missing \"Blocks\"!");
		if(blocks.data.length != width*height*length)
			throw new IllegalArgumentException("NBT data \"Blocks\" invalid length!");
		NBTByteArray blockData = (NBTByteArray)getTag(data.mainTag, "Data", NBTByteArray.class);
		if(blockData == null)
			throw new IllegalArgumentException("NBT data missing \"Data\"!");
		if(blockData.data.length != width*height*length)
			throw new IllegalArgumentException("NBT data \"Data\" invalid length!");
		for(int x = 0; x < width; x++)
		{
			for(int z = 0; z < length; z++)
			{
				for(int y = 0; y < height; y++)
				{
					int dataPos = x+z*width+y*width*length;
					int pos = 0;
					while(pos < tileData.size())
					{
						byte[] data2 = tileData.get(pos);
						if(blocks.data[dataPos] != data2[0] || blockData.data[dataPos] != data2[1]) pos++;
						else break;
					}
					if(pos == tileData.size())
					{
						tileData.add(new byte[]{blocks.data[dataPos], blockData.data[dataPos]});
					}
					structure[x][y][z] = pos;
				}
			}
		}
		materialList = new byte[tileData.size()];
		materialListData = new byte[tileData.size()];
		for(int a = 0; a < tileData.size(); a++)
		{
			byte[] data2 = tileData.get(a);
			materialList[a] = data2[0];
			materialListData[a] = data2[1];
		}
		ArrayList<int[]> airPoints = new ArrayList<int[]>();
		for(int x = 0; x < width; x++)
		{
			for(int y = 0; y < height; y++)
			{
				for(int z = 0; z < length; z++)
				{
					if(x == 0 || y == 0 || z == 0 ||
					   x == width-1 || y == height-1 || z == length-1)
					{
						if(materialList[structure[x][y][z]] == 0) airPoints.add(new int[]{x, y, z});
					}
				}
			}
		}
		while(airPoints.size() > 0)
		{
			int[] point = airPoints.remove(0);
			try
			{
				if(materialList[structure[point[0]][point[1]][point[2]]] == 0)
				{
					structure[point[0]][point[1]][point[2]] = -1;
					airPoints.add(new int[]{point[0]+1,point[1],point[2]});
					airPoints.add(new int[]{point[0]-1,point[1],point[2]});
					airPoints.add(new int[]{point[0],point[1]+1,point[2]});
					airPoints.add(new int[]{point[0],point[1]-1,point[2]});
					airPoints.add(new int[]{point[0],point[1],point[2]+1});
					airPoints.add(new int[]{point[0],point[1],point[2]-1});
				}
			}
			catch(IndexOutOfBoundsException e){}
		}
		//TODO: entities?
		//TODO: tile entities
		System.out.println("Loaded schematic: "+name);
		valid = true;
	}
	
	private NBTTag getTag(NBTCompound tag, String name, Class<?> type)
	{
		for(int a = 0; a < tag.tagList.size(); a++)
		{
			NBTTag t = tag.tagList.get(a);
			if(t.name.equals(name))
			{
				if(t.getClass().equals(type))
				{
					return tag.tagList.get(a);
				}
			}
		}
		return null;
	}
	
	@SuppressWarnings("unused")
	private void populateChestEntity(TileEntityChest entity, NBTCompound tag)
	{
		//TODO
	}
	
	public void paste(World world, int x, int y, int z)
	{
		if(valid)
		{
			for(int xPos = x; xPos < x+width; x++)
			{
				for(int zPos = z; zPos < z+length; z++)
				{
					for(int yPos = y; yPos < y+height; yPos++)
					{
						populator.setBlockWithData(world, xPos, yPos, zPos, materialList[structure[x][y][z]], materialListData[structure[x][y][z]]);
					}
				}
			}
			int size = blockStates.size();
			for(int a = 0; a < size; a++)
			{
				TileEntity t = blockStates.get(a);
				NBTTagCompound data = new NBTTagCompound();
				t.b(data);
				t = TileEntity.c(data);
				t.x += x;
				t.y += y;
				t.z += z;
				populator.addTileEntity(world, t);
			}
		}
	}
}
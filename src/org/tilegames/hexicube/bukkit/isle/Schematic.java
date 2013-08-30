package org.tilegames.hexicube.bukkit.isle;

import java.util.ArrayList;

import net.minecraft.server.v1_6_R2.NBTTagCompound;
import net.minecraft.server.v1_6_R2.TileEntity;

import org.bukkit.World;

public class Schematic
{
	public String name;
	public boolean valid;
	
	public int width, height, depth, weight, offset;
	public int[] materialList, materialListData;
	public int[][][] structure;
	public ArrayList<TileEntity> blockStates;
	
	private IslePopulator populator;
	
	public Schematic(String name, IslePopulator populator)
	{
		this.name = name;
		this.populator = populator;
		//TODO: load data
	}
	
	public void paste(World world, int x, int y, int z)
	{
		if(valid)
		{
			for(int xPos = x; xPos < x+width; x++)
			{
				for(int zPos = z; zPos < z+depth; z++)
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
				t.y +=y;
				t.z += z;
				populator.addTileEntity(world, t);
			}
		}
	}
}
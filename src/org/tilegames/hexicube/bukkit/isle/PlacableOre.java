package org.tilegames.hexicube.bukkit.isle;

import java.util.Random;

public class PlacableOre
{
	private int oreID, oreData, veinMinSize, veinMaxSize;
	private int[] replacableBlocks;
	
	private double chance;
	
	public PlacableOre(int oreID, int oreData, int veinMinSize, int veinMaxSize, int[] replacableBlocks, double chance)
	{
		this.oreID = oreID;
		this.oreData = oreData;
		this.veinMinSize = veinMinSize;
		this.veinMaxSize = veinMaxSize;
		this.replacableBlocks = replacableBlocks;
		this.chance = chance;
	}
	
	public int getOreID()
	{
		return oreID;
	}
	
	public int getOreData()
	{
		return oreData;
	}
	
	public int getVeinSize(Random rand)
	{
		return veinMinSize+rand.nextInt(veinMaxSize-veinMinSize+1);
	}
	
	public boolean canReplace(int ID)
	{
		for(int a = 0; a < replacableBlocks.length; a++)
		{
			if(ID == replacableBlocks[a]) return true;
		}
		return false;
	}
	
	public double getChance()
	{
		return chance;
	}
}
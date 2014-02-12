package org.tilegames.hexicube.bukkit.isle;

public class DungeonLootItem
{
	public DungeonLootItem(int ID, int DMG, int min, int max, int weight)
	{
		itemID = ID;
		itemDMG = DMG;
		minCount = min;
		maxCount = max;
		this.weight = weight;
	}
	
	public int itemID, itemDMG, minCount, maxCount, weight;
}
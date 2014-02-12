package org.tilegames.hexicube.bukkit.isle;

import java.util.ArrayList;

public class DungeonLootChest
{
	public String chestName;
	public boolean useChestName;
	
	public boolean allowSameEntry;
	public int weight, minItems, maxItems, groupTotalWeight;
	public ArrayList<DungeonLootItemGroup> itemGroups;
}
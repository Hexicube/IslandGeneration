package org.tilegames.hexicube.bukkit.isle;

import java.util.ArrayList;

public class DungeonLootItemGroup
{
	public DungeonLootItemGroup(ArrayList<DungeonLootItem> itemList, int weight)
	{
		items = new DungeonLootItem[itemList.size()];
		for(int a = 0; a < items.length; a++)
		{
			items[a] = itemList.get(a);
			itemTotalWeight += items[a].weight;
		}
		this.weight = weight;
	}
	
	public DungeonLootItem[] items;
	public int weight, itemTotalWeight;
}
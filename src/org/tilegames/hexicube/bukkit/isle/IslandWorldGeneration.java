package org.tilegames.hexicube.bukkit.isle;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;

public final class IslandWorldGeneration extends JavaPlugin implements Listener
{
	public static int islandSpacing, islandStartY;
	public static double[] rarityModifiers;
	
	@Override
	public void onEnable()
	{
		islandSpacing = getConfig().getInt("island.spacing", 8);
		getConfig().set("island.spacing", islandSpacing);
		islandStartY = getConfig().getInt("island.height", 150);
		getConfig().set("island.height", islandStartY);
		rarityModifiers = new double[8];
		rarityModifiers[0] = getConfig().getDouble("rarity.coalore", 1);
		getConfig().set("rarity.coalore", rarityModifiers[0]);
		rarityModifiers[1] = getConfig().getDouble("rarity.ironore", 1);
		getConfig().set("rarity.ironore", rarityModifiers[1]);
		rarityModifiers[2] = getConfig().getDouble("rarity.goldore", 1);
		getConfig().set("rarity.goldore", rarityModifiers[2]);
		rarityModifiers[3] = getConfig().getDouble("rarity.redstoneore", 1);
		getConfig().set("rarity.redstoneore", rarityModifiers[3]);
		rarityModifiers[4] = getConfig().getDouble("rarity.diamondore", 1);
		getConfig().set("rarity.diamondore", rarityModifiers[4]);
		rarityModifiers[5] = getConfig().getDouble("rarity.emeraldore", 1);
		getConfig().set("rarity.emeraldore", rarityModifiers[5]);
		rarityModifiers[6] = getConfig().getDouble("rarity.fluidpool", 1);
		getConfig().set("rarity.fluidpool", rarityModifiers[6]);
		rarityModifiers[7] = getConfig().getDouble("rarity.gravelpatch", 1);
		getConfig().set("rarity.gravelpatch", rarityModifiers[7]);
		saveConfig();
	}
	
	@Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
	{
		return new ChunkGen();
	}
}
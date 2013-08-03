package org.tilegames.hexicube.bukkit.isle;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;

public final class IslandWorldGeneration extends JavaPlugin implements Listener
{
	@Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
	{
		return new ChunkGen();
	}
}
package org.tilegames.hexicube.bukkit.isle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

public class ChunkGen extends ChunkGenerator
{
	public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid)
	{
		for(int x = 0; x < 16; x++)
		{
			for(int z = 0; z < 16; z++)
			{
				world.setBiome(chunkX*16+x, chunkZ*16+z, Biome.SKY);
			}
		}
		return new byte[world.getMaxHeight()/16][];
	}
	
	@Override
	public List<BlockPopulator> getDefaultPopulators(World world)
	{
		ArrayList<BlockPopulator> pops = new ArrayList<BlockPopulator>();
		pops.add(new IslePopulator());
		return pops;
	}
}
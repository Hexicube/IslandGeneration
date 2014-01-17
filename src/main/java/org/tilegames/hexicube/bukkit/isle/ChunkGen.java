package org.tilegames.hexicube.bukkit.isle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

public class ChunkGen extends ChunkGenerator
{
	private static void setBlockId(byte[][] chunk_data, int x, int y, int z, byte id)
	{
		int sec_id = (y >> 4);
		int yy = y & 0xF;
		if (chunk_data[sec_id] == null)
		{
			chunk_data[sec_id] = new byte[4096];
		}
		chunk_data[sec_id][(yy << 8) | (z << 4) | x] = id;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid)
	{
		for(int x = 0; x < 16; x++)
		{
			for(int z = 0; z < 16; z++)
			{
				world.setBiome(chunkX*16+x, chunkZ*16+z, Biome.SKY);
			}
		}
		
		if(IslandWorldGeneration.waterLevel > 0)
		{
			byte[][] data = new byte[world.getMaxHeight()/16][];
			for(int x = 0; x < 16; x++)
			{
				for(int z = 0; z < 16; z++)
				{
					setBlockId(data, x, 0, z, (byte)Material.BEDROCK.getId());
					for(int y = 1; y <= IslandWorldGeneration.waterLevel; y++)
					{
						setBlockId(data, x, y, z, (byte)IslandWorldGeneration.waterBlock);
					}
				}
			}
			return data;
		}
		else return new byte[world.getMaxHeight()/16][];
	}
	
	@Override
	public List<BlockPopulator> getDefaultPopulators(World world)
	{
		ArrayList<BlockPopulator> pops = new ArrayList<BlockPopulator>();
		pops.add(new IslePopulator());
		return pops;
	}
}
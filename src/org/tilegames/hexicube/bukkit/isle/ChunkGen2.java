package org.tilegames.hexicube.bukkit.isle;

import java.util.List;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

public class ChunkGen2 extends ChunkGenerator
{
	private ChunkGenerator parent;
	
	public ChunkGen2(ChunkGenerator parent)
	{
		this.parent = parent;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public byte[] generate(World world, Random random, int chunkX, int chunkZ)
	{
		return parent.generate(world, random, chunkX, chunkZ);
	}
	
	@Override
	public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid)
	{
		return parent.generateBlockSections(world, random, chunkX, chunkZ, biomeGrid);
	}
	
	@Override
	public short[][] generateExtBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid)
	{
		return parent.generateExtBlockSections(world, random, chunkX, chunkZ, biomeGrid);
	}
	
	@Override
	public List<BlockPopulator> getDefaultPopulators(World world)
	{
		List<BlockPopulator> pops = parent.getDefaultPopulators(world);
		pops.add(new IslePopulator());
		return pops;
	}
}
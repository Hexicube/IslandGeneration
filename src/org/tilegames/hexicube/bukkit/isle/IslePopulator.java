package org.tilegames.hexicube.bukkit.isle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.server.v1_6_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_6_R2.ChunkSection;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_6_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;

@SuppressWarnings("deprecation")
public class IslePopulator extends BlockPopulator
{
	private ArrayList<net.minecraft.server.v1_6_R2.Chunk> chunksToReload;
	private UsedSections lastUsedSections;
	
	@SuppressWarnings("unchecked")
	private void sendChunkToClient(Chunk chunk, Player player)
	{
		ChunkCoordIntPair ccip = new ChunkCoordIntPair(chunk.getX(), chunk.getZ());
		for(Player p: chunk.getWorld().getPlayers())
		{
			int playerChunkX = p.getLocation().getBlockX() >> 4;
			int playerChunkZ = p.getLocation().getBlockZ() >> 4;
			int dist = Math.min(Math.abs(playerChunkX-chunk.getX()), Math.abs(playerChunkZ-chunk.getZ()));
			if(dist <= Bukkit.getServer().getViewDistance())
			{
				List<ChunkCoordIntPair> chunkCoordIntPairQueue = (List<ChunkCoordIntPair>)((CraftPlayer)p).getHandle().chunkCoordIntPairQueue;
				if(!chunkCoordIntPairQueue.contains(ccip))
				chunkCoordIntPairQueue.add(ccip);
			}
		}
	}
	
	private int[][] genIslandData(int size, Random rand)
	{
		int tileSize = 10;
		int[][] tileData = new int[size][size];
		int subsize = size*tileSize;
		int radiusSteps = Math.min(subsize, subsize)/15;
		byte[][] data = new byte[subsize][subsize];
		ArrayList<int[]> steps = new ArrayList<int[]>();
		steps.add(new int[]{(int)(subsize*0.5), (int)(subsize*0.5), radiusSteps*5});
		while(steps.size() > 0)
		{
			int[] step = steps.remove(0);
			if(step[2] > radiusSteps)
			{
				for(int x = 0; x < step[2]; x++)
				{
					for(int y = 0; y < step[2]; y++)
					{
						double distSqrd = (step[2]*0.5-x)*(step[2]*0.5-x)+(step[2]*0.5-y)*(step[2]*0.5-y);
						if(distSqrd < step[2]*step[2]*0.25)
						{
							double strength = (1.0-distSqrd/(step[2]*step[2])*4)*((step[2]==radiusSteps*5)?0.1:0.065);
							int xPos = (int)(x+step[0]-step[2]*0.5), yPos = (int)(y+step[1]-step[2]*0.5);
							int val = data[xPos][yPos];
							if(val < 0) val += 256;
							val += strength*255;
							if(val > 255) val = 255;
							data[xPos][yPos] = (byte)val;
						}
					}
				}
				for(int a = 0; a < 6; a++)
				{
					double angle = (double)rand.nextInt(360)/180*Math.PI;
					steps.add(new int[]{(int)((double)step[0]+Math.cos(angle)*(double)step[2]*0.5), (int)((double)step[1]+Math.sin(angle)*(double)step[2]*0.5), step[2]-radiusSteps});
				}
			}
		}
		for(int x = 0; x < size; x++)
		{
			for(int y = 0; y < size; y++)
			{
				float strength = 0;
				for(int x2 = 0; x2 < tileSize; x2++)
				{
					for(int y2 = 0; y2 < tileSize; y2++)
					{
						int val = data[x*tileSize+x2][y*tileSize+y2];
						if(val < 0) val += 256;
						strength += (float)val/(float)(tileSize*tileSize);
					}
				}
				int value = (int)strength;
				tileData[x][y] = value;
			}
		}
		return tileData;
	}
	
	private ChunkSection getChunkSection(World world, int x, int y, int z)
	{
		Chunk c = world.getChunkAt(x>>4, z>>4);
		UsedSections usedSections = null;
		if(lastUsedSections != null &&
		   lastUsedSections.chunkX == x>>4 &&
		   lastUsedSections.chunkZ == z>>4) usedSections = lastUsedSections;
		if(usedSections == null)
		{
			net.minecraft.server.v1_6_R2.Chunk chunk = ((CraftChunk)c).getHandle();
			chunksToReload.add(chunk);
			Field f = null;
			try
			{
				f = chunk.getClass().getDeclaredField("sections");
			}
			catch(NoSuchFieldException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			catch(SecurityException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			f.setAccessible(true);
			ChunkSection[] sections = null;
			try
			{
				sections = (ChunkSection[]) f.get(chunk);
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			usedSections = new UsedSections();
			usedSections.chunkX = x>>4;
			usedSections.chunkZ = z>>4;
			usedSections.sections = sections;
		}
		lastUsedSections = usedSections;
		ChunkSection[] section = usedSections.sections;
		ChunkSection chunksection = section[y >> 4];
		if(chunksection == null)
		{
			chunksection = section[y >> 4] = new ChunkSection(y >> 4 << 4, !((CraftChunk)c).getHandle().world.worldProvider.f);
		}
		return chunksection;
	}
	
	private void setBlock(World world, int x, int y, int z, int id)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		if(chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.MOB_SPAWNER.getId())
		{
			world.getBlockAt(x, y, z).setTypeId(0);
		}
		chunksection.setTypeId(x & 15, y & 15, z & 15, id);
		chunksection.setData(x & 15, y & 15, z & 15, 0);
	}
	
	private void setBlockWithData(World world, int x, int y, int z, int id, int data)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		chunksection.setTypeId(x & 15, y & 15, z & 15, id);
		chunksection.setData(x & 15, y & 15, z & 15, data);
	}
	
	private void setFluidAndNeighbourStone(World world, int x, int y, int z, boolean lava)
	{
		int fluid = lava?Material.LAVA.getId():Material.WATER.getId();
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		chunksection.setTypeId(x & 15, y & 15, z & 15, fluid);
		chunksection.setData(x & 15, y & 15, z & 15, 0);
		chunksection = getChunkSection(world, x-1, y, z);
		if(chunksection.getTypeId((x-1) & 15, y & 15, z & 15) != fluid)
		{
			chunksection.setTypeId((x-1) & 15, y & 15, z & 15, Material.STONE.getId());
			chunksection.setData((x-1) & 15, y & 15, z & 15, 0);
		}
		chunksection = getChunkSection(world, x+1, y, z);
		if(chunksection.getTypeId((x+1) & 15, y & 15, z & 15) != fluid)
		{
			chunksection.setTypeId((x+1) & 15, y & 15, z & 15, Material.STONE.getId());
			chunksection.setData((x+1) & 15, y & 15, z & 15, 0);
		}
		chunksection = getChunkSection(world, x, y, z-1);
		if(chunksection.getTypeId(x & 15, y & 15, (z-1) & 15) != fluid)
		{
			chunksection.setTypeId(x & 15, y & 15, (z-1) & 15, Material.STONE.getId());
			chunksection.setData(x & 15, y & 15, (z-1) & 15, 0);
		}
		chunksection = getChunkSection(world, x, y, z+1);
		if(chunksection.getTypeId(x & 15, y & 15, (z+1) & 15) != fluid)
		{
			chunksection.setTypeId(x & 15, y & 15, (z+1) & 15, Material.STONE.getId());
			chunksection.setData(x & 15, y & 15, (z+1) & 15, 0);
		}
		chunksection = getChunkSection(world, x, y-1, z);
		if(chunksection.getTypeId(x & 15, (y-1) & 15, z & 15) != fluid)
		{
			chunksection.setTypeId(x & 15, (y-1) & 15, z & 15, Material.STONE.getId());
			chunksection.setData(x & 15, (y-1) & 15, z & 15, 0);
		}
	}
	
	private void setBlockIfAlreadyAir(World world, int x, int y, int z, int id)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		if(chunksection.getTypeId(x & 15, y & 15, z & 15) == 0 ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.SNOW.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.VINE.getId())
		{
			chunksection.setTypeId(x & 15, y & 15, z & 15, id);
			chunksection.setData(x & 15, y & 15, z & 15, 0);
		}
	}
	
	private void setBlockIfAlreadyAirWithData(World world, int x, int y, int z, int id, int data)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		if(chunksection.getTypeId(x & 15, y & 15, z & 15) == 0 ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.SNOW.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.VINE.getId())
		{
			chunksection.setTypeId(x & 15, y & 15, z & 15, id);
			chunksection.setData(x & 15, y & 15, z & 15, data);
		}
	}
	
	private boolean setBlockIfAcceptsOre(World world, int x, int y, int z, int id)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		int oldid = chunksection.getTypeId(x & 15, y & 15, z & 15);
		if(oldid == Material.STONE.getId() ||
		   oldid == Material.NETHERRACK.getId() ||
		   oldid == Material.SAND.getId())
		{
			chunksection.setTypeId(x & 15, y & 15, z & 15, id);
			chunksection.setData(x & 15, y & 15, z & 15, 0);
			return true;
		}
		return false;
	}
	
	private void placeBasicTree(World world, int x, int y, int z, Random rand)
	{
		int height = 4 + rand.nextInt(3);
		for(int y2 = 0; y2 <= height; y2++)
		{
			if(y2 < height) setBlock(world, x, y+y2, z, Material.LOG.getId());
			if(height-y2 < 4)
			{
				for(int x2 = -2; x2 < 3; x2++)
				{
					for(int z2 = -2; z2 < 3; z2++)
					{
						if((x2 != -2 && x2 != 2) ||
						   (z2 != -2 && z2 != 2)) setBlockIfAlreadyAir(world, x+x2, y+y2, z+z2, Material.LEAVES.getId());
					}
				}
			}
		}
		for(int x2 = -1; x2 < 2; x2++)
		{
			for(int z2 = -1; z2 < 2; z2++)
			{
				setBlockIfAlreadyAir(world, x+x2, y+height+1, z+z2, Material.LEAVES.getId());
			}
		}
	}
	
	private void placeJungleTree(World world, int x, int y, int z, Random rand)
	{
		int height = rand.nextBoolean()?1:(4 + rand.nextInt(6));
		for(int y2 = 0; y2 <= height; y2++)
		{
			if(y2 < height) setBlockWithData(world, x, y+y2, z, Material.LOG.getId(), 3);
			if(height-y2 < 4)
			{
				for(int x2 = -2; x2 < 3; x2++)
				{
					for(int z2 = -2; z2 < 3; z2++)
					{
						if((x2 != -2 && x2 != 2) ||
						   (z2 != -2 && z2 != 2)) setBlockIfAlreadyAirWithData(world, x+x2, y+y2, z+z2, Material.LEAVES.getId(), 3);
					}
				}
			}
			else
			{
				if(rand.nextInt(5) == 3) setBlockWithData(world, x-1, y+y2, z, Material.COCOA.getId(), 3+rand.nextInt(3)*4);
				if(rand.nextInt(5) == 3) setBlockWithData(world, x+1, y+y2, z, Material.COCOA.getId(), 1+rand.nextInt(3)*4);
				if(rand.nextInt(5) == 3) setBlockWithData(world, x, y+y2, z-1, Material.COCOA.getId(), 0+rand.nextInt(3)*4);
				if(rand.nextInt(5) == 3) setBlockWithData(world, x, y+y2, z+1, Material.COCOA.getId(), 2+rand.nextInt(3)*4);
			}
		}
		for(int x2 = -1; x2 < 2; x2++)
		{
			for(int z2 = -1; z2 < 2; z2++)
			{
				setBlockIfAlreadyAirWithData(world, x+x2, y+height+1, z+z2, Material.LEAVES.getId(), 3);
			}
		}
		if(height == 1)
		{
			for(int x2 = -2; x2 < 3; x2++)
			{
				for(int z2 = -2; z2 < 3; z2++)
				{
					if((x2 != -2 && x2 != 2) ||
					   (z2 != -2 && z2 != 2)) setBlockIfAlreadyAirWithData(world, x+x2, y-1, z+z2, Material.LEAVES.getId(), 3);
				}
			}
			for(int x2 = -1; x2 < 2; x2++)
			{
				for(int z2 = -1; z2 < 2; z2++)
				{
					setBlockIfAlreadyAirWithData(world, x+x2, y-2, z+z2, Material.LEAVES.getId(), 3);
				}
			}
		}
	}
	
	private void placeSwampTree(World world, int x, int y, int z, Random rand)
	{
		int height = 4 + rand.nextInt(3);
		for(int y2 = 0; y2 <= height; y2++)
		{
			if(y2 < height) setBlock(world, x, y+y2, z, Material.LOG.getId());
			if(height-y2 < 4)
			{
				for(int x2 = -2; x2 < 3; x2++)
				{
					for(int z2 = -2; z2 < 3; z2++)
					{
						if((x2 != -2 && x2 != 2) ||
						   (z2 != -2 && z2 != 2)) setBlockIfAlreadyAir(world, x+x2, y+y2, z+z2, Material.LEAVES.getId());
					}
				}
			}
		}
		for(int x2 = -1; x2 < 2; x2++)
		{
			for(int z2 = -1; z2 < 2; z2++)
			{
				setBlockIfAlreadyAir(world, x+x2, y+height+1, z+z2, Material.LEAVES.getId());
			}
		}
		for(int a = 0; a < 10; a++)
		{
			int pos = rand.nextInt(16);
			int yPos = rand.nextInt(height-1)+2;
			if(pos == 0)
			{
				for(int y2 = yPos; y2 >= 0; y2--)
				{
					setBlockIfAlreadyAirWithData(world, x+2, y+y2, z+2, Material.VINE.getId(), 6);
				}
			}
			else if(pos == 1)
			{
				for(int y2 = yPos; y2 >= 0; y2--)
				{
					setBlockIfAlreadyAirWithData(world, x+2, y+y2, z-2, Material.VINE.getId(), 3);
				}
			}
			else if(pos == 2)
			{
				for(int y2 = yPos; y2 >= 0; y2--)
				{
					setBlockIfAlreadyAirWithData(world, x-2, y+y2, z+2, Material.VINE.getId(), 12);
				}
			}
			else if(pos == 3)
			{
				for(int y2 = yPos; y2 >= 0; y2--)
				{
					setBlockIfAlreadyAirWithData(world, x-2, y+y2, z-2, Material.VINE.getId(), 9);
				}
			}
			else if(pos < 7)
			{
				for(int y2 = yPos; y2 >= 0; y2--)
				{
					setBlockIfAlreadyAirWithData(world, x+3, y+y2, z+pos-5, Material.VINE.getId(), 2);
				}
			}
			else if(pos < 10)
			{
				for(int y2 = yPos; y2 >= 0; y2--)
				{
					setBlockIfAlreadyAirWithData(world, x-3, y+y2, z+pos-8, Material.VINE.getId(), 8);
				}
			}
			else if(pos < 13)
			{
				for(int y2 = yPos; y2 >= 0; y2--)
				{
					setBlockIfAlreadyAirWithData(world, x+pos-11, y+y2, z+3, Material.VINE.getId(), 4);
				}
			}
			else
			{
				for(int y2 = yPos; y2 >= 0; y2--)
				{
					setBlockIfAlreadyAirWithData(world, x+pos-14, y+y2, z-3, Material.VINE.getId(), 1);
				}
			}
		}
	}
	
	private void placeRedwoodTree(World world, int x, int y, int z, Random rand)
	{
		int height = 6 + rand.nextInt(7);
		for(int y2 = 0; y2 < height; y2++)
		{
			setBlockWithData(world, x, y+y2, z, Material.LOG.getId(), 1);
			if(y2 > 1)
			{
				if((height-y2)%2 == 1)
				{
					for(int x2 = -2; x2 < 3; x2++)
					{
						for(int z2 = -2; z2 < 3; z2++)
						{
							if((x2 != -2 && x2 != 2) ||
							   (z2 != -2 && z2 != 2))
							{
								setBlockIfAlreadyAirWithData(world, x+x2, y+y2, z+z2, Material.LEAVES.getId(), 1);
							}
						}
					}
				}
				else
				{
					for(int x2 = -1; x2 < 2; x2++)
					{
						for(int z2 = -1; z2 < 2; z2++)
						{
							if(x2 == 0 || z2 == 0)
							{
								setBlockIfAlreadyAirWithData(world, x+x2, y+y2, z+z2, Material.LEAVES.getId(), 1);
							}
						}
					}
				}
			}
		}
		for(int x2 = -1; x2 < 2; x2++)
		{
			for(int z2 = -1; z2 < 2; z2++)
			{
				if(x2 == 0 || z2 == 0)
				{
					setBlockIfAlreadyAirWithData(world, x+x2, y+height, z+z2, Material.LEAVES.getId(), 1);
					if(x2 != 0 || z2 != 0) setBlockIfAlreadyAir(world, x+x2, y+height, z+z2, Material.SNOW.getId());
				}
			}
		}
		for(int x2 = -2; x2 < 3; x2++)
		{
			for(int z2 = -2; z2 < 3; z2++)
			{
				if(x2 == -2 || x2 == 2)
				{
					if(z2 != -2 && z2 != 2)
					{
						setBlockIfAlreadyAir(world, x+x2, y+height, z+z2, Material.SNOW.getId());
					}
				}
				if(z2 == -2 || z2 == 2)
				{
					if(x2 != -2 && x2 != 2)
					{
						setBlockIfAlreadyAir(world, x+x2, y+height, z+z2, Material.SNOW.getId());
					}
				}
				if(Math.abs(x2) == 1 && Math.abs(z2) == 1)
				{
					setBlockIfAlreadyAir(world, x+x2, y+height, z+z2, Material.SNOW.getId());
				}
				if(Math.abs(x2)+Math.abs(z2) == 1)
				{
					setBlockIfAlreadyAir(world, x+x2, y+height+1, z+z2, Material.SNOW.getId());
				}
			}
		}
		setBlockIfAlreadyAirWithData(world, x, y+height+1, z, Material.LEAVES.getId(), 1);
		setBlockIfAlreadyAir(world, x, y+height+2, z, Material.SNOW.getId());
	}
	
	private void placeMushroomTree(World world, int x, int y, int z, Random rand)
	{
		boolean brownTree = rand.nextBoolean();
		if(brownTree)
		{
			int height = rand.nextInt(3)+5;
			for(int y2 = 0; y2 < height; y2++)
			{
				setBlockWithData(world, x, y+y2, z, Material.HUGE_MUSHROOM_1.getId(), 10);
			}
			for(int x2 = -2; x2 <= 2; x2++)
			{
				for(int z2 = -2; z2 <= 2; z2++)
				{
					setBlockWithData(world, x+x2, y+height, z+z2, Material.HUGE_MUSHROOM_1.getId(), 5);
				}
			}
			for(int x2 = -1; x2 <= 1; x2++)
			{
				setBlockWithData(world, x+x2, y+height, z-3, Material.HUGE_MUSHROOM_1.getId(), 2);
				setBlockWithData(world, x+x2, y+height, z+3, Material.HUGE_MUSHROOM_1.getId(), 8);
			}
			for(int z2 = -1; z2 <= 1; z2++)
			{
				setBlockWithData(world, x-3, y+height, z+z2, Material.HUGE_MUSHROOM_1.getId(), 4);
				setBlockWithData(world, x+3, y+height, z+z2, Material.HUGE_MUSHROOM_1.getId(), 6);
			}
			setBlockWithData(world, x-3, y+height, z-2, Material.HUGE_MUSHROOM_1.getId(), 1);
			setBlockWithData(world, x-2, y+height, z-3, Material.HUGE_MUSHROOM_1.getId(), 1);
			setBlockWithData(world, x+3, y+height, z-2, Material.HUGE_MUSHROOM_1.getId(), 3);
			setBlockWithData(world, x+2, y+height, z-3, Material.HUGE_MUSHROOM_1.getId(), 3);
			setBlockWithData(world, x-3, y+height, z+2, Material.HUGE_MUSHROOM_1.getId(), 7);
			setBlockWithData(world, x-2, y+height, z+3, Material.HUGE_MUSHROOM_1.getId(), 7);
			setBlockWithData(world, x+3, y+height, z+2, Material.HUGE_MUSHROOM_1.getId(), 9);
			setBlockWithData(world, x+2, y+height, z+3, Material.HUGE_MUSHROOM_1.getId(), 9);
		}
		else
		{
			int height = rand.nextInt(3)+4;
			for(int y2 = 0; y2 < height; y2++)
			{
				setBlockWithData(world, x, y+y2, z, Material.HUGE_MUSHROOM_2.getId(), 10);
			}
			setBlockWithData(world, x, y+height, z, Material.HUGE_MUSHROOM_2.getId(), 5);
			
			setBlockWithData(world, x, y+height, z-1, Material.HUGE_MUSHROOM_2.getId(), 2);
			setBlockWithData(world, x-1, y+height, z, Material.HUGE_MUSHROOM_2.getId(), 4);
			setBlockWithData(world, x+1, y+height, z, Material.HUGE_MUSHROOM_2.getId(), 6);
			setBlockWithData(world, x, y+height, z+1, Material.HUGE_MUSHROOM_2.getId(), 8);
			
			for(int y2 = height-3; y2 <= height-1; y2++)
			{
				setBlockWithData(world, x, y+y2, z-2, Material.HUGE_MUSHROOM_2.getId(), 2);
				setBlockWithData(world, x-2, y+y2, z, Material.HUGE_MUSHROOM_2.getId(), 4);
				setBlockWithData(world, x+2, y+y2, z, Material.HUGE_MUSHROOM_2.getId(), 6);
				setBlockWithData(world, x, y+y2, z+2, Material.HUGE_MUSHROOM_2.getId(), 8);
				
				setBlockWithData(world, x-2, y+y2, z-1, Material.HUGE_MUSHROOM_2.getId(), 1);
				setBlockWithData(world, x-1, y+y2, z-2, Material.HUGE_MUSHROOM_2.getId(), 1);
				setBlockWithData(world, x+2, y+y2, z-1, Material.HUGE_MUSHROOM_2.getId(), 3);
				setBlockWithData(world, x+1, y+y2, z-2, Material.HUGE_MUSHROOM_2.getId(), 3);
				setBlockWithData(world, x-2, y+y2, z+1, Material.HUGE_MUSHROOM_2.getId(), 7);
				setBlockWithData(world, x-1, y+y2, z+2, Material.HUGE_MUSHROOM_2.getId(), 7);
				setBlockWithData(world, x+2, y+y2, z+1, Material.HUGE_MUSHROOM_2.getId(), 9);
				setBlockWithData(world, x+1, y+y2, z+2, Material.HUGE_MUSHROOM_2.getId(), 9);
			}
			
			setBlockWithData(world, x-1, y+height, z-1, Material.HUGE_MUSHROOM_2.getId(), 1);
			setBlockWithData(world, x+1, y+height, z+1, Material.HUGE_MUSHROOM_2.getId(), 9);
			setBlockWithData(world, x+1, y+height, z-1, Material.HUGE_MUSHROOM_2.getId(), 3);
			setBlockWithData(world, x-1, y+height, z+1, Material.HUGE_MUSHROOM_2.getId(), 7);
		}
	}
	
	private void placePool(World world, int poolX, int poolY, int poolZ, Random rand)
	{
		boolean lava = rand.nextInt(10) < 3;
		int size = rand.nextInt(21)+10;
		int[][] tileData = genIslandData(size, rand);
		int startX = poolX - size/2;
		int startY = poolY;
		int startZ = poolZ - size/2;
		for(int x = 0; x < size; x++)
		{
			for(int z = 0; z < size; z++)
			{
				if(tileData[x][z] > 10)
				{
					int upAmount = (int) (tileData[x][z]/35)+3;
					int downAmount = (int) (tileData[x][z]/55)+1;
					for(int y = -downAmount; y <= 0; y++)
					{
						setFluidAndNeighbourStone(world, startX+x, startY+y, startZ+z, lava);
					}
					for(int y = 1; y <= upAmount; y++)
					{
						setBlock(world, startX+x, startY+y, startZ+z, 0);
					}
				}
			}
		}
	}
	
	private void gravelBlob(World world, int poolX, int poolY, int poolZ, Random rand)
	{
		int size = rand.nextInt(11)+5;
		int[][] tileData = genIslandData(size, rand);
		int startX = poolX - size/2;
		int startY = poolY;
		int startZ = poolZ - size/2;
		for(int x = 0; x < size; x++)
		{
			for(int z = 0; z < size; z++)
			{
				if(tileData[x][z] > 10)
				{
					int upAmount = (int) (tileData[x][z]/35);
					int downAmount = (int) (tileData[x][z]/55)+1;
					for(int y = -downAmount; y <= upAmount; y++)
					{
						setBlock(world, startX+x, startY+y, startZ+z, Material.GRAVEL.getId());
					}
				}
			}
		}
	}
	
	private void generateVein(World world, int x, int y, int z, int id, int size, Random rand)
	{
		int placed = 0;
		ArrayList<int[]> targets = new ArrayList<int[]>();
		targets.add(new int[]{x,y,z});
		while(targets.size() > 0 && placed < size)
		{
			int[] target = targets.remove(rand.nextInt(targets.size()));
			if(setBlockIfAcceptsOre(world, target[0], target[1], target[2], id))
			{
				targets.add(new int[]{target[0]+1,target[1],target[2]});
				targets.add(new int[]{target[0]-1,target[1],target[2]});
				targets.add(new int[]{target[0],target[1]+1,target[2]});
				targets.add(new int[]{target[0],target[1]-1,target[2]});
				targets.add(new int[]{target[0],target[1],target[2]+1});
				targets.add(new int[]{target[0],target[1],target[2]-1});
				placed++;
			}
		}
	}
	
	@Override
	public void populate(World world, Random rand, Chunk chunk)
	{
		if((chunk.getX()%12 != 0 || chunk.getZ()%12 != 0) &&
		   (Math.abs(chunk.getX())%12 != 6 || Math.abs(chunk.getZ())%12 != 6)) return;
		chunksToReload = new ArrayList<net.minecraft.server.v1_6_R2.Chunk>();
		boolean sandEdges = rand.nextInt(10) < 3;
		boolean gravel = false;
		if(sandEdges)
		{
			gravel = rand.nextInt(12) < 3;
		}
		boolean flatIsland = rand.nextInt(12) < 5;
		Biome islandType;
		int randVal = rand.nextInt(1000);
		if(randVal < 200) islandType = Biome.PLAINS;
		else if(randVal < 400) islandType = Biome.FOREST;
		else if(randVal < 550) islandType = Biome.TAIGA;
		else if(randVal < 700) islandType = Biome.SWAMPLAND;
		else if(randVal < 850) islandType = Biome.JUNGLE;
		else if(randVal < 900) islandType = Biome.DESERT;
		else if(randVal < 993) islandType = Biome.EXTREME_HILLS;
		else islandType = Biome.MUSHROOM_ISLAND;
		int size = rand.nextInt(221)+40;
		float heightMult = rand.nextFloat()/2.0f+0.75f;
		int[][] tileData = genIslandData(size, rand);
		int startX = chunk.getX()*16+8 - size/2;
		int startY = 150+rand.nextInt(61);
		int startZ = chunk.getZ()*16+8 - size/2;
		for(int x = 0; x < size; x++)
		{
			for(int z = 0; z < size; z++)
			{
				if(tileData[x][z] > 10)
				{
					world.setBiome(startX+x, startZ+z, islandType);
					//int upAmount = (int)(tileData[x][z]*size*heightMult/(flatIsland?12000:2000));
					int upAmount = (int)((tileData[x][z]-(tileData[x-1][z]+tileData[x+1][z]+tileData[x][z-1]+tileData[x][z+1])/16)*size*heightMult/(flatIsland?12000:2000));
					if(x == size/2 && z == size/2)
					{
						if(world.getSpawnLocation().getBlockY() < 80)
						{
							world.setSpawnLocation(startX+x, startY+upAmount+4, startZ+z);
						}
					}
					int total = 0;
					for(int x2 = -4; x2 <= 4; x2++)
					{
						for(int z2 = -4; z2 <= 4; z2++)
						{
							try
							{
								total += tileData[x+x2][z+z2];
							}
							catch(IndexOutOfBoundsException e){}
						}
					}
					total /= 49;
					int downAmount = (int) (total*size*heightMult/3000+1);
					int thickness = downAmount+upAmount+1;
					for(int y = -downAmount; y <= upAmount; y++)
					{
						int blockX = startX+x, blockY = startY+y, blockZ = startZ+z;
						int distFromTop = upAmount-y;
						int distFromBottom = downAmount+y;
						if(islandType == Biome.EXTREME_HILLS)
						{
							if(sandEdges)
							{
								if(thickness < 7 && distFromTop < 3-(thickness-3)/2)
								{
									setBlock(world, blockX, blockY, blockZ, Material.SOUL_SAND.getId());
								}
								else if(distFromTop == 0 && rand.nextInt(50) == 15)
								{
									setBlock(world, blockX, blockY, blockZ, Material.MOB_SPAWNER.getId());
									((CreatureSpawner)world.getBlockAt(blockX, blockY, blockZ).getState()).setCreatureType(CreatureType.PIG_ZOMBIE);
								}
								else setBlock(world, blockX, blockY, blockZ, Material.NETHERRACK.getId());
								if(distFromTop == 0 && rand.nextInt(10000) == 151)
								{
									if(rand.nextBoolean())
									{
										for(int x2 = -2; x2 < 2; x2++)
										{
											for(int y2 = 1; y2 < 6; y2++)
											{
												if(x2 == -2 || x2 == 1 || y2 == 1 || y2 == 5) setBlock(world, blockX+x2, blockY+y2, blockZ, Material.OBSIDIAN.getId());
											}
										}
									}
									else
									{
										for(int z2 = -2; z2 < 2; z2++)
										{
											for(int y2 = 1; y2 < 6; y2++)
											{
												if(z2 == -2 || z2 == 1 || y2 == 1 || y2 == 5) setBlock(world, blockX, blockY+y2, blockZ+z2, Material.OBSIDIAN.getId());
											}
										}
									}
								}
							}
							else if(distFromTop == 0 && rand.nextInt(50) == 15)
							{
								setBlock(world, blockX, blockY, blockZ, Material.MOB_SPAWNER.getId());
								((CreatureSpawner)world.getBlockAt(blockX, blockY, blockZ).getState()).setCreatureType(CreatureType.PIG_ZOMBIE);
							}
							else setBlock(world, blockX, blockY, blockZ, Material.NETHERRACK.getId());
						}
						else if(islandType == Biome.PLAINS || islandType == Biome.FOREST)
						{
							if(sandEdges && thickness < 7 && distFromTop < 3-(thickness-3)/2)
							{
								if(distFromBottom == 0) setBlock(world, blockX, blockY, blockZ, gravel?Material.COBBLESTONE.getId():Material.SANDSTONE.getId());
								else setBlock(world, blockX, blockY, blockZ, gravel?Material.GRAVEL.getId():Material.SAND.getId());
							}
							else if(distFromTop == 0)
							{
								setBlock(world, blockX, blockY, blockZ, Material.GRASS.getId());
								if(islandType == Biome.FOREST && rand.nextInt(140) == 37) placeBasicTree(world, blockX, blockY+1, blockZ, rand);
							}
							else if(distFromTop < 4) setBlock(world, blockX, blockY, blockZ, Material.DIRT.getId());
							else setBlock(world, blockX, blockY, blockZ, Material.STONE.getId());
						}
						else if(islandType == Biome.TAIGA)
						{
							if(sandEdges && thickness < 7 && distFromTop < 3-(thickness-3)/2)
							{
								if(distFromBottom == 0) setBlock(world, blockX, blockY, blockZ, gravel?Material.COBBLESTONE.getId():Material.SANDSTONE.getId());
								else setBlock(world, blockX, blockY, blockZ, gravel?Material.GRAVEL.getId():Material.SAND.getId());
							}
							else if(distFromTop == 0)
							{
								setBlock(world, blockX, blockY, blockZ, Material.GRASS.getId());
								if(rand.nextInt(120) == 37) placeRedwoodTree(world, blockX, blockY+1, blockZ, rand);
								else setBlockIfAlreadyAir(world, blockX, blockY+1, blockZ, Material.SNOW.getId());
							}
							else if(distFromTop < 4) setBlock(world, blockX, blockY, blockZ, Material.DIRT.getId());
							else setBlock(world, blockX, blockY, blockZ, Material.STONE.getId());
						}
						else if(islandType == Biome.DESERT) //desert
						{
							if(distFromBottom > 2)
							{
								setBlock(world, blockX, blockY, blockZ, Material.SAND.getId());
								if(distFromTop == 0 && rand.nextInt(40) == 2) setBlock(world, blockX, blockY+1, blockZ, Material.CACTUS.getId());
							}
							else setBlock(world, blockX, blockY, blockZ, Material.SANDSTONE.getId());
						}
						else if(islandType == Biome.JUNGLE)
						{
							if(sandEdges && thickness < 7 && distFromTop < 3-(thickness-3)/2)
							{
								if(distFromBottom == 0) setBlock(world, blockX, blockY, blockZ, gravel?Material.COBBLESTONE.getId():Material.SANDSTONE.getId());
								else setBlock(world, blockX, blockY, blockZ, gravel?Material.GRAVEL.getId():Material.SAND.getId());
							}
							else if(distFromTop == 0)
							{
								setBlock(world, blockX, blockY, blockZ, Material.GRASS.getId());
								if(rand.nextInt(40) == 37) placeJungleTree(world, blockX, blockY+1, blockZ, rand);
							}
							else if(distFromTop < 4) setBlock(world, blockX, blockY, blockZ, Material.DIRT.getId());
							else setBlock(world, blockX, blockY, blockZ, Material.STONE.getId());
						}
						else if(islandType == Biome.SWAMPLAND)
						{
							if(sandEdges && thickness < 7 && distFromTop < 3-(thickness-3)/2)
							{
								if(distFromBottom == 0) setBlock(world, blockX, blockY, blockZ, Material.SANDSTONE.getId());
								else setBlock(world, blockX, blockY, blockZ, Material.SAND.getId());
							}
							else if(distFromTop == 0)
							{
								setBlock(world, blockX, blockY, blockZ, Material.GRASS.getId());
								if(rand.nextInt(80) == 37) placeSwampTree(world, blockX, blockY+1, blockZ, rand);
							}
							else if(distFromTop < 4) setBlock(world, blockX, blockY, blockZ, Material.DIRT.getId());
							else setBlock(world, blockX, blockY, blockZ, Material.STONE.getId());
						}
						else if(islandType == Biome.MUSHROOM_ISLAND)
						{
							if(distFromTop == 0)
							{
								setBlock(world, blockX, blockY, blockZ, Material.MYCEL.getId());
								if(rand.nextInt(250) == 51) placeMushroomTree(world, blockX, blockY+1, blockZ, rand);
								else if(rand.nextInt(40) == 12) setBlock(world, blockX, blockY+1, blockZ, rand.nextBoolean()?Material.RED_MUSHROOM.getId():Material.BROWN_MUSHROOM.getId());
							}
							else if(distFromTop < 4) setBlock(world, blockX, blockY, blockZ, Material.DIRT.getId());
							else setBlock(world, blockX, blockY, blockZ, Material.STONE.getId());
						}
						else
						{
							System.out.println("Unknown island type: "+islandType.toString());
							@SuppressWarnings("unused")
							int a = 0/0;
						}
						if(distFromTop > 4 && distFromBottom > 2)
						{
							if(rand.nextFloat() < 0.001)
							{
								generateVein(world, blockX, blockY, blockZ, Material.COAL_ORE.getId(), rand.nextInt(17)+4, rand);
							}
							if(rand.nextFloat() < 0.001)
							{
								generateVein(world, blockX, blockY, blockZ, Material.IRON_ORE.getId(), rand.nextInt(7)+4, rand);
							}
							if(rand.nextFloat() < 0.0002)
							{
								generateVein(world, blockX, blockY, blockZ, Material.GOLD_ORE.getId(), rand.nextInt(5)+3, rand);
							}
							if(rand.nextFloat() < 0.0002)
							{
								generateVein(world, blockX, blockY, blockZ, Material.REDSTONE_ORE.getId(), rand.nextInt(15)+1, rand);
							}
							if(rand.nextFloat() < 0.00005)
							{
								generateVein(world, blockX, blockY, blockZ, Material.DIAMOND_ORE.getId(), rand.nextInt(8)+1, rand);
							}
							if(rand.nextDouble() < 0.000025)
							{
								generateVein(world, blockX, blockY, blockZ, Material.EMERALD_ORE.getId(), rand.nextInt(5)+4, rand);
							}
							if(distFromTop > 12)
							{
								if(rand.nextFloat() < 0.00004)
								{
									placePool(world, blockX, blockY, blockZ, rand);
								}
								if(rand.nextFloat() < 0.0004)
								{
									gravelBlob(world, blockX, blockY, blockZ, rand);
								}
							}
							//TODO: villages n shit
						}
					}
				}
			}
		}
		while(chunksToReload.size() > 0)
		{
			net.minecraft.server.v1_6_R2.Chunk c = chunksToReload.remove(0);
			c.initLighting();
			Iterator<Player> players = world.getPlayers().iterator();
			while(players.hasNext())
			{
				sendChunkToClient(c.bukkitChunk, players.next());
			}
		}
	}
}
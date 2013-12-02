package org.tilegames.hexicube.bukkit.isle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.server.v1_6_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_6_R3.ChunkSection;
import net.minecraft.server.v1_6_R3.NBTTagCompound;
import net.minecraft.server.v1_6_R3.TileEntity;
import net.minecraft.server.v1_6_R3.TileEntityChest;
import net.minecraft.server.v1_6_R3.TileEntityMobSpawner;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_6_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_6_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_6_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

public class IslePopulator extends BlockPopulator
{
	private ArrayList<net.minecraft.server.v1_6_R3.Chunk> chunksToReload;
	private UsedSections lastUsedSections;
	
	public static ArrayList<Schematic> schematics;
	
	public static PlacableOre[] placableOres;
	
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
		int tileSize = 6+rand.nextInt(5);
		int[][] tileData = new int[size][size];
		int subsize = size*tileSize;
		int radiusSteps = Math.min(subsize, subsize)/15;
		int[][] data = new int[subsize][subsize];
		ArrayList<int[]> steps = new ArrayList<int[]>();
		steps.add(new int[]{(int)(subsize*0.5), (int)(subsize*0.5), radiusSteps*5});
		while(steps.size() > 0)
		{
			int[] step = steps.remove(0);
			if(step[2] > radiusSteps/1.3)
			{
				double mult = 0.85+rand.nextDouble()*0.25;
				mult *= 1-((double)step[2]/(double)(radiusSteps*5))/4;
				if(rand.nextInt(7) == 1) mult *= step[2]/radiusSteps;
				int stepSqrd = step[2]*step[2];
				for(int x = 0; x < step[2]; x++)
				{
					for(int y = 0; y < step[2]; y++)
					{
						double distSqrd = (step[2]*0.5-x)*(step[2]*0.5-x)+(step[2]*0.5-y)*(step[2]*0.5-y);
						if(distSqrd < stepSqrd*0.25)
						{
							double strength = (1.0-distSqrd/stepSqrd*4)*((step[2]==radiusSteps*5)?0.1:0.065)*mult;
							int xPos = (int)(x+step[0]-step[2]*0.5);
							int yPos = (int)(y+step[1]-step[2]*0.5);
							int val = data[xPos][yPos];
							val += strength*255;
							if(val > 2500) val = 2500;
							data[xPos][yPos] = val;
						}
					}
				}
				int factor = 4+rand.nextInt(4);
				for(int a = 0; a < factor; a++)
				{
					double angle = (double)rand.nextInt(360)/180*Math.PI;
					steps.add(new int[]{(int)((double)step[0]+Math.cos(angle)*(double)step[2]*0.5), (int)((double)step[1]+Math.sin(angle)*(double)step[2]*0.5), step[2]-(int)(radiusSteps*(1+rand.nextDouble()*0.2))});
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
						strength += (float)val/(float)(tileSize*tileSize);
					}
				}
				int value = (int)strength;
				tileData[x][y] = value;
			}
		}
		return tileData;
	}
	
	private int[][][] genIslandDataPair(int size, Random rand)
	{
		int tileSize = 6+rand.nextInt(5);
		int[][][] tileData = new int[2][size][size];
		int subsize = size*tileSize;
		int radiusSteps = Math.min(subsize, subsize)/15;
		int[][][] data = new int[2][subsize][subsize];
		ArrayList<int[]> steps = new ArrayList<int[]>();
		steps.add(new int[]{(int)(subsize*0.5), (int)(subsize*0.5), radiusSteps*5});
		while(steps.size() > 0)
		{
			int[] step = steps.remove(0);
			if(step[2] > radiusSteps/1.3)
			{
				double mult = 0.85+rand.nextDouble()*0.25;
				mult *= 1-((double)step[2]/(double)(radiusSteps*5))/4;
				double mult2 = 1.2+rand.nextDouble()*0.8;
				if(rand.nextInt(7) == 1) mult *= step[2]/radiusSteps;
				int stepSqrd = step[2]*step[2];
				for(int x = 0; x < step[2]; x++)
				{
					for(int y = 0; y < step[2]; y++)
					{
						double distSqrd = (step[2]*0.5-x)*(step[2]*0.5-x)+(step[2]*0.5-y)*(step[2]*0.5-y);
						if(distSqrd < stepSqrd*0.25)
						{
							double strength = (1.0-distSqrd/stepSqrd*4)*((step[2]==radiusSteps*5)?0.1:0.065)*mult;
							int xPos = (int)(x+step[0]-step[2]*0.5);
							int yPos = (int)(y+step[1]-step[2]*0.5);
							int val = data[0][xPos][yPos];
							val += strength*255;
							if(val > 2500) val = 2500;
							data[0][xPos][yPos] = val;
							strength = (1.0-distSqrd/stepSqrd*4)*((step[2]==radiusSteps*5)?0.1:0.065)*mult2;
							val = data[1][xPos][yPos];
							val += strength*255;
							if(val > 3500) val = 3500;
							data[1][xPos][yPos] = val;
						}
					}
				}
				int factor = 4+rand.nextInt(4);
				for(int a = 0; a < factor; a++)
				{
					double angle = (double)rand.nextInt(360)/180*Math.PI;
					steps.add(new int[]{(int)((double)step[0]+Math.cos(angle)*(double)step[2]*0.5), (int)((double)step[1]+Math.sin(angle)*(double)step[2]*0.5), step[2]-(int)(radiusSteps*(1+rand.nextDouble()*0.2))});
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
						int val = data[0][x*tileSize+x2][y*tileSize+y2];
						strength += (float)val/(float)(tileSize*tileSize);
					}
				}
				int value = (int)strength;
				tileData[0][x][y] = value;
				strength = 0;
				for(int x2 = 0; x2 < tileSize; x2++)
				{
					for(int y2 = 0; y2 < tileSize; y2++)
					{
						int val = data[1][x*tileSize+x2][y*tileSize+y2];
						strength += (float)val/(float)(tileSize*tileSize);
					}
				}
				value = (int)strength;
				tileData[1][x][y] = value;
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
			net.minecraft.server.v1_6_R3.Chunk chunk = ((CraftChunk)c).getHandle();
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
		try
		{
			ChunkSection chunksection = section[y >> 4];
			if(chunksection == null)
			{
				chunksection = section[y >> 4] = new ChunkSection(y >> 4 << 4, !((CraftChunk)c).getHandle().world.worldProvider.f);
			}
			return chunksection;
		}
		catch(IndexOutOfBoundsException e)
		{
			return new ChunkSection(y >> 4 << 4, !((CraftChunk)c).getHandle().world.worldProvider.f);
		}
	}
	
	public void addTileEntity(World world, TileEntity entity)
	{
		((CraftWorld)world).getHandle().setTileEntity(entity.x, entity.y, entity.z, entity);
	}
	
	public void removeTileEntity(World world, int x, int y, int z)
	{
		((CraftWorld)world).getHandle().setTileEntity(x, y, z, null);
	}
	
	public TileEntity getTileEntity(World world, int x, int y, int z)
	{
		return ((CraftWorld)world).getHandle().getTileEntity(x, y, z);
	}
	
	public int getBlock(World world, int x, int y, int z)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		return chunksection.getTypeId(x & 15, y & 15, z & 15);
	}
	
	private void setBlock(World world, int x, int y, int z, int id)
	{
		setBlockWithData(world, x, y, z, id, 0);
	}
	
	public void setBlockWithData(World world, int x, int y, int z, int id, int data)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		chunksection.setTypeId(x & 15, y & 15, z & 15, id);
		chunksection.setData(x & 15, y & 15, z & 15, data);
	}
	
	@SuppressWarnings("deprecation")
	private boolean canSetAir(World world, int x, int y, int z, boolean dungeonGen)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		if(chunksection.getTypeId(x & 15, y & 15, z & 15) == 0 ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.STONE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.COBBLESTONE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.MOSSY_COBBLESTONE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.DIRT.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.SNOW.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.NETHERRACK.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.GRASS.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.LONG_GRASS.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.YELLOW_FLOWER.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.RED_ROSE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.GRAVEL.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.SAND.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.SANDSTONE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.CACTUS.getId() ||
		   (dungeonGen && chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.WOOD.getId()) ||
		   (dungeonGen && chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.LEAVES.getId()) ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.COAL_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.IRON_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.GOLD_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.REDSTONE_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.DIAMOND_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.EMERALD_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.QUARTZ_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.GLOWSTONE.getId())
		{
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	private boolean setAirIfAllowed(World world, int x, int y, int z, boolean dungeonGen)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		if(canSetAir(world, x, y, z, dungeonGen))
		{
			chunksection.setTypeId(x & 15, y & 15, z & 15, 0);
			chunksection.setData(x & 15, y & 15, z & 15, 0);
			int id = getBlock(world, x, y+1, z);
			if(id == Material.SNOW.getId() ||
			   id == Material.YELLOW_FLOWER.getId() ||
			   id == Material.RED_ROSE.getId() ||
			   id == Material.LONG_GRASS.getId() ||
			   id == Material.RED_MUSHROOM.getId() ||
			   id == Material.BROWN_MUSHROOM.getId())
				setBlock(world, x, y+1, z, 0);
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	private void setFluid(World world, int x, int y, int z, boolean lava, boolean neighbourStone)
	{
		int fluid = lava?Material.LAVA.getId():Material.WATER.getId();
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		if(setAirIfAllowed(world, x, y, z, false))
		{
			setBlock(world, x, y, z, fluid);
			if(neighbourStone)
			{
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
		}
	}
	
	private boolean setBlockIfAlreadyAir(World world, int x, int y, int z, int id)
	{
		return setBlockIfAlreadyAirWithData(world, x, y, z, id, 0);
	}
	
	@SuppressWarnings("deprecation")
	private boolean setBlockIfAlreadyAirWithData(World world, int x, int y, int z, int id, int data)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		if(chunksection.getTypeId(x & 15, y & 15, z & 15) == 0 ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.SNOW.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.VINE.getId())
		{
			chunksection.setTypeId(x & 15, y & 15, z & 15, id);
			chunksection.setData(x & 15, y & 15, z & 15, data);
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	private boolean placeBasicTree(World world, int x, int y, int z, Random rand)
	{
		if(getBlock(world, x+2, y+3, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x+2, y+3, z-2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+3, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+3, z-2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x+2, y+6, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x+2, y+6, z-2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+6, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+6, z-2) == Material.LEAVES.getId()) return false;
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
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean placeJungleTree(World world, int x, int y, int z, Random rand)
	{
		boolean bush = rand.nextBoolean();
		if(!bush)
		{
			if(getBlock(world, x+2, y+3, z+2) == Material.LEAVES.getId()) return false;
			if(getBlock(world, x+2, y+3, z-2) == Material.LEAVES.getId()) return false;
			if(getBlock(world, x-2, y+3, z+2) == Material.LEAVES.getId()) return false;
			if(getBlock(world, x-2, y+3, z-2) == Material.LEAVES.getId()) return false;
			if(getBlock(world, x+2, y+6, z+2) == Material.LEAVES.getId()) return false;
			if(getBlock(world, x+2, y+6, z-2) == Material.LEAVES.getId()) return false;
			if(getBlock(world, x-2, y+6, z+2) == Material.LEAVES.getId()) return false;
			if(getBlock(world, x-2, y+6, z-2) == Material.LEAVES.getId()) return false;
		}
		int height = bush?1:(4 + rand.nextInt(6));
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
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean placeSwampTree(World world, int x, int y, int z, Random rand)
	{
		if(getBlock(world, x+2, y+3, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x+2, y+3, z-2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+3, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+3, z-2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x+2, y+6, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x+2, y+6, z-2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+6, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+6, z-2) == Material.LEAVES.getId()) return false;
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
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private boolean placeRedwoodTree(World world, int x, int y, int z, Random rand)
	{
		if(getBlock(world, x+2, y+3, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x+2, y+3, z-2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+3, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+3, z-2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x+2, y+6, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x+2, y+6, z-2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+6, z+2) == Material.LEAVES.getId()) return false;
		if(getBlock(world, x-2, y+6, z-2) == Material.LEAVES.getId()) return false;
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
		return true;
	}
	
	@SuppressWarnings("deprecation")
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
	
	@SuppressWarnings("deprecation")
	private void placePool(World world, int poolX, int poolY, int poolZ, boolean lava, boolean stoneEdge, Random rand)
	{
		int size = stoneEdge?(rand.nextInt(21)+10):(rand.nextInt(8)+8);
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
					int upAmount = (int) (tileData[x][z]/(stoneEdge?35:75))+(stoneEdge?3:1);
					int downAmount = (int) (tileData[x][z]/(stoneEdge?55:150))+1;
					for(int y = -downAmount; y <= 0; y++)
					{
						setFluid(world, startX+x, startY+y, startZ+z, lava, stoneEdge);
					}
					for(int y = 1; y <= upAmount; y++)
					{
						setBlock(world, startX+x, startY+y, startZ+z, 0);
						if(y == upAmount)
						{
							int id = getBlock(world, startX+x, startY+y+1, startZ+z);
							if(id == Material.SNOW.getId() ||
							   id == Material.YELLOW_FLOWER.getId() ||
							   id == Material.RED_ROSE.getId() ||
							   id == Material.LONG_GRASS.getId() ||
							   id == Material.RED_MUSHROOM.getId() ||
							   id == Material.BROWN_MUSHROOM.getId())
								setBlock(world, startX+x, startY+y+1, startZ+z, 0);
						}
					}
				}
			}
		}
	}
	
	@SuppressWarnings("deprecation")
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
						if(getBlock(world, startX+x, startY+y-1, startZ+z) != 0)
						{
							if(getTileEntity(world, startX+x, startY+y, startZ+z) == null) setBlock(world, startX+x, startY+y, startZ+z, Material.GRAVEL.getId());
						}
					}
				}
			}
		}
	}
	
	private void generateVein(World world, int x, int y, int z, PlacableOre ore, Random rand)
	{
		int placed = 0;
		int size = ore.getVeinSize(rand);
		ArrayList<int[]> targets = new ArrayList<int[]>();
		targets.add(new int[]{x, y, z});
		while(targets.size() > 0 && placed < size)
		{
			int[] target = targets.remove(rand.nextInt(targets.size()));
			if(ore.canReplace(getBlock(world, target[0], target[1], target[2])))
			{
				setBlockWithData(world, target[0], target[1], target[2], ore.getOreID(), ore.getOreData());
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
	
	private void emptyLine(World world, int[] start, int[] end)
	{
		int xDiff = Math.abs(start[0]-end[0]);
		int yDiff = Math.abs(start[1]-end[1]);
		int zDiff = Math.abs(start[2]-end[2]);
		if(xDiff > yDiff)
		{
			if(xDiff > zDiff)
			{
				int startX, endX;
				double posY, changeY, posZ, changeZ;
				if(start[0] < end[0])
				{
					startX = start[0];
					endX = end[0];
					posY = (double)start[1];
					changeY = (double)(end[1]-start[1])/(double)(end[0]-start[0]);
					posZ = (double)start[2];
					changeZ = (double)(end[2]-start[2])/(double)(end[0]-start[0]);
				}
				else
				{
					startX = end[0];
					endX = start[0];
					posY = (double)end[1];
					changeY = (double)(start[1]-end[1])/(double)(start[0]-end[0]);
					posZ = (double)end[2];
					changeZ = (double)(start[2]-end[2])/(double)(start[0]-end[0]);
				}
				for(int x2 = startX; x2 <= endX; x2++)
				{
					for(int x = x2-2; x <= x2+2; x++)
					{
						for(int z = (int)(posZ-2); z <= (int)(posZ+2); z++)
						{
							for(int y = (int)(posY-2); y <= (int)(posY+2); y++)
							{
								int count = 0;
								if(x == x2-2 || x == x2+2) count++;
								if(y == (int)(posY-2) || y == (int)(posY+2)) count++;
								if(z == (int)(posZ-2) || z == (int)(posZ+2)) count++;
								if(count < 2)
								{
									setAirIfAllowed(world, x, y, z, false);
								}
							}
						}
					}
					posY += changeY;
					posZ += changeZ;
				}
			}
			else
			{
				int startZ, endZ;
				double posX, changeX, posY, changeY;
				if(start[2] < end[2])
				{
					startZ = start[2];
					endZ = end[2];
					posX = (double)start[0];
					changeX = (double)(end[0]-start[0])/(double)(end[2]-start[2]);
					posY = (double)start[1];
					changeY = (double)(end[1]-start[1])/(double)(end[2]-start[2]);
				}
				else
				{
					startZ = end[2];
					endZ = start[2];
					posX = (double)end[0];
					changeX = (double)(start[0]-end[0])/(double)(start[2]-end[2]);
					posY = (double)end[1];
					changeY = (double)(start[1]-end[1])/(double)(start[2]-end[2]);
				}
				for(int z2 = startZ; z2 <= endZ; z2++)
				{
					for(int z = z2-2; z <= z2+2; z++)
					{
						for(int x = (int)(posX-2); x <= (int)(posX+2); x++)
						{
							for(int y = (int)(posY-2); y <= (int)(posY+2); y++)
							{
								int count = 0;
								if(x == (int)(posX-2) || x == (int)(posX+2)) count++;
								if(y == (int)(posY-2) || y == (int)(posY+2)) count++;
								if(z == z2-2 || z == z2+2) count++;
								if(count < 2)
								{
									setAirIfAllowed(world, x, y, z, false);
								}
							}
						}
					}
					posX += changeX;
					posY += changeY;
				}
			}
		}
		else
		{
			if(yDiff > zDiff)
			{
				int startY, endY;
				double posX, changeX, posZ, changeZ;
				if(start[1] < end[1])
				{
					startY = start[1];
					endY = end[1];
					posX = (double)start[0];
					changeX = (double)(end[0]-start[0])/(double)(end[1]-start[1]);
					posZ = (double)start[2];
					changeZ = (double)(end[2]-start[2])/(double)(end[1]-start[1]);
				}
				else
				{
					startY = end[1];
					endY = start[1];
					posX = (double)end[0];
					changeX = (double)(start[0]-end[0])/(double)(start[1]-end[1]);
					posZ = (double)end[2];
					changeZ = (double)(start[2]-end[2])/(double)(start[1]-end[1]);
				}
				for(int y2 = startY; y2 <= endY; y2++)
				{
					for(int x = (int)(posX-2); x <= (int)(posX+2); x++)
					{
						for(int y = y2-2; y <= y2+2; y++)
						{
							for(int z = (int)(posZ-2); z <= (int)(posZ+2); z++)
							{
								int count = 0;
								if(x == (int)(posX-2) || x == (int)(posX+2)) count++;
								if(y == y2-2 || y == y2+2) count++;
								if(z == (int)(posZ-2) || z == (int)(posZ+2)) count++;
								if(count < 2)
								{
									setAirIfAllowed(world, x, y, z, false);
								}
							}
						}
					}
					posX += changeX;
					posZ += changeZ;
				}
			}
			else
			{
				int startZ, endZ;
				double posX, changeX, posY, changeY;
				if(start[2] < end[2])
				{
					startZ = start[2];
					endZ = end[2];
					posX = (double)start[0];
					changeX = (double)(end[0]-start[0])/(double)(end[2]-start[2]);
					posY = (double)start[1];
					changeY = (double)(end[1]-start[1])/(double)(end[2]-start[2]);
				}
				else
				{
					startZ = end[2];
					endZ = start[2];
					posX = (double)end[0];
					changeX = (double)(start[0]-end[0])/(double)(start[2]-end[2]);
					posY = (double)end[1];
					changeY = (double)(start[1]-end[1])/(double)(start[2]-end[2]);
				}
				for(int z2 = startZ; z2 <= endZ; z2++)
				{
					for(int z = z2-2; z <= z2+2; z++)
					{
						for(int x = (int)(posX-2); x <= (int)(posX+2); x++)
						{
							for(int y = (int)(posY-2); y <= (int)(posY+2); y++)
							{
								int count = 0;
								if(x == (int)(posX-2) || x == (int)(posX+2)) count++;
								if(y == (int)(posY-2) || y == (int)(posY+2)) count++;
								if(z == z2-2 || z == z2+2) count++;
								if(count < 2)
								{
									setAirIfAllowed(world, x, y, z, false);
								}
							}
						}
					}
					posX += changeX;
					posY += changeY;
				}
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	private void createVillage(World world, int islandX, int islandY, int islandZ, int[][] tileData, float heightMult, Random rand)
	{
		boolean[][] spaceTaken = new boolean[tileData.length][tileData[0].length];
		if(schematics == null || schematics.size() == 0)
		{
			System.out.println("Tried to create village without loaded schematics!");
			return;
		}
		int count = 0;
		int totalWeight = 0;
		int size = schematics.size();
		for(int a = 0; a < size; a++)
		{
			totalWeight += schematics.get(a).weight;
		}
		ArrayList<int[]> positions = new ArrayList<int[]>();
		positions.add(new int[]{tileData.length/2, tileData[0].length/2});
		while(positions.size() > 0)
		{
			int[] position = positions.remove(rand.nextInt(positions.size()));
			int xPos = position[0]+islandX;
			int yPos;
			try
			{
				yPos = islandY+(int)((tileData[position[0]][position[1]]-(tileData[position[0]-1][position[1]]+tileData[position[0]+1][position[1]]+tileData[position[0]][position[1]-1]+tileData[position[0]][position[1]+1])/16)*tileData.length*heightMult/12000);
			}
			catch(IndexOutOfBoundsException e)
			{
				continue;
			}
			int zPos = position[1]+islandZ;
			int schematicWeightVal = rand.nextInt(totalWeight);
			Schematic s = null;
			for(int a = 0; a < size; a++)
			{
				s = schematics.get(a);
				schematicWeightVal -= s.weight;
				if(schematicWeightVal < 0) break;
			}
			yPos += s.offset;
			boolean found = false;
			int left = s.width/2, right = (s.width-1)/2;
			int back = s.length/2, forward = (s.length-1)/2;
			for(int x2 = position[0]-left; x2 <= position[0]+right; x2++)
			{
				for(int z2 = position[1]-back; z2 <= position[1]+forward; z2++)
				{
					try
					{
						if(spaceTaken[x2][z2]) found = true;
					}
					catch(IndexOutOfBoundsException e)
					{
						found = true;
					}
				}
			}
			int up = s.height-1;
			/*for(int y1 = -2; y1 <= 2 && !found; y1++)
			{
				if(getBlock(world, xPos, yPos+y1, zPos) == Material.WOOD.getId()) found = true;
				if(found) break;
				if(getBlock(world, xPos+right, yPos+y1, zPos+back) == Material.WOOD.getId()) found = true;
				if(found) break;
				if(getBlock(world, xPos-left, yPos+y1, zPos+back) == Material.WOOD.getId()) found = true;
				if(found) break;
				if(getBlock(world, xPos+right, yPos+y1, zPos-forward) == Material.WOOD.getId()) found = true;
				if(found) break;
				if(getBlock(world, xPos-left, yPos+y1, zPos-forward) == Material.WOOD.getId()) found = true;
			}*/
			if(found) continue;
			if(!canSetAir(world, xPos, yPos+up+2, zPos, false)) continue;
			if(!canSetAir(world, xPos+right, yPos+up+2, zPos+back, false)) continue;
			if(!canSetAir(world, xPos-left, yPos+up+2, zPos+back, false)) continue;
			if(!canSetAir(world, xPos+right, yPos+up+2, zPos-forward, false)) continue;
			if(!canSetAir(world, xPos-left, yPos+up+2, zPos-forward, false)) continue;
			if(!Material.getMaterial(getBlock(world, xPos, yPos-1, zPos)).isSolid()) continue;
			if(!Material.getMaterial(getBlock(world, xPos+right, yPos-1, zPos+back)).isSolid()) continue;
			if(!Material.getMaterial(getBlock(world, xPos-left, yPos-1, zPos+back)).isSolid()) continue;
			if(!Material.getMaterial(getBlock(world, xPos+right, yPos-1, zPos-forward)).isSolid()) continue;
			if(!Material.getMaterial(getBlock(world, xPos-left, yPos-1, zPos-forward)).isSolid()) continue;
			count++;
			for(int x2 = position[0]-left-1; x2 <= position[0]+right+1; x2++)
			{
				for(int z2 = position[1]-back-1; z2 <= position[1]+forward+1; z2++)
				{
					try
					{
						spaceTaken[x2][z2] = true;
					}
					catch(IndexOutOfBoundsException e){}
				}
			}
			for(int x2 = 0; x2 <= left+right; x2++)
			{
				for(int z2 = 0; z2 <= back+forward; z2++)
				{
					for(int y2 = 0; y2 <= up; y2++)
					{
						int dataID = s.structure[x2][y2][z2];
						if(dataID != -1)
						{
							//if(y2 > -s.offset || s.materialList[dataID] != 0)
							{
								setBlockWithData(world, xPos+x2-left, yPos+y2, zPos+z2-forward, s.materialList[dataID], s.materialListData[dataID]);
							}
						}
					}
				}
			}
			//TODO: handle path
			positions.add(new int[]{position[0]+right*2+2+rand.nextInt(4), position[1]-2-forward*2-rand.nextInt(4)});
			positions.add(new int[]{position[0]-2-left*2-rand.nextInt(4), position[1]-2-forward*2-rand.nextInt(4)});
			positions.add(new int[]{position[0]+2+right*2+rand.nextInt(4), position[1]+2+back*2+rand.nextInt(4)});
			positions.add(new int[]{position[0]-2-left*2-rand.nextInt(4), position[1]+2+back*2+rand.nextInt(4)});
			if(count >= 1000)
			{
				System.out.println("WARNING: Capped village house count at 1000! This shouldn't happen!");
				break;
			}
		}
		System.out.println("Placed a "+count+"-house village at: "+islandX+"/"+islandY+"/"+islandZ);
	}
	
	@SuppressWarnings("deprecation")
	private void generateLinkedDungeons(World world, int x, int y, int z, int count, Random rand)
	{
		int placedRooms = 0;
		ArrayList<int[]> positions = new ArrayList<int[]>();
		positions.add(new int[]{x, y, z, -1});
		while(positions.size() > 0)
		{
			int[] position = positions.remove(rand.nextInt(positions.size()));
			if(getBlock(world, position[0], position[1]+1, position[2]) == Material.MOB_SPAWNER.getId())
			{
				//if(rand.nextInt(3) == 1)
				{
					if(position[3] == 0) //link -y
					{
						for(int xPos = position[0]-1; xPos <= position[0]+1; xPos++)
						{
							for(int zPos = position[2]-1; zPos <= position[2]+1; zPos++)
							{
								setAirIfAllowed(world, xPos, position[1], zPos, true);
							}
						}
					}
					else if(position[3] == 1) //link +y
					{
						for(int xPos = position[0]-1; xPos <= position[0]+1; xPos++)
						{
							for(int zPos = position[2]-1; zPos <= position[2]+1; zPos++)
							{
								setAirIfAllowed(world, xPos, position[1]+5, zPos, true);
							}
						}
					}
					else if(position[3] == 2) //link -x
					{
						for(int yPos = position[1]+1; yPos <= position[1]+3; yPos++)
						{
							for(int zPos = position[2]-1; zPos <= position[2]+1; zPos++)
							{
								setAirIfAllowed(world, position[0]-4, yPos, zPos, true);
							}
						}
					}
					else if(position[3] == 3) //link +x
					{
						for(int yPos = position[1]+1; yPos <= position[1]+3; yPos++)
						{
							for(int zPos = position[2]-1; zPos <= position[2]+1; zPos++)
							{
								setAirIfAllowed(world, position[0]+4, yPos, zPos, true);
							}
						}
					}
					else if(position[3] == 4) //link -z
					{
						for(int yPos = position[1]+1; yPos <= position[1]+3; yPos++)
						{
							for(int xPos = position[0]-1; xPos <= position[0]+1; xPos++)
							{
								setAirIfAllowed(world, xPos, yPos, position[2]-4, true);
							}
						}
					}
					else if(position[3] == 5) //link +z
					{
						for(int yPos = position[1]+1; yPos <= position[1]+3; yPos++)
						{
							for(int xPos = position[0]-1; xPos <= position[0]+1; xPos++)
							{
								setAirIfAllowed(world, xPos, yPos, position[2]+4, true);
							}
						}
					}
				}
				continue;
			}
			if(count <= 0) continue;
			if(getBlock(world, position[0]-4, position[1], position[2]-4) == 0 ||
			   getBlock(world, position[0]-4, position[1], position[2]+4) == 0 ||
			   getBlock(world, position[0]+4, position[1], position[2]-4) == 0 ||
			   getBlock(world, position[0]+4, position[1], position[2]+4) == 0 ||
			   getBlock(world, position[0], position[1], position[2]) == 0) continue;
			count--;
			placedRooms++;
			for(int xPos = position[0]-4; xPos <= position[0]+4; xPos++)
			{
				for(int zPos = position[2]-4; zPos <= position[2]+4; zPos++)
				{
					for(int yPos = position[1]; yPos <= position[1]+5; yPos++)
					{
						if(xPos == position[0]-4 || xPos == position[0]+4 ||
						   yPos == position[1] || yPos == position[1]+5 ||
						   zPos == position[2]-4 || zPos == position[2]+4)
						{
							setAirIfAllowed(world, xPos, yPos, zPos, true);
							setBlockIfAlreadyAir(world, xPos, yPos, zPos, rand.nextBoolean()?Material.MOSSY_COBBLESTONE.getId():Material.COBBLESTONE.getId());
						}
						else setAirIfAllowed(world, xPos, yPos, zPos, true);
					}
				}
			}
			if(position[3] == 0) //link -y
			{
				for(int xPos = position[0]-1; xPos <= position[0]+1; xPos++)
				{
					for(int zPos = position[2]-1; zPos <= position[2]+1; zPos++)
					{
						setAirIfAllowed(world, xPos, position[1], zPos, true);
					}
				}
			}
			else if(position[3] == 1) //link +y
			{
				for(int xPos = position[0]-1; xPos <= position[0]+1; xPos++)
				{
					for(int zPos = position[2]-1; zPos <= position[2]+1; zPos++)
					{
						setAirIfAllowed(world, xPos, position[1]+5, zPos, true);
					}
				}
			}
			else if(position[3] == 2) //link -x
			{
				for(int yPos = position[1]+1; yPos <= position[1]+3; yPos++)
				{
					for(int zPos = position[2]-1; zPos <= position[2]+1; zPos++)
					{
						setAirIfAllowed(world, position[0]-4, yPos, zPos, true);
					}
				}
			}
			else if(position[3] == 3) //link +x
			{
				for(int yPos = position[1]+1; yPos <= position[1]+3; yPos++)
				{
					for(int zPos = position[2]-1; zPos <= position[2]+1; zPos++)
					{
						setAirIfAllowed(world, position[0]+4, yPos, zPos, true);
					}
				}
			}
			else if(position[3] == 4) //link -z
			{
				for(int yPos = position[1]+1; yPos <= position[1]+3; yPos++)
				{
					for(int xPos = position[0]-1; xPos <= position[0]+1; xPos++)
					{
						setAirIfAllowed(world, xPos, yPos, position[2]-4, true);
					}
				}
			}
			else if(position[3] == 5) //link +z
			{
				for(int yPos = position[1]+1; yPos <= position[1]+3; yPos++)
				{
					for(int xPos = position[0]-1; xPos <= position[0]+1; xPos++)
					{
						setAirIfAllowed(world, xPos, yPos, position[2]+4, true);
					}
				}
			}
			setAirIfAllowed(world, position[0], position[1]+1, position[2], true);
			if(setBlockIfAlreadyAir(world, position[0], position[1]+1, position[2], Material.MOB_SPAWNER.getId()))
			{
				TileEntityMobSpawner t = new TileEntityMobSpawner();
				t.x = position[0];
				t.y = position[1]+1;
				t.z = position[2];
				NBTTagCompound data = new NBTTagCompound();
				t.b(data);
				int val = rand.nextInt(25);
				if(val < 7) data.setString("EntityId", "Skeleton");
				else if(val < 14) data.setString("EntityId", "Zombie");
				else if(val < 21) data.setString("EntityId", "Spider");
				else data.setString("EntityId", "CaveSpider");
				data.setShort("RequiredPlayerRange", (short)24);
				t.a(data);
				addTileEntity(world, t);
			}
			int chestCount = rand.nextInt(2)+1;
			for(int a = 0; a < chestCount; a++)
			{
				int wallID = rand.nextInt(4);
				int chestX, chestZ, chestData;
				if(wallID == 0)
				{
					chestX = position[0]-3;
					chestZ = position[2]-2+rand.nextInt(5);
					chestData = 5;
				}
				else if(wallID == 1)
				{
					chestX = position[0]+3;
					chestZ = position[2]-2+rand.nextInt(5);
					chestData = 4;
				}
				else if(wallID == 2)
				{
					chestX = position[0]-2+rand.nextInt(5);
					chestZ = position[2]-3;
					chestData = 3;
				}
				else if(wallID == 3)
				{
					chestX = position[0]-2+rand.nextInt(5);
					chestZ = position[2]+3;
					chestData = 2;
				}
				else
				{
					chestX = position[0];
					chestZ = position[2]+1;
					chestData = 2;
				}
				if(getBlock(world, chestX, position[1]+1, chestZ) == 0)
				{
					setAirIfAllowed(world, chestX, position[1]+1, chestZ, true);
					if(setBlockIfAlreadyAirWithData(world, chestX, position[1]+1, chestZ, Material.CHEST.getId(), chestData))
						populateChest(world, chestX, position[1]+1, chestZ, rand);
				}
			}
			positions.add(new int[]{position[0], position[1]+5, position[2], 0});
			positions.add(new int[]{position[0], position[1]-5, position[2], 1});
			positions.add(new int[]{position[0]+8, position[1], position[2], 2});
			positions.add(new int[]{position[0]-8, position[1], position[2], 3});
			positions.add(new int[]{position[0], position[1], position[2]+8, 4});
			positions.add(new int[]{position[0], position[1], position[2]-8, 5});
		}
		System.out.println("Placed a "+placedRooms+"-room dungeon centred at: "+x+"/"+y+"/"+z);
	}
	
	private void populateChest(World world, int x, int y, int z, Random rand)
	{
		TileEntity t = ((CraftWorld)world).getTileEntityAt(x, y, z);
		if(!(t instanceof TileEntityChest))
		{
			System.out.println("Tried to fill a chest that doesn't exist!");
			return;
		}
		TileEntityChest c = (TileEntityChest)t;
		int numItems = 2+rand.nextInt(3)+rand.nextInt(4)+rand.nextInt(3);
		for(int a = 0; a < numItems; a++)
		{
			int val = rand.nextInt(1000);
			ItemStack stack = null;
			if(val < 66)
			{
				int amount = 5+rand.nextInt(16);
				stack = new ItemStack(Material.MELON_SEEDS, amount);
			}
			else if(val < 134)
			{
				int amount = 5+rand.nextInt(16);
				stack = new ItemStack(Material.PUMPKIN_SEEDS, amount);
			}
			else if(val < 200)
			{
				int amount = 5+rand.nextInt(16);
				stack = new ItemStack(Material.SUGAR_CANE, amount);
			}
			else if(val < 400)
			{
				int amount = 2+rand.nextInt(4)+rand.nextInt(4);
				stack = new ItemStack(Material.REDSTONE, amount);
			}
			else if(val < 550)
			{
				int amount = 2+rand.nextInt(3);
				stack = new ItemStack(Material.IRON_INGOT, amount);
			}
			else if(val < 650)
			{
				stack = new ItemStack(Material.SADDLE);
			}
			else if(val < 700)
			{
				int amount = 2+rand.nextInt(7);
				stack = new ItemStack(Material.BLAZE_ROD, amount);
			}
			else if(val < 800)
			{
				int amount = 1+rand.nextInt(3);
				stack = new ItemStack(Material.GOLD_INGOT, amount);
			}
			else if(val < 850)
			{
				int amount = 1+rand.nextInt(2);
				stack = new ItemStack(Material.ENDER_PEARL, amount);
			}
			else if(val < 900)
			{
				int amount = 1+rand.nextInt(2);
				stack = new ItemStack(Material.DIAMOND, amount);
			}
			else if(val < 950)
			{
				int amount = 1+rand.nextInt(10);
				stack = new ItemStack(Material.SLIME_BALL, amount);
			}
			else if(val < 960)
			{
				stack = new ItemStack(Material.ENCHANTED_BOOK);
				EnchantmentStorageMeta meta = (EnchantmentStorageMeta)stack.getItemMeta();
				Enchantment e = Enchantment.values()[rand.nextInt(Enchantment.values().length)];
				meta.addStoredEnchant(e, rand.nextInt(e.getMaxLevel())+1, false);
				stack.setItemMeta(meta);
			}
			else
			{
				Material m = null;
				int number = 3+rand.nextInt(10);
				if(number == 3) m = Material.RECORD_3;
				else if(number == 4) m = Material.RECORD_4;
				else if(number == 5) m = Material.RECORD_5;
				else if(number == 6) m = Material.RECORD_6;
				else if(number == 7) m = Material.RECORD_7;
				else if(number == 8) m = Material.RECORD_8;
				else if(number == 9) m = Material.RECORD_9;
				else if(number == 10) m = Material.RECORD_10;
				else if(number == 11) m = Material.RECORD_11;
				else if(number == 12) m = Material.RECORD_12;
				if(m == null) System.out.println("Bad record number: "+number);
				else stack = new ItemStack(m);
			}
			c.setItem(rand.nextInt(3)*9+a, CraftItemStack.asNMSCopy(stack));
		}
		addTileEntity(world, c);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void populate(World world, Random rand, Chunk chunk)
	{
		if((chunk.getX()%(IslandWorldGeneration.islandSpacing*2) != 0 || chunk.getZ()%(IslandWorldGeneration.islandSpacing*2) != 0) &&
		   (Math.abs(chunk.getX())%(IslandWorldGeneration.islandSpacing*2) != IslandWorldGeneration.islandSpacing || Math.abs(chunk.getZ())%(IslandWorldGeneration.islandSpacing*2) != IslandWorldGeneration.islandSpacing)) return;
		chunksToReload = new ArrayList<net.minecraft.server.v1_6_R3.Chunk>();
		boolean sandEdges = rand.nextInt(10) < 3;
		boolean flatIsland = rand.nextInt(17) < 5;
		boolean islandValidSpawn = false;
		Biome islandType;
		if(IslandWorldGeneration.islandTotalChance == 0) islandType = Biome.FOREST;
		else
		{
			int randVal = rand.nextInt(IslandWorldGeneration.islandTotalChance);
			int pos = 0;
			while(randVal >= IslandWorldGeneration.islandChances[pos])
			{
				randVal -= IslandWorldGeneration.islandChances[pos];
				pos++;
			}
			if(pos == 0) islandType = Biome.PLAINS;
			else if(pos == 1) islandType = Biome.FOREST;
			else if(pos == 2) islandType = Biome.TAIGA;
			else if(pos == 3) islandType = Biome.SWAMPLAND;
			else if(pos == 4) islandType = Biome.JUNGLE;
			else if(pos == 5) islandType = Biome.DESERT;
			else if(pos == 6) islandType = Biome.EXTREME_HILLS;
			else if(pos == 7) islandType = Biome.SMALL_MOUNTAINS;
			else if(pos == 8) islandType = Biome.MUSHROOM_ISLAND;
			else islandType = Biome.OCEAN;
		}
		int size = rand.nextInt(111)+70;
		float heightMult = rand.nextFloat()*0.5f+0.75f;
		if(islandType == Biome.OCEAN)
		{
			heightMult *= 1.5f;
			size *= 1.5;
		}
		int[][][] tileData = genIslandDataPair(size, rand);
		int startX = chunk.getX()*16+8 - size/2;
		int startY = IslandWorldGeneration.islandStartY+rand.nextInt(61);
		int startZ = chunk.getZ()*16+8 - size/2;
		ArrayList<int[]> points = new ArrayList<int[]>();
		ArrayList<int[]> poolPoints = new ArrayList<int[]>();
		ArrayList<int[]> gravelPoints = new ArrayList<int[]>();
		ArrayList<int[]> poolPoints2 = new ArrayList<int[]>();
		ArrayList<int[]> orePoints = new ArrayList<int[]>();
		ArrayList<int[]> clayPatches = new ArrayList<int[]>();
		ArrayList<int[]> sugarCane = new ArrayList<int[]>();
		ArrayList<int[]> cactiPoints = new ArrayList<int[]>();
		for(int x = 0; x < size; x++)
		{
			for(int z = 0; z < size; z++)
			{
				if(tileData[0][x][z] > 10 || tileData[1][x][z] > 25)
				{
					if(IslandWorldGeneration.parentGen.equals("")) world.setBiome(startX+x, startZ+z, islandType);
					if(islandType == Biome.OCEAN)
					{
						int upAmount = (int)((tileData[0][x][z]-(tileData[0][x-1][z]+tileData[0][x+1][z]+tileData[0][x][z-1]+tileData[0][x][z+1])/16));
						upAmount = (int)((Math.sin(upAmount/77.12+Math.PI/2)*81-upAmount/5-60)*size*heightMult/1000);
						int total = 0;
						for(int x2 = -4; x2 <= 4; x2++)
						{
							for(int z2 = -4; z2 <= 4; z2++)
							{
								try
								{
									total += tileData[1][x+x2][z+z2];
								}
								catch(IndexOutOfBoundsException e){}
							}
						}
						total /= 49;
						int downAmount = (int) (total*size*heightMult/3000+1);
						if(upAmount < -downAmount+3) upAmount = -downAmount+3;
						int blockX = startX+x, blockZ = startZ+z;
						for(int y = -downAmount; y <= upAmount; y++)
						{
							int distFromTop = upAmount-y;
							int distFromBottom = downAmount+y;
							int blockY = startY+y;
							{
								if(distFromBottom > 0 && distFromTop < 3) setBlock(world, blockX, blockY, blockZ, Material.SAND.getId());
								else if(distFromTop < 6) setBlock(world, blockX, blockY, blockZ, Material.SANDSTONE.getId());
								else setBlock(world, blockX, blockY, blockZ, Material.STONE.getId());
								if(distFromTop == 0 && rand.nextDouble() < 0.01*IslandWorldGeneration.rarityModifiers[10]) clayPatches.add(new int[]{blockX, blockY, blockZ});
								if(y == 0 && distFromTop == 0 && rand.nextDouble() < 0.1*IslandWorldGeneration.rarityModifiers[11]) sugarCane.add(new int[]{blockX, blockY+1, blockZ});
							}
							if(distFromTop > 4 && distFromBottom > 2)
							{
								if(distFromTop > 12)
								{
									if(rand.nextDouble() < 0.00004*IslandWorldGeneration.rarityModifiers[6])
									{
										poolPoints.add(new int[]{blockX,blockY,blockZ});
									}
									if(rand.nextDouble() < 0.0004*IslandWorldGeneration.rarityModifiers[7])
									{
										gravelPoints.add(new int[]{blockX,blockY,blockZ});
									}
								}
							}
							if(getBlock(world, blockX, blockY, blockZ) != 0)
							{
								for(int a = 0; a < placableOres.length; a++)
								{
									if(rand.nextDouble() < placableOres[a].getChance())
									{
										orePoints.add(new int[]{blockX, blockY, blockZ, a});
									}
								}
							}
						}
						for(int y = upAmount+1; y <= 0; y++)
						{
							setBlock(world, blockX, startY+y, blockZ, Material.WATER.getId());
						}
					}
					else
					{
						int upAmount = (int)((tileData[0][x][z]-(tileData[0][x-1][z]+tileData[0][x+1][z]+tileData[0][x][z-1]+tileData[0][x][z+1])/16)*size*heightMult/(flatIsland?12000:2000));
						int total = 0;
						for(int x2 = -4; x2 <= 4; x2++)
						{
							for(int z2 = -4; z2 <= 4; z2++)
							{
								try
								{
									total += tileData[1][x+x2][z+z2];
								}
								catch(IndexOutOfBoundsException e){}
							}
						}
						total /= 49;
						int downAmount = (int) (total*size*heightMult/3000+1);
						int thickness = downAmount+upAmount+1;
						int blockX = startX+x, blockZ = startZ+z;
						for(int y = -downAmount; y <= upAmount; y++)
						{
							int blockY = startY+y;
							int distFromTop = upAmount-y;
							int distFromBottom = downAmount+y;
							if(islandType != Biome.DESERT && (!flatIsland || distFromTop > 6) && rand.nextDouble() < 0.000015*IslandWorldGeneration.rarityModifiers[8])
							{
								points.add(new int[]{blockX, blockY, blockZ});
							}
							if(islandType == Biome.EXTREME_HILLS)
							{
								if(sandEdges)
								{
									if(thickness < 7 && distFromTop < 3-(thickness-3)/2)
									{
										setBlock(world, blockX, blockY, blockZ, Material.SOUL_SAND.getId());
									}
									else if(IslandWorldGeneration.pigZombieSpawners && distFromTop == 0 && rand.nextInt(50) == 15)
									{
										setBlock(world, blockX, blockY, blockZ, Material.MOB_SPAWNER.getId());
										TileEntityMobSpawner t = new TileEntityMobSpawner();
										t.x = blockX;
										t.y = blockY;
										t.z = blockZ;
										NBTTagCompound data = new NBTTagCompound();
										t.b(data);
										data.setString("EntityId", "PigZombie");
										data.setShort("RequiredPlayerRange", (short)64);
										t.a(data);
										addTileEntity(world, t);
									}
									else setBlock(world, blockX, blockY, blockZ, Material.NETHERRACK.getId());
									if(IslandWorldGeneration.netherPortals && distFromTop == 0 && rand.nextInt(10000) == 151)
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
									TileEntityMobSpawner t = new TileEntityMobSpawner();
									t.x = blockX;
									t.y = blockY;
									t.z = blockZ;
									NBTTagCompound data = new NBTTagCompound();
									t.b(data);
									data.setString("EntityId", "PigZombie");
									data.setShort("RequiredPlayerRange", (short)64);
									t.a(data);
									addTileEntity(world, t);
								}
								else setBlock(world, blockX, blockY, blockZ, Material.NETHERRACK.getId());
							}
							else if(islandType == Biome.SMALL_MOUNTAINS)
							{
								if(getBlock(world, blockX, blockY, blockZ) != Material.ENDER_PORTAL_FRAME.getId())
									setBlock(world, blockX, blockY, blockZ, Material.ENDER_STONE.getId());
								if(distFromTop == 0)
								{
									if(IslandWorldGeneration.endPortals && rand.nextInt(10000) == 151)
									{
										if(rand.nextBoolean())
										{
											for(int x2 = -2; x2 <= 2; x2++)
											{
												for(int z2 = -2; z2 <= 2; z2++)
												{
													boolean xEdge = (x2 == -2 || x2 == 2);
													boolean zEdge = (z2 == -2 || z2 == 2);
													if(xEdge && !zEdge)
													{
														setBlockWithData(world, blockX+x2, blockY, blockZ+z2, Material.ENDER_PORTAL_FRAME.getId(), ((x==2)?1:3)+(rand.nextBoolean()?4:0));
													}
													if(zEdge && !xEdge)
													{
														setBlockWithData(world, blockX+x2, blockY, blockZ+z2, Material.ENDER_PORTAL_FRAME.getId(), ((z==2)?0:2)+(rand.nextBoolean()?4:0));
													}
												}
											}
										}
									}
									else if(IslandWorldGeneration.obsidianPillars && rand.nextInt(4000) == 3117)
									{
										int towerHeight = 50+rand.nextInt(51);
										for(int x2 = -2; x2 <= 2; x2++)
										{
											for(int z2 = -2; z2 <= 2; z2++)
											{
												boolean xEdge = (x2 == -2 || x2 == 2);
												boolean zEdge = (z2 == -2 || z2 == 2);
												if(!xEdge || !zEdge)
												{
													for(int y2 = 0; y2 < towerHeight; y2++)
													{
														if(getBlock(world, blockX+x2, blockY+y2+1, blockZ+z2) != Material.ENDER_PORTAL_FRAME.getId())
															setBlock(world, blockX+x2, blockY+y2+1, blockZ+z2, Material.OBSIDIAN.getId());
													}
												}
											}
										}
										for(int x2 = -1; x2 <= 1; x2++)
										{
											for(int z2 = -1; z2 <= 1; z2++)
											{
												if(getBlock(world, blockX+x2, blockY+1, blockZ+z2) != Material.ENDER_PORTAL_FRAME.getId())
													setBlock(world, blockX+x2, blockY+1, blockZ+z2, Material.OBSIDIAN.getId());
											}
										}
									}
								}
							}
							else if(islandType == Biome.PLAINS || islandType == Biome.FOREST)
							{
								if(sandEdges && thickness < 7 && distFromTop < 3-(thickness-3)/2)
								{
									if(distFromBottom == 0) setBlock(world, blockX, blockY, blockZ, Material.SANDSTONE.getId());
									else setBlock(world, blockX, blockY, blockZ, Material.SAND.getId());
								}
								else if(distFromTop == 0)
								{
									setBlock(world, blockX, blockY, blockZ, Material.GRASS.getId());
									if(islandType == Biome.FOREST && rand.nextInt(70) == 37)
									{
										if(placeBasicTree(world, blockX, blockY+1, blockZ, rand)) islandValidSpawn = true;
									}
									else if(rand.nextDouble() < IslandWorldGeneration.grassChance)
									{
										if(rand.nextDouble() < IslandWorldGeneration.flowerChance)
										{
											if(rand.nextBoolean()) setBlockIfAlreadyAir(world, blockX, blockY+1, blockZ, Material.RED_ROSE.getId());
											else setBlockIfAlreadyAir(world, blockX, blockY+1, blockZ, Material.YELLOW_FLOWER.getId());
										}
										else setBlockIfAlreadyAirWithData(world, blockX, blockY+1, blockZ, Material.LONG_GRASS.getId(), 1);
									}
								}
								else if(distFromTop < 4) setBlock(world, blockX, blockY, blockZ, Material.DIRT.getId());
								else setBlock(world, blockX, blockY, blockZ, Material.STONE.getId());
							}
							else if(islandType == Biome.TAIGA)
							{
								if(distFromTop == 0)
								{
									setBlock(world, blockX, blockY, blockZ, Material.GRASS.getId());
									if(rand.nextInt(65) == 37)
									{
										if(placeRedwoodTree(world, blockX, blockY+1, blockZ, rand)) islandValidSpawn = true;
									}
									else if(IslandWorldGeneration.coverSnowWithGrass && rand.nextDouble() < IslandWorldGeneration.grassChance)
									{
										if(rand.nextDouble() < IslandWorldGeneration.flowerChance)
										{
											if(rand.nextBoolean()) setBlockIfAlreadyAir(world, blockX, blockY+1, blockZ, Material.RED_ROSE.getId());
											else setBlockIfAlreadyAir(world, blockX, blockY+1, blockZ, Material.YELLOW_FLOWER.getId());
										}
										else setBlockIfAlreadyAirWithData(world, blockX, blockY+1, blockZ, Material.LONG_GRASS.getId(), 1);
									}
									else setBlockIfAlreadyAir(world, blockX, blockY+1, blockZ, Material.SNOW.getId());
								}
								else if(distFromTop < 4) setBlock(world, blockX, blockY, blockZ, Material.DIRT.getId());
								else setBlock(world, blockX, blockY, blockZ, Material.STONE.getId());
							}
							else if(islandType == Biome.DESERT)
							{
								if(distFromTop < 4 && distFromBottom > 0)
								{
									setBlock(world, blockX, blockY, blockZ, Material.SAND.getId());
									if(distFromTop == 0 && rand.nextInt(40) == 2) cactiPoints.add(new int[]{blockX, blockY+1, blockZ});
								}
								else setBlock(world, blockX, blockY, blockZ, Material.SANDSTONE.getId());
							}
							else if(islandType == Biome.JUNGLE)
							{
								if(distFromTop == 0)
								{
									setBlock(world, blockX, blockY, blockZ, Material.GRASS.getId());
									if(rand.nextInt(30) == 37)
									{
										if(placeJungleTree(world, blockX, blockY+1, blockZ, rand)) islandValidSpawn = true;
									}
									else if(rand.nextDouble() < IslandWorldGeneration.grassChance)
									{
										if(rand.nextDouble() < IslandWorldGeneration.flowerChance)
										{
											if(rand.nextBoolean()) setBlockIfAlreadyAir(world, blockX, blockY+1, blockZ, Material.RED_ROSE.getId());
											else setBlockIfAlreadyAir(world, blockX, blockY+1, blockZ, Material.YELLOW_FLOWER.getId());
										}
										else setBlockIfAlreadyAirWithData(world, blockX, blockY+1, blockZ, Material.LONG_GRASS.getId(), 1);
									}
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
									if(rand.nextInt(40) == 37)
									{
										if(placeSwampTree(world, blockX, blockY+1, blockZ, rand)) islandValidSpawn = true;
									}
									else if(rand.nextDouble() < IslandWorldGeneration.grassChance)
									{
										if(rand.nextDouble() < IslandWorldGeneration.flowerChance)
										{
											if(rand.nextBoolean()) setBlockIfAlreadyAir(world, blockX, blockY+1, blockZ, Material.RED_ROSE.getId());
											else setBlockIfAlreadyAir(world, blockX, blockY+1, blockZ, Material.YELLOW_FLOWER.getId());
										}
										else setBlockIfAlreadyAirWithData(world, blockX, blockY+1, blockZ, Material.LONG_GRASS.getId(), 1);
									}
								}
								else if(distFromTop < 4) setBlock(world, blockX, blockY, blockZ, Material.DIRT.getId());
								else setBlock(world, blockX, blockY, blockZ, Material.STONE.getId());
								if(distFromTop == 2 && distFromBottom > 4 && rand.nextInt(250) == 117) poolPoints2.add(new int[]{blockX, blockY, blockZ});
							}
							else if(islandType == Biome.MUSHROOM_ISLAND)
							{
								if(distFromTop == 0)
								{
									setBlock(world, blockX, blockY, blockZ, Material.MYCEL.getId());
									if(rand.nextInt(250) == 51) placeMushroomTree(world, blockX, blockY+1, blockZ, rand);
									else if(rand.nextInt(30) == 12) setBlock(world, blockX, blockY+1, blockZ, rand.nextBoolean()?Material.RED_MUSHROOM.getId():Material.BROWN_MUSHROOM.getId());
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
								if(distFromTop > 12)
								{
									if(rand.nextDouble() < 0.00004*IslandWorldGeneration.rarityModifiers[6])
									{
										poolPoints.add(new int[]{blockX,blockY,blockZ});
									}
									if(rand.nextDouble() < 0.0004*IslandWorldGeneration.rarityModifiers[7])
									{
										gravelPoints.add(new int[]{blockX,blockY,blockZ});
									}
								}
							}
							if(getBlock(world, blockX, blockY, blockZ) != 0)
							{
								for(int a = 0; a < placableOres.length; a++)
								{
									if(rand.nextDouble() < placableOres[a].getChance())
									{
										orePoints.add(new int[]{blockX, blockY, blockZ, a});
									}
								}
							}
						}
					}
				}
			}
		}
		while(clayPatches.size() > 0)
		{
			int[] pos = clayPatches.remove(0);
			for(int x = -2; x <= 2; x++)
			{
				for(int z = -2; z <= 2; z++)
				{
					for(int y = -2; y <= 2; y++)
					{
						int count = 0;
						if(x == -2 || x == 2) count++;
						if(y == -2 || y == 2) count++;
						if(z == -2 || z == 2) count++;
						if(count < 2 && getBlock(world, pos[0]+x, pos[1]+y, pos[2]+z) == Material.SAND.getId())
							setBlock(world, pos[0]+x, pos[1]+y, pos[2]+z, Material.CLAY.getId());
					}
				}
			}
		}
		while(sugarCane.size() > 0)
		{
			int[] pos = sugarCane.remove(0);
			if(getBlock(world, pos[0], pos[1]-1, pos[2]) == Material.SAND.getId())
			{
				if(getBlock(world, pos[0]-1, pos[1]-1, pos[2]) == Material.WATER.getId() ||
				   getBlock(world, pos[0]+1, pos[1]-1, pos[2]) == Material.WATER.getId() ||
				   getBlock(world, pos[0], pos[1]-1, pos[2]-1) == Material.WATER.getId() ||
				   getBlock(world, pos[0], pos[1]-1, pos[2]+1) == Material.WATER.getId())
				{
					int height = rand.nextInt(3)+1;
					for(int a = 0; a < height; a++)
					{
						setBlockIfAlreadyAir(world, pos[0], pos[1]+a, pos[2], Material.SUGAR_CANE_BLOCK.getId());
					}
				}
			}
		}
		while(orePoints.size() > 0)
		{
			int[] point = orePoints.remove(rand.nextInt(orePoints.size()));
			generateVein(world, point[0], point[1], point[2], placableOres[point[3]], rand);
		}
		while(gravelPoints.size() > 0)
		{
			int[] point = gravelPoints.remove(0);
			gravelBlob(world, point[0], point[1], point[2], rand);
		}
		while(cactiPoints.size() > 0)
		{
			int[] point = cactiPoints.remove(0);
			int height = rand.nextInt(2)+rand.nextInt(2)+1;
			for(int a = 0; a < height; a++)
			{
				if(getBlock(world, point[0]+1, point[1]+a, point[2]) == 0 &&
				   getBlock(world, point[0]-1, point[1]+a, point[2]) == 0 &&
				   getBlock(world, point[0], point[1]+a, point[2]+1) == 0 &&
				   getBlock(world, point[0], point[1]+a, point[2]-1) == 0)
					setBlock(world, point[0], point[1]+a, point[2], Material.CACTUS.getId());
				else break;
			}
		}
		while(poolPoints.size() > 0)
		{
			int[] point = poolPoints.remove(0);
			placePool(world, point[0], point[1], point[2], rand.nextInt(10)<3, true, rand);
		}
		while(poolPoints2.size() > 0)
		{
			int[] point = poolPoints2.remove(0);
			placePool(world, point[0], point[1], point[2], false, false, rand);
		}
		while(points.size() > 1)
		{
			int[] start = points.remove(rand.nextInt(points.size()));
			int[] end = points.remove(rand.nextInt(points.size()));
			int[] mid = new int[]{(start[0]+end[0])/2+rand.nextInt(size/10),
								  (start[1]+end[1])/2+rand.nextInt(size/10),
								  (start[2]+end[2])/2+rand.nextInt(size/10)};
			int[] midstart = new int[]{(start[0]+mid[0])/2+rand.nextInt(size/20),
									   (start[1]+mid[1])/2+rand.nextInt(size/20),
									   (start[2]+mid[2])/2+rand.nextInt(size/20)};
			int[] midend = new int[]{(end[0]+mid[0])/2+rand.nextInt(size/20),
									 (end[1]+mid[1])/2+rand.nextInt(size/20),
									 (end[2]+mid[2])/2+rand.nextInt(size/20)};
			emptyLine(world, start, midstart);
			emptyLine(world, midstart, mid);
			emptyLine(world, mid, midend);
			emptyLine(world, midend, end);
			if(rand.nextBoolean()) points.add(start);
			if(rand.nextBoolean()) points.add(mid);
			if(rand.nextBoolean()) points.add(end);
		}
		if(!flatIsland && islandType != Biome.EXTREME_HILLS && islandType != Biome.MUSHROOM_ISLAND && islandType != Biome.SMALL_MOUNTAINS && islandType != Biome.OCEAN)
		{
			if(rand.nextDouble() < IslandWorldGeneration.dungeonChance) generateLinkedDungeons(world, startX+size/2, startY, startZ+size/2, (5+rand.nextInt(21))*size*size/10000, rand);
		}
		if(flatIsland && islandType == Biome.PLAINS && rand.nextInt(3) == 1)
		{
			createVillage(world, startX, startY, startZ, tileData[0], heightMult, rand);
		}
		while(chunksToReload.size() > 0)
		{
			net.minecraft.server.v1_6_R3.Chunk c = chunksToReload.remove(0);
			c.initLighting();
			Iterator<Player> players = world.getPlayers().iterator();
			while(players.hasNext())
			{
				sendChunkToClient(c.bukkitChunk, players.next());
			}
		}
		if(!IslandWorldGeneration.spawnVerified)
		{
			if(islandValidSpawn)
			{
				Location loc = world.getSpawnLocation();
				Chunk chu = world.getChunkAt(loc);
				if(!world.isChunkLoaded(chu)) world.loadChunk(chu);
				int spawnY = world.getHighestBlockYAt(loc);
				if(spawnY < loc.getBlockY()-3)
				{
					world.setSpawnLocation(startX+size/2, world.getHighestBlockYAt(startX+size/2, startZ+size/2)+1, startZ+size/2);
				}
				IslandWorldGeneration.spawnVerified = true;
			}
		}
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o == null) return false;
		return o instanceof IslePopulator;
	}
}
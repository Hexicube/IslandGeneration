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
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_6_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

@SuppressWarnings("deprecation")
public class IslePopulator extends BlockPopulator
{
	private ArrayList<net.minecraft.server.v1_6_R2.Chunk> chunksToReload;
	private UsedSections lastUsedSections;
	
	private static ArrayList<Schematic> schematics;
	
	public static void interpretSchematic(String data)
	{
		String[] data2 = data.split("=");
		if(data2.length < 4)
		{
			System.out.println("Invalid schematic: "+data);
			System.out.println("Reason: Missing data.");
			return;
		}
		String[] materials = data2[1].split(";");
		Schematic s = new Schematic();
		s.materialList = new int[materials.length];
		s.materialListData = new int[materials.length];
		for(int a = 0; a < materials.length; a++)
		{
			int id = 0, dmg = 0;
			try
			{
				String[] data3 = materials[a].split("\\.");
				id = Integer.parseInt(data3[0]);
				if(data3.length > 1) dmg = Integer.parseInt(data3[1]);
			}
			catch(NumberFormatException e)
			{
				System.out.println("Invalid schematic: "+data);
				System.out.println("Reason: Bad number for material.");
				return;
			}
			s.materialList[a] = id;
			s.materialListData[a] = dmg;
		}
		String[] data3 = data2[2].split("\\.");
		if(data3.length < 3)
		{
			System.out.println("Invalid schematic: "+data);
			System.out.println("Reason: Bad size.");
			return;
		}
		try
		{
			s.width = Integer.parseInt(data3[0]);
			s.height = Integer.parseInt(data3[1]);
			s.depth = Integer.parseInt(data3[2]);
		}
		catch(NumberFormatException e)
		{
			System.out.println("Invalid schematic: "+data);
			System.out.println("Reason: Bad size.");
			return;
		}
		if(s.width < 1 || s.height < 1 || s.depth < 1)
		{
			System.out.println("Invalid schematic: "+data);
			System.out.println("Reason: Bad size.");
			return;
		}
		s.structure = new int[s.width][s.height][s.depth];
		data3 = data2[3].split("\\.");
		if(data3.length < s.width*s.height*s.depth)
		{
			System.out.println("Invalid schematic: "+data);
			System.out.println("Reason: Bad structure data.");
			return;
		}
		int pos = 0;
		for(int x = 0; x < s.width; x++)
		{
			for(int y = 0; y < s.height; y++)
			{
				for(int z = 0; z < s.depth; z++)
				{
					try
					{
						s.structure[x][y][z] = Integer.parseInt(data3[pos++]);
					}
					catch(NumberFormatException e)
					{
						System.out.println("Invalid schematic: "+data);
						System.out.println("Reason: Bad structure data.");
						return;
					}
				}
			}
		}
		data3 = data2[0].split("\\.");
		try
		{
			s.weight = Integer.parseInt(data3[0]);
			s.offset = Integer.parseInt(data3[1]);
		}
		catch(NumberFormatException e)
		{
			System.out.println("Invalid schematic: "+data);
			System.out.println("Reason: Bad initial data.");
			return;
		}
		catch(IndexOutOfBoundsException e)
		{
			System.out.println("Invalid schematic: "+data);
			System.out.println("Reason: Bad initial data.");
			return;
		}
		if(schematics == null) schematics = new ArrayList<Schematic>();
		schematics.add(s);
	}
	
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
	
	private int getBlock(World world, int x, int y, int z)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		return chunksection.getTypeId(x & 15, y & 15, z & 15);
	}
	
	private void setBlock(World world, int x, int y, int z, int id)
	{
		setBlockWithData(world, x, y, z, id, 0);
	}
	
	private void setBlockWithData(World world, int x, int y, int z, int id, int data)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		chunksection.setTypeId(x & 15, y & 15, z & 15, id);
		chunksection.setData(x & 15, y & 15, z & 15, data);
	}
	
	private boolean setAirIfAllowed(World world, int x, int y, int z, boolean dungeonGen)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		if(chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.STONE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.COBBLESTONE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.MOSSY_COBBLESTONE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.DIRT.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.SNOW.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.NETHERRACK.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.GRASS.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.GRAVEL.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.SAND.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.CACTUS.getId() ||
		   (dungeonGen && chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.WOOD.getId()) ||
		   (dungeonGen && chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.LEAVES.getId()) ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.COAL_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.IRON_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.GOLD_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.REDSTONE_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.DIAMOND_ORE.getId() ||
		   chunksection.getTypeId(x & 15, y & 15, z & 15) == Material.EMERALD_ORE.getId())
		{
			chunksection.setTypeId(x & 15, y & 15, z & 15, 0);
			chunksection.setData(x & 15, y & 15, z & 15, 0);
			return true;
		}
		return false;
	}
	
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
	
	private boolean setBlockIfAcceptsOre(World world, int x, int y, int z, int id)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		int oldid = chunksection.getTypeId(x & 15, y & 15, z & 15);
		if(oldid == Material.STONE.getId() ||
		   oldid == Material.NETHERRACK.getId() ||
		   oldid == Material.SAND.getId() ||
		   oldid == Material.ENDER_STONE.getId())
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
	
	private void createVillage(World world, int islandX, int islandY, int islandZ, int[][] tileData, float heightMult, Random rand)
	{
		if(schematics == null)
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
				yPos = islandY+(int)((tileData[position[0]][position[1]]-(tileData[position[0]-1][position[1]]+tileData[position[0]+1][position[1]]+tileData[position[0]][position[1]-1]+tileData[position[0]][position[1]+1])/16)*tileData.length*heightMult/12000);;
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
			int back = s.depth/2, forward = (s.depth-1)/2;
			int up = s.height-1;
			for(int y1 = -2; y1 <= 2 && !found; y1++)
			{
				if(getBlock(world, xPos, yPos+y1, zPos) == Material.WOOD.getId()) found = true;
				if(getBlock(world, xPos+right, yPos+y1, zPos+back) == Material.WOOD.getId()) found = true;
				if(getBlock(world, xPos-left, yPos+y1, zPos+back) == Material.WOOD.getId()) found = true;
				if(getBlock(world, xPos+right, yPos+y1, zPos-forward) == Material.WOOD.getId()) found = true;
				if(getBlock(world, xPos-left, yPos+y1, zPos-forward) == Material.WOOD.getId()) found = true;
			}
			if(found) continue;
			if(getBlock(world, xPos, yPos+up+2, zPos) != 0) continue;
			if(getBlock(world, xPos+right, yPos+up+2, zPos+back) != 0) continue;
			if(getBlock(world, xPos-left, yPos+up+2, zPos+back) != 0) continue;
			if(getBlock(world, xPos+right, yPos+up+2, zPos-forward) != 0) continue;
			if(getBlock(world, xPos-left, yPos+up+2, zPos-forward) != 0) continue;
			if(getBlock(world, xPos, yPos-1, zPos) == 0) continue;
			if(getBlock(world, xPos+right, yPos-1, zPos+back) == 0) continue;
			if(getBlock(world, xPos-left, yPos-1, zPos+back) == 0) continue;
			if(getBlock(world, xPos+right, yPos-1, zPos-forward) == 0) continue;
			if(getBlock(world, xPos-left, yPos-1, zPos-forward) == 0) continue;
			count++;
			for(int x2 = 0; x2 <= left+right; x2++)
			{
				for(int z2 = 0; z2 <= back+forward; z2++)
				{
					for(int y2 = 0; y2 <= up; y2++)
					{
						int dataID = s.structure[x2][y2][z2];
						if(dataID != -1) setBlockWithData(world, xPos+x2-left, yPos+y2, zPos+z2-forward, s.materialList[dataID], s.materialListData[dataID]);
					}
				}
			}
			positions.add(new int[]{position[0]+right+2+rand.nextInt(4), position[1]-2-forward-rand.nextInt(4)});
			positions.add(new int[]{position[0]-2-left-rand.nextInt(4), position[1]-2-forward-rand.nextInt(4)});
			positions.add(new int[]{position[0]+2+right+rand.nextInt(4), position[1]+2+back+rand.nextInt(4)});
			positions.add(new int[]{position[0]-2-left-rand.nextInt(4), position[1]+2+back+rand.nextInt(4)});
		}
		System.out.println("Placed a "+count+"-house village at: "+islandX+"/"+islandY+"/"+islandZ);
	}
	
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
				CreatureType mobType = null;
				int val = rand.nextInt(25);
				if(val < 7) mobType = CreatureType.SKELETON;
				else if(val < 14) mobType = CreatureType.ZOMBIE;
				else if(val < 21) mobType = CreatureType.SPIDER;
				else mobType = CreatureType.CAVE_SPIDER;
				((CreatureSpawner)world.getBlockAt(position[0], position[1]+1, position[2]).getState()).setCreatureType(mobType);
			}
			int chestCount = rand.nextInt(4);
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
		Chest c = (Chest)world.getBlockAt(x, y, z).getState();
		Inventory v = c.getInventory();
		int numItems = 2+rand.nextInt(3)+rand.nextInt(4)+rand.nextInt(3);
		for(int a = 0; a < numItems; a++)
		{
			int val = rand.nextInt(1000);
			ItemStack stack = null;
			if(val < 200)
			{
				int amount = 10+rand.nextInt(55);
				stack = new ItemStack(Material.DIRT, amount);
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
			v.setItem(rand.nextInt(3)*9+a, stack);
		}
	}
	
	@Override
	public void populate(World world, Random rand, Chunk chunk)
	{
		if((chunk.getX()%(IslandWorldGeneration.islandSpacing*2) != 0 || chunk.getZ()%(IslandWorldGeneration.islandSpacing*2) != 0) &&
		   (Math.abs(chunk.getX())%(IslandWorldGeneration.islandSpacing*2) != IslandWorldGeneration.islandSpacing || Math.abs(chunk.getZ())%(IslandWorldGeneration.islandSpacing*2) != IslandWorldGeneration.islandSpacing)) return;
		chunksToReload = new ArrayList<net.minecraft.server.v1_6_R2.Chunk>();
		boolean sandEdges = rand.nextInt(10) < 3;
		boolean flatIsland = rand.nextInt(17) < 5;
		Biome islandType;
		int randVal = rand.nextInt(1000);
		if(randVal < 200) islandType = Biome.PLAINS;
		else if(randVal < 400) islandType = Biome.FOREST;
		else if(randVal < 550) islandType = Biome.TAIGA;
		else if(randVal < 700) islandType = Biome.SWAMPLAND;
		else if(randVal < 850) islandType = Biome.JUNGLE;
		else if(randVal < 900) islandType = Biome.DESERT;
		else if(randVal < 946) islandType = Biome.SMALL_MOUNTAINS;
		else if(randVal < 993) islandType = Biome.EXTREME_HILLS;
		else islandType = Biome.MUSHROOM_ISLAND;
		int size = rand.nextInt(111)+70;
		float heightMult = rand.nextFloat()/2.0f+0.75f;
		int[][] tileData = genIslandData(size, rand);
		int startX = chunk.getX()*16+8 - size/2;
		int startY = IslandWorldGeneration.islandStartY+rand.nextInt(61);
		int startZ = chunk.getZ()*16+8 - size/2;
		ArrayList<int[]> points = new ArrayList<int[]>();
		ArrayList<int[]> poolPoints = new ArrayList<int[]>();
		ArrayList<int[]> gravelPoints = new ArrayList<int[]>();
		ArrayList<int[]> poolPoints2 = new ArrayList<int[]>();
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
						if(world.getSpawnLocation().getY()%1 == 0)
						{
							world.setSpawnLocation(startX+x, startY+upAmount+4, startZ+z);
							world.getSpawnLocation().add(0.0, 0.1, 0.0);
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
						else if(islandType == Biome.SMALL_MOUNTAINS)
						{
							if(getBlock(world, blockX, blockY, blockZ) != Material.ENDER_PORTAL_FRAME.getId())
								setBlock(world, blockX, blockY, blockZ, Material.ENDER_STONE.getId());
							if(distFromTop == 0)
							{
								if(rand.nextInt(10000) == 151)
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
								else if(rand.nextInt(4000) == 3117)
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
								if(islandType == Biome.FOREST && rand.nextInt(140) == 37) placeBasicTree(world, blockX, blockY+1, blockZ, rand);
							}
							else if(distFromTop < 4) setBlock(world, blockX, blockY, blockZ, Material.DIRT.getId());
							else setBlock(world, blockX, blockY, blockZ, Material.STONE.getId());
						}
						else if(islandType == Biome.TAIGA)
						{
							if(distFromTop == 0)
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
							if(distFromTop == 0)
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
							if(distFromTop == 2 && distFromBottom > 4 && rand.nextInt(250) == 117) poolPoints2.add(new int[]{blockX, blockY, blockZ});
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
							if(rand.nextDouble() < 0.001*IslandWorldGeneration.rarityModifiers[0])
							{
								generateVein(world, blockX, blockY, blockZ, Material.COAL_ORE.getId(), rand.nextInt(17)+4, rand);
							}
							if(rand.nextDouble() < 0.001*IslandWorldGeneration.rarityModifiers[1])
							{
								generateVein(world, blockX, blockY, blockZ, Material.IRON_ORE.getId(), rand.nextInt(7)+4, rand);
							}
							if(rand.nextDouble() < 0.0002*IslandWorldGeneration.rarityModifiers[2])
							{
								generateVein(world, blockX, blockY, blockZ, Material.GOLD_ORE.getId(), rand.nextInt(5)+3, rand);
							}
							if(rand.nextDouble() < 0.0002*IslandWorldGeneration.rarityModifiers[3])
							{
								generateVein(world, blockX, blockY, blockZ, Material.REDSTONE_ORE.getId(), rand.nextInt(15)+1, rand);
							}
							if(rand.nextDouble() < 0.00005*IslandWorldGeneration.rarityModifiers[4])
							{
								generateVein(world, blockX, blockY, blockZ, Material.DIAMOND_ORE.getId(), rand.nextInt(8)+1, rand);
							}
							if(rand.nextDouble() < 0.000025*IslandWorldGeneration.rarityModifiers[5])
							{
								generateVein(world, blockX, blockY, blockZ, Material.EMERALD_ORE.getId(), rand.nextInt(5)+4, rand);
							}
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
					}
				}
			}
		}
		while(gravelPoints.size() > 0)
		{
			int[] point = gravelPoints.remove(0);
			gravelBlob(world, point[0], point[1], point[2], rand);
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
		if(!flatIsland && islandType != Biome.EXTREME_HILLS && islandType != Biome.MUSHROOM_ISLAND)
		{
			if(rand.nextDouble() < IslandWorldGeneration.dungeonChance) generateLinkedDungeons(world, startX+size/2, startY, startZ+size/2, (5+rand.nextInt(21))*size*size/10000, rand);
		}
		if(flatIsland && islandType == Biome.PLAINS && rand.nextInt(3) == 1)
		{
			createVillage(world, startX, startY, startZ, tileData, heightMult, rand);
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
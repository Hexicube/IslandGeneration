package org.tilegames.hexicube.bukkit.isle;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.generator.ChunkGenerator;
import org.mcstats.Metrics;

public final class IslandWorldGeneration extends JavaPlugin implements Listener
{
	public static int islandSpacing, islandTotalChance,
					  minIslandHeight, maxIslandHeight,
					  maxDungeonSize, dungeonMinChests,
					  dungeonMaxChests, minIslandSize,
					  maxIslandSize;
	public static double[] rarityModifiers;
	public static int[] islandChances;
	public static double dungeonChance, grassChance, flowerChance,
						 islandHeightScalar, islandUnderbellyScalar,
						 dungeonExtraDoorChance;
	public static boolean coverSnowWithGrass,
						  pigZombieSpawners,
						  endPortals, netherPortals,
						  obsidianPillars,
						  randomSpawnPoint;
	
	public static int waterLevel = 0, waterBlock = 0; //TODO: improve
	
	
	public static boolean enabled;
	
	public static boolean spawnVerified = false;
	
	private static int taskID, taskRepeatTimer;
	
	public static String parentGen;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onEnable()
	{
		enabled = true;
		islandSpacing = getConfig().getInt("island.spacing", 8);
		getConfig().set("island.spacing", islandSpacing);
		islandHeightScalar = getConfig().getDouble("island.heightscale", 1);
		getConfig().set("island.heightscale", islandHeightScalar);
		islandUnderbellyScalar = getConfig().getDouble("island.underbellyscale", 1);
		getConfig().set("island.underbellyscale", islandUnderbellyScalar);
		if(getConfig().isSet("island.height") && getConfig().isInt("island.height"))
		{
			int islandStartY = getConfig().getInt("island.height", 150);
			minIslandHeight = islandStartY;
			maxIslandHeight = islandStartY+60;
		}
		else
		{
			minIslandHeight = getConfig().getInt("island.height.low", 150);
			maxIslandHeight = getConfig().getInt("island.height.high", 210);
		}
		getConfig().set("island.height.low", minIslandHeight);
		getConfig().set("island.height.high", maxIslandHeight);
		minIslandSize = getConfig().getInt("island.size.min", 70);
		getConfig().set("island.size.min", minIslandSize);
		maxIslandSize = getConfig().getInt("island.size.max", 180);
		getConfig().set("island.size.max", maxIslandSize);
		rarityModifiers = new double[14];
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
		rarityModifiers[8] = getConfig().getDouble("rarity.caves", 1);
		getConfig().set("rarity.caves", rarityModifiers[8]);
		rarityModifiers[9] = getConfig().getDouble("rarity.quartzore", 1);
		getConfig().set("rarity.quartzore", rarityModifiers[9]);
		rarityModifiers[10] = getConfig().getDouble("rarity.clay", 1);
		getConfig().set("rarity.clay", rarityModifiers[10]);
		rarityModifiers[11] = getConfig().getDouble("rarity.sugarcane", 1);
		getConfig().set("rarity.sugarcane", rarityModifiers[11]);
		rarityModifiers[12] = getConfig().getDouble("rarity.glowstone", 1);
		getConfig().set("rarity.glowstone", rarityModifiers[12]);
		rarityModifiers[13] = getConfig().getDouble("rarity.lapisore", 1);
		getConfig().set("rarity.lapisore", rarityModifiers[13]);
		
		dungeonChance = getConfig().getDouble("dungeonchance", 0.02);
		getConfig().set("dungeonchance", dungeonChance);
		
		islandChances = new int[10];
		islandChances[0] = getConfig().getInt("islandchance.plains", 4);
		getConfig().set("islandchance.plains", islandChances[0]);
		islandChances[1] = getConfig().getInt("islandchance.forest", 8);
		getConfig().set("islandchance.forest", islandChances[1]);
		islandChances[2] = getConfig().getInt("islandchance.taiga", 6);
		getConfig().set("islandchance.taiga", islandChances[2]);
		islandChances[3] = getConfig().getInt("islandchance.swamp", 3);
		getConfig().set("islandchance.swamp", islandChances[3]);
		islandChances[4] = getConfig().getInt("islandchance.jungle", 4);
		getConfig().set("islandchance.jungle", islandChances[4]);
		islandChances[5] = getConfig().getInt("islandchance.desert", 4);
		getConfig().set("islandchance.desert", islandChances[5]);
		islandChances[6] = getConfig().getInt("islandchance.nether", 1);
		getConfig().set("islandchance.nether", islandChances[6]);
		islandChances[7] = getConfig().getInt("islandchance.ender", 1);
		getConfig().set("islandchance.ender", islandChances[7]);
		islandChances[8] = getConfig().getInt("islandchance.mushroom", 1);
		getConfig().set("islandchance.mushroom", islandChances[8]);
		islandChances[9] = getConfig().getInt("islandchance.lake", 4);
		getConfig().set("islandchance.lake", islandChances[9]);
		islandTotalChance = 0;
		for(int a = 0; a < islandChances.length; a++)
		{
			if(islandChances[a] < 0) islandChances[a] = 0;
			islandTotalChance += islandChances[a];
		}
		
		coverSnowWithGrass = getConfig().getBoolean("rarity.grass.coversnow", false);
		getConfig().set("rarity.grass.coversnow", coverSnowWithGrass);
		grassChance = getConfig().getDouble("rarity.grass.coverchance", 0.4);
		getConfig().set("rarity.grass.coverchance", grassChance);
		flowerChance = getConfig().getDouble("rarity.grass.flowerchance", 0.15);
		getConfig().set("rarity.grass.flowerchance", flowerChance);
		
		pigZombieSpawners = getConfig().getBoolean("features.pigzombiespawners", true);
		getConfig().set("features.pigzombiespawners", pigZombieSpawners);
		endPortals = getConfig().getBoolean("features.endportals", true);
		getConfig().set("features.endportals", endPortals);
		netherPortals = getConfig().getBoolean("features.netherportals", true);
		getConfig().set("features.netherportals", netherPortals);
		obsidianPillars = getConfig().getBoolean("features.obsidiantowers", true);
		getConfig().set("features.obsidiantowers", obsidianPillars);
		
		maxDungeonSize = getConfig().getInt("dungeon.maxrooms", 0);
		getConfig().set("dungeon.maxrooms", maxDungeonSize);
		dungeonMinChests = getConfig().getInt("dungeon.chests.min", 2);
		getConfig().set("dungeon.chests.min", dungeonMinChests);
		dungeonMaxChests = getConfig().getInt("dungeon.chests.max", 3);
		getConfig().set("dungeon.chests.max", dungeonMaxChests);
		dungeonExtraDoorChance = getConfig().getDouble("dungeon.extradoorchance", 0.75);
		getConfig().set("dungeon.extradoorchance", dungeonExtraDoorChance);
		
		randomSpawnPoint = getConfig().getBoolean("random_spawn_point", false);
		getConfig().set("random_spawn_point", randomSpawnPoint);
		
		taskRepeatTimer = getConfig().getInt("minutes_between_update_checks", 15);
		getConfig().set("minutes_between_update_checks", taskRepeatTimer);
		
		parentGen = getConfig().getString("parent_generator", "");
		getConfig().set("parent_generator", parentGen);
		saveConfig();
		
		loadOres();
		loadStructures();
		loadDungeonChests();
		
		if(taskRepeatTimer > 0)
		{
			try
			{
				UpdateChecker c = new UpdateChecker(this, getDescription().getVersion());
				taskID = getServer().getScheduler().scheduleAsyncRepeatingTask(this, c, 0, 20*60*taskRepeatTimer);
			}
			catch (MalformedURLException e)
			{
				e.printStackTrace();
				getLogger().severe("Error with starting update checker, disabling!");
			}
		}
		
		try
		{
			Metrics m = new Metrics(this);
			/*Graph g = m.createGraph("Island Chances");
			g.addPlotter(new Plotter("Plains"){
				public int getValue()
				{
					return islandTotalChance==0?0:islandChances[0]*1000/islandTotalChance;
				}
			});
			g.addPlotter(new Plotter("Forest"){
				public int getValue()
				{
					return islandTotalChance==0?1000:islandChances[1]*1000/islandTotalChance;
				}
			});
			g.addPlotter(new Plotter("Taiga"){
				public int getValue()
				{
					return islandTotalChance==0?0:islandChances[2]*1000/islandTotalChance;
				}
			});
			g.addPlotter(new Plotter("Swamp"){
				public int getValue()
				{
					return islandTotalChance==0?0:islandChances[3]*1000/islandTotalChance;
				}
			});
			g.addPlotter(new Plotter("Jungle"){
				public int getValue()
				{
					return islandTotalChance==0?0:islandChances[4]*1000/islandTotalChance;
				}
			});
			g.addPlotter(new Plotter("Desert"){
				public int getValue()
				{
					return islandTotalChance==0?0:islandChances[5]*1000/islandTotalChance;
				}
			});
			g.addPlotter(new Plotter("Nether"){
				public int getValue()
				{
					return islandTotalChance==0?0:islandChances[6]*1000/islandTotalChance;
				}
			});
			g.addPlotter(new Plotter("Ender"){
				public int getValue()
				{
					return islandTotalChance==0?0:islandChances[7]*1000/islandTotalChance;
				}
			});
			g.addPlotter(new Plotter("Mushroom"){
				public int getValue()
				{
					return islandTotalChance==0?0:islandChances[8]*1000/islandTotalChance;
				}
			});
			g.addPlotter(new Plotter("Ocean"){
				public int getValue()
				{
					return islandTotalChance==0?0:islandChances[9]*1000/islandTotalChance;
				}
			});
			m.addGraph(g);
			g = m.createGraph("Island Positions");
			g.addPlotter(new Plotter("Spacing"){
				public int getValue()
				{
					return islandSpacing;
				}
			});
			g.addPlotter(new Plotter("Height"){
				public int getValue()
				{
					return islandStartY;
				}
			});
			m.addGraph(g);
			g = m.createGraph("Number of Custom Ores");
			g.addPlotter(new Plotter("Count"){
				public int getValue()
				{
					return IslePopulator.placableOres.length-9;
				}
			});
			m.addGraph(g);
			g = m.createGraph("Plugin Version");
			g.addPlotter(new Plotter(getDescription().getVersion()){
				public int getValue()
				{
					return 1;
				}
			});
			m.addGraph(g);*/
			m.start();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDisable()
	{
		if(taskRepeatTimer > 0) getServer().getScheduler().cancelTask(taskID);
		enabled = false;
	}
	
	private void loadStructures()
	{
		IslePopulator.schematics = new ArrayList<Schematic>();
		File f = new File(getDataFolder().getAbsolutePath()+File.separator+"houses");
		System.out.println(f.getAbsolutePath());
		File[] files = f.listFiles();
		if(files != null)
		{
			for(int a = 0; a < files.length; a++)
			{
				try
				{
					Schematic s = new Schematic(files[a].getName(), files[a]);
					if(s.valid) IslePopulator.schematics.add(s);
					else throw new Exception("File invalid!");
				}
				catch(Exception e)
				{
					getLogger().warning("Invalid house NBT file: "+f.getName());
					getLogger().warning(e.getMessage());
				}
			}
		}
		if(IslePopulator.schematics.size() == 0)
		{
			IslePopulator.schematics = null;
		}
	}
	
	@SuppressWarnings("deprecation")
	private void loadOres()
	{
		ArrayList<PlacableOre> oreList = new ArrayList<PlacableOre>();
		int[] generalReplacableBlocks = new int[]{Material.STONE.getId(), Material.COBBLESTONE.getId(), Material.MOSSY_COBBLESTONE.getId(), Material.SANDSTONE.getId()};
		int[] netherReplacableBlocks = new int[]{Material.NETHERRACK.getId(), 0};
		oreList.add(new PlacableOre(Material.COAL_ORE.getId(), 0, 4, 20, generalReplacableBlocks, 0.001*IslandWorldGeneration.rarityModifiers[0]));
		oreList.add(new PlacableOre(Material.IRON_ORE.getId(), 0, 4, 10, generalReplacableBlocks, 0.001*IslandWorldGeneration.rarityModifiers[1]));
		oreList.add(new PlacableOre(Material.GOLD_ORE.getId(), 0, 3, 7, generalReplacableBlocks, 0.0002*IslandWorldGeneration.rarityModifiers[2]));
		oreList.add(new PlacableOre(Material.REDSTONE_ORE.getId(), 0, 1, 15, generalReplacableBlocks, 0.0002*IslandWorldGeneration.rarityModifiers[3]));
		oreList.add(new PlacableOre(Material.DIAMOND_ORE.getId(), 0, 1, 8, generalReplacableBlocks, 0.00005*IslandWorldGeneration.rarityModifiers[4]));
		oreList.add(new PlacableOre(Material.EMERALD_ORE.getId(), 0, 4, 8, generalReplacableBlocks, 0.000025*IslandWorldGeneration.rarityModifiers[5]));
		oreList.add(new PlacableOre(Material.QUARTZ_ORE.getId(), 0, 3, 15, netherReplacableBlocks, 0.001*IslandWorldGeneration.rarityModifiers[9]));
		oreList.add(new PlacableOre(Material.GLOWSTONE.getId(), 0, 15, 70, netherReplacableBlocks, 0.0001*IslandWorldGeneration.rarityModifiers[12]));
		oreList.add(new PlacableOre(Material.LAPIS_ORE.getId(), 0, 2, 10, generalReplacableBlocks, 0.0001*IslandWorldGeneration.rarityModifiers[13]));
		try
		{
			YamlConfiguration y;
			File f = new File(getDataFolder().getAbsolutePath()+File.separator+"ores");
			System.out.println(f.getAbsolutePath());
			File[] files = f.listFiles();
			if(files != null)
			{
				for(int a = 0; a < files.length; a++)
				{
					y = new YamlConfiguration();
					try
					{
						y.load(files[a]);
						boolean debug = y.getBoolean("debug", false);
						if(debug) getLogger().info("Ore debug: "+files[a].getName());
						int oreID = getMaterialID(y.getString("id", ""));
						if(debug) getLogger().info("  Ore ID: "+oreID);
						int oreData = y.getInt("dmg", 0);
						if(debug) getLogger().info("  Ore Data: "+oreData);
						int veinMinSize = y.getInt("minsize", 1);
						int veinMaxSize = y.getInt("maxsize", veinMinSize);
						if(debug) getLogger().info("  Ore Vein Size: "+veinMinSize+"-"+veinMaxSize);
						String[] data = y.getString("replaces", "").split(",");
						int[] canReplace = new int[data.length];
						for(int b = 0; b < data.length; b++)
						{
							canReplace[b] = getMaterialID(data[b]);
							if(debug) getLogger().info("  Ore Can Replace: "+canReplace[b]);
						}
						double chance = y.getDouble("chance", 0.0);
						if(debug) getLogger().info("  Ore Chance: "+chance);
						oreList.add(new PlacableOre(oreID, oreData, veinMinSize, veinMaxSize, canReplace, chance));
					}
					catch(InvalidConfigurationException e)
					{
						getLogger().warning("Invalid ore YAML file: "+f.getName());
					}
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		int size = oreList.size();
		IslePopulator.placableOres = new PlacableOre[size];
		for(int a = 0; a < size; a++)
		{
			IslePopulator.placableOres[a] = oreList.get(a);
		}
	}
	
	@SuppressWarnings("deprecation")
	private void loadDungeonChests()
	{
		ArrayList<DungeonLootChest> chests = new ArrayList<DungeonLootChest>();
		try
		{
			YamlConfiguration y;
			File f = new File(getDataFolder().getAbsolutePath()+File.separator+"chests");
			System.out.println(f.getAbsolutePath());
			File[] files = f.listFiles();
			if(files != null)
			{
				for(int a = 0; a < files.length; a++)
				{
					y = new YamlConfiguration();
					try
					{
						y.load(files[a]);
						DungeonLootChest chest = new DungeonLootChest();
						chest.chestName = y.getString("name");
						if(chest.chestName != null && chest.chestName.length() > 0) chest.useChestName = true;
						chest.allowSameEntry = y.getBoolean("reuse_groups", true);
						chest.weight = y.getInt("weight", 0);
						if(chest.weight <= 0) continue;
						chest.minItems = y.getInt("min_items", 0);
						chest.maxItems = y.getInt("max_items", chest.minItems);
						//TODO: read data
						chest.itemGroups = new ArrayList<DungeonLootItemGroup>();
						List<Map<?,?>> list = y.getMapList("item_groups");
						for(Map<?,?> itemGroup : list)
						{
							ArrayList<DungeonLootItem> itemArray = new ArrayList<DungeonLootItem>();
							int weight = 0;
							Object o = itemGroup.get("items");
							if(o instanceof ArrayList<?>)
							{
								@SuppressWarnings("unchecked")
								ArrayList<Map<String, Object>> itemList = (ArrayList<Map<String, Object>>)itemGroup.get("items");
								for(Map<String, Object> itemInList : itemList)
								{
									int weight2 = 0, id = 0, dmg = 0, min = 0, max = 0;
									o = itemInList.get("weight");
									if(o instanceof Integer) weight2 = (Integer)o;
									o = itemInList.get("id");
									if(o instanceof Integer) id = (Integer)o;
									else if(o instanceof String) id = getMaterialID((String)o);
									o = itemInList.get("dmg");
									if(o instanceof Integer) dmg = (Integer)o;
									o = itemInList.get("minstack");
									if(o instanceof Integer) min = (Integer)o;
									o = itemInList.get("maxstack");
									if(o instanceof Integer) max = (Integer)o;
									else max = min;
									if(weight2 > 0 && id != 0 && min > 0 && max >= min)
									{
										itemArray.add(new DungeonLootItem(id, dmg, min, max, weight2));
									}
								}
							}
							o = itemGroup.get("weight");
							if(o instanceof Integer) weight = (Integer)o;
							if(weight > 0 && itemArray.size() > 0)
							{
								chest.itemGroups.add(new DungeonLootItemGroup(itemArray, weight));
								chest.groupTotalWeight += weight;
							}
						}
						if(chest.groupTotalWeight > 0)
						{
							chests.add(chest);
							IslePopulator.dungeonChestWeightSum += chest.weight;
						}
					}
					catch(InvalidConfigurationException e)
					{
						getLogger().warning("Invalid ore YAML file: "+f.getName());
					}
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		if(IslePopulator.dungeonChestWeightSum <= 0)
		{
			DungeonLootChest chest = new DungeonLootChest();
			chest.useChestName = false;
			chest.weight = 1;
			chest.allowSameEntry = true;
			chest.minItems = 2;
			chest.maxItems = 9;
			chest.itemGroups = new ArrayList<DungeonLootItemGroup>();
			ArrayList<DungeonLootItem> items = new ArrayList<DungeonLootItem>();
			items.add(new DungeonLootItem(Material.MELON_SEEDS.getId(), 0, 5, 20, 70));
			items.add(new DungeonLootItem(Material.PUMPKIN_SEEDS.getId(), 0, 5, 20, 70));
			items.add(new DungeonLootItem(Material.SUGAR_CANE.getId(), 0, 5, 20, 70));
			items.add(new DungeonLootItem(Material.REDSTONE.getId(), 0, 2, 8, 200));
			items.add(new DungeonLootItem(Material.IRON_INGOT.getId(), 0, 2, 4, 150));
			items.add(new DungeonLootItem(Material.SADDLE.getId(), 0, 1, 1, 100));
			items.add(new DungeonLootItem(Material.GOLD_INGOT.getId(), 0, 1, 3, 100));
			items.add(new DungeonLootItem(Material.BLAZE_ROD.getId(), 0, 2, 8, 50));
			items.add(new DungeonLootItem(Material.ENDER_PEARL.getId(), 0, 1, 2, 50));
			items.add(new DungeonLootItem(Material.DIAMOND.getId(), 0, 1, 2, 50));
			items.add(new DungeonLootItem(Material.SLIME_BALL.getId(), 0, 1, 10, 50));
			items.add(new DungeonLootItem(Material.ENCHANTED_BOOK.getId(), 10, 1, 1, 10));
			chest.itemGroups.add(new DungeonLootItemGroup(items, 1));
			chest.groupTotalWeight = 1;
			chests.add(chest);
			IslePopulator.dungeonChestWeightSum = 1;
		}
		IslePopulator.dungeonChests = chests.toArray(new DungeonLootChest[0]);
	}
	
	@Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
	{
		if(!enabled) getServer().getPluginManager().enablePlugin(this);
		if(parentGen.equals("")) return new ChunkGen();
		Plugin p = getServer().getPluginManager().getPlugin(parentGen);
		if(p == null)
		{
			getLogger().warning("Plugin does not exist for extended world gen: "+parentGen);
			return new ChunkGen();
		}
		if(!p.isEnabled()) getServer().getPluginManager().enablePlugin(p);
		return new ChunkGen2(WorldCreator.getGeneratorForName(worldName, parentGen+":"+id, getServer().getConsoleSender()));
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if(cmd.getName().equalsIgnoreCase("islegen"))
		{
			if(args.length > 0)
			{
				if(args[0].equalsIgnoreCase("data"))
				{
					sender.sendMessage("[IsleWorldGen] Island height range: "+minIslandHeight+"-"+maxIslandHeight);
					sender.sendMessage("[IsleWorldGen] Island spacing: "+islandSpacing);
					sender.sendMessage("[IsleWorldGen] Minutes between update checks: "+((taskRepeatTimer>0)?taskRepeatTimer:"Disabled"));
					sender.sendMessage("[IsleWorldGen] Type \"/islegen chancemod\" for rarity modifiers.");
					sender.sendMessage("[IsleWorldGen] Type \"/islegen islechance\" for island type chances.");
					if(sender.isOp()) sender.sendMessage("[IsleWorldGen] Type \"/islegen checkver\" to check for updates.");
				}
				else if(args[0].equalsIgnoreCase("chancemod"))
				{
					sender.sendMessage("[IsleWorldGen] Chance of dungeon per island: "+dungeonChance*100+"%");
					sender.sendMessage("[IsleWorldGen] Chances modifiers:");
					sender.sendMessage("[IsleWorldGen]   Coal ore: "+rarityModifiers[0]);
					sender.sendMessage("[IsleWorldGen]   Iron ore: "+rarityModifiers[1]);
					sender.sendMessage("[IsleWorldGen]   Gold ore: "+rarityModifiers[2]);
					sender.sendMessage("[IsleWorldGen]   Redstone ore: "+rarityModifiers[3]);
					sender.sendMessage("[IsleWorldGen]   Lapis ore: "+rarityModifiers[13]);
					sender.sendMessage("[IsleWorldGen]   Diamond ore: "+rarityModifiers[4]);
					sender.sendMessage("[IsleWorldGen]   Emerald ore: "+rarityModifiers[5]);
					sender.sendMessage("[IsleWorldGen]   Quartz ore: "+rarityModifiers[9]);
					sender.sendMessage("[IsleWorldGen]   Glowstone ore: "+rarityModifiers[12]);
					sender.sendMessage("[IsleWorldGen]   Clay patches (lakes): "+rarityModifiers[10]);
					sender.sendMessage("[IsleWorldGen]   Sugar cane (lakes): "+rarityModifiers[11]);
					sender.sendMessage("[IsleWorldGen]   Water/Lava pools: "+rarityModifiers[6]);
					sender.sendMessage("[IsleWorldGen]   Gravel patches: "+rarityModifiers[7]);
					sender.sendMessage("[IsleWorldGen]   Caves: "+rarityModifiers[8]);
					sender.sendMessage("[IsleWorldGen]   Grass: "+grassChance*100+"%");
					sender.sendMessage("[IsleWorldGen]   Flowers: "+flowerChance*100+"%");
				}
				else if(args[0].equalsIgnoreCase("islechance"))
				{
					sender.sendMessage("[IsleWorldGen] Island chances:");
					if(islandTotalChance == 0)
					{
						sender.sendMessage("[IsleWorldGen]   Forest: 1 (100.0%)");
					}
					else
					{
						if(islandChances[0] > 0) sender.sendMessage("[IsleWorldGen]   Plains: "+islandChances[0]+" ("+(double)Math.round((double)islandChances[0]*10000/islandTotalChance)/100+"%)");
						if(islandChances[1] > 0) sender.sendMessage("[IsleWorldGen]   Forest: "+islandChances[1]+" ("+(double)Math.round((double)islandChances[1]*10000/islandTotalChance)/100+"%)");
						if(islandChances[2] > 0) sender.sendMessage("[IsleWorldGen]   Taiga: "+islandChances[2]+" ("+(double)Math.round((double)islandChances[2]*10000/islandTotalChance)/100+"%)");
						if(islandChances[3] > 0) sender.sendMessage("[IsleWorldGen]   Swamp: "+islandChances[3]+" ("+(double)Math.round((double)islandChances[3]*10000/islandTotalChance)/100+"%)");
						if(islandChances[4] > 0) sender.sendMessage("[IsleWorldGen]   Jungle: "+islandChances[4]+" ("+(double)Math.round((double)islandChances[4]*10000/islandTotalChance)/100+"%)");
						if(islandChances[5] > 0) sender.sendMessage("[IsleWorldGen]   Desert: "+islandChances[5]+" ("+(double)Math.round((double)islandChances[5]*10000/islandTotalChance)/100+"%)");
						if(islandChances[6] > 0) sender.sendMessage("[IsleWorldGen]   Nether: "+islandChances[6]+" ("+(double)Math.round((double)islandChances[6]*10000/islandTotalChance)/100+"%)");
						if(islandChances[7] > 0) sender.sendMessage("[IsleWorldGen]   Ender: "+islandChances[7]+" ("+(double)Math.round((double)islandChances[7]*10000/islandTotalChance)/100+"%)");
						if(islandChances[8] > 0) sender.sendMessage("[IsleWorldGen]   Mushroom: "+islandChances[8]+" ("+(double)Math.round((double)islandChances[8]*10000/islandTotalChance)/100+"%)");
						if(islandChances[9] > 0) sender.sendMessage("[IsleWorldGen]   Ocean: "+islandChances[9]+" ("+(double)Math.round((double)islandChances[9]*10000/islandTotalChance)/100+"%)");
					}
				}
				else if(args[0].equalsIgnoreCase("checkver") && sender.isOp())
				{
					sender.sendMessage("[IsleWorldGen] Current version: "+getDescription().getVersion());
					sender.sendMessage("[IsleWorldGen] Latest version: "+UpdateChecker.latestVer);
					if(UpdateChecker.outdated) sender.sendMessage("[IsleWorldGen] Link: "+UpdateChecker.latestLink);
					else sender.sendMessage("[IsleWorldGen] The plugin is up to date.");
				}
				else
				{
					sender.sendMessage("[IsleWorldGen] Island World Generation V"+getDescription().getVersion());
					sender.sendMessage("[IsleWorldGen] Type \"/islegen data\" for information on the current settings.");
				}
			}
			else
			{
				sender.sendMessage("[IsleWorldGen] Island World Generation V"+getDescription().getVersion());
				sender.sendMessage("[IsleWorldGen] Type \"/islegen data\" for information on the current settings.");
			}
			return true;
		}
		return false;
	}

	public void tellOps(String string)
	{
		Player[] players = getServer().getOnlinePlayers();
		for(int a = 0; a < players.length; a++)
		{
			if(players[a].isOp()) players[a].sendMessage(string);
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player p = event.getPlayer();
		if(p.isOp())
		{
			if(UpdateChecker.outdated)
			{
				p.sendMessage("[IsleWorldGen] Current version: "+getDescription().getVersion());
				p.sendMessage("[IsleWorldGen] Latest version: "+UpdateChecker.latestVer);
				p.sendMessage("[IsleWorldGen] Link: "+UpdateChecker.latestLink);
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	private int getMaterialID(String s)
	{
		try
		{
			return Integer.parseInt(s);
		}
		catch(NumberFormatException e)
		{
			Material m = Material.getMaterial(s);
			if(m != null) return m.getId();
		}
		return 0;
	}
}
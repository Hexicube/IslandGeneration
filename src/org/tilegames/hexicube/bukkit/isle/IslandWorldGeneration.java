package org.tilegames.hexicube.bukkit.isle;

import java.net.MalformedURLException;

import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.generator.ChunkGenerator;

public final class IslandWorldGeneration extends JavaPlugin implements Listener
{
	public static int islandSpacing, islandStartY;
	public static double[] rarityModifiers;
	public static int[] islandChances;
	public static int islandTotalChance;
	public static double dungeonChance, grassChance, flowerChance;
	
	public static boolean enabled;
	
	public static boolean spawnVerified = false;
	
	private static int taskID, taskRepeatTimer;
	
	public static String parentGen;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onEnable()
	{
		enabled = true;
		
		//IslePopulator.interpretSchematic("1.1=5.0=3.3.3=0.0.0.0.0.0.-1.0.-1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.-1.0.-1");
		//IslePopulator.interpretSchematic("1.0=5.1=5.1.5=-1.-1.0.-1.-1.-1.0.0.0.-1.0.0.0.0.0.-1.0.0.0.-1.-1.-1.0.-1.-1");
		
		islandSpacing = getConfig().getInt("island.spacing", 8);
		getConfig().set("island.spacing", islandSpacing);
		islandStartY = getConfig().getInt("island.height", 150);
		getConfig().set("island.height", islandStartY);
		rarityModifiers = new double[12];
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
		
		grassChance = getConfig().getDouble("rarity.grass.coverchance", 0.4);
		getConfig().set("rarity.grass.coverchance", grassChance);
		flowerChance = getConfig().getDouble("rarity.grass.flowerchance", 0.15);
		getConfig().set("rarity.grass.flowerchance", flowerChance);
		
		taskRepeatTimer = getConfig().getInt("minutes_between_update_checks", 15);
		getConfig().set("minutes_between_update_checks", taskRepeatTimer);
		
		parentGen = getConfig().getString("parent_generator", "");
		getConfig().set("parent_generator", parentGen);
		saveConfig();
		
		if(taskRepeatTimer > 0)
		{
			try
			{
				UpdateChecker c = new UpdateChecker(this, getDescription().getVersion(), "http://dev.bukkit.org/bukkit-plugins/floating-island-world-generation/files.rss");
				taskID = getServer().getScheduler().scheduleAsyncRepeatingTask(this, c, 0, 20*60*taskRepeatTimer);
			}
			catch (MalformedURLException e)
			{
				e.printStackTrace();
				getLogger().severe("Error with starting update checker, disabling!");
			}
		}
	}
	
	@Override
	public void onDisable()
	{
		if(taskRepeatTimer > 0) getServer().getScheduler().cancelTask(taskID);
		enabled = false;
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
					sender.sendMessage("[IsleWorldGen] Island starting height: "+islandStartY);
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
					sender.sendMessage("[IsleWorldGen]   Diamond ore: "+rarityModifiers[4]);
					sender.sendMessage("[IsleWorldGen]   Emerald ore: "+rarityModifiers[5]);
					sender.sendMessage("[IsleWorldGen]   Quartz ore: "+rarityModifiers[9]);
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
}
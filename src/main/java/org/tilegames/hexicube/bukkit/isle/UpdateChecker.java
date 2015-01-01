package org.tilegames.hexicube.bukkit.isle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class UpdateChecker implements Runnable
{
	private int[] curVer, newestVer;
	private String curVerStr;
	private IslandWorldGeneration plugin;
	
	public static String latestVer, latestLink;
	public static boolean outdated;
	
	public UpdateChecker(IslandWorldGeneration plugin, String currentVersion) throws MalformedURLException
	{
		this.plugin = plugin;
		curVer = convertVersion(currentVersion);
		curVerStr = currentVersion;
	}
	
	private int[] convertVersion(String ver)
	{
		String[] data = ver.split("\\.");
		int[] verInts = new int[data.length];
		for(int a = 0; a < data.length; a++)
		{
			try
			{
				verInts[a] = Integer.parseInt(data[a]);
			}
			catch(NumberFormatException e) {}
		}
		return verInts;
	}
	
	private String shortenURL(String url) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(new URL("https://api-ssl.bitly.com/v3/shorten?access_token=59b718cba373dbeb516c90a5ed9b727267b8b5d1&format=txt&longUrl="+url).openConnection().getInputStream()));
		try
		{
			return reader.readLine();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return url;
		}
	}
	
	@Override
	public void run()
	{
		try
		{
			plugin.getLogger().info("Starting version check...");
			BufferedReader reader = new BufferedReader(new InputStreamReader(new URL("https://api.curseforge.com/servermods/files?projectIds=63030").openConnection().getInputStream()));
			try{Thread.sleep(5000);}catch(InterruptedException e){}
			String data = "";
			while(reader.ready()) data += reader.readLine()+"\n";
			reader.close();
			String link = "http:///";
			String version = "0.0";
			JSONArray array = (JSONArray)JSONValue.parse(data);
			if(array.size() > 0)
			{
				JSONObject latest = (JSONObject)array.get(array.size() - 1);
				link = StringEscapeUtils.unescapeJava((String)latest.get("downloadUrl"));
				String temp = (String)latest.get("name");
				version = temp.substring(temp.lastIndexOf(" ")+1);
			}
			String shortLink = shortenURL(link);
			int[] newVersion = convertVersion(version);
			latestVer = version;
			latestLink = shortLink;
			plugin.getLogger().info("Current version: "+curVerStr);
			plugin.getLogger().info("Latest version: "+version);
			boolean newVer = false;
			for(int a = 0; a < newVersion.length; a++)
			{
				if(a >= curVer.length)
				{
					newVer = true;
					break;
				}
				if(newVersion[a] > curVer[a])
				{
					newVer = true;
					break;
				}
				if(newVersion[a] < curVer[a]) break;
			}
			outdated = newVer;
			boolean newVerChanged = false;
			if(newestVer == null) newVerChanged = true;
			else for(int a = 0; a < newVersion.length; a++)
			{
				if(a >= newestVer.length)
				{
					newVerChanged = true;
					break;
				}
				if(newVersion[a] != newestVer[a])
				{
					newVerChanged = true;
					break;
				}
			}
			if(newVer)
			{
				plugin.getLogger().info("New version link: "+link);
				plugin.getLogger().info("Short link: "+shortLink);
				if(newVerChanged)
				{
					newestVer = newVersion;
					plugin.tellOps("[IsleWorldGen] New version detected: "+version);
					plugin.tellOps("[IsleWorldGen] Link: "+shortLink);
				}
			}
			else
			{
				plugin.getLogger().info("No newer version detected.");
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
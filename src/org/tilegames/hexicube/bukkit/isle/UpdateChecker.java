package org.tilegames.hexicube.bukkit.isle;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class UpdateChecker implements Runnable
{
	private int[] curVer;
	private String curVerStr;
	private URL feedUrl;
	private IslandWorldGeneration plugin;
	
	public static String latestVer, latestLink;
	public static boolean outdated;
	
	public UpdateChecker(IslandWorldGeneration plugin, String currentVersion, String url) throws MalformedURLException
	{
		this.plugin = plugin;
		feedUrl = new URL(url);
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
		BufferedReader reader = new BufferedReader(new InputStreamReader(new URL("https://api-ssl.bitly.com/v3/shorten?access_token=59b718cba373dbeb516c90a5ed9b727267b8b5d1&format=txt&longUrl="+escapeHtml(url)).openConnection().getInputStream()));
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
			BufferedReader reader = new BufferedReader(new InputStreamReader(feedUrl.openConnection().getInputStream()));
			try{Thread.sleep(5000);}catch(InterruptedException e){}
			plugin.getLogger().info("Starting version check...");
			String data = "";
			while(reader.ready()) data += reader.readLine()+"\n";
			reader.close();
			data = data.substring(data.indexOf("<item>")+6, data.indexOf("</item>"));
			String version = data.substring(data.indexOf("<title>")+33, data.indexOf("</title>"));
			String link = data.substring(data.indexOf("<link>")+6, data.indexOf("</link>"));
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
			if(newVer)
			{
				plugin.getLogger().info("New version link: "+link);
				plugin.getLogger().info("Short link: "+shortLink);
				plugin.tellOps("[IsleWorldGen] New version detected: "+version);
				plugin.tellOps("[IsleWorldGen] Link: "+shortLink);
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
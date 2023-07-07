package l2jorion.bots.xml;

import l2jorion.game.templates.StatsSet;

public class ClanSettings
{
	private int clanId;
	private String name;
	private int crestId;
	
	public ClanSettings(StatsSet set)
	{
		this.clanId = set.getInteger("clanId", 0);
		this.name = set.getString("name", "default");
		this.crestId = set.getInteger("crestId", 0);
	}
	
	public int geClanId()
	{
		return this.clanId;
	}
	
	public String getClanName()
	{
		return this.name;
	}
	
	public int getCrestId()
	{
		return this.crestId;
	}
}
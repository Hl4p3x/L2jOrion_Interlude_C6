/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2jorion.game.datatables.csv;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2MapRegion;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.model.zone.type.L2ClanHallZone;
import l2jorion.game.model.zone.type.L2RespawnZone;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.xml.IXmlReader;

public class MapRegionTable implements IXmlReader
{
	private static Logger LOG = LoggerFactory.getLogger(MapRegionTable.class);
	
	public static enum TeleportWhereType
	{
		Castle,
		ClanHall,
		SiegeFlag,
		Town,
		Fortress
	}
	
	private static final Map<String, L2MapRegion> _regions = new HashMap<>();
	private static final String defaultRespawn = "talking_island_town";
	
	protected MapRegionTable()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_regions.clear();
		parseDatapackDirectory("data/xml/mapregion", false);
		LOG.info("{}: Loaded {} map regions", getClass().getSimpleName(), _regions.size());
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		NamedNodeMap attrs;
		String name;
		String town;
		int locId;
		int castle;
		int bbs;
		
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("region".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						name = attrs.getNamedItem("name").getNodeValue();
						town = attrs.getNamedItem("town").getNodeValue();
						locId = parseInteger(attrs, "locId");
						castle = parseInteger(attrs, "castle");
						bbs = parseInteger(attrs, "bbs");
						
						L2MapRegion region = new L2MapRegion(name, town, locId, castle, bbs);
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							attrs = c.getAttributes();
							if ("respawnPoint".equalsIgnoreCase(c.getNodeName()))
							{
								int spawnX = parseInteger(attrs, "X");
								int spawnY = parseInteger(attrs, "Y");
								int spawnZ = parseInteger(attrs, "Z");
								
								boolean other = parseBoolean(attrs, "isOther", false);
								boolean chaotic = parseBoolean(attrs, "isChaotic", false);
								boolean banish = parseBoolean(attrs, "isBanish", false);
								
								if (other)
								{
									region.addOtherSpawn(spawnX, spawnY, spawnZ);
								}
								else if (chaotic)
								{
									region.addChaoticSpawn(spawnX, spawnY, spawnZ);
								}
								else if (banish)
								{
									region.addBanishSpawn(spawnX, spawnY, spawnZ);
								}
								else
								{
									region.addSpawn(spawnX, spawnY, spawnZ);
								}
							}
							else if ("map".equalsIgnoreCase(c.getNodeName()))
							{
								region.addMap(parseInteger(attrs, "X"), parseInteger(attrs, "Y"));
							}
							else if ("banned".equalsIgnoreCase(c.getNodeName()))
							{
								region.addBannedRace(attrs.getNamedItem("race").getNodeValue(), attrs.getNamedItem("point").getNodeValue());
							}
						}
						_regions.put(name, region);
					}
				}
			}
		}
	}
	
	public final L2MapRegion getMapRegion(int locX, int locY)
	{
		for (L2MapRegion region : _regions.values())
		{
			if (region.isZoneInRegion(getMapRegionX(locX), getMapRegionY(locY)))
			{
				return region;
			}
		}
		return null;
	}
	
	public final int getMapRegionX(int posX)
	{
		return (posX >> 15) + 9 + 11;// + centerTileX;
	}
	
	public final int getMapRegionY(int posY)
	{
		return (posY >> 15) + 10 + 8;// + centerTileX;
	}
	
	public final int getMapRegionLocId(int locX, int locY)
	{
		L2MapRegion region = getMapRegion(locX, locY);
		if (region != null)
		{
			return region.getLocId();
		}
		return 0;
	}
	
	public final L2MapRegion getMapRegion(L2Object obj)
	{
		return getMapRegion(obj.getX(), obj.getY());
	}
	
	public final int getMapRegionLocId(L2Object obj)
	{
		return getMapRegionLocId(obj.getX(), obj.getY());
	}
	
	public int getClosestTownNumber(L2Character activeChar)
	{
		return getMapRegionLocId(activeChar.getX(), activeChar.getY());
	}
	
	public String getClosestTownName(L2Character activeChar)
	{
		L2MapRegion region = getMapRegion(activeChar);
		
		if (region == null)
		{
			return "Aden Castle Town";
		}
		
		return region.getTown();
	}
	
	public int getAreaCastle(L2Character activeChar)
	{
		L2MapRegion region = getMapRegion(activeChar);
		
		if (region == null)
		{
			return 0;
		}
		
		return region.getCastle();
	}
	
	public Location getTeleToLocation(L2Character activeChar, TeleportWhereType teleportWhere)
	{
		if (activeChar instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) activeChar;
			
			// If in Monster Derby Track
			if (player.isInsideZone(ZoneId.ZONE_MONSTERTRACK))
			{
				return new Location(12661, 181687, -3560);
			}
			
			Castle castle = null;
			Fort fort = null;
			ClanHall clanhall = null;
			
			if (player.getClan() != null)
			{
				// If teleport to clan hall
				if (teleportWhere == TeleportWhereType.ClanHall)
				{
					clanhall = ClanHallManager.getInstance().getAbstractHallByOwner(player.getClan());
					if (clanhall != null)
					{
						L2ClanHallZone zone = clanhall.getZone();
						if ((zone != null))
						{
							if (player.getKarma() > 0)
							{
								return zone.getChaoticSpawnLoc();
							}
							return zone.getSpawnLoc();
						}
					}
				}
				
				// If teleport to castle
				if (teleportWhere == TeleportWhereType.Castle)
				{
					castle = CastleManager.getInstance().getCastleByOwner(player.getClan());
				}
				
				// If teleport to fort
				if (teleportWhere == TeleportWhereType.Fortress)
				{
					fort = FortManager.getInstance().getFortByOwner(player.getClan());
				}
				
				// Check if player is on castle&fortress ground
				if (castle == null)
				{
					castle = CastleManager.getInstance().getCastle(player);
				}
				
				if (fort == null)
				{
					fort = FortManager.getInstance().getFort(player);
				}
				
				if (castle != null && castle.getCastleId() > 0)
				{
					// If Teleporting to castle or
					// If is on castle with siege and player's clan is defender
					if (teleportWhere == TeleportWhereType.Castle || teleportWhere == TeleportWhereType.Castle && castle.getSiege().getIsInProgress() && castle.getSiege().getDefenderClan(player.getClan()) != null)
					{
						if (player.getKarma() > 0)
						{
							return castle.getResidenceZone().getChaoticSpawnLoc();
						}
						return castle.getResidenceZone().getSpawnLoc();
					}
					
					if (teleportWhere == TeleportWhereType.SiegeFlag && castle.getSiege().getIsInProgress())
					{
						// Check if player's clan is attacker
						List<L2NpcInstance> flags = castle.getSiege().getFlag(player.getClan());
						if (flags != null && !flags.isEmpty())
						{
							// Spawn to flag - Need more work to get player to the nearest flag
							L2NpcInstance flag = flags.get(0);
							return new Location(flag.getX(), flag.getY(), flag.getZ());
						}
					}
				}
				
				else if (fort != null && fort.getFortId() > 0)
				{
					// teleporting to castle or fortress
					// is on caslte with siege and player's clan is defender
					/*
					 * if (teleportWhere == TeleportWhereType.Fortress || teleportWhere == TeleportWhereType.Fortress && fort.getSiege().getIsInProgress() && fort.getSiege().getDefenderClan(player.getClan()) != null) { coord = fort.getZone().getSpawn(); return new Location(coord[0], coord[1],
					 * coord[2]); } if (teleportWhere == TeleportWhereType.SiegeFlag && fort.getSiege().getIsInProgress()) { // check if player's clan is attacker List<L2NpcInstance> flags = fort.getSiege().getFlag(player.getClan()); if (flags != null && !flags.isEmpty()) { // spawn to flag
					 * L2NpcInstance flag = flags.get(0); return new Location(flag.getX(), flag.getY(), flag.getZ()); } flags = null; }
					 */
				}
			}
			
			// teleport RED PK 5+ to Floran Village
			if (player.getPkKills() > 5 && player.getKarma() > 1)
			{
				return new Location(17817, 170079, -3530);
			}
			
			// Karma player land out of city
			if (player.getKarma() > 0)
			{
				try
				{
					L2RespawnZone zone = ZoneManager.getInstance().getZone(player, L2RespawnZone.class);
					if (zone != null)
					{
						return getRestartRegion(activeChar, zone.getRespawnPoint((L2PcInstance) activeChar)).getChaoticSpawnLoc();
					}
					
					return getMapRegion(activeChar).getChaoticSpawnLoc();
				}
				catch (Exception e)
				{
					if (player.isFlying())
					{
						return _regions.get("union_base_of_kserth").getChaoticSpawnLoc();
					}
					
					return _regions.get(defaultRespawn).getChaoticSpawnLoc();
				}
			}
			
		}
		// Get the nearest town
		try
		{
			L2RespawnZone zone = ZoneManager.getInstance().getZone(activeChar, L2RespawnZone.class);
			if (zone != null)
			{
				return getRestartRegion(activeChar, zone.getRespawnPoint((L2PcInstance) activeChar)).getSpawnLoc();
			}
			
			return getMapRegion(activeChar).getSpawnLoc();
		}
		catch (Exception e)
		{
			LOG.warn("ZoneManager:", e);
			
			// Port to the default respawn if no closest town found.
			return _regions.get(defaultRespawn).getSpawnLoc();
		}
	}
	
	public L2MapRegion getRestartRegion(L2Character activeChar, String point)
	{
		try
		{
			L2PcInstance player = ((L2PcInstance) activeChar);
			L2MapRegion region = _regions.get(point);
			
			if (region != null && region.getBannedRace().containsKey(player.getRace()))
			{
				getRestartRegion(player, region.getBannedRace().get(player.getRace()));
			}
			
			return region;
		}
		catch (Exception e)
		{
			LOG.warn("getRestartRegion - point:" + point, e);
			
			return _regions.get(defaultRespawn);
		}
	}
	
	public L2MapRegion getMapRegionByName(String regionName)
	{
		return _regions.get(regionName);
	}
	
	public static MapRegionTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final MapRegionTable _instance = new MapRegionTable();
	}
}

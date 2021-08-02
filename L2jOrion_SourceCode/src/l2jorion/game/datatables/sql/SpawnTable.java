/*
 * L2jOrion Project - www.l2jorion.com 
 * 
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
package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.managers.DayNightSpawnManager;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class SpawnTable
{
	private final static Logger LOG = LoggerFactory.getLogger(SpawnTable.class);
	
	private static final SpawnTable _instance = new SpawnTable();
	
	private Map<Integer, L2Spawn> spawntable = new FastMap<Integer, L2Spawn>().shared();
	private int customSpawnCount;
	
	private int _highestId;
	
	public static SpawnTable getInstance()
	{
		return _instance;
	}
	
	private SpawnTable()
	{
		if (!Config.ALT_DEV_NO_SPAWNS)
		{
			fillSpawnTable();
		}
	}
	
	public Map<Integer, L2Spawn> getSpawnTable()
	{
		return spawntable;
	}
	
	private void fillSpawnTable()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, loc_id, periodOfDay FROM spawnlist ORDER BY id");
			
			final ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					if ((template1.type.equalsIgnoreCase("L2SiegeGuard")) || (template1.type.equalsIgnoreCase("L2RaidBoss")) || (template1.type.equalsIgnoreCase("L2GrandBoss")) || (!Config.ALLOW_CLASS_MASTERS && template1.type.equals("L2ClassMaster"))
						|| (!Config.ALLOW_HITMAN_GDE && template1.type.equals("L2Hitman")))
					{
						continue;
					}
					
					spawnDat = new L2Spawn(template1);
					spawnDat.setId(rset.getInt("id"));
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					
					final int loc_id = rset.getInt("loc_id");
					
					spawnDat.setLocation(loc_id);
					
					switch (rset.getInt("periodOfDay"))
					{
						case 0: // default
							customSpawnCount += spawnDat.init();
							break;
						case 1: // Day
							DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
							customSpawnCount++;
							break;
						case 2: // Night
							DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
							customSpawnCount++;
							break;
					}
					
					spawntable.put(spawnDat.getId(), spawnDat);
					
					if (spawnDat.getId() > _highestId)
					{
						_highestId = spawnDat.getId();
					}
				}
				else
				{
					LOG.warn("SpawnTable: Data missing in NPC table for ID: " + rset.getInt("npc_templateid"));
				}
			}
			DatabaseUtils.close(statement);
			DatabaseUtils.close(rset);
		}
		catch (final Exception e)
		{
			LOG.error("SpawnTable: Spawn could not be initialized ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		LOG.info("SpawnTable: Spawning completed, total number of NPCs in the world: " + spawntable.size());
		
		// -------------------------------Custom Spawnlist----------------------------//
		if (Config.CUSTOM_SPAWNLIST_TABLE)
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				final PreparedStatement statement;
				statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, loc_id, periodOfDay FROM custom_spawnlist ORDER BY id");
				final ResultSet rset = statement.executeQuery();
				
				L2Spawn spawnDat;
				L2NpcTemplate template1;
				
				while (rset.next())
				{
					template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
					
					if (template1 != null)
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setId(rset.getInt("id"));
						spawnDat.setAmount(rset.getInt("count"));
						spawnDat.setLocx(rset.getInt("locx"));
						spawnDat.setLocy(rset.getInt("locy"));
						spawnDat.setLocz(rset.getInt("locz"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						
						final int loc_id = rset.getInt("loc_id");
						
						spawnDat.setLocation(loc_id);
						spawnDat.setCustom(true);
						
						switch (rset.getInt("periodOfDay"))
						{
							case 0: // default
								customSpawnCount += spawnDat.init();
								break;
							case 1: // Day
								DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
								customSpawnCount++;
								break;
							case 2: // Night
								DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
								customSpawnCount++;
								break;
						}
						
						spawntable.put(spawnDat.getId(), spawnDat);
						if (spawnDat.getId() > _highestId)
						{
							_highestId = spawnDat.getId();
						}
						
					}
					else
					{
						LOG.warn("CustomSpawnTable: Data missing in NPC table for ID: {}. " + rset.getInt("npc_templateid"));
					}
				}
				DatabaseUtils.close(statement);
				DatabaseUtils.close(rset);
			}
			catch (final Exception e)
			{
				LOG.error("CustomSpawnTable: Spawn could not be initialized ", e);
			}
			finally
			{
				CloseUtil.close(con);
			}
			
			LOG.info("CustomSpawnTable: Spawning completed, total number of NPCs in the world: " + customSpawnCount);
		}
	}
	
	public L2Spawn getTemplate(final int id)
	{
		return spawntable.get(id);
	}
	
	public void addNewSpawn(final L2Spawn spawn, final boolean storeInDb)
	{
		_highestId++;
		spawn.setId(_highestId);
		spawntable.put(_highestId, spawn);
		
		if (storeInDb)
		{
			Connection con = null;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				final PreparedStatement statement = con.prepareStatement("INSERT INTO " + (spawn.isCustom() ? "custom_spawnlist" : "spawnlist") + "(id,count,npc_templateid,locx,locy,locz,heading,respawn_delay,loc_id) values(?,?,?,?,?,?,?,?,?)");
				statement.setInt(1, spawn.getId());
				statement.setInt(2, spawn.getAmount());
				statement.setInt(3, spawn.getNpcid());
				statement.setInt(4, spawn.getLocx());
				statement.setInt(5, spawn.getLocy());
				statement.setInt(6, spawn.getLocz());
				statement.setInt(7, spawn.getHeading());
				statement.setInt(8, spawn.getRespawnDelay() / 1000);
				statement.setInt(9, spawn.getLocation());
				statement.execute();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				LOG.error("SpawnTable: Could not store spawn in the DB ", e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	public void deleteSpawn(final L2Spawn spawn, final boolean updateDb)
	{
		if (spawntable.remove(spawn.getId()) == null)
		{
			return;
		}
		
		if (updateDb)
		{
			Connection con = null;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				final PreparedStatement statement = con.prepareStatement("DELETE FROM " + (Config.DELETE_GMSPAWN_ON_CUSTOM && (spawn.isCustom()) ? "custom_spawnlist" : "spawnlist") + " WHERE id=?");
				statement.setInt(1, spawn.getId());
				statement.execute();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				LOG.error("SpawnTable: Spawn {} could not be removed from DB " + spawn.getId(), e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	// just wrapper
	public void reloadAll()
	{
		fillSpawnTable();
	}
	
	/**
	 * Get all the spawn of a NPC<BR>
	 * <BR>
	 * @param activeChar
	 * @param npcId : ID of the NPC to find.
	 * @param teleportIndex
	 */
	public void findNPCInstances(final L2PcInstance activeChar, final int npcId, final int teleportIndex)
	{
		int index = 0;
		for (final L2Spawn spawn : spawntable.values())
		{
			if (npcId == spawn.getNpcid())
			{
				index++;
				if (teleportIndex > -1)
				{
					if (teleportIndex == index)
					{
						activeChar.teleToLocation(spawn.getLocx(), spawn.getLocy(), spawn.getLocz(), true);
					}
				}
				else
				{
					activeChar.sendMessage(index + " - " + spawn.getTemplate().name + " (" + spawn.getId() + "): " + spawn.getLocx() + " " + spawn.getLocy() + " " + spawn.getLocz());
				}
			}
		}
		
		if (index == 0)
		{
			activeChar.sendMessage("No current spawns found.");
		}
	}
	
	public Map<Integer, L2Spawn> getAllTemplates()
	{
		return spawntable;
	}
}

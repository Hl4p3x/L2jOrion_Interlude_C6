/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.type.L2BossZone;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class GrandBossManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(GrandBossManager.class.getName());
	
	private static final String DELETE_GRAND_BOSS_LIST = "DELETE FROM grandboss_list";
	private static final String INSERT_GRAND_BOSS_LIST = "INSERT INTO grandboss_list (player_id,zone) VALUES (?,?)";
	private static final String UPDATE_GRAND_BOSS_DATA = "UPDATE grandboss_data set loc_x = ?, loc_y = ?, loc_z = ?, heading = ?, respawn_time = ?, killed_time = ?, next_respawn = ?, currentHP = ?, currentMP = ?, status = ? where boss_id = ?";
	private static final String UPDATE_GRAND_BOSS_DATA2 = "UPDATE grandboss_data set status = ? where boss_id = ?";
	
	private static GrandBossManager _instance;
	
	private Map<Integer, L2BossZone> _zones = new ConcurrentHashMap<>();
	protected static Map<Integer, L2GrandBossInstance> _bosses = new ConcurrentHashMap<>();
	protected static Map<Integer, StatsSet> _storedInfo = new ConcurrentHashMap<>();
	private Map<Integer, Integer> _bossStatus = new ConcurrentHashMap<>();
	
	public static GrandBossManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new GrandBossManager();
		}
		return _instance;
	}
	
	protected GrandBossManager()
	{
	}
	
	public void init()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement("SELECT * from grandboss_data ORDER BY boss_id");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				StatsSet info = new StatsSet();
				int bossId = rset.getInt("boss_id");
				info.set("loc_x", rset.getInt("loc_x"));
				info.set("loc_y", rset.getInt("loc_y"));
				info.set("loc_z", rset.getInt("loc_z"));
				info.set("heading", rset.getInt("heading"));
				info.set("respawn_time", rset.getLong("respawn_time"));
				
				double HP = rset.getDouble("currentHP"); // jython doesn't recognize doubles
				int true_HP = (int) HP; // so use java's ability to type cast
				info.set("currentHP", true_HP); // to convert double to int
				double MP = rset.getDouble("currentMP");
				int true_MP = (int) MP;
				info.set("currentMP", true_MP);
				
				_bossStatus.put(bossId, rset.getInt("status"));
				
				_storedInfo.put(bossId, info);
			}
			
			LOG.info("GrandBossManager: Loaded " + _storedInfo.size() + " bosses");
			
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("GrandBossManager: Could not load grandboss_data table");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void initZones()
	{
		if (_zones == null)
		{
			LOG.warn("GrandBossManager: Could not read Grand Boss zone data");
			return;
		}
		
		final Map<Integer, List<Integer>> zones = new HashMap<>();
		for (Integer zoneId : _zones.keySet())
		{
			zones.put(zoneId, new ArrayList<>());
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * from grandboss_list ORDER BY player_id");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				zones.get(rset.getInt("zone")).add(rset.getInt("player_id"));
			}
			rset.close();
			statement.close();
			
			// LOG.info("GrandBossManager: Initialized " + _zones.size() + " Grand Boss Zones");
		}
		catch (SQLException e)
		{
			LOG.warn("GrandBossManager: Could not load grandboss_list table");
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		for (Entry<Integer, L2BossZone> e : _zones.entrySet())
		{
			e.getValue().setAllowedPlayers(zones.get(e.getKey()));
		}
		
		zones.clear();
	}
	
	public void addZone(L2BossZone zone)
	{
		_zones.put(zone.getId(), zone);
	}
	
	public L2BossZone getZone(int zoneId)
	{
		return _zones.get(zoneId);
	}
	
	public final L2BossZone getZone(L2Character character)
	{
		if (_zones != null)
		{
			for (L2BossZone temp : _zones.values())
			{
				if (temp.isCharacterInZone(character))
				{
					return temp;
				}
			}
		}
		return null;
	}
	
	public L2BossZone getZone(Location loc)
	{
		return getZone(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public L2BossZone getZone(int x, int y, int z)
	{
		return _zones.values().stream().filter(zone -> zone.isInsideZone(x, y, z)).findFirst().orElse(null);
	}
	
	public boolean checkIfInZone(String zoneType, L2Object obj)
	{
		final L2BossZone temp = getZone(obj.getX(), obj.getY(), obj.getZ());
		return (temp != null) && temp.getName().equalsIgnoreCase(zoneType);
	}
	
	public Integer getBossStatus(int bossId)
	{
		return _bossStatus.get(bossId);
	}
	
	public void setBossStatus(int bossId, int status)
	{
		_bossStatus.put(bossId, status);
		updateDb(bossId, true);
	}
	
	public void addBoss(L2GrandBossInstance boss)
	{
		if (boss != null)
		{
			_bosses.put(boss.getNpcId(), boss);
		}
	}
	
	public L2GrandBossInstance getBoss(int bossId)
	{
		return _bosses.get(bossId);
	}
	
	public L2GrandBossInstance deleteBoss(int bossId)
	{
		return _bosses.remove(bossId);
	}
	
	public StatsSet getStatsSet(int bossId)
	{
		return _storedInfo.get(bossId);
	}
	
	public void setStatsSet(int bossId, StatsSet info)
	{
		_storedInfo.put(bossId, info);
		updateDb(bossId, false);
	}
	
	private void storeToDb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement delete = con.prepareStatement(DELETE_GRAND_BOSS_LIST);
			delete.executeUpdate();
			delete.close();
			
			try (PreparedStatement insert = con.prepareStatement(INSERT_GRAND_BOSS_LIST))
			{
				for (Entry<Integer, L2BossZone> e : _zones.entrySet())
				{
					List<Integer> list = e.getValue().getAllowedPlayers();
					if ((list == null) || list.isEmpty())
					{
						continue;
					}
					for (Integer player : list)
					{
						insert.setInt(1, player);
						insert.setInt(2, e.getKey());
						insert.executeUpdate();
						insert.clearParameters();
					}
				}
			}
			
			for (Integer bossId : _storedInfo.keySet())
			{
				L2GrandBossInstance boss = _bosses.get(bossId);
				StatsSet info = _storedInfo.get(bossId);
				
				if (boss == null || info == null)
				{
					PreparedStatement update2 = con.prepareStatement(UPDATE_GRAND_BOSS_DATA2);
					update2.setInt(1, _bossStatus.get(bossId));
					update2.setInt(2, bossId);
					update2.executeUpdate();
					update2.close();
				}
				else
				{
					PreparedStatement update1 = con.prepareStatement(UPDATE_GRAND_BOSS_DATA);
					
					update1.setInt(1, boss.getX());
					update1.setInt(2, boss.getY());
					update1.setInt(3, boss.getZ());
					update1.setInt(4, boss.getHeading());
					
					update1.setLong(5, info.getLong("respawn_time"));
					
					if (boss.isDead())
					{
						update1.setString(6, info.getString("killed_time"));
						update1.setString(7, info.getString("next_respawn"));
					}
					else
					{
						update1.setString(6, "-");
						update1.setString(7, "-");
					}
					
					double hp = boss.getCurrentHp();
					double mp = boss.getCurrentMp();
					if (boss.isDead())
					{
						hp = boss.getMaxHp();
						mp = boss.getMaxMp();
					}
					update1.setDouble(8, hp);
					update1.setDouble(9, mp);
					update1.setInt(10, _bossStatus.get(bossId));
					update1.setInt(11, bossId);
					update1.executeUpdate();
					update1.close();
				}
			}
		}
		catch (SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("GrandBossManager: Couldn't store grandbosses to database:" + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void updateDb(int bossId, boolean statusOnly)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			L2GrandBossInstance boss = _bosses.get(bossId);
			StatsSet info = _storedInfo.get(bossId);
			
			if (statusOnly || boss == null || info == null)
			{
				statement = con.prepareStatement(UPDATE_GRAND_BOSS_DATA2);
				statement.setInt(1, _bossStatus.get(bossId));
				statement.setInt(2, bossId);
			}
			else
			{
				statement = con.prepareStatement(UPDATE_GRAND_BOSS_DATA);
				statement.setInt(1, boss.getX());
				statement.setInt(2, boss.getY());
				statement.setInt(3, boss.getZ());
				statement.setInt(4, boss.getHeading());
				statement.setLong(5, info.getLong("respawn_time"));
				
				statement.setString(6, info.getString("killed_time"));
				statement.setString(7, info.getString("next_respawn"));
				
				double hp = boss.getCurrentHp();
				double mp = boss.getCurrentMp();
				
				if (boss.isDead())
				{
					hp = boss.getMaxHp();
					mp = boss.getMaxMp();
				}
				
				statement.setDouble(8, hp);
				statement.setDouble(9, mp);
				
				statement.setInt(10, _bossStatus.get(bossId));
				statement.setInt(11, bossId);
			}
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.warn("GrandBossManager: Couldn't update grandbosses data in database (boss id:" + bossId + "):" + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Saves all Grand Boss info and then clears all info from memory, including all schedules.
	 */
	public void cleanUp()
	{
		storeToDb();
		
		_bosses.clear();
		_storedInfo.clear();
		_bossStatus.clear();
		_zones.clear();
	}
	
	public void findGrandBoss(final L2PcInstance activeChar, final int npcId, final int teleportIndex)
	{
		int index = 0;
		for (final L2GrandBossInstance spawn : _bosses.values())
		{
			if (npcId == spawn.getNpcId())
			{
				index++;
				if (teleportIndex > -1)
				{
					if (teleportIndex == index)
					{
						activeChar.teleToLocation(spawn.getX(), spawn.getY(), spawn.getZ(), true);
					}
				}
				else
				{
					activeChar.sendMessage(index + " - " + spawn.getTemplate().name + " (" + spawn.getName() + "): " + spawn.getX() + " " + spawn.getY() + " " + spawn.getZ());
				}
			}
		}
		
		if (index == 0)
		{
			activeChar.sendMessage("No Grand Boss found.");
		}
	}
	
	public L2NpcTemplate getValidTemplate(int bossId)
	{
		L2NpcTemplate template = NpcTable.getInstance().getTemplate(bossId);
		if (template == null)
		{
			return null;
		}
		
		if (!template.type.equalsIgnoreCase("L2GrandBoss"))
		{
			return null;
		}
		
		return template;
	}
	
	public boolean isDefined(int bossId) // into database
	{
		return _bossStatus.get(bossId) != null;
	}
	
	public Map<Integer, L2BossZone> getZones()
	{
		return _zones;
	}
}

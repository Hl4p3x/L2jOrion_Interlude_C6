/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.ConfigLoader;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.Location;
import l2jorion.game.model.TowerSpawn;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Siege;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class SiegeManager
{
	private static final Logger LOG = LoggerFactory.getLogger(SiegeManager.class);
	
	public static final SiegeManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private int _attackerMaxClans = 500; // Max number of clans
	private int _attackerRespawnDelay = 20000; // Time in ms. Changeable in siege.config
	private int _defenderMaxClans = 500; // Max number of clans
	private int _defenderRespawnDelay = 10000; // Time in ms. Changeable in siege.config
	
	// Siege settings
	private FastMap<Integer, FastList<SiegeSpawn>> _artefactSpawnList;
	private FastMap<Integer, FastList<SiegeSpawn>> _controlTowerSpawnList;
	
	private final Map<Integer, List<TowerSpawn>> _flameTowers = new HashMap<>();
	
	private int _controlTowerLosePenalty = 20000; // Time in ms. Changeable in siege.config
	private int _flagMaxCount = 1; // Changeable in siege.config
	private int _siegeClanMinLevel = 4; // Changeable in siege.config
	private int _siegeLength = 120; // Time in minute. Changeable in siege.config
	
	private boolean _teleport_to_siege = false;
	private boolean _teleport_to_siege_town = false;
	
	private int _siege_delay = 14;
	
	private SiegeManager()
	{
		load();
	}
	
	// =========================================================
	// Method - Public
	public final void addSiegeSkills(final L2PcInstance character)
	{
		character.addSkill(SkillTable.getInstance().getInfo(246, 1), false);
		character.addSkill(SkillTable.getInstance().getInfo(247, 1), false);
	}
	
	public final boolean checkIfOkToSummon(final L2Character activeChar, final boolean isCheckOnly)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return false;
		}
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		L2PcInstance player = (L2PcInstance) activeChar;
		Castle castle = CastleManager.getInstance().getCastle(player);
		
		if (castle == null || castle.getCastleId() <= 0)
		{
			sm.addString("You must be on castle ground to summon this");
		}
		else if (!castle.getSiege().getIsInProgress())
		{
			sm.addString("You can only summon this during a siege.");
		}
		else if (player.getClanId() != 0 && castle.getSiege().getAttackerClan(player.getClanId()) == null)
		{
			sm.addString("You can only summon this as a registered attacker.");
		}
		else
		{
			return true;
		}
		
		if (!isCheckOnly)
		{
			player.sendPacket(sm);
		}
		
		return false;
	}
	
	public final boolean checkIsRegisteredInSiege(final L2Clan clan)
	{
		
		for (final Castle castle : CastleManager.getInstance().getCastles())
		{
			if (checkIsRegistered(clan, castle.getCastleId()) && castle.getSiege() != null && castle.getSiege().getIsInProgress())
			{
				return true;
			}
		}
		return false;
	}
	
	public final boolean checkIsRegistered(final L2Clan clan, final int castleid)
	{
		if (clan == null)
		{
			return false;
		}
		
		if (clan.getHasCastle() > 0)
		{
			return true;
		}
		
		Connection con = null;
		boolean register = false;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM siege_clans where clan_id=? and castle_id=?");
			statement.setInt(1, clan.getClanId());
			statement.setInt(2, castleid);
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				register = true;
				break;
			}
			
			rs.close();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.info("Exception: checkIsRegistered(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
		return register;
	}
	
	public final void removeSiegeSkills(final L2PcInstance character)
	{
		character.removeSkill(SkillTable.getInstance().getInfo(246, 1));
		character.removeSkill(SkillTable.getInstance().getInfo(247, 1));
	}
	
	private final void load()
	{
		// LOG.info("Initializing SiegeManager");
		InputStream is = null;
		try
		{
			is = new FileInputStream(new File(ConfigLoader.SIEGE_CONFIGURATION_FILE));
			Properties siegeSettings = new Properties();
			siegeSettings.load(is);
			
			// Siege setting
			_attackerMaxClans = Integer.decode(siegeSettings.getProperty("AttackerMaxClans", "500"));
			_attackerRespawnDelay = Integer.decode(siegeSettings.getProperty("AttackerRespawn", "30000"));
			_controlTowerLosePenalty = Integer.decode(siegeSettings.getProperty("CTLossPenalty", "20000"));
			_defenderMaxClans = Integer.decode(siegeSettings.getProperty("DefenderMaxClans", "500"));
			_defenderRespawnDelay = Integer.decode(siegeSettings.getProperty("DefenderRespawn", "20000"));
			_flagMaxCount = Integer.decode(siegeSettings.getProperty("MaxFlags", "1"));
			_siegeClanMinLevel = Integer.decode(siegeSettings.getProperty("SiegeClanMinLevel", "4"));
			_siegeLength = Integer.decode(siegeSettings.getProperty("SiegeLength", "120"));
			
			_siege_delay = Integer.decode(siegeSettings.getProperty("SiegeDelay", "14"));
			
			// Siege Teleports
			_teleport_to_siege = Boolean.parseBoolean(siegeSettings.getProperty("AllowTeleportToSiege", "false"));
			_teleport_to_siege_town = Boolean.parseBoolean(siegeSettings.getProperty("AllowTeleportToSiegeTown", "false"));
			
			// Siege spawns settings
			_controlTowerSpawnList = new FastMap<>();
			_artefactSpawnList = new FastMap<>();
			
			for (final Castle castle : CastleManager.getInstance().getCastles())
			{
				FastList<SiegeSpawn> _controlTowersSpawns = new FastList<>();
				
				for (int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty(castle.getName() + "ControlTower" + Integer.toString(i), "");
					
					if (_spawnParams.length() == 0)
					{
						break;
					}
					
					StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
					
					try
					{
						final int x = Integer.parseInt(st.nextToken());
						final int y = Integer.parseInt(st.nextToken());
						final int z = Integer.parseInt(st.nextToken());
						final int npc_id = Integer.parseInt(st.nextToken());
						final int hp = Integer.parseInt(st.nextToken());
						
						_controlTowersSpawns.add(new SiegeSpawn(castle.getCastleId(), x, y, z, 0, npc_id, hp));
					}
					catch (final Exception e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
						{
							e.printStackTrace();
						}
						LOG.warn("Error while loading control tower(s) for " + castle.getName() + " castle.");
					}
				}
				
				FastList<SiegeSpawn> _artefactSpawns = new FastList<>();
				for (int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty(castle.getName() + "Artefact" + Integer.toString(i), "");
					
					if (_spawnParams.length() == 0)
					{
						break;
					}
					
					StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
					try
					{
						final int x = Integer.parseInt(st.nextToken());
						final int y = Integer.parseInt(st.nextToken());
						final int z = Integer.parseInt(st.nextToken());
						final int heading = Integer.parseInt(st.nextToken());
						final int npc_id = Integer.parseInt(st.nextToken());
						
						_artefactSpawns.add(new SiegeSpawn(castle.getCastleId(), x, y, z, heading, npc_id));
					}
					catch (final Exception e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
						{
							e.printStackTrace();
						}
						LOG.warn("Error while loading artefact(s) for " + castle.getName() + " castle.");
					}
				}
				
				final List<TowerSpawn> flameTowers = new ArrayList<>();
				for (int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty(castle.getName() + "FlameTower" + Integer.toString(i), "");
					
					if (_spawnParams.length() == 0)
					{
						break;
					}
					
					StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
					try
					{
						final int x = Integer.parseInt(st.nextToken());
						final int y = Integer.parseInt(st.nextToken());
						final int z = Integer.parseInt(st.nextToken());
						final int npcId = Integer.parseInt(st.nextToken());
						final List<Integer> zoneList = new ArrayList<>();
						
						while (st.hasMoreTokens())
						{
							zoneList.add(Integer.parseInt(st.nextToken()));
						}
						
						flameTowers.add(new TowerSpawn(npcId, new Location(x, y, z), zoneList));
					}
					catch (Exception e)
					{
						LOG.warn(getClass().getSimpleName() + ": Error while loading flame tower(s) for " + castle.getName() + " castle.");
					}
				}
				
				_controlTowerSpawnList.put(castle.getResidenceId(), _controlTowersSpawns);
				_artefactSpawnList.put(castle.getResidenceId(), _artefactSpawns);
				_flameTowers.put(castle.getResidenceId(), flameTowers);
				
				if (castle.getOwnerId() != 0)
				{
					loadTrapUpgrade(castle.getResidenceId());
				}
			}
		}
		catch (final Exception e)
		{
			// _initialized = false;
			LOG.error("Error while loading siege data.");
			e.printStackTrace();
			
		}
		finally
		{
			
			if (is != null)
			{
				try
				{
					is.close();
				}
				catch (final IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public final FastList<SiegeSpawn> getArtefactSpawnList(final int _castleId)
	{
		if (_artefactSpawnList.containsKey(_castleId))
		{
			return _artefactSpawnList.get(_castleId);
		}
		return null;
	}
	
	public final FastList<SiegeSpawn> getControlTowerSpawnList(final int _castleId)
	{
		if (_controlTowerSpawnList.containsKey(_castleId))
		{
			return _controlTowerSpawnList.get(_castleId);
		}
		return null;
	}
	
	public final int getAttackerMaxClans()
	{
		return _attackerMaxClans;
	}
	
	public final int getAttackerRespawnDelay()
	{
		return _attackerRespawnDelay;
	}
	
	public final int getControlTowerLosePenalty()
	{
		return _controlTowerLosePenalty;
	}
	
	public final int getDefenderMaxClans()
	{
		return _defenderMaxClans;
	}
	
	public final int getDefenderRespawnDelay()
	{
		return _defenderRespawnDelay;
	}
	
	public final int getFlagMaxCount()
	{
		return _flagMaxCount;
	}
	
	public final Siege getSiege(L2Object activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public final Siege getSiege(int x, int y, int z)
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle.getSiege().checkIfInZone(x, y, z))
			{
				return castle.getSiege();
			}
		}
		return null;
	}
	
	public final int getSiegeClanMinLevel()
	{
		return _siegeClanMinLevel;
	}
	
	public final int getSiegeLength()
	{
		return _siegeLength;
	}
	
	public final int getSiegeDelay()
	{
		return _siege_delay;
	}
	
	public final List<Siege> getSieges()
	{
		final FastList<Siege> _sieges = new FastList<>();
		for (final Castle castle : CastleManager.getInstance().getCastles())
		{
			_sieges.add(castle.getSiege());
		}
		return _sieges;
	}
	
	/**
	 * @return the _teleport_to_siege
	 */
	public boolean is_teleport_to_siege_allowed()
	{
		return _teleport_to_siege;
	}
	
	/**
	 * @return the _teleport_to_siege_town
	 */
	public boolean is_teleport_to_siege_town_allowed()
	{
		return _teleport_to_siege_town;
	}
	
	public class SiegeSpawn
	{
		Location _location;
		private final int _npcId;
		private final int _heading;
		private final int _castleId;
		private int _hp;
		
		public SiegeSpawn(final int castle_id, final int x, final int y, final int z, final int heading, final int npc_id)
		{
			_castleId = castle_id;
			_location = new Location(x, y, z, heading);
			_heading = heading;
			_npcId = npc_id;
		}
		
		public SiegeSpawn(final int castle_id, final int x, final int y, final int z, final int heading, final int npc_id, final int hp)
		{
			_castleId = castle_id;
			_location = new Location(x, y, z, heading);
			_heading = heading;
			_npcId = npc_id;
			_hp = hp;
		}
		
		public int getCastleId()
		{
			return _castleId;
		}
		
		public int getNpcId()
		{
			return _npcId;
		}
		
		public int getHeading()
		{
			return _heading;
		}
		
		public int getHp()
		{
			return _hp;
		}
		
		public Location getLocation()
		{
			return _location;
		}
	}
	
	private final void loadTrapUpgrade(int castleId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_trapupgrade WHERE castleId=?");
			ps.setInt(1, castleId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_flameTowers.get(castleId).get(rs.getInt("towerIndex")).setUpgradeLevel(rs.getInt("level"));
				}
			}
		}
		catch (Exception e)
		{
			LOG.info("Exception: loadTrapUpgrade(): " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public final List<TowerSpawn> getFlameTowers(int castleId)
	{
		return _flameTowers.get(castleId);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final SiegeManager _instance = new SiegeManager();
	}
}

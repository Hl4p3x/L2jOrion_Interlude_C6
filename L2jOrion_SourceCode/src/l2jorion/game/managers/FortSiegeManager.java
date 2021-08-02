/*
 * L2jOrion Project - www.l2jorion.com 
 * 
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
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
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.entity.siege.FortSiege;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class FortSiegeManager
{
	private static final Logger LOG = LoggerFactory.getLogger(FortSiegeManager.class);
	
	public static final FortSiegeManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public FortSiegeManager()
	{
		load();
	}
	
	// =========================================================
	// Data Field
	private int _attackerMaxClans = 500; // Max number of clans
	private int _attackerRespawnDelay = 20000; // Time in ms. Changeable in siege.config
	private int _defenderMaxClans = 500; // Max number of clans
	private int _defenderRespawnDelay = 10000; // Time in ms. Changeable in siege.config
	
	// Fort Siege settings
	private FastMap<Integer, FastList<SiegeSpawn>> _commanderSpawnList;
	private FastMap<Integer, FastList<SiegeSpawn>> _flagList;
	
	private int _controlTowerLosePenalty = 20000; // Time in ms. Changeable in siege.config
	private int _flagMaxCount = 1; // Changeable in siege.config
	private int _siegeClanMinLevel = 4; // Changeable in siege.config
	private int _siegeLength = 120; // Time in minute. Changeable in siege.config
	private List<FortSiege> _sieges;
	
	public final void addSiegeSkills(final L2PcInstance character)
	{
		character.addSkill(SkillTable.getInstance().getInfo(246, 1), false);
		character.addSkill(SkillTable.getInstance().getInfo(247, 1), false);
	}
	
	/**
	 * Return true if character summon<BR>
	 * <BR>
	 * @param activeChar The L2Character of the character can summon
	 * @param isCheckOnly
	 * @return
	 */
	public final boolean checkIfOkToSummon(final L2Character activeChar, final boolean isCheckOnly)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
			return false;
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		L2PcInstance player = (L2PcInstance) activeChar;
		Fort fort = FortManager.getInstance().getFort(player);
		
		if (fort == null || fort.getFortId() <= 0)
		{
			sm.addString("You must be on fort ground to summon this");
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			sm.addString("You can only summon this during a siege.");
		}
		else if (player.getClanId() != 0 && fort.getSiege().getAttackerClan(player.getClanId()) == null)
		{
			sm.addString("You can only summon this as a registered attacker.");
		}
		else
			return true;
		
		if (!isCheckOnly)
		{
			player.sendPacket(sm);
		}
		
		sm = null;
		player = null;
		fort = null;
		
		return false;
	}
	
	/**
	 * Return true if the clan is registered or owner of a fort<BR>
	 * <BR>
	 * @param clan The L2Clan of the player
	 * @param fortid
	 * @return
	 */
	public final boolean checkIsRegistered(final L2Clan clan, final int fortid)
	{
		if (clan == null)
			return false;
		
		if (clan.getHasFort() > 0)
			return true;
		
		Connection con = null;
		boolean register = false;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM fortsiege_clans where clan_id=? and fort_id=?");
			statement.setInt(1, clan.getClanId());
			statement.setInt(2, fortid);
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				register = true;
				break;
			}
			
			rs.close();
			DatabaseUtils.close(statement);
			rs = null;
			statement = null;
		}
		catch (final Exception e)
		{
			LOG.warn("Exception: checkIsRegistered(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		return register;
	}
	
	public final void removeSiegeSkills(final L2PcInstance character)
	{
		character.removeSkill(SkillTable.getInstance().getInfo(246, 1));
		character.removeSkill(SkillTable.getInstance().getInfo(247, 1));
	}
	
	// =========================================================
	// Method - Private
	private final void load()
	{
		//LOG.info("Initializing FortSiegeManager");
		InputStream is = null;
		try
		{
			is = new FileInputStream(new File(ConfigLoader.FORTSIEGE_CONFIGURATION_FILE));
			final Properties siegeSettings = new Properties();
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
			
			// Siege spawns settings
			_commanderSpawnList = new FastMap<>();
			_flagList = new FastMap<>();
			
			for (final Fort fort : FortManager.getInstance().getForts())
			{
				final FastList<SiegeSpawn> _commanderSpawns = new FastList<>();
				final FastList<SiegeSpawn> _flagSpawns = new FastList<>();
				
				for (int i = 1; i < 5; i++)
				{
					final String _spawnParams = siegeSettings.getProperty(fort.getName() + "Commander" + Integer.toString(i), "");
					
					if (_spawnParams.length() == 0)
					{
						break;
					}
					
					final StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
					
					try
					{
						final int x = Integer.parseInt(st.nextToken());
						final int y = Integer.parseInt(st.nextToken());
						final int z = Integer.parseInt(st.nextToken());
						final int heading = Integer.parseInt(st.nextToken());
						final int npc_id = Integer.parseInt(st.nextToken());
						
						_commanderSpawns.add(new SiegeSpawn(fort.getFortId(), x, y, z, heading, npc_id));
					}
					catch (final Exception e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
							e.printStackTrace();
						
						LOG.warn("Error while loading commander(s) for " + fort.getName() + " fort.");
					}
				}
				
				_commanderSpawnList.put(fort.getFortId(), _commanderSpawns);
				
				for (int i = 1; i < 4; i++)
				{
					final String _spawnParams = siegeSettings.getProperty(fort.getName() + "Flag" + Integer.toString(i), "");
					
					if (_spawnParams.length() == 0)
					{
						break;
					}
					
					final StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");
					
					try
					{
						final int x = Integer.parseInt(st.nextToken());
						final int y = Integer.parseInt(st.nextToken());
						final int z = Integer.parseInt(st.nextToken());
						final int flag_id = Integer.parseInt(st.nextToken());
						
						_flagSpawns.add(new SiegeSpawn(fort.getFortId(), x, y, z, 0, flag_id));
					}
					catch (final Exception e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
							e.printStackTrace();
						
						LOG.warn("Error while loading flag(s) for " + fort.getName() + " fort.");
					}
				}
				_flagList.put(fort.getFortId(), _flagSpawns);
			}
			
		}
		catch (final Exception e)
		{
			LOG.error("Error while loading fortsiege data.");
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
	
	// =========================================================
	// Property - Public
	public final FastList<SiegeSpawn> getCommanderSpawnList(final int _fortId)
	{
		if (_commanderSpawnList.containsKey(_fortId))
			return _commanderSpawnList.get(_fortId);
		return null;
	}
	
	public final FastList<SiegeSpawn> getFlagList(final int _fortId)
	{
		if (_flagList.containsKey(_fortId))
			return _flagList.get(_fortId);
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
	
	public final FortSiege getSiege(final L2Object activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public final FortSiege getSiege(final int x, final int y, final int z)
	{
		for (final Fort fort : FortManager.getInstance().getForts())
			if (fort.getSiege().checkIfInZone(x, y, z))
				return fort.getSiege();
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
	
	public final List<FortSiege> getSieges()
	{
		if (_sieges == null)
		{
			_sieges = new FastList<>();
		}
		return _sieges;
	}
	
	public final void addSiege(final FortSiege fortSiege)
	{
		if (_sieges == null)
		{
			_sieges = new FastList<>();
		}
		_sieges.add(fortSiege);
	}
	
	public final void removeSiege(final FortSiege fortSiege)
	{
		if (_sieges == null)
		{
			_sieges = new FastList<>();
		}
		_sieges.remove(fortSiege);
	}
	
	public boolean isCombat(final int itemId)
	{
		return itemId == 9819;
	}
	
	public class SiegeSpawn
	{
		Location _location;
		private final int _npcId;
		private final int _heading;
		private final int _fortId;
		private int _hp;
		
		public SiegeSpawn(final int fort_id, final int x, final int y, final int z, final int heading, final int npc_id)
		{
			_fortId = fort_id;
			_location = new Location(x, y, z, heading);
			_heading = heading;
			_npcId = npc_id;
		}
		
		public SiegeSpawn(final int fort_id, final int x, final int y, final int z, final int heading, final int npc_id, final int hp)
		{
			_fortId = fort_id;
			_location = new Location(x, y, z, heading);
			_heading = heading;
			_npcId = npc_id;
			_hp = hp;
		}
		
		public int getFortId()
		{
			return _fortId;
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
	
	public final boolean checkIsRegisteredInSiege(final L2Clan clan)
	{
		for (final Fort fort : FortManager.getInstance().getForts())
		{
			if (checkIsRegistered(clan, fort.getFortId()) && fort.getSiege() != null && fort.getSiege().getIsInProgress())
			{
				return true;
			}
		}
		
		return false;
		
	}
	
	private static class SingletonHolder
	{
		protected static final FortSiegeManager _instance = new FortSiegeManager();
	}
}

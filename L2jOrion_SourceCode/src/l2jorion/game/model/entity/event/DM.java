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
package l2jorion.game.model.entity.event;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.event.manager.EventTask;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.olympiad.Olympiad;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.Ride;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

/**
 * The Class DM.
 */
public class DM implements EventTask
{
	
	/** The Constant LOG. */
	protected static final Logger LOG = LoggerFactory.getLogger(DM.class);
	
	/** The _joining location name. */
	private static String _eventName = new String(), _eventDesc = new String(), _joiningLocationName = new String();
	
	/** The _npc spawn. */
	private static L2Spawn _npcSpawn;
	
	/** The _in progress. */
	private static boolean _joining = false, _teleport = false, _started = false, _aborted = false, _sitForced = false, _inProgress = false;
	
	/** The _player z. */
	protected static int _npcId = 0, _npcX = 0, _npcY = 0, _npcZ = 0, _npcHeading = 0, _rewardId = 0, _rewardAmount = 0, _minlvl = 0, _maxlvl = 0, _joinTime = 0, _eventTime = 0, _minPlayers = 0, _maxPlayers = 0, _topKills = 0, _playerColors = 0, _playerX = 0, _playerY = 0, _playerZ = 0;
	
	/** The _interval between matchs. */
	private static long _intervalBetweenMatchs = 0;
	
	/** The start event time. */
	private String startEventTime;
	
	/** The _team event. */
	protected static boolean _teamEvent = false; // TODO to be integrated
	
	/** The _players. */
	public static Vector<L2PcInstance> _players = new Vector<>();
	
	/** The _top players. */
	public static List<L2PcInstance> _topPlayers = new ArrayList<>();
	
	/** The _save players. */
	public static Vector<String> _savePlayers = new Vector<>();
	
	/**
	 * Instantiates a new dM.
	 */
	private DM()
	{
	}
	
	/**
	 * Gets the new instance.
	 * @return the new instance
	 */
	public static DM getNewInstance()
	{
		return new DM();
	}
	
	/**
	 * Gets the _event name.
	 * @return the _eventName
	 */
	public static String get_eventName()
	{
		return _eventName;
	}
	
	/**
	 * Set_event name.
	 * @param _eventName the _eventName to set
	 * @return true, if successful
	 */
	public static boolean set_eventName(final String _eventName)
	{
		if (!is_inProgress())
		{
			DM._eventName = _eventName;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _event desc.
	 * @return the _eventDesc
	 */
	public static String get_eventDesc()
	{
		return _eventDesc;
	}
	
	/**
	 * Set_event desc.
	 * @param _eventDesc the _eventDesc to set
	 * @return true, if successful
	 */
	public static boolean set_eventDesc(final String _eventDesc)
	{
		if (!is_inProgress())
		{
			DM._eventDesc = _eventDesc;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _joining location name.
	 * @return the _joiningLocationName
	 */
	public static String get_joiningLocationName()
	{
		return _joiningLocationName;
	}
	
	/**
	 * Set_joining location name.
	 * @param _joiningLocationName the _joiningLocationName to set
	 * @return true, if successful
	 */
	public static boolean set_joiningLocationName(final String _joiningLocationName)
	{
		if (!is_inProgress())
		{
			DM._joiningLocationName = _joiningLocationName;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _npc id.
	 * @return the _npcId
	 */
	public static int get_npcId()
	{
		return _npcId;
	}
	
	/**
	 * Set_npc id.
	 * @param _npcId the _npcId to set
	 * @return true, if successful
	 */
	public static boolean set_npcId(final int _npcId)
	{
		if (!is_inProgress())
		{
			DM._npcId = _npcId;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _npc location.
	 * @return the _npc location
	 */
	public static Location get_npcLocation()
	{
		final Location npc_loc = new Location(_npcX, _npcY, _npcZ, _npcHeading);
		
		return npc_loc;
	}
	
	/**
	 * Gets the _reward id.
	 * @return the _rewardId
	 */
	public static int get_rewardId()
	{
		return _rewardId;
	}
	
	/**
	 * Set_reward id.
	 * @param _rewardId the _rewardId to set
	 * @return true, if successful
	 */
	public static boolean set_rewardId(final int _rewardId)
	{
		if (!is_inProgress())
		{
			DM._rewardId = _rewardId;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _reward amount.
	 * @return the _rewardAmount
	 */
	public static int get_rewardAmount()
	{
		return _rewardAmount;
	}
	
	/**
	 * Set_reward amount.
	 * @param _rewardAmount the _rewardAmount to set
	 * @return true, if successful
	 */
	public static boolean set_rewardAmount(final int _rewardAmount)
	{
		if (!is_inProgress())
		{
			DM._rewardAmount = _rewardAmount;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _minlvl.
	 * @return the _minlvl
	 */
	public static int get_minlvl()
	{
		return _minlvl;
	}
	
	/**
	 * Set_minlvl.
	 * @param _minlvl the _minlvl to set
	 * @return true, if successful
	 */
	public static boolean set_minlvl(final int _minlvl)
	{
		if (!is_inProgress())
		{
			DM._minlvl = _minlvl;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _maxlvl.
	 * @return the _maxlvl
	 */
	public static int get_maxlvl()
	{
		return _maxlvl;
	}
	
	/**
	 * Set_maxlvl.
	 * @param _maxlvl the _maxlvl to set
	 * @return true, if successful
	 */
	public static boolean set_maxlvl(final int _maxlvl)
	{
		if (!is_inProgress())
		{
			DM._maxlvl = _maxlvl;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _join time.
	 * @return the _joinTime
	 */
	public static int get_joinTime()
	{
		return _joinTime;
	}
	
	/**
	 * Set_join time.
	 * @param _joinTime the _joinTime to set
	 * @return true, if successful
	 */
	public static boolean set_joinTime(final int _joinTime)
	{
		if (!is_inProgress())
		{
			DM._joinTime = _joinTime;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _event time.
	 * @return the _eventTime
	 */
	public static int get_eventTime()
	{
		return _eventTime;
	}
	
	/**
	 * Set_event time.
	 * @param _eventTime the _eventTime to set
	 * @return true, if successful
	 */
	public static boolean set_eventTime(final int _eventTime)
	{
		if (!is_inProgress())
		{
			DM._eventTime = _eventTime;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _min players.
	 * @return the _minPlayers
	 */
	public static int get_minPlayers()
	{
		return _minPlayers;
	}
	
	/**
	 * Set_min players.
	 * @param _minPlayers the _minPlayers to set
	 * @return true, if successful
	 */
	public static boolean set_minPlayers(final int _minPlayers)
	{
		if (!is_inProgress())
		{
			DM._minPlayers = _minPlayers;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _max players.
	 * @return the _maxPlayers
	 */
	public static int get_maxPlayers()
	{
		return _maxPlayers;
	}
	
	/**
	 * Set_max players.
	 * @param _maxPlayers the _maxPlayers to set
	 * @return true, if successful
	 */
	public static boolean set_maxPlayers(final int _maxPlayers)
	{
		if (!is_inProgress())
		{
			DM._maxPlayers = _maxPlayers;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _interval between matchs.
	 * @return the _intervalBetweenMatchs
	 */
	public static long get_intervalBetweenMatchs()
	{
		return _intervalBetweenMatchs;
	}
	
	/**
	 * Set_interval between matchs.
	 * @param _intervalBetweenMatchs the _intervalBetweenMatchs to set
	 * @return true, if successful
	 */
	public static boolean set_intervalBetweenMatchs(final long _intervalBetweenMatchs)
	{
		if (!is_inProgress())
		{
			DM._intervalBetweenMatchs = _intervalBetweenMatchs;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the start event time.
	 * @return the startEventTime
	 */
	public String getStartEventTime()
	{
		return startEventTime;
	}
	
	/**
	 * Sets the start event time.
	 * @param startEventTime the startEventTime to set
	 * @return true, if successful
	 */
	public boolean setStartEventTime(final String startEventTime)
	{
		if (!is_inProgress())
		{
			this.startEventTime = startEventTime;
			return true;
		}
		return false;
	}
	
	/**
	 * Checks if is _joining.
	 * @return the _joining
	 */
	public static boolean is_joining()
	{
		return _joining;
	}
	
	/**
	 * Checks if is _teleport.
	 * @return the _teleport
	 */
	public static boolean is_teleport()
	{
		return _teleport;
	}
	
	/**
	 * Checks if is _started.
	 * @return the _started
	 */
	public static boolean is_started()
	{
		return _started;
	}
	
	/**
	 * Checks if is _aborted.
	 * @return the _aborted
	 */
	public static boolean is_aborted()
	{
		return _aborted;
	}
	
	/**
	 * Checks if is _sit forced.
	 * @return the _sitForced
	 */
	public static boolean is_sitForced()
	{
		return _sitForced;
	}
	
	/**
	 * Checks if is _in progress.
	 * @return the _inProgress
	 */
	public static boolean is_inProgress()
	{
		return _inProgress;
	}
	
	/**
	 * Check max level.
	 * @param maxlvl the maxlvl
	 * @return true, if successful
	 */
	public static boolean checkMaxLevel(final int maxlvl)
	{
		if (_minlvl >= maxlvl)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check min level.
	 * @param minlvl the minlvl
	 * @return true, if successful
	 */
	public static boolean checkMinLevel(final int minlvl)
	{
		if (_maxlvl <= minlvl)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * returns true if participated players is higher or equal then minimum needed players.
	 * @param players the players
	 * @return true, if successful
	 */
	public static boolean checkMinPlayers(final int players)
	{
		if (_minPlayers <= players)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * returns true if max players is higher or equal then participated players.
	 * @param players the players
	 * @return true, if successful
	 */
	public static boolean checkMaxPlayers(final int players)
	{
		if (_maxPlayers > players)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check start join ok.
	 * @return true, if successful
	 */
	public static boolean checkStartJoinOk()
	{
		if (_started || _teleport || _joining || _eventName.equals("") || _joiningLocationName.equals("") || _eventDesc.equals("") || _npcId == 0 || _npcX == 0 || _npcY == 0 || _npcZ == 0 || _rewardId == 0 || _rewardAmount == 0)
		{
			return false;
		}
		
		if (_teamEvent)
		{
			if (!checkStartJoinTeamInfo())
			{
				return false;
			}
		}
		else
		{
			if (!checkStartJoinPlayerInfo())
			{
				return false;
			}
		}
		
		if (!Config.ALLOW_EVENTS_DURING_OLY && Olympiad.getInstance().inCompPeriod())
		{
			return false;
		}
		
		for (final Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle != null && castle.getSiege() != null && castle.getSiege().getIsInProgress())
			{
				return false;
			}
		}
		
		if (!checkOptionalEventStartJoinOk())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check start join team info.
	 * @return true, if successful
	 */
	private static boolean checkStartJoinTeamInfo()
	{
		// TODO be integrated
		return true;
	}
	
	/**
	 * Check start join player info.
	 * @return true, if successful
	 */
	private static boolean checkStartJoinPlayerInfo()
	{
		if (_playerX == 0 || _playerY == 0 || _playerZ == 0 || _playerColors == 0)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check auto event start join ok.
	 * @return true, if successful
	 */
	private static boolean checkAutoEventStartJoinOk()
	{
		if (_joinTime == 0 || _eventTime == 0)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check optional event start join ok.
	 * @return true, if successful
	 */
	private static boolean checkOptionalEventStartJoinOk()
	{
		// TODO be integrated
		return true;
	}
	
	/**
	 * Sets the npc pos.
	 * @param activeChar the new npc pos
	 */
	public static void setNpcPos(final L2PcInstance activeChar)
	{
		_npcX = activeChar.getX();
		_npcY = activeChar.getY();
		_npcZ = activeChar.getZ();
		_npcHeading = activeChar.getHeading();
	}
	
	/**
	 * Spawn event npc.
	 */
	private static void spawnEventNpc()
	{
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_npcId);
		
		try
		{
			_npcSpawn = new L2Spawn(tmpl);
			
			_npcSpawn.setLocx(_npcX);
			_npcSpawn.setLocy(_npcY);
			_npcSpawn.setLocz(_npcZ);
			_npcSpawn.setAmount(1);
			_npcSpawn.setHeading(_npcHeading);
			_npcSpawn.setRespawnDelay(1);
			
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			
			_npcSpawn.init();
			_npcSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			_npcSpawn.getLastSpawn().setTitle(_eventName);
			_npcSpawn.getLastSpawn()._isEventMobDM = true;
			_npcSpawn.getLastSpawn().isAggressive();
			_npcSpawn.getLastSpawn().decayMe();
			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
			
			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUser(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn(_eventName + " Engine[spawnEventNpc(exception: " + e.getMessage());
		}
	}
	
	/**
	 * Unspawn event npc.
	 */
	private static void unspawnEventNpc()
	{
		if (_npcSpawn == null || _npcSpawn.getLastSpawn() == null)
		{
			return;
		}
		
		_npcSpawn.getLastSpawn().deleteMe();
		_npcSpawn.stopRespawn();
		SpawnTable.getInstance().deleteSpawn(_npcSpawn, true);
	}
	
	/**
	 * Start join.
	 * @return true, if successful
	 */
	public static boolean startJoin()
	{
		if (!checkStartJoinOk())
		{
			if (Config.DEBUG)
			{
				LOG.warn(_eventName + " Engine[startJoin]: startJoinOk() = false");
			}
			return false;
		}
		
		_inProgress = true;
		_joining = true;
		spawnEventNpc();
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + _eventDesc);
		
		if (Config.DM_ANNOUNCE_REWARD && ItemTable.getInstance().getTemplate(_rewardId) != null)
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Prize: " + ItemTable.getInstance().getTemplate(_rewardId).getName() + " (" + _rewardAmount + ")");
		}
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Recruiting levels: " + _minlvl + "-" + _maxlvl);
		
		if (!Config.TVT_COMMAND)
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Joinable in " + _joiningLocationName);
		}
		
		if (Config.DM_COMMAND)
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Commands .dmjoin .dmleave .dminfo");
		}
		
		return true;
	}
	
	/**
	 * Start teleport.
	 * @return true, if successful
	 */
	public static boolean startTeleport()
	{
		if (!_joining || _started || _teleport)
		{
			return false;
		}
		
		removeOfflinePlayers();
		
		if (_teamEvent)
		{
			
		}
		else
		{
			// final int size = getPlayers().size();
			synchronized (_players)
			{
				final int size = _players.size();
				if (!checkMinPlayers(size))
				{
					Announcements.getInstance().gameAnnounceToAll(_eventName + ": Not enough players. Minimum: " + _minPlayers);
					
					if (Config.DM_STATS_LOGGER)
					{
						LOG.info(_eventName + ": Not enough players for event. Min requested: " + _minPlayers + ", Participated: " + size);
					}
					
					return false;
				}
			}
		}
		
		_joining = false;
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Teleport to team spot in 10 seconds!");
		
		setUserData();
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				sit();
				afterTeleportOperations();
				
				// final Vector<L2PcInstance> players = getPlayers();
				synchronized (_players)
				{
					for (final L2PcInstance player : _players)
					{
						if (player != null)
						{
							if (Config.DM_ON_START_UNSUMMON_PET)
							{
								// Remove Summon's buffs
								if (player.getPet() != null)
								{
									final L2Summon summon = player.getPet();
									summon.stopAllEffects();
									
									if (summon instanceof L2PetInstance)
									{
										summon.unSummon(player);
									}
								}
							}
							
							if (Config.DM_ON_START_REMOVE_ALL_EFFECTS)
							{
								player.stopAllEffects();
								// custom buff
								// ww
								L2Skill skill;
								skill = SkillTable.getInstance().getInfo(1204, 2);
								skill.getEffects(player, player);
								player.broadcastPacket(new MagicSkillUser(player, player, skill.getId(), 2, skill.getHitTime(), 0));
								
								if (player.isMageClass())
								{
									// acumen
									L2Skill skill2;
									skill2 = SkillTable.getInstance().getInfo(1085, 1);
									skill2.getEffects(player, player);
									player.broadcastPacket(new MagicSkillUser(player, player, skill2.getId(), 1, skill2.getHitTime(), 0));
								}
								else
								{
									// haste
									L2Skill skill1;
									skill1 = SkillTable.getInstance().getInfo(1086, 2);
									skill1.getEffects(player, player);
									player.broadcastPacket(new MagicSkillUser(player, player, skill1.getId(), 2, skill1.getHitTime(), 0));
								}
								// custom buff end
							}
							
							// Remove player from his party
							if (player.getParty() != null)
							{
								final L2Party party = player.getParty();
								party.removePartyMember(player);
							}
							
							// player._originalTitleDM = player.getTitle();
							// player.setTitle("Kills: " + player._countDMkills);
							
							if (_teamEvent)
							{
								// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)), _teamsY.get(_teams.indexOf(player._teamNameCTF)), _teamsZ.get(_teams.indexOf(player._teamNameCTF)));
								
							}
							else
							{
								final int offset = Config.DM_SPAWN_OFFSET;
								player.teleToLocation(_playerX + Rnd.get(offset), _playerY + Rnd.get(offset), _playerZ);
							}
						}
					}
					
				}
				
			}
		}, 20000);
		
		_teleport = true;
		
		return true;
	}
	
	/**
	 * After teleport operations.
	 */
	protected static void afterTeleportOperations()
	{
		
	}
	
	/**
	 * Start event.
	 * @return true, if successful
	 */
	public static boolean startEvent()
	{
		if (!startEventOk())
		{
			if (Config.DEBUG)
			{
				LOG.warn(_eventName + " Engine[startEvent()]: startEventOk() = false");
			}
			return false;
		}
		
		_teleport = false;
		
		sit();
		removeParties();
		
		afterStartOperations();
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Started. Go to kill your enemies!");
		_started = true;
		
		return true;
	}
	
	/**
	 * Removes the parties.
	 */
	private static void removeParties()
	{
		// final Vector<L2PcInstance> players = getPlayers();
		synchronized (_players)
		{
			
			for (final L2PcInstance player : _players)
			{
				if (player.getParty() != null)
				{
					final L2Party party = player.getParty();
					party.removePartyMember(player);
				}
			}
		}
	}
	
	/**
	 * After start operations.
	 */
	private static void afterStartOperations()
	{
		
	}
	
	/**
	 * Restarts Event checks if event was aborted. and if true cancels restart task
	 */
	public synchronized static void restartEvent()
	{
		LOG.info(_eventName + ": Event has been restarted...");
		_joining = false;
		_started = false;
		_inProgress = false;
		_aborted = false;
		final long delay = _intervalBetweenMatchs;
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": joining period will be avaible again in " + _intervalBetweenMatchs + " minute(s)!");
		
		waiter(delay);
		
		try
		{
			if (!_aborted)
			{
				autoEvent(); // start a new event
			}
			else
			{
				Announcements.getInstance().gameAnnounceToAll(_eventName + ": next event aborted!");
			}
		}
		catch (final Exception e)
		{
			LOG.error(_eventName + ": Error While Trying to restart Event...", e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Finish event.
	 */
	public static void finishEvent()
	{
		if (!finishEventOk())
		{
			if (Config.DEBUG)
			{
				LOG.warn(_eventName + " Engine[finishEvent]: finishEventOk() = false");
			}
			return;
		}
		
		_started = false;
		_aborted = false;
		unspawnEventNpc();
		
		afterFinishOperations();
		
		if (_teamEvent)
		{
			processTopTeam();
		}
		else
		{
			processTopPlayer();
			
			if (_topKills != 0)
			{
				String winners = "";
				for (final L2PcInstance winner : _topPlayers)
				{
					winners = winners + " " + winner.getName();
				}
				Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + winners + " win the match! " + _topKills + " kills.");
				rewardPlayer();
				
				if (Config.DM_STATS_LOGGER)
				{
					LOG.info("**** " + _eventName + " ****");
					LOG.info(_eventName + ": " + winners + " win the match! " + _topKills + " kills.");
				}
			}
			else
			{
				
				Announcements.getInstance().gameAnnounceToAll(_eventName + ": No players win the match(nobody killed).");
				if (Config.DM_STATS_LOGGER)
				{
					LOG.info(_eventName + ": No players win the match(nobody killed).");
				}
			}
		}
		
		teleportFinish();
	}
	
	/**
	 * After finish operations.
	 */
	private static void afterFinishOperations()
	{
		
	}
	
	/**
	 * Abort event.
	 */
	public static void abortEvent()
	{
		if (!_joining && !_teleport && !_started)
		{
			return;
		}
		
		if (_joining && !_teleport && !_started)
		{
			unspawnEventNpc();
			cleanDM();
			_joining = false;
			_inProgress = false;
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": The Event cancelled!");
			return;
		}
		_joining = false;
		_teleport = false;
		_started = false;
		_aborted = true;
		unspawnEventNpc();
		
		afterFinish();
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": The Event cancelled!");
		teleportFinish();
	}
	
	/**
	 * After finish.
	 */
	private static void afterFinish()
	{
		
	}
	
	/**
	 * Teleport finish.
	 */
	public static void teleportFinish()
	{
		sit();
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Teleport back to participation NPC in 20 seconds!");
		
		removeUserData();
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				// final Vector<L2PcInstance> players = getPlayers();
				synchronized (_players)
				{
					
					for (final L2PcInstance player : _players)
					{
						if (player != null)
						{
							if (player.isOnline() != 0)
							{
								player.teleToLocation(_npcX, _npcY, _npcZ, false);
							}
							else
							{
								java.sql.Connection con = null;
								try
								{
									con = L2DatabaseFactory.getInstance().getConnection();
									
									final PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=? WHERE char_name=?");
									statement.setInt(1, _npcX);
									statement.setInt(2, _npcY);
									statement.setInt(3, _npcZ);
									statement.setString(4, player.getName());
									statement.execute();
									DatabaseUtils.close(statement);
								}
								catch (final Exception e)
								{
									if (Config.ENABLE_ALL_EXCEPTIONS)
									{
										e.printStackTrace();
									}
									
									LOG.error(e.getMessage(), e);
								}
								finally
								{
									CloseUtil.close(con);
									con = null;
								}
							}
						}
					}
					
				}
				
				sit();
				cleanDM();
			}
		}, 20000);
	}
	
	/**
	 * Auto event.
	 */
	public static void autoEvent()
	{
		LOG.info("Starting " + _eventName + "!");
		LOG.info("Matchs Are Restarted At Every: " + getIntervalBetweenMatchs() + " Minutes.");
		if (checkAutoEventStartJoinOk() && startJoin() && !_aborted)
		{
			if (_joinTime > 0)
			{
				waiter(_joinTime * 60 * 1000); // minutes for join event
			}
			else if (_joinTime <= 0)
			{
				LOG.info(_eventName + ": join time <=0 aborting event.");
				abortEvent();
				return;
			}
			if (startTeleport() && !_aborted)
			{
				waiter(30 * 1000); // 30 sec wait time untill start fight after teleported
				if (startEvent() && !_aborted)
				{
					LOG.warn(_eventName + ": waiting.....minutes for event time " + _eventTime);
					
					waiter(_eventTime * 60 * 1000); // minutes for event time
					finishEvent();
					
					LOG.info(_eventName + ": waiting... delay for final messages ");
					waiter(60000);// just a give a delay delay for final messages
					sendFinalMessages();
					
					/*
					 * if (!_started && !_aborted) { // if is not already started and it's not aborted LOG.info(_eventName + ": waiting.....delay for restart event  " + _intervalBetweenMatchs + " minutes."); waiter(60000);// just a give a delay to next restart try { if (!_aborted) restartEvent(); }
					 * catch (final Exception e) { LOG.error("Error while tying to Restart Event", e); e.printStackTrace(); } }
					 */
					
				}
			}
			else if (!_aborted)
			{
				
				abortEvent();
				// restartEvent();
				
			}
		}
	}
	
	// start without restart
	/**
	 * Event once start.
	 */
	public static void eventOnceStart()
	{
		if (startJoin() && !_aborted)
		{
			if (_joinTime > 0)
			{
				waiter(_joinTime * 60 * 1000); // minutes for join event
			}
			else if (_joinTime <= 0)
			{
				abortEvent();
				return;
			}
			if (startTeleport() && !_aborted)
			{
				waiter(1 * 60 * 1000); // 1 min wait time untill start fight after teleported
				if (startEvent() && !_aborted)
				{
					waiter(_eventTime * 60 * 1000); // minutes for event time
					finishEvent();
				}
			}
			else if (!_aborted)
			{
				abortEvent();
			}
		}
	}
	
	/**
	 * Waiter.
	 * @param interval the interval
	 */
	private static void waiter(final long interval)
	{
		final long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000);
		
		while (startWaiterTime + interval > System.currentTimeMillis() && !_aborted)
		{
			seconds--; // Here because we don't want to see two time announce at the same time
			
			if (_joining || _started || _teleport)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						removeOfflinePlayers();
						
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": Joinable in " + _joiningLocationName + "!");
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 / 60 + " hour(s) till registration close!");
						}
						else if (_started)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 / 60 + " hour(s) till event finish!");
						}
						
						break;
					case 1800: // 30 minutes left
					case 900: // 15 minutes left
					case 600: // 10 minutes left
					case 300: // 5 minutes left
					case 240: // 4 minutes left
					case 180: // 3 minutes left
					case 120: // 2 minutes left
						if (_joining)
						{
							if (!Config.TVT_COMMAND)
							{
								Announcements.getInstance().gameAnnounceToAll(_eventName + ": Joinable in " + _joiningLocationName + "!");
							}
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 + " minutes till registration close!");
						}
						else if (_started)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 + " minutes till event finish!");
						}
						
						break;
					case 60: // 1 minute left
						
						if (_joining)
						{
							if (!Config.TVT_COMMAND)
							{
								Announcements.getInstance().gameAnnounceToAll(_eventName + ": Joinable in " + _joiningLocationName + "!");
							}
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 + " minute till registration close!");
						}
						else if (_started)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 + " minute till event finish!");
						}
						
						break;
					case 30: // 30 seconds left
					case 15: // 15 seconds left
					case 10: // 10 seconds left
						removeOfflinePlayers();
					case 3: // 3 seconds left
					case 2: // 2 seconds left
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds + " seconds till registration close!");
						}
						else if (_teleport)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds + " seconds till start fight!");
						}
						else if (_started)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds + " seconds till event finish!");
						}
						break;
					case 1: // 1 seconds left
						
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds + " second till registration close!");
						}
						else if (_teleport)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds + " second till start fight!");
						}
						else if (_started)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds + " second till event finish!");
						}
						break;
				}
			}
			
			final long startOneSecondWaiterStartTime = System.currentTimeMillis();
			
			// Only the try catch with Thread.sleep(1000) give bad countdown on high wait times
			while (startOneSecondWaiterStartTime + 1000 > System.currentTimeMillis())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (final InterruptedException ie)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						ie.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * Sit.
	 */
	public static void sit()
	{
		if (_sitForced)
		{
			_sitForced = false;
		}
		else
		{
			_sitForced = true;
		}
		
		// final Vector<L2PcInstance> players = getPlayers();
		synchronized (_players)
		{
			
			for (final L2PcInstance player : _players)
			{
				if (player != null)
				{
					if (_sitForced)
					{
						player.stopMove(null);
						player.abortAttack();
						player.abortCast();
						
						if (!player.isSitting())
						{
							player.sitDown();
						}
					}
					else
					{
						if (player.isSitting())
						{
							player.standUp();
						}
					}
				}
			}
			
		}
		
	}
	
	/**
	 * Removes the offline players.
	 */
	public static void removeOfflinePlayers()
	{
		try
		{
			// final Vector<L2PcInstance> players = getPlayers();
			synchronized (_players)
			{
				
				if (_players == null || _players.isEmpty())
				{
					return;
				}
				
				final List<L2PcInstance> toBeRemoved = new ArrayList<>();
				
				for (final L2PcInstance player : _players)
				{
					if (player == null)
					{
						continue;
					}
					else if (player._inEventDM && player.isOnline() == 0 || player.isInJail() || player.isInOfflineMode())
					{
						
						if (!_joining)
						{
							player.getAppearance().setNameColor(player._originalNameColorDM);
							player.setTitle(player._originalTitleDM);
							player.setKarma(player._originalKarmaDM);
							
							player.broadcastUserInfo();
							
						}
						
						// after remove, all event data must be cleaned in player
						player._originalNameColorDM = 0;
						player._originalTitleDM = null;
						player._originalKarmaDM = 0;
						player._countDMkills = 0;
						player._inEventDM = false;
						
						toBeRemoved.add(player);
						// _players.remove(player);
						
						player.sendMessage("Your participation in the DeathMatch event has been removed.");
					}
					
				}
				_players.removeAll(toBeRemoved);
				
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error(e.getMessage(), e);
			return;
		}
	}
	
	/**
	 * Start event ok.
	 * @return true, if successful
	 */
	private static boolean startEventOk()
	{
		if (_joining || !_teleport || _started)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Finish event ok.
	 * @return true, if successful
	 */
	private static boolean finishEventOk()
	{
		if (!_started)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Adds the player ok.
	 * @param eventPlayer the event player
	 * @return true, if successful
	 */
	private static boolean addPlayerOk(final L2PcInstance eventPlayer)
	{
		if (eventPlayer._inEventDM)
		{
			eventPlayer.sendMessage("You already participated in the event!");
			return false;
		}
		
		if (eventPlayer._inEventTvT || eventPlayer._inEventCTF)
		{
			eventPlayer.sendMessage("You already participated to another event!");
			return false;
		}
		
		if (OlympiadManager.getInstance().isRegistered(eventPlayer) || eventPlayer.isInOlympiadMode())
		{
			eventPlayer.sendMessage("You already participated in Olympiad!");
			return false;
		}
		
		if (eventPlayer._active_boxes > 1 && !Config.ALLOW_DUALBOX_EVENT)
		{
			final List<String> players_in_boxes = eventPlayer.active_boxes_characters;
			
			if (players_in_boxes != null && players_in_boxes.size() > 1)
			{
				for (final String character_name : players_in_boxes)
				{
					final L2PcInstance player = L2World.getInstance().getPlayer(character_name);
					
					if (player != null && player._inEventDM)
					{
						eventPlayer.sendMessage("You already participated in event with another char!");
						return false;
					}
				}
			}
		}
		
		if (!Config.DM_ALLOW_HEALER_CLASSES && (eventPlayer.getClassId() == ClassId.cardinal || eventPlayer.getClassId() == ClassId.evaSaint || eventPlayer.getClassId() == ClassId.shillienSaint))
		{
			eventPlayer.sendMessage("You cant join with Healer Class!");
			return false;
		}
		
		synchronized (_players)
		{
			if (_players.contains(eventPlayer))
			{
				eventPlayer.sendMessage("You already participated in the event!");
				return false;
			}
			
			for (final L2PcInstance player : _players)
			{
				if (player.getObjectId() == eventPlayer.getObjectId())
				{
					eventPlayer.sendMessage("You already participated in the event!");
					return false;
				}
				else if (player.getName().equalsIgnoreCase(eventPlayer.getName()))
				{
					eventPlayer.sendMessage("You already participated in the event!");
					return false;
				}
			}
			
		}
		
		return true;
	}
	
	/**
	 * Sets the user data.
	 */
	public static void setUserData()
	{
		// final Vector<L2PcInstance> players = getPlayers();
		
		synchronized (_players)
		{
			
			for (final L2PcInstance player : _players)
			{
				player._originalNameColorDM = player.getAppearance().getNameColor();
				player._originalKarmaDM = player.getKarma();
				player._originalTitleDM = player.getTitle();
				player.getAppearance().setNameColor(_playerColors);
				player.setKarma(0);
				player.setTitle("Kills: " + player._countDMkills);
				
				if (player.isMounted())
				{
					if (player.setMountType(0))
					{
						if (player.isFlying())
						{
							player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
						}
						
						final Ride dismount = new Ride(player.getObjectId(), Ride.ACTION_DISMOUNT, 0);
						player.broadcastPacket(dismount);
						player.setMountObjectID(0);
					}
				}
				player.broadcastUserInfo();
			}
		}
	}
	
	/**
	 * Dump data.
	 */
	public static void dumpData()
	{
		LOG.info("");
		LOG.info("");
		
		if (!_joining && !_teleport && !_started)
		{
			LOG.info("<<---------------------------------->>");
			LOG.info(">> " + _eventName + " Engine infos dump (INACTIVE) <<");
			LOG.info("<<--^----^^-----^----^^------^^----->>");
		}
		else if (_joining && !_teleport && !_started)
		{
			LOG.info("<<--------------------------------->>");
			LOG.info(">> " + _eventName + " Engine infos dump (JOINING) <<");
			LOG.info("<<--^----^^-----^----^^------^----->>");
		}
		else if (!_joining && _teleport && !_started)
		{
			LOG.info("<<---------------------------------->>");
			LOG.info(">> " + _eventName + " Engine infos dump (TELEPORT) <<");
			LOG.info("<<--^----^^-----^----^^------^^----->>");
		}
		else if (!_joining && !_teleport && _started)
		{
			LOG.info("<<--------------------------------->>");
			LOG.info(">> " + _eventName + " Engine infos dump (STARTED) <<");
			LOG.info("<<--^----^^-----^----^^------^----->>");
		}
		
		LOG.info("Name: " + _eventName);
		LOG.info("Desc: " + _eventDesc);
		LOG.info("Join location: " + _joiningLocationName);
		LOG.info("Min lvl: " + _minlvl);
		LOG.info("Max lvl: " + _maxlvl);
		
		LOG.info("");
		LOG.info("##################################");
		LOG.info("# _players(Vector<L2PcInstance>) #");
		LOG.info("##################################");
		
		// final Vector<L2PcInstance> players = getPlayers();
		synchronized (_players)
		{
			LOG.info("Total Players : " + _players.size());
			
			for (final L2PcInstance player : _players)
			{
				if (player != null)
				{
					LOG.info("Name: " + player.getName() + " kills :" + player._countDMkills);
				}
			}
		}
		
		LOG.info("");
		LOG.info("################################");
		LOG.info("# _savePlayers(Vector<String>) #");
		LOG.info("################################");
		
		for (final String player : _savePlayers)
		{
			LOG.info("Name: " + player);
		}
		
		LOG.info("");
		LOG.info("");
		
		dumpLocalEventInfo();
	}
	
	/**
	 * Dump local event info.
	 */
	private static void dumpLocalEventInfo()
	{
		
	}
	
	/**
	 * Load data.
	 */
	public static void loadData()
	{
		_eventName = new String();
		_eventDesc = new String();
		_joiningLocationName = new String();
		_savePlayers = new Vector<>();
		
		synchronized (_players)
		{
			_players.clear();
		}
		
		_topPlayers = new ArrayList<>();
		_npcSpawn = null;
		_joining = false;
		_teleport = false;
		_started = false;
		_sitForced = false;
		_aborted = false;
		_inProgress = false;
		
		_npcId = 0;
		_npcX = 0;
		_npcY = 0;
		_npcZ = 0;
		_npcHeading = 0;
		_rewardId = 0;
		_rewardAmount = 0;
		_topKills = 0;
		_minlvl = 0;
		_maxlvl = 0;
		_joinTime = 0;
		_eventTime = 0;
		_minPlayers = 0;
		_maxPlayers = 0;
		_intervalBetweenMatchs = 0;
		_playerColors = 0;
		_playerX = 0;
		_playerY = 0;
		_playerZ = 0;
		
		java.sql.Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("Select * from dm");
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				_eventName = rs.getString("eventName");
				_eventDesc = rs.getString("eventDesc");
				_joiningLocationName = rs.getString("joiningLocation");
				_minlvl = rs.getInt("minlvl");
				_maxlvl = rs.getInt("maxlvl");
				_npcId = rs.getInt("npcId");
				_npcX = rs.getInt("npcX");
				_npcY = rs.getInt("npcY");
				_npcZ = rs.getInt("npcZ");
				_npcHeading = rs.getInt("npcHeading");
				_rewardId = rs.getInt("rewardId");
				_rewardAmount = rs.getInt("rewardAmount");
				_joinTime = rs.getInt("joinTime");
				_eventTime = rs.getInt("eventTime");
				_minPlayers = rs.getInt("minPlayers");
				_maxPlayers = rs.getInt("maxPlayers");
				_playerColors = rs.getInt("color");
				_playerX = rs.getInt("playerX");
				_playerY = rs.getInt("playerY");
				_playerZ = rs.getInt("playerZ");
				_intervalBetweenMatchs = rs.getInt("delayForNextEvent");
			}
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			LOG.error("Exception: DM.loadData(): " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Save data.
	 */
	public static void saveData()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			statement = con.prepareStatement("Delete from dm");
			statement.execute();
			DatabaseUtils.close(statement);
			
			statement = con.prepareStatement("INSERT INTO dm (eventName, eventDesc, joiningLocation, minlvl, maxlvl, npcId, npcX, npcY, npcZ, npcHeading, rewardId, rewardAmount, joinTime, eventTime, minPlayers, maxPlayers, color, playerX, playerY, playerZ, delayForNextEvent ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setString(1, _eventName);
			statement.setString(2, _eventDesc);
			statement.setString(3, _joiningLocationName);
			statement.setInt(4, _minlvl);
			statement.setInt(5, _maxlvl);
			statement.setInt(6, _npcId);
			statement.setInt(7, _npcX);
			statement.setInt(8, _npcY);
			statement.setInt(9, _npcZ);
			statement.setInt(10, _npcHeading);
			statement.setInt(11, _rewardId);
			statement.setInt(12, _rewardAmount);
			statement.setInt(13, _joinTime);
			statement.setInt(14, _eventTime);
			statement.setInt(15, _minPlayers);
			statement.setInt(16, _maxPlayers);
			statement.setInt(17, _playerColors);
			statement.setInt(18, _playerX);
			statement.setInt(19, _playerY);
			statement.setInt(20, _playerZ);
			statement.setLong(21, _intervalBetweenMatchs);
			statement.execute();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Exception: DM.saveData(): " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Show event html.
	 * @param eventPlayer the event player
	 * @param objectId the object id
	 */
	public static void showEventHtml(final L2PcInstance eventPlayer, final String objectId)
	{
		try
		{
			final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			
			final TextBuilder replyMSG = new TextBuilder("<html><title>" + _eventName + "</title><body>");
			replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br1>");
			replyMSG.append("<center><font color=\"3366CC\">Current event:</font></center><br1>");
			replyMSG.append("<center>Name:&nbsp;<font color=\"00FF00\">" + _eventName + "</font></center><br1>");
			replyMSG.append("<center>Description:&nbsp;<font color=\"00FF00\">" + _eventDesc + "</font></center><br><br>");
			replyMSG.append("<center>Event Type:&nbsp;<font color=\"00FF00\"> Full Buff Event!!! </font></center><br><br>");
			
			// final Vector<L2PcInstance> players = getPlayers();
			synchronized (_players)
			{
				
				if (!_started && !_joining)
				{
					replyMSG.append("<center>Wait till the admin/gm start the participation.</center>");
				}
				else if (!checkMaxPlayers(_players.size()))
				{
					if (!_started)
					{
						replyMSG.append("Currently participated: <font color=\"00FF00\">" + _players.size() + ".</font><br>");
						replyMSG.append("Max players: <font color=\"00FF00\">" + _maxPlayers + "</font><br><br>");
						replyMSG.append("<font color=\"FFFF00\">You can't participate to this event.</font><br>");
					}
				}
				else if (eventPlayer.isCursedWeaponEquiped() && !Config.DM_JOIN_CURSED)
				{
					replyMSG.append("<font color=\"FFFF00\">You can't participate to this event with a cursed Weapon.</font><br>");
				}
				else if (!_started && _joining && eventPlayer.getLevel() >= _minlvl && eventPlayer.getLevel() <= _maxlvl)
				{
					if (_players.contains(eventPlayer))
					{
						replyMSG.append("<center><font color=\"3366CC\">You participated already!</font></center><br><br>");
						
						replyMSG.append("<center>Joined Players: <font color=\"00FF00\">" + _players.size() + "</font></center><br>");
						replyMSG.append("<table border=\"0\"><tr>");
						replyMSG.append("<td width=\"200\">Wait till event start or</td>");
						replyMSG.append("<td width=\"60\"><center><button value=\"remove\" action=\"bypass -h npc_" + objectId + "_dmevent_player_leave\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></td>");
						replyMSG.append("<td width=\"100\">your participation!</td>");
						replyMSG.append("</tr></table>");
					}
					else
					{
						replyMSG.append("<center>Joined Players: <font color=\"00FF00\">" + _players.size() + "</font></center><br>");
						replyMSG.append("<center><font color=\"3366CC\">You want to participate in the event?</font></center><br>");
						replyMSG.append("<center><td width=\"200\">Min lvl: <font color=\"00FF00\">" + _minlvl + "</font></center></td><br>");
						replyMSG.append("<center><td width=\"200\">Max lvl: <font color=\"00FF00\">" + _maxlvl + "</font></center></td><br><br>");
						replyMSG.append("<center><button value=\"Join\" action=\"bypass -h npc_" + objectId + "_dmevent_player_join\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center><br>");
						
					}
				}
				else if (_started && !_joining)
				{
					replyMSG.append("<center>" + _eventName + " match is in progress.</center>");
				}
				else if (eventPlayer.getLevel() < _minlvl || eventPlayer.getLevel() > _maxlvl)
				{
					replyMSG.append("Your lvl: <font color=\"00FF00\">" + eventPlayer.getLevel() + "</font><br>");
					replyMSG.append("Min lvl: <font color=\"00FF00\">" + _minlvl + "</font><br>");
					replyMSG.append("Max lvl: <font color=\"00FF00\">" + _maxlvl + "</font><br><br>");
					replyMSG.append("<font color=\"FFFF00\">You can't participate to this event.</font><br>");
				}
				
			}
			
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);
			
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error(_eventName + " Engine[showEventHtlm(" + eventPlayer.getName() + ", " + objectId + ")]: exception" + e.getMessage());
		}
	}
	
	/*
	 * public static void showEventHtml(L2PcInstance eventPlayer, String objectId) { try { NpcHtmlMessage adminReply = new NpcHtmlMessage(5); TextBuilder replyMSG = new TextBuilder("<html><body>"); replyMSG.append("DM Match<br><br><br>"); replyMSG.append("Current event...<br1>");
	 * replyMSG.append("	... name:&nbsp;<font color=\"00FF00\">" + _eventName + "</font><br1>"); replyMSG.append("	... description:&nbsp;<font color=\"00FF00\">" + _eventDesc + "</font><br><br>"); if(!_started && !_joining)
	 * replyMSG.append("<center>Wait till the admin/gm start the participation.</center>"); else if(!checkMaxPlayers(_players.size())){ if(!DM._started) { replyMSG.append("Currently participated: <font color=\"00FF00\">" + _players.size() + ".</font><br>");
	 * replyMSG.append("Max players: <font color=\"00FF00\">" + _maxPlayers + "</font><br><br>"); replyMSG.append("<font color=\"FFFF00\">You can't participate to this event.</font><br>"); } } else if(!_started && _joining && eventPlayer.getLevel()>=_minlvl && eventPlayer.getLevel()<=_maxlvl) {
	 * if(_players.contains(eventPlayer)) { replyMSG.append("You participated already!<br><br>"); replyMSG.append("<table border=\"0\"><tr>"); replyMSG.append("<td width=\"200\">Wait till event start or</td>");
	 * replyMSG.append("<td width=\"60\"><center><button value=\"remove\" action=\"bypass -h npc_" + objectId + "_dmevent_player_leave\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></td>"); replyMSG.append("<td width=\"100\">your participation!</td>");
	 * replyMSG.append("</tr></table>"); } else { replyMSG.append("You want to participate in the event?<br><br>"); replyMSG.append("<td width=\"200\">Admin set min lvl : <font color=\"00FF00\">" + _minlvl + "</font></td><br>");
	 * replyMSG.append("<td width=\"200\">Admin set max lvl : <font color=\"00FF00\">" + _maxlvl + "</font></td><br><br>"); replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_dmevent_player_join\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">"); } } else
	 * if(_started && !_joining) replyMSG.append("<center>"+_eventName+" match is in progress.</center>"); else if(eventPlayer.getLevel() < _minlvl || eventPlayer.getLevel() > _maxlvl) { replyMSG.append("Your lvl: <font color=\"00FF00\">" + eventPlayer.getLevel() + "</font><br>");
	 * replyMSG.append("Min lvl: <font color=\"00FF00\">" + _minlvl + "</font><br>"); replyMSG.append("Max lvl: <font color=\"00FF00\">" + _maxlvl + "</font><br><br>"); replyMSG.append("<font color=\"FFFF00\">You can't participate to this event.</font><br>"); } replyMSG.append("</body></html>");
	 * adminReply.setHtml(replyMSG.toString()); eventPlayer.sendPacket(adminReply); // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet eventPlayer.sendPacket( ActionFailed.STATIC_PACKET ); } catch(Exception e) { LOG.error(
	 * _eventName+" Engine[showEventHtlm(" + eventPlayer.getName() + ", " + objectId + ")]: exception" + e.getMessage()); } }
	 */
	
	/**
	 * Adds the player.
	 * @param player the player
	 */
	public static void addPlayer(final L2PcInstance player)
	{
		if (!addPlayerOk(player))
		{
			return;
		}
		
		synchronized (_players)
		{
			_players.add(player);
		}
		
		player._inEventDM = true;
		player._countDMkills = 0;
		_savePlayers.add(player.getName());
		player.sendMessage("DM: You successfully registered for the DeathMatch event.");
	}
	
	/**
	 * Removes the player.
	 * @param player the player
	 */
	public static void removePlayer(final L2PcInstance player)
	{
		if (player != null && player._inEventDM)
		{
			if (!_joining)
			{
				player.getAppearance().setNameColor(player._originalNameColorDM);
				player.setTitle(player._originalTitleDM);
				player.setKarma(player._originalKarmaDM);
				
				player.broadcastUserInfo();
				
			}
			
			// after remove, all event data must be cleaned in player
			player._originalNameColorDM = 0;
			player._originalTitleDM = null;
			player._originalKarmaDM = 0;
			player._countDMkills = 0;
			player._inEventDM = false;
			
			synchronized (_players)
			{
				_players.remove(player);
			}
			
			player.sendMessage("Your participation in the DeathMatch event has been removed.");
			
		}
	}
	
	/**
	 * Clean dm.
	 */
	public static void cleanDM()
	{
		// final Vector<L2PcInstance> players = getPlayers();
		synchronized (_players)
		{
			
			for (final L2PcInstance player : _players)
			{
				if (player != null)
				{
					
					cleanEventPlayer(player);
					
					if (player._inEventDM)
					{
						if (!_joining)
						{
							player.getAppearance().setNameColor(player._originalNameColorDM);
							player.setTitle(player._originalTitleDM);
							player.setKarma(player._originalKarmaDM);
							
							player.broadcastUserInfo();
							
						}
						
						// after remove, all event data must be cleaned in player
						player._originalNameColorDM = 0;
						player._originalTitleDM = null;
						player._originalKarmaDM = 0;
						player._countDMkills = 0;
						player._inEventDM = false;
						
						// _players.remove(player);
						
						player.sendMessage("Your participation in the DeathMatch event has been removed.");
						
					}
					
					if (_savePlayers.contains(player.getName()))
					{
						_savePlayers.remove(player.getName());
					}
					player._inEventDM = false;
				}
			}
			
			_players.clear();
			
		}
		
		_topKills = 0;
		_savePlayers = new Vector<>();
		_topPlayers = new ArrayList<>();
		
		cleanLocalEventInfo();
		
		_inProgress = false;
		
		loadData();
	}
	
	/**
	 * Clean local event info.
	 */
	private static void cleanLocalEventInfo()
	{
		// nothing
	}
	
	/**
	 * Clean event player.
	 * @param player the player
	 */
	private static void cleanEventPlayer(final L2PcInstance player)
	{
		// nothing
	}
	
	/*
	 * public static void cleanDM() { synchronized (_players){ for(L2PcInstance player : _players) { removePlayer(player); } _savePlayers = new Vector<String>(); _topPlayer = null; _npcSpawn = null; _joining = false; _teleport = false; _started = false; _inProgress = false; _sitForced = false;
	 * _topKills = 0; _players = new Vector<L2PcInstance>(); } }
	 */
	
	/**
	 * Adds the disconnected player.
	 * @param player the player
	 */
	public static void addDisconnectedPlayer(final L2PcInstance player)
	{
		// final Vector<L2PcInstance> players = getPlayers();
		synchronized (_players)
		{
			
			if (!_players.contains(player) && _savePlayers.contains(player.getName()))
			{
				if (Config.DM_ON_START_REMOVE_ALL_EFFECTS)
				{
					player.stopAllEffects();
					// custom buff
					// ww
					L2Skill skill;
					skill = SkillTable.getInstance().getInfo(1204, 2);
					skill.getEffects(player, player);
					player.broadcastPacket(new MagicSkillUser(player, player, skill.getId(), 2, skill.getHitTime(), 0));
					
					if (player.isMageClass())
					{
						// acumen
						L2Skill skill2;
						skill2 = SkillTable.getInstance().getInfo(1085, 1);
						skill2.getEffects(player, player);
						player.broadcastPacket(new MagicSkillUser(player, player, skill2.getId(), 1, skill2.getHitTime(), 0));
					}
					else
					{
						// haste
						L2Skill skill1;
						skill1 = SkillTable.getInstance().getInfo(1086, 2);
						skill1.getEffects(player, player);
						player.broadcastPacket(new MagicSkillUser(player, player, skill1.getId(), 2, skill1.getHitTime(), 0));
					}
					// custom buff end
				}
				
				_players.add(player);
				
				player._originalNameColorDM = player.getAppearance().getNameColor();
				player._originalTitleDM = player.getTitle();
				player._originalKarmaDM = player.getKarma();
				player._inEventDM = true;
				player._countDMkills = 0;
				if (_teleport || _started)
				{
					player.setTitle("Kills: " + player._countDMkills);
					player.getAppearance().setNameColor(_playerColors);
					player.setKarma(0);
					player.broadcastUserInfo();
					player.teleToLocation(_playerX + Rnd.get(Config.DM_SPAWN_OFFSET), _playerY + Rnd.get(Config.DM_SPAWN_OFFSET), _playerZ);
				}
			}
		}
	}
	
	/**
	 * Gets the _player colors.
	 * @return the _playerColors
	 */
	public static int get_playerColors()
	{
		return _playerColors;
	}
	
	/**
	 * Set_player colors.
	 * @param _playerColors the _playerColors to set
	 * @return true, if successful
	 */
	public static boolean set_playerColors(final int _playerColors)
	{
		if (!is_inProgress())
		{
			DM._playerColors = _playerColors;
			return true;
		}
		return false;
	}
	
	/**
	 * Reward player.
	 */
	public static void rewardPlayer()
	{
		if (_topPlayers.size() > 0)
		{
			
			for (final L2PcInstance _topPlayer : _topPlayers)
			{
				_topPlayer.addItem("DM Event: " + _eventName, _rewardId, _rewardAmount, _topPlayer, true);
				
				final StatusUpdate su = new StatusUpdate(_topPlayer.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, _topPlayer.getCurrentLoad());
				_topPlayer.sendPacket(su);
				
				final NpcHtmlMessage nhm = new NpcHtmlMessage(5);
				final TextBuilder replyMSG = new TextBuilder("");
				
				replyMSG.append("<html><body>You won the event. Look in your inventory for the reward.</body></html>");
				
				nhm.setHtml(replyMSG.toString());
				_topPlayer.sendPacket(nhm);
				
				// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
				_topPlayer.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
		/*
		 * if(_topPlayer != null) { _topPlayer.addItem("DM Event: " + _eventName, _rewardId, _rewardAmount, _topPlayer, true); StatusUpdate su = new StatusUpdate(_topPlayer.getObjectId()); su.addAttribute(StatusUpdate.CUR_LOAD, _topPlayer.getCurrentLoad()); _topPlayer.sendPacket(su); NpcHtmlMessage
		 * nhm = new NpcHtmlMessage(5); TextBuilder replyMSG = new TextBuilder(""); replyMSG.append("<html><body>You won the event. Look in your inventory for the reward.</body></html>"); nhm.setHtml(replyMSG.toString()); _topPlayer.sendPacket(nhm); // Send a Server->Client ActionFailed to the
		 * L2PcInstance in order to avoid that the client wait another packet _topPlayer.sendPacket( ActionFailed.STATIC_PACKET ); }
		 */
	}
	
	/**
	 * Process top player.
	 */
	private static void processTopPlayer()
	{
		// final Vector<L2PcInstance> players = getPlayers();
		synchronized (_players)
		{
			
			for (final L2PcInstance player : _players)
			{
				if (player._countDMkills > _topKills)
				{
					_topPlayers.clear();
					_topPlayers.add(player);
					_topKills = player._countDMkills;
					
				}
				else if (player._countDMkills == _topKills)
				{
					if (!_topPlayers.contains(player))
					{
						_topPlayers.add(player);
					}
				}
			}
		}
	}
	
	/**
	 * Process top team.
	 */
	private static void processTopTeam()
	{
		
	}
	
	/**
	 * Gets the _players spawn location.
	 * @return the _players spawn location
	 */
	public static Location get_playersSpawnLocation()
	{
		final Location npc_loc = new Location(_playerX + Rnd.get(Config.DM_SPAWN_OFFSET), _playerY + Rnd.get(Config.DM_SPAWN_OFFSET), _playerZ, 0);
		
		return npc_loc;
	}
	
	/**
	 * Gets the players.
	 * @return the players
	 */
	/*
	 * protected synchronized static Vector<L2PcInstance> getPlayers() { return _players; }
	 */
	
	/**
	 * Sets the players pos.
	 * @param activeChar the new players pos
	 */
	public static void setPlayersPos(final L2PcInstance activeChar)
	{
		_playerX = activeChar.getX();
		_playerY = activeChar.getY();
		_playerZ = activeChar.getZ();
	}
	
	/**
	 * Removes the user data.
	 */
	public static void removeUserData()
	{
		// final Vector<L2PcInstance> players = getPlayers();
		synchronized (_players)
		{
			for (final L2PcInstance player : _players)
			{
				player.getAppearance().setNameColor(player._originalNameColorDM);
				player.setTitle(player._originalTitleDM);
				player.setKarma(player._originalKarmaDM);
				player._inEventDM = false;
				player._countDMkills = 0;
				player.broadcastUserInfo();
			}
		}
		
	}
	
	/**
	 * just an announcer to send termination messages.
	 */
	public static void sendFinalMessages()
	{
		if (!_started && !_aborted)
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Thank you For participating!");
		}
	}
	
	/**
	 * returns the interval between each event.
	 * @return the interval between matchs
	 */
	public static int getIntervalBetweenMatchs()
	{
		final long actualTime = System.currentTimeMillis();
		final long totalTime = actualTime + _intervalBetweenMatchs;
		final long interval = totalTime - actualTime;
		final int seconds = (int) (interval / 1000);
		
		return seconds / 60;
	}
	
	/**
	 * Sets the event start time.
	 * @param newTime the new event start time
	 */
	public void setEventStartTime(final String newTime)
	{
		startEventTime = newTime;
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.entity.event.manager.EventTask#getEventIdentifier()
	 */
	@Override
	public String getEventIdentifier()
	{
		return _eventName;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		LOG.info("DM: Event notification start");
		eventOnceStart();
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.entity.event.manager.EventTask#getEventStartTime()
	 */
	@Override
	public String getEventStartTime()
	{
		return startEventTime;
	}
	
	/**
	 * On disconnect.
	 * @param player the player
	 */
	public static void onDisconnect(final L2PcInstance player)
	{
		if (player._inEventDM)
		{
			removePlayer(player);
			player.teleToLocation(_npcX, _npcY, _npcZ);
		}
	}
}
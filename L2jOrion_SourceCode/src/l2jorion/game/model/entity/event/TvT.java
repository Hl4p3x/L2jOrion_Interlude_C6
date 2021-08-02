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
import java.util.List;
import java.util.Vector;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.DoorTable;
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
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

public class TvT implements EventTask
{
	protected static final Logger LOG = LoggerFactory.getLogger(TvT.class);
	
	protected static String _eventName = new String(), _eventDesc = new String(), _joiningLocationName = new String();
	
	/** The _npc spawn. */
	private static L2Spawn _npcSpawn;
	
	/** The _in progress. */
	protected static boolean _joining = false, _teleport = false, _started = false, _aborted = false, _sitForced = false, _inProgress = false;
	
	/** The _max players. */
	protected static int _npcId = 0, _npcX = 0, _npcY = 0, _npcZ = 0, _npcHeading = 0, _rewardId = 0, _rewardAmount = 0, _minlvl = 0, _maxlvl = 0, _joinTime = 0, _eventTime = 0, _minPlayers = 0, _maxPlayers = 0;
	
	/** The _interval between matchs. */
	protected static long _intervalBetweenMatchs = 0;
	
	/** The start event time. */
	private String startEventTime;
	
	/** The _team event. */
	private static boolean _teamEvent = true; // TODO to be integrated
	
	/** The _players. */
	public static Vector<L2PcInstance> _players = new Vector<>();
	
	/** The _top team. */
	private static String _topTeam = new String();
	
	/** The _players shuffle. */
	public static Vector<L2PcInstance> _playersShuffle = new Vector<>();
	
	public static Vector<String> _teams = new Vector<>(), _savePlayers = new Vector<>(), _savePlayerTeams = new Vector<>();
	public static Vector<Integer> _teamPlayersCount = new Vector<>(), _teamColors = new Vector<>(), _teamsX = new Vector<>(), _teamsY = new Vector<>(), _teamsZ = new Vector<>();
	public static Vector<Integer> _teamPointsCount = new Vector<>();
	
	/** The _top kills. */
	public static int _topKills = 0;
	
	/**
	 * Instantiates a new tv t.
	 */
	private TvT()
	{
	}
	
	/**
	 * Gets the new instance.
	 * @return the new instance
	 */
	public static TvT getNewInstance()
	{
		return new TvT();
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
			TvT._eventName = _eventName;
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
			TvT._eventDesc = _eventDesc;
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
			TvT._joiningLocationName = _joiningLocationName;
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
			TvT._npcId = _npcId;
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
			TvT._rewardId = _rewardId;
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
			TvT._rewardAmount = _rewardAmount;
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
			TvT._minlvl = _minlvl;
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
			TvT._maxlvl = _maxlvl;
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
			TvT._joinTime = _joinTime;
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
			TvT._eventTime = _eventTime;
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
			TvT._minPlayers = _minPlayers;
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
			TvT._maxPlayers = _maxPlayers;
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
			TvT._intervalBetweenMatchs = _intervalBetweenMatchs;
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
		
		if (_teams.size() < 2 || _teamsX.contains(0) || _teamsY.contains(0) || _teamsZ.contains(0))
		{
			return false;
		}
		
		return true;
		
	}
	
	/**
	 * Check start join player info.
	 * @return true, if successful
	 */
	private static boolean checkStartJoinPlayerInfo()
	{
		
		// TODO be integrated
		return true;
		
	}
	
	/**
	 * Check auto event start join ok.
	 * @return true, if successful
	 */
	protected static boolean checkAutoEventStartJoinOk()
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
			_npcSpawn.getLastSpawn()._isEventMobTvT = true;
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
		
		if (Config.TVT_ANNOUNCE_REWARD && ItemTable.getInstance().getTemplate(_rewardId) != null)
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Prize: " + ItemTable.getInstance().getTemplate(_rewardId).getName() + " (" + _rewardAmount + ")");
		}
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Recruiting levels: " + _minlvl + "-" + _maxlvl);
		
		if (!Config.TVT_COMMAND)
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Joinable in " + _joiningLocationName + ".");
		}
		
		if (Config.TVT_COMMAND)
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Commands .tvtjoin .tvtleave .tvtinfo");
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
		
		if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE") && checkMinPlayers(_playersShuffle.size()))
		{
			shuffleTeams();
		}
		else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE") && !checkMinPlayers(_playersShuffle.size()))
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Not enough players. Minimum: " + _minPlayers);
			
			if (Config.CTF_STATS_LOGGER)
			{
				LOG.info(_eventName + ": Not enough players for event. Min requested: " + _minPlayers + ", Participated: " + _playersShuffle.size());
			}
			
			return false;
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
				
				synchronized (_players)
				{
					for (final L2PcInstance player : _players)
					{
						if (player != null)
						{
							if (Config.TVT_ON_START_UNSUMMON_PET)
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
							
							if (Config.TVT_ON_START_REMOVE_ALL_EFFECTS)
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
							
							player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameTvT)) + Rnd.get(201) - 100, _teamsY.get(_teams.indexOf(player._teamNameTvT)) + Rnd.get(201) - 100, _teamsZ.get(_teams.indexOf(player._teamNameTvT)));
						}
					}
				}
				
			}
		}, 20000);
		_teleport = true;
		return true;
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
		
		if (Config.TVT_CLOSE_FORT_DOORS)
		{
			closeFortDoors();
		}
		
		if (Config.TVT_CLOSE_ADEN_COLOSSEUM_DOORS)
		{
			closeAdenColosseumDoors();
		}
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Started. Go to kill your enemies!");
		_started = true;
		
		return true;
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
			synchronized (_players)
			{
				final L2PcInstance bestKiller = findBestKiller(_players);
				final L2PcInstance looser = findLooser(_players);
				
				if (_topKills != 0)
				{
					
					playKneelAnimation(_topTeam);
					
					if (Config.TVT_ANNOUNCE_TEAM_STATS)
					{
						Announcements.getInstance().gameAnnounceToAll(_eventName + " Team Statistics:");
						for (final String team : _teams)
						{
							final int _kills = teamKillsCount(team);
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": Team: " + team + " - Kills: " + _kills);
						}
						
						if (bestKiller != null)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": Top killer: " + bestKiller.getName() + " - Kills: " + bestKiller._countTvTkills);
						}
						if ((looser != null) && (!looser.equals(bestKiller)))
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": Top looser: " + looser.getName() + " - Dies: " + looser._countTvTdies);
						}
					}
					
					if (_topTeam != null)
					{
						Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + _topTeam + "'s win the match! " + _topKills + " kills.");
					}
					else
					{
						Announcements.getInstance().gameAnnounceToAll(_eventName + ": The event finished with a TIE: " + _topKills + " kills by each team!");
					}
					rewardTeam(_topTeam, bestKiller, looser);
					
					if (Config.TVT_STATS_LOGGER)
					{
						LOG.info("**** " + _eventName + " ****");
						LOG.info(_eventName + " Team Statistics:");
						for (final String team : _teams)
						{
							final int _kills = teamKillsCount(team);
							LOG.info("Team: " + team + " - Kills: " + _kills);
						}
						
						if (bestKiller != null)
						{
							LOG.info("Top killer: " + bestKiller.getName() + " - Kills: " + bestKiller._countTvTkills);
						}
						if ((looser != null) && (!looser.equals(bestKiller)))
						{
							LOG.info("Top looser: " + looser.getName() + " - Dies: " + looser._countTvTdies);
						}
						
						LOG.info(_eventName + ": " + _topTeam + "'s win the match! " + _topKills + " kills.");
						
					}
					
				}
				else
				{
					
					Announcements.getInstance().gameAnnounceToAll(_eventName + ": The event finished with a TIE: No team wins the match(nobody killed)!");
					
					if (Config.TVT_STATS_LOGGER)
					{
						LOG.info(_eventName + ": No team win the match(nobody killed).");
					}
					
					rewardTeam(_topTeam, bestKiller, looser);
				}
			}
		}
		else
		{
			processTopPlayer();
		}
		
		teleportFinish();
	}
	
	/**
	 * After finish operations.
	 */
	private static void afterFinishOperations()
	{
		
		if (Config.TVT_OPEN_FORT_DOORS)
		{
			openFortDoors();
		}
		
		if (Config.TVT_OPEN_ADEN_COLOSSEUM_DOORS)
		{
			openAdenColosseumDoors();
		}
		
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
			cleanTvT();
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
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": The Event cancelled!");
		teleportFinish();
	}
	
	/**
	 * Teleport finish.
	 */
	public static void teleportFinish()
	{
		sit();
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Teleport back to participation NPC in 20 seconds!");
		
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
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
				cleanTvT();
			}
		}, 20000);
	}
	
	protected static class AutoEventTask implements Runnable
	{
		@Override
		public void run()
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
					waiter(30 * 1000); // 30 sec wait time until start fight after teleported
					if (startEvent() && !_aborted)
					{
						LOG.warn(_eventName + ": waiting.....minutes for event time " + _eventTime);
						
						waiter(_eventTime * 60 * 1000); // minutes for event time
						finishEvent();
						
						LOG.info(_eventName + ": waiting... delay for final messages ");
						waiter(60000);// just a give a delay delay for final messages
						sendFinalMessages();
						
						/*
						 * if (!_started && !_aborted) { // if is not already started and it's not aborted LOG.info(_eventName + ": waiting.....delay for restart event  " + _intervalBetweenMatchs + " minutes."); waiter(60000);// just a give a delay to next restart try { if (!_aborted)
						 * restartEvent(); } catch (final Exception e) { LOG.error("Error while tying to Restart Event", e); e.printStackTrace(); } }
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
		
	}
	
	/**
	 * Auto event.
	 */
	public static void autoEvent()
	{
		ThreadPoolManager.getInstance().executeAi(new AutoEventTask());
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
	protected static void waiter(final long interval)
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
			if (_playersShuffle == null || _playersShuffle.isEmpty())
			{
				return;
			}
			else if (_playersShuffle.size() > 0)
			{
				for (final L2PcInstance player : _playersShuffle)
				{
					if (player == null)
					{
						_playersShuffle.remove(player);
					}
					else if (player.isOnline() == 0 || player.isInJail() || player.isInOfflineMode())
					{
						removePlayer(player);
					}
					if (_playersShuffle.size() == 0 || _playersShuffle.isEmpty())
					{
						break;
					}
				}
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
		
		if (Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE"))
		{
			if (_teamPlayersCount.contains(0))
			{
				return false;
			}
		}
		else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE"))
		{
			final Vector<L2PcInstance> playersShuffleTemp = new Vector<>();
			int loopCount = 0;
			
			loopCount = _playersShuffle.size();
			
			for (int i = 0; i < loopCount; i++)
			{
				playersShuffleTemp.add(_playersShuffle.get(i));
			}
			
			_playersShuffle = playersShuffleTemp;
			playersShuffleTemp.clear();
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
	 * @param teamName the team name
	 * @param eventPlayer the event player
	 * @return true, if successful
	 */
	private static boolean addPlayerOk(final String teamName, final L2PcInstance eventPlayer)
	{
		if (checkShufflePlayers(eventPlayer) || eventPlayer._inEventTvT)
		{
			eventPlayer.sendMessage("You already participated in the event!");
			return false;
		}
		
		if (eventPlayer._inEventCTF || eventPlayer._inEventDM)
		{
			eventPlayer.sendMessage("You already participated in another event!");
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
					
					if (player != null && player._inEventTvT)
					{
						eventPlayer.sendMessage("You already participated in event with another char!");
						return false;
					}
				}
			}
			
			/*
			 * eventPlayer.sendMessage("Dual Box not allowed in Events"); return false;
			 */
		}
		
		synchronized (_players)
		{
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
			
			if (_players.contains(eventPlayer))
			{
				eventPlayer.sendMessage("You already participated in the event!");
				return false;
			}
		}
		
		if (Config.TVT_EVEN_TEAMS.equals("NO"))
		{
			return true;
		}
		else if (Config.TVT_EVEN_TEAMS.equals("BALANCE"))
		{
			boolean allTeamsEqual = true;
			int countBefore = -1;
			
			for (final int playersCount : _teamPlayersCount)
			{
				if (countBefore == -1)
				{
					countBefore = playersCount;
				}
				
				if (countBefore != playersCount)
				{
					allTeamsEqual = false;
					break;
				}
				
				countBefore = playersCount;
			}
			
			if (allTeamsEqual)
			{
				return true;
			}
			
			countBefore = Integer.MAX_VALUE;
			
			for (final int teamPlayerCount : _teamPlayersCount)
			{
				if (teamPlayerCount < countBefore)
				{
					countBefore = teamPlayerCount;
				}
			}
			
			final Vector<String> joinableTeams = new Vector<>();
			
			for (final String team : _teams)
			{
				if (teamPlayersCount(team) == countBefore)
				{
					joinableTeams.add(team);
				}
			}
			
			if (joinableTeams.contains(teamName))
			{
				return true;
			}
		}
		else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE"))
		{
			return true;
		}
		
		eventPlayer.sendMessage("Too many players in team \"" + teamName + "\"");
		return false;
	}
	
	/**
	 * Sets the user data.
	 */
	public static void setUserData()
	{
		synchronized (_players)
		{
			for (final L2PcInstance player : _players)
			{
				player._originalNameColorTvT = player.getAppearance().getNameColor();
				player._originalKarmaTvT = player.getKarma();
				player._originalTitleTvT = player.getTitle();
				player.getAppearance().setNameColor(_teamColors.get(_teams.indexOf(player._teamNameTvT)));
				player.setKarma(0);
				player.setTitle("Kills: " + player._countTvTkills);
				if (Config.TVT_AURA)
				{
					if (_teams.size() >= 2)
					{
						player.setTeam(_teams.indexOf(player._teamNameTvT) + 1);
					}
				}
				
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
		LOG.info("##########################");
		LOG.info("# _teams(Vector<String>) #");
		LOG.info("##########################");
		
		for (final String team : _teams)
		{
			LOG.info(team + " Kills Done :" + _teamPointsCount.get(_teams.indexOf(team)));
		}
		
		if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE"))
		{
			LOG.info("");
			LOG.info("#########################################");
			LOG.info("# _playersShuffle(Vector<L2PcInstance>) #");
			LOG.info("#########################################");
			
			for (final L2PcInstance player : _playersShuffle)
			{
				if (player != null)
				{
					LOG.info("Name: " + player.getName());
				}
			}
		}
		
		LOG.info("");
		LOG.info("##################################");
		LOG.info("# _players(Vector<L2PcInstance>) #");
		LOG.info("##################################");
		
		synchronized (_players)
		{
			for (final L2PcInstance player : _players)
			{
				if (player != null)
				{
					LOG.info("Name: " + player.getName() + "   Team: " + player._teamNameTvT + "  Kills Done:" + player._countTvTkills);
				}
			}
		}
		
		LOG.info("");
		LOG.info("#####################################################################");
		LOG.info("# _savePlayers(Vector<String>) and _savePlayerTeams(Vector<String>) #");
		LOG.info("#####################################################################");
		
		for (final String player : _savePlayers)
		{
			LOG.info("Name: " + player + "	Team: " + _savePlayerTeams.get(_savePlayers.indexOf(player)));
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
		_players = new Vector<>();
		
		_topTeam = new String();
		_teams = new Vector<>();
		_savePlayerTeams = new Vector<>();
		_playersShuffle = new Vector<>();
		_teamPlayersCount = new Vector<>();
		_teamPointsCount = new Vector<>();
		_teamColors = new Vector<>();
		_teamsX = new Vector<>();
		_teamsY = new Vector<>();
		_teamsZ = new Vector<>();
		
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
		
		java.sql.Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("Select * from tvt");
			rs = statement.executeQuery();
			
			int teams = 0;
			
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
				teams = rs.getInt("teamsCount");
				_joinTime = rs.getInt("joinTime");
				_eventTime = rs.getInt("eventTime");
				_minPlayers = rs.getInt("minPlayers");
				_maxPlayers = rs.getInt("maxPlayers");
				_intervalBetweenMatchs = rs.getLong("delayForNextEvent");
			}
			DatabaseUtils.close(statement);
			
			int index = -1;
			if (teams > 0)
			{
				index = 0;
			}
			while (index < teams && index > -1)
			{
				statement = con.prepareStatement("Select * from tvt_teams where teamId = ?");
				statement.setInt(1, index);
				rs = statement.executeQuery();
				while (rs.next())
				{
					_teams.add(rs.getString("teamName"));
					_teamPlayersCount.add(0);
					_teamPointsCount.add(0);
					_teamColors.add(0);
					_teamsX.add(0);
					_teamsY.add(0);
					_teamsZ.add(0);
					_teamsX.set(index, rs.getInt("teamX"));
					_teamsY.set(index, rs.getInt("teamY"));
					_teamsZ.set(index, rs.getInt("teamZ"));
					_teamColors.set(index, rs.getInt("teamColor"));
					
				}
				index++;
				DatabaseUtils.close(statement);
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Exception: loadData(): " + e.getMessage());
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
			
			statement = con.prepareStatement("Delete from tvt");
			statement.execute();
			DatabaseUtils.close(statement);
			
			statement = con.prepareStatement("INSERT INTO tvt (eventName, eventDesc, joiningLocation, minlvl, maxlvl, npcId, npcX, npcY, npcZ, npcHeading, rewardId, rewardAmount, teamsCount, joinTime, eventTime, minPlayers, maxPlayers,delayForNextEvent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)");
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
			statement.setInt(13, _teams.size());
			statement.setInt(14, _joinTime);
			statement.setInt(15, _eventTime);
			statement.setInt(16, _minPlayers);
			statement.setInt(17, _maxPlayers);
			statement.setLong(18, _intervalBetweenMatchs);
			statement.execute();
			DatabaseUtils.close(statement);
			
			statement = con.prepareStatement("Delete from tvt_teams");
			statement.execute();
			DatabaseUtils.close(statement);
			
			for (final String teamName : _teams)
			{
				final int index = _teams.indexOf(teamName);
				
				if (index == -1)
				{
					CloseUtil.close(con);
					con = null;
					return;
				}
				statement = con.prepareStatement("INSERT INTO tvt_teams (teamId ,teamName, teamX, teamY, teamZ, teamColor) VALUES (?, ?, ?, ?, ?, ?)");
				statement.setInt(1, index);
				statement.setString(2, teamName);
				statement.setInt(3, _teamsX.get(index));
				statement.setInt(4, _teamsY.get(index));
				statement.setInt(5, _teamsZ.get(index));
				statement.setInt(6, _teamColors.get(index));
				
				statement.execute();
				DatabaseUtils.close(statement);
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Exception: saveData(): " + e.getMessage());
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
			
			if (!_started && !_joining)
			{
				replyMSG.append("<center>Wait till the admin/gm start the participation.</center>");
			}
			else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE") && !checkMaxPlayers(_playersShuffle.size()))
			{
				if (!_started)
				{
					replyMSG.append("Currently participated: <font color=\"00FF00\">" + _playersShuffle.size() + ".</font><br>");
					replyMSG.append("Max players: <font color=\"00FF00\">" + _maxPlayers + "</font><br><br>");
					replyMSG.append("<font color=\"FFFF00\">You can't participate to this event.</font><br>");
				}
			}
			else if (eventPlayer.isCursedWeaponEquiped() && !Config.TVT_JOIN_CURSED)
			{
				replyMSG.append("<font color=\"FFFF00\">You can't participate to this event with a cursed Weapon.</font><br>");
			}
			else if (!_started && _joining && eventPlayer.getLevel() >= _minlvl && eventPlayer.getLevel() <= _maxlvl)
			{
				synchronized (_players)
				{
					if (_players.contains(eventPlayer) || _playersShuffle.contains(eventPlayer) || checkShufflePlayers(eventPlayer))
					{
						if (Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE"))
						{
							replyMSG.append("You participated already in team <font color=\"LEVEL\">" + eventPlayer._teamNameTvT + "</font><br><br>");
						}
						else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE"))
						{
							replyMSG.append("<center><font color=\"3366CC\">You participated already!</font></center><br><br>");
						}
						
						replyMSG.append("<center>Joined Players: <font color=\"00FF00\">" + _playersShuffle.size() + "</font></center><br>");
						
						replyMSG.append("<center><font color=\"3366CC\">Wait till event start or remove your participation!</font><center>");
						replyMSG.append("<center><button value=\"Remove\" action=\"bypass -h npc_" + objectId + "_tvt_player_leave\" width=85 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center>");
					}
					else
					{
						replyMSG.append("<center><font color=\"3366CC\">You want to participate in the event?</font></center><br>");
						replyMSG.append("<center><td width=\"200\">Min lvl: <font color=\"00FF00\">" + _minlvl + "</font></center></td><br>");
						replyMSG.append("<center><td width=\"200\">Max lvl: <font color=\"00FF00\">" + _maxlvl + "</font></center></td><br><br>");
						replyMSG.append("<center><font color=\"3366CC\">Teams:</font></center><br>");
						
						if (Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE"))
						{
							replyMSG.append("<center><table border=\"0\">");
							
							for (final String team : _teams)
							{
								replyMSG.append("<tr><td width=\"100\"><font color=\"LEVEL\">" + team + "</font>&nbsp;(" + teamPlayersCount(team) + " joined)</td>");
								replyMSG.append("<center><td width=\"60\"><button value=\"Join\" action=\"bypass -h npc_" + objectId + "_tvt_player_join " + team + "\" width=85 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center></td></tr>");
							}
							replyMSG.append("</table></center>");
						}
						else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE"))
						{
							replyMSG.append("<center>");
							
							for (final String team : _teams)
							{
								replyMSG.append("<tr><td width=\"100\"><font color=\"LEVEL\">" + team + "</font> &nbsp;</td>");
							}
							
							replyMSG.append("</center><br>");
							
							replyMSG.append("<center><button value=\"Join Event\" action=\"bypass -h npc_" + objectId + "_tvt_player_join eventShuffle\" width=85 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center>");
							replyMSG.append("<center><font color=\"3366CC\">Teams will be reandomly generated!</font></center><br>");
							replyMSG.append("<center>Joined Players:</font> <font color=\"LEVEL\">" + _playersShuffle.size() + "</center></font><br>");
							replyMSG.append("<center>Reward: <font color=\"LEVEL\">" + _rewardAmount + " " + ItemTable.getInstance().getTemplate(_rewardId).getName() + "</center></font>");
						}
					}
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
	
	/**
	 * Adds the player.
	 * @param player the player
	 * @param teamName the team name
	 */
	public static void addPlayer(final L2PcInstance player, final String teamName)
	{
		if (!addPlayerOk(teamName, player))
		{
			return;
		}
		
		synchronized (_players)
		{
			if (Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE"))
			{
				player._teamNameTvT = teamName;
				_players.add(player);
				setTeamPlayersCount(teamName, teamPlayersCount(teamName) + 1);
			}
			else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE"))
			{
				_playersShuffle.add(player);
			}
		}
		
		player._inEventTvT = true;
		player._countTvTkills = 0;
		player.sendMessage(_eventName + ": You successfully registered for the event.");
	}
	
	/**
	 * Removes the player.
	 * @param player the player
	 */
	public static void removePlayer(final L2PcInstance player)
	{
		if (player._inEventTvT)
		{
			if (!_joining)
			{
				player.getAppearance().setNameColor(player._originalNameColorTvT);
				player.setTitle(player._originalTitleTvT);
				player.setKarma(player._originalKarmaTvT);
				if (Config.TVT_AURA)
				{
					if (_teams.size() >= 2)
					{
						player.setTeam(0);// clear aura :P
					}
				}
				player.broadcastUserInfo();
			}
			
			// after remove, all event data must be cleaned in player
			player._originalNameColorTvT = 0;
			player._originalTitleTvT = null;
			player._originalKarmaTvT = 0;
			player._teamNameTvT = new String();
			player._countTvTkills = 0;
			player._inEventTvT = false;
			
			synchronized (_players)
			{
				if ((Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE")) && _players.contains(player))
				{
					setTeamPlayersCount(player._teamNameTvT, teamPlayersCount(player._teamNameTvT) - 1);
					_players.remove(player);
				}
				else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE") && (!_playersShuffle.isEmpty() && _playersShuffle.contains(player)))
				{
					_playersShuffle.remove(player);
				}
				
			}
			
			player.sendMessage("Your participation in the TvT event has been removed.");
		}
	}
	
	/**
	 * Clean tv t.
	 */
	public static void cleanTvT()
	{
		synchronized (_players)
		{
			for (final L2PcInstance player : _players)
			{
				if (player != null)
				{
					
					cleanEventPlayer(player);
					
					removePlayer(player);
					if (_savePlayers.contains(player.getName()))
					{
						_savePlayers.remove(player.getName());
					}
					player._inEventTvT = false;
				}
			}
		}
		
		if (_playersShuffle != null && !_playersShuffle.isEmpty())
		{
			for (final L2PcInstance player : _playersShuffle)
			{
				if (player != null)
				{
					player._inEventTvT = false;
				}
			}
		}
		
		_topKills = 0;
		_topTeam = new String();
		_players = new Vector<>();
		_playersShuffle = new Vector<>();
		_savePlayers = new Vector<>();
		_savePlayerTeams = new Vector<>();
		
		_teamPointsCount = new Vector<>();
		_teamPlayersCount = new Vector<>();
		
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
	
	/**
	 * Adds the disconnected player.
	 * @param player the player
	 */
	public static synchronized void addDisconnectedPlayer(final L2PcInstance player)
	{
		if ((Config.TVT_EVEN_TEAMS.equals("SHUFFLE") && (_teleport || _started)) || (Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE") && (_teleport || _started)))
		{
			if (Config.TVT_ON_START_REMOVE_ALL_EFFECTS)
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
			
			player._teamNameTvT = _savePlayerTeams.get(_savePlayers.indexOf(player.getName()));
			
			synchronized (_players)
			{
				for (final L2PcInstance p : _players)
				{
					if (p == null)
					{
						continue;
					}
					// check by name incase player got new objectId
					else if (p.getName().equals(player.getName()))
					{
						player._originalNameColorTvT = player.getAppearance().getNameColor();
						player._originalTitleTvT = player.getTitle();
						player._originalKarmaTvT = player.getKarma();
						player._inEventTvT = true;
						player._countTvTkills = p._countTvTkills;
						_players.remove(p); // removing old object id from vector
						_players.add(player); // adding new objectId to vector
						break;
					}
				}
			}
			
			player.getAppearance().setNameColor(_teamColors.get(_teams.indexOf(player._teamNameTvT)));
			player.setKarma(0);
			if (Config.TVT_AURA)
			{
				if (_teams.size() >= 2)
				{
					player.setTeam(_teams.indexOf(player._teamNameTvT) + 1);
				}
			}
			player.broadcastUserInfo();
			
			player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameTvT)), _teamsY.get(_teams.indexOf(player._teamNameTvT)), _teamsZ.get(_teams.indexOf(player._teamNameTvT)));
			
			afterAddDisconnectedPlayerOperations(player);
			
		}
	}
	
	/**
	 * After add disconnected player operations.
	 * @param player the player
	 */
	private static void afterAddDisconnectedPlayerOperations(final L2PcInstance player)
	{
		
		// nothing
	}
	
	/**
	 * Shuffle teams.
	 */
	public static void shuffleTeams()
	{
		int teamCount = 0, playersCount = 0;
		
		synchronized (_players)
		{
			for (;;)
			{
				if (_playersShuffle.isEmpty())
				{
					break;
				}
				
				final int playerToAddIndex = Rnd.nextInt(_playersShuffle.size());
				L2PcInstance player = null;
				player = _playersShuffle.get(playerToAddIndex);
				
				_players.add(player);
				_players.get(playersCount)._teamNameTvT = _teams.get(teamCount);
				_savePlayers.add(_players.get(playersCount).getName());
				_savePlayerTeams.add(_teams.get(teamCount));
				playersCount++;
				
				if (teamCount == _teams.size() - 1)
				{
					teamCount = 0;
				}
				else
				{
					teamCount++;
				}
				
				_playersShuffle.remove(playerToAddIndex);
			}
		}
		
	}
	
	// Show loosers and winners animations
	/**
	 * Play kneel animation.
	 * @param teamName the team name
	 */
	public static void playKneelAnimation(final String teamName)
	{
		synchronized (_players)
		{
			for (final L2PcInstance player : _players)
			{
				if (player != null)
				{
					if (!player._teamNameTvT.equals(teamName))
					{
						player.broadcastPacket(new SocialAction(player.getObjectId(), 7));
					}
					else if (player._teamNameTvT.equals(teamName))
					{
						player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
					}
				}
			}
		}
		
	}
	
	/**
	 * Reward team.
	 * @param teamName the team name
	 * @param bestKiller the best killer
	 * @param looser the looser
	 */
	public static void rewardTeam(final String teamName, final L2PcInstance bestKiller, final L2PcInstance looser)
	{
		synchronized (_players)
		{
			for (final L2PcInstance player : _players)
			{
				if (player != null && (player.isOnline() != 0) && (player._inEventTvT) && (!player.equals(looser)) && (player._countTvTkills > 0 || Config.TVT_PRICE_NO_KILLS))
				{
					if ((bestKiller != null) && (bestKiller.equals(player)))
					{
						player.addItem(_eventName + " Event: " + _eventName, _rewardId, _rewardAmount, player, true);
						player.addItem(_eventName + " Event: " + _eventName, Config.TVT_TOP_KILLER_REWARD, Config.TVT_TOP_KILLER_QTY, player, true);
						
					}
					else if (teamName != null && (player._teamNameTvT.equals(teamName)))
					{
						
						player.addItem(_eventName + " Event: " + _eventName, _rewardId, _rewardAmount, player, true);
						
						final NpcHtmlMessage nhm = new NpcHtmlMessage(5);
						final TextBuilder replyMSG = new TextBuilder("");
						
						replyMSG.append("<html><body>");
						replyMSG.append("<font color=\"FFFF00\">Your team wins the event. Look in your inventory for the reward.</font>");
						replyMSG.append("</body></html>");
						
						nhm.setHtml(replyMSG.toString());
						player.sendPacket(nhm);
						
						// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
						player.sendPacket(ActionFailed.STATIC_PACKET);
						
					}
					else if (teamName == null)
					{ // TIE
						
						int minus_reward = 0;
						if (_topKills != 0)
						{
							minus_reward = _rewardAmount / 2;
						}
						else
						{
							// nobody killed
							minus_reward = _rewardAmount / 4;
						}
						
						player.addItem(_eventName + " Event: " + _eventName, _rewardId, minus_reward, player, true);
						
						final NpcHtmlMessage nhm = new NpcHtmlMessage(5);
						final TextBuilder replyMSG = new TextBuilder("");
						
						replyMSG.append("<html><body>");
						replyMSG.append("<font color=\"FFFF00\">Your team had a tie in the event. Look in your inventory for the reward.</font>");
						replyMSG.append("</body></html>");
						
						nhm.setHtml(replyMSG.toString());
						player.sendPacket(nhm);
						
						// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
						player.sendPacket(ActionFailed.STATIC_PACKET);
						
					}
				}
			}
		}
		
	}
	
	/**
	 * Process top player.
	 */
	private static void processTopPlayer()
	{
		//
	}
	
	/**
	 * Process top team.
	 */
	private static void processTopTeam()
	{
		_topTeam = null;
		for (final String team : _teams)
		{
			if (teamKillsCount(team) == _topKills && _topKills > 0)
			{
				_topTeam = null;
			}
			
			if (teamKillsCount(team) > _topKills)
			{
				_topTeam = team;
				_topKills = teamKillsCount(team);
			}
		}
	}
	
	/**
	 * Adds the team.
	 * @param teamName the team name
	 */
	public static void addTeam(final String teamName)
	{
		if (is_inProgress())
		{
			if (Config.DEBUG)
			{
				LOG.warn(_eventName + " Engine[addTeam(" + teamName + ")]: checkTeamOk() = false");
			}
			return;
		}
		
		if (teamName.equals(" "))
		{
			return;
		}
		
		_teams.add(teamName);
		_teamPlayersCount.add(0);
		_teamPointsCount.add(0);
		_teamColors.add(0);
		_teamsX.add(0);
		_teamsY.add(0);
		_teamsZ.add(0);
		
		addTeamEventOperations(teamName);
		
	}
	
	/**
	 * Adds the team event operations.
	 * @param teamName the team name
	 */
	private static void addTeamEventOperations(final String teamName)
	{
		
		// nothing
		
	}
	
	/**
	 * Removes the team.
	 * @param teamName the team name
	 */
	public static void removeTeam(final String teamName)
	{
		if (is_inProgress() || _teams.isEmpty())
		{
			if (Config.DEBUG)
			{
				LOG.warn(_eventName + " Engine[removeTeam(" + teamName + ")]: checkTeamOk() = false");
			}
			return;
		}
		
		if (teamPlayersCount(teamName) > 0)
		{
			if (Config.DEBUG)
			{
				LOG.warn(_eventName + " Engine[removeTeam(" + teamName + ")]: teamPlayersCount(teamName) > 0");
			}
			return;
		}
		
		final int index = _teams.indexOf(teamName);
		
		if (index == -1)
		{
			return;
		}
		
		_teamsZ.remove(index);
		_teamsY.remove(index);
		_teamsX.remove(index);
		_teamColors.remove(index);
		_teamPointsCount.remove(index);
		_teamPlayersCount.remove(index);
		_teams.remove(index);
		
		removeTeamEventItems(teamName);
		
	}
	
	/**
	 * Removes the team event items.
	 * @param teamName the team name
	 */
	private static void removeTeamEventItems(final String teamName)
	{
		
		_teams.indexOf(teamName);
		
		//
	}
	
	/**
	 * Sets the team pos.
	 * @param teamName the team name
	 * @param activeChar the active char
	 */
	public static void setTeamPos(final String teamName, final L2PcInstance activeChar)
	{
		final int index = _teams.indexOf(teamName);
		
		if (index == -1)
		{
			return;
		}
		
		_teamsX.set(index, activeChar.getX());
		_teamsY.set(index, activeChar.getY());
		_teamsZ.set(index, activeChar.getZ());
	}
	
	/**
	 * Sets the team pos.
	 * @param teamName the team name
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 */
	public static void setTeamPos(final String teamName, final int x, final int y, final int z)
	{
		final int index = _teams.indexOf(teamName);
		
		if (index == -1)
		{
			return;
		}
		
		_teamsX.set(index, x);
		_teamsY.set(index, y);
		_teamsZ.set(index, z);
	}
	
	/**
	 * Sets the team color.
	 * @param teamName the team name
	 * @param color the color
	 */
	public static void setTeamColor(final String teamName, final int color)
	{
		if (is_inProgress())
		{
			return;
		}
		
		final int index = _teams.indexOf(teamName);
		
		if (index == -1)
		{
			return;
		}
		
		_teamColors.set(index, color);
	}
	
	/**
	 * Team players count.
	 * @param teamName the team name
	 * @return the int
	 */
	public static int teamPlayersCount(final String teamName)
	{
		final int index = _teams.indexOf(teamName);
		
		if (index == -1)
		{
			return -1;
		}
		
		return _teamPlayersCount.get(index);
	}
	
	/**
	 * Sets the team players count.
	 * @param teamName the team name
	 * @param teamPlayersCount the team players count
	 */
	public static void setTeamPlayersCount(final String teamName, final int teamPlayersCount)
	{
		final int index = _teams.indexOf(teamName);
		
		if (index == -1)
		{
			return;
		}
		
		_teamPlayersCount.set(index, teamPlayersCount);
	}
	
	/**
	 * Check shuffle players.
	 * @param eventPlayer the event player
	 * @return true, if successful
	 */
	public static boolean checkShufflePlayers(final L2PcInstance eventPlayer)
	{
		try
		{
			for (final L2PcInstance player : _playersShuffle)
			{
				if (player == null || player.isOnline() == 0)
				{
					_playersShuffle.remove(player);
					eventPlayer._inEventTvT = false;
					continue;
				}
				else if (player.getObjectId() == eventPlayer.getObjectId())
				{
					eventPlayer._inEventTvT = true;
					eventPlayer._countTvTkills = 0;
					return true;
				}
				
				// This 1 is incase player got new objectid after DC or reconnect
				else if (player.getName().equals(eventPlayer.getName()))
				{
					_playersShuffle.remove(player);
					_playersShuffle.add(eventPlayer);
					eventPlayer._inEventTvT = true;
					eventPlayer._countTvTkills = 0;
					return true;
				}
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * just an announcer to send termination messages.
	 */
	public static void sendFinalMessages()
	{
		if (!_started && !_aborted)
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Thank you For Participating At, " + _eventName + " Event.");
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
	
	@Override
	public void run()
	{
		LOG.info(_eventName + ": Event notification start");
		eventOnceStart();
	}
	
	@Override
	public String getEventIdentifier()
	{
		return _eventName;
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
	 * Sets the event start time.
	 * @param newTime the new event start time
	 */
	public void setEventStartTime(final String newTime)
	{
		startEventTime = newTime;
	}
	
	/**
	 * On disconnect.
	 * @param player the player
	 */
	public static void onDisconnect(final L2PcInstance player)
	{
		
		if (player._inEventTvT)
		{
			removePlayer(player);
			player.teleToLocation(_npcX, _npcY, _npcZ);
		}
	}
	
	/**
	 * Team kills count.
	 * @param teamName the team name
	 * @return the int
	 */
	public static int teamKillsCount(final String teamName)
	{
		final int index = _teams.indexOf(teamName);
		
		if (index == -1)
		{
			return -1;
		}
		
		return _teamPointsCount.get(index);
	}
	
	/**
	 * Sets the team kills count.
	 * @param teamName the team name
	 * @param teamKillsCount the team kills count
	 */
	public static void setTeamKillsCount(final String teamName, final int teamKillsCount)
	{
		final int index = _teams.indexOf(teamName);
		
		if (index == -1)
		{
			return;
		}
		
		_teamPointsCount.set(index, teamKillsCount);
	}
	
	/**
	 * Kick player from tvt.
	 * @param playerToKick the player to kick
	 */
	public static void kickPlayerFromTvt(final L2PcInstance playerToKick)
	{
		if (playerToKick == null)
		{
			return;
		}
		
		synchronized (_players)
		{
			if (_joining)
			{
				_playersShuffle.remove(playerToKick);
				_players.remove(playerToKick);
				playerToKick._inEventTvT = false;
				playerToKick._teamNameTvT = "";
				playerToKick._countTvTkills = 0;
			}
		}
		
		if (_started || _teleport)
		{
			_playersShuffle.remove(playerToKick);
			// playerToKick._inEventTvT = false;
			removePlayer(playerToKick);
			if (playerToKick.isOnline() != 0)
			{
				playerToKick.getAppearance().setNameColor(playerToKick._originalNameColorTvT);
				playerToKick.setKarma(playerToKick._originalKarmaTvT);
				playerToKick.setTitle(playerToKick._originalTitleTvT);
				playerToKick.broadcastUserInfo();
				playerToKick.sendMessage("You have been kicked from the TvT.");
				playerToKick.teleToLocation(_npcX, _npcY, _npcZ, false);
				playerToKick.teleToLocation(_npcX + Rnd.get(201) - 100, _npcY + Rnd.get(201) - 100, _npcZ, false);
			}
		}
	}
	
	/**
	 * Find best killer.
	 * @param players the players
	 * @return the l2 pc instance
	 */
	public static L2PcInstance findBestKiller(final Vector<L2PcInstance> players)
	{
		if (players == null)
		{
			return null;
		}
		L2PcInstance bestKiller = null;
		for (final L2PcInstance player : players)
		{
			if ((bestKiller == null) || (bestKiller._countTvTkills < player._countTvTkills))
			{
				bestKiller = player;
			}
		}
		return bestKiller;
	}
	
	/**
	 * Find looser.
	 * @param players the players
	 * @return the l2 pc instance
	 */
	public static L2PcInstance findLooser(final Vector<L2PcInstance> players)
	{
		if (players == null)
		{
			return null;
		}
		L2PcInstance looser = null;
		for (final L2PcInstance player : players)
		{
			if ((looser == null) || (looser._countTvTdies < player._countTvTdies))
			{
				looser = player;
			}
		}
		return looser;
	}
	
	/**
	 * The Class TvTTeam.
	 */
	public static class TvTTeam
	{
		
		/** The kill count. */
		private int killCount = -1;
		
		/** The name. */
		private String name = null;
		
		/**
		 * Instantiates a new tv t team.
		 * @param name the name
		 * @param killCount the kill count
		 */
		TvTTeam(final String name, final int killCount)
		{
			this.killCount = killCount;
			this.name = name;
		}
		
		/**
		 * Gets the kill count.
		 * @return the kill count
		 */
		public int getKillCount()
		{
			return this.killCount;
		}
		
		/**
		 * Sets the kill count.
		 * @param killCount the new kill count
		 */
		public void setKillCount(final int killCount)
		{
			this.killCount = killCount;
		}
		
		/**
		 * Gets the name.
		 * @return the name
		 */
		public String getName()
		{
			return this.name;
		}
		
		/**
		 * Sets the name.
		 * @param name the new name
		 */
		public void setName(final String name)
		{
			this.name = name;
		}
	}
	
	/**
	 * Close fort doors.
	 */
	private static void closeFortDoors()
	{
		DoorTable.getInstance().getDoor(23170004).closeMe();
		DoorTable.getInstance().getDoor(23170005).closeMe();
		DoorTable.getInstance().getDoor(23170002).closeMe();
		DoorTable.getInstance().getDoor(23170003).closeMe();
		DoorTable.getInstance().getDoor(23170006).closeMe();
		DoorTable.getInstance().getDoor(23170007).closeMe();
		DoorTable.getInstance().getDoor(23170008).closeMe();
		DoorTable.getInstance().getDoor(23170009).closeMe();
		DoorTable.getInstance().getDoor(23170010).closeMe();
		DoorTable.getInstance().getDoor(23170011).closeMe();
		
		try
		{
			Thread.sleep(20);
		}
		catch (final InterruptedException ie)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				ie.printStackTrace();
			}
			
			LOG.error("Error, " + ie.getMessage());
		}
	}
	
	/**
	 * Open fort doors.
	 */
	private static void openFortDoors()
	{
		DoorTable.getInstance().getDoor(23170004).openMe();
		DoorTable.getInstance().getDoor(23170005).openMe();
		DoorTable.getInstance().getDoor(23170002).openMe();
		DoorTable.getInstance().getDoor(23170003).openMe();
		DoorTable.getInstance().getDoor(23170006).openMe();
		DoorTable.getInstance().getDoor(23170007).openMe();
		DoorTable.getInstance().getDoor(23170008).openMe();
		DoorTable.getInstance().getDoor(23170009).openMe();
		DoorTable.getInstance().getDoor(23170010).openMe();
		DoorTable.getInstance().getDoor(23170011).openMe();
		
	}
	
	/**
	 * Close aden colosseum doors.
	 */
	private static void closeAdenColosseumDoors()
	{
		DoorTable.getInstance().getDoor(24190002).closeMe();
		DoorTable.getInstance().getDoor(24190003).closeMe();
		
		try
		{
			Thread.sleep(20);
		}
		catch (final InterruptedException ie)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				ie.printStackTrace();
			}
			
			LOG.error("Error, " + ie.getMessage());
		}
	}
	
	/**
	 * Open aden colosseum doors.
	 */
	private static void openAdenColosseumDoors()
	{
		DoorTable.getInstance().getDoor(24190002).openMe();
		DoorTable.getInstance().getDoor(24190003).openMe();
		
	}
}
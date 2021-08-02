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
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2Radar;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.event.manager.EventTask;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.olympiad.Olympiad;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.RadarControl;
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

/**
 * The Class CTF.
 */
public class CTF implements EventTask
{
	/** The Constant LOG. */
	protected static final Logger LOG = LoggerFactory.getLogger(CTF.class);
	
	/** The _joining location name. */
	protected static String _eventName = new String(), _eventDesc = new String(), _joiningLocationName = new String();
	
	/** The _npc spawn. */
	private static L2Spawn _npcSpawn;
	
	/** The _in progress. */
	protected static boolean _joining = false, _teleport = false, _started = false, _aborted = false, _sitForced = false, _inProgress = false;
	
	/** The _max players. */
	protected static int _npcId = 0, _npcX = 0, _npcY = 0, _npcZ = 0, _npcHeading = 0, _rewardId = 0, _rewardAmount = 0, _minlvl = 0, _maxlvl = 0, _joinTime = 0, _eventTime = 0, _minPlayers = 0, _maxPlayers = 0;
	
	/** The _interval between matches. */
	protected static long _intervalBetweenMatches = 0;
	
	/** The start event time. */
	private String startEventTime;
	
	/** The _team event. */
	protected static boolean _teamEvent = true; // TODO to be integrated
	
	/** The _players. */
	public static Vector<L2PcInstance> _players = new Vector<>();
	
	/** The _top team. */
	private static String _topTeam = new String();
	
	/** The _players shuffle. */
	public static Vector<L2PcInstance> _playersShuffle = new Vector<>();
	
	/** The _save player teams. */
	public static Vector<String> _teams = new Vector<>(), _savePlayers = new Vector<>(), _savePlayerTeams = new Vector<>();
	
	/** The _teams z. */
	public static Vector<Integer> _teamPlayersCount = new Vector<>(), _teamColors = new Vector<>(), _teamsX = new Vector<>(), _teamsY = new Vector<>(), _teamsZ = new Vector<>();
	
	/** The _team points count. */
	public static Vector<Integer> _teamPointsCount = new Vector<>();
	
	/** The _top score. */
	public static int _topScore = 0;
	
	/** The _event offset. */
	public static int _eventCenterX = 0, _eventCenterY = 0, _eventCenterZ = 0, _eventOffset = 0;
	
	/** The _ fla g_ i n_ han d_ ite m_ id. */
	private static int _FlagNPC = 35062, _FLAG_IN_HAND_ITEM_ID = 6718;
	
	/** The _flags z. */
	public static Vector<Integer> _flagIds = new Vector<>(), _flagsX = new Vector<>(), _flagsY = new Vector<>(), _flagsZ = new Vector<>();
	
	/** The _throne spawns. */
	public static Vector<L2Spawn> _flagSpawns = new Vector<>(), _throneSpawns = new Vector<>();
	
	/** The _flags taken. */
	public static Vector<Boolean> _flagsTaken = new Vector<>();
	
	/**
	 * Instantiates a new cTF.
	 */
	private CTF()
	{
	}
	
	/**
	 * Gets the new instance.
	 * @return the new instance
	 */
	public static CTF getNewInstance()
	{
		return new CTF();
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
			CTF._eventName = _eventName;
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
			CTF._eventDesc = _eventDesc;
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
			CTF._joiningLocationName = _joiningLocationName;
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
			CTF._npcId = _npcId;
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
			CTF._rewardId = _rewardId;
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
			CTF._rewardAmount = _rewardAmount;
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
			CTF._minlvl = _minlvl;
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
			CTF._maxlvl = _maxlvl;
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
			CTF._joinTime = _joinTime;
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
			CTF._eventTime = _eventTime;
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
			CTF._minPlayers = _minPlayers;
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
			CTF._maxPlayers = _maxPlayers;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _interval between matchs.
	 * @return the _intervalBetweenMatches
	 */
	public static long get_intervalBetweenMatches()
	{
		return _intervalBetweenMatches;
	}
	
	/**
	 * Set_interval between matchs.
	 * @param _intervalBetweenMatches the _intervalBetweenMatches to set
	 * @return true, if successful
	 */
	public static boolean set_intervalBetweenMatches(final long _intervalBetweenMatches)
	{
		if (!is_inProgress())
		{
			CTF._intervalBetweenMatches = _intervalBetweenMatches;
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
		
		try
		{
			if (_flagsX.contains(0) || _flagsY.contains(0) || _flagsZ.contains(0) || _flagIds.contains(0))
			{
				return false;
			}
			if (_flagsX.size() < _teams.size() || _flagsY.size() < _teams.size() || _flagsZ.size() < _teams.size() || _flagIds.size() < _teams.size())
			{
				return false;
			}
		}
		catch (final ArrayIndexOutOfBoundsException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			return false;
		}
		
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
			_npcSpawn.getLastSpawn()._isEventMobCTF = true;
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
		
		if (Config.CTF_ANNOUNCE_REWARD && ItemTable.getInstance().getTemplate(_rewardId) != null)
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Prize: " + ItemTable.getInstance().getTemplate(_rewardId).getName() + " (" + _rewardAmount + ")");
		}
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Recruiting levels: " + _minlvl + "-" + _maxlvl);
		
		if (!Config.TVT_COMMAND)
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Joinable in " + _joiningLocationName + ".");
		}
		
		if (Config.CTF_COMMAND)
		{
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": Commands .ctfjoin .ctfleave .ctfinfo!");
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
			
			if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && checkMinPlayers(_playersShuffle.size()))
			{
				shuffleTeams();
			}
			else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && !checkMinPlayers(_playersShuffle.size()))
			{
				Announcements.getInstance().gameAnnounceToAll(_eventName + ": Not enough players. Minimum: " + _minPlayers);
				
				if (Config.CTF_STATS_LOGGER)
				{
					LOG.info(_eventName + ": Not enough players for event. Min requested: " + _minPlayers + ", Participated: " + _playersShuffle.size());
				}
				
				return false;
			}
			
		}
		else
		{
			
			if (!checkMinPlayers(_players.size()))
			{
				Announcements.getInstance().gameAnnounceToAll(_eventName + ": Not enough players. Minimum: " + _minPlayers);
				
				if (Config.CTF_STATS_LOGGER)
				{
					LOG.info(_eventName + ": Not enough players for event. Min requested: " + _minPlayers + ", Participated: " + _players.size());
				}
				
				return false;
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
				
				synchronized (_players)
				{
					for (final L2PcInstance player : _players)
					{
						if (player != null)
						{
							if (Config.CTF_ON_START_UNSUMMON_PET)
							{
								// Remove Summon's buffs
								if (player.getPet() != null)
								{
									final L2Summon summon = player.getPet();
									for (final L2Effect e : summon.getAllEffects())
									{
										if (e != null)
										{
											e.exit(true);
										}
									}
									
									if (summon instanceof L2PetInstance)
									{
										summon.unSummon(player);
									}
								}
							}
							
							if (Config.CTF_ON_START_REMOVE_ALL_EFFECTS)
							{
								for (final L2Effect e : player.getAllEffects())
								{
									if (e != null)
									{
										e.exit(true);
									}
								}
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
							
							if (_teamEvent)
							{
								final int offset = Config.CTF_SPAWN_OFFSET;
								player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)) + Rnd.get(offset), _teamsY.get(_teams.indexOf(player._teamNameCTF)) + Rnd.get(offset), _teamsZ.get(_teams.indexOf(player._teamNameCTF)));
								
							}
							else
							{
								// player.teleToLocation(_playerX, _playerY, _playerZ);
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
		spawnAllFlags();
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
		
		afterStartOperations();
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Started. Go Capture the Flags!");
		_started = true;
		
		return true;
	}
	
	/**
	 * After start operations.
	 */
	private static void afterStartOperations()
	{
		
		synchronized (_players)
		{
			for (final L2PcInstance player : _players)
			{
				if (player != null)
				{
					player._teamNameHaveFlagCTF = null;
					player._haveFlagCTF = false;
				}
			}
			
		}
		
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
		final long delay = _intervalBetweenMatches;
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": joining period will be avaible again in " + _intervalBetweenMatches + " minute(s)!");
		
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
			
			if (_topScore != 0)
			{
				
				playKneelAnimation(_topTeam);
				
				if (Config.CTF_ANNOUNCE_TEAM_STATS)
				{
					Announcements.getInstance().gameAnnounceToAll(_eventName + " Team Statistics:");
					for (final String team : _teams)
					{
						final int _flags_ = teamPointsCount(team);
						Announcements.getInstance().gameAnnounceToAll(_eventName + ": Team: " + team + " - Flags taken: " + _flags_);
					}
				}
				
				if (_topTeam != null)
				{
					Announcements.getInstance().gameAnnounceToAll(_eventName + ": Team " + _topTeam + " wins the match, with " + _topScore + " flags taken!");
				}
				else
				{
					Announcements.getInstance().gameAnnounceToAll(_eventName + ": The event finished with a TIE: " + _topScore + " flags taken by each team!");
				}
				rewardTeam(_topTeam);
				
				if (Config.CTF_STATS_LOGGER)
				{
					
					LOG.info("**** " + _eventName + " ****");
					LOG.info(_eventName + " Team Statistics:");
					for (final String team : _teams)
					{
						final int _flags_ = teamPointsCount(team);
						LOG.info("Team: " + team + " - Flags taken: " + _flags_);
					}
					
					LOG.info(_eventName + ": Team " + _topTeam + " wins the match, with " + _topScore + " flags taken!");
					
				}
				
			}
			else
			{
				
				Announcements.getInstance().gameAnnounceToAll(_eventName + ": The event finished with a TIE: No team wins the match(nobody took flags)!");
				
				if (Config.CTF_STATS_LOGGER)
				{
					LOG.info(_eventName + ": No team win the match(nobody took flags).");
				}
				
				rewardTeam(_topTeam);
				
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
		unspawnAllFlags();
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
			cleanCTF();
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
		
		unspawnAllFlags();
		
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
				cleanCTF();
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
						 * if (!_started && !_aborted) { // if is not already started and it's not aborted LOG.info(_eventName + ": waiting.....delay for restart event  " + _intervalBetweenMatches + " minutes."); waiter(60000);// just a give a delay to next restart try { if (!_aborted)
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
		
		if (Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE"))
		{
			if (_teamPlayersCount.contains(0))
			{
				return false;
			}
		}
		else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
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
		if (checkShufflePlayers(eventPlayer) || eventPlayer._inEventCTF)
		{
			eventPlayer.sendMessage("You already participated in the event!");
			return false;
		}
		
		if (eventPlayer._inEventTvT || eventPlayer._inEventDM)
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
					
					if (player != null && player._inEventCTF)
					{
						eventPlayer.sendMessage("You already participated in event with another char!");
						return false;
					}
				}
			}
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
		
		if (CTF._savePlayers.contains(eventPlayer.getName()))
		{
			eventPlayer.sendMessage("You already participated in another event!");
			return false;
		}
		
		if (Config.CTF_EVEN_TEAMS.equals("NO"))
		{
			return true;
		}
		else if (Config.CTF_EVEN_TEAMS.equals("BALANCE"))
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
		else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
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
				player._originalNameColorCTF = player.getAppearance().getNameColor();
				player._originalKarmaCTF = player.getKarma();
				player._originalTitleCTF = player.getTitle();
				player.getAppearance().setNameColor(_teamColors.get(_teams.indexOf(player._teamNameCTF)));
				player.setKarma(0);
				if (Config.CTF_AURA)
				{
					if (_teams.size() >= 2)
					{
						player.setTeam(_teams.indexOf(player._teamNameCTF) + 1);
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
			LOG.info(team + " Flags Taken :" + _teamPointsCount.get(_teams.indexOf(team)));
		}
		
		if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
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
					LOG.info("Name: " + player.getName() + "   Team: " + player._teamNameCTF + "  Flags :" + player._countCTFflags);
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
		LOG.info("**********==CTF==************");
		LOG.info("CTF._teamPointsCount:" + _teamPointsCount.toString());
		LOG.info("CTF._flagIds:" + _flagIds.toString());
		LOG.info("CTF._flagSpawns:" + _flagSpawns.toString());
		LOG.info("CTF._throneSpawns:" + _throneSpawns.toString());
		LOG.info("CTF._flagsTaken:" + _flagsTaken.toString());
		LOG.info("CTF._flagsX:" + _flagsX.toString());
		LOG.info("CTF._flagsY:" + _flagsY.toString());
		LOG.info("CTF._flagsZ:" + _flagsZ.toString());
		LOG.info("************EOF**************\n");
		LOG.info("");
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
			_players = new Vector<>();
		}
		
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
		
		_throneSpawns = new Vector<>();
		_flagSpawns = new Vector<>();
		_flagsTaken = new Vector<>();
		_flagIds = new Vector<>();
		_flagsX = new Vector<>();
		_flagsY = new Vector<>();
		_flagsZ = new Vector<>();
		
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
		_topScore = 0;
		_minlvl = 0;
		_maxlvl = 0;
		_joinTime = 0;
		_eventTime = 0;
		_minPlayers = 0;
		_maxPlayers = 0;
		_intervalBetweenMatches = 0;
		
		java.sql.Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("Select * from ctf");
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
				_intervalBetweenMatches = rs.getLong("delayForNextEvent");
			}
			DatabaseUtils.close(statement);
			
			int index = -1;
			if (teams > 0)
			{
				index = 0;
			}
			while (index < teams && index > -1)
			{
				statement = con.prepareStatement("Select * from ctf_teams where teamId = ?");
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
					
					_flagsX.add(0);
					_flagsY.add(0);
					_flagsZ.add(0);
					_flagsX.set(index, rs.getInt("flagX"));
					_flagsY.set(index, rs.getInt("flagY"));
					_flagsZ.set(index, rs.getInt("flagZ"));
					_flagSpawns.add(null);
					_flagIds.add(_FlagNPC);
					_flagsTaken.add(false);
					
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
			
			statement = con.prepareStatement("Delete from ctf");
			statement.execute();
			DatabaseUtils.close(statement);
			
			statement = con.prepareStatement("INSERT INTO ctf (eventName, eventDesc, joiningLocation, minlvl, maxlvl, npcId, npcX, npcY, npcZ, npcHeading, rewardId, rewardAmount, teamsCount, joinTime, eventTime, minPlayers, maxPlayers,delayForNextEvent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
			statement.setLong(18, _intervalBetweenMatches);
			statement.execute();
			DatabaseUtils.close(statement);
			
			statement = con.prepareStatement("Delete from ctf_teams");
			statement.execute();
			DatabaseUtils.close(statement);
			
			for (final String teamName : _teams)
			{
				final int index = _teams.indexOf(teamName);
				
				if (index == -1)
				{
					return;
				}
				statement = con.prepareStatement("INSERT INTO ctf_teams (teamId ,teamName, teamX, teamY, teamZ, teamColor, flagX, flagY, flagZ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
				statement.setInt(1, index);
				statement.setString(2, teamName);
				statement.setInt(3, _teamsX.get(index));
				statement.setInt(4, _teamsY.get(index));
				statement.setInt(5, _teamsZ.get(index));
				statement.setInt(6, _teamColors.get(index));
				
				statement.setInt(7, _flagsX.get(index));
				statement.setInt(8, _flagsY.get(index));
				statement.setInt(9, _flagsZ.get(index));
				
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
			else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && !checkMaxPlayers(_playersShuffle.size()))
			{
				if (!_started)
				{
					replyMSG.append("Currently participated: <font color=\"00FF00\">" + _playersShuffle.size() + ".</font><br>");
					replyMSG.append("Max players: <font color=\"00FF00\">" + _maxPlayers + "</font><br><br>");
					replyMSG.append("<font color=\"FFFF00\">You can't participate to this event.</font><br>");
				}
			}
			else if (eventPlayer.isCursedWeaponEquiped() && !Config.CTF_JOIN_CURSED)
			{
				replyMSG.append("<font color=\"FFFF00\">You can't participate to this event with a cursed Weapon.</font><br>");
			}
			else if (!_started && _joining && eventPlayer.getLevel() >= _minlvl && eventPlayer.getLevel() <= _maxlvl)
			{
				synchronized (_players)
				{
					if (_players.contains(eventPlayer) || _playersShuffle.contains(eventPlayer) || checkShufflePlayers(eventPlayer))
					{
						if (Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE"))
						{
							replyMSG.append("You participated already in team <font color=\"LEVEL\">" + eventPlayer._teamNameCTF + "</font><br><br>");
						}
						else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
						{
							replyMSG.append("<center><font color=\"3366CC\">You participated already!</font></center><br><br>");
						}
						
						replyMSG.append("<center>Joined Players: <font color=\"00FF00\">" + _playersShuffle.size() + "</font></center><br>");
						
						replyMSG.append("<center><font color=\"3366CC\">Wait till event start or remove your participation!</font><center>");
						replyMSG.append("<center><button value=\"Remove\" action=\"bypass -h npc_" + objectId + "_ctf_player_leave\" width=85 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center>");
					}
					else
					{
						replyMSG.append("<center><font color=\"3366CC\">You want to participate in the event?</font></center><br>");
						replyMSG.append("<center><td width=\"200\">Min lvl: <font color=\"00FF00\">" + _minlvl + "</font></center></td><br>");
						replyMSG.append("<center><td width=\"200\">Max lvl: <font color=\"00FF00\">" + _maxlvl + "</font></center></td><br><br>");
						replyMSG.append("<center><font color=\"3366CC\">Teams:</font></center><br>");
						
						if (Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE"))
						{
							replyMSG.append("<center><table border=\"0\">");
							
							for (final String team : _teams)
							{
								replyMSG.append("<tr><td width=\"100\"><font color=\"LEVEL\">" + team + "</font>&nbsp;(" + teamPlayersCount(team) + " joined)</td>");
								replyMSG.append("<center><td width=\"60\"><button value=\"Join\" action=\"bypass -h npc_" + objectId + "_ctf_player_join " + team + "\" width=85 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center></td></tr>");
							}
							replyMSG.append("</table></center>");
						}
						else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
						{
							replyMSG.append("<center>");
							
							for (final String team : _teams)
							{
								replyMSG.append("<tr><td width=\"100\"><font color=\"LEVEL\">" + team + "</font> &nbsp;</td>");
							}
							
							replyMSG.append("</center><br>");
							
							replyMSG.append("<center><button value=\"Join Event\" action=\"bypass -h npc_" + objectId + "_ctf_player_join eventShuffle\" width=85 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center>");
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
			if (Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE"))
			{
				player._teamNameCTF = teamName;
				_players.add(player);
				setTeamPlayersCount(teamName, teamPlayersCount(teamName) + 1);
			}
			else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
			{
				_playersShuffle.add(player);
			}
			
		}
		
		player._inEventCTF = true;
		player._countCTFflags = 0;
		player.sendMessage(_eventName + ": You successfully registered for the event.");
	}
	
	/**
	 * Removes the player.
	 * @param player the player
	 */
	public static void removePlayer(final L2PcInstance player)
	{
		if (player._inEventCTF)
		{
			if (!_joining)
			{
				player.getAppearance().setNameColor(player._originalNameColorCTF);
				player.setTitle(player._originalTitleCTF);
				player.setKarma(player._originalKarmaCTF);
				if (Config.CTF_AURA)
				{
					if (_teams.size() >= 2)
					{
						player.setTeam(0);// clear aura :P
					}
				}
				player.broadcastUserInfo();
			}
			
			// after remove, all event data must be cleaned in player
			player._originalNameColorCTF = 0;
			player._originalTitleCTF = null;
			player._originalKarmaCTF = 0;
			player._teamNameCTF = new String();
			player._countCTFflags = 0;
			player._inEventCTF = false;
			
			synchronized (_players)
			{
				if ((Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE")) && _players.contains(player))
				{
					setTeamPlayersCount(player._teamNameCTF, teamPlayersCount(player._teamNameCTF) - 1);
					_players.remove(player);
				}
				else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && (!_playersShuffle.isEmpty() && _playersShuffle.contains(player)))
				{
					_playersShuffle.remove(player);
				}
				
			}
			
			player.sendMessage("Your participation in the CTF event has been removed.");
		}
	}
	
	/**
	 * Clean ctf.
	 */
	public static void cleanCTF()
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
					player._inEventCTF = false;
				}
			}
		}
		
		if (_playersShuffle != null && !_playersShuffle.isEmpty())
		{
			for (final L2PcInstance player : _playersShuffle)
			{
				if (player != null)
				{
					player._inEventCTF = false;
				}
			}
		}
		
		_topScore = 0;
		_topTeam = new String();
		synchronized (_players)
		{
			_players = new Vector<>();
		}
		
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
		
		_flagSpawns = new Vector<>();
		_flagsTaken = new Vector<>();
		
	}
	
	/**
	 * Clean event player.
	 * @param player the player
	 */
	private static void cleanEventPlayer(final L2PcInstance player)
	{
		
		if (player._haveFlagCTF)
		{
			removeFlagFromPlayer(player);
		}
		else
		{
			player.getInventory().destroyItemByItemId("", CTF._FLAG_IN_HAND_ITEM_ID, 1, player, null);
		}
		player._haveFlagCTF = false;
		
	}
	
	/**
	 * Adds the disconnected player.
	 * @param player the player
	 */
	public static synchronized void addDisconnectedPlayer(final L2PcInstance player)
	{
		if ((Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && (_teleport || _started)) || (Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE") && (_teleport || _started)))
		{
			if (Config.CTF_ON_START_REMOVE_ALL_EFFECTS)
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
			
			player._teamNameCTF = _savePlayerTeams.get(_savePlayers.indexOf(player.getName()));
			
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
						player._originalNameColorCTF = player.getAppearance().getNameColor();
						player._originalTitleCTF = player.getTitle();
						player._originalKarmaCTF = player.getKarma();
						player._inEventCTF = true;
						player._countCTFflags = p._countCTFflags;
						_players.remove(p); // removing old object id from vector
						_players.add(player); // adding new objectId to vector
						break;
					}
				}
			}
			
			player.getAppearance().setNameColor(_teamColors.get(_teams.indexOf(player._teamNameCTF)));
			player.setKarma(0);
			if (Config.CTF_AURA)
			{
				if (_teams.size() >= 2)
				{
					player.setTeam(_teams.indexOf(player._teamNameCTF) + 1);
				}
			}
			player.broadcastUserInfo();
			
			final int offset = Config.CTF_SPAWN_OFFSET;
			player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)) + Rnd.get(offset), _teamsY.get(_teams.indexOf(player._teamNameCTF)) + Rnd.get(offset), _teamsZ.get(_teams.indexOf(player._teamNameCTF)));
			
			afterAddDisconnectedPlayerOperations(player);
			
		}
	}
	
	/**
	 * After add disconnected player operations.
	 * @param player the player
	 */
	private static void afterAddDisconnectedPlayerOperations(final L2PcInstance player)
	{
		
		player._teamNameHaveFlagCTF = null;
		player._haveFlagCTF = false;
		checkRestoreFlags();
		
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
				_players.get(playersCount)._teamNameCTF = _teams.get(teamCount);
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
					if (!player._teamNameCTF.equals(teamName))
					{
						player.broadcastPacket(new SocialAction(player.getObjectId(), 7));
					}
					else if (player._teamNameCTF.equals(teamName))
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
	 */
	public static void rewardTeam(final String teamName)
	{
		synchronized (_players)
		{
			for (final L2PcInstance player : _players)
			{
				if (player != null && (player.isOnline() != 0) && (player._inEventCTF))
				{
					if (teamName != null && (player._teamNameCTF.equals(teamName)))
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
						if (_topScore != 0)
						{
							minus_reward = _rewardAmount / 2;
						}
						else
						{
							// nobody took flags
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
		
		/*
		 * for(L2PcInstance player : _players) { if(player != null && (player.isOnline() != 0) && (player._inEventCTF == true) && (player._teamNameCTF.equals(teamName))) { player.addItem(_eventName+" Event: " + _eventName, _rewardId, _rewardAmount, player, true); NpcHtmlMessage nhm = new
		 * NpcHtmlMessage(5); TextBuilder replyMSG = new TextBuilder(""); replyMSG.append("<html><body>"); replyMSG.append("<font color=\"FFFF00\">Your team wins the event. Look in your inventory for the reward.</font>"); replyMSG.append("</body></html>"); nhm.setHtml(replyMSG.toString());
		 * player.sendPacket(nhm); // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet player.sendPacket( ActionFailed.STATIC_PACKET ); } }
		 */
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
			if (teamPointsCount(team) == _topScore && _topScore > 0)
			{
				_topTeam = null;
			}
			
			if (teamPointsCount(team) > _topScore)
			{
				_topTeam = team;
				_topScore = teamPointsCount(team);
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
		
		addOrSet(_teams.indexOf(teamName), null, false, _FlagNPC, 0, 0, 0);
		
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
		
		final int index = _teams.indexOf(teamName);
		
		_flagSpawns.remove(index);
		_flagsTaken.remove(index);
		_flagIds.remove(index);
		_flagsX.remove(index);
		_flagsY.remove(index);
		_flagsZ.remove(index);
		
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
					eventPlayer._inEventCTF = false;
					continue;
				}
				else if (player.getObjectId() == eventPlayer.getObjectId())
				{
					eventPlayer._inEventCTF = true;
					eventPlayer._countCTFflags = 0;
					return true;
				}
				
				// This 1 is incase player got new objectid after DC or reconnect
				else if (player.getName().equals(eventPlayer.getName()))
				{
					_playersShuffle.remove(player);
					_playersShuffle.add(eventPlayer);
					eventPlayer._inEventCTF = true;
					eventPlayer._countCTFflags = 0;
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
	 * @return the interval between matches
	 */
	public static int getIntervalBetweenMatchs()
	{
		final long actualTime = System.currentTimeMillis();
		final long totalTime = actualTime + _intervalBetweenMatches;
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
		
		if (player._inEventCTF)
		{
			removePlayer(player);
			player.teleToLocation(_npcX, _npcY, _npcZ);
		}
		
	}
	
	/**
	 * Team points count.
	 * @param teamName the team name
	 * @return the int
	 */
	public static int teamPointsCount(final String teamName)
	{
		final int index = _teams.indexOf(teamName);
		
		if (index == -1)
		{
			return -1;
		}
		
		return _teamPointsCount.get(index);
	}
	
	/**
	 * Sets the team points count.
	 * @param teamName the team name
	 * @param teamPointCount the team point count
	 */
	public static void setTeamPointsCount(final String teamName, final int teamPointCount)
	{
		final int index = _teams.indexOf(teamName);
		
		if (index == -1)
		{
			return;
		}
		
		_teamPointsCount.set(index, teamPointCount);
	}
	
	/**
	 * Gets the _event offset.
	 * @return the _eventOffset
	 */
	public static int get_eventOffset()
	{
		return _eventOffset;
	}
	
	/**
	 * Set_event offset.
	 * @param _eventOffset the _eventOffset to set
	 * @return true, if successful
	 */
	public static boolean set_eventOffset(final int _eventOffset)
	{
		if (!is_inProgress())
		{
			CTF._eventOffset = _eventOffset;
			return true;
		}
		return false;
	}
	
	/**
	 * Show flag html.
	 * @param eventPlayer the event player
	 * @param objectId the object id
	 * @param teamName the team name
	 */
	public static void showFlagHtml(final L2PcInstance eventPlayer, final String objectId, final String teamName)
	{
		if (eventPlayer == null)
		{
			return;
		}
		
		try
		{
			final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			
			final TextBuilder replyMSG = new TextBuilder("<html><head><body><center>");
			replyMSG.append("CTF Flag<br><br>");
			replyMSG.append("<font color=\"00FF00\">" + teamName + "'s Flag</font><br1>");
			if (eventPlayer._teamNameCTF != null && eventPlayer._teamNameCTF.equals(teamName))
			{
				replyMSG.append("<font color=\"LEVEL\">This is your Flag</font><br1>");
			}
			else
			{
				replyMSG.append("<font color=\"LEVEL\">Enemy Flag!</font><br1>");
			}
			if (_started)
			{
				processInFlagRange(eventPlayer);
			}
			else
			{
				replyMSG.append("CTF match is not in progress yet.<br>Wait for a GM to start the event<br>");
			}
			replyMSG.append("</center></body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			LOG.info("CTF Engine[showEventHtlm(" + eventPlayer.getName() + ", " + objectId + ")]: exception: " + e.getStackTrace());
		}
	}
	
	/**
	 * Check restore flags.
	 */
	public static void checkRestoreFlags()
	{
		final Vector<Integer> teamsTakenFlag = new Vector<>();
		try
		{
			synchronized (_players)
			{
				for (final L2PcInstance player : _players)
				{
					if (player != null)
					{
						if (player.isOnline() == 0 && player._haveFlagCTF)
						{ // logged off with a flag in his hands
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + player.getName() + " logged off with a CTF flag!");
							player._haveFlagCTF = false;
							if (_teams.indexOf(player._teamNameHaveFlagCTF) >= 0)
							{
								if (_flagsTaken.get(_teams.indexOf(player._teamNameHaveFlagCTF)))
								{
									_flagsTaken.set(_teams.indexOf(player._teamNameHaveFlagCTF), false);
									spawnFlag(player._teamNameHaveFlagCTF);
									Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + player._teamNameHaveFlagCTF + " flag now returned to place.");
								}
							}
							removeFlagFromPlayer(player);
							player._teamNameHaveFlagCTF = null;
							return;
						}
						else if (player._haveFlagCTF)
						{
							teamsTakenFlag.add(_teams.indexOf(player._teamNameHaveFlagCTF));
						}
					}
				}
			}
			
			// Go over the list of ALL teams
			for (final String team : _teams)
			{
				if (team == null)
				{
					continue;
				}
				final int index = _teams.indexOf(team);
				if (!teamsTakenFlag.contains(index))
				{
					if (_flagsTaken.get(index))
					{
						_flagsTaken.set(index, false);
						spawnFlag(team);
						Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + team + " flag returned due to player error.");
					}
				}
			}
			// Check if a player ran away from the event holding a flag:
			synchronized (_players)
			{
				for (final L2PcInstance player : _players)
				{
					if (player != null && player._haveFlagCTF)
					{
						if (isOutsideCTFArea(player))
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + player.getName() + " escaped from the event holding a flag!");
							player._haveFlagCTF = false;
							if (_teams.indexOf(player._teamNameHaveFlagCTF) >= 0)
							{
								if (_flagsTaken.get(_teams.indexOf(player._teamNameHaveFlagCTF)))
								{
									_flagsTaken.set(_teams.indexOf(player._teamNameHaveFlagCTF), false);
									spawnFlag(player._teamNameHaveFlagCTF);
									Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + player._teamNameHaveFlagCTF + " flag now returned to place.");
								}
							}
							removeFlagFromPlayer(player);
							player._teamNameHaveFlagCTF = null;
							player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)), _teamsY.get(_teams.indexOf(player._teamNameCTF)), _teamsZ.get(_teams.indexOf(player._teamNameCTF)));
							player.sendMessage("You have been returned to your team spawn");
							return;
						}
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
			
			LOG.info("CTF.restoreFlags() Error:" + e.toString());
			return;
		}
	}
	
	/**
	 * Adds the flag to player.
	 * @param _player the _player
	 */
	public static void addFlagToPlayer(final L2PcInstance _player)
	{
		// Remove items from the player hands (right, left, both)
		// This is NOT a BUG, I don't want them to see the icon they have 8D
		L2ItemInstance wpn = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			wpn = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
			if (wpn != null)
			{
				_player.getInventory().unEquipItemInBodySlotAndRecord(Inventory.PAPERDOLL_LRHAND);
			}
		}
		else
		{
			_player.getInventory().unEquipItemInBodySlotAndRecord(Inventory.PAPERDOLL_RHAND);
			wpn = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if (wpn != null)
			{
				_player.getInventory().unEquipItemInBodySlotAndRecord(Inventory.PAPERDOLL_LHAND);
			}
		}
		// Add the flag in his hands
		_player.getInventory().equipItem(ItemTable.getInstance().createItem("", CTF._FLAG_IN_HAND_ITEM_ID, 1, _player, null));
		_player.broadcastPacket(new SocialAction(_player.getObjectId(), 16)); // Amazing Glow
		_player._haveFlagCTF = true;
		_player.broadcastUserInfo();
		final CreatureSay cs = new CreatureSay(_player.getObjectId(), 15, ":", "You got it! Run back! ::"); // 8D
		_player.sendPacket(cs);
	}
	
	/**
	 * Removes the flag from player.
	 * @param player the player
	 */
	public static void removeFlagFromPlayer(final L2PcInstance player)
	{
		final L2ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
		player._haveFlagCTF = false;
		if (wpn != null)
		{
			final L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			player.getInventory().destroyItemByItemId("", CTF._FLAG_IN_HAND_ITEM_ID, 1, player, null);
			final InventoryUpdate iu = new InventoryUpdate();
			for (final L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			player.sendPacket(iu);
			player.sendPacket(new ItemList(player, true)); // Get your weapon back now ...
			player.abortAttack();
			player.broadcastUserInfo();
		}
		else
		{
			player.getInventory().destroyItemByItemId("", CTF._FLAG_IN_HAND_ITEM_ID, 1, player, null);
			player.sendPacket(new ItemList(player, true)); // Get your weapon back now ...
			player.abortAttack();
			player.broadcastUserInfo();
		}
	}
	
	/**
	 * Sets the team flag.
	 * @param teamName the team name
	 * @param activeChar the active char
	 */
	public static void setTeamFlag(final String teamName, final L2PcInstance activeChar)
	{
		final int index = _teams.indexOf(teamName);
		
		if (index == -1)
		{
			return;
		}
		addOrSet(_teams.indexOf(teamName), null, false, _FlagNPC, activeChar.getX(), activeChar.getY(), activeChar.getZ());
	}
	
	/**
	 * Spawn all flags.
	 */
	public static void spawnAllFlags()
	{
		while (_flagSpawns.size() < _teams.size())
		{
			_flagSpawns.add(null);
		}
		while (_throneSpawns.size() < _teams.size())
		{
			_throneSpawns.add(null);
		}
		for (final String team : _teams)
		{
			final int index = _teams.indexOf(team);
			final L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_flagIds.get(index));
			final L2NpcTemplate throne = NpcTable.getInstance().getTemplate(32027);
			try
			{
				// Spawn throne
				_throneSpawns.set(index, new L2Spawn(throne));
				_throneSpawns.get(index).setLocx(_flagsX.get(index));
				_throneSpawns.get(index).setLocy(_flagsY.get(index));
				_throneSpawns.get(index).setLocz(_flagsZ.get(index) - 10);
				_throneSpawns.get(index).setAmount(1);
				_throneSpawns.get(index).setHeading(0);
				_throneSpawns.get(index).setRespawnDelay(1);
				SpawnTable.getInstance().addNewSpawn(_throneSpawns.get(index), false);
				_throneSpawns.get(index).init();
				_throneSpawns.get(index).getLastSpawn().getStatus().setCurrentHp(999999999);
				_throneSpawns.get(index).getLastSpawn().deleteMe();
				_throneSpawns.get(index).getLastSpawn().spawnMe(_throneSpawns.get(index).getLastSpawn().getX(), _throneSpawns.get(index).getLastSpawn().getY(), _throneSpawns.get(index).getLastSpawn().getZ());
				_throneSpawns.get(index).getLastSpawn().setTitle(team + " Throne");
				_throneSpawns.get(index).getLastSpawn().broadcastPacket(new MagicSkillUser(_throneSpawns.get(index).getLastSpawn(), _throneSpawns.get(index).getLastSpawn(), 1036, 1, 5500, 1));
				_throneSpawns.get(index).getLastSpawn()._isCTF_throneSpawn = true;
				// Spawn flag
				_flagSpawns.set(index, new L2Spawn(tmpl));
				_flagSpawns.get(index).setLocx(_flagsX.get(index));
				_flagSpawns.get(index).setLocy(_flagsY.get(index));
				_flagSpawns.get(index).setLocz(_flagsZ.get(index));
				_flagSpawns.get(index).setAmount(1);
				_flagSpawns.get(index).setHeading(0);
				_flagSpawns.get(index).setRespawnDelay(1);
				SpawnTable.getInstance().addNewSpawn(_flagSpawns.get(index), false);
				_flagSpawns.get(index).init();
				_flagSpawns.get(index).getLastSpawn().getStatus().setCurrentHp(999999999);
				_flagSpawns.get(index).getLastSpawn().setTitle(team + "'s Flag");
				_flagSpawns.get(index).getLastSpawn()._CTF_FlagTeamName = team;
				_flagSpawns.get(index).getLastSpawn().deleteMe();
				_flagSpawns.get(index).getLastSpawn().spawnMe(_flagSpawns.get(index).getLastSpawn().getX(), _flagSpawns.get(index).getLastSpawn().getY(), _flagSpawns.get(index).getLastSpawn().getZ());
				_flagSpawns.get(index).getLastSpawn()._isCTF_Flag = true;
				calculateOutSideOfCTF(); // Sets event boundaries so players don't run with the flag.
			}
			catch (final Exception e)
			{
				LOG.info("CTF Engine[spawnAllFlags()]: exception: ");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Unspawn all flags.
	 */
	public static void unspawnAllFlags()
	{
		try
		{
			if (_throneSpawns == null || _flagSpawns == null || _teams == null)
			{
				return;
			}
			for (final String team : _teams)
			{
				final int index = _teams.indexOf(team);
				if (_throneSpawns.get(index) != null)
				{
					_throneSpawns.get(index).getLastSpawn().deleteMe();
					_throneSpawns.get(index).stopRespawn();
					SpawnTable.getInstance().deleteSpawn(_throneSpawns.get(index), true);
				}
				if (_flagSpawns.get(index) != null)
				{
					_flagSpawns.get(index).getLastSpawn().deleteMe();
					_flagSpawns.get(index).stopRespawn();
					SpawnTable.getInstance().deleteSpawn(_flagSpawns.get(index), true);
				}
			}
			_throneSpawns.removeAllElements();
		}
		catch (final Exception e)
		{
			LOG.info("CTF Engine[unspawnAllFlags()]: exception: ");
			e.printStackTrace();
		}
	}
	
	/**
	 * Unspawn flag.
	 * @param teamName the team name
	 */
	private static void unspawnFlag(final String teamName)
	{
		final int index = _teams.indexOf(teamName);
		
		_flagSpawns.get(index).getLastSpawn().deleteMe();
		_flagSpawns.get(index).stopRespawn();
		SpawnTable.getInstance().deleteSpawn(_flagSpawns.get(index), true);
	}
	
	/**
	 * Spawn flag.
	 * @param teamName the team name
	 */
	public static void spawnFlag(final String teamName)
	{
		final int index = _teams.indexOf(teamName);
		final L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_flagIds.get(index));
		
		try
		{
			_flagSpawns.set(index, new L2Spawn(tmpl));
			
			_flagSpawns.get(index).setLocx(_flagsX.get(index));
			_flagSpawns.get(index).setLocy(_flagsY.get(index));
			_flagSpawns.get(index).setLocz(_flagsZ.get(index));
			_flagSpawns.get(index).setAmount(1);
			_flagSpawns.get(index).setHeading(0);
			_flagSpawns.get(index).setRespawnDelay(1);
			
			SpawnTable.getInstance().addNewSpawn(_flagSpawns.get(index), false);
			
			_flagSpawns.get(index).init();
			_flagSpawns.get(index).getLastSpawn().getStatus().setCurrentHp(999999999);
			_flagSpawns.get(index).getLastSpawn().setTitle(teamName + "'s Flag");
			_flagSpawns.get(index).getLastSpawn()._CTF_FlagTeamName = teamName;
			_flagSpawns.get(index).getLastSpawn()._isCTF_Flag = true;
			_flagSpawns.get(index).getLastSpawn().deleteMe();
			_flagSpawns.get(index).getLastSpawn().spawnMe(_flagSpawns.get(index).getLastSpawn().getX(), _flagSpawns.get(index).getLastSpawn().getY(), _flagSpawns.get(index).getLastSpawn().getZ());
		}
		catch (final Exception e)
		{
			LOG.info("CTF Engine[spawnFlag(" + teamName + ")]: exception: ");
			e.printStackTrace();
		}
	}
	
	/**
	 * In range of flag.
	 * @param _player the _player
	 * @param flagIndex the flag index
	 * @param offset the offset
	 * @return true, if successful
	 */
	public static boolean InRangeOfFlag(final L2PcInstance _player, final int flagIndex, final int offset)
	{
		if (_player.getX() > CTF._flagsX.get(flagIndex) - offset && _player.getX() < CTF._flagsX.get(flagIndex) + offset && _player.getY() > CTF._flagsY.get(flagIndex) - offset && _player.getY() < CTF._flagsY.get(flagIndex) + offset && _player.getZ() > CTF._flagsZ.get(flagIndex) - offset
			&& _player.getZ() < CTF._flagsZ.get(flagIndex) + offset)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Process in flag range.
	 * @param _player the _player
	 */
	public static void processInFlagRange(final L2PcInstance _player)
	{
		try
		{
			checkRestoreFlags();
			for (final String team : _teams)
			{
				if (team.equals(_player._teamNameCTF))
				{
					final int indexOwn = _teams.indexOf(_player._teamNameCTF);
					
					// If player is near his team flag holding the enemy flag
					if (InRangeOfFlag(_player, indexOwn, 100) && !_flagsTaken.get(indexOwn) && _player._haveFlagCTF)
					{
						final int indexEnemy = _teams.indexOf(_player._teamNameHaveFlagCTF);
						// Return enemy flag to place
						_flagsTaken.set(indexEnemy, false);
						spawnFlag(_player._teamNameHaveFlagCTF);
						// Remove the flag from this player
						_player.broadcastPacket(new SocialAction(_player.getObjectId(), 16)); // Amazing Glow
						_player.broadcastUserInfo();
						_player.broadcastPacket(new SocialAction(_player.getObjectId(), 3)); // Victory
						_player.broadcastUserInfo();
						removeFlagFromPlayer(_player);
						_teamPointsCount.set(indexOwn, teamPointsCount(team) + 1);
						Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + _player.getName() + " scores for " + _player._teamNameCTF + ".");
					}
				}
				else
				{
					final int indexEnemy = _teams.indexOf(team);
					// If the player is near a enemy flag
					if (InRangeOfFlag(_player, indexEnemy, 100) && !_flagsTaken.get(indexEnemy) && !_player._haveFlagCTF && !_player.isDead())
					{
						_flagsTaken.set(indexEnemy, true);
						unspawnFlag(team);
						_player._teamNameHaveFlagCTF = team;
						addFlagToPlayer(_player);
						_player.broadcastUserInfo();
						_player._haveFlagCTF = true;
						Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + team + " flag taken by " + _player.getName() + "...");
						pointTeamTo(_player, team);
						break;
					}
				}
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * Point team to.
	 * @param hasFlag the has flag
	 * @param ourFlag the our flag
	 */
	public static void pointTeamTo(final L2PcInstance hasFlag, final String ourFlag)
	{
		try
		{
			synchronized (_players)
			{
				for (final L2PcInstance player : _players)
				{
					if (player != null && player.isOnline() != 0)
					{
						if (player._teamNameCTF.equals(ourFlag))
						{
							player.sendMessage(hasFlag.getName() + " took your flag!");
							if (player._haveFlagCTF)
							{
								player.sendMessage("You can not return the flag to headquarters, until your flag is returned to it's place.");
								player.sendPacket(new RadarControl(1, 1, player.getX(), player.getY(), player.getZ()));
							}
							else
							{
								player.sendPacket(new RadarControl(0, 1, hasFlag.getX(), hasFlag.getY(), hasFlag.getZ()));
								final L2Radar rdr = new L2Radar(player);
								final L2Radar.RadarOnPlayer radar = rdr.new RadarOnPlayer(hasFlag, player);
								ThreadPoolManager.getInstance().scheduleGeneral(radar, 10000 + Rnd.get(30000));
							}
						}
					}
				}
			}
			
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * Adds the or set.
	 * @param listSize the list size
	 * @param flagSpawn the flag spawn
	 * @param flagsTaken the flags taken
	 * @param flagId the flag id
	 * @param flagX the flag x
	 * @param flagY the flag y
	 * @param flagZ the flag z
	 */
	private static void addOrSet(final int listSize, final L2Spawn flagSpawn, final boolean flagsTaken, final int flagId, final int flagX, final int flagY, final int flagZ)
	{
		while (_flagsX.size() <= listSize)
		{
			_flagSpawns.add(null);
			_flagsTaken.add(false);
			_flagIds.add(_FlagNPC);
			_flagsX.add(0);
			_flagsY.add(0);
			_flagsZ.add(0);
		}
		_flagSpawns.set(listSize, flagSpawn);
		_flagsTaken.set(listSize, flagsTaken);
		_flagIds.set(listSize, flagId);
		_flagsX.set(listSize, flagX);
		_flagsY.set(listSize, flagY);
		_flagsZ.set(listSize, flagZ);
	}
	
	/**
	 * Used to calculate the event CTF area, so that players don't run off with the flag. Essential, since a player may take the flag just so other teams can't score points. This function is Only called upon ONE time on BEGINING OF EACH EVENT right after we spawn the flags.
	 */
	private static void calculateOutSideOfCTF()
	{
		if (_teams == null || _flagSpawns == null || _teamsX == null || _teamsY == null || _teamsZ == null)
		{
			return;
		}
		final int division = _teams.size() * 2;
		int pos = 0;
		final int[] locX = new int[division], locY = new int[division], locZ = new int[division];
		// Get all coordinates inorder to create a polygon:
		for (final L2Spawn flag : _flagSpawns)
		{
			if (flag == null)
			{
				continue;
			}
			
			locX[pos] = flag.getLocx();
			locY[pos] = flag.getLocy();
			locZ[pos] = flag.getLocz();
			pos++;
			if (pos > division / 2)
			{
				break;
			}
		}
		for (int x = 0; x < _teams.size(); x++)
		{
			locX[pos] = _teamsX.get(x);
			locY[pos] = _teamsY.get(x);
			locZ[pos] = _teamsZ.get(x);
			pos++;
			if (pos > division)
			{
				break;
			}
		}
		// Find the polygon center, note that it's not the mathematical center of the polygon,
		// Rather than a point which centers all coordinates:
		int centerX = 0, centerY = 0, centerZ = 0;
		for (int x = 0; x < pos; x++)
		{
			centerX += (locX[x] / division);
			centerY += (locY[x] / division);
			centerZ += (locZ[x] / division);
		}
		// Now let's find the furthest distance from the "center" to the egg shaped sphere
		// Surrounding the polygon, size x1.5 (for maximum logical area to wander...):
		int maxX = 0, maxY = 0, maxZ = 0;
		for (int x = 0; x < pos; x++)
		{
			if (maxX < 2 * Math.abs(centerX - locX[x]))
			{
				maxX = (2 * Math.abs(centerX - locX[x]));
			}
			if (maxY < 2 * Math.abs(centerY - locY[x]))
			{
				maxY = (2 * Math.abs(centerY - locY[x]));
			}
			if (maxZ < 2 * Math.abs(centerZ - locZ[x]))
			{
				maxZ = (2 * Math.abs(centerZ - locZ[x]));
			}
		}
		
		// CenterX,centerY,centerZ are the coordinates of the "event center".
		// So let's save those coordinates to check on the players:
		_eventCenterX = centerX;
		_eventCenterY = centerY;
		_eventCenterZ = centerZ;
		_eventOffset = maxX;
		if (_eventOffset < maxY)
		{
			_eventOffset = maxY;
		}
		if (_eventOffset < maxZ)
		{
			_eventOffset = maxZ;
		}
	}
	
	/**
	 * Checks if is outside ctf area.
	 * @param _player the _player
	 * @return true, if is outside ctf area
	 */
	public static boolean isOutsideCTFArea(final L2PcInstance _player)
	{
		if (_player == null || _player.isOnline() == 0)
		{
			return true;
		}
		if (!(_player.getX() > _eventCenterX - _eventOffset && _player.getX() < _eventCenterX + _eventOffset && _player.getY() > _eventCenterY - _eventOffset && _player.getY() < _eventCenterY + _eventOffset && _player.getZ() > _eventCenterZ - _eventOffset
			&& _player.getZ() < _eventCenterZ + _eventOffset))
		{
			return true;
		}
		return false;
	}
	
}
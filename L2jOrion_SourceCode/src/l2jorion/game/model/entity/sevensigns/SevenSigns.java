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
package l2jorion.game.model.entity.sevensigns;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.handler.AutoChatHandler;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.AutoSpawnInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.spawn.AutoSpawn;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SignsSky;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.StatsSet;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Broadcast;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class SevenSigns
{
	protected static final Logger LOG = LoggerFactory.getLogger(SevenSigns.class);
	
	/** The _instance. */
	private static SevenSigns _instance;
	
	public static final String SEVEN_SIGNS_DATA_FILE = "config/signs.ini";
	public static final String SEVEN_SIGNS_HTML_PATH = "data/html/seven_signs/";
	
	public static final int CABAL_NULL = 0;
	
	/** The Constant CABAL_DUSK. */
	public static final int CABAL_DUSK = 1;
	
	/** The Constant CABAL_DAWN. */
	public static final int CABAL_DAWN = 2;
	
	/** The Constant SEAL_NULL. */
	public static final int SEAL_NULL = 0;
	
	/** The Constant SEAL_AVARICE. */
	public static final int SEAL_AVARICE = 1;
	
	/** The Constant SEAL_GNOSIS. */
	public static final int SEAL_GNOSIS = 2;
	
	/** The Constant SEAL_STRIFE. */
	public static final int SEAL_STRIFE = 3;
	
	/** The Constant PERIOD_COMP_RECRUITING. */
	public static final int PERIOD_COMP_RECRUITING = 0;
	
	/** The Constant PERIOD_COMPETITION. */
	public static final int PERIOD_COMPETITION = 1;
	
	/** The Constant PERIOD_COMP_RESULTS. */
	public static final int PERIOD_COMP_RESULTS = 2;
	
	/** The Constant PERIOD_SEAL_VALIDATION. */
	public static final int PERIOD_SEAL_VALIDATION = 3;
	
	/** The Constant PERIOD_START_HOUR. */
	public static final int PERIOD_START_HOUR = 18;
	
	/** The Constant PERIOD_START_MINS. */
	public static final int PERIOD_START_MINS = 00;
	
	/** The Constant PERIOD_START_DAY. */
	public static final int PERIOD_START_DAY = Calendar.MONDAY;
	
	// The quest event and seal validation periods last for approximately one week
	// with a 15 minutes "interval" period sandwiched between them.
	/** The Constant PERIOD_MINOR_LENGTH. */
	public static final int PERIOD_MINOR_LENGTH = 900000;
	
	/** The Constant PERIOD_MAJOR_LENGTH. */
	public static final int PERIOD_MAJOR_LENGTH = 604800000 - PERIOD_MINOR_LENGTH;
	
	/** The Constant ANCIENT_ADENA_ID. */
	public static final int ANCIENT_ADENA_ID = 5575;
	
	/** The Constant RECORD_SEVEN_SIGNS_ID. */
	public static final int RECORD_SEVEN_SIGNS_ID = 5707;
	
	/** The Constant CERTIFICATE_OF_APPROVAL_ID. */
	public static final int CERTIFICATE_OF_APPROVAL_ID = 6388;
	
	/** The Constant RECORD_SEVEN_SIGNS_COST. */
	public static final int RECORD_SEVEN_SIGNS_COST = 500;
	
	/** The Constant ADENA_JOIN_DAWN_COST. */
	public static final int ADENA_JOIN_DAWN_COST = 50000;
	
	// NPC Related Constants \\
	/** The Constant ORATOR_NPC_ID. */
	public static final int ORATOR_NPC_ID = 31094;
	
	/** The Constant PREACHER_NPC_ID. */
	public static final int PREACHER_NPC_ID = 31093;
	
	/** The Constant MAMMON_MERCHANT_ID. */
	public static final int MAMMON_MERCHANT_ID = 31113;
	
	/** The Constant MAMMON_BLACKSMITH_ID. */
	public static final int MAMMON_BLACKSMITH_ID = 31126;
	
	/** The Constant MAMMON_MARKETEER_ID. */
	public static final int MAMMON_MARKETEER_ID = 31092;
	
	/** The Constant SPIRIT_IN_ID. */
	public static final int SPIRIT_IN_ID = 31111;
	
	/** The Constant SPIRIT_OUT_ID. */
	public static final int SPIRIT_OUT_ID = 31112;
	
	/** The Constant LILITH_NPC_ID. */
	public static final int LILITH_NPC_ID = 25283;
	
	/** The Constant ANAKIM_NPC_ID. */
	public static final int ANAKIM_NPC_ID = 25286;
	
	/** The Constant CREST_OF_DAWN_ID. */
	public static final int CREST_OF_DAWN_ID = 31170;
	
	/** The Constant CREST_OF_DUSK_ID. */
	public static final int CREST_OF_DUSK_ID = 31171;
	// Seal Stone Related Constants \\
	/** The Constant SEAL_STONE_BLUE_ID. */
	public static final int SEAL_STONE_BLUE_ID = 6360;
	
	/** The Constant SEAL_STONE_GREEN_ID. */
	public static final int SEAL_STONE_GREEN_ID = 6361;
	
	/** The Constant SEAL_STONE_RED_ID. */
	public static final int SEAL_STONE_RED_ID = 6362;
	
	/** The Constant SEAL_STONE_BLUE_VALUE. */
	public static final int SEAL_STONE_BLUE_VALUE = 3;
	
	/** The Constant SEAL_STONE_GREEN_VALUE. */
	public static final int SEAL_STONE_GREEN_VALUE = 5;
	
	/** The Constant SEAL_STONE_RED_VALUE. */
	public static final int SEAL_STONE_RED_VALUE = 10;
	
	/** The Constant BLUE_CONTRIB_POINTS. */
	public static final int BLUE_CONTRIB_POINTS = 3;
	
	/** The Constant GREEN_CONTRIB_POINTS. */
	public static final int GREEN_CONTRIB_POINTS = 5;
	
	/** The Constant RED_CONTRIB_POINTS. */
	public static final int RED_CONTRIB_POINTS = 10;
	
	/** The _calendar. */
	private final Calendar _calendar = Calendar.getInstance();
	
	/** The _active period. */
	protected int _activePeriod;
	
	/** The _current cycle. */
	protected int _currentCycle;
	
	/** The _dawn stone score. */
	protected double _dawnStoneScore;
	
	/** The _dusk stone score. */
	protected double _duskStoneScore;
	
	/** The _dawn festival score. */
	protected int _dawnFestivalScore;
	
	/** The _dusk festival score. */
	protected int _duskFestivalScore;
	
	/** The _comp winner. */
	protected int _compWinner;
	
	/** The _previous winner. */
	protected int _previousWinner;
	
	/** The _signs player data. */
	private final Map<Integer, StatsSet> _signsPlayerData;
	
	/** The _signs seal owners. */
	private final Map<Integer, Integer> _signsSealOwners;
	
	/** The _signs dusk seal totals. */
	private final Map<Integer, Integer> _signsDuskSealTotals;
	
	/** The _signs dawn seal totals. */
	private final Map<Integer, Integer> _signsDawnSealTotals;
	
	/** The _merchant spawn. */
	private static AutoSpawnInstance _merchantSpawn;
	
	/** The _blacksmith spawn. */
	private static AutoSpawnInstance _blacksmithSpawn;
	
	/** The _spirit in spawn. */
	private static AutoSpawnInstance _spiritInSpawn;
	
	/** The _spirit out spawn. */
	private static AutoSpawnInstance _spiritOutSpawn;
	
	/** The _lilith spawn. */
	private static AutoSpawnInstance _lilithSpawn;
	
	/** The _anakim spawn. */
	private static AutoSpawnInstance _anakimSpawn;
	
	/** The _crestofdawnspawn. */
	private static AutoSpawnInstance _crestofdawnspawn;
	
	/** The _crestofduskspawn. */
	private static AutoSpawnInstance _crestofduskspawn;
	
	/** The _orator spawns. */
	private static Map<Integer, AutoSpawnInstance> _oratorSpawns;
	
	/** The _preacher spawns. */
	private static Map<Integer, AutoSpawnInstance> _preacherSpawns;
	
	/** The _marketeer spawns. */
	private static Map<Integer, AutoSpawnInstance> _marketeerSpawns;
	
	/**
	 * Instantiates a new seven signs.
	 */
	public SevenSigns()
	{
		_signsPlayerData = new FastMap<>();
		_signsSealOwners = new FastMap<>();
		_signsDuskSealTotals = new FastMap<>();
		_signsDawnSealTotals = new FastMap<>();
		
		try
		{
			restoreSevenSignsData();
		}
		catch (final Exception e)
		{
			LOG.error("SevenSigns: Failed to load configuration", e);
		}
		
		LOG.info("SevenSigns: Currently in the " + getCurrentPeriodName() + " period");
		initializeSeals();
		
		if (isSealValidationPeriod())
		{
			if (getCabalHighestScore() == CABAL_NULL)
			{
				LOG.info("SevenSigns: The competition ended with a tie last week");
			}
			else
			{
				LOG.info("SevenSigns: The " + getCabalName(getCabalHighestScore()) + " were victorious last week");
			}
		}
		else if (getCabalHighestScore() == CABAL_NULL)
		{
			LOG.info("SevenSigns: The competition, if the current trend continues, will end in a tie this week");
		}
		else
		{
			LOG.info("SevenSigns: The " + getCabalName(getCabalHighestScore()) + " are in the lead this week");
		}
		
		synchronized (this)
		{
			setCalendarForNextPeriodChange();
			final long milliToChange = getMilliToPeriodChange();
			
			SevenSignsPeriodChange sspc = new SevenSignsPeriodChange();
			ThreadPoolManager.getInstance().scheduleGeneral(sspc, milliToChange);
			sspc = null;
			
			// Thanks to http://rainbow.arch.scriptmania.com/scripts/timezone_countdown.html for help with this.
			final double numSecs = milliToChange / 1000 % 60;
			double countDown = (milliToChange / 1000 - numSecs) / 60;
			final int numMins = (int) Math.floor(countDown % 60);
			countDown = (countDown - numMins) / 60;
			final int numHours = (int) Math.floor(countDown % 24);
			final int numDays = (int) Math.floor((countDown - numHours) / 24);
			
			LOG.info("SevenSigns: Next period begins in " + numDays + " days, " + numHours + " hours and " + numMins + " mins");
		}
		
		spawnSevenSignsNPC();
	}
	
	/**
	 * Registers all random spawns and auto-chats for Seven Signs NPCs, along with spawns for the Preachers of Doom and Orators of Revelations at the beginning of the Seal Validation period.
	 */
	public void spawnSevenSignsNPC()
	{
		_merchantSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(MAMMON_MERCHANT_ID, false);
		_blacksmithSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(MAMMON_BLACKSMITH_ID, false);
		_marketeerSpawns = AutoSpawn.getInstance().getAutoSpawnInstances(MAMMON_MARKETEER_ID);
		_spiritInSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(SPIRIT_IN_ID, false);
		_spiritOutSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(SPIRIT_OUT_ID, false);
		_lilithSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(LILITH_NPC_ID, false);
		_anakimSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(ANAKIM_NPC_ID, false);
		_crestofdawnspawn = AutoSpawn.getInstance().getAutoSpawnInstance(CREST_OF_DAWN_ID, false);
		_crestofduskspawn = AutoSpawn.getInstance().getAutoSpawnInstance(CREST_OF_DUSK_ID, false);
		_oratorSpawns = AutoSpawn.getInstance().getAutoSpawnInstances(ORATOR_NPC_ID);
		_preacherSpawns = AutoSpawn.getInstance().getAutoSpawnInstances(PREACHER_NPC_ID);
		
		if (isSealValidationPeriod() || isCompResultsPeriod())
		{
			for (final AutoSpawnInstance spawnInst : _marketeerSpawns.values())
			{
				AutoSpawn.getInstance().setSpawnActive(spawnInst, true);
			}
			
			if (getSealOwner(SEAL_GNOSIS) == getCabalHighestScore() && getSealOwner(SEAL_GNOSIS) != CABAL_NULL)
			{
				if (!Config.ANNOUNCE_MAMMON_SPAWN)
				{
					_blacksmithSpawn.setBroadcast(false);
				}
				
				if (!AutoSpawn.getInstance().getAutoSpawnInstance(_blacksmithSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawn.getInstance().setSpawnActive(_blacksmithSpawn, true);
				}
				
				for (final AutoSpawnInstance spawnInst : _oratorSpawns.values())
				{
					if (!AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawn.getInstance().setSpawnActive(spawnInst, true);
					}
				}
				
				for (final AutoSpawnInstance spawnInst : _preacherSpawns.values())
				{
					if (!AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawn.getInstance().setSpawnActive(spawnInst, true);
					}
				}
				
				if (!AutoChatHandler.getInstance().getAutoChatInstance(PREACHER_NPC_ID, false).isActive() && !AutoChatHandler.getInstance().getAutoChatInstance(ORATOR_NPC_ID, false).isActive())
				{
					AutoChatHandler.getInstance().setAutoChatActive(true);
				}
			}
			else
			{
				AutoSpawn.getInstance().setSpawnActive(_blacksmithSpawn, false);
				
				for (final AutoSpawnInstance spawnInst : _oratorSpawns.values())
				{
					AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
				}
				
				for (final AutoSpawnInstance spawnInst : _preacherSpawns.values())
				{
					AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
				}
				
				AutoChatHandler.getInstance().setAutoChatActive(false);
			}
			
			if (getSealOwner(SEAL_AVARICE) == getCabalHighestScore() && getSealOwner(SEAL_AVARICE) != CABAL_NULL)
			{
				if (!Config.ANNOUNCE_MAMMON_SPAWN)
				{
					_merchantSpawn.setBroadcast(false);
				}
				
				if (!AutoSpawn.getInstance().getAutoSpawnInstance(_merchantSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawn.getInstance().setSpawnActive(_merchantSpawn, true);
				}
				
				if (!AutoSpawn.getInstance().getAutoSpawnInstance(_spiritInSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawn.getInstance().setSpawnActive(_spiritInSpawn, true);
				}
				
				if (!AutoSpawn.getInstance().getAutoSpawnInstance(_spiritOutSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawn.getInstance().setSpawnActive(_spiritOutSpawn, true);
				}
				
				switch (getCabalHighestScore())
				{
					case CABAL_DAWN:
						if (!AutoSpawn.getInstance().getAutoSpawnInstance(_lilithSpawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawn.getInstance().setSpawnActive(_lilithSpawn, true);
						}
						
						AutoSpawn.getInstance().setSpawnActive(_anakimSpawn, false);
						if (!AutoSpawn.getInstance().getAutoSpawnInstance(_crestofdawnspawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawn.getInstance().setSpawnActive(_crestofdawnspawn, true);
						}
						
						AutoSpawn.getInstance().setSpawnActive(_crestofduskspawn, false);
						break;
					
					case CABAL_DUSK:
						if (!AutoSpawn.getInstance().getAutoSpawnInstance(_anakimSpawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawn.getInstance().setSpawnActive(_anakimSpawn, true);
						}
						
						AutoSpawn.getInstance().setSpawnActive(_lilithSpawn, false);
						if (!AutoSpawn.getInstance().getAutoSpawnInstance(_crestofduskspawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawn.getInstance().setSpawnActive(_crestofduskspawn, true);
						}
						
						AutoSpawn.getInstance().setSpawnActive(_crestofdawnspawn, false);
						break;
				}
			}
			else
			{
				AutoSpawn.getInstance().setSpawnActive(_merchantSpawn, false);
				AutoSpawn.getInstance().setSpawnActive(_lilithSpawn, false);
				AutoSpawn.getInstance().setSpawnActive(_anakimSpawn, false);
				AutoSpawn.getInstance().setSpawnActive(_crestofdawnspawn, false);
				AutoSpawn.getInstance().setSpawnActive(_crestofduskspawn, false);
				AutoSpawn.getInstance().setSpawnActive(_spiritInSpawn, false);
				AutoSpawn.getInstance().setSpawnActive(_spiritOutSpawn, false);
			}
		}
		else
		{
			AutoSpawn.getInstance().setSpawnActive(_merchantSpawn, false);
			AutoSpawn.getInstance().setSpawnActive(_blacksmithSpawn, false);
			AutoSpawn.getInstance().setSpawnActive(_lilithSpawn, false);
			AutoSpawn.getInstance().setSpawnActive(_anakimSpawn, false);
			AutoSpawn.getInstance().setSpawnActive(_crestofdawnspawn, false);
			AutoSpawn.getInstance().setSpawnActive(_crestofduskspawn, false);
			AutoSpawn.getInstance().setSpawnActive(_spiritInSpawn, false);
			AutoSpawn.getInstance().setSpawnActive(_spiritOutSpawn, false);
			
			for (final AutoSpawnInstance spawnInst : _oratorSpawns.values())
			{
				AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
			}
			
			for (final AutoSpawnInstance spawnInst : _preacherSpawns.values())
			{
				AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
			}
			
			for (final AutoSpawnInstance spawnInst : _marketeerSpawns.values())
			{
				AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
			}
			
			AutoChatHandler.getInstance().setAutoChatActive(false);
		}
	}
	
	/**
	 * Gets the single instance of SevenSigns.
	 * @return single instance of SevenSigns
	 */
	public static SevenSigns getInstance()
	{
		if (_instance == null)
		{
			_instance = new SevenSigns();
		}
		
		return _instance;
	}
	
	/**
	 * Calc contribution score.
	 * @param blueCount the blue count
	 * @param greenCount the green count
	 * @param redCount the red count
	 * @return the int
	 */
	public static int calcContributionScore(final int blueCount, final int greenCount, final int redCount)
	{
		int contrib = blueCount * BLUE_CONTRIB_POINTS;
		contrib += greenCount * GREEN_CONTRIB_POINTS;
		contrib += redCount * RED_CONTRIB_POINTS;
		
		return contrib;
	}
	
	/**
	 * Calc ancient adena reward.
	 * @param blueCount the blue count
	 * @param greenCount the green count
	 * @param redCount the red count
	 * @return the int
	 */
	public static int calcAncientAdenaReward(final int blueCount, final int greenCount, final int redCount)
	{
		/*
		 * int reward = blueCount * SEAL_STONE_BLUE_VALUE; reward += greenCount * SEAL_STONE_GREEN_VALUE; reward += redCount * SEAL_STONE_RED_VALUE; return reward;
		 */
		return blueCount * SEAL_STONE_BLUE_VALUE + greenCount * SEAL_STONE_GREEN_VALUE + redCount * SEAL_STONE_RED_VALUE;
	}
	
	public static final String getCabalShortName(int cabal2)
	{
		switch (cabal2)
		{
			case CABAL_DAWN:
				return "dawn";
			case CABAL_DUSK:
				return "dusk";
		}
		
		return "No Cabal";
	}
	
	/**
	 * Gets the cabal name.
	 * @param cabal the cabal
	 * @return the cabal name
	 */
	public static final String getCabalName(int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
				return "Lords of Dawn";
			case CABAL_DUSK:
				return "Revolutionaries of Dusk";
		}
		
		return "No Cabal";
	}
	
	/**
	 * Gets the seal name.
	 * @param seal the seal
	 * @param shortName the short name
	 * @return the seal name
	 */
	public static final String getSealName(int seal, boolean shortName)
	{
		String sealName = !shortName ? "Seal of " : "";
		
		switch (seal)
		{
			case SEAL_AVARICE:
				sealName += "Avarice";
				break;
			case SEAL_GNOSIS:
				sealName += "Gnosis";
				break;
			case SEAL_STRIFE:
				sealName += "Strife";
				break;
		}
		
		return sealName;
	}
	
	/**
	 * Gets the current cycle.
	 * @return the current cycle
	 */
	public final int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	/**
	 * Gets the current period.
	 * @return the current period
	 */
	public final int getCurrentPeriod()
	{
		return _activePeriod;
	}
	
	/**
	 * Gets the days to period change.
	 * @return the days to period change
	 */
	private final int getDaysToPeriodChange()
	{
		final int numDays = _calendar.get(Calendar.DAY_OF_WEEK) - PERIOD_START_DAY;
		
		if (numDays < 0)
		{
			return 0 - numDays;
		}
		
		return 7 - numDays;
	}
	
	/**
	 * Gets the milli to period change.
	 * @return the milli to period change
	 */
	public final long getMilliToPeriodChange()
	{
		final long currTimeMillis = System.currentTimeMillis();
		final long changeTimeMillis = _calendar.getTimeInMillis();
		
		return changeTimeMillis - currTimeMillis;
	}
	
	/**
	 * Sets the calendar for next period change.
	 */
	protected void setCalendarForNextPeriodChange()
	{
		// Calculate the number of days until the next period
		// A period starts at 18:00 pm (local time), like on official servers.
		switch (getCurrentPeriod())
		{
			case PERIOD_SEAL_VALIDATION:
			case PERIOD_COMPETITION:
				int daysToChange = getDaysToPeriodChange();
				
				if (daysToChange == 7)
				{
					if (_calendar.get(Calendar.HOUR_OF_DAY) < PERIOD_START_HOUR)
					{
						daysToChange = 0;
					}
					else if (_calendar.get(Calendar.HOUR_OF_DAY) == PERIOD_START_HOUR && _calendar.get(Calendar.MINUTE) < PERIOD_START_MINS)
					{
						daysToChange = 0;
					}
				}
				
				// Otherwise...
				if (daysToChange > 0)
				{
					_calendar.add(Calendar.DATE, daysToChange);
				}
				
				_calendar.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				_calendar.set(Calendar.MINUTE, PERIOD_START_MINS);
				break;
			case PERIOD_COMP_RECRUITING:
			case PERIOD_COMP_RESULTS:
				_calendar.add(Calendar.MILLISECOND, PERIOD_MINOR_LENGTH);
				break;
		}
	}
	
	/**
	 * Gets the current period name.
	 * @return the current period name
	 */
	public final String getCurrentPeriodName()
	{
		String periodName = null;
		
		switch (_activePeriod)
		{
			case PERIOD_COMP_RECRUITING:
				periodName = "Quest Event Initialization";
				break;
			case PERIOD_COMPETITION:
				periodName = "Competition (Quest Event)";
				break;
			case PERIOD_COMP_RESULTS:
				periodName = "Quest Event Results";
				break;
			case PERIOD_SEAL_VALIDATION:
				periodName = "Seal Validation";
				break;
		}
		
		return periodName;
	}
	
	/**
	 * Checks if is seal validation period.
	 * @return true, if is seal validation period
	 */
	public final boolean isSealValidationPeriod()
	{
		return _activePeriod == PERIOD_SEAL_VALIDATION;
	}
	
	/**
	 * Checks if is comp results period.
	 * @return true, if is comp results period
	 */
	public final boolean isCompResultsPeriod()
	{
		return _activePeriod == PERIOD_COMP_RESULTS;
	}
	
	/**
	 * Gets the current score.
	 * @param cabal the cabal
	 * @return the current score
	 */
	public final int getCurrentScore(final int cabal)
	{
		final double totalStoneScore = _dawnStoneScore + _duskStoneScore;
		
		switch (cabal)
		{
			case CABAL_DAWN:
				return Math.round((float) (_dawnStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _dawnFestivalScore;
			case CABAL_DUSK:
				return Math.round((float) (_duskStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _duskFestivalScore;
		}
		
		return 0;
	}
	
	/**
	 * Gets the current stone score.
	 * @param cabal the cabal
	 * @return the current stone score
	 */
	public final double getCurrentStoneScore(final int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
				return _dawnStoneScore;
			case CABAL_DUSK:
				return _duskStoneScore;
		}
		
		return 0;
	}
	
	/**
	 * Gets the current festival score.
	 * @param cabal the cabal
	 * @return the current festival score
	 */
	public final int getCurrentFestivalScore(final int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
				return _dawnFestivalScore;
			case CABAL_DUSK:
				return _duskFestivalScore;
		}
		
		return 0;
	}
	
	/**
	 * Gets the cabal highest score.
	 * @return the cabal highest score
	 */
	public final int getCabalHighestScore()
	{
		if (getCurrentScore(CABAL_DUSK) == getCurrentScore(CABAL_DAWN))
		{
			return CABAL_NULL;
		}
		
		if (getCurrentScore(CABAL_DUSK) > getCurrentScore(CABAL_DAWN))
		{
			return CABAL_DUSK;
		}
		
		return CABAL_DAWN;
	}
	
	/**
	 * Gets the seal owner.
	 * @param seal the seal
	 * @return the seal owner
	 */
	public final int getSealOwner(final int seal)
	{
		return _signsSealOwners.get(seal);
	}
	
	/**
	 * Gets the seal proportion.
	 * @param seal the seal
	 * @param cabal the cabal
	 * @return the seal proportion
	 */
	public final int getSealProportion(final int seal, final int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
				return _signsDawnSealTotals.get(seal);
			case CABAL_DUSK:
				return _signsDuskSealTotals.get(seal);
		}
		
		return 0;
	}
	
	/**
	 * Gets the total members.
	 * @param cabal the cabal
	 * @return the total members
	 */
	public final int getTotalMembers(int cabal)
	{
		int cabalMembers = 0;
		String cabalName = getCabalShortName(cabal);
		
		for (StatsSet sevenDat : _signsPlayerData.values())
		{
			if (sevenDat.getString("cabal").equals(cabalName))
			{
				cabalMembers++;
			}
		}
		
		return cabalMembers;
	}
	
	/**
	 * Gets the player data.
	 * @param player the player
	 * @return the player data
	 */
	public final StatsSet getPlayerData(final L2PcInstance player)
	{
		return _signsPlayerData.get(player.getObjectId());
	}
	
	/**
	 * Gets the player stone contrib.
	 * @param player the player
	 * @return the player stone contrib
	 */
	public int getPlayerStoneContrib(final L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return 0;
		}
		
		int stoneCount = 0;
		
		StatsSet currPlayer = getPlayerData(player);
		
		stoneCount += currPlayer.getInteger("red_stones");
		stoneCount += currPlayer.getInteger("green_stones");
		stoneCount += currPlayer.getInteger("blue_stones");
		
		currPlayer = null;
		
		return stoneCount;
	}
	
	/**
	 * Gets the player contrib score.
	 * @param player the player
	 * @return the player contrib score
	 */
	public int getPlayerContribScore(final L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return 0;
		}
		
		final StatsSet currPlayer = getPlayerData(player);
		
		return currPlayer.getInteger("contribution_score");
	}
	
	/**
	 * Gets the player adena collect.
	 * @param player the player
	 * @return the player adena collect
	 */
	public int getPlayerAdenaCollect(final L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return 0;
		}
		
		return _signsPlayerData.get(player.getObjectId()).getInteger("ancient_adena_amount");
	}
	
	/**
	 * Gets the player seal.
	 * @param player the player
	 * @return the player seal
	 */
	public int getPlayerSeal(final L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return SEAL_NULL;
		}
		
		return getPlayerData(player).getInteger("seal");
	}
	
	/**
	 * Gets the player cabal.
	 * @param player the player
	 * @return the player cabal
	 */
	public int getPlayerCabal(final L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return CABAL_NULL;
		}
		
		String playerCabal = getPlayerData(player).getString("cabal");
		
		if (playerCabal.equalsIgnoreCase("dawn"))
		{
			return CABAL_DAWN;
		}
		else if (playerCabal.equalsIgnoreCase("dusk"))
		{
			playerCabal = null;
			return CABAL_DUSK;
		}
		else
		{
			return CABAL_NULL;
		}
	}
	
	/**
	 * Restores all Seven Signs data and settings, usually called at server startup.
	 */
	protected void restoreSevenSignsData()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT char_obj_id, cabal, seal, red_stones, green_stones, blue_stones, " + "ancient_adena_amount, contribution_score FROM seven_signs");
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				final int charObjId = rset.getInt("char_obj_id");
				
				final StatsSet sevenDat = new StatsSet();
				sevenDat.set("char_obj_id", charObjId);
				sevenDat.set("cabal", rset.getString("cabal"));
				sevenDat.set("seal", rset.getInt("seal"));
				sevenDat.set("red_stones", rset.getInt("red_stones"));
				sevenDat.set("green_stones", rset.getInt("green_stones"));
				sevenDat.set("blue_stones", rset.getInt("blue_stones"));
				sevenDat.set("ancient_adena_amount", rset.getDouble("ancient_adena_amount"));
				sevenDat.set("contribution_score", rset.getDouble("contribution_score"));
				
				if (Config.DEBUG)
				{
					LOG.info("SevenSigns: Loaded data from DB for char ID " + charObjId + " (" + sevenDat.getString("cabal") + ")");
				}
				
				_signsPlayerData.put(charObjId, sevenDat);
			}
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
			
			statement = con.prepareStatement("SELECT * FROM seven_signs_status WHERE id=0");
			rset = statement.executeQuery();
			
			while (rset.next())
			{
				_currentCycle = rset.getInt("current_cycle");
				_activePeriod = rset.getInt("active_period");
				_previousWinner = rset.getInt("previous_winner");
				
				_dawnStoneScore = rset.getDouble("dawn_stone_score");
				_dawnFestivalScore = rset.getInt("dawn_festival_score");
				_duskStoneScore = rset.getDouble("dusk_stone_score");
				_duskFestivalScore = rset.getInt("dusk_festival_score");
				
				_signsSealOwners.put(SEAL_AVARICE, rset.getInt("avarice_owner"));
				_signsSealOwners.put(SEAL_GNOSIS, rset.getInt("gnosis_owner"));
				_signsSealOwners.put(SEAL_STRIFE, rset.getInt("strife_owner"));
				
				_signsDawnSealTotals.put(SEAL_AVARICE, rset.getInt("avarice_dawn_score"));
				_signsDawnSealTotals.put(SEAL_GNOSIS, rset.getInt("gnosis_dawn_score"));
				_signsDawnSealTotals.put(SEAL_STRIFE, rset.getInt("strife_dawn_score"));
				_signsDuskSealTotals.put(SEAL_AVARICE, rset.getInt("avarice_dusk_score"));
				_signsDuskSealTotals.put(SEAL_GNOSIS, rset.getInt("gnosis_dusk_score"));
				_signsDuskSealTotals.put(SEAL_STRIFE, rset.getInt("strife_dusk_score"));
			}
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
			
			statement = con.prepareStatement("UPDATE seven_signs_status SET date=? WHERE id=0");
			statement.setInt(1, Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
			statement.execute();
			
			DatabaseUtils.close(statement);
			
		}
		catch (final SQLException e)
		{
			LOG.error("SevenSigns: Unable to load Seven Signs data from database", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		
		// Festival data is loaded now after the Seven Signs engine data.
	}
	
	/**
	 * Saves all Seven Signs data, both to the database and properties file (if updateSettings = True). Often called to preserve data integrity and synchronization with DB, in case of errors. <BR>
	 * If player != null, just that player's data is updated in the database, otherwise all player's data is sequentially updated.
	 * @param player the player
	 * @param updateSettings the update settings
	 */
	public void saveSevenSignsData(final L2PcInstance player, final boolean updateSettings)
	{
		Connection con = null;
		
		if (Config.DEBUG)
		{
			LOG.info("SevenSigns: Saving data to disk.");
		}
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			
			for (final StatsSet sevenDat : _signsPlayerData.values())
			{
				if (player != null)
				{
					if (sevenDat.getInteger("char_obj_id") != player.getObjectId())
					{
						continue;
					}
				}
				
				statement = con.prepareStatement("UPDATE seven_signs SET cabal=?, seal=?, red_stones=?, " + "green_stones=?, blue_stones=?, " + "ancient_adena_amount=?, contribution_score=? " + "WHERE char_obj_id=?");
				statement.setString(1, sevenDat.getString("cabal"));
				statement.setInt(2, sevenDat.getInteger("seal"));
				statement.setInt(3, sevenDat.getInteger("red_stones"));
				statement.setInt(4, sevenDat.getInteger("green_stones"));
				statement.setInt(5, sevenDat.getInteger("blue_stones"));
				statement.setDouble(6, sevenDat.getDouble("ancient_adena_amount"));
				statement.setDouble(7, sevenDat.getDouble("contribution_score"));
				statement.setInt(8, sevenDat.getInteger("char_obj_id"));
				statement.execute();
				
				DatabaseUtils.close(statement);
				
				if (Config.DEBUG)
				{
					LOG.info("SevenSigns: Updated data in database for char ID " + sevenDat.getInteger("char_obj_id") + " (" + sevenDat.getString("cabal") + ")");
				}
			}
			
			if (updateSettings)
			{
				String sqlQuery = "UPDATE seven_signs_status SET current_cycle=?, active_period=?, previous_winner=?, " + "dawn_stone_score=?, dawn_festival_score=?, dusk_stone_score=?, dusk_festival_score=?, "
					+ "avarice_owner=?, gnosis_owner=?, strife_owner=?, avarice_dawn_score=?, gnosis_dawn_score=?, " + "strife_dawn_score=?, avarice_dusk_score=?, gnosis_dusk_score=?, strife_dusk_score=?, " + "festival_cycle=?, ";
				
				for (int i = 0; i < SevenSignsFestival.FESTIVAL_COUNT; i++)
				{
					sqlQuery += "accumulated_bonus" + String.valueOf(i) + "=?, ";
				}
				
				sqlQuery += "date=? WHERE id=0";
				
				statement = con.prepareStatement(sqlQuery);
				statement.setInt(1, _currentCycle);
				statement.setInt(2, _activePeriod);
				statement.setInt(3, _previousWinner);
				statement.setDouble(4, _dawnStoneScore);
				statement.setInt(5, _dawnFestivalScore);
				statement.setDouble(6, _duskStoneScore);
				statement.setInt(7, _duskFestivalScore);
				statement.setInt(8, _signsSealOwners.get(SEAL_AVARICE));
				statement.setInt(9, _signsSealOwners.get(SEAL_GNOSIS));
				statement.setInt(10, _signsSealOwners.get(SEAL_STRIFE));
				statement.setInt(11, _signsDawnSealTotals.get(SEAL_AVARICE));
				statement.setInt(12, _signsDawnSealTotals.get(SEAL_GNOSIS));
				statement.setInt(13, _signsDawnSealTotals.get(SEAL_STRIFE));
				statement.setInt(14, _signsDuskSealTotals.get(SEAL_AVARICE));
				statement.setInt(15, _signsDuskSealTotals.get(SEAL_GNOSIS));
				statement.setInt(16, _signsDuskSealTotals.get(SEAL_STRIFE));
				statement.setInt(17, SevenSignsFestival.getInstance().getCurrentFestivalCycle());
				
				for (int i = 0; i < SevenSignsFestival.FESTIVAL_COUNT; i++)
				{
					statement.setInt(18 + i, SevenSignsFestival.getInstance().getAccumulatedBonus(i));
				}
				
				statement.setInt(18 + SevenSignsFestival.FESTIVAL_COUNT, Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
				statement.execute();
				
				DatabaseUtils.close(statement);
				
				if (Config.DEBUG)
				{
					LOG.info("SevenSigns: Updated data in database.");
				}
				
				sqlQuery = null;
				
			}
		}
		catch (final SQLException e)
		{
			LOG.error("SevenSigns: Unable to save data to database", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Used to reset the cabal details of all players, and update the database.<BR>
	 * Primarily used when beginning a new cycle, and should otherwise never be called.
	 */
	protected void resetPlayerData()
	{
		if (Config.DEBUG)
		{
			LOG.info("SevenSigns: Resetting player data for new event period.");
		}
		
		// Reset each player's contribution data as well as seal and cabal.
		for (final StatsSet sevenDat : _signsPlayerData.values())
		{
			final int charObjId = sevenDat.getInteger("char_obj_id");
			
			// Reset the player's cabal and seal information
			sevenDat.set("cabal", "");
			sevenDat.set("seal", SEAL_NULL);
			sevenDat.set("contribution_score", 0);
			
			_signsPlayerData.put(charObjId, sevenDat);
		}
	}
	
	/**
	 * Tests whether the specified player has joined a cabal in the past.
	 * @param player the player
	 * @return boolean hasRegistered
	 */
	private boolean hasRegisteredBefore(final L2PcInstance player)
	{
		return _signsPlayerData.containsKey(player.getObjectId());
	}
	
	/**
	 * Used to specify cabal-related details for the specified player. This method checks to see if the player has registered before and will update the database if necessary. <BR>
	 * Returns the cabal ID the player has joined.
	 * @param player the player
	 * @param chosenCabal the chosen cabal
	 * @param chosenSeal the chosen seal
	 * @return int cabal
	 */
	public int setPlayerInfo(L2PcInstance player, int chosenCabal, int chosenSeal)
	{
		int charObjId = player.getObjectId();
		Connection con = null;
		PreparedStatement statement = null;
		StatsSet currPlayerData = getPlayerData(player);
		
		if (currPlayerData != null)
		{
			// If the seal validation period has passed,
			// cabal information was removed and so "re-register" player
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);
			
			_signsPlayerData.put(charObjId, currPlayerData);
		}
		else
		{
			currPlayerData = new StatsSet();
			currPlayerData.set("char_obj_id", charObjId);
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);
			currPlayerData.set("red_stones", 0);
			currPlayerData.set("green_stones", 0);
			currPlayerData.set("blue_stones", 0);
			currPlayerData.set("ancient_adena_amount", 0);
			currPlayerData.set("contribution_score", 0);
			
			_signsPlayerData.put(charObjId, currPlayerData);
			
			// Update data in database, as we have a new player signing up.
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("INSERT INTO seven_signs (char_obj_id, cabal, seal) VALUES (?,?,?)");
				statement.setInt(1, charObjId);
				statement.setString(2, getCabalShortName(chosenCabal));
				statement.setInt(3, chosenSeal);
				statement.execute();
				
				DatabaseUtils.close(statement);
				
				if (Config.DEBUG)
				{
					LOG.info("SevenSigns: Inserted data in DB for char ID " + currPlayerData.getInteger("char_obj_id") + " (" + currPlayerData.getString("cabal") + ")");
				}
			}
			catch (final SQLException e)
			{
				LOG.error("SevenSigns: Failed to save data", e);
			}
			finally
			{
				CloseUtil.close(con);
				con = null;
			}
		}
		
		// Increasing Seal total score for the player chosen Seal.
		if (currPlayerData.getString("cabal") == "dawn")
		{
			_signsDawnSealTotals.put(chosenSeal, _signsDawnSealTotals.get(chosenSeal) + 1);
		}
		else
		{
			_signsDuskSealTotals.put(chosenSeal, _signsDuskSealTotals.get(chosenSeal) + 1);
		}
		
		currPlayerData = null;
		
		saveSevenSignsData(player, true);
		
		if (Config.DEBUG)
		{
			LOG.info("SevenSigns: " + player.getName() + " has joined the " + getCabalName(chosenCabal) + " for the " + getSealName(chosenSeal, false) + "!");
		}
		
		return chosenCabal;
	}
	
	/**
	 * Returns the amount of ancient adena the specified player can claim, if any.<BR>
	 * If removeReward = True, all the ancient adena owed to them is removed, then DB is updated.
	 * @param player the player
	 * @param removeReward the remove reward
	 * @return int rewardAmount
	 */
	public int getAncientAdenaReward(final L2PcInstance player, final boolean removeReward)
	{
		StatsSet currPlayer = getPlayerData(player);
		final int rewardAmount = currPlayer.getInteger("ancient_adena_amount");
		
		currPlayer.set("red_stones", 0);
		currPlayer.set("green_stones", 0);
		currPlayer.set("blue_stones", 0);
		currPlayer.set("ancient_adena_amount", 0);
		
		if (removeReward)
		{
			_signsPlayerData.put(player.getObjectId(), currPlayer);
			saveSevenSignsData(player, true);
		}
		
		currPlayer = null;
		
		return rewardAmount;
	}
	
	/**
	 * Used to add the specified player's seal stone contribution points to the current total for their cabal. Returns the point score the contribution was worth. Each stone count <B>must be</B> broken down and specified by the stone's color.
	 * @param player the player
	 * @param blueCount the blue count
	 * @param greenCount the green count
	 * @param redCount the red count
	 * @return int contribScore
	 */
	public int addPlayerStoneContrib(final L2PcInstance player, final int blueCount, final int greenCount, final int redCount)
	{
		StatsSet currPlayer = getPlayerData(player);
		
		final int contribScore = calcContributionScore(blueCount, greenCount, redCount);
		final int totalAncientAdena = currPlayer.getInteger("ancient_adena_amount") + calcAncientAdenaReward(blueCount, greenCount, redCount);
		final int totalContribScore = currPlayer.getInteger("contribution_score") + contribScore;
		
		if (totalContribScore > Config.ALT_MAXIMUM_PLAYER_CONTRIB)
		{
			return -1;
		}
		
		currPlayer.set("red_stones", currPlayer.getInteger("red_stones") + redCount);
		currPlayer.set("green_stones", currPlayer.getInteger("green_stones") + greenCount);
		currPlayer.set("blue_stones", currPlayer.getInteger("blue_stones") + blueCount);
		currPlayer.set("ancient_adena_amount", totalAncientAdena);
		currPlayer.set("contribution_score", totalContribScore);
		_signsPlayerData.put(player.getObjectId(), currPlayer);
		
		switch (getPlayerCabal(player))
		{
			case CABAL_DAWN:
				_dawnStoneScore += contribScore;
				break;
			case CABAL_DUSK:
				_duskStoneScore += contribScore;
				break;
		}
		
		saveSevenSignsData(player, true);
		
		if (Config.DEBUG)
		{
			LOG.info("SevenSigns: " + player.getName() + " contributed " + contribScore + " seal stone points to their cabal.");
		}
		
		return contribScore;
	}
	
	/**
	 * Adds the specified number of festival points to the specified cabal. Remember, the same number of points are <B>deducted from the rival cabal</B> to maintain proportionality.
	 * @param cabal the cabal
	 * @param amount the amount
	 */
	public void addFestivalScore(final int cabal, final int amount)
	{
		if (cabal == CABAL_DUSK)
		{
			_duskFestivalScore += amount;
			
			// To prevent negative scores!
			if (_dawnFestivalScore >= amount)
			{
				_dawnFestivalScore -= amount;
			}
		}
		else
		{
			_dawnFestivalScore += amount;
			
			if (_duskFestivalScore >= amount)
			{
				_duskFestivalScore -= amount;
			}
		}
	}
	
	/**
	 * Send info on the current Seven Signs period to the specified player.
	 * @param player the player
	 */
	public void sendCurrentPeriodMsg(final L2PcInstance player)
	{
		SystemMessage sm = null;
		
		switch (getCurrentPeriod())
		{
			case PERIOD_COMP_RECRUITING:
				sm = new SystemMessage(SystemMessageId.PREPARATIONS_PERIOD_BEGUN);
				break;
			case PERIOD_COMPETITION:
				sm = new SystemMessage(SystemMessageId.COMPETITION_PERIOD_BEGUN);
				break;
			case PERIOD_COMP_RESULTS:
				sm = new SystemMessage(SystemMessageId.RESULTS_PERIOD_BEGUN);
				break;
			case PERIOD_SEAL_VALIDATION:
				sm = new SystemMessage(SystemMessageId.VALIDATION_PERIOD_BEGUN);
				break;
		}
		
		player.sendPacket(sm);
	}
	
	/**
	 * Sends the built-in system message specified by sysMsgId to all online players.
	 * @param sysMsgId the sys msg id
	 */
	public void sendMessageToAll(final SystemMessageId sysMsgId)
	{
		SystemMessage sm = new SystemMessage(sysMsgId);
		
		for (final L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			player.sendPacket(sm);
		}
		sm = null;
	}
	
	/**
	 * Used to initialize the seals for each cabal. (Used at startup or at beginning of a new cycle). This method should be called after <B>resetSeals()</B> and <B>calcNewSealOwners()</B> on a new cycle.
	 */
	protected void initializeSeals()
	{
		for (final Integer currSeal : _signsSealOwners.keySet())
		{
			final int sealOwner = _signsSealOwners.get(currSeal);
			
			if (sealOwner != CABAL_NULL)
			{
				if (isSealValidationPeriod())
				{
					LOG.info("SevenSigns: The " + getCabalName(sealOwner) + " have won the " + getSealName(currSeal, false) + "");
				}
				else
				{
					LOG.info("SevenSigns: The " + getSealName(currSeal, false) + " is currently owned by " + getCabalName(sealOwner) + "");
				}
			}
			else
			{
				LOG.info("SevenSigns: The " + getSealName(currSeal, false) + " remains unclaimed");
			}
		}
	}
	
	/**
	 * Only really used at the beginning of a new cycle, this method resets all seal-related data.
	 */
	protected void resetSeals()
	{
		_signsDawnSealTotals.put(SEAL_AVARICE, 0);
		_signsDawnSealTotals.put(SEAL_GNOSIS, 0);
		_signsDawnSealTotals.put(SEAL_STRIFE, 0);
		_signsDuskSealTotals.put(SEAL_AVARICE, 0);
		_signsDuskSealTotals.put(SEAL_GNOSIS, 0);
		_signsDuskSealTotals.put(SEAL_STRIFE, 0);
	}
	
	/**
	 * Calculates the ownership of the three Seals of the Seven Signs, based on various criterion. <BR>
	 * <BR>
	 * Should only ever called at the beginning of a new cycle.
	 */
	protected void calcNewSealOwners()
	{
		if (Config.DEBUG)
		{
			LOG.info("SevenSigns: (Avarice) Dawn = " + _signsDawnSealTotals.get(SEAL_AVARICE) + ", Dusk = " + _signsDuskSealTotals.get(SEAL_AVARICE));
			LOG.info("SevenSigns: (Gnosis) Dawn = " + _signsDawnSealTotals.get(SEAL_GNOSIS) + ", Dusk = " + _signsDuskSealTotals.get(SEAL_GNOSIS));
			LOG.info("SevenSigns: (Strife) Dawn = " + _signsDawnSealTotals.get(SEAL_STRIFE) + ", Dusk = " + _signsDuskSealTotals.get(SEAL_STRIFE));
		}
		
		for (final Integer currSeal : _signsDawnSealTotals.keySet())
		{
			final int prevSealOwner = _signsSealOwners.get(currSeal);
			int newSealOwner = CABAL_NULL;
			final int dawnProportion = getSealProportion(currSeal, CABAL_DAWN);
			final int totalDawnMembers = getTotalMembers(CABAL_DAWN) == 0 ? 1 : getTotalMembers(CABAL_DAWN);
			final int dawnPercent = Math.round((float) dawnProportion / (float) totalDawnMembers * 100);
			final int duskProportion = getSealProportion(currSeal, CABAL_DUSK);
			final int totalDuskMembers = getTotalMembers(CABAL_DUSK) == 0 ? 1 : getTotalMembers(CABAL_DUSK);
			final int duskPercent = Math.round((float) duskProportion / (float) totalDuskMembers * 100);
			
			/*
			 * - If a Seal was already closed or owned by the opponent and the new winner wants to assume ownership of the Seal, 35% or more of the members of the Cabal must have chosen the Seal. If they chose less than 35%, they cannot own the Seal. - If the Seal was owned by the winner in the
			 * previous Seven Signs, they can retain that seal if 10% or more members have chosen it. If they want to possess a new Seal, at least 35% of the members of the Cabal must have chosen the new Seal.
			 */
			switch (prevSealOwner)
			{
				case CABAL_NULL:
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
							newSealOwner = CABAL_NULL;
							break;
						case CABAL_DAWN:
							if (dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if (duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
				case CABAL_DAWN:
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
							if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DAWN:
							if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if (duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
				case CABAL_DUSK:
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
							if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DAWN:
							if (dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
			}
			
			_signsSealOwners.put(currSeal, newSealOwner);
			
			// Alert all online players to new seal status.
			switch (currSeal)
			{
				case SEAL_AVARICE:
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_AVARICE);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_AVARICE);
					}
					break;
				case SEAL_GNOSIS:
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_GNOSIS);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_GNOSIS);
					}
					break;
				case SEAL_STRIFE:
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_STRIFE);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_STRIFE);
					}
					
					CastleManager.getInstance().validateTaxes(newSealOwner);
					break;
			}
		}
	}
	
	/**
	 * This method is called to remove all players from catacombs and necropolises, who belong to the losing cabal. <BR>
	 * <BR>
	 * Should only ever called at the beginning of Seal Validation.
	 * @param compWinner the comp winner
	 */
	protected void teleLosingCabalFromDungeons(final String compWinner)
	{
		for (final L2PcInstance onlinePlayer : L2World.getInstance().getAllPlayers().values())
		{
			StatsSet currPlayer = getPlayerData(onlinePlayer);
			
			if (isSealValidationPeriod() || isCompResultsPeriod())
			{
				if (!onlinePlayer.isGM() && onlinePlayer.isIn7sDungeon() && !currPlayer.getString("cabal").equals(compWinner))
				{
					onlinePlayer.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					onlinePlayer.setIsIn7sDungeon(false);
					onlinePlayer.sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
				}
			}
			else
			{
				if (!onlinePlayer.isGM() && onlinePlayer.isIn7sDungeon() && ((currPlayer == null) || !currPlayer.getString("cabal").isEmpty()))
				{
					onlinePlayer.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					onlinePlayer.setIsIn7sDungeon(false);
					onlinePlayer.sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
				}
			}
		}
	}
	
	/**
	 * The primary controller of period change of the Seven Signs system. This runs all related tasks depending on the period that is about to begin.
	 * @author Tempy
	 */
	protected class SevenSignsPeriodChange implements Runnable
	{
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			/*
			 * Remember the period check here refers to the period just ENDED!
			 */
			final int periodEnded = getCurrentPeriod();
			_activePeriod++;
			
			switch (periodEnded)
			{
				case PERIOD_COMP_RECRUITING: // Initialization
					
					// Start the Festival of Darkness cycle.
					SevenSignsFestival.getInstance().startFestivalManager();
					
					// Send message that Competition has begun.
					sendMessageToAll(SystemMessageId.QUEST_EVENT_PERIOD_BEGUN);
					break;
				case PERIOD_COMPETITION: // Results Calculation
					
					// Send message that Competition has ended.
					sendMessageToAll(SystemMessageId.QUEST_EVENT_PERIOD_ENDED);
					
					final int compWinner = getCabalHighestScore();
					
					// Schedule a stop of the festival engine.
					SevenSignsFestival.getInstance().getFestivalManagerSchedule().cancel(false);
					
					calcNewSealOwners();
					
					switch (compWinner)
					{
						case CABAL_DAWN:
							sendMessageToAll(SystemMessageId.DAWN_WON);
							break;
						case CABAL_DUSK:
							sendMessageToAll(SystemMessageId.DUSK_WON);
							break;
					}
					
					_previousWinner = compWinner;
					break;
				case PERIOD_COMP_RESULTS: // Seal Validation
					
					// Perform initial Seal Validation set up.
					initializeSeals();
					
					// Send message that Seal Validation has begun.
					sendMessageToAll(SystemMessageId.SEAL_VALIDATION_PERIOD_BEGUN);
					
					LOG.info("SevenSigns: The " + getCabalName(_previousWinner) + " have won the competition with " + getCurrentScore(_previousWinner) + " points!");
					break;
				case PERIOD_SEAL_VALIDATION: // Reset for New Cycle
					
					SevenSignsFestival.getInstance().rewardHighRanked();
					
					// Ensure a cycle restart when this period ends.
					_activePeriod = PERIOD_COMP_RECRUITING;
					
					// Send message that Seal Validation has ended.
					sendMessageToAll(SystemMessageId.SEAL_VALIDATION_PERIOD_ENDED);
					
					// Reset all data
					resetPlayerData();
					resetSeals();
					
					// Reset all Festival-related data and remove any unused blood offerings.
					// NOTE: A full update of Festival data in the database is also performed.
					SevenSignsFestival.getInstance().resetFestivalData(false);
					
					_dawnStoneScore = 0;
					_duskStoneScore = 0;
					
					_dawnFestivalScore = 0;
					_duskFestivalScore = 0;
					
					_currentCycle++;
					
					break;
			}
			
			// Make sure all Seven Signs data is saved for future use.
			saveSevenSignsData(null, true);
			
			teleLosingCabalFromDungeons(getCabalShortName(getCabalHighestScore()));
			
			Broadcast.toAllOnlinePlayers(SignsSky.Sky());
			
			spawnSevenSignsNPC();
			
			LOG.info("SevenSigns: The " + getCurrentPeriodName() + " period has begun!");
			
			setCalendarForNextPeriodChange();
			
			SevenSignsPeriodChange sspc = new SevenSignsPeriodChange();
			ThreadPoolManager.getInstance().scheduleGeneral(sspc, getMilliToPeriodChange());
			sspc = null;
		}
	}
}

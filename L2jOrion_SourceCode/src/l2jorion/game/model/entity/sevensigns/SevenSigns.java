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
	
	private static SevenSigns _instance;
	
	public static final String SEVEN_SIGNS_DATA_FILE = "config/signs.ini";
	public static final String SEVEN_SIGNS_HTML_PATH = "data/html/seven_signs/";
	
	public static final int CABAL_NULL = 0;
	public static final int CABAL_DUSK = 1;
	public static final int CABAL_DAWN = 2;
	
	public static final int SEAL_NULL = 0;
	public static final int SEAL_AVARICE = 1;
	public static final int SEAL_GNOSIS = 2;
	public static final int SEAL_STRIFE = 3;
	
	public static final int PERIOD_COMP_RECRUITING = 0;
	public static final int PERIOD_COMPETITION = 1;
	public static final int PERIOD_COMP_RESULTS = 2;
	public static final int PERIOD_SEAL_VALIDATION = 3;
	
	public static final int PERIOD_START_HOUR = 18;
	public static final int PERIOD_START_MINS = 00;
	
	public static final int PERIOD_START_DAY = Calendar.MONDAY;
	
	// The quest event and seal validation periods last for approximately one week
	// with a 15 minutes "interval" period sandwiched between them.
	public static final int PERIOD_MINOR_LENGTH = 900000;
	public static final int PERIOD_MAJOR_LENGTH = 604800000 - PERIOD_MINOR_LENGTH;
	
	public static final int ANCIENT_ADENA_ID = 5575;
	public static final int RECORD_SEVEN_SIGNS_ID = 5707;
	public static final int CERTIFICATE_OF_APPROVAL_ID = 6388;
	
	public static final int RECORD_SEVEN_SIGNS_COST = 500;
	public static final int ADENA_JOIN_DAWN_COST = 50000;
	
	public static final int ORATOR_NPC_ID = 31094;
	public static final int PREACHER_NPC_ID = 31093;
	public static final int MAMMON_MERCHANT_ID = 31113;
	public static final int MAMMON_BLACKSMITH_ID = 31126;
	public static final int MAMMON_MARKETEER_ID = 31092;
	public static final int SPIRIT_IN_ID = 31111;
	public static final int SPIRIT_OUT_ID = 31112;
	public static final int LILITH_NPC_ID = 25283;
	public static final int ANAKIM_NPC_ID = 25286;
	public static final int CREST_OF_DAWN_ID = 31170;
	public static final int CREST_OF_DUSK_ID = 31171;
	
	public static final int SEAL_STONE_BLUE_ID = 6360;
	public static final int SEAL_STONE_GREEN_ID = 6361;
	public static final int SEAL_STONE_RED_ID = 6362;
	
	public static final int SEAL_STONE_BLUE_VALUE = 3;
	public static final int SEAL_STONE_GREEN_VALUE = 5;
	public static final int SEAL_STONE_RED_VALUE = 10;
	public static final int BLUE_CONTRIB_POINTS = 3;
	public static final int GREEN_CONTRIB_POINTS = 5;
	public static final int RED_CONTRIB_POINTS = 10;
	
	private final Calendar _calendar = Calendar.getInstance();
	
	protected int _activePeriod;
	protected int _currentCycle;
	protected double _dawnStoneScore;
	protected double _duskStoneScore;
	protected int _dawnFestivalScore;
	protected int _duskFestivalScore;
	protected int _compWinner;
	protected int _previousWinner;
	
	private final Map<Integer, StatsSet> _signsPlayerData;
	private final Map<Integer, Integer> _signsSealOwners;
	private final Map<Integer, Integer> _signsDuskSealTotals;
	private final Map<Integer, Integer> _signsDawnSealTotals;
	private static AutoSpawnInstance _merchantSpawn;
	private static AutoSpawnInstance _blacksmithSpawn;
	private static AutoSpawnInstance _spiritInSpawn;
	private static AutoSpawnInstance _spiritOutSpawn;
	private static AutoSpawnInstance _lilithSpawn;
	private static AutoSpawnInstance _anakimSpawn;
	private static Map<Integer, AutoSpawnInstance> _crestofdawnspawn;
	private static Map<Integer, AutoSpawnInstance> _crestofduskspawn;
	private static Map<Integer, AutoSpawnInstance> _oratorSpawns;
	private static Map<Integer, AutoSpawnInstance> _preacherSpawns;
	private static Map<Integer, AutoSpawnInstance> _marketeerSpawns;
	
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
	
	public void spawnSevenSignsNPC()
	{
		_merchantSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(MAMMON_MERCHANT_ID, false);
		_blacksmithSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(MAMMON_BLACKSMITH_ID, false);
		_marketeerSpawns = AutoSpawn.getInstance().getAutoSpawnInstances(MAMMON_MARKETEER_ID);
		_spiritInSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(SPIRIT_IN_ID, false);
		_spiritOutSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(SPIRIT_OUT_ID, false);
		_lilithSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(LILITH_NPC_ID, false);
		_anakimSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(ANAKIM_NPC_ID, false);
		
		_crestofdawnspawn = AutoSpawn.getInstance().getAutoSpawnInstances(CREST_OF_DAWN_ID);
		_crestofduskspawn = AutoSpawn.getInstance().getAutoSpawnInstances(CREST_OF_DUSK_ID);
		
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
						
						for (final AutoSpawnInstance spawnInst : _crestofdawnspawn.values())
						{
							if (!AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
							{
								AutoSpawn.getInstance().setSpawnActive(spawnInst, true);
							}
						}
						
						for (final AutoSpawnInstance spawnInst : _crestofduskspawn.values())
						{
							if (AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
							{
								AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
							}
						}
						break;
					
					case CABAL_DUSK:
						if (!AutoSpawn.getInstance().getAutoSpawnInstance(_anakimSpawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawn.getInstance().setSpawnActive(_anakimSpawn, true);
						}
						
						AutoSpawn.getInstance().setSpawnActive(_lilithSpawn, false);
						
						for (final AutoSpawnInstance spawnInst : _crestofduskspawn.values())
						{
							if (!AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
							{
								AutoSpawn.getInstance().setSpawnActive(spawnInst, true);
							}
						}
						
						for (final AutoSpawnInstance spawnInst : _crestofdawnspawn.values())
						{
							if (AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
							{
								AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
							}
						}
						break;
				}
			}
			else
			{
				AutoSpawn.getInstance().setSpawnActive(_merchantSpawn, false);
				AutoSpawn.getInstance().setSpawnActive(_lilithSpawn, false);
				AutoSpawn.getInstance().setSpawnActive(_anakimSpawn, false);
				
				for (final AutoSpawnInstance spawnInst : _crestofdawnspawn.values())
				{
					if (AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
					}
				}
				for (final AutoSpawnInstance spawnInst : _crestofduskspawn.values())
				{
					if (AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
					}
				}
				
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
			
			for (final AutoSpawnInstance spawnInst : _crestofdawnspawn.values())
			{
				if (AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
				{
					AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
				}
			}
			for (final AutoSpawnInstance spawnInst : _crestofduskspawn.values())
			{
				if (AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
				{
					AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
				}
			}
			
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
	
	public static SevenSigns getInstance()
	{
		if (_instance == null)
		{
			_instance = new SevenSigns();
		}
		
		return _instance;
	}
	
	public static int calcContributionScore(final int blueCount, final int greenCount, final int redCount)
	{
		int contrib = blueCount * BLUE_CONTRIB_POINTS;
		contrib += greenCount * GREEN_CONTRIB_POINTS;
		contrib += redCount * RED_CONTRIB_POINTS;
		
		return contrib;
	}
	
	public static int calcAncientAdenaReward(final int blueCount, final int greenCount, final int redCount)
	{
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
	
	public final int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	public final int getCurrentPeriod()
	{
		return _activePeriod;
	}
	
	private final int getDaysToPeriodChange()
	{
		final int numDays = _calendar.get(Calendar.DAY_OF_WEEK) - PERIOD_START_DAY;
		
		if (numDays < 0)
		{
			return 0 - numDays;
		}
		
		return 7 - numDays;
	}
	
	public final long getMilliToPeriodChange()
	{
		final long currTimeMillis = System.currentTimeMillis();
		final long changeTimeMillis = _calendar.getTimeInMillis();
		
		return changeTimeMillis - currTimeMillis;
	}
	
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
	
	public final boolean isSealValidationPeriod()
	{
		return _activePeriod == PERIOD_SEAL_VALIDATION;
	}
	
	public final boolean isCompResultsPeriod()
	{
		return _activePeriod == PERIOD_COMP_RESULTS;
	}
	
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
	
	public final int getSealOwner(final int seal)
	{
		return _signsSealOwners.get(seal);
	}
	
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
	
	public final StatsSet getPlayerData(final L2PcInstance player)
	{
		return _signsPlayerData.get(player.getObjectId());
	}
	
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
		
		return stoneCount;
	}
	
	public int getPlayerContribScore(final L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return 0;
		}
		
		final StatsSet currPlayer = getPlayerData(player);
		
		return currPlayer.getInteger("contribution_score");
	}
	
	public int getPlayerAdenaCollect(final L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return 0;
		}
		
		return _signsPlayerData.get(player.getObjectId()).getInteger("ancient_adena_amount");
	}
	
	public int getPlayerSeal(final L2PcInstance player)
	{
		if (!hasRegisteredBefore(player))
		{
			return SEAL_NULL;
		}
		
		return getPlayerData(player).getInteger("seal");
	}
	
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
		}
	}
	
	public void saveSevenSignsData(final L2PcInstance player, final boolean updateSettings)
	{
		Connection con = null;
		
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
			}
		}
		catch (final SQLException e)
		{
			LOG.error("SevenSigns: Unable to save data to database", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
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
	
	private boolean hasRegisteredBefore(final L2PcInstance player)
	{
		return _signsPlayerData.containsKey(player.getObjectId());
	}
	
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
		
		return rewardAmount;
	}
	
	public int addPlayerStoneContrib(final L2PcInstance player, final int blueCount, final int greenCount, final int redCount)
	{
		StatsSet currPlayer = getPlayerData(player);
		
		final int contribScore = calcContributionScore(blueCount, greenCount, redCount);
		
		if (currPlayer == null) // fix for error
		{
			return -1;
		}
		
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
		
		return contribScore;
	}
	
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
	
	public void sendMessageToAll(final SystemMessageId sysMsgId)
	{
		SystemMessage sm = new SystemMessage(sysMsgId);
		
		for (final L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			player.sendPacket(sm);
		}
	}
	
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
	
	protected void resetSeals()
	{
		_signsDawnSealTotals.put(SEAL_AVARICE, 0);
		_signsDawnSealTotals.put(SEAL_GNOSIS, 0);
		_signsDawnSealTotals.put(SEAL_STRIFE, 0);
		_signsDuskSealTotals.put(SEAL_AVARICE, 0);
		_signsDuskSealTotals.put(SEAL_GNOSIS, 0);
		_signsDuskSealTotals.put(SEAL_STRIFE, 0);
	}
	
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
	
	protected void teleLosingCabalFromDungeons(final String compWinner)
	{
		for (final L2PcInstance onlinePlayer : L2World.getInstance().getAllPlayers().values())
		{
			StatsSet currPlayer = getPlayerData(onlinePlayer);
			
			if (isSealValidationPeriod() || isCompResultsPeriod())
			{
				if (!onlinePlayer.isGM() && onlinePlayer.isIn7sDungeon() //
					&& !currPlayer.getString("cabal").equals(compWinner))
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
	
	protected class SevenSignsPeriodChange implements Runnable
	{
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
					
					LOG.info("SevenSigns: The " + getCabalName(_previousWinner) + " have won the competition with " + getCurrentScore(_previousWinner) + " points");
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
			
			LOG.info("SevenSigns: The " + getCurrentPeriodName() + " period has begun");
			
			setCalendarForNextPeriodChange();
			
			SevenSignsPeriodChange sspc = new SevenSignsPeriodChange();
			ThreadPoolManager.getInstance().scheduleGeneral(sspc, getMilliToPeriodChange());
		}
	}
}

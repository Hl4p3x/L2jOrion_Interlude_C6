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
package l2jorion.game.ai.additional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.DoorTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.SpecialCamera;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.StatsSet;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

public class VanHalter extends Quest implements Runnable
{
	private static final Logger LOG = LoggerFactory.getLogger(VanHalter.class);
	
	// List of intruders.
	protected Map<Integer, List<L2PcInstance>> _bleedingPlayers = new FastMap<>();
	
	// Spawn data of monsters.
	protected Map<Integer, L2Spawn> _monsterSpawn = new FastMap<>();
	protected List<L2Spawn> _royalGuardSpawn = new FastList<>();
	protected List<L2Spawn> _royalGuardCaptainSpawn = new FastList<>();
	protected List<L2Spawn> _royalGuardHelperSpawn = new FastList<>();
	protected List<L2Spawn> _triolRevelationSpawn = new FastList<>();
	protected List<L2Spawn> _triolRevelationAlive = new FastList<>();
	protected List<L2Spawn> _guardOfAltarSpawn = new FastList<>();
	protected Map<Integer, L2Spawn> _cameraMarkerSpawn = new FastMap<>();
	protected L2Spawn _ritualOfferingSpawn = null;
	protected L2Spawn _ritualSacrificeSpawn = null;
	protected L2Spawn _vanHalterSpawn = null;
	private final SimpleDateFormat date = new SimpleDateFormat("H:mm:ss yyyy/MM/dd");
	
	// Instance of monsters.
	protected List<L2NpcInstance> _monsters = new FastList<>();
	protected List<L2NpcInstance> _royalGuard = new FastList<>();
	protected List<L2NpcInstance> _royalGuardCaptain = new FastList<>();
	protected List<L2NpcInstance> _royalGuardHepler = new FastList<>();
	protected List<L2NpcInstance> _triolRevelation = new FastList<>();
	protected List<L2NpcInstance> _guardOfAltar = new FastList<>();
	protected Map<Integer, L2NpcInstance> _cameraMarker = new FastMap<>();
	protected List<L2DoorInstance> _doorOfAltar = new FastList<>();
	protected List<L2DoorInstance> _doorOfSacrifice = new FastList<>();
	protected L2NpcInstance _ritualOffering = null;
	protected L2NpcInstance _ritualSacrifice = null;
	protected L2GrandBossInstance _vanHalter = null;
	
	// Task
	protected ScheduledFuture<?> _movieTask = null;
	protected ScheduledFuture<?> _closeDoorOfAltarTask = null;
	protected ScheduledFuture<?> _openDoorOfAltarTask = null;
	protected ScheduledFuture<?> _lockUpDoorOfAltarTask = null;
	protected ScheduledFuture<?> _callRoyalGuardHelperTask = null;
	protected ScheduledFuture<?> _timeUpTask = null;
	protected ScheduledFuture<?> _intervalTask = null;
	protected ScheduledFuture<?> _halterEscapeTask = null;
	protected ScheduledFuture<?> _setBleedTask = null;
	
	// State of High Priestess van Halter
	boolean _isLocked = false;
	boolean _isHalterSpawned = false;
	boolean _isSacrificeSpawned = false;
	boolean _isCaptainSpawned = false;
	boolean _isHelperCalled = false;
	
	// VanHalter Status Tracking :
	private static final byte INTERVAL = 0;
	private static final byte NOTSPAWN = 1;
	private static final byte ALIVE = 2;
	
	// Initialize
	public VanHalter(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		int[] mobs =
		{
			29062,
			22188,
			32058,
			32059,
			32060,
			32061,
			32062,
			32063,
			32064,
			32065,
			32066
		};
		
		addEventId(29062, Quest.QuestEventType.ON_ATTACK);
		for (int mob : mobs)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
		}
		
		// GrandBossManager.getInstance().addBoss(29062);
		// Clear flag.
		_isLocked = false;
		_isCaptainSpawned = false;
		_isHelperCalled = false;
		_isHalterSpawned = false;
		
		// Setting door state.
		_doorOfAltar.add(DoorTable.getInstance().getDoor(19160014));
		_doorOfAltar.add(DoorTable.getInstance().getDoor(19160015));
		openDoorOfAltar(true);
		_doorOfSacrifice.add(DoorTable.getInstance().getDoor(19160016));
		_doorOfSacrifice.add(DoorTable.getInstance().getDoor(19160017));
		closeDoorOfSacrifice();
		
		// Load spawn data of monsters.
		loadRoyalGuard();
		loadTriolRevelation();
		loadRoyalGuardCaptain();
		loadRoyalGuardHelper();
		loadGuardOfAltar();
		loadVanHalter();
		loadRitualOffering();
		loadRitualSacrifice();
		
		// Spawn monsters.
		spawnRoyalGuard();
		spawnTriolRevelation();
		spawnVanHalter();
		spawnRitualOffering();
		
		// Setting spawn data of Dummy camera marker.
		_cameraMarkerSpawn.clear();
		try
		{
			L2NpcTemplate template1 = NpcTable.getInstance().getTemplate(13014); // Dummy npc
			L2Spawn tempSpawn;
			
			// Dummy camera marker.
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(-16397);
			tempSpawn.setLocy(-55200);
			tempSpawn.setLocz(-10449);
			tempSpawn.setHeading(16384);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(60000);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_cameraMarkerSpawn.put(1, tempSpawn);
			
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(-16397);
			tempSpawn.setLocy(-55200);
			tempSpawn.setLocz(-10051);
			tempSpawn.setHeading(16384);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(60000);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_cameraMarkerSpawn.put(2, tempSpawn);
			
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(-16397);
			tempSpawn.setLocy(-55200);
			tempSpawn.setLocz(-9741);
			tempSpawn.setHeading(16384);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(60000);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_cameraMarkerSpawn.put(3, tempSpawn);
			
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(-16397);
			tempSpawn.setLocy(-55200);
			tempSpawn.setLocz(-9394);
			tempSpawn.setHeading(16384);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(60000);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_cameraMarkerSpawn.put(4, tempSpawn);
			
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(-16397);
			tempSpawn.setLocy(-55197);
			tempSpawn.setLocz(-8739);
			tempSpawn.setHeading(16384);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(60000);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_cameraMarkerSpawn.put(5, tempSpawn);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("VanHalterManager : " + e.getMessage() + " :" + e);
		}
		
		// Set time up.
		if (_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new TimeUp(), Config.HPH_ACTIVITYTIMEOFHALTER);
		
		// Set bleeding to palyers.
		if (_setBleedTask != null)
		{
			_setBleedTask.cancel(false);
		}
		_setBleedTask = ThreadPoolManager.getInstance().scheduleGeneral(new Bleeding(), 2000);
		
		Integer status = GrandBossManager.getInstance().getBossStatus(29062);
		
		if (status == INTERVAL)
		{
			enterInterval();
		}
		else
		{
			GrandBossManager.getInstance().setBossStatus(29062, NOTSPAWN);
		}
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (npc.getNpcId() == 29062)
		{
			if ((int) (npc.getStatus().getCurrentHp() / npc.getMaxHp()) * 100 <= 20)
			{
				callRoyalGuardHelper();
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		
		if (npcId == 32058 || npcId == 32059 || npcId == 32060 || npcId == 32061 || npcId == 32062 || npcId == 32063 || npcId == 32064 || npcId == 32065 || npcId == 32066)
		{
			removeBleeding(npcId);
		}
		
		checkTriolRevelationDestroy();
		
		if (npcId == 22188)
		{
			checkRoyalGuardCaptainDestroy();
		}
		
		if (npcId == 29062)
		{
			enterInterval();
		}
		return super.onKill(npc, killer, isPet);
	}
	
	// Load Royal Guard.
	protected void loadRoyalGuard()
	{
		_royalGuardSpawn.clear();
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist Where npc_templateid between ? and ? ORDER BY id");
			statement.setInt(1, 22175);
			statement.setInt(2, 22176);
			ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_royalGuardSpawn.add(spawnDat);
				}
				else
				{
					LOG.warn("VanHalterManager.loadRoyalGuard: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			
			rset.close();
			statement.close();
			if (Config.DEBUG)
			{
				LOG.info("VanHalterManager.loadRoyalGuard: Loaded " + _royalGuardSpawn.size() + " Royal Guard spawn locations.");
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			// Problem with initializing spawn, go to next one
			LOG.warn("VanHalterManager.loadRoyalGuard: Spawn could not be initialized: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	protected void spawnRoyalGuard()
	{
		if (!_royalGuard.isEmpty())
		{
			deleteRoyalGuard();
		}
		
		for (L2Spawn rgs : _royalGuardSpawn)
		{
			rgs.startRespawn();
			_royalGuard.add(rgs.doSpawn());
		}
	}
	
	protected void deleteRoyalGuard()
	{
		for (L2NpcInstance rg : _royalGuard)
		{
			rg.getSpawn().stopRespawn();
			rg.deleteMe();
		}
		
		_royalGuard.clear();
	}
	
	// Load Triol's Revelation.
	protected void loadTriolRevelation()
	{
		_triolRevelationSpawn.clear();
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist Where npc_templateid between ? and ? ORDER BY id");
			statement.setInt(1, 32058);
			statement.setInt(2, 32068);
			ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_triolRevelationSpawn.add(spawnDat);
				}
				else
				{
					LOG.warn("VanHalterManager.loadTriolRevelation: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			
			rset.close();
			statement.close();
			if (Config.DEBUG)
			{
				LOG.info("VanHalterManager.loadTriolRevelation: Loaded " + _triolRevelationSpawn.size() + " Triol's Revelation spawn locations.");
			}
		}
		catch (Exception e)
		{
			// Problem with initializing spawn, go to next one
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("VanHalterManager.loadTriolRevelation: Spawn could not be initialized: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	protected void spawnTriolRevelation()
	{
		if (!_triolRevelation.isEmpty())
		{
			deleteTriolRevelation();
		}
		
		for (L2Spawn trs : _triolRevelationSpawn)
		{
			trs.startRespawn();
			_triolRevelation.add(trs.doSpawn());
			if (trs.getNpcid() != 32067 && trs.getNpcid() != 32068)
			{
				_triolRevelationAlive.add(trs);
			}
		}
	}
	
	protected void deleteTriolRevelation()
	{
		for (L2NpcInstance tr : _triolRevelation)
		{
			tr.getSpawn().stopRespawn();
			tr.deleteMe();
		}
		_triolRevelation.clear();
		_bleedingPlayers.clear();
	}
	
	// Load Royal Guard Captain.
	protected void loadRoyalGuardCaptain()
	{
		_royalGuardCaptainSpawn.clear();
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist Where npc_templateid = ? ORDER BY id");
			statement.setInt(1, 22188);
			ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_royalGuardCaptainSpawn.add(spawnDat);
				}
				else
				{
					LOG.warn("VanHalterManager.loadRoyalGuardCaptain: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			
			rset.close();
			statement.close();
			if (Config.DEBUG)
			{
				LOG.info("VanHalterManager.loadRoyalGuardCaptain: Loaded " + _royalGuardCaptainSpawn.size() + " Royal Guard Captain spawn locations.");
			}
		}
		catch (Exception e)
		{
			// Problem with initializing spawn, go to next one
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("VanHalterManager.loadRoyalGuardCaptain: Spawn could not be initialized: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	protected void spawnRoyalGuardCaptain()
	{
		if (!_royalGuardCaptain.isEmpty())
		{
			deleteRoyalGuardCaptain();
		}
		
		for (L2Spawn trs : _royalGuardCaptainSpawn)
		{
			trs.startRespawn();
			_royalGuardCaptain.add(trs.doSpawn());
		}
		_isCaptainSpawned = true;
	}
	
	protected void deleteRoyalGuardCaptain()
	{
		for (L2NpcInstance tr : _royalGuardCaptain)
		{
			tr.getSpawn().stopRespawn();
			tr.deleteMe();
		}
		
		_royalGuardCaptain.clear();
	}
	
	// Load Royal Guard Helper.
	protected void loadRoyalGuardHelper()
	{
		_royalGuardHelperSpawn.clear();
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist Where npc_templateid = ? ORDER BY id");
			statement.setInt(1, 22191);
			ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_royalGuardHelperSpawn.add(spawnDat);
				}
				else
				{
					LOG.warn("VanHalterManager.loadRoyalGuardHelper: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			
			rset.close();
			statement.close();
			if (Config.DEBUG)
			{
				LOG.info("VanHalterManager.loadRoyalGuardHelper: Loaded " + _royalGuardHelperSpawn.size() + " Royal Guard Helper spawn locations.");
			}
		}
		catch (Exception e)
		{
			// Problem with initializing spawn, go to next one
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("VanHalterManager.loadRoyalGuardHelper: Spawn could not be initialized: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	protected void spawnRoyalGuardHepler()
	{
		for (L2Spawn trs : _royalGuardHelperSpawn)
		{
			trs.startRespawn();
			_royalGuardHepler.add(trs.doSpawn());
		}
	}
	
	protected void deleteRoyalGuardHepler()
	{
		for (L2NpcInstance tr : _royalGuardHepler)
		{
			tr.getSpawn().stopRespawn();
			tr.deleteMe();
		}
		_royalGuardHepler.clear();
	}
	
	// Load Guard Of Altar
	protected void loadGuardOfAltar()
	{
		_guardOfAltarSpawn.clear();
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist Where npc_templateid = ? ORDER BY id");
			statement.setInt(1, 32051);
			ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_guardOfAltarSpawn.add(spawnDat);
				}
				else
				{
					LOG.warn("VanHalterManager.loadGuardOfAltar: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			
			rset.close();
			statement.close();
			if (Config.DEBUG)
			{
				LOG.info("VanHalterManager.loadGuardOfAltar: Loaded " + _guardOfAltarSpawn.size() + " Guard Of Altar spawn locations.");
			}
		}
		catch (Exception e)
		{
			// Problem with initializing spawn, go to next one
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("VanHalterManager.loadGuardOfAltar: Spawn could not be initialized: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	protected void spawnGuardOfAltar()
	{
		if (!_guardOfAltar.isEmpty())
		{
			deleteGuardOfAltar();
		}
		
		for (L2Spawn trs : _guardOfAltarSpawn)
		{
			trs.startRespawn();
			_guardOfAltar.add(trs.doSpawn());
		}
	}
	
	protected void deleteGuardOfAltar()
	{
		for (L2NpcInstance tr : _guardOfAltar)
		{
			tr.getSpawn().stopRespawn();
			tr.deleteMe();
		}
		
		_guardOfAltar.clear();
	}
	
	// Load High Priestess van Halter.
	protected void loadVanHalter()
	{
		_vanHalterSpawn = null;
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist Where npc_templateid = ? ORDER BY id");
			statement.setInt(1, 29062);
			ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_vanHalterSpawn = spawnDat;
				}
				else
				{
					LOG.warn("VanHalterManager.loadVanHalter: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			
			rset.close();
			statement.close();
			if (Config.DEBUG)
			{
				LOG.info("VanHalterManager.loadVanHalter: Loaded High Priestess van Halter spawn locations.");
			}
		}
		catch (Exception e)
		{
			// Problem with initializing spawn, go to next one
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("VanHalterManager.loadVanHalter: Spawn could not be initialized: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	protected void spawnVanHalter()
	{
		_vanHalter = (L2GrandBossInstance) _vanHalterSpawn.doSpawn();
		_vanHalter.setIsInvul(true);
		_isHalterSpawned = true;
	}
	
	protected void deleteVanHalter()
	{
		_vanHalter.setIsInvul(false);
		_vanHalter.getSpawn().stopRespawn();
		_vanHalter.deleteMe();
	}
	
	// Load Ritual Offering.
	protected void loadRitualOffering()
	{
		_ritualOfferingSpawn = null;
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist Where npc_templateid = ? ORDER BY id");
			statement.setInt(1, 32038);
			ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_ritualOfferingSpawn = spawnDat;
				}
				else
				{
					LOG.warn("VanHalterManager.loadRitualOffering: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			
			rset.close();
			statement.close();
			if (Config.DEBUG)
			{
				LOG.info("VanHalterManager.loadRitualOffering: Loaded Ritual Offering spawn locations.");
			}
		}
		catch (Exception e)
		{
			// Problem with initializing spawn, go to next one
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("VanHalterManager.loadRitualOffering: Spawn could not be initialized: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	protected void spawnRitualOffering()
	{
		_ritualOffering = _ritualOfferingSpawn.doSpawn();
		// _ritualOffering.setIsImmobilized(true);
		_ritualOffering.setIsInvul(true);
		_ritualOffering.setIsParalyzed(true);
	}
	
	protected void deleteRitualOffering()
	{
		// _ritualOffering.setIsImmobilized(false);
		_ritualOffering.setIsInvul(false);
		_ritualOffering.setIsParalyzed(false);
		_ritualOffering.getSpawn().stopRespawn();
		_ritualOffering.deleteMe();
	}
	
	// Load Ritual Sacrifice.
	protected void loadRitualSacrifice()
	{
		_ritualSacrificeSpawn = null;
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist Where npc_templateid = ? ORDER BY id");
			statement.setInt(1, 22195);
			ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_ritualSacrificeSpawn = spawnDat;
				}
				else
				{
					LOG.warn("VanHalterManager.loadRitualSacrifice: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			
			rset.close();
			statement.close();
			if (Config.DEBUG)
			{
				LOG.info("VanHalterManager.loadRitualSacrifice: Loaded Ritual Sacrifice spawn locations.");
			}
		}
		catch (Exception e)
		{
			// Problem with initializing spawn, go to next one
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("VanHalterManager.loadRitualSacrifice: Spawn could not be initialized: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	protected void spawnRitualSacrifice()
	{
		_ritualSacrifice = _ritualSacrificeSpawn.doSpawn();
		// _ritualSacrifice.setIsImmobilized(true);
		_ritualSacrifice.setIsInvul(true);
		_isSacrificeSpawned = true;
	}
	
	protected void deleteRitualSacrifice()
	{
		if (!_isSacrificeSpawned)
		{
			return;
		}
		
		_ritualSacrifice.getSpawn().stopRespawn();
		_ritualSacrifice.deleteMe();
		_isSacrificeSpawned = false;
	}
	
	protected void spawnCameraMarker()
	{
		_cameraMarker.clear();
		for (int i = 1; i <= _cameraMarkerSpawn.size(); i++)
		{
			_cameraMarker.put(i, _cameraMarkerSpawn.get(i).doSpawn());
			_cameraMarker.get(i).getSpawn().stopRespawn();
			_cameraMarker.get(i).setIsImobilised(true);
		}
	}
	
	protected void deleteCameraMarker()
	{
		if (_cameraMarker.isEmpty())
		{
			return;
		}
		
		for (int i = 1; i <= _cameraMarker.size(); i++)
		{
			_cameraMarker.get(i).deleteMe();
		}
		_cameraMarker.clear();
	}
	
	// Door control.
	/**
	 * @param intruder
	 */
	public void intruderDetection(L2PcInstance intruder)
	{
		if (_lockUpDoorOfAltarTask == null && !_isLocked && _isCaptainSpawned)
		{
			_lockUpDoorOfAltarTask = ThreadPoolManager.getInstance().scheduleGeneral(new LockUpDoorOfAltar(), Config.HPH_TIMEOFLOCKUPDOOROFALTAR);
		}
	}
	
	protected class LockUpDoorOfAltar implements Runnable
	{
		@Override
		public void run()
		{
			closeDoorOfAltar(false);
			_isLocked = true;
			_lockUpDoorOfAltarTask = null;
		}
	}
	
	protected void openDoorOfAltar(boolean loop)
	{
		for (L2DoorInstance door : _doorOfAltar)
		{
			try
			{
				door.openMe();
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn(e.getMessage() + " :" + e);
			}
		}
		
		if (loop)
		{
			_isLocked = false;
			
			if (_closeDoorOfAltarTask != null)
			{
				_closeDoorOfAltarTask.cancel(false);
			}
			_closeDoorOfAltarTask = null;
			_closeDoorOfAltarTask = ThreadPoolManager.getInstance().scheduleGeneral(new CloseDoorOfAltar(), Config.HPH_INTERVALOFDOOROFALTER);
		}
		else
		{
			if (_closeDoorOfAltarTask != null)
			{
				_closeDoorOfAltarTask.cancel(false);
			}
			_closeDoorOfAltarTask = null;
		}
	}
	
	protected class OpenDoorOfAltar implements Runnable
	{
		@Override
		public void run()
		{
			openDoorOfAltar(true);
		}
	}
	
	protected void closeDoorOfAltar(boolean loop)
	{
		for (L2DoorInstance door : _doorOfAltar)
		{
			door.closeMe();
		}
		
		if (loop)
		{
			if (_openDoorOfAltarTask != null)
			{
				_openDoorOfAltarTask.cancel(false);
			}
			_openDoorOfAltarTask = null;
			_openDoorOfAltarTask = ThreadPoolManager.getInstance().scheduleGeneral(new OpenDoorOfAltar(), Config.HPH_INTERVALOFDOOROFALTER);
		}
		else
		{
			if (_openDoorOfAltarTask != null)
			{
				_openDoorOfAltarTask.cancel(false);
			}
			_openDoorOfAltarTask = null;
		}
	}
	
	protected class CloseDoorOfAltar implements Runnable
	{
		@Override
		public void run()
		{
			closeDoorOfAltar(true);
		}
	}
	
	protected void openDoorOfSacrifice()
	{
		for (L2DoorInstance door : _doorOfSacrifice)
		{
			try
			{
				door.openMe();
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn(e.getMessage() + " :" + e);
			}
		}
	}
	
	protected void closeDoorOfSacrifice()
	{
		for (L2DoorInstance door : _doorOfSacrifice)
		{
			try
			{
				door.closeMe();
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn(e.getMessage() + " :" + e);
			}
		}
	}
	
	// event
	public void checkTriolRevelationDestroy()
	{
		if (_isCaptainSpawned)
		{
			return;
		}
		
		boolean isTriolRevelationDestroyed = true;
		for (L2Spawn tra : _triolRevelationAlive)
		{
			if (!tra.getLastSpawn().isDead())
			{
				isTriolRevelationDestroyed = false;
			}
		}
		
		if (isTriolRevelationDestroyed)
		{
			spawnRoyalGuardCaptain();
		}
	}
	
	public void checkRoyalGuardCaptainDestroy()
	{
		if (!_isHalterSpawned)
		{
			return;
		}
		
		deleteRoyalGuard();
		deleteRoyalGuardCaptain();
		spawnGuardOfAltar();
		openDoorOfSacrifice();
		
		// _vanHalter.setIsImmobilized(true);
		_vanHalter.setIsInvul(true);
		spawnCameraMarker();
		
		if (_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = null;
		
		_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(1), Config.HPH_APPTIMEOFHALTER);
	}
	
	// Start fight against High Priestess van Halter.
	protected void combatBeginning()
	{
		if (_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new TimeUp(), Config.HPH_FIGHTTIMEOFHALTER);
		
		Map<Integer, L2PcInstance> _targets = new FastMap<>();
		int i = 0;
		
		for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
		{
			i++;
			_targets.put(i, pc);
		}
		
		_vanHalter.reduceCurrentHp(1, _targets.get(Rnd.get(1, i)));
	}
	
	// Call Royal Guard Helper and escape from player.
	public void callRoyalGuardHelper()
	{
		if (!_isHelperCalled)
		{
			_isHelperCalled = true;
			_halterEscapeTask = ThreadPoolManager.getInstance().scheduleGeneral(new HalterEscape(), 500);
			_callRoyalGuardHelperTask = ThreadPoolManager.getInstance().scheduleGeneral(new CallRoyalGuardHelper(), 1000);
		}
	}
	
	protected class CallRoyalGuardHelper implements Runnable
	{
		@Override
		public void run()
		{
			spawnRoyalGuardHepler();
			
			if (_royalGuardHepler.size() <= Config.HPH_CALLROYALGUARDHELPERCOUNT && !_vanHalter.isDead())
			{
				if (_callRoyalGuardHelperTask != null)
				{
					_callRoyalGuardHelperTask.cancel(false);
				}
				_callRoyalGuardHelperTask = ThreadPoolManager.getInstance().scheduleGeneral(new CallRoyalGuardHelper(), Config.HPH_CALLROYALGUARDHELPERINTERVAL);
			}
			else
			{
				if (_callRoyalGuardHelperTask != null)
				{
					_callRoyalGuardHelperTask.cancel(false);
				}
				_callRoyalGuardHelperTask = null;
			}
		}
	}
	
	protected class HalterEscape implements Runnable
	{
		@Override
		public void run()
		{
			if (_royalGuardHepler.size() <= Config.HPH_CALLROYALGUARDHELPERCOUNT && !_vanHalter.isDead())
			{
				if (_vanHalter.isAfraid())
				{
					_vanHalter.stopEffects(L2Effect.EffectType.FEAR);
					_vanHalter.setIsAfraid(false);
					_vanHalter.updateAbnormalEffect();
				}
				else
				{
					_vanHalter.startFear();
					if (_vanHalter.getZ() >= -10476)
					{
						Location pos = new Location(-16397, -53308, -10448, 0);
						if (_vanHalter.getX() == pos.getX() && _vanHalter.getY() == pos.getY())
						{
							_vanHalter.stopEffects(L2Effect.EffectType.FEAR);
							_vanHalter.setIsAfraid(false);
							_vanHalter.updateAbnormalEffect();
						}
						else
						{
							_vanHalter.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, pos);
						}
					}
					else if (_vanHalter.getX() >= -16397)
					{
						Location pos = new Location(-15548, -54830, -10475, 0);
						_vanHalter.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, pos);
					}
					else
					{
						Location pos = new Location(-17248, -54830, -10475, 0);
						_vanHalter.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, pos);
					}
				}
				if (_halterEscapeTask != null)
				{
					_halterEscapeTask.cancel(false);
				}
				_halterEscapeTask = ThreadPoolManager.getInstance().scheduleGeneral(new HalterEscape(), 5000);
			}
			else
			{
				_vanHalter.stopEffects(L2Effect.EffectType.FEAR);
				_vanHalter.setIsAfraid(false);
				_vanHalter.updateAbnormalEffect();
				if (_halterEscapeTask != null)
				{
					_halterEscapeTask.cancel(false);
				}
				_halterEscapeTask = null;
			}
		}
	}
	
	// Check bleeding player.
	protected void addBleeding()
	{
		L2Skill bleed = SkillTable.getInstance().getInfo(4615, 12);
		
		for (L2NpcInstance tr : _triolRevelation)
		{
			if (!tr.getKnownList().getKnownPlayersInRadius(tr.getAggroRange()).iterator().hasNext() || tr.isDead())
			{
				continue;
			}
			
			List<L2PcInstance> bpc = new FastList<>();
			
			for (L2PcInstance pc : tr.getKnownList().getKnownPlayersInRadius(tr.getAggroRange()))
			{
				if (pc.getFirstEffect(bleed) == null)
				{
					bleed.getEffects(tr, pc, false, false, false);
					tr.broadcastPacket(new MagicSkillUser(tr, pc, bleed.getId(), 12, 1, 1));
				}
				
				bpc.add(pc);
			}
			_bleedingPlayers.remove(tr.getNpcId());
			_bleedingPlayers.put(tr.getNpcId(), bpc);
		}
	}
	
	public void removeBleeding(int npcId)
	{
		if (_bleedingPlayers.get(npcId) == null)
		{
			return;
		}
		for (L2PcInstance pc : (FastList<L2PcInstance>) _bleedingPlayers.get(npcId))
		{
			if (pc.getFirstEffect(L2Effect.EffectType.DMG_OVER_TIME) != null)
			{
				pc.stopEffects(L2Effect.EffectType.DMG_OVER_TIME);
			}
		}
		_bleedingPlayers.remove(npcId);
	}
	
	protected class Bleeding implements Runnable
	{
		@Override
		public void run()
		{
			addBleeding();
			
			if (_setBleedTask != null)
			{
				_setBleedTask.cancel(false);
			}
			_setBleedTask = ThreadPoolManager.getInstance().scheduleGeneral(new Bleeding(), 2000);
		}
	}
	
	// High Priestess van Halter dead or time up.
	public void enterInterval()
	{
		// Cancel all task
		if (_callRoyalGuardHelperTask != null)
		{
			_callRoyalGuardHelperTask.cancel(false);
		}
		_callRoyalGuardHelperTask = null;
		
		if (_closeDoorOfAltarTask != null)
		{
			_closeDoorOfAltarTask.cancel(false);
		}
		_closeDoorOfAltarTask = null;
		
		if (_halterEscapeTask != null)
		{
			_halterEscapeTask.cancel(false);
		}
		_halterEscapeTask = null;
		
		if (_intervalTask != null)
		{
			_intervalTask.cancel(false);
		}
		_intervalTask = null;
		
		if (_lockUpDoorOfAltarTask != null)
		{
			_lockUpDoorOfAltarTask.cancel(false);
		}
		_lockUpDoorOfAltarTask = null;
		
		if (_movieTask != null)
		{
			_movieTask.cancel(false);
		}
		_movieTask = null;
		
		if (_openDoorOfAltarTask != null)
		{
			_openDoorOfAltarTask.cancel(false);
		}
		_openDoorOfAltarTask = null;
		
		if (_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = null;
		
		// Delete monsters
		if (_vanHalter.isDead())
		{
			_vanHalter.getSpawn().stopRespawn();
		}
		else
		{
			deleteVanHalter();
		}
		deleteRoyalGuardHepler();
		deleteRoyalGuardCaptain();
		deleteRoyalGuard();
		deleteRitualOffering();
		deleteRitualSacrifice();
		deleteGuardOfAltar();
		
		// Set interval end.
		if (_intervalTask != null)
		{
			_intervalTask.cancel(false);
		}
		
		Integer status = GrandBossManager.getInstance().getBossStatus(29062);
		
		if (status != INTERVAL)
		{
			long interval = Rnd.get(Config.HPH_FIXINTERVALOFHALTER, Config.HPH_FIXINTERVALOFHALTER + Config.HPH_RANDOMINTERVALOFHALTER);
			
			StatsSet info = GrandBossManager.getInstance().getStatsSet(29062);
			
			long respawn = System.currentTimeMillis() + interval;
			
			info.set("respawn_time", respawn);
			
			GrandBossManager.getInstance().setBossStatus(29062, INTERVAL);
			
			GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
			gc.clear();
			gc.setTimeInMillis(respawn);
			
			info.set("killed_time", date.format(new Date(System.currentTimeMillis())));
			info.set("next_respawn", DateFormat.getDateTimeInstance().format(gc.getTime()));
			
			GrandBossManager.getInstance().setStatsSet(29062, info);
			
			String text = "Van Halter killed. Next respawn: " + DateFormat.getDateTimeInstance().format(gc.getTime());
			Log.add(text, "GrandBosses");
		}
		
		StatsSet info = GrandBossManager.getInstance().getStatsSet(29062);
		long temp = info.getLong("respawn_time") - System.currentTimeMillis();
		_intervalTask = ThreadPoolManager.getInstance().scheduleGeneral(new Interval(), temp);
	}
	
	// Interval.
	protected class Interval implements Runnable
	{
		@Override
		public void run()
		{
			setupAltar();
		}
	}
	
	// Interval end.
	public void setupAltar()
	{
		// Cancel all task
		if (_callRoyalGuardHelperTask != null)
		{
			_callRoyalGuardHelperTask.cancel(false);
		}
		_callRoyalGuardHelperTask = null;
		
		if (_closeDoorOfAltarTask != null)
		{
			_closeDoorOfAltarTask.cancel(false);
		}
		_closeDoorOfAltarTask = null;
		
		if (_halterEscapeTask != null)
		{
			_halterEscapeTask.cancel(false);
		}
		_halterEscapeTask = null;
		
		if (_intervalTask != null)
		{
			_intervalTask.cancel(false);
		}
		_intervalTask = null;
		
		if (_lockUpDoorOfAltarTask != null)
		{
			_lockUpDoorOfAltarTask.cancel(false);
		}
		_lockUpDoorOfAltarTask = null;
		
		if (_movieTask != null)
		{
			_movieTask.cancel(false);
		}
		_movieTask = null;
		
		if (_openDoorOfAltarTask != null)
		{
			_openDoorOfAltarTask.cancel(false);
		}
		_openDoorOfAltarTask = null;
		
		if (_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = null;
		
		// Delete all monsters
		deleteVanHalter();
		deleteTriolRevelation();
		deleteRoyalGuardHepler();
		deleteRoyalGuardCaptain();
		deleteRoyalGuard();
		deleteRitualSacrifice();
		deleteRitualOffering();
		deleteGuardOfAltar();
		deleteCameraMarker();
		
		// Clear flag.
		_isLocked = false;
		_isCaptainSpawned = false;
		_isHelperCalled = false;
		_isHalterSpawned = false;
		
		// Set door state
		closeDoorOfSacrifice();
		openDoorOfAltar(true);
		
		// Respawn monsters.
		spawnTriolRevelation();
		spawnRoyalGuard();
		spawnRitualOffering();
		spawnVanHalter();
		
		GrandBossManager.getInstance().setBossStatus(29062, NOTSPAWN);
		
		// Set time up.
		if (_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new TimeUp(), Config.HPH_ACTIVITYTIMEOFHALTER);
	}
	
	// Time up.
	protected class TimeUp implements Runnable
	{
		@Override
		public void run()
		{
			enterInterval();
		}
	}
	
	// Appearance movie.
	private class Movie implements Runnable
	{
		private final int _distance = 6502500;
		private final int _taskId;
		
		public Movie(int taskId)
		{
			_taskId = taskId;
		}
		
		@Override
		public void run()
		{
			_vanHalter.setHeading(16384);
			_vanHalter.setTarget(_ritualOffering);
			
			switch (_taskId)
			{
				case 1:
					GrandBossManager.getInstance().setBossStatus(29062, ALIVE);
					
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 50, 90, 0, 0, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(2), 16);
					
					break;
				
				case 2:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_cameraMarker.get(5)) <= _distance)
						{
							_cameraMarker.get(5).broadcastPacket(new SpecialCamera(_cameraMarker.get(5).getObjectId(), 1842, 100, -3, 0, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(3), 1);
					
					break;
				
				case 3:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_cameraMarker.get(5)) <= _distance)
						{
							_cameraMarker.get(5).broadcastPacket(new SpecialCamera(_cameraMarker.get(5).getObjectId(), 1861, 97, -10, 1500, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(4), 1500);
					
					break;
				
				case 4:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_cameraMarker.get(4)) <= _distance)
						{
							_cameraMarker.get(4).broadcastPacket(new SpecialCamera(_cameraMarker.get(4).getObjectId(), 1876, 97, 12, 0, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(5), 1);
					
					break;
				
				case 5:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_cameraMarker.get(4)) <= _distance)
						{
							_cameraMarker.get(4).broadcastPacket(new SpecialCamera(_cameraMarker.get(4).getObjectId(), 1839, 94, 0, 1500, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(6), 1500);
					
					break;
				
				case 6:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_cameraMarker.get(3)) <= _distance)
						{
							_cameraMarker.get(3).broadcastPacket(new SpecialCamera(_cameraMarker.get(3).getObjectId(), 1872, 94, 15, 0, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(7), 1);
					
					break;
				
				case 7:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_cameraMarker.get(3)) <= _distance)
						{
							_cameraMarker.get(3).broadcastPacket(new SpecialCamera(_cameraMarker.get(3).getObjectId(), 1839, 92, 0, 1500, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(8), 1500);
					
					break;
				
				case 8:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_cameraMarker.get(2)) <= _distance)
						{
							_cameraMarker.get(2).broadcastPacket(new SpecialCamera(_cameraMarker.get(2).getObjectId(), 1872, 92, 15, 0, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(9), 1);
					
					break;
				
				case 9:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_cameraMarker.get(2)) <= _distance)
						{
							_cameraMarker.get(2).broadcastPacket(new SpecialCamera(_cameraMarker.get(2).getObjectId(), 1839, 90, 5, 1500, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(10), 1500);
					
					break;
				
				case 10:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_cameraMarker.get(1)) <= _distance)
						{
							_cameraMarker.get(1).broadcastPacket(new SpecialCamera(_cameraMarker.get(1).getObjectId(), 1872, 90, 5, 0, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(11), 1);
					
					break;
				
				case 11:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_cameraMarker.get(1)) <= _distance)
						{
							_cameraMarker.get(1).broadcastPacket(new SpecialCamera(_cameraMarker.get(1).getObjectId(), 2002, 90, 2, 1500, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(12), 2000);
					
					break;
				
				case 12:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 50, 90, 10, 0, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(13), 1000);
					
					break;
				
				case 13:
					// High Priestess van Halter uses the skill to kill Ritual Offering.
					L2Skill skill = SkillTable.getInstance().getInfo(1168, 7);
					_ritualOffering.setIsInvul(false);
					_vanHalter.setTarget(_ritualOffering);
					// _vanHalter.setIsImmobilized(false);
					_vanHalter.doCast(skill);
					// _vanHalter.setIsImmobilized(true);
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(14), 4700);
					
					break;
				
				case 14:
					_ritualOffering.setIsInvul(false);
					_ritualOffering.reduceCurrentHp(_ritualOffering.getMaxHp() + 1, _vanHalter);
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(15), 4300);
					
					break;
				
				case 15:
					spawnRitualSacrifice();
					deleteRitualOffering();
					
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 100, 90, 15, 1500, 15000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(16), 2000);
					
					break;
				
				case 16:
					// Set camera.
					for (L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if (pc.getPlanDistanceSq(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 5200, 90, -10, 9500, 6000));
						}
					}
					
					// Set next task.
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(17), 6000);
					
					break;
				
				case 17:
					deleteRitualSacrifice();
					deleteCameraMarker();
					// _vanHalter.setIsImmobilized(false);
					_vanHalter.setIsInvul(false);
					
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(18), 1000);
					
					break;
				
				case 18:
					combatBeginning();
					if (_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
			}
		}
	}
	
	@Override
	public void run()
	{
	}
}

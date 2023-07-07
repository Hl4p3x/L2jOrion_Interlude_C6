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
package l2jorion.game.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.model.L2World;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.form.ZoneCylinder;
import l2jorion.game.model.zone.type.L2BossZone;
import l2jorion.game.skills.Stats;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.StatsSet;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

public class RaidBossSpawnManager
{
	private static Logger LOG = LoggerFactory.getLogger(RaidBossSpawnManager.class);
	
	protected static final Map<Integer, L2RaidBossInstance> _bosses = new ConcurrentHashMap<>();
	public static final Map<Integer, L2RaidBossInstance> _bossesForCommand = new ConcurrentHashMap<>();
	protected static final Map<Integer, L2Spawn> _spawns = new ConcurrentHashMap<>();
	protected static final Map<Integer, StatsSet> _storedInfo = new ConcurrentHashMap<>();
	protected static final Map<Integer, ScheduledFuture<?>> _schedules = new ConcurrentHashMap<>();
	protected static L2BossZone _zone;
	
	public static List<L2RaidBossInstance> BOSSES_LIST = new ArrayList<>();
	
	private final SimpleDateFormat date = new SimpleDateFormat("H:mm:ss yyyy/MM/dd");
	
	public static enum StatusEnum
	{
		ALIVE,
		DEAD,
		UNDEFINED
	}
	
	protected RaidBossSpawnManager()
	{
	}
	
	public void load()
	{
		_bosses.clear();
		_bossesForCommand.clear();
		_schedules.clear();
		_storedInfo.clear();
		_spawns.clear();
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement("SELECT * from raidboss_spawnlist ORDER BY boss_id");
			ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template;
			while (rset.next())
			{
				StatsSet info = new StatsSet();
				int bossId = rset.getInt("boss_id");
				info.set("loc_x", rset.getInt("loc_x"));
				info.set("loc_y", rset.getInt("loc_y"));
				info.set("loc_z", rset.getInt("loc_z"));
				info.set("heading", rset.getInt("heading"));
				info.set("respawnTime", rset.getLong("respawn_time"));
				
				double HP = rset.getDouble("currentHP");
				int true_HP = (int) HP;
				info.set("currentHP", true_HP);
				double MP = rset.getDouble("currentMP");
				int true_MP = (int) MP;
				info.set("currentMP", true_MP);
				
				_storedInfo.put(bossId, info);
				
				template = getValidTemplate(rset.getInt("boss_id"));
				if (template != null)
				{
					spawnDat = new L2Spawn(template);
					spawnDat.setLocx(rset.getInt("loc_x"));
					spawnDat.setLocy(rset.getInt("loc_y"));
					spawnDat.setLocz(rset.getInt("loc_z"));
					spawnDat.setAmount(rset.getInt("amount"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnMinDelay(rset.getInt("respawn_min_delay"));
					spawnDat.setRespawnMaxDelay(rset.getInt("respawn_max_delay"));
					
					addNewSpawn(spawnDat, rset.getLong("respawn_time"), rset.getDouble("currentHP"), rset.getDouble("currentMP"), false);
				}
				else
				{
					LOG.warn("RaidBossSpawnManager: Could not load raidboss #" + rset.getInt("boss_id") + " from DB");
				}
			}
			
			LOG.info("RaidBossSpawnManager: Loaded " + _bosses.size() + " Instances");
			LOG.info("RaidBossSpawnManager: Scheduled " + _schedules.size() + " Instances");
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
		}
		catch (final SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.warn("RaidBossSpawnManager: Couldnt load raidboss_spawnlist table");
				e.printStackTrace();
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private static class spawnSchedule implements Runnable
	{
		private final int bossId;
		
		public spawnSchedule(int npcId)
		{
			bossId = npcId;
		}
		
		@Override
		public void run()
		{
			L2RaidBossInstance raidboss = null;
			
			if (bossId == 25328)
			{
				raidboss = DayNightSpawnManager.getInstance().handleBoss(_spawns.get(bossId));
			}
			else
			{
				raidboss = (L2RaidBossInstance) _spawns.get(bossId).doSpawn();
			}
			
			if (raidboss != null)
			{
				raidboss.setRaidStatus(StatusEnum.ALIVE);
				
				StatsSet info = new StatsSet();
				info.set("currentHP", raidboss.getCurrentHp());
				info.set("currentMP", raidboss.getCurrentMp());
				info.set("respawnTime", 0L);
				
				_storedInfo.put(bossId, info);
				
				GmListTable.broadcastMessageToGMs("Spawning Raid Boss " + raidboss.getName());
				if (Config.ANNOUNCE_TO_ALL_SPAWN_RB)
				{
					Announcements.getInstance().announceToAll("Raid boss " + raidboss.getName() + " spawned in the world.");
				}
				
				if (Config.ANNOUNCE_TO_ALL_SPAWN_JUST_RB && !(bossId == 22217) && !(bossId == 22215) && !(bossId == 22319) && !(bossId == 22216) && !(bossId == 22318))
				{
					Announcements.getInstance().announceWithServerName("The Raid Boss " + raidboss.getName() + " spawned!");
				}
				
				_bosses.put(bossId, raidboss);
				
				if (Config.RON_CUSTOM)
				{
					addRaidBossZone(raidboss);
				}
			}
			
			_schedules.remove(bossId);
		}
	}
	
	public void updateStatus(L2RaidBossInstance boss, boolean isBossDead)
	{
		final StatsSet info = _storedInfo.get(boss.getNpcId());
		if (info == null)
		{
			return;
		}
		
		if (isBossDead)
		{
			boss.setRaidStatus(StatusEnum.DEAD);
			
			long respawnTime;
			final int RespawnMinDelay = boss.getSpawn().getRespawnMinDelay();
			final int RespawnMaxDelay = boss.getSpawn().getRespawnMaxDelay();
			
			final long respawn_delay = Rnd.get((int) (RespawnMinDelay * 1000 * Config.RAID_MIN_RESPAWN_MULTIPLIER), (int) (RespawnMaxDelay * 1000 * Config.RAID_MAX_RESPAWN_MULTIPLIER));
			respawnTime = Calendar.getInstance().getTimeInMillis() + respawn_delay;
			
			info.set("currentHP", boss.getMaxHp());
			info.set("currentMP", boss.getMaxMp());
			info.set("respawnTime", respawnTime);
			
			GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
			gc.clear();
			gc.setTimeInMillis(respawnTime);
			final String text = boss.getName() + " killed. Next respawn: " + DateFormat.getDateTimeInstance().format(gc.getTime());
			Log.add(text, "RaidBosses");
			
			info.set("killed_time", date.format(new Date(System.currentTimeMillis())));
			info.set("next_respawn", DateFormat.getDateTimeInstance().format(gc.getTime()));
			
			if (!_schedules.containsKey(boss.getNpcId()))
			{
				ScheduledFuture<?> futureSpawn;
				futureSpawn = ThreadPoolManager.getInstance().scheduleGeneral(new spawnSchedule(boss.getNpcId()), respawn_delay);
				
				_schedules.put(boss.getNpcId(), futureSpawn);
				
				// To update immediately the database, used for website to show up RaidBoss status.
				if (Config.SAVE_RAIDBOSS_STATUS_INTO_DB)
				{
					updateDb();
				}
			}
		}
		else
		{
			boss.setRaidStatus(StatusEnum.ALIVE);
			
			info.set("currentHP", boss.getCurrentHp());
			info.set("currentMP", boss.getCurrentMp());
			info.set("respawnTime", 0L);
			info.set("killed_time", "-");
			info.set("next_respawn", "-");
		}
		
		// _storedInfo.remove(boss.getNpcId());
		_storedInfo.put(boss.getNpcId(), info);
		
	}
	
	public void addNewSpawn(final L2Spawn spawnDat, final long respawnTime, double currentHP, final double currentMP, final boolean storeInDb)
	{
		if (spawnDat == null)
		{
			return;
		}
		
		if (_spawns.containsKey(spawnDat.getNpcid()))
		{
			return;
		}
		
		final int bossId = spawnDat.getNpcid();
		final long time = Calendar.getInstance().getTimeInMillis();
		
		SpawnTable.getInstance().addNewSpawn(spawnDat, false);
		
		L2RaidBossInstance raidboss = null;
		
		if (respawnTime == 0L || time > respawnTime)
		{
			if (bossId == 25328)
			{
				raidboss = DayNightSpawnManager.getInstance().handleBoss(spawnDat);
			}
			else
			{
				raidboss = (L2RaidBossInstance) spawnDat.doSpawn();
			}
			
			if (raidboss != null)
			{
				final double bonus = raidboss.getStat().calcStat(Stats.MAX_HP, 1, raidboss, null);
				// if new spawn, the currentHp is equal to maxHP/bonus, so set it to max
				if ((int) (bonus * currentHP) == raidboss.getCurrentHp())
				{
					currentHP = (raidboss.getCurrentHp());
				}
				
				raidboss.setCurrentHp(currentHP);
				raidboss.setCurrentMp(currentMP);
				raidboss.setRaidStatus(StatusEnum.ALIVE);
				
				_bosses.put(bossId, raidboss);
				
				final StatsSet info = new StatsSet();
				info.set("currentHP", currentHP);
				info.set("currentMP", currentMP);
				info.set("respawnTime", 0L);
				
				_storedInfo.put(bossId, info);
				
				if (Config.RON_CUSTOM)
				{
					addRaidBossZone(raidboss);
				}
			}
		}
		else
		{
			ScheduledFuture<?> futureSpawn;
			final long spawnTime = respawnTime - Calendar.getInstance().getTimeInMillis();
			
			futureSpawn = ThreadPoolManager.getInstance().scheduleGeneral(new spawnSchedule(bossId), spawnTime);
			
			_schedules.put(bossId, futureSpawn);
		}
		
		_spawns.put(bossId, spawnDat);
		_bossesForCommand.put(bossId, (L2RaidBossInstance) spawnDat.getBossInfo());
		
		if (storeInDb)
		{
			Connection con = null;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO raidboss_spawnlist (boss_id,amount,loc_x,loc_y,loc_z,heading,respawn_time,currentHp,currentMp) values(?,?,?,?,?,?,?,?,?)");
				statement.setInt(1, spawnDat.getNpcid());
				statement.setInt(2, spawnDat.getAmount());
				statement.setInt(3, spawnDat.getLocx());
				statement.setInt(4, spawnDat.getLocy());
				statement.setInt(5, spawnDat.getLocz());
				statement.setInt(6, spawnDat.getHeading());
				statement.setLong(7, respawnTime);
				statement.setDouble(8, currentHP);
				statement.setDouble(9, currentMP);
				statement.execute();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				// problem with storing spawn
				LOG.warn("RaidBossSpawnManager: Could not store raidboss #" + bossId + " in the DB:" + e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	public void deleteSpawn(final L2Spawn spawnDat, final boolean updateDb)
	{
		if (spawnDat == null)
		{
			return;
		}
		
		if (!_spawns.containsKey(spawnDat.getNpcid()))
		{
			return;
		}
		
		final int bossId = spawnDat.getNpcid();
		
		SpawnTable.getInstance().deleteSpawn(spawnDat, false);
		_spawns.remove(bossId);
		
		if (_bosses.containsKey(bossId))
		{
			_bosses.remove(bossId);
		}
		
		if (_schedules.containsKey(bossId))
		{
			final ScheduledFuture<?> f = _schedules.get(bossId);
			f.cancel(true);
			_schedules.remove(bossId);
		}
		
		if (_storedInfo.containsKey(bossId))
		{
			_storedInfo.remove(bossId);
		}
		
		if (updateDb)
		{
			Connection con = null;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("DELETE FROM raidboss_spawnlist WHERE boss_id=?");
				statement.setInt(1, bossId);
				statement.execute();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("RaidBossSpawnManager: Could not remove raidboss #" + bossId + " from DB: " + e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	private void updateDb()
	{
		Connection con = null;
		
		for (Integer bossId : _storedInfo.keySet())
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				
				if (bossId == null)
				{
					continue;
				}
				
				L2RaidBossInstance boss = _bosses.get(bossId);
				
				if (boss == null)
				{
					continue;
				}
				
				if (boss.getRaidStatus().equals(StatusEnum.ALIVE))
				{
					updateStatus(boss, false);
				}
				
				StatsSet info = _storedInfo.get(bossId);
				
				if (info == null)
				{
					continue;
				}
				
				PreparedStatement statement = con.prepareStatement("UPDATE raidboss_spawnlist set respawn_time = ?, killed_time = ?, next_respawn = ?, currentHP = ?, currentMP = ? where boss_id = ?");
				statement.setLong(1, info.getLong("respawnTime"));
				statement.setString(2, info.getString("killed_time"));
				statement.setString(3, info.getString("next_respawn"));
				
				double hp = boss.getCurrentHp();
				double mp = boss.getCurrentMp();
				
				if (boss.isDead())
				{
					hp = boss.getMaxHp();
					mp = boss.getMaxMp();
				}
				
				statement.setDouble(4, hp);
				statement.setDouble(5, mp);
				
				// statement.setDouble(4, info.getDouble("currentHP"));
				// statement.setDouble(5, info.getDouble("currentMP"));
				
				statement.setInt(6, bossId);
				statement.execute();
				DatabaseUtils.close(statement);
			}
			catch (SQLException e)
			{
				LOG.warn("RaidBossSpawnManager: Couldn't update raidboss_spawnlist table:" + e.getMessage(), e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	public String[] getAllRaidBossStatus()
	{
		final String[] msg = new String[_bosses == null ? 0 : _bosses.size()];
		
		if (_bosses == null)
		{
			msg[0] = "None";
			return msg;
		}
		
		int index = 0;
		
		for (final int i : _bosses.keySet())
		{
			L2RaidBossInstance boss = _bosses.get(i);
			
			msg[index] = boss.getName() + ": " + boss.getRaidStatus().name();
			index++;
			
			boss = null;
		}
		
		return msg;
	}
	
	public String getRaidBossStatus(final int bossId)
	{
		String msg = "RaidBoss Status....\n";
		
		if (_bosses == null)
		{
			msg += "None";
			return msg;
		}
		
		if (_bosses.containsKey(bossId))
		{
			final L2RaidBossInstance boss = _bosses.get(bossId);
			
			msg += boss.getName() + ": " + boss.getRaidStatus().name();
		}
		
		return msg;
	}
	
	public StatusEnum getRaidBossStatusId(final int bossId)
	{
		if (_bosses.containsKey(bossId))
		{
			return _bosses.get(bossId).getRaidStatus();
		}
		else if (_schedules.containsKey(bossId))
		{
			return StatusEnum.DEAD;
		}
		else
		{
			return StatusEnum.UNDEFINED;
		}
	}
	
	public L2NpcTemplate getValidTemplate(final int bossId)
	{
		final L2NpcTemplate template = NpcTable.getInstance().getTemplate(bossId);
		if (template == null)
		{
			return null;
		}
		
		if (!template.type.equalsIgnoreCase("L2RaidBoss"))
		{
			return null;
		}
		
		return template;
	}
	
	public void notifySpawnNightBoss(final L2RaidBossInstance raidboss)
	{
		StatsSet info = new StatsSet();
		info.set("currentHP", raidboss.getCurrentHp());
		info.set("currentMP", raidboss.getCurrentMp());
		info.set("respawnTime", 0L);
		
		raidboss.setRaidStatus(StatusEnum.ALIVE);
		
		_storedInfo.put(raidboss.getNpcId(), info);
		
		info = null;
		
		GmListTable.broadcastMessageToGMs("Spawning Raid Boss " + raidboss.getName());
		
		_bosses.put(raidboss.getNpcId(), raidboss);
	}
	
	public boolean isDefined(final int bossId)
	{
		return _spawns.containsKey(bossId);
	}
	
	public Map<Integer, L2RaidBossInstance> getBosses()
	{
		return _bosses;
	}
	
	public Map<Integer, L2Spawn> getSpawns()
	{
		return _spawns;
	}
	
	public void reloadBosses()
	{
		load();
	}
	
	public void DataSave()
	{
		updateDb();
		
		_bossesForCommand.clear();
		_bosses.clear();
		
		if (_schedules != null)
		{
			for (Integer bossId : _schedules.keySet())
			{
				ScheduledFuture<?> f = _schedules.get(bossId);
				f.cancel(true);
			}
			_schedules.clear();
		}
		_storedInfo.clear();
		_spawns.clear();
	}
	
	public StatsSet getStatsSet(int bossId)
	{
		return _storedInfo.get(bossId);
	}
	
	public void findRaidBoss(final L2PcInstance activeChar, final int npcId, final int teleportIndex)
	{
		int index = 0;
		for (final L2RaidBossInstance spawn : _bosses.values())
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
			activeChar.sendMessage("No Raid Boss found.");
		}
	}
	
	public L2RaidBossInstance getBoss(final int bossId)
	{
		return _bosses.get(bossId);
	}
	
	public static RaidBossSpawnManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public static void addRaidBossZone(L2RaidBossInstance raidboss)
	{
		_zone = GrandBossManager.getInstance().getZone(raidboss.getX(), raidboss.getY(), raidboss.getZ());
		if (_zone == null)
		{
			int levelLimit = (raidboss.getLevel() + 8);
			L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();
			L2ZoneType temp = new L2BossZone(99000 + raidboss.getNpcId());
			temp.setName("zone: " + raidboss.getName() + " Level limit:" + levelLimit);
			temp.setParameter("maxLevel", "" + levelLimit);
			temp.setParameter("pvp", "true");
			temp.setParameter("teleportOut", "false");
			temp.setZone(new ZoneCylinder(raidboss.getX(), raidboss.getY(), (raidboss.getZ() - 500), (raidboss.getZ() + 500), 1000));
			int ax, ay, bx, by;
			
			for (int x = 0; x < worldRegions.length; x++)
			{
				for (int y = 0; y < worldRegions[x].length; y++)
				{
					ax = x - L2World.OFFSET_X << L2World.SHIFT_BY;
					bx = x + 1 - L2World.OFFSET_X << L2World.SHIFT_BY;
					ay = y - L2World.OFFSET_Y << L2World.SHIFT_BY;
					by = y + 1 - L2World.OFFSET_Y << L2World.SHIFT_BY;
					
					if (temp.getZone().intersectsRectangle(ax, bx, ay, by))
					{
						worldRegions[x][y].addZone(temp);
					}
				}
			}
			GrandBossManager.getInstance().addZone((L2BossZone) temp);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final RaidBossSpawnManager _instance = new RaidBossSpawnManager();
	}
}

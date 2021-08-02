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
package l2jorion.game.model.spawn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.AutoSpawnInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.entity.Announcements;
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

public class AutoSpawn
{
	protected static final Logger LOG = LoggerFactory.getLogger(AutoSpawn.class);
	
	private static final int DEFAULT_INITIAL_SPAWN = 30000; // 30 seconds after registration
	private static final int DEFAULT_RESPAWN = 3600000; // 1 hour in millisecs
	private static final int DEFAULT_DESPAWN = 3600000; // 1 hour in millisecs
	
	protected static Map<Integer, AutoSpawnInstance> _Mobs = new ConcurrentHashMap<>();
	protected static Map<Integer, L2Spawn> _spawns = new ConcurrentHashMap<>();
	protected static Map<Integer, StatsSet> _storedInfo = new FastMap<>();
	protected static Map<Integer, ScheduledFuture<?>> _schedules = new ConcurrentHashMap<>();
	
	protected final SimpleDateFormat date = new SimpleDateFormat("H:mm:ss yyyy/MM/dd");
	
	protected boolean _activeState = true;
	
	protected AutoSpawn()
	{
		restoreSpawnData();
	}
	
	public static AutoSpawn getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public int size()
	{
		synchronized (_Mobs)
		{
			return _Mobs.size();
		}
	}
	
	private void restoreSpawnData()
	{
		int numLoaded = 0;
		Connection con = null;
		
		try
		{
			PreparedStatement statement = null;
			PreparedStatement statement2 = null;
			ResultSet rs = null;
			ResultSet rs2 = null;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			// Restore spawn group data, then the location data.
			statement = con.prepareStatement("SELECT * FROM random_spawn ORDER BY groupId ASC");
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				StatsSet info = new StatsSet();
				int bossId = rs.getInt("npcId");
				// Register random spawn group, set various options on the created spawn instance.
				final AutoSpawnInstance spawnInst = registerSpawn(bossId, rs.getInt("initialDelay"), rs.getInt("respawnDelay"), rs.getInt("despawnDelay"), rs.getLong("respawn_time"));
				
				info.set("respawnTime", rs.getLong("respawn_time"));
				_storedInfo.put(bossId, info);
				
				spawnInst.setSpawnCount(rs.getInt("count"));
				spawnInst.setBroadcast(rs.getBoolean("broadcastSpawn"));
				spawnInst.setRandomSpawn(rs.getBoolean("randomSpawn"));
				numLoaded++;
				
				// Restore the spawn locations for this spawn group/instance.
				statement2 = con.prepareStatement("SELECT * FROM random_spawn_loc WHERE groupId=?");
				statement2.setInt(1, rs.getInt("groupId"));
				rs2 = statement2.executeQuery();
				
				while (rs2.next())
				{
					// Add each location to the spawn group/instance.
					spawnInst.addSpawnLocation(rs2.getInt("x"), rs2.getInt("y"), rs2.getInt("z"), rs2.getInt("heading"));
				}
				
				statement2.close();
				rs2.close();
			}
			
			DatabaseUtils.close(statement);
			rs.close();
			
			if (Config.DEBUG)
			{
				LOG.debug("AutoSpawnHandler: Loaded " + numLoaded + " spawn group(s) from the database.");
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("AutoSpawnHandler: Could not restore spawn data: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public AutoSpawnInstance registerSpawn(final int npcId, final int[][] spawnPoints, int initialDelay, int respawnDelay, int despawnDelay, long respawnTime)
	{
		if (initialDelay < 0)
		{
			initialDelay = DEFAULT_INITIAL_SPAWN;
		}
		
		if (respawnDelay < 0)
		{
			respawnDelay = DEFAULT_RESPAWN;
		}
		
		if (despawnDelay < 0)
		{
			despawnDelay = DEFAULT_DESPAWN;
		}
		
		final AutoSpawnInstance newSpawn = new AutoSpawnInstance(npcId, initialDelay, respawnDelay, despawnDelay, respawnTime);
		
		if (spawnPoints != null)
		{
			for (final int[] spawnPoint : spawnPoints)
			{
				newSpawn.addSpawnLocation(spawnPoint);
			}
		}
		
		final int newId = IdFactory.getInstance().getNextId();
		newSpawn._objectId = newId;
		
		synchronized (_Mobs)
		{
			_Mobs.put(newId, newSpawn);
		}
		
		setSpawnActive(newSpawn, true);
		
		if (Config.DEBUG)
		{
			LOG.debug("AutoSpawnHandler: Registered auto spawn for NPC ID " + npcId + " (Object ID = " + newId + ").");
		}
		
		return newSpawn;
	}
	
	public AutoSpawnInstance registerSpawn(final int npcId, final int initialDelay, final int respawnDelay, final int despawnDelay, long respawnTime)
	{
		return registerSpawn(npcId, null, initialDelay, respawnDelay, despawnDelay, respawnTime);
	}
	
	public boolean removeSpawn(final AutoSpawnInstance spawnInst)
	{
		synchronized (_Mobs)
		{
			
			if (!_Mobs.containsValue(spawnInst))
			{
				return false;
			}
			
			// Try to remove from the list of registered spawns if it exists.
			_Mobs.remove(spawnInst.getNpcId());
			
			synchronized (_schedules)
			{
				
				// Cancel the currently associated running scheduled task.
				final ScheduledFuture<?> respawnTask = _schedules.remove(spawnInst._objectId);
				
				try
				{
					respawnTask.cancel(false);
					
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					LOG.warn("AutoSpawnHandler: Could not auto spawn for NPC ID " + spawnInst._npcId + " (Object ID = " + spawnInst._objectId + "): " + e);
					
					return false;
				}
				
			}
			
			if (Config.DEBUG)
			{
				LOG.debug("AutoSpawnHandler: Removed auto spawn for NPC ID " + spawnInst._npcId + " (Object ID = " + spawnInst._objectId + ").");
			}
			
		}
		
		return true;
	}
	
	public void removeSpawn(final int objectId)
	{
		AutoSpawnInstance spawn_inst = null;
		
		synchronized (_Mobs)
		{
			spawn_inst = _Mobs.get(objectId);
		}
		
		removeSpawn(spawn_inst);
	}
	
	public void setSpawnActive(final AutoSpawnInstance spawnInst, final boolean isActive)
	{
		if (spawnInst == null)
		{
			return;
		}
		
		final int objectId = spawnInst._objectId;
		
		if (isSpawnRegistered(objectId))
		{
			ScheduledFuture<?> spawnTask = null;
			
			if (isActive)
			{
				AutoSpawner rs = new AutoSpawner(objectId);
				
				if (spawnInst._desDelay > 0)
				{
					spawnTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(rs, spawnInst._initDelay, spawnInst._resDelay);
				}
				else
				{
					spawnTask = ThreadPoolManager.getInstance().scheduleEffect(rs, spawnInst._initDelay);
				}
				
				synchronized (_schedules)
				{
					_schedules.put(objectId, spawnTask);
				}
			}
			else
			{
				AutoDespawner rd = new AutoDespawner(objectId);
				
				synchronized (_schedules)
				{
					spawnTask = _schedules.remove(objectId);
				}
				
				if (spawnTask != null)
				{
					spawnTask.cancel(false);
				}
				
				ThreadPoolManager.getInstance().scheduleEffect(rd, 0);
			}
			spawnInst.setSpawnActive(isActive);
		}
	}
	
	public void setAllActive(final boolean isActive)
	{
		if (_activeState == isActive)
		{
			return;
		}
		
		Collection<AutoSpawnInstance> instances;
		synchronized (_Mobs)
		{
			instances = _Mobs.values();
		}
		
		for (final AutoSpawnInstance spawnInst : instances)
		{
			setSpawnActive(spawnInst, isActive);
		}
		
		_activeState = isActive;
	}
	
	public final long getTimeToNextSpawn(final AutoSpawnInstance spawnInst)
	{
		if (spawnInst == null)
		{
			return -1;
		}
		
		final int objectId = spawnInst.getObjectId();
		
		synchronized (_schedules)
		{
			
			final ScheduledFuture<?> future_task = _schedules.get(objectId);
			if (future_task != null)
			{
				return future_task.getDelay(TimeUnit.MILLISECONDS);
			}
		}
		
		return -1;
	}
	
	public final AutoSpawnInstance getAutoSpawnInstance(final int id, final boolean isObjectId)
	{
		if (isObjectId)
		{
			return _Mobs.get(id);
		}
		
		Collection<AutoSpawnInstance> instances;
		synchronized (_Mobs)
		{
			instances = _Mobs.values();
		}
		
		for (final AutoSpawnInstance spawnInst : instances)
		{
			if (spawnInst.getNpcId() == id)
			{
				return spawnInst;
			}
		}
		return null;
	}
	
	public Map<Integer, AutoSpawnInstance> getAutoSpawnInstances(final int npcId)
	{
		final Map<Integer, AutoSpawnInstance> spawnInstList = new FastMap<>();
		
		Collection<AutoSpawnInstance> instances;
		synchronized (_Mobs)
		{
			instances = _Mobs.values();
		}
		
		for (final AutoSpawnInstance spawnInst : instances)
		{
			if (spawnInst.getNpcId() == npcId)
			{
				spawnInstList.put(spawnInst.getObjectId(), spawnInst);
			}
		}
		
		return spawnInstList;
	}
	
	public final boolean isSpawnRegistered(final int objectId)
	{
		synchronized (_Mobs)
		{
			return _Mobs.containsKey(objectId);
		}
		
	}
	
	public boolean isSpawnRegistered(final AutoSpawnInstance spawnInst)
	{
		synchronized (_Mobs)
		{
			return _Mobs.containsValue(spawnInst);
		}
		
	}
	
	private class AutoSpawner implements Runnable
	{
		private final int _objectId;
		
		protected AutoSpawner(final int objectId)
		{
			_objectId = objectId;
		}
		
		@Override
		public void run()
		{
			try
			{
				AutoSpawnInstance spawnInst = null;
				
				synchronized (_Mobs)
				{
					// Retrieve the required spawn instance for this spawn task.
					spawnInst = _Mobs.get(_objectId);
				}
				
				// If the spawn is not scheduled to be active, cancel the spawn
				// task.
				if (!spawnInst.isSpawnActive())
				{
					return;
				}
				
				Location[] locationList = spawnInst.getLocationList();
				
				// If there are no set co-ordinates, cancel the spawn task.
				if (locationList.length == 0)
				{
					LOG.info("AutoSpawnHandler: No location co-ords specified for spawn instance (Object ID = " + _objectId + ").");
					return;
				}
				
				final int locationCount = locationList.length;
				int locationIndex = Rnd.nextInt(locationCount);
				
				/*
				 * If random spawning is disabled, the spawn at the next set of co-ordinates after the last. If the index is greater than the number of possible spawns, reset the counter to zero.
				 */
				if (!spawnInst.isRandomSpawn())
				{
					locationIndex = spawnInst._lastLocIndex;
					locationIndex++;
					
					if (locationIndex == locationCount)
					{
						locationIndex = 0;
					}
					
					spawnInst._lastLocIndex = locationIndex;
				}
				
				// Set the X, Y and Z co-ordinates, where this spawn will take
				// place.
				final int x = locationList[locationIndex].getX();
				final int y = locationList[locationIndex].getY();
				final int z = locationList[locationIndex].getZ();
				final int heading = locationList[locationIndex].getHeading();
				
				// Fetch the template for this NPC ID and create a new spawn.
				L2NpcTemplate npcTemp = NpcTable.getInstance().getTemplate(spawnInst.getNpcId());
				
				if (npcTemp == null)
				{
					LOG.warn("Couldnt find NPC id" + spawnInst.getNpcId() + " Try to update your DP");
					return;
				}
				
				L2Spawn spawnDat = new L2Spawn(npcTemp);
				
				final int mobId = spawnInst.getNpcId();
				final long time = Calendar.getInstance().getTimeInMillis();
				long respawnTime = spawnInst.getRespawnTime();
				
				spawnDat.setLocx(x);
				spawnDat.setLocy(y);
				spawnDat.setLocz(z);
				
				if (heading != -1)
				{
					spawnDat.setHeading(heading);
				}
				
				spawnDat.setAmount(spawnInst.getSpawnCount());
				
				L2NpcInstance mob = null;
				
				// Add the new spawn information to the spawn table, but do not store it.
				SpawnTable.getInstance().addNewSpawn(spawnDat, false);
				
				if (respawnTime == 0L || time > respawnTime)
				{
					if (spawnInst._spawnCount == 1)
					{
						mob = spawnDat.doSpawn();
						mob.setXYZ(mob.getX(), mob.getY(), mob.getZ());
						spawnInst.addNpcInstance(mob);
					}
					else
					{
						for (int i = 0; i < spawnInst._spawnCount; i++)
						{
							mob = spawnDat.doSpawn();
							mob.setXYZ(mob.getX() + Rnd.nextInt(50), mob.getY() + Rnd.nextInt(50), mob.getZ());
							spawnInst.addNpcInstance(mob);
						}
					}
					
					if (mob != null)
					{
						final StatsSet info = new StatsSet();
						info.set("respawnTime", 0L);
						setStatsSet(mobId, info);
						
						String nearestTown = MapRegionTable.getInstance().getClosestTownName(mob);
						if (spawnInst.isBroadcasting())
						{
							Announcements.getInstance().announceToAll("The " + mob.getName() + " has spawned near " + nearestTown + "!");
						}
					}
				}
				else
				{
					ScheduledFuture<?> futureSpawn;
					final long spawnTime = respawnTime - Calendar.getInstance().getTimeInMillis();
					
					futureSpawn = ThreadPoolManager.getInstance().scheduleGeneral(new spawnSchedule(mobId), spawnTime);
					
					_schedules.put(mobId, futureSpawn);
				}
				_spawns.put(mobId, spawnDat);
				
				if (spawnInst.getDespawnDelay() > 0)
				{
					AutoDespawner rd = new AutoDespawner(_objectId);
					ThreadPoolManager.getInstance().scheduleAi(rd, spawnInst.getDespawnDelay() - 1000);
				}
			}
			catch (final Exception e)
			{
				LOG.warn("AutoSpawnHandler: An error occurred while initializing spawn instance (ID = " + _objectId + "): " + e);
				e.printStackTrace();
			}
		}
	}
	
	private class AutoDespawner implements Runnable
	{
		private final int _objectId;
		
		protected AutoDespawner(final int objectId)
		{
			_objectId = objectId;
		}
		
		@Override
		public void run()
		{
			try
			{
				AutoSpawnInstance spawnInst = null;
				synchronized (_Mobs)
				{
					spawnInst = _Mobs.get(_objectId);
				}
				
				if (spawnInst == null)
				{
					LOG.info("AutoSpawnHandler: No spawn registered for object ID = " + _objectId + ".");
					return;
				}
				
				final L2NpcInstance[] npc_instances = spawnInst.getNPCInstanceList();
				if (npc_instances == null)
				{
					LOG.info("AutoSpawnHandler: No spawn registered");
					return;
				}
				
				for (final L2NpcInstance npcInst : npc_instances)
				{
					if (npcInst == null)
					{
						continue;
					}
					
					npcInst.deleteMe();
					spawnInst.removeNpcInstance(npcInst);
					
					if (Config.DEBUG)
					{
						LOG.info("AutoSpawnHandler: Spawns removed for spawn instance (Object ID = " + _objectId + ").");
					}
				}
				
				spawnInst = null;
			}
			catch (final Exception e)
			{
				// if(Config.ENABLE_ALL_EXCEPTIONS)
				// e.printStackTrace();
				
				LOG.warn("AutoSpawnHandler: An error occurred while despawning spawn (Object ID = " + _objectId + "): " + e);
				e.printStackTrace();
			}
		}
	}
	
	public void cleanUp()
	{
		updateDb();
		
		_Mobs.clear();
		
		if (_schedules != null)
		{
			for (final Integer bossId : _schedules.keySet())
			{
				final ScheduledFuture<?> f = _schedules.get(bossId);
				f.cancel(true);
			}
			_schedules.clear();
		}
		
		_storedInfo.clear();
		_spawns.clear();
	}
	
	private static void updateDb()
	{
		for (final Integer mobId : _storedInfo.keySet())
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				
				AutoSpawnInstance mob = _Mobs.get(mobId);
				
				if (mob == null)
				{
					continue;
				}
				
				StatsSet info = _storedInfo.get(mobId);
				
				if (info == null)
				{
					continue;
				}
				
				PreparedStatement statement = con.prepareStatement("UPDATE random_spawn set respawn_time = ? where npcId = ?");
				statement.setLong(1, info.getLong("respawnTime"));
				statement.setInt(2, mobId);
				statement.execute();
				DatabaseUtils.close(statement);
			}
			catch (SQLException e)
			{
				LOG.warn("AutoSpawnInstance: Couldnt update random_spawn table " + e.getMessage(), e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	private static void updateDb(int mobId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			StatsSet info = _storedInfo.get(mobId);
			PreparedStatement statement = con.prepareStatement("UPDATE random_spawn set respawn_time = ? where npcId = ?");
			statement.setLong(1, info.getLong("respawnTime"));
			statement.setInt(2, mobId);
			statement.execute();
			DatabaseUtils.close(statement);
		}
		catch (SQLException e)
		{
			LOG.warn("AutoSpawnInstance: Couldnt update random_spawn table " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public static void updateStatus(L2NpcInstance mob, boolean isMobDead)
	{
		if (!_storedInfo.containsKey(mob.getNpcId()))
		{
			return;
		}
		
		Collection<AutoSpawnInstance> instances;
		synchronized (_Mobs)
		{
			instances = _Mobs.values();
		}
		
		if (isMobDead)
		{
			long respawnTime;
			long respawn_delay;
			
			for (AutoSpawnInstance spawnInst : instances)
			{
				if (spawnInst.getNpcId() == mob.getNpcId())
				{
					respawn_delay = spawnInst.getRespawnDelay() * 1000;
					respawnTime = Calendar.getInstance().getTimeInMillis() + respawn_delay;
					
					StatsSet info = getStatsSet(spawnInst.getNpcId());
					info.set("respawnTime", respawnTime);
					
					GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
					gc.clear();
					gc.setTimeInMillis(respawnTime);
					final String text = "" + mob.getName() + " killed. Next respawn: " + DateFormat.getDateTimeInstance().format(gc.getTime());
					Log.add(text, "RaidBosses");
					
					_storedInfo.remove(mob.getNpcId());
					_storedInfo.put(mob.getNpcId(), info);
					
					setStatsSet(spawnInst.getNpcId(), info);
					
					if (!_schedules.containsKey(spawnInst.getNpcId()))
					{
						ScheduledFuture<?> futureSpawn;
						futureSpawn = ThreadPoolManager.getInstance().scheduleGeneral(new spawnSchedule(spawnInst.getNpcId()), respawn_delay);
						
						_schedules.put(spawnInst.getNpcId(), futureSpawn);
					}
				}
			}
		}
	}
	
	public static class spawnSchedule implements Runnable
	{
		private final int mobId;
		
		public spawnSchedule(final int npcId)
		{
			mobId = npcId;
		}
		
		@Override
		public void run()
		{
			L2NpcInstance mob = null;
			
			mob = _spawns.get(mobId).doSpawn();
			
			if (mob != null)
			{
				StatsSet info = new StatsSet();
				info.set("respawnTime", 0L);
				setStatsSet(mobId, info);
			}
			_schedules.remove(mobId);
		}
	}
	
	public static StatsSet getStatsSet(int mobId)
	{
		return _storedInfo.get(mobId);
	}
	
	public static void setStatsSet(int mobId, StatsSet info)
	{
		_storedInfo.put(mobId, info);
		updateDb(mobId);
	}
	
	private static class SingletonHolder
	{
		protected static final AutoSpawn _instance = new AutoSpawn();
	}
}

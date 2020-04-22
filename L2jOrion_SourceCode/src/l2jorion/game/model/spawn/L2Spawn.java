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
package l2jorion.game.model.spawn;

import java.lang.reflect.Constructor;
import java.util.List;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.sql.TerritoryTable;
import l2jorion.game.geo.GeoData;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.random.Rnd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2Spawn
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2Spawn.class);
	
	private L2NpcTemplate _template;
	private int _id;
	private int _location;
	private int _maximumCount;
	private int _currentCount;
	protected int _scheduledCount;
	private int _locX;
	private int _locY;
	private int _locZ;
	private int _heading;
	private int _respawnDelay;
	private int _respawnMinDelay;
	private int _respawnMaxDelay;
	private Constructor<?> _constructor;
	private boolean _doRespawn;
	private int _instanceId = 0;
	
	private L2NpcInstance _lastSpawn;
	private static List<SpawnListener> _spawnListeners = new FastList<>();
	
	class SpawnTask implements Runnable
	{
		private final L2NpcInstance _oldNpc;
		
		public SpawnTask(final L2NpcInstance pOldNpc)
		{
			_oldNpc = pOldNpc;
		}
		
		@Override
		public void run()
		{
			try
			{
				respawnNpc(_oldNpc);
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("", e);
			}
			
			_scheduledCount--;
		}
	}
	
	/**
	 * Constructor of L2Spawn.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Spawn owns generic and static properties (ex : RewardExp, RewardSP, AggroRange...). All of those properties are stored in a different L2NpcTemplate for each type of L2Spawn. Each template is loaded once in the server cache memory (reduce memory use). When a new instance of L2Spawn is
	 * created, server just create a link between the instance and the template. This link is stored in <B>_template</B><BR>
	 * <BR>
	 * Each L2NpcInstance is linked to a L2Spawn that manages its spawn and respawn (delay, location...). This link is stored in <B>_spawn</B> of the L2NpcInstance<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the _template of the L2Spawn</li> <li>Calculate the implementationName used to generate the generic constructor of L2NpcInstance managed by this L2Spawn</li> <li>Create the generic constructor of L2NpcInstance managed by this L2Spawn</li><BR>
	 * <BR>
	 * @param mobTemplate The L2NpcTemplate to link to this L2Spawn
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 */
	public L2Spawn(final L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		// Set the _template of the L2Spawn
		_template = mobTemplate;
		
		if (_template == null)
			return;
		
		// The Name of the L2NpcInstance type managed by this L2Spawn
		String implementationName = _template.type; // implementing class name
		
		if (mobTemplate.npcId == 30995)
		{
			implementationName = "L2RaceManager";
		}
		
		// if (mobTemplate.npcId == 8050)
		
		if (mobTemplate.npcId >= 31046 && mobTemplate.npcId <= 31053)
		{
			implementationName = "L2SymbolMaker";
		}
		
		// Create the generic constructor of L2NpcInstance managed by this L2Spawn
		final Class<?>[] parameters =
		{
			int.class,
			L2NpcTemplate.class
		};
		_constructor = Class.forName("l2jorion.game.model.actor.instance." + implementationName + "Instance").getConstructor(parameters);
		implementationName = null;
	}
	
	/**
	 * @return the maximum number of L2NpcInstance that this L2Spawn can manage.
	 */
	public int getAmount()
	{
		return _maximumCount;
	}
	
	/**
	 * @return the Identifier of this L2Spwan (used as key in the SpawnTable).
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return the Identifier of the location area where L2NpcInstance can be spwaned.
	 */
	public int getLocation()
	{
		return _location;
	}
	
	/**
	 * @return the X position of the spwan point.
	 */
	public int getLocx()
	{
		return _locX;
	}
	
	/**
	 * @return the Y position of the spwan point.
	 */
	public int getLocy()
	{
		return _locY;
	}
	
	/**
	 * @return the Z position of the spwan point.
	 */
	public int getLocz()
	{
		return _locZ;
	}
	
	/**
	 * @return the Identifier of the L2NpcInstance manage by this L2Spwan contained in the L2NpcTemplate.
	 */
	public int getNpcid()
	{
		if (_template == null)
			return -1;
		
		return _template.npcId;
	}
	
	/**
	 * @return the heading of L2NpcInstance when they are spawned.
	 */
	public int getHeading()
	{
		return _heading;
	}
	
	/**
	 * @return the delay between a L2NpcInstance remove and its re-spawn.
	 */
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}
	
	/**
	 * @return Min RaidBoss Spawn delay.
	 */
	public int getRespawnMinDelay()
	{
		return _respawnMinDelay;
	}
	
	/**
	 * @return Max RaidBoss Spawn delay.
	 */
	public int getRespawnMaxDelay()
	{
		return _respawnMaxDelay;
	}
	
	/**
	 * Set the maximum number of L2NpcInstance that this L2Spawn can manage.
	 * @param amount
	 */
	public void setAmount(final int amount)
	{
		_maximumCount = amount;
	}
	
	/**
	 * Set the Identifier of this L2Spwan (used as key in the SpawnTable).
	 * @param id
	 */
	public void setId(final int id)
	{
		_id = id;
	}
	
	/**
	 * Set the Identifier of the location area where L2NpcInstance can be spwaned.
	 * @param location
	 */
	public void setLocation(final int location)
	{
		_location = location;
	}
	
	/**
	 * Set Minimum Respawn Delay.
	 * @param date
	 */
	public void setRespawnMinDelay(final int date)
	{
		_respawnMinDelay = date;
	}
	
	/**
	 * Set Maximum Respawn Delay.
	 * @param date
	 */
	public void setRespawnMaxDelay(final int date)
	{
		_respawnMaxDelay = date;
	}
	
	/**
	 * Set the X position of the spwan point.
	 * @param locx
	 */
	public void setLocx(final int locx)
	{
		_locX = locx;
	}
	
	/**
	 * Set the Y position of the spwan point.
	 * @param locy
	 */
	public void setLocy(final int locy)
	{
		_locY = locy;
	}
	
	/**
	 * Set the Z position of the spwan point.
	 * @param locz
	 */
	public void setLocz(final int locz)
	{
		_locZ = locz;
	}
	
	/**
	 * Set the heading of L2NpcInstance when they are spawned.
	 * @param heading
	 */
	public void setHeading(final int heading)
	{
		_heading = heading;
	}
	
	/**
	 * Kidzor Set the spawn as custom.
	 * @param custom
	 */
	public void setCustom(final boolean custom)
	{
		_customSpawn = custom;
	}
	
	/**
	 * Kidzor Return type of spawn.
	 * @return
	 */
	public boolean isCustom()
	{
		return _customSpawn;
	}
	
	/** If true then spawn is custom */
	private boolean _customSpawn;
	
	private boolean _customBossInstance = false;
	
	/**
	 * @return the _customBossInstance
	 */
	public boolean is_customBossInstance()
	{
		return _customBossInstance;
	}
	
	/**
	 * @param customBossInstance the _customBossInstance to set
	 */
	public void set_customBossInstance(final boolean customBossInstance)
	{
		_customBossInstance = customBossInstance;
	}
	
	/**
	 * Decrease the current number of L2NpcInstance of this L2Spawn and if necessary create a SpawnTask to launch after the respawn Delay.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Decrease the current number of L2NpcInstance of this L2Spawn</li> <li>Check if respawn is possible to prevent multiple respawning caused by lag</li> <li>Update the current number of SpawnTask in progress or stand by of this L2Spawn</li> <li>Create a new SpawnTask to launch after the
	 * respawn Delay</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : A respawn is possible ONLY if _doRespawn=True and _scheduledCount + _currentCount < _maximumCount</B></FONT><BR>
	 * <BR>
	 * @param oldNpc
	 */
	public void decreaseCount(final L2NpcInstance oldNpc)
	{
		// Decrease the current number of L2NpcInstance of this L2Spawn
		_currentCount--;
		
		// Check if respawn is possible to prevent multiple respawning caused by lag
		if (_doRespawn && _scheduledCount + _currentCount < _maximumCount)
		{
			// Update the current number of SpawnTask in progress or stand by of this L2Spawn
			_scheduledCount++;
			
			// Create a new SpawnTask to launch after the respawn Delay
			ThreadPoolManager.getInstance().scheduleGeneral(new SpawnTask(oldNpc), _respawnDelay);
		}
	}
	
	/**
	 * Create the initial spawning and set _doRespawn to True.<BR>
	 * <BR>
	 * @return The number of L2NpcInstance that were spawned
	 */
	public int init()
	{
		while (_currentCount < _maximumCount)
		{
			doSpawn();
		}
		_doRespawn = true;
		
		return _currentCount;
	}
	
	/**
	 * Create a L2NpcInstance in this L2Spawn.
	 * @return
	 */
	public L2NpcInstance spawnOne()
	{
		return doSpawn();
	}
	
	/**
	 * Set _doRespawn to False to stop respawn in this L2Spawn.
	 */
	public void stopRespawn()
	{
		_doRespawn = false;
	}
	
	/**
	 * Set _doRespawn to True to start or restart respawn in this L2Spawn.
	 */
	public void startRespawn()
	{
		_doRespawn = true;
	}
	
	/**
	 * Create the L2NpcInstance, add it to the world and launch its OnSpawn action.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position. The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR>
	 * <BR>
	 * <B><U> Actions for an random spawn into location area</U> : <I>(if Locx=0 and Locy=0)</I></B><BR>
	 * <BR>
	 * <li>Get L2NpcInstance Init parameters and its generate an Identifier</li> <li>Call the constructor of the L2NpcInstance</li> <li>Calculate the random position in the location area (if Locx=0 and Locy=0) or get its exact position from the L2Spawn</li> <li>Set the position of the L2NpcInstance</li>
	 * <li>Set the HP and MP of the L2NpcInstance to the max</li> <li>Set the heading of the L2NpcInstance (random heading if not defined : value=-1)</li> <li>Link the L2NpcInstance to this L2Spawn</li> <li>Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...)
	 * and add it in the world</li> <li>Lauch the action OnSpawn fo the L2NpcInstance</li><BR>
	 * <BR>
	 * <li>Increase the current number of L2NpcInstance managed by this L2Spawn</li><BR>
	 * <BR>
	 * @return
	 */
	public L2NpcInstance doSpawn()
	{
		L2NpcInstance mob = null;
		try
		{
			// Check if the L2Spawn is not a L2Pet or L2Minion spawn
			if (_template.type.equalsIgnoreCase("L2Pet") || _template.type.equalsIgnoreCase("L2Minion"))
			{
				_currentCount++;
				
				return mob;
			}
			
			// Get L2NpcInstance Init parameters and its generate an Identifier
			final Object[] parameters =
			{
				IdFactory.getInstance().getNextId(),
				_template
			};
			
			// Call the constructor of the L2NpcInstance
			// (can be a L2ArtefactInstance, L2FriendlyMobInstance, L2GuardInstance, L2MonsterInstance, L2SiegeGuardInstance, L2BoxInstance,
			// L2FeedableBeastInstance, L2TamedBeastInstance, L2FolkInstance)
			final Object tmp = _constructor.newInstance(parameters);
			
			// Must be done before object is spawned into visible world
			((L2Object) tmp).setInstanceId(_instanceId);
			
			// Check if the Instance is a L2NpcInstance
			if (!(tmp instanceof L2NpcInstance))
			{
				return mob;
			}
			
			mob = (L2NpcInstance) tmp;
			
			return intializeNpcInstance(mob);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("NPC " + _template.npcId + " class not found", e);
		}
		return mob;
	}
	
	public L2NpcInstance getBossInfo()
	{
		L2NpcInstance mob = null;
		try
		{
			final Object[] parameters =
			{
				IdFactory.getInstance().getNextId(),
				_template
			};
			
			final Object tmp = _constructor.newInstance(parameters);
			
			// Must be done before object is spawned into visible world
			((L2Object) tmp).setInstanceId(_instanceId);
			
			// Check if the Instance is a L2NpcInstance
			if (!(tmp instanceof L2NpcInstance))
				return mob;
			
			mob = (L2NpcInstance) tmp;
			
			return mob;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
		}
		return mob;
	}
	
	private L2NpcInstance intializeNpcInstance(final L2NpcInstance mob)
	{
		int newlocx = 0;
		int newlocy = 0;
		int newlocz = 0;
		
		// If Locx=0 and Locy=0, the L2NpcInstance must be spawned in an area defined by location
		if (getLocx() == 0 && getLocy() == 0)
		{
			if (getLocation() == 0)
			{
				return mob;
			}
			
			// Calculate the random position in the location area
			final Location location = TerritoryTable.getInstance().getRandomPoint(getLocation());
			
			// Set the calculated position of the L2NpcInstance
			if (location != null)
			{
				newlocx = location.getX();
				newlocy = location.getY();
				newlocz = location.getZ();
			}
		}
		else
		{
			// The L2NpcInstance is spawned at the exact position (Lox, Locy, Locz)
			newlocx = getLocx();
			newlocy = getLocy();
			newlocz = getLocz();
		}
		
		if (mob != null)
		{	
			mob.stopAllEffects();
			
			// Set the HP and MP of the L2NpcInstance to the max
			mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
			
			// Set the heading of the L2NpcInstance (random heading if not defined)
			if (getHeading() == -1)
			{
				mob.setHeading(Rnd.nextInt(61794));
			}
			else
			{
				mob.setHeading(getHeading());
			}
			
			// Reset decay info
			mob.setDecayed(false);
			
			// Link the L2NpcInstance to this L2Spawn
			mob.setSpawn(this);
			
			if (mob instanceof L2MonsterInstance && !mob.isRaid() && !mob.isRaidMinion())
			{
				int x1 = mob.getSpawn().getLocx();
				int y1 = mob.getSpawn().getLocy();
				final int range = Config.MAX_RESPAWN_RANGE;
				
				int deltaX = Rnd.nextInt(range * 2);
				int deltaY = Rnd.get(deltaX, range * 2);
				deltaY = (int) Math.sqrt((deltaY * deltaY) - (deltaX * deltaX));
				x1 = (deltaX + x1) - range;
				y1 = (deltaY + y1) - range;
				
				if (GeoData.getInstance().canMove(newlocx, newlocy, newlocz, x1, y1, newlocz, mob.getInstanceId()))
				{
					mob.spawnMe(x1, y1, newlocz);
				}
				else
				{
					mob.spawnMe(newlocx, newlocy, newlocz);
				}
			}
			else
			{
				mob.spawnMe(newlocx, newlocy, newlocz);
			}
			
			//Sub chests correction
			if (mob.getNpcId() == 31027 || mob.getNpcId() == 31028 || mob.getNpcId() == 31029 || mob.getNpcId() == 31030)
			{
				mob.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_MAGIC_CIRCLE);
			}
			
			L2Spawn.notifyNpcSpawned(mob);
			
			_lastSpawn = mob;
			
			if (Config.DEBUG)
			{
				LOG.debug("spawned Mob ID: " + _template.npcId + " ,at: " + mob.getX() + " x, " + mob.getY() + " y, " + mob.getZ() + " z");
			}
			
			for (final Quest quest : mob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SPAWN))
			{
				quest.notifySpawn(mob);
			}
			
			// Increase the current number of L2NpcInstance managed by this L2Spawn
			_currentCount++;
		}
		return mob;
	}
	
	public static void addSpawnListener(final SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.add(listener);
		}
	}
	
	public static void removeSpawnListener(final SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.remove(listener);
		}
	}
	
	public static void notifyNpcSpawned(final L2NpcInstance npc)
	{
		synchronized (_spawnListeners)
		{
			for (final SpawnListener listener : _spawnListeners)
			{
				listener.npcSpawned(npc);
			}
		}
	}
	
	/**
	 * @param i delay in seconds
	 */
	public void setRespawnDelay(int i)
	{
		if (i < 0)
		{
			LOG.warn("respawn delay is negative for spawnId:" + _id);
		}
		
		if (i < 10)
		{
			i = 10;
		}
		
		_respawnDelay = i * 1000;
	}
	
	public L2NpcInstance getLastSpawn()
	{
		return _lastSpawn;
	}
	
	/**
	 * @param oldNpc
	 */
	public void respawnNpc(final L2NpcInstance oldNpc)
	{
		if (_doRespawn)
		{
			oldNpc.refreshID();
			intializeNpcInstance(oldNpc);
		}
	}
	
	public L2NpcTemplate getTemplate()
	{
		return _template;
	}
	
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	public void setInstanceId(final int instanceId)
	{
		_instanceId = instanceId;
	}
	
}

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
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.TerritoryTable;
import l2jorion.game.geo.GeoData;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2FeedableBeastInstance;
import l2jorion.game.model.actor.instance.L2GourdInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

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
	
	private boolean _isNoRandom = false;
	
	private boolean _customSpawn;
	private boolean _customBossInstance = false;
	
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
	
	public L2Spawn(final L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		_template = mobTemplate;
		
		if (_template == null)
		{
			return;
		}
		
		// The Name of the L2NpcInstance type managed by this L2Spawn
		String implementationName = _template.type; // implementing class name
		
		if (mobTemplate.npcId == 30995)
		{
			implementationName = "L2RaceManager";
		}
		
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
	}
	
	public L2Spawn(int npcId) throws SecurityException, ClassNotFoundException, NoSuchMethodException, ClassCastException
	{
		this(NpcTable.getInstance().getTemplate(npcId));
	}
	
	public int getAmount()
	{
		return _maximumCount;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLocation()
	{
		return _location;
	}
	
	public int getLocx()
	{
		return _locX;
	}
	
	public int getLocy()
	{
		return _locY;
	}
	
	public int getLocz()
	{
		return _locZ;
	}
	
	public int getNpcid()
	{
		if (_template == null)
		{
			return -1;
		}
		
		return _template.npcId;
	}
	
	public int getHeading()
	{
		return _heading;
	}
	
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}
	
	public int getRespawnMinDelay()
	{
		return _respawnMinDelay;
	}
	
	public int getRespawnMaxDelay()
	{
		return _respawnMaxDelay;
	}
	
	public void setAmount(final int amount)
	{
		_maximumCount = amount;
	}
	
	public void setId(final int id)
	{
		_id = id;
	}
	
	public void setLocation(final int location)
	{
		_location = location;
	}
	
	public void setRespawnMinDelay(final int date)
	{
		_respawnMinDelay = date;
	}
	
	public void setRespawnMaxDelay(final int date)
	{
		_respawnMaxDelay = date;
	}
	
	public void setLocx(final int locx)
	{
		_locX = locx;
	}
	
	public void setLocy(final int locy)
	{
		_locY = locy;
	}
	
	public void setLocz(final int locz)
	{
		_locZ = locz;
	}
	
	public void setHeading(final int heading)
	{
		_heading = heading;
	}
	
	public void setCustom(final boolean custom)
	{
		_customSpawn = custom;
	}
	
	public boolean isCustom()
	{
		return _customSpawn;
	}
	
	public void setNoRandomLoc(boolean noRandom)
	{
		_isNoRandom = noRandom;
	}
	
	public boolean isNoRandomLoc()
	{
		return _isNoRandom;
	}
	
	public boolean is_customBossInstance()
	{
		return _customBossInstance;
	}
	
	public void set_customBossInstance(final boolean customBossInstance)
	{
		_customBossInstance = customBossInstance;
	}
	
	public void decreaseCount(L2NpcInstance oldNpc)
	{
		_currentCount--;
		
		if (_doRespawn && _scheduledCount + _currentCount < _maximumCount)
		{
			_scheduledCount++;
			
			ThreadPoolManager.getInstance().scheduleGeneral(new SpawnTask(oldNpc), _respawnDelay);
		}
	}
	
	public int init()
	{
		while (_currentCount < _maximumCount)
		{
			doSpawn();
		}
		_doRespawn = true;
		
		return _currentCount;
	}
	
	public L2NpcInstance spawnOne()
	{
		return doSpawn();
	}
	
	public void stopRespawn()
	{
		_doRespawn = false;
	}
	
	public void startRespawn()
	{
		_doRespawn = true;
	}
	
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
			{
				e.printStackTrace();
			}
			
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
			{
				return mob;
			}
			
			mob = (L2NpcInstance) tmp;
			
			return mob;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		return mob;
	}
	
	private L2NpcInstance intializeNpcInstance(final L2NpcInstance mob)
	{
		int newlocx = 0;
		int newlocy = 0;
		int newlocz = 0;
		
		if (getLocx() == 0 && getLocy() == 0)
		{
			if (getLocation() == 0)
			{
				return mob;
			}
			
			// Calculate the random position in the location area
			final Location location = TerritoryTable.getInstance().getRandomPoint(getLocation());
			
			if (location != null)
			{
				newlocx = location.getX();
				newlocy = location.getY();
				newlocz = location.getZ();
			}
		}
		else
		{
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
			
			for (final Quest quest : mob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SPAWN))
			{
				quest.notifySpawn(mob);
			}
			
			if (Config.L2JMOD_CHAMPION_ENABLE)
			{
				if (mob instanceof L2Attackable && Config.L2JMOD_CHAMPION_FREQUENCY > 0 && mob.getLevel() >= Config.L2JMOD_CHAMP_MIN_LVL && mob.getLevel() <= Config.L2JMOD_CHAMP_MAX_LVL)
				{
					switch (mob.getNpcId())
					{
						case 29004:
						case 29005:
						{
							((L2Attackable) mob).setIsRaidMinion(true);
							break;
						}
					}
				}
			}
			
			if (!isNoRandomLoc() && mob instanceof L2MonsterInstance && !(mob instanceof L2FeedableBeastInstance) && !(mob instanceof L2GourdInstance) && !mob.isRaid() && !mob.isRaidMinion() && !mob.isBossInstance())
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
			
			switch (mob.getNpcId())
			{
				case 31027:
				case 31028:
				case 31029:
				case 31030:
					mob.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_MAGIC_CIRCLE);
					break;
			}
			
			L2Spawn.notifyNpcSpawned(mob);
			
			_lastSpawn = mob;
			
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

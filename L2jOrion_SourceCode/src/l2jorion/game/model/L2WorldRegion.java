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
package l2jorion.game.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.type.L2PeaceZone;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class L2WorldRegion
{
	private static Logger LOG = LoggerFactory.getLogger(L2WorldRegion.class);
	
	private final Map<Integer, L2PlayableInstance> _allPlayable = new ConcurrentHashMap<>();
	private final Map<Integer, L2Object> _visibleObjects = new ConcurrentHashMap<>();
	private FastList<L2WorldRegion> _surroundingRegions = new FastList<>();
	
	private final int _tileX, _tileY;
	private Boolean _active = false;
	private ScheduledFuture<?> _neighborsTask = null;
	private final List<L2ZoneType> _zones = new CopyOnWriteArrayList<>();
	
	public L2WorldRegion(int pTileX, int pTileY)
	{
		_tileX = pTileX;
		_tileY = pTileY;
		
		_active = Config.GRIDS_ALWAYS_ON;
	}
	
	public List<L2ZoneType> getZones()
	{
		return _zones;
	}
	
	public void addZone(L2ZoneType zone)
	{
		_zones.add(zone);
	}
	
	public void removeZone(L2ZoneType zone)
	{
		_zones.remove(zone);
	}
	
	public void revalidateZones(L2Character character)
	{
		if (character.isTeleporting())
		{
			return;
		}
		
		for (L2ZoneType z : getZones())
		{
			if (z != null)
			{
				z.revalidateInZone(character);
			}
		}
	}
	
	public void removeFromZones(L2Character character)
	{
		for (L2ZoneType z : getZones())
		{
			if (z != null)
			{
				z.removeCharacter(character);
			}
		}
	}
	
	public void onDeath(L2Character character)
	{
		for (L2ZoneType z : getZones())
		{
			if (z != null)
			{
				z.onDieInside(character);
			}
		}
	}
	
	public void onRevive(L2Character character)
	{
		for (L2ZoneType z : getZones())
		{
			if (z != null)
			{
				z.onReviveInside(character);
			}
		}
	}
	
	public class NeighborsTask implements Runnable
	{
		private boolean _isActivating;
		
		public NeighborsTask(boolean isActivating)
		{
			_isActivating = isActivating;
		}
		
		@Override
		public void run()
		{
			if (_isActivating)
			{
				for (L2WorldRegion neighbor : getSurroundingRegions())
				{
					neighbor.setActive(true);
				}
			}
			else
			{
				if (areNeighborsEmpty())
				{
					setActive(false);
				}
				
				for (L2WorldRegion neighbor : getSurroundingRegions())
				{
					if (neighbor.areNeighborsEmpty())
					{
						neighbor.setActive(false);
					}
				}
			}
		}
	}
	
	private void switchAI(final Boolean isOn)
	{
		int count = 0;
		if (!isOn)
		{
			for (L2Object object : _visibleObjects.values())
			{
				if (object instanceof L2Attackable)
				{
					count++;
					
					final L2Attackable mob = (L2Attackable) object;
					mob.setTarget(null);
					mob.stopMove(null);
					mob.stopAllEffects();
					mob.clearAggroList();
					mob.getKnownList().removeAllKnownObjects();
					
					if (mob.hasAI())
					{
						mob.getAI().setIntention(l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE);
						mob.getAI().stopAITask();
					}
				}
				else if (object instanceof L2Vehicle)
				{
					((L2Vehicle) object).getKnownList().removeAllKnownObjects();
				}
			}
			
			if (Config.DEBUG)
			{
				LOG.info(count + " mobs were turned off");
			}
		}
		else
		{
			for (L2Object object : _visibleObjects.values())
			{
				if (object instanceof L2Attackable)
				{
					count++;
					
					((L2Attackable) object).getStatus().startHpMpRegeneration();
				}
				else if (object instanceof L2NpcInstance)
				{
					((L2NpcInstance) object).startRandomAnimationTimer();
				}
			}
			
			if (Config.DEBUG)
			{
				LOG.info(count + " mobs were turned on");
			}
		}
		
	}
	
	public Boolean isActive()
	{
		return _active;
	}
	
	// check if all 9 neighbors (including self) are inactive or active but with no players.
	// returns true if the above condition is met.
	public Boolean areNeighborsEmpty()
	{
		if (isActive() && !_allPlayable.isEmpty())
		{
			return false;
		}
		
		// if any one of the neighbors is occupied, return false
		for (L2WorldRegion neighbor : _surroundingRegions)
		{
			if (neighbor.isActive() && !neighbor._allPlayable.isEmpty())
			{
				return false;
			}
		}
		// in all other cases, return true.
		return true;
	}
	
	public void setActive(final boolean value)
	{
		if (_active == value)
		{
			return;
		}
		
		_active = value;
		
		// turn the AI on or off to match the region's activation.
		switchAI(value);
		
		if (Config.DEBUG)
		{
			if (value)
			{
				LOG.info("Starting Grid " + _tileX + "," + _tileY);
			}
			else
			{
				LOG.info("Stoping Grid " + _tileX + "," + _tileY);
			}
		}
	}
	
	/**
	 * Immediately sets self as active and starts a timer to set neighbors as active this timer is to avoid turning on neighbors in the case when a person just teleported into a region and then teleported out immediately...there is no reason to activate all the neighbors in that case.
	 */
	private void startActivation()
	{
		setActive(true);
		
		// if the timer to deactivate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// then, set a timer to activate the neighbors
			_neighborsTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(true), 1000 * Config.GRID_NEIGHBOR_TURNON_TIME);
		}
	}
	
	/**
	 * starts a timer to set neighbors (including self) as inactive this timer is to avoid turning off neighbors in the case when a person just moved out of a region that he may very soon return to. There is no reason to turn self & neighbors off in that case.
	 */
	private void startDeactivation()
	{
		// if the timer to activate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// start a timer to "suggest" a deactivate to self and neighbors.
			// suggest means: first check if a neighbor has L2PcInstances in it. If not, deactivate.
			_neighborsTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(false), 1000 * Config.GRID_NEIGHBOR_TURNOFF_TIME);
		}
	}
	
	/**
	 * Add the L2Object in the L2ObjectHashSet(L2Object) _visibleObjects containing L2Object visible in this L2WorldRegion <BR>
	 * If L2Object is a L2PcInstance, Add the L2PcInstance in the L2ObjectHashSet(L2PcInstance) _allPlayable containing L2PcInstance of all player in game in this L2WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this
	 * @param object
	 */
	public void addVisibleObject(final L2Object object)
	{
		if (Config.ASSERT)
		{
			assert object.getWorldRegion() == this;
		}
		
		if (object == null)
		{
			return;
		}
		
		_visibleObjects.put(object.getObjectId(), object);
		
		if (object instanceof L2PlayableInstance)
		{
			_allPlayable.put(object.getObjectId(), (L2PlayableInstance) object);
			
			// if this is the first player to enter the region, activate self & neighbor
			if (_allPlayable.size() == 1 && !Config.GRIDS_ALWAYS_ON)
			{
				startActivation();
			}
		}
	}
	
	/**
	 * Remove the L2Object from the L2ObjectHashSet(L2Object) _visibleObjects in this L2WorldRegion <BR>
	 * <BR>
	 * If L2Object is a L2PcInstance, remove it from the L2ObjectHashSet(L2PcInstance) _allPlayable of this L2WorldRegion <BR>
	 * Assert : object.getCurrentWorldRegion() == this || object.getCurrentWorldRegion() == null
	 * @param object
	 */
	public void removeVisibleObject(final L2Object object)
	{
		if (Config.ASSERT)
		{
			assert object.getWorldRegion() == this || object.getWorldRegion() == null;
		}
		
		if (object == null)
		{
			return;
		}
		
		_visibleObjects.remove(object.getObjectId());
		
		if (object instanceof L2PlayableInstance)
		{
			_allPlayable.remove(object.getObjectId());
			
			if (_allPlayable.size() == 0 && !Config.GRIDS_ALWAYS_ON)
			{
				startDeactivation();
			}
		}
	}
	
	public void addSurroundingRegion(final L2WorldRegion region)
	{
		_surroundingRegions.add(region);
	}
	
	public FastList<L2WorldRegion> getSurroundingRegions()
	{
		return _surroundingRegions;
	}
	
	public Map<Integer, L2PlayableInstance> getVisiblePlayable()
	{
		return _allPlayable;
	}
	
	public Map<Integer, L2Object> getVisibleObjects()
	{
		return _visibleObjects;
	}
	
	public String getName()
	{
		return "(" + _tileX + ", " + _tileY + ")";
	}
	
	public synchronized void deleteVisibleNpcSpawns()
	{
		LOG.debug("Deleting all visible NPC's in Region: " + getName());
		for (L2Object obj : _visibleObjects.values())
		{
			if (obj instanceof L2NpcInstance)
			{
				L2NpcInstance target = (L2NpcInstance) obj;
				target.deleteMe();
				L2Spawn spawn = target.getSpawn();
				
				if (spawn != null)
				{
					spawn.stopRespawn();
					SpawnTable.getInstance().deleteSpawn(spawn, false);
				}
				
				LOG.debug("Removed NPC " + target.getObjectId());
			}
		}
	}
	
	public boolean checkEffectRangeInsidePeaceZone(L2Skill skill, final int x, final int y, final int z)
	{
		final int range = skill.getEffectRange();
		final int up = y + range;
		final int down = y - range;
		final int left = x + range;
		final int right = x - range;
		
		for (L2ZoneType e : getZones())
		{
			if (e instanceof L2PeaceZone)
			{
				if (e.isInsideZone(x, up, z))
				{
					return false;
				}
				
				if (e.isInsideZone(x, down, z))
				{
					return false;
				}
				
				if (e.isInsideZone(left, y, z))
				{
					return false;
				}
				
				if (e.isInsideZone(right, y, z))
				{
					return false;
				}
				
				if (e.isInsideZone(x, y, z))
				{
					return false;
				}
			}
		}
		return true;
	}
}

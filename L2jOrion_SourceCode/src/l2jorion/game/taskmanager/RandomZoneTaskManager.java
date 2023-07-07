/*
 * $HeadURL: $
 * 
 * $Author: $ $Date: $ $Revision: $
 * 
 * 
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
package l2jorion.game.taskmanager;

import java.util.stream.Collectors;

import l2jorion.Config;
import l2jorion.game.datatables.xml.FenceData;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.actor.instance.L2FenceInstance;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.model.zone.type.L2RandomZone;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Broadcast;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class RandomZoneTaskManager
{
	public int _id;
	public int _wallId;
	public int _timer;
	
	protected static final Logger LOG = LoggerFactory.getLogger(RandomZoneTaskManager.class);
	
	protected RandomZoneTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ZoneScheduler(), 0, 1000);
	}
	
	private class ZoneScheduler implements Runnable
	{
		protected ZoneScheduler()
		{
		}
		
		@Override
		public void run()
		{
			try
			{
				if (_timer > 0)
				{
					_timer--;
				}
				
				switch (_timer)
				{
					case 7200:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + (_timer / 60) / 60 + " hours", true);
						break;
					case 3600:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + (_timer / 60) / 60 + " hour", true);
						break;
					case 1800:
					case 900:
					case 600:
					case 300:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + _timer / 60 + " minutes", true);
						break;
					case 60:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + _timer / 60 + " minute", true);
						break;
					case 30:
					case 15:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + _timer + " seconds", true);
						break;
					case 5:
					case 4:
					case 3:
					case 2:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + _timer + " seconds", true);
						L2World.getInstance().getPlayers().stream().filter(player -> player.isInsideZone(ZoneId.ZONE_RANDOM)).forEach(player -> player.sendPacket(new ExShowScreenMessage("PvP zone will change in: " + _timer + " seconds", 1000, 2, false)));
						break;
					case 1:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + _timer + " second", true);
						L2World.getInstance().getPlayers().stream().filter(player -> player.isInsideZone(ZoneId.ZONE_RANDOM)).forEach(player -> player.sendPacket(new ExShowScreenMessage("PvP zone will change in: " + _timer + " seconds", 1000, 2, false)));
						
						break;
					case 0:
						if (getCurrentZone() != null)
						{
							if (getCurrentZone().useFenceWall())
							{
								L2Object fence = L2World.getInstance().findObject(getWallId());
								L2WorldRegion region = fence.getWorldRegion();
								fence.decayMe();
								if (region != null)
								{
									region.removeVisibleObject(fence);
								}
								fence.getKnownList().removeAllKnownObjects();
								L2World.getInstance().removeObject(fence);
								FenceData.removeFence((L2FenceInstance) fence);
							}
							getCurrentZone().setActiveZone(false);
						}
						
						selectNextZone();
						
						Broadcast.toAllOnlinePlayers("PvP zone has been changed to: " + getCurrentZone().getName(), true);
						// Broadcast.toAllOnlinePlayers("Next zone: " + getCurrentZone().getName(), true);
						
						getCurrentZone().setActiveZone(true);
						
						if (getCurrentZone().useFenceWall())
						{
							setWallId();
							L2FenceInstance fence = new L2FenceInstance(getWallId(), 2, getCurrentZone().getFenceWallWidth(), getCurrentZone().getFenceWallLength(), getCurrentZone().getCenterLoc().getX(), getCurrentZone().getCenterLoc().getY());
							fence.setInstanceId(Config.PVP_ZONE_INSTANCE_ID);
							fence.spawnMe(getCurrentZone().getCenterLoc().getX(), getCurrentZone().getCenterLoc().getY(), getCurrentZone().getCenterLoc().getZ());
							FenceData.addFence(fence);
						}
						
						L2World.getInstance().getPlayers().stream().filter(player -> player.isInsideZone(ZoneId.ZONE_RANDOM)).forEach(player -> player.teleToLocation(getCurrentZone().getLoc(), 50, true));
						break;
				}
			}
			catch (Exception e)
			{
				LOG.warn("RandomZoneTaskManager: Error in ZoneScheduler: " + e.getMessage(), e);
			}
		}
	}
	
	public int getZoneId()
	{
		return _id;
	}
	
	public void selectNextZone()
	{
		int nextZoneId = Rnd.get(1, getTotalZones());
		_id = nextZoneId;
		_timer = getCurrentZone().getTime();
	}
	
	public int getWallId()
	{
		return _wallId;
	}
	
	public void setWallId()
	{
		_wallId = IdFactory.getInstance().getNextId();
	}
	
	// Get current random zone
	public final L2RandomZone getCurrentZone()
	{
		return ZoneManager.getInstance().getAllZones(L2RandomZone.class).stream().filter(t -> t.getId() == getZoneId()).findFirst().orElse(null);
	}
	
	public static final int getTotalZones()
	{
		return ZoneManager.getInstance().getAllZones(L2RandomZone.class).stream().filter(zone -> zone != null).collect(Collectors.toList()).size();
	}
	
	public static RandomZoneTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RandomZoneTaskManager _instance = new RandomZoneTaskManager();
	}
}

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

import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.L2World;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.model.zone.type.L2RandomZone;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Broadcast;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class RandomZoneTaskManager
{
	public int _id;
	public int _timer;
	
	protected static final Logger LOG = LoggerFactory.getLogger(RandomZoneTaskManager.class);
	
	protected RandomZoneTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ZoneScheduler(), 1000, 1000);
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
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + (_timer / 60) / 60 + " hours.", true);
						break;
					case 3600:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + (_timer / 60) / 60 + " hour.", true);
						break;
					case 1800:
					case 900:
					case 600:
					case 300:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + _timer / 60 + " minutes.", true);
						break;
					case 60:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + _timer / 60 + " minute.", true);
						break;
					case 30:
					case 15:
					case 5:
					case 4:
					case 3:
					case 2:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + _timer + " seconds.", true);
						break;
					case 1:
						Broadcast.toAllOnlinePlayers("PvP zone will change in: " + _timer + " second.", true);
						break;
					case 0:
						selectNextZone();
						Broadcast.toAllOnlinePlayers("PvP zone has been changed.", true);
						Broadcast.toAllOnlinePlayers("Next zone: " + getCurrentZone().getName(), true);
						L2World.getInstance().getPlayers().stream().filter(x -> x.isInsideZone(ZoneId.ZONE_RANDOM)).forEach(x -> x.teleToLocation(getCurrentZone().getLoc(), 50));
						break;
				}
			}
			catch (final Exception e)
			{
				LOG.warn("Error in ZoneScheduler: " + e.getMessage(), e);
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

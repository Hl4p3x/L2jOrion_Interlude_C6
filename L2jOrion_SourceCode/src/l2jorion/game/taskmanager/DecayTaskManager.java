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
package l2jorion.game.taskmanager;

import java.util.Map;
import java.util.NoSuchElementException;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class DecayTaskManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(DecayTaskManager.class);
	
	protected Map<L2Character, Long> _decayTasks = new FastMap<L2Character, Long>().shared();
	
	private static DecayTaskManager _instance;
	
	public DecayTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new DecayScheduler(), 10000, 5000);
	}
	
	public static DecayTaskManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new DecayTaskManager();
		}
		
		return _instance;
	}
	
	public void addDecayTask(L2Character actor)
	{
		_decayTasks.put(actor, System.currentTimeMillis());
	}
	
	public void addDecayTask(L2Character actor, int interval)
	{
		_decayTasks.put(actor, System.currentTimeMillis() + interval);
	}
	
	public void cancelDecayTask(L2Character actor)
	{
		try
		{
			_decayTasks.remove(actor);
		}
		catch (NoSuchElementException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
	}
	
	private class DecayScheduler implements Runnable
	{
		protected DecayScheduler()
		{
			// Do nothing
		}
		
		@Override
		public void run()
		{
			Long current = System.currentTimeMillis();
			int delay;
			try
			{
				if (_decayTasks != null)
				{
					for (L2Character actor : _decayTasks.keySet())
					{
						if (actor instanceof L2RaidBossInstance)
						{
							delay = 30000;
						}
						else
						{
							delay = 7000;
						}
						
						if ((actor instanceof L2Attackable) && (((L2Attackable) actor).getSpoilerId() != 0))
						{
							delay += 10000;
						}
						
						if (current - _decayTasks.get(actor) > delay)
						{
							actor.onDecay();
							_decayTasks.remove(actor);
						}
					}
				}
			}
			catch (Throwable e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				// TODO: Find out the reason for exception. Unless caught here, mob decay would stop.
				LOG.warn(e.toString());
			}
		}
	}
	
	@Override
	public String toString()
	{
		String ret = "============= DecayTask Manager Report ============\r\n";
		ret += "Tasks count: " + _decayTasks.size() + "\r\n";
		ret += "Tasks dump:\r\n";
		
		Long current = System.currentTimeMillis();
		for (L2Character actor : _decayTasks.keySet())
		{
			ret += "Class/Name: " + actor.getClass().getSimpleName() + "/" + actor.getName() + " decay timer: " + (current - _decayTasks.get(actor)) + "\r\n";
		}
		
		return ret;
	}
}

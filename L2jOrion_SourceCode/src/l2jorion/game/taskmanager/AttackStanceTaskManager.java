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

import java.util.Map;

import javolution.util.FastMap;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2CubicInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.AutoAttackStop;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class AttackStanceTaskManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(AttackStanceTaskManager.class);
	
	protected Map<L2Character, Long> _attackStanceTasks = new FastMap<L2Character, Long>().shared();
	
	protected AttackStanceTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FightModeScheduler(), 0, 1000);
	}
	
	public static AttackStanceTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public void addAttackStanceTask(L2Character actor)
	{
		if (actor instanceof L2Summon)
		{
			final L2Summon summon = (L2Summon) actor;
			actor = summon.getOwner();
		}
		
		if (actor instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) actor;
			for (final L2CubicInstance cubic : player.getCubics().values())
			{
				if (cubic.getId() != L2CubicInstance.LIFE_CUBIC)
				{
					cubic.doAction();
				}
			}
		}
		
		_attackStanceTasks.put(actor, System.currentTimeMillis());
	}
	
	public void removeAttackStanceTask(L2Character actor)
	{
		if (actor instanceof L2Summon)
		{
			final L2Summon summon = (L2Summon) actor;
			actor = summon.getOwner();
		}
		
		_attackStanceTasks.remove(actor);
	}
	
	public boolean getAttackStanceTask(L2Character actor)
	{
		if (actor instanceof L2Summon)
		{
			final L2Summon summon = (L2Summon) actor;
			actor = summon.getOwner();
		}
		
		return _attackStanceTasks.containsKey(actor);
	}
	
	private class FightModeScheduler implements Runnable
	{
		protected FightModeScheduler()
		{
			// Do nothing
		}
		
		@Override
		public void run()
		{
			final Long current = System.currentTimeMillis();
			try
			{
				if (_attackStanceTasks != null)
				{
					synchronized (this)
					{
						for (final L2Character actor : _attackStanceTasks.keySet())
						{
							if ((current - _attackStanceTasks.get(actor)) > 15000)
							{
								actor.broadcastPacket(new AutoAttackStop(actor.getObjectId()));
								if (actor instanceof L2PcInstance && ((L2PcInstance) actor).getPet() != null)
								{
									((L2PcInstance) actor).getPet().broadcastPacket(new AutoAttackStop(((L2PcInstance) actor).getPet().getObjectId()));
								}
								
								actor.getAI().setAutoAttacking(false);
								_attackStanceTasks.remove(actor);
							}
						}
					}
				}
			}
			catch (final Exception e)
			{
				LOG.warn("Error in FightModeScheduler: " + e.getMessage(), e);
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final AttackStanceTaskManager _instance = new AttackStanceTaskManager();
	}
}

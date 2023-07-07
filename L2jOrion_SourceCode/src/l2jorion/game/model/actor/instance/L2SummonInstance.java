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
package l2jorion.game.model.actor.instance;

import java.util.concurrent.Future;

import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Summon;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SetSummonRemainTime;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class L2SummonInstance extends L2Summon
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2SummonInstance.class);
	
	private float _expPenalty = 0; // exp decrease multiplier (i.e. 0.3 (= 30%) for shadow)
	private int _itemConsumeId;
	private int _itemConsumeCount;
	private int _itemConsumeSteps;
	private final int _totalLifeTime;
	private final int _timeLostIdle;
	private final int _timeLostActive;
	private int _timeRemaining;
	private int _nextItemConsumeTime;
	public int lastShowntimeRemaining; // Following FbiAgent's example to avoid sending useless packets
	
	private Future<?> _summonLifeTask;
	
	public L2SummonInstance(final int objectId, final L2NpcTemplate template, final L2PcInstance owner, final L2Skill skill)
	{
		super(objectId, template, owner);
		setShowSummonAnimation(true);
		
		if (skill != null)
		{
			_itemConsumeId = skill.getItemConsumeIdOT();
			_itemConsumeCount = skill.getItemConsumeOT();
			_itemConsumeSteps = skill.getItemConsumeSteps();
			if (Config.CUSTOM_SUMMON_LIFE)
			{
				_totalLifeTime = Config.CUSTOM_SUMMON_LIFE_TIME;
			}
			else
			{
				_totalLifeTime = skill.getTotalLifeTime();
			}
			_timeLostIdle = skill.getTimeLostIdle();
			_timeLostActive = skill.getTimeLostActive();
		}
		else
		{
			// defaults
			_itemConsumeId = 0;
			_itemConsumeCount = 0;
			_itemConsumeSteps = 0;
			if (Config.CUSTOM_SUMMON_LIFE)
			{
				_totalLifeTime = Config.CUSTOM_SUMMON_LIFE_TIME;
			}
			else
			{
				_totalLifeTime = 1200000; // 20 minutes
			}
			_timeLostIdle = 1000;
			_timeLostActive = 1000;
		}
		_timeRemaining = _totalLifeTime;
		lastShowntimeRemaining = _totalLifeTime;
		
		if (_itemConsumeId == 0)
		{
			_nextItemConsumeTime = -1; // do not consume
		}
		else if (_itemConsumeSteps == 0)
		{
			_nextItemConsumeTime = -1; // do not consume
		}
		else
		{
			_nextItemConsumeTime = _totalLifeTime - _totalLifeTime / (_itemConsumeSteps + 1);
		}
		
		// When no item consume is defined task only need to check when summon life time has ended.
		// Otherwise have to destroy items from owner's inventory in order to let summon live.
		final int delay = 1000;
		
		if (Config.DEBUG && _itemConsumeCount != 0)
		{
			LOG.warn("L2SummonInstance: Item Consume ID: " + _itemConsumeId + ", Count: " + _itemConsumeCount + ", Rate: " + _itemConsumeSteps + " times.");
		}
		
		if (Config.DEBUG)
		{
			LOG.warn("L2SummonInstance: Task Delay " + delay / 1000 + " seconds.");
		}
		
		_summonLifeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new SummonLifetime(getOwner(), this), delay, delay);
	}
	
	@Override
	public final int getLevel()
	{
		return getTemplate() != null ? getTemplate().level : 0;
	}
	
	@Override
	public int getSummonType()
	{
		return 1;
	}
	
	public void setExpPenalty(final float expPenalty)
	{
		_expPenalty = expPenalty;
	}
	
	public float getExpPenalty()
	{
		return _expPenalty;
	}
	
	public int getItemConsumeCount()
	{
		return _itemConsumeCount;
	}
	
	public int getItemConsumeId()
	{
		return _itemConsumeId;
	}
	
	public int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}
	
	public int getNextItemConsumeTime()
	{
		return _nextItemConsumeTime;
	}
	
	public int getTotalLifeTime()
	{
		return _totalLifeTime;
	}
	
	public int getTimeLostIdle()
	{
		return _timeLostIdle;
	}
	
	public int getTimeLostActive()
	{
		return _timeLostActive;
	}
	
	public int getTimeRemaining()
	{
		return _timeRemaining;
	}
	
	public void setNextItemConsumeTime(final int value)
	{
		_nextItemConsumeTime = value;
	}
	
	public void decNextItemConsumeTime(final int value)
	{
		_nextItemConsumeTime -= value;
	}
	
	public void decTimeRemaining(final int value)
	{
		_timeRemaining -= value;
	}
	
	public void addExpAndSp(final int addToExp, final int addToSp)
	{
		getOwner().addExpAndSp(addToExp, addToSp);
	}
	
	public void reduceCurrentHp(final int damage, final L2Character attacker)
	{
		super.reduceCurrentHp(damage, attacker);
		SystemMessage sm = new SystemMessage(SystemMessageId.SUMMON_RECEIVED_DAMAGE_S2_BY_S1);
		
		if (attacker instanceof L2NpcInstance)
		{
			sm.addNpcName(((L2NpcInstance) attacker).getTemplate().npcId);
		}
		else
		{
			sm.addString(attacker.getName());
		}
		
		sm.addNumber(damage);
		getOwner().sendPacket(sm);
	}
	
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (Config.DEBUG)
		{
			LOG.warn("L2SummonInstance: " + getTemplate().name + " (" + getOwner().getName() + ") has been killed.");
		}
		
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(true);
			_summonLifeTask = null;
		}
		return true;
		
	}
	
	static class SummonLifetime implements Runnable
	{
		private final L2PcInstance _activeChar;
		private final L2SummonInstance _summon;
		
		SummonLifetime(final L2PcInstance activeChar, final L2SummonInstance newpet)
		{
			_activeChar = activeChar;
			_summon = newpet;
		}
		
		@Override
		public void run()
		{
			if (Config.DEBUG)
			{
				LOG.warn("L2SummonInstance: " + _summon.getTemplate().name + " (" + _activeChar.getName() + ") run task.");
			}
			
			try
			{
				final double oldTimeRemaining = _summon.getTimeRemaining();
				final int maxTime = _summon.getTotalLifeTime();
				double newTimeRemaining;
				
				// if pet is attacking
				if (_summon.isAttackingNow())
				{
					_summon.decTimeRemaining(_summon.getTimeLostActive());
				}
				else
				{
					_summon.decTimeRemaining(_summon.getTimeLostIdle());
				}
				newTimeRemaining = _summon.getTimeRemaining();
				// check if the summon's lifetime has ran out
				if (newTimeRemaining < 0)
				{
					_summon.unSummon(_activeChar);
				}
				// check if it is time to consume another item
				else if (newTimeRemaining <= _summon.getNextItemConsumeTime() && oldTimeRemaining > _summon.getNextItemConsumeTime())
				{
					_summon.decNextItemConsumeTime(maxTime / (_summon.getItemConsumeSteps() + 1));
					
					// check if owner has enought itemConsume, if requested
					if (_summon.getItemConsumeCount() > 0 && _summon.getItemConsumeId() != 0 && !_summon.isDead() && !_summon.destroyItemByItemId("Consume", _summon.getItemConsumeId(), _summon.getItemConsumeCount(), _activeChar, true))
					{
						_summon.unSummon(_activeChar);
					}
				}
				
				// prevent useless packet-sending when the difference isn't visible.
				if (_summon.lastShowntimeRemaining - newTimeRemaining > maxTime / 352)
				{
					_summon.getOwner().sendPacket(new SetSummonRemainTime(maxTime, (int) newTimeRemaining));
					_summon.lastShowntimeRemaining = (int) newTimeRemaining;
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				if (Config.DEBUG)
				{
					LOG.warn("Summon of player [#" + _activeChar.getName() + "] has encountered item consumption errors: " + e);
				}
			}
		}
	}
	
	@Override
	public synchronized void unSummon(final L2PcInstance owner)
	{
		if (Config.DEBUG)
		{
			LOG.warn("L2SummonInstance: " + getTemplate().name + " (" + owner.getName() + ") unsummoned.");
		}
		
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(true);
			_summonLifeTask = null;
		}
		
		super.unSummon(owner);
	}
	
	@Override
	public boolean destroyItem(final String process, final int objectId, final int count, final L2Object reference, final boolean sendMessage)
	{
		return getOwner().destroyItem(process, objectId, count, reference, sendMessage);
	}
	
	@Override
	public boolean destroyItem(final String process, final L2ItemInstance item, final int count, final L2Object reference, final boolean sendMessage)
	{
		return getOwner().destroyItem(process, item, count, reference, sendMessage);
	}
	
	@Override
	public boolean destroyItemByItemId(final String process, final int itemId, final int count, final L2Object reference, final boolean sendMessage)
	{
		return getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
	}
}

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
package l2jorion.game.model.quest;

import java.util.concurrent.ScheduledFuture;

import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.thread.ThreadPoolManager;

public class QuestTimer
{
	public class ScheduleTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!getIsActive())
			{
				return;
			}
			
			try
			{
				if (!getIsRepeating())
				{
					cancel();
				}
				getQuest().notifyEvent(getName(), getNpc(), getPlayer());
			}
			catch (final Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
		}
	}
	
	private boolean _isActive = true;
	private final String _name;
	private final Quest _quest;
	private final L2NpcInstance _npc;
	private final L2PcInstance _player;
	private final boolean _isRepeating;
	private ScheduledFuture<?> _schedular;
	
	public QuestTimer(final Quest quest, final String name, final long time, final L2NpcInstance npc, final L2PcInstance player, final boolean repeating)
	{
		_name = name;
		_quest = quest;
		_player = player;
		_npc = npc;
		_isRepeating = repeating;
		if (repeating)
		{
			_schedular = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ScheduleTimerTask(), time, time); // Prepare auto end task
		}
		else
		{
			_schedular = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), time); // Prepare auto end task
		}
	}
	
	public QuestTimer(final Quest quest, final String name, final long time, final L2NpcInstance npc, final L2PcInstance player)
	{
		this(quest, name, time, npc, player, false);
	}
	
	public QuestTimer(final QuestState qs, final String name, final long time)
	{
		this(qs.getQuest(), name, time, null, qs.getPlayer(), false);
	}
	
	/*
	 * public QuestTimer(Quest quest, String name, long time, L2NpcInstance npc, L2PcInstance player) { _name = name; _quest = quest; _player = player; _npc = npc; _schedular = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), time); // Prepare auto end task } public
	 * QuestTimer(QuestState qs, String name, long time) { _name = name; _quest = qs.getQuest(); _player = qs.getPlayer(); _npc = null; _schedular = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), time); // Prepare auto end task }
	 */
	// =========================================================
	// Method - Public
	public void cancel()
	{
		cancel(true);
	}
	
	public void cancel(final boolean removeTimer)
	{
		_isActive = false;
		
		if (_schedular != null)
		{
			_schedular.cancel(false);
		}
		
		if (removeTimer)
		{
			getQuest().removeQuestTimer(this);
		}
		
	}
	
	// public method to compare if this timer matches with the key attributes passed.
	// a quest and a name are required.
	// null npc or player act as wildcards for the match
	public boolean isMatch(final Quest quest, final String name, final L2NpcInstance npc, final L2PcInstance player)
	{
		/*
		 * if (quest instanceof Frintezza_l2j) { LOG.info("#### INPUT Parameters ####"); LOG.info("Quest Name: " + quest.getName()); LOG.info("Quest Timer Name: " + name); LOG.info("Quest NPC: " + npc); if (npc != null) { LOG.info(" NPC Name: " + npc.getName()); LOG.info(" NPC Id: " +
		 * npc.getNpcId()); LOG.info(" NPC Instance: " + npc.getInstanceId()); } LOG.info("Quest Player: " + player); if (player != null) { LOG.info(" Player Name: " + player.getName()); LOG.info(" Player Instance: " + player.getInstanceId()); } LOG.info("\n#### LOCAL Parameters ####");
		 * LOG.info("Quest Name: " + getQuest().getName()); LOG.info("Quest Timer Name: " + getName()); LOG.info("Quest NPC: " + getNpc()); if (getNpc() != null) { LOG.info(" NPC Name: " + getNpc().getName()); LOG.info(" NPC Id: " + getNpc().getNpcId()); LOG.info(" NPC Instance: " +
		 * getNpc().getInstanceId()); } LOG.info("Quest Player: " + getPlayer()); if (getPlayer() != null) { LOG.info(" Player Name: " + getPlayer().getName()); LOG.info(" Player Instance: " + getPlayer().getInstanceId()); } }
		 */
		
		if (quest == null || name == null)
		{
			return false;
		}
		
		if (quest != getQuest() || name.compareToIgnoreCase(getName()) != 0)
		{
			return false;
		}
		
		return npc == getNpc() && player == getPlayer();
	}
	
	// =========================================================
	// Property - Public
	public final boolean getIsActive()
	{
		return _isActive;
	}
	
	public final boolean getIsRepeating()
	{
		return _isRepeating;
	}
	
	public final Quest getQuest()
	{
		return _quest;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final L2NpcInstance getNpc()
	{
		return _npc;
	}
	
	public final L2PcInstance getPlayer()
	{
		return _player;
	}
	
	@Override
	public final String toString()
	{
		return _name;
	}
}

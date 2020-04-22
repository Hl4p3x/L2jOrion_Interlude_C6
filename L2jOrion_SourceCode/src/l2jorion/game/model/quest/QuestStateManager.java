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

import java.util.List;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.thread.ThreadPoolManager;

public class QuestStateManager
{
	// =========================================================
	// Schedule Task
	public class ScheduleTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				cleanUp();
				ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), 60000);
			}
			catch (final Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					t.printStackTrace();
			}
		}
	}
	
	// =========================================================
	// Data Field
	private static QuestStateManager _instance;
	private List<QuestState> _questStates = new FastList<>();
	
	// =========================================================
	// Constructor
	public QuestStateManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), 60000);
	}
	
	// =========================================================
	// Property - Public
	public static final QuestStateManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new QuestStateManager();
		}
		
		return _instance;
	}
	
	// =========================================================
	// Method - Public
	/**
	 * Add QuestState for the specified player instance
	 * @param quest
	 * @param player
	 * @param state
	 * @param completed
	 */
	public void addQuestState(final Quest quest, final L2PcInstance player, final State state, final boolean completed)
	{
		QuestState qs = getQuestState(player);
		if (qs == null)
		{
			qs = new QuestState(quest, player, state, completed);
		}
		
		// Save the state of the quest for the player in the player's list of quest onwed
		player.setQuestState(qs);
		
	}
	
	/**
	 * Remove all QuestState for all player instance that does not exist
	 */
	public void cleanUp()
	{
		for (int i = getQuestStates().size() - 1; i >= 0; i--)
		{
			if (getQuestStates().get(i).getPlayer() == null)
			{
				getQuestStates().remove(i);
			}
		}
	}
	
	/**
	 * Return QuestState for specified player instance
	 * @param player
	 * @return
	 */
	public QuestState getQuestState(final L2PcInstance player)
	{
		for (int i = 0; i < getQuestStates().size(); i++)
		{
			if (getQuestStates().get(i).getPlayer() != null && getQuestStates().get(i).getPlayer().getObjectId() == player.getObjectId())
				return getQuestStates().get(i);
			
		}
		
		return null;
	}
	
	/**
	 * Return all QuestState
	 * @return
	 */
	public List<QuestState> getQuestStates()
	{
		if (_questStates == null)
		{
			_questStates = new FastList<>();
		}
		
		return _questStates;
	}
}

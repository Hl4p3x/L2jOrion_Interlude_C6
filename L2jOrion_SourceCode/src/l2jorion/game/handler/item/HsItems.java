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
package l2jorion.game.handler.item;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.util.Util;

public class HsItems implements IItemHandler
{
	public static final int INTERACTION_DISTANCE = 100;
	
	private static final int[] ITEM_IDS =
	{
		8030,
		8031,
		8032,
		8033
	};
	
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance activeChar = (L2PcInstance) playable;
		L2NpcInstance target = (L2NpcInstance) activeChar.getTarget();
		
		if (target == null)
		{
			return;
		}
		
		if (!canUse(activeChar))
		{
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, activeChar.getTarget());
		}
		else
		{
			for (Quest quest : target.getTemplate().getEventQuests(Quest.QuestEventType.ON_ITEM_USE))
			{
				quest.notifyItemUse(target, activeChar, item);
			}
			
			activeChar.destroyItem("Consume", item.getObjectId(), 1, null, true);
		}
	}
	
	protected boolean canUse(L2PcInstance activeChar)
	{
		if (!Util.checkIfInRange(INTERACTION_DISTANCE, activeChar, activeChar.getTarget(), true))
		{
			return false;
		}
		
		return true;
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
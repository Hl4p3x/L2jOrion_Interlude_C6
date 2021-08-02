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
package l2jorion.game.handler.item;

import l2jorion.game.datatables.csv.DoorTable;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.network.serverpackets.SystemMessage;

public class MOSKey implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		8056
	};
	
	public static final int INTERACTION_DISTANCE = 150;
	public static long LAST_OPEN = 0;
	
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		int itemId = item.getItemId();
		
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance activeChar = (L2PcInstance) playable;
		L2Object target = activeChar.getTarget();
		
		if (!(target instanceof L2DoorInstance))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		L2DoorInstance door = (L2DoorInstance) target;
		
		if (!activeChar.isInsideRadius(door, INTERACTION_DISTANCE, false, false))
		{
			activeChar.sendMessage("Door is too far.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.getAbnormalEffect() > 0 || activeChar.isInCombat())
		{
			activeChar.sendMessage("You can't use the key right now.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (LAST_OPEN + 1800000 > System.currentTimeMillis()) // 30 * 60 * 1000 = 1800000
		{
			activeChar.sendMessage("You can't use the key right now.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
		{
			return;
		}
		
		if (itemId == 8056)
		{
			if (door.getDoorId() == 23150003 || door.getDoorId() == 23150004)
			{
				DoorTable.getInstance().getDoor(23150003).openMe();
				DoorTable.getInstance().getDoor(23150004).openMe();
				activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
				LAST_OPEN = System.currentTimeMillis();
			}
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
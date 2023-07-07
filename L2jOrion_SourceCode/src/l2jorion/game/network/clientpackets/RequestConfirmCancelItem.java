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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExConfirmCancelItem;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;

public final class RequestConfirmCancelItem extends PacketClient
{
	private int _itemId;
	
	@Override
	protected void readImpl()
	{
		_itemId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		final L2ItemInstance item = (L2ItemInstance) L2World.getInstance().findObject(_itemId);
		
		if (activeChar == null || item == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getUseAugItem().tryPerformAction("use cancel augitem"))
		{
			return;
		}
		
		if (!item.isAugmented())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM));
			return;
		}
		
		int price = 0;
		
		if (!Config.REMOVAL_AUGMENTATION_FREE)
		{
			switch (item.getItem().getItemGrade())
			{
				case L2Item.CRYSTAL_C:
					if (item.getCrystalCount() < 1720)
					{
						price = 95000;
					}
					else if (item.getCrystalCount() < 2452)
					{
						price = 150000;
					}
					else
					{
						price = 210000;
					}
					break;
				case L2Item.CRYSTAL_B:
					if (item.getCrystalCount() < 1746)
					{
						price = 240000;
					}
					else
					{
						price = 270000;
					}
					break;
				case L2Item.CRYSTAL_A:
					if (item.getCrystalCount() < 2160)
					{
						price = 330000;
					}
					else if (item.getCrystalCount() < 2824)
					{
						price = 390000;
					}
					else
					{
						price = 420000;
					}
					break;
				case L2Item.CRYSTAL_S:
					price = 480000;
					break;
				default:
					return;
			}
		}
		
		activeChar.sendPacket(new ExConfirmCancelItem(_itemId, price));
	}
	
	@Override
	public String getType()
	{
		return "[C] D0:2D RequestConfirmCancelItem";
	}
}

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

import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExConfirmVariationGemstone;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;

public final class RequestConfirmGemStone extends PacketClient
{
	private int _targetItemObjId;
	private int _refinerItemObjId;
	private int _gemstoneItemObjId;
	private int _gemstoneCount;
	
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
		_gemstoneItemObjId = readD();
		_gemstoneCount = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (!getClient().getFloodProtectors().getUseAugItem().tryPerformAction("use confirm aug gem"))
		{
			return;
		}
		
		final L2ItemInstance targetItem = (L2ItemInstance) L2World.getInstance().findObject(_targetItemObjId);
		final L2ItemInstance refinerItem = (L2ItemInstance) L2World.getInstance().findObject(_refinerItemObjId);
		final L2ItemInstance gemstoneItem = (L2ItemInstance) L2World.getInstance().findObject(_gemstoneItemObjId);
		
		if (targetItem == null || refinerItem == null || gemstoneItem == null)
		{
			return;
		}
		
		// Make sure the item is a gemstone
		final int gemstoneItemId = gemstoneItem.getItem().getItemId();
		
		if (gemstoneItemId != 2130 && gemstoneItemId != 2131)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}
		
		// Check if the gemstoneCount is sufficant
		final int itemGrade = targetItem.getItem().getItemGrade();
		
		switch (itemGrade)
		{
			case L2Item.CRYSTAL_C:
				if (_gemstoneCount != 20 || gemstoneItemId != 2130)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT));
					return;
				}
				break;
			case L2Item.CRYSTAL_B:
				if (_gemstoneCount != 30 || gemstoneItemId != 2130)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT));
					return;
				}
				break;
			case L2Item.CRYSTAL_A:
				if (_gemstoneCount != 20 || gemstoneItemId != 2131)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT));
					return;
				}
				break;
			case L2Item.CRYSTAL_S:
				if (_gemstoneCount != 25 || gemstoneItemId != 2131)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT));
					return;
				}
				break;
		}
		
		activeChar.sendPacket(new ExConfirmVariationGemstone(_gemstoneItemObjId, _gemstoneCount));
		activeChar.sendPacket(new SystemMessage(SystemMessageId.PRESS_THE_AUGMENT_BUTTON_TO_BEGIN));
	}
	
	@Override
	public String getType()
	{
		return "[C] D0:2B RequestConfirmGemStone";
	}
}

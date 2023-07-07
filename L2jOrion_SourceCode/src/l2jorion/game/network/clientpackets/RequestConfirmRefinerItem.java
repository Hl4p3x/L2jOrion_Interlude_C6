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
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ExConfirmVariationRefiner;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;

public class RequestConfirmRefinerItem extends PacketClient
{
	private static final int GEMSTONE_D = 2130;
	private static final int GEMSTONE_C = 2131;
	
	private int _targetItemObjId;
	private int _refinerItemObjId;
	
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getUseAugItem().tryPerformAction("use augitem"))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final L2ItemInstance targetItem = (L2ItemInstance) L2World.getInstance().findObject(_targetItemObjId);
		final L2ItemInstance refinerItem = (L2ItemInstance) L2World.getInstance().findObject(_refinerItemObjId);
		
		if (targetItem == null || refinerItem == null)
		{
			return;
		}
		
		final int itemGrade = targetItem.getItem().getItemGrade();
		final int refinerItemId = refinerItem.getItem().getItemId();
		
		// is the item a life stone?
		if (refinerItemId < 8723 || refinerItemId > 8762)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}
		
		int gemstoneCount = 0;
		int gemstoneItemId = 0;
		
		@SuppressWarnings("unused")
		final int lifeStoneLevel = getLifeStoneLevel(refinerItemId);
		final SystemMessage sm = new SystemMessage(SystemMessageId.REQUIRES_S1_S2);
		
		switch (itemGrade)
		{
			case L2Item.CRYSTAL_C:
				gemstoneCount = 20;
				gemstoneItemId = GEMSTONE_D;
				sm.addNumber(gemstoneCount);
				sm.addString("Gemstone D");
				break;
			case L2Item.CRYSTAL_B:
				gemstoneCount = 30;
				gemstoneItemId = GEMSTONE_D;
				sm.addNumber(gemstoneCount);
				sm.addString("Gemstone D");
				break;
			case L2Item.CRYSTAL_A:
				gemstoneCount = 20;
				gemstoneItemId = GEMSTONE_C;
				sm.addNumber(gemstoneCount);
				sm.addString("Gemstone C");
				break;
			case L2Item.CRYSTAL_S:
				gemstoneCount = 25;
				gemstoneItemId = GEMSTONE_C;
				sm.addNumber(gemstoneCount);
				sm.addString("Gemstone C");
				break;
		}
		
		activeChar.sendPacket(new ExConfirmVariationRefiner(_refinerItemObjId, refinerItemId, gemstoneItemId, gemstoneCount));
		activeChar.sendPacket(sm);
	}
	
	private int getLifeStoneGrade(int itemId)
	{
		itemId -= 8723;
		if (itemId < 10)
		{
			return 0; // normal grade
		}
		
		if (itemId < 20)
		{
			return 1; // mid grade
		}
		
		if (itemId < 30)
		{
			return 2; // high grade
		}
		
		return 3; // top grade
	}
	
	private int getLifeStoneLevel(int itemId)
	{
		itemId -= 10 * getLifeStoneGrade(itemId);
		itemId -= 8722;
		return itemId;
	}
	
	@Override
	public String getType()
	{
		return "[C] D0:2A RequestConfirmRefinerItem";
	}
}
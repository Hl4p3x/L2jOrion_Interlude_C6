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
package l2jorion.game.network.serverpackets;

import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class TradeStart extends PacketServer
{
	private static final String _S__2E_TRADESTART = "[S] 1E TradeStart";
	
	private final L2PcInstance _activeChar;
	private final L2ItemInstance[] _itemList;
	
	public TradeStart(final L2PcInstance player)
	{
		_activeChar = player;
		_itemList = _activeChar.getInventory().getAvailableItems(true);
	}
	
	@Override
	protected final void writeImpl()
	{// 0x2e TradeStart d h (h dddhh dhhh)
		if (_activeChar.getActiveTradeList() == null || _activeChar.getActiveTradeList().getPartner() == null)
		{
			return;
		}
		
		writeC(0x1E);
		writeD(_activeChar.getActiveTradeList().getPartner().getObjectId());
		// writeD((_activeChar != null || _activeChar.getTransactionRequester() != null)? _activeChar.getTransactionRequester().getObjectId() : 0);
		
		writeH(_itemList.length);
		for (final L2ItemInstance item : _itemList)// int i = 0; i < count; i++)
		{
			writeH(item.getItem().getType1()); // item type1
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(item.getCount());
			writeH(item.getItem().getType2()); // item type2
			writeH(0x00); // ?
			
			writeD(item.getItem().getBodyPart()); // rev 415 slot 0006-lr.ear 0008-neck 0030-lr.finger 0040-head 0080-?? 0100-l.hand 0200-gloves 0400-chest 0800-pants 1000-feet 2000-?? 4000-r.hand 8000-r.hand
			writeH(item.getEnchantLevel()); // enchant level
			writeH(0x00); // ?
			writeH(0x00);
		}
	}
	
	@Override
	public String getType()
	{
		return _S__2E_TRADESTART;
	}
}

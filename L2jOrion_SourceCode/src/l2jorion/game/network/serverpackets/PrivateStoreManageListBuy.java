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

import l2jorion.game.model.TradeList;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class PrivateStoreManageListBuy extends PacketServer
{
	private static final String _S__D0_PRIVATESELLLISTBUY = "[S] b7 PrivateSellListBuy";
	
	private final L2PcInstance _activeChar;
	private int _playerAdena;
	private final L2ItemInstance[] _itemList;
	private final TradeList.TradeItem[] _buyList;
	private int _sellItemId;
	
	public PrivateStoreManageListBuy(final L2PcInstance player)
	{
		_activeChar = player;
		_playerAdena = _activeChar.getAdena();
		player.getBuyList().setSellBuyItemId(57);
		_sellItemId = 57;
		_itemList = _activeChar.getInventory().getUniqueItems(false, true, true);
		_buyList = _activeChar.getBuyList().getItems();
	}
	
	public PrivateStoreManageListBuy(final L2PcInstance player, int sellItemId)
	{
		_activeChar = player;
		_playerAdena = player.getItemCount(sellItemId, -1);
		player.getBuyList().setSellBuyItemId(sellItemId);
		_sellItemId = sellItemId;
		_itemList = _activeChar.getInventory().getUniqueItems(false, true, true);
		_buyList = _activeChar.getBuyList().getItems();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xb7);
		// section 1
		writeD(_activeChar.getObjectId());
		writeD(_playerAdena);
		
		// section2
		writeD(_itemList.length); // inventory items for potential buy
		for (final L2ItemInstance item : _itemList)
		{
			writeD(item.getItemId());
			writeH(item.getEnchantLevel()); // show enchant lvl, but you can't buy enchanted weapons because of L2 Interlude Client bug
			writeD(item.getCount());
			writeD(item.getReferencePrice());
			writeH(0x00);
			// writeD(item.getItem().getBodyPart());
			writeD(_sellItemId);
			writeH(item.getItem().getType2());
		}
		
		// section 3
		writeD(_buyList.length); // count for all items already added for buy
		for (final TradeList.TradeItem item : _buyList)
		{
			writeD(item.getItem().getItemId());
			writeH(item.getEnchant());
			writeD(item.getCount());
			writeD(item.getItem().getReferencePrice());
			writeH(0x00);
			// writeD(item.getItem().getBodyPart());
			writeD(_sellItemId);
			writeH(item.getItem().getType2());
			writeD(item.getPrice());// your price
			writeD(item.getItem().getReferencePrice());// fixed store price
		}
	}
	
	@Override
	public String getType()
	{
		return _S__D0_PRIVATESELLLISTBUY;
	}
}
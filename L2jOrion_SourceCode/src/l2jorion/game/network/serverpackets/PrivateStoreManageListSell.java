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
import l2jorion.game.model.actor.instance.L2PcInstance;

public class PrivateStoreManageListSell extends L2GameServerPacket
{
	private static final String _S__B3_PRIVATESELLLISTSELL = "[S] 9a PrivateSellListSell";
	
	private final int _objId;
	private int _playerAdena;
	private final boolean _packageSale;
	private final TradeList.TradeItem[] _itemList;
	private final TradeList.TradeItem[] _sellList;
	
	public PrivateStoreManageListSell(L2PcInstance player)
	{
		_objId = player.getObjectId();
		_playerAdena = player.getAdena();
		player.getSellList().updateItems();
		_packageSale = player.getSellList().isPackaged();
		_itemList = player.getInventory().getAvailableItems(player.getSellList());
		_sellList = player.getSellList().getItems();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x9a);
		// section 1
		writeD(_objId);
		writeD(_packageSale ? 1 : 0); // Package sell
		writeD(_playerAdena);
		
		// section2
		writeD(_itemList.length); // for potential sells
		for (final TradeList.TradeItem item : _itemList)
		{
			writeD(item.getItem().getType2());
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getCount());
			writeH(0x00);
			writeH(item.getEnchant());// enchant lvl
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeD(item.getPrice()); // store price
		}
		// section 3
		writeD(_sellList.length); // count for any items already added for sell
		for (final TradeList.TradeItem item : _sellList)
		{
			writeD(item.getItem().getType2());
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getCount());
			writeH(0x00);
			writeH(item.getEnchant());// enchant lvl
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeD(item.getPrice());// your price
			writeD(item.getItem().getReferencePrice()); // store price
		}
	}
	
	@Override
	public String getType()
	{
		return _S__B3_PRIVATESELLLISTSELL;
	}
}
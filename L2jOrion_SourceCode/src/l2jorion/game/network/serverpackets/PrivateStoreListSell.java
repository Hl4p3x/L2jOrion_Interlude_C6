/*
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
import l2jorion.game.network.PacketServer;

public class PrivateStoreListSell extends PacketServer
{
	private static final String _S__B4_PRIVATESTORELISTSELL = "[S] 9b PrivateStoreListSell";
	
	private L2PcInstance _storePlayer;
	// private L2PcInstance _activeChar;
	private int _playerAdena;
	private boolean _packageSale;
	private TradeList.TradeItem[] _items;
	
	public PrivateStoreListSell(L2PcInstance player, L2PcInstance storePlayer)
	{
		// _activeChar = player;
		_storePlayer = storePlayer;
		
		_playerAdena = player.getAdena();
		if (_storePlayer.getSellList().isBuffer())
		{
			_playerAdena = player.getItemCount(_storePlayer.getSellList().getSellBuyItemId(), -1);
		}
		
		_items = _storePlayer.getSellList().getItems();
		_packageSale = _storePlayer.getSellList().isPackaged();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x9b);
		
		writeD(_storePlayer.getObjectId());
		
		// writeD(_packageSale ? 1 : 0);
		if (_storePlayer.getSellList().isBuffer())
		{
			writeD(_packageSale ? 2 : 3);
		}
		else
		{
			writeD(_packageSale ? 1 : 0);
		}
		
		writeD(_playerAdena);
		
		writeD(_items.length);
		for (TradeList.TradeItem item : _items)
		{
			if (_storePlayer.getSellList().isBuffer())
			{
				writeD(0);
				writeD(item.getObjectId());
				writeD(item.getItem().getItemId());
				writeD(1); // count
				writeH(0x00);
				writeH(item.getEnchant());
				writeH(0x00);
				writeD(_storePlayer.getSellList().getSellBuyItemId());
				writeD(item.getPrice()); // your price
				writeD(item.getItem().getReferencePrice()); // store price
			}
			else
			{
				writeD(item.getItem().getType2());
				writeD(item.getObjectId());
				writeD(item.getItem().getItemId());
				writeD(item.getCount());
				writeH(0x00);
				writeH(item.getEnchant());
				writeH(0x00);
				writeD(_storePlayer.getSellList().getSellBuyItemId());
				writeD(item.getPrice()); // your price
				writeD(item.getItem().getReferencePrice()); // store price
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__B4_PRIVATESTORELISTSELL;
	}
}
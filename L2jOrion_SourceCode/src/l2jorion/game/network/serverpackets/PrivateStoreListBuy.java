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

public class PrivateStoreListBuy extends PacketServer
{
	private static final String _S__D1_PRIVATESTORELISTBUY = "[S] b8 PrivateStoreListBuy";
	
	private L2PcInstance _storePlayer;
	private L2PcInstance _activeChar;
	private int _playerAdena;
	
	private TradeList.TradeItem[] _items;
	
	public PrivateStoreListBuy(L2PcInstance player, L2PcInstance storePlayer)
	{
		_storePlayer = storePlayer;
		_activeChar = player;
		
		_playerAdena = _activeChar.getAdena();
		if (_storePlayer.getSellList().isBuffer())
		{
			_playerAdena = player.getItemCount(_storePlayer.getBuyList().getSellBuyItemId(), -1);
		}
		
		_items = _storePlayer.getBuyList().getAvailableItems(_activeChar.getInventory());
		
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xb8);
		writeD(_storePlayer.getObjectId());
		writeD(_playerAdena);
		
		writeD(_items.length);
		
		for (TradeList.TradeItem item : _items)
		{
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeH(item.getEnchant());
			
			writeD(item.getCurCount());
			writeD(item.getItem().getReferencePrice());
			writeH(0);
			
			writeD(_storePlayer.getBuyList().getSellBuyItemId());
			// writeD(item.getItem().getBodyPart());
			
			writeH(item.getItem().getType2());
			writeD(item.getPrice());
			writeD(item.getCount());
		}
	}
	
	@Override
	public String getType()
	{
		return _S__D1_PRIVATESTORELISTBUY;
	}
}
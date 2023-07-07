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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.model.ItemRequest;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.TradeList;
import l2jorion.game.model.TradeList.TradeItem;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestPrivateStoreBuy extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(RequestPrivateStoreBuy.class);
	
	private int _storePlayerId;
	private int _count;
	private ItemRequest[] _items;
	
	@Override
	protected void readImpl()
	{
		_storePlayerId = readD();
		_count = readD();
		
		// count*12 is the size of a for iteration of each item
		if (_count < 0 || _count * 12 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET)
		{
			_count = 0;
		}
		
		_items = new ItemRequest[_count];
		
		for (int i = 0; i < _count; i++)
		{
			final int objectId = readD();
			long count = readD();
			if (count > Integer.MAX_VALUE)
			{
				count = Integer.MAX_VALUE;
			}
			final int price = readD();
			
			_items[i] = new ItemRequest(objectId, (int) count, price);
		}
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getTransaction().tryPerformAction("privatestorebuy"))
		{
			player.sendMessage("You buying items too fast.");
			return;
		}
		
		final L2Object object = L2World.getInstance().findObject(_storePlayerId);
		if (object == null || !(object instanceof L2PcInstance))
		{
			return;
		}
		
		final L2PcInstance storePlayer = (L2PcInstance) object;
		if (!(storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL))
		{
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final TradeList storeList = storePlayer.getSellList();
		
		if (storeList == null)
		{
			return;
		}
		
		// Check if player didn't choose any items
		if (_items == null || _items.length == 0)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		long priceTotal = 0;
		for (final ItemRequest ir : _items)
		{
			if (ir.getCount() > Integer.MAX_VALUE || ir.getCount() < 0)
			{
				final String msgErr = "[RequestPrivateStoreBuy] player " + getClient().getActiveChar().getName() + " tried an overflow exploit, ban this player!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
				return;
			}
			
			final TradeItem sellersItem = storeList.getItem(ir.getObjectId());
			
			if (sellersItem == null)
			{
				final String msgErr = "[RequestPrivateStoreBuy] player " + getClient().getActiveChar().getName() + " tried to buy an item not sold in a private store (buy), ban this player!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
				return;
			}
			
			if (ir.getPrice() != sellersItem.getPrice())
			{
				final String msgErr = "[RequestPrivateStoreBuy] player " + getClient().getActiveChar().getName() + " tried to change the seller's price in a private store (buy), ban this player!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
				return;
			}
			
			final L2ItemInstance iEnchant = storePlayer.getInventory().getItemByObjectId(ir.getObjectId());
			int enchant = 0;
			if (iEnchant == null)
			{
				enchant = 0;
			}
			else
			{
				enchant = iEnchant.getEnchantLevel();
			}
			ir.setEnchant(enchant);
			
			if (storeList.isBuffer())
			{
				ir.setId(sellersItem.getId());
				ir.setEnchant(sellersItem.getEnchant());
			}
			
			priceTotal += ir.getPrice() * ir.getCount();
		}
		
		if (priceTotal < 0 || priceTotal > Integer.MAX_VALUE)
		{
			final String msgErr = "[RequestPrivateStoreBuy] player " + getClient().getActiveChar().getName() + " tried an overflow exploit, ban this player!";
			Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
			return;
		}
		
		if (player.getAdena() < priceTotal)
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
		{
			if (storeList.getItemCount() > _count)
			{
				final String msgErr = "[RequestPrivateStoreBuy] player " + getClient().getActiveChar().getName() + " tried to buy less items then sold by package-sell, ban this player for bot-usage!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
				return;
			}
		}
		
		if (storeList.isBuffer())
		{
			storeList.GetBuffs(player, _items, (int) priceTotal);
			return;
		}
		
		if (!storeList.PrivateStoreBuy(player, _items, (int) priceTotal))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			Util.handleIllegalPlayerAction(storePlayer, "PrivateStore buy has failed due to invalid list or request. Player: " + player.getName(), Config.DEFAULT_PUNISH);
			LOG.warn("PrivateStore buy has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
			return;
		}
		
		if (storeList.getItemCount() == 0)
		{
			storePlayer.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			storePlayer.broadcastUserInfo();
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 79 RequestPrivateStoreBuy";
	}
}
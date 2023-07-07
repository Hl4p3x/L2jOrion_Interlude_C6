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

import l2jorion.game.model.L2ManufactureList;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.RecipeShopManageList;

public final class RequestRecipeShopManageList extends PacketClient
{
	@Override
	protected void readImpl()
	{
		// trigger
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (player.isAlikeDead())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getPrivateStoreType() != 0)
		{
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			player.broadcastUserInfo();
			if (player.isSitting())
			{
				player.standUp();
			}
		}
		
		if (player.getCreateList() == null)
		{
			player.setCreateList(new L2ManufactureList());
		}
		
		player.sendPacket(new RecipeShopManageList(player, true));
		
		/*
		 * int privatetype=player.getPrivateStoreType(); if (privatetype == 0) { if (player.getWaitType() !=1) { player.setWaitType(1); player.sendPacket(new ChangeWaitType (player,1)); player.broadcastPacket(new ChangeWaitType (player,1)); } if (player.getTradeList() == null) {
		 * player.setTradeList(new L2TradeList(0)); } if (player.getSellList() == null) { player.setSellList(new ArrayList()); } player.getTradeList().updateSellList(player,player.getSellList()); player.setPrivateStoreType(2); player.sendPacket(new PrivateSellListSell(client.getActiveChar()));
		 * player.sendPacket(new UserInfo(player)); player.broadcastPacket(new UserInfo(player)); } if (privatetype == 1) { player.setPrivateStoreType(2); player.sendPacket(new PrivateSellListSell(client.getActiveChar())); player.sendPacket(new ChangeWaitType (player,1)); player.broadcastPacket(new
		 * ChangeWaitType (player,1)); }
		 */
		
	}
	
	@Override
	public String getType()
	{
		return "[C] b0 RequestRecipeShopManageList";
	}
}

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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.model.BlockList;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.SendTradeRequest;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.util.Util;

public final class TradeRequest extends PacketClient
{
	// private static Logger LOG = LoggerFactory.getLogger(TradeRequest.class.getName());
	
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disable for your Access Level");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final L2PcInstance target = L2World.getInstance().getPlayer(_objectId);
		if ((target == null) || !player.getKnownList().knowsObject(target) || target.getObjectId() == player.getObjectId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (BlockList.isBlocked(target, player))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
			sm.addString(target.getName());
			player.sendPacket(sm);
			return;
		}
		
		if (target.isInOlympiadMode() || player.isInOlympiadMode())
		{
			player.sendMessage("You or your target can't request trade in Olympiad mode");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isStunned())
		{
			player.sendMessage("You can't Request a Trade when target Stunned");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isConfused())
		{
			player.sendMessage("You can't Request a Trade when target Confused");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isCastingNow() || target.isCastingPotionNow())
		{
			player.sendMessage("You can't Request a Trade when target Casting Now");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isInDuel())
		{
			player.sendMessage("You can't Request a Trade when target in Duel");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isImobilised())
		{
			player.sendMessage("You can't Request a Trade when target is Imobilised");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isInFunEvent())
		{
			player.sendMessage("You can't Request a Trade when target in Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.getActiveEnchantItem() != null)
		{
			player.sendMessage("You can't Request a Trade when target Enchanting");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isParalyzed())
		{
			player.sendMessage("You can't Request a Trade when target is Paralyzed");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.inObserverMode())
		{
			player.sendMessage("You can't Request a Trade when target in Observation Mode");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isAttackingNow())
		{
			player.sendMessage("You can't Request a Trade when target Attacking Now");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.getTradeRefusal())
		{
			player.sendMessage("Target is in trade refusal mode");
			return;
		}
		
		if (player.isStunned())
		{
			player.sendMessage("You can't Request a Trade when you Stunned");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isConfused())
		{
			player.sendMessage("You can't Request a Trade when you Confused");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isCastingNow() || player.isCastingPotionNow())
		{
			player.sendMessage("You can't Request a Trade when you Casting");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInDuel())
		{
			player.sendMessage("You can't Request a Trade when you in Duel");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isImobilised())
		{
			player.sendMessage("You can't Request a Trade when you are Imobilised");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInFunEvent())
		{
			player.sendMessage("You can't Request a Trade when you are in Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getActiveEnchantItem() != null)
		{
			player.sendMessage("You can't Request a Trade when you Enchanting");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isParalyzed())
		{
			player.sendMessage("You can't Request a Trade when you are Paralyzed");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.inObserverMode())
		{
			player.sendMessage("You can't Request a Trade when you in Observation Mode");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getDistanceSq(target) > 22500) // 150
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TRADE && (player.getKarma() > 0 || target.getKarma() > 0))
		{
			player.sendMessage("Chaotic players can't use Trade.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getPrivateStoreType() != 0 || target.getPrivateStoreType() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!Config.ALLOW_LOW_LEVEL_TRADE)
		{
			if (player.getLevel() < 76 && target.getLevel() >= 76 || target.getLevel() < 76 || player.getLevel() >= 76)
			{
				player.sendMessage("You cannot trade a lower level character.");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		if (player.isProcessingTransaction())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_TRADING));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isProcessingRequest() || target.isProcessingTransaction())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
			sm.addString(target.getName());
			player.sendPacket(sm);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (Util.calculateDistance(player, target, true) > 150)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.TARGET_TOO_FAR);
			player.sendPacket(sm);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.onTransactionRequest(target);
		target.sendPacket(new SendTradeRequest(player.getObjectId()));
		SystemMessage sm = new SystemMessage(SystemMessageId.REQUEST_S1_FOR_TRADE);
		sm.addString(target.getName());
		player.sendPacket(sm);
	}
	
	@Override
	public String getType()
	{
		return "[C] 15 TradeRequest";
	}
}
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

package l2jorion.game.handler.user;

import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.handler.IUserCommandHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.TradeList;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.sevensigns.SevenSignsFestival;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.taskmanager.AttackStanceTaskManager;

public class OfflineShop implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		114
	};
	
	@SuppressWarnings("null")
	@Override
	public synchronized boolean useUserCommand(final int id, final L2PcInstance player)
	{
		if (player == null)
		{
			return false;
		}
		
		// Message like L2OFF
		if ((!player.isInStoreMode() && (!player.isInCraftMode())) || !player.isSitting())
		{
			player.sendMessage("You are not running a private store or private work shop.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (player.isInFunEvent() && !player.isGM())
		{
			player.sendMessage("You cannot logout while in registered in an Event.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		final TradeList storeListBuy = player.getBuyList();
		if (storeListBuy == null && storeListBuy.getItemCount() == 0)
		{
			player.sendMessage("Your buy list is empty.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		final TradeList storeListSell = player.getSellList();
		if (storeListSell == null && storeListSell.getItemCount() == 0)
		{
			player.sendMessage("Your sell list is empty.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		player.getInventory().updateDatabase();
		
		if (AttackStanceTaskManager.getInstance().getAttackStanceTask(player) && !(player.isGM() && Config.GM_RESTART_FIGHTING))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANT_LOGOUT_WHILE_FIGHTING));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Dont allow leaving if player is in combat
		if (player.isInCombat() && !player.isGM())
		{
			player.sendMessage("You cannot logout while in combat mode.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (player.isTeleporting() && !player.isGM())
		{
			player.sendMessage("You cannot logout while is teleporting.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (player.atEvent)
		{
			player.sendPacket(SystemMessage.sendString("A superior power doesn't allow you to leave the event."));
			return false;
		}
		
		if (player.isInOlympiadMode() /* || Olympiad.getInstance().isRegistered(player) */)
		{
			player.sendMessage("You can't logout in Olympiad mode.");
			return false;
		}
		
		if (player.isFestivalParticipant())
		{
			if (SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				player.sendMessage("You cannot Logout while you are a participant in a Festival.");
				return false;
			}
			
			final L2Party playerParty = player.getParty();
			if (playerParty != null)
			{
				player.getParty().broadcastToPartyMembers(SystemMessage.sendString(player.getName() + " has been removed from the upcoming Festival."));
			}
		}
		
		if (player.isFlying())
		{
			player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
		}
		
		if ((player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE) || (player.isInCraftMode() && Config.OFFLINE_CRAFT_ENABLE))
		{
			// Sleep effect, not official feature but however L2OFF features (like offline trade)
			if (Config.OFFLINE_SLEEP_EFFECT)
			{
				player.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_SLEEP);
			}
			
			player.sendMessage("Your private store has succesfully been flagged as an offline shop and will remain active for ever.");
			
			player.logout();
			
			return true;
		}
		return false;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}
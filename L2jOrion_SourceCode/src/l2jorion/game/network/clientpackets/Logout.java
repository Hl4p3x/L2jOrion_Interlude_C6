/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.sevensigns.SevenSignsFestival;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.taskmanager.AttackStanceTaskManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class Logout extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(Logout.class);
	
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (player.isInFunEvent() && !player.isGM())
		{
			player.sendMessage("You cannot Logout while in registered in an Event.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInArenaEvent() || player.isArenaProtection())
		{
			player.sendMessage("You cannot logout while in Tournament Event!");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.getInventory().updateDatabase();
		
		if (AttackStanceTaskManager.getInstance().getAttackStanceTask(player) && !(player.isGM() && Config.GM_RESTART_FIGHTING))
		{
			if (Config.DEBUG)
			{
				LOG.warn("DEBUG " + getType() + ": Player " + player.getName() + " tried to logout while Fighting");
			}
			
			player.sendPacket(new SystemMessage(SystemMessageId.CANT_LOGOUT_WHILE_FIGHTING));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Dont allow leaving if player is in combat
		if (player.isInCombat() && !player.isGM())
		{
			player.sendMessage("You cannot Logout while is in Combat mode.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Dont allow leaving if player is teleporting
		if (player.isTeleporting() && !player.isGM())
		{
			player.sendMessage("You cannot Logout while is Teleporting.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.atEvent)
		{
			player.sendPacket(SystemMessage.sendString("A superior power doesn't allow you to leave the event."));
			return;
		}
		
		if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage("You can't Logout in Olympiad mode.");
			return;
		}
		
		// Prevent player from logging out if they are a festival participant nd it is in progress,
		// otherwise notify party members that the player is not longer a participant.
		if (player.isFestivalParticipant())
		{
			if (SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				player.sendMessage("You cannot Logout while you are a participant in a Festival.");
				return;
			}
			
			L2Party playerParty = player.getParty();
			if (playerParty != null)
			{
				player.getParty().broadcastToPartyMembers(SystemMessage.sendString(player.getName() + " has been removed from the upcoming Festival."));
			}
		}
		
		if (player.isFlying())
		{
			player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
		}
		
		if (Config.SELLBUFF_SYSTEM_OFFLINE && player.isSellBuff() && player.isSitting())
		{
			// Sleep effect, not official feature but however L2OFF features (like offline trade)
			if (Config.SELLBUFF_SYSTEM_OFFLINE_EFFECT)
			{
				player.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_SLEEP);
			}
			
			player.store();
			player.closeNetConnection();
			
			if (player.getOfflineStartTime() == 0)
			{
				player.setOfflineStartTime(System.currentTimeMillis());
			}
			
			return;
		}
		
		if (Config.OFFLINE_LOGOUT && player.isSitting())
		{
			if ((player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE) || (player.isInCraftMode() && Config.OFFLINE_CRAFT_ENABLE))
			{
				// Sleep effect, not official feature but however L2OFF features (like offline trade)
				if (Config.OFFLINE_SLEEP_EFFECT)
				{
					player.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_SLEEP);
				}
				
				player.store();
				player.closeNetConnection();
				
				if (player.getOfflineStartTime() == 0)
				{
					player.setOfflineStartTime(System.currentTimeMillis());
				}
				
				return;
			}
		}
		else if (player.isStored())
		{
			player.store();
			player.closeNetConnection();
			
			if (player.getOfflineStartTime() == 0)
			{
				player.setOfflineStartTime(System.currentTimeMillis());
			}
			
			return;
		}
		
		if (player.isCastingNow())
		{
			player.abortCast();
			player.sendPacket(new ActionFailed());
		}
		
		player.deleteMe();
	}
	
	@Override
	public String getType()
	{
		return "[C] 09 Logout";
	}
}
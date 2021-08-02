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
import l2jorion.game.GameServer;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.sevensigns.SevenSignsFestival;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.L2GameClient.GameClientState;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.CharSelectInfo;
import l2jorion.game.network.serverpackets.RestartResponse;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.taskmanager.AttackStanceTaskManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestRestart extends L2GameClientPacket
{
	private static Logger LOG = LoggerFactory.getLogger(RequestRestart.class.getName());
	
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
			LOG.warn("[RequestRestart] activeChar null!?");
			return;
		}
		
		// Check if player is enchanting
		if (player.getActiveEnchantItem() != null)
		{
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		// Check if player are changing class
		if (player.isLocked())
		{
			LOG.error(" [ERROR] [WARNING]Player " + player.getName() + " tried to restart during class change.");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		player.getInventory().updateDatabase();
		
		// Check if player is in private store
		if (player.getPrivateStoreType() != 0)
		{
			player.sendMessage("Cannot restart while trading.");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		// Check if player is in combat
		if (AttackStanceTaskManager.getInstance().getAttackStanceTask(player) && !(player.isGM() && Config.GM_RESTART_FIGHTING))
		{
			if (Config.DEBUG)
			{
				LOG.warn("Player " + player.getName() + " tried to logout while fighting.");
			}
			
			player.sendPacket(new SystemMessage(SystemMessageId.CANT_RESTART_WHILE_FIGHTING));
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		// Check if player is registred on olympiad
		if (player.getOlympiadGameId() > 0 || player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage("You can't restart while in Olympiad.");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		// Prevent player from restarting if they are a festival participant
		// and it is in progress, otherwise notify party members that the player
		// is not longer a participant.
		if (player.isFestivalParticipant())
		{
			if (SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				player.sendPacket(SystemMessage.sendString("You cannot restart while you are a participant in a festival."));
				player.sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(RestartResponse.valueOf(false));
				return;
			}
			
			L2Party playerParty = player.getParty();
			if (playerParty != null)
			{
				player.getParty().broadcastToPartyMembers(SystemMessage.sendString(player.getName() + " has been removed from the upcoming festival."));
			}
		}
		
		// Check if player is in Event
		if (player._inEventCTF || player._inEventDM || player._inEventTvT || player._inEventVIP)
		{
			player.sendMessage("You can't restart during Event.");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		// Fix against exploit anti-target
		if (player.isCastingNow())
		{
			player.abortCast();
			player.sendPacket(new ActionFailed());
		}
		
		// Check if player is teleporting
		if (player.isTeleporting())
		{
			player.abortCast();
			player.setIsTeleporting(false);
		}
		
		// Check if player is trading
		if (player.getActiveRequester() != null)
		{
			player.getActiveRequester().onTradeCancel(player);
			player.onTradeCancel(player.getActiveRequester());
		}
		
		// Check if player are flying
		if (player.isFlying())
		{
			player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
		}
		
		if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND) != null && player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND).isAugmented())
		{
			player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND).getAugmentation().removeBoni(player);
		}
		
		// delete box from the world
		if (player._active_boxes != -1)
		{
			player.decreaseBoxes();
		}
		
		L2GameClient client = getClient();
		
		// detach the client from the char so that the connection isnt closed in the deleteMe
		player.setClient(null);
		
		// removing player from the world
		player.deleteMe();
		player.store();
		
		getClient().setActiveChar(null);
		
		// return the client to the authed status
		client.setState(GameClientState.AUTHED);
		
		// before the char selection, check shutdown status
		if (GameServer.gameServer.getSelectorThread().isShutdown())
		{
			getClient().closeNow();
			return;
		}
		
		// Restart true
		sendPacket(RestartResponse.valueOf(true));
		
		// send char list
		CharSelectInfo cl = new CharSelectInfo(client.getAccountName(), client.getSessionId().playOkID1);
		sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
	}
	
	@Override
	public String getType()
	{
		return "[C] 46 RequestRestart";
	}
}

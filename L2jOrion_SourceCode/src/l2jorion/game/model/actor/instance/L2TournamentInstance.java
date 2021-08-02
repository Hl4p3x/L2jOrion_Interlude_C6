/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.actor.instance;

import java.util.List;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2World;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.entity.event.tournament.Arena2x2;
import l2jorion.game.model.entity.event.tournament.Arena4x4;
import l2jorion.game.model.entity.event.tournament.Arena9x9;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.util.random.Rnd;

public class L2TournamentInstance extends L2NpcInstance
{
	public L2TournamentInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onAction(L2PcInstance player)
	{
		if (this != player.getTarget())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
			player.sendPacket(new ValidateLocation(this));
		}
		else if (isInsideRadius(player, 150, false, false))
		{
			SocialAction sa = new SocialAction(getObjectId(), (int) Rnd.get());
			broadcastPacket(sa);
			player.sendPacket(new MoveToPawn(player, this, 150));
			showMessageWindow(player);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	@Override
	public String getHtmlPath(L2PcInstance player, int npcId, int val)
	{
		String filename = "";
		
		if (val == 0)
		{
			filename = "" + npcId;
		}
		else
		{
			filename = npcId + "-" + val;
		}
		
		return "data/html/mods/tournament/" + filename + ".htm";
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (player._active_boxes > 1 && !Config.ALLOW_DUALBOX_EVENT)
		{
			final List<String> players_in_boxes = player.active_boxes_characters;
			
			if (players_in_boxes != null && players_in_boxes.size() > 1)
			{
				for (final String character_name : players_in_boxes)
				{
					final L2PcInstance plyr = L2World.getInstance().getPlayer(character_name);
					
					if (plyr != null && plyr.isInArenaEvent())
					{
						player.sendMessage("You already participated in event with another character!");
						return;
					}
				}
			}
		}
		
		if (command.startsWith("2x2_register"))
		{
			if (!player.isInParty())
			{
				player.sendMessage("You dont have a party.");
				return;
			}
			
			if (!player.getParty().isLeader(player))
			{
				player.sendMessage("You are not the party leader!");
				return;
			}
			
			L2PcInstance assist = player.getParty().getPartyMembers().get(1);
			
			// checks
			if (player.isCursedWeaponEquipped() || assist.isCursedWeaponEquipped() || player.inObserverMode() || assist.inObserverMode() || player.isInStoreMode() || assist.isInStoreMode() || !player.isNoble() || !assist.isNoble() || player.isAio() || assist.isAio() || player.getKarma() > 0
				|| assist.getKarma() > 0)
			{
				player.sendMessage("You or your member does not have the necessary requirements.");
				assist.sendMessage("You or your member does not have the necessary requirements.");
				return;
			}
			
			// oly checks
			if (player.isInOlympiadMode() || assist.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player) || OlympiadManager.getInstance().isRegistered(assist))
			{
				player.sendMessage("You or your member is registered in the Olympiad.");
				assist.sendMessage("You or your member is registered in the Olympiad.");
				return;
			}
			
			// event checks
			if (player.isInFunEvent() || assist.isInFunEvent())
			{
				player.sendMessage("You or your member is registered in another event.");
				assist.sendMessage("You or your member is registered in another event.");
				return;
			}
			
			if (assist.getClassId() == ClassId.shillenElder || assist.getClassId() == ClassId.shillienSaint || assist.getClassId() == ClassId.bishop || assist.getClassId() == ClassId.cardinal || assist.getClassId() == ClassId.elder || assist.getClassId() == ClassId.evaSaint)
			{
				assist.sendMessage("You or your member class is not allowed in tournament.");
				player.sendMessage("You or your member class is not allowed in tournament.");
				return;
			}
			
			if (player.getClassId() == ClassId.shillenElder || player.getClassId() == ClassId.shillienSaint || player.getClassId() == ClassId.bishop || player.getClassId() == ClassId.cardinal || player.getClassId() == ClassId.elder || player.getClassId() == ClassId.evaSaint)
			{
				assist.sendMessage("You or your member class is not allowed in tournament.");
				player.sendMessage("You or your member class is not allowed in tournament.");
				return;
			}
			
			if (player.getClassId().getId() == assist.getClassId().getId())
			{
				player.sendMessage("Same class partner are not allowed.");
				assist.sendMessage("Same class partner are not allowed.");
				return;
			}
			
			/*
			 * if (player.getClient() != null && assist.getClient() != null) { String ip1 = player.getClient().getConnection().getInetAddress().getHostAddress(); String ip2 = assist.getClient().getConnection().getInetAddress().getHostAddress(); if (ip1.equals(ip2)) {
			 * player.sendMessage("Dual box is not allowed on tournament."); assist.sendMessage("Dual box is not allowed on tournament."); return; } }
			 */
			
			if (Arena2x2.getInstance().register(player, assist))
			{
				player.sendMessage(player.getName() + " Bring up your sword! Your party is registered!");
				assist.sendMessage(assist.getName() + " Bring up your sword! Your party is registered!");
				
				player.setArenaProtection(true);
				assist.setArenaProtection(true);
			}
			else
			{
				return;
			}
		}
		else if (command.startsWith("4x4_register"))
		{
			L2Party party = player.getParty();
			
			if (!player.isInParty())
			{
				player.sendMessage("You dont have a party.");
				return;
			}
			
			if (!player.getParty().isLeader(player))
			{
				player.sendMessage("You are not the party leader!");
				return;
			}
			
			if (party.getMemberCount() < 3)
			{
				player.sendMessage("You need party with at 3 members to register!");
				return;
			}
			
			// 4 Player + 1 Leader
			L2PcInstance assist = player.getParty().getPartyMembers().get(1);
			L2PcInstance assist2 = player.getParty().getPartyMembers().get(2);
			L2PcInstance assist3 = player.getParty().getPartyMembers().get(3);
			
			// checks
			if (player.isCursedWeaponEquipped() || assist.isCursedWeaponEquipped() || assist2.isCursedWeaponEquipped() || assist3.isCursedWeaponEquipped() || player.inObserverMode() || assist.inObserverMode() || assist2.inObserverMode() || assist3.inObserverMode() || player.isInStoreMode()
				|| assist.isInStoreMode() || assist2.isInStoreMode() || assist3.isInStoreMode() || !player.isNoble() || !assist.isNoble() || !assist2.isNoble() || !assist3.isNoble() || player.isAio() || assist.isAio() || assist2.isAio() || assist3.isAio() || player.getKarma() > 0
				|| assist.getKarma() > 0 || assist2.getKarma() > 0 || assist3.getKarma() > 0)
			{
				player.sendMessage("You or your member does not have the necessary requirements.");
				assist.sendMessage("You or your member does not have the necessary requirements.");
				assist2.sendMessage("You or your member does not have the necessary requirements.");
				assist3.sendMessage("You or your member does not have the necessary requirements.");
				return;
			}
			
			// oly checks
			if (player.isInOlympiadMode() || assist.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player) || OlympiadManager.getInstance().isRegistered(assist) || assist2.isInOlympiadMode() || assist3.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(assist2)
				|| OlympiadManager.getInstance().isRegistered(assist3))
			{
				player.sendMessage("You or your member is registered in the Olympiad.");
				assist.sendMessage("You or your member is registered in the Olympiad.");
				assist2.sendMessage("You or your member is registered in the Olympiad.");
				assist3.sendMessage("You or your member is registered in the Olympiad.");
				return;
			}
			
			// event checks
			if (player.isInFunEvent() || assist.isInFunEvent() || assist2.isInFunEvent() || assist3.isInFunEvent())
			{
				player.sendMessage("You or your member is registered in another event.");
				assist.sendMessage("You or your member is registered in another event.");
				assist2.sendMessage("You or your member is registered in another event.");
				assist3.sendMessage("You or your member is registered in another event.");
				return;
			}
			
			// dual box checks
			/*
			 * if (player.getClient() != null && assist.getClient() != null && assist2.getClient() != null && assist3.getClient() != null) { String ip1 = player.getClient().getConnection().getInetAddress().getHostAddress(); String ip2 =
			 * assist.getClient().getConnection().getInetAddress().getHostAddress(); String ip3 = assist2.getClient().getConnection().getInetAddress().getHostAddress(); String ip4 = assist3.getClient().getConnection().getInetAddress().getHostAddress(); if (ip1.equals(ip2) || ip1.equals(ip3) ||
			 * ip1.equals(ip4) || ip2.equals(ip1) || ip2.equals(ip3) || ip2.equals(ip4)) { player.sendMessage("Dual box is not allowed on tournament."); assist.sendMessage("Dual box is not allowed on tournament."); assist2.sendMessage("Dual box is not allowed on tournament.");
			 * assist3.sendMessage("Dual box is not allowed on tournament."); return; } }
			 */
			
			// class
			if (player.getClassId().getId() == assist.getClassId().getId() || player.getClassId().getId() == assist2.getClassId().getId() || player.getClassId().getId() == assist3.getClassId().getId() || assist.getClassId().getId() == assist2.getClassId().getId()
				|| assist.getClassId().getId() == assist3.getClassId().getId() || assist2.getClassId().getId() == assist.getClassId().getId() || assist2.getClassId().getId() == assist3.getClassId().getId())
			{
				player.sendMessage("Same class partner are not allowed.");
				assist.sendMessage("Same class partner are not allowed.");
				assist2.sendMessage("Same class partner are not allowed.");
				assist3.sendMessage("Same class partner are not allowed.");
				return;
			}
			
			// Register party
			if (Arena4x4.getInstance().register(player, assist, assist2, assist3))
			{
				player.sendMessage(player.getName() + " Bring up your sword! Your party is registered!");
				assist.sendMessage(assist.getName() + " Bring up your sword! Your party is registered!");
				assist2.sendMessage(assist2.getName() + " Bring up your sword! Your party is registered!");
				assist3.sendMessage(assist3.getName() + " Bring up your sword! Your party is registered!");
			}
			else
			{
				return;
			}
		}
		else if (command.startsWith("9x9_register"))
		{
			L2Party party = player.getParty();
			
			if (!player.isInParty())
			{
				player.sendMessage("You dont have a party.");
				return;
			}
			
			if (!player.getParty().isLeader(player))
			{
				player.sendMessage("You are not the party leader!");
				return;
			}
			
			if (party.getMemberCount() < 8)
			{
				player.sendMessage("You need party with at 9 members to register!");
				return;
			}
			
			// 8 Player + 1 Leader
			L2PcInstance assist1 = player.getParty().getPartyMembers().get(1);
			L2PcInstance assist2 = player.getParty().getPartyMembers().get(2);
			L2PcInstance assist3 = player.getParty().getPartyMembers().get(3);
			L2PcInstance assist4 = player.getParty().getPartyMembers().get(4);
			L2PcInstance assist5 = player.getParty().getPartyMembers().get(5);
			L2PcInstance assist6 = player.getParty().getPartyMembers().get(6);
			L2PcInstance assist7 = player.getParty().getPartyMembers().get(7);
			L2PcInstance assist8 = player.getParty().getPartyMembers().get(8);
			
			// checks
			if (player.isCursedWeaponEquipped() || assist1.isCursedWeaponEquipped() || assist2.isCursedWeaponEquipped() || assist3.isCursedWeaponEquipped() || assist4.isCursedWeaponEquipped() || assist5.isCursedWeaponEquipped() || assist6.isCursedWeaponEquipped() || assist7.isCursedWeaponEquipped()
				|| assist8.isCursedWeaponEquipped() || player.inObserverMode() || assist1.inObserverMode() || assist2.inObserverMode() || assist3.inObserverMode() || assist4.inObserverMode() || assist5.inObserverMode() || assist6.inObserverMode() || assist7.inObserverMode()
				|| assist8.inObserverMode() || player.isInStoreMode() || assist1.isInStoreMode() || assist2.isInStoreMode() || assist3.isInStoreMode() || assist4.isInStoreMode() || assist5.isInStoreMode() || assist6.isInStoreMode() || assist7.isInStoreMode() || assist8.isInStoreMode()
				|| !player.isNoble() || !assist1.isNoble() || !assist2.isNoble() || !assist3.isNoble() || !assist4.isNoble() || !assist5.isNoble() || !assist6.isNoble() || !assist7.isNoble() || !assist8.isNoble() || player.isAio() || assist1.isAio() || assist2.isAio() || assist3.isAio()
				|| assist4.isAio() || assist5.isAio() || assist6.isAio() || assist7.isAio() || assist8.isAio() || player.getKarma() > 0 || assist1.getKarma() > 0 || assist2.getKarma() > 0 || assist3.getKarma() > 0 || assist4.getKarma() > 0 || assist5.getKarma() > 0 || assist6.getKarma() > 0
				|| assist7.getKarma() > 0 || assist8.getKarma() > 0)
			{
				player.sendMessage("You or your member does not have the necessary requirements.");
				assist1.sendMessage("You or your member does not have the necessary requirements.");
				assist2.sendMessage("You or your member does not have the necessary requirements.");
				assist3.sendMessage("You or your member does not have the necessary requirements.");
				assist4.sendMessage("You or your member does not have the necessary requirements.");
				assist5.sendMessage("You or your member does not have the necessary requirements.");
				assist6.sendMessage("You or your member does not have the necessary requirements.");
				assist7.sendMessage("You or your member does not have the necessary requirements.");
				assist8.sendMessage("You or your member does not have the necessary requirements.");
				return;
			}
			
			// oly checks
			if (player.isInOlympiadMode() || assist1.isInOlympiadMode() || assist2.isInOlympiadMode() || assist3.isInOlympiadMode() || assist4.isInOlympiadMode() || assist5.isInOlympiadMode() || assist6.isInOlympiadMode() || assist7.isInOlympiadMode() || assist8.isInOlympiadMode()
				|| OlympiadManager.getInstance().isRegistered(player) || OlympiadManager.getInstance().isRegistered(assist1) || OlympiadManager.getInstance().isRegistered(assist2) || OlympiadManager.getInstance().isRegistered(assist3) || OlympiadManager.getInstance().isRegistered(assist4)
				|| OlympiadManager.getInstance().isRegistered(assist5) || OlympiadManager.getInstance().isRegistered(assist6) || OlympiadManager.getInstance().isRegistered(assist7) || OlympiadManager.getInstance().isRegistered(assist8))
			{
				player.sendMessage("You or your member is registered in the Olympiad.");
				assist1.sendMessage("You or your member is registered in the Olympiad.");
				assist2.sendMessage("You or your member is registered in the Olympiad.");
				assist3.sendMessage("You or your member is registered in the Olympiad.");
				assist4.sendMessage("You or your member is registered in the Olympiad.");
				assist5.sendMessage("You or your member is registered in the Olympiad.");
				assist6.sendMessage("You or your member is registered in the Olympiad.");
				assist7.sendMessage("You or your member is registered in the Olympiad.");
				assist8.sendMessage("You or your member is registered in the Olympiad.");
				return;
			}
			
			// event checks
			if (player.isInFunEvent() || assist1.isInFunEvent() || assist2.isInFunEvent() || assist3.isInFunEvent() || assist4.isInFunEvent() || assist5.isInFunEvent() || assist6.isInFunEvent() || assist7.isInFunEvent() || assist8.isInFunEvent())
			{
				player.sendMessage("You or your member is registered in another event.");
				assist1.sendMessage("You or your member is registered in another event.");
				assist2.sendMessage("You or your member is registered in another event.");
				assist3.sendMessage("You or your member is registered in another event.");
				assist4.sendMessage("You or your member is registered in another event.");
				assist5.sendMessage("You or your member is registered in another event.");
				assist6.sendMessage("You or your member is registered in another event.");
				assist7.sendMessage("You or your member is registered in another event.");
				assist8.sendMessage("You or your member is registered in another event.");
				return;
			}
			
			// dual box checks
			/*
			 * if (player.getClient() != null && assist1.getClient() != null && assist2.getClient() != null && assist3.getClient() != null && assist4.getClient() != null && assist5.getClient() != null && assist6.getClient() != null && assist7.getClient() != null && assist8.getClient() != null) {
			 * String ip1 = player.getClient().getConnection().getInetAddress().getHostAddress(); String ip2 = assist1.getClient().getConnection().getInetAddress().getHostAddress(); String ip3 = assist2.getClient().getConnection().getInetAddress().getHostAddress(); String ip4 =
			 * assist3.getClient().getConnection().getInetAddress().getHostAddress(); String ip5 = assist4.getClient().getConnection().getInetAddress().getHostAddress(); String ip6 = assist5.getClient().getConnection().getInetAddress().getHostAddress(); String ip7 =
			 * assist6.getClient().getConnection().getInetAddress().getHostAddress(); String ip8 = assist7.getClient().getConnection().getInetAddress().getHostAddress(); String ip9 = assist8.getClient().getConnection().getInetAddress().getHostAddress(); if (ip1.equals(ip2) || ip1.equals(ip3) ||
			 * ip1.equals(ip4) || ip1.equals(ip5) || ip1.equals(ip6) || ip1.equals(ip7) || ip1.equals(ip8) || ip1.equals(ip9)) { player.sendMessage("Dual box is not allowed on tournament."); assist1.sendMessage("Dual box is not allowed on tournament.");
			 * assist2.sendMessage("Dual box is not allowed on tournament."); assist3.sendMessage("Dual box is not allowed on tournament."); assist4.sendMessage("Dual box is not allowed on tournament."); assist5.sendMessage("Dual box is not allowed on tournament.");
			 * assist6.sendMessage("Dual box is not allowed on tournament."); assist7.sendMessage("Dual box is not allowed on tournament."); assist8.sendMessage("Dual box is not allowed on tournament."); return; } }
			 */
			
			// Register party
			if (Arena9x9.getInstance().register(player, assist1, assist2, assist3, assist4, assist5, assist6, assist7, assist8))
			{
				player.sendMessage(player.getName() + " Bring up your sword! Your party is registered!");
				assist1.sendMessage(assist1.getName() + " Bring up your sword! Your party is registered!");
				assist2.sendMessage(assist2.getName() + " Bring up your sword! Your party is registered!");
				assist3.sendMessage(assist3.getName() + " Bring up your sword! Your party is registered!");
				assist4.sendMessage(assist4.getName() + " Bring up your sword! Your party is registered!");
				assist5.sendMessage(assist5.getName() + " Bring up your sword! Your party is registered!");
				assist6.sendMessage(assist6.getName() + " Bring up your sword! Your party is registered!");
				assist7.sendMessage(assist7.getName() + " Bring up your sword! Your party is registered!");
				assist8.sendMessage(assist8.getName() + " Bring up your sword! Your party is registered!");
			}
			else
			{
				return;
			}
		}
		else if (command.startsWith("remove"))
		{
			Arena2x2.getInstance().remove(player);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	private void showMessageWindow(L2PcInstance player)
	{
		String filename = "data/html/mods/tournament/" + getNpcId() + ".htm";
		
		filename = getHtmlPath(player, getNpcId(), 0);
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}
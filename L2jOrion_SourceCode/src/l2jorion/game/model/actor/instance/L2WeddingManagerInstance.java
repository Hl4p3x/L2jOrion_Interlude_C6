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
package l2jorion.game.model.actor.instance;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.enums.AchType;
import l2jorion.game.managers.CoupleManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.Wedding;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.util.random.Rnd;

public class L2WeddingManagerInstance extends L2NpcInstance
{
	public L2WeddingManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else
		{
			if (!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				if (player.isMoving())
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, this);
				}
				
				player.broadcastPacket(new MoveToPawn(player, this, L2NpcInstance.INTERACTION_DISTANCE));
				
				broadcastPacket(new SocialAction(getObjectId(), Rnd.get(8)));
				
				showMessageWindow(player);
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void showMessageWindow(L2PcInstance player)
	{
		String filename = "data/html/mods/Wedding_start.htm";
		String replace = String.valueOf(Config.L2JMOD_WEDDING_PRICE);
		
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%replace%", replace);
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		// standard msg
		String filename = "data/html/mods/Wedding_start.htm";
		String replace = "";
		
		// if player has no partner
		if (player.getPartnerId() == 0)
		{
			filename = "data/html/mods/Wedding_nopartner.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}
		
		L2PcInstance ptarget = (L2PcInstance) L2World.getInstance().findObject(player.getPartnerId());
		// partner online ?
		if (ptarget == null || ptarget.isOnline() == 0)
		{
			filename = "data/html/mods/Wedding_notfound.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}
		
		// already married ?
		if (player.isMarried())
		{
			filename = "data/html/mods/Wedding_already.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}
		else if (player.isMarryAccepted())
		{
			filename = "data/html/mods/Wedding_waitforpartner.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}
		else if (command.startsWith("AcceptWedding"))
		{
			// accept the wedding request
			player.setMarryAccepted(true);
			
			int type;
			if (player.getAppearance().getSex() && ptarget.getAppearance().getSex())
			{
				player.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_LESBO);
				ptarget.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_LESBO);
				type = 1;
			}
			else if (!player.getAppearance().getSex() && !ptarget.getAppearance().getSex())
			{
				player.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_GEY);
				ptarget.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_GEY);
				type = 2;
			}
			else
			{
				player.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_NORMAL);
				ptarget.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_NORMAL);
				type = 0;
			}
			
			Wedding wedding = CoupleManager.getInstance().getCouple(player.getCoupleId());
			wedding.marry(type);
			
			// messages to the couple
			player.sendMessage("Congratulations you are married!");
			player.setMarried(true);
			player.setMaryRequest(false);
			player.setmarriedType(type);
			ptarget.sendMessage("Congratulations you are married!");
			ptarget.setMarried(true);
			ptarget.setMaryRequest(false);
			ptarget.setmarriedType(type);
			
			if (Config.WEDDING_GIVE_CUPID_BOW)
			{
				player.addItem("Cupids Bow", 9140, 1, player, true);
				player.getInventory().updateDatabase();
				ptarget.addItem("Cupids Bow", 9140, 1, ptarget, true);
				ptarget.getInventory().updateDatabase();
				player.sendSkillList();
				ptarget.sendSkillList();
			}
			
			// wedding march
			MagicSkillUser MSU = new MagicSkillUser(player, player, 2230, 1, 1, 0);
			player.broadcastPacket(MSU);
			MSU = new MagicSkillUser(ptarget, ptarget, 2230, 1, 1, 0);
			ptarget.broadcastPacket(MSU);
			
			// fireworks
			L2Skill skill = SkillTable.getInstance().getInfo(2025, 1);
			if (skill != null)
			{
				MSU = new MagicSkillUser(player, player, 2025, 1, 1, 0);
				player.sendPacket(MSU);
				player.broadcastPacket(MSU);
				player.useMagic(skill, false, false);
				
				MSU = new MagicSkillUser(ptarget, ptarget, 2025, 1, 1, 0);
				ptarget.sendPacket(MSU);
				ptarget.broadcastPacket(MSU);
				ptarget.useMagic(skill, false, false);
			}
			
			player.getAchievement().increase(AchType.MARRIED);
			ptarget.getAchievement().increase(AchType.MARRIED);
			
			if (Config.ANNOUNCE_WEDDING)
			{
				Announcements.getInstance().announceToAll("Congratulations to " + player.getName() + " and " + ptarget.getName() + "! They have been married.");
			}
			
			filename = "data/html/mods/Wedding_accepted.htm";
			replace = ptarget.getName();
			sendHtmlMessage(ptarget, filename, replace);
			return;
		}
		else if (command.startsWith("DeclineWedding"))
		{
			player.setMaryRequest(false);
			ptarget.setMaryRequest(false);
			player.setMarryAccepted(false);
			ptarget.setMarryAccepted(false);
			player.getAppearance().setNameColor(0xFFFFFF);
			ptarget.getAppearance().setNameColor(0xFFFFFF);
			player.sendMessage("You declined");
			ptarget.sendMessage("Your partner declined");
			replace = ptarget.getName();
			filename = "data/html/mods/Wedding_declined.htm";
			sendHtmlMessage(ptarget, filename, replace);
			return;
		}
		else if (player.isMaryRequest())
		{
			// check for formalwear
			if (Config.L2JMOD_WEDDING_FORMALWEAR)
			{
				Inventory inv3 = player.getInventory();
				L2ItemInstance item3 = inv3.getPaperdollItem(10);
				if (item3 == null)
				{
					player.setIsWearingFormalWear(false);
				}
				else
				{
					String strItem = Integer.toString(item3.getItemId());
					String frmWear = Integer.toString(6408);
					player.sendMessage(strItem);
					if (strItem.equals(frmWear))
					{
						player.setIsWearingFormalWear(true);
					}
					else
					{
						player.setIsWearingFormalWear(false);
					}
				}
			}
			
			if (Config.L2JMOD_WEDDING_FORMALWEAR && !player.isWearingFormalWear())
			{
				filename = "data/html/mods/Wedding_noformal.htm";
				sendHtmlMessage(player, filename, replace);
				return;
			}
			
			filename = "data/html/mods/Wedding_ask.htm";
			player.setMaryRequest(false);
			ptarget.setMaryRequest(false);
			replace = ptarget.getName();
			sendHtmlMessage(player, filename, replace);
			return;
		}
		else if (command.startsWith("AskWedding"))
		{
			// check for formalwear
			if (Config.L2JMOD_WEDDING_FORMALWEAR)
			{
				Inventory inv3 = player.getInventory();
				L2ItemInstance item3 = inv3.getPaperdollItem(10);
				
				if (null == item3)
				{
					player.setIsWearingFormalWear(false);
				}
				else
				{
					String frmWear = Integer.toString(6408);
					String strItem = null;
					strItem = Integer.toString(item3.getItemId());
					
					if (null != strItem && strItem.equals(frmWear))
					{
						player.setIsWearingFormalWear(true);
					}
					else
					{
						player.setIsWearingFormalWear(false);
					}
				}
			}
			
			if (Config.L2JMOD_WEDDING_FORMALWEAR && !player.isWearingFormalWear())
			{
				filename = "data/html/mods/Wedding_noformal.htm";
				sendHtmlMessage(player, filename, replace);
				return;
			}
			else if (player.getAdena() < Config.L2JMOD_WEDDING_PRICE)
			{
				filename = "data/html/mods/Wedding_adena.htm";
				replace = String.valueOf(Config.L2JMOD_WEDDING_PRICE);
				sendHtmlMessage(player, filename, replace);
				return;
			}
			else
			{
				player.setMarryAccepted(true);
				ptarget.setMaryRequest(true);
				replace = ptarget.getName();
				filename = "data/html/mods/Wedding_requested.htm";
				player.getInventory().reduceAdena("Wedding", Config.L2JMOD_WEDDING_PRICE, player, player.getLastFolkNPC());
				sendHtmlMessage(player, filename, replace);
				return;
			}
		}
		sendHtmlMessage(player, filename, replace);
	}
	
	private void sendHtmlMessage(L2PcInstance player, String filename, String replace)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%replace%", replace);
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}

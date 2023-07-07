package l2jorion.game.powerpack.other;

import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.managers.CoupleManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.Wedding;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.templates.L2Item;

public class WeddingPanel implements ICustomByPassHandler
{
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"wedding",
			"AskWedding",
			"AcceptWedding",
			"DeclineWedding"
		};
	}
	
	private enum CommandEnum
	{
		wedding,
		AskWedding,
		AcceptWedding,
		DeclineWedding
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		CommandEnum comm = CommandEnum.valueOf(command);
		
		if (comm == null)
		{
			return;
		}
		
		String filename = "data/html/gmshop/wedding/Wedding_start.htm";
		String replace = String.valueOf(Config.L2JMOD_WEDDING_PRICE);
		
		switch (comm)
		{
			case wedding:
			{
				
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(filename);
				html.replace("%replace%", replace);
				player.sendPacket(html);
				break;
			}
			case AskWedding:
			{
				L2PcInstance ptarget = (L2PcInstance) L2World.getInstance().findObject(player.getPartnerId());
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
				
				int pcBangPoints = player.getPcBangScore();
				final L2ItemInstance item = player.getInventory().getItemByItemId(4037);
				
				if (item == null || item.getCount() < 300)
				{
					player.sendMessage("You don't have enough " + L2Item.getItemNameById(Config.CUSTOM_ITEM_ID) + ".");
					return;
				}
				
				if (pcBangPoints < 300)
				{
					player.sendMessage("You don't have enough PCB Points.");
					return;
				}
				
				player.reducePcBangScore(300);
				
				player.destroyItem("Consume", item.getObjectId(), Config.CHANGEACCOUNT, null, true);
				
				if (Config.L2JMOD_WEDDING_FORMALWEAR && !player.isWearingFormalWear())
				{
					filename = "data/html/gmshop/wedding/Wedding_noformal.htm";
					sendHtmlMessage(player, filename, replace);
					return;
				}
				/*
				 * else if (player.getAdena() < Config.L2JMOD_WEDDING_PRICE) { filename = "data/html/gmshop/wedding/Wedding_adena.htm"; replace = String.valueOf(Config.L2JMOD_WEDDING_PRICE); sendHtmlMessage(player, filename, replace); return; }
				 */
				else if (player.isMaryRequest())
				{
					filename = "data/html/gmshop/wedding/Wedding_ask.htm";
					player.setMaryRequest(false);
					ptarget.setMaryRequest(false);
					replace = ptarget.getName();
					sendHtmlMessage(player, filename, replace);
					return;
				}
				else
				{
					player.setMarryAccepted(true);
					ptarget.setMaryRequest(true);
					replace = ptarget.getName();
					filename = "data/html/gmshop/wedding/Wedding_requested.htm";
					
					// player.getInventory().reduceAdena("Wedding", Config.L2JMOD_WEDDING_PRICE, player, player.getLastFolkNPC());
					
					sendHtmlMessage(player, filename, replace);
					return;
				}
			}
			case AcceptWedding:
			{
				L2PcInstance ptarget = (L2PcInstance) L2World.getInstance().findObject(player.getPartnerId());
				// accept the wedding request
				player.setMarryAccepted(true);
				
				int type;
				if (player.getAppearance().getSex() && ptarget.getAppearance().getSex())
				{
					// player.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_LESBO);
					// ptarget.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_LESBO);
					type = 1;
				}
				else if (!player.getAppearance().getSex() && !ptarget.getAppearance().getSex())
				{
					// player.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_GEY);
					// ptarget.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_GEY);
					type = 2;
				}
				else
				{
					// player.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_NORMAL);
					// ptarget.getAppearance().setNameColor(Config.L2JMOD_WEDDING_NAME_COLOR_NORMAL);
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
				
				if (Config.ANNOUNCE_WEDDING)
				{
					Announcements.getInstance().announceToAll("Congratulations to " + player.getName() + " and " + ptarget.getName() + "! They have been married.");
				}
				
				filename = "data/html/gmshop/wedding/Wedding_accepted.htm";
				replace = ptarget.getName();
				sendHtmlMessage(ptarget, filename, replace);
				break;
			}
			case DeclineWedding:
			{
				L2PcInstance ptarget = (L2PcInstance) L2World.getInstance().findObject(player.getPartnerId());
				player.setMaryRequest(false);
				ptarget.setMaryRequest(false);
				player.setMarryAccepted(false);
				ptarget.setMarryAccepted(false);
				player.getAppearance().setNameColor(0xFFFFFF);
				ptarget.getAppearance().setNameColor(0xFFFFFF);
				player.sendMessage("You declined");
				ptarget.sendMessage("Your partner declined");
				replace = ptarget.getName();
				filename = "data/html/gmshop/wedding/Wedding_declined.htm";
				sendHtmlMessage(ptarget, filename, replace);
				break;
			}
		}
	}
	
	private void sendHtmlMessage(L2PcInstance player, String filename, String replace)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%replace%", replace);
		player.sendPacket(html);
	}
}
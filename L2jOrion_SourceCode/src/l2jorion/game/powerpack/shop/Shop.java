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
package l2jorion.game.powerpack.shop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.community.manager.BaseBBSManager;
import l2jorion.game.controllers.TradeController;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.CharNameTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.datatables.sql.HennaTreeTable;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.handler.ICommunityBoardHandler;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2EnchantSkillLearn;
import l2jorion.game.model.L2PledgeSkillLearn;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2TradeList;
import l2jorion.game.model.L2World;
import l2jorion.game.model.PcFreight;
import l2jorion.game.model.actor.instance.L2HennaInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.multisell.L2Multisell;
import l2jorion.game.model.multisell.MultiSellListContainer;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.AquireSkillList;
import l2jorion.game.network.serverpackets.BuyList;
import l2jorion.game.network.serverpackets.EnchantResult;
import l2jorion.game.network.serverpackets.EtcStatusUpdate;
import l2jorion.game.network.serverpackets.ExEnchantSkillList;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.ExShowVariationCancelWindow;
import l2jorion.game.network.serverpackets.ExShowVariationMakeWindow;
import l2jorion.game.network.serverpackets.HennaEquipList;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PackageToList;
import l2jorion.game.network.serverpackets.PartySmallWindowAll;
import l2jorion.game.network.serverpackets.PartySmallWindowDeleteAll;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.PledgeShowInfoUpdate;
import l2jorion.game.network.serverpackets.PledgeShowMemberListAll;
import l2jorion.game.network.serverpackets.PledgeShowMemberListUpdate;
import l2jorion.game.network.serverpackets.PledgeSkillList;
import l2jorion.game.network.serverpackets.SellList;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.game.network.serverpackets.WareHouseDepositList;
import l2jorion.game.network.serverpackets.WareHouseWithdrawalList;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.taskmanager.AttackStanceTaskManager;
import l2jorion.game.templates.L2Item;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class Shop implements IVoicedCommandHandler, ICustomByPassHandler, ICommunityBoardHandler
{
	private static Logger LOG = LoggerFactory.getLogger(Shop.class);
	
	private int _secondsDc;
	
	private String path = "data/html/custom/services/item_enchanter/";
	private String filename = "template.htm";
	private boolean free = false;
	
	private List<L2ItemInstance> list = new ArrayList<>();
	
	public static Map<Integer, String> _visitedPages = new ConcurrentHashMap<>();
	
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[]
		{
			PowerPackConfig.GMSHOP_COMMAND
		};
	}
	
	private boolean checkAllowed(L2PcInstance activeChar)
	{
		if (activeChar.isGM())
		{
			return true;
		}
		
		String msg = null;
		
		if (Config.L2LIMIT_CUSTOM)
		{
			if (!activeChar.isInsideZone(ZoneId.ZONE_PEACE))
			{
				msg = "You can't use Shop outside town.";
				activeChar.sendMessage(msg);
				activeChar.sendPacket(new ExShowScreenMessage(msg, 2000, 2, false));
				activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return false;
			}
		}
		
		if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("PREMIUM") && activeChar.getPremiumService() == 0)
		{
			msg = "This feature is only available for The Premium Account.";
		}
		else if (activeChar.isSitting())
		{
			msg = "Shop is not available when you sit.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("ALL"))
		{
			msg = "Shop is not available in this area.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("CURSED") && activeChar.isCursedWeaponEquiped())
		{
			msg = "Shop is not available with the cursed weapon.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("ATTACK") && AttackStanceTaskManager.getInstance().getAttackStanceTask(activeChar))
		{
			msg = "Shop is not available during the battle.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("DUNGEON") && activeChar.isIn7sDungeon())
		{
			msg = "Shop is not available in the catacomb and necropolis.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("RB") && activeChar.isInsideZone(ZoneId.ZONE_NOSUMMONFRIEND))
		{
			msg = "Shop is not available in this area.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("PVP") && activeChar.isInsideZone(ZoneId.ZONE_PVP))
		{
			msg = "Shop is not available in this area.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("PEACE") && activeChar.isInsideZone(ZoneId.ZONE_PEACE))
		{
			msg = "Shop is not available in this area.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("SIEGE") && activeChar.isInsideZone(ZoneId.ZONE_SIEGE))
		{
			msg = "Shop is not available in this area.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("OLYMPIAD") && (activeChar.isInOlympiadMode() || activeChar.isInsideZone(ZoneId.ZONE_OLY) || OlympiadManager.getInstance().isRegistered(activeChar) || OlympiadManager.getInstance().isRegisteredInComp(activeChar)))
		{
			msg = "Shop is not available at Olympiad.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("EVENT") && (activeChar._inEvent))
		{
			msg = "Shop is not available at the opening event.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("TVT") && activeChar._inEventTvT && TvT.is_started())
		{
			msg = "Shop is not available in TVT.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("CTF") && activeChar._inEventCTF && CTF.is_started())
		{
			msg = "Shop is not available in CTF.";
		}
		else if (PowerPackConfig.GMSHOP_EXCLUDE_ON.contains("DM") && activeChar._inEventDM && DM.is_started())
		{
			msg = "Shop is not available in DM.";
		}
		
		if (msg != null)
		{
			activeChar.sendMessage(msg);
			activeChar.sendPacket(new ExShowScreenMessage(msg, 2000, 2, false));
			activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
		}
		
		return msg == null;
	}
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String params)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (!checkAllowed(activeChar))
		{
			return false;
		}
		
		if (command.startsWith(PowerPackConfig.GMSHOP_COMMAND))
		{
			String text = HtmCache.getInstance().getHtm("data/html/gmshop/gmshop.htm");
			NpcHtmlMessage htm = new NpcHtmlMessage(1);
			htm.setHtml(text);
			activeChar.sendPacket(htm);
			
			synchronized (_visitedPages)
			{
				_visitedPages.put(activeChar.getObjectId(), String.valueOf(0));
			}
		}
		
		return false;
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		if (player == null)
		{
			return;
		}
		
		StringTokenizer st = new StringTokenizer(parameters, " ");
		
		final int currency = Config.CUSTOM_ITEM_ID;
		final L2ItemInstance item = player.getInventory().getItemByItemId(currency);
		String currencyName = L2Item.getItemNameById(currency);
		
		if (parameters == null || parameters.length() == 0)
		{
			return;
		}
		
		if (!checkAllowed(player))
		{
			return;
		}
		
		L2NpcInstance gmshopnpc = null;
		
		if (!PowerPackConfig.GMSHOP_USEBBS && !PowerPackConfig.GMSHOP_USECOMMAND)
		{
			if (player.getTarget() != null && player.getTarget() instanceof L2NpcInstance)
			{
				gmshopnpc = (L2NpcInstance) player.getTarget();
			}
			
			if (gmshopnpc == null)
			{
				return;
			}
			
			if (!player.isInsideRadius(gmshopnpc, L2NpcInstance.INTERACTION_DISTANCE, false, false))
			{
				return;
			}
		}
		
		// _visitedPages.clear();
		
		if (parameters.startsWith("Chat"))
		{
			String chatIndex = parameters.substring(4).trim();
			
			synchronized (_visitedPages)
			{
				_visitedPages.put(player.getObjectId(), chatIndex);
			}
			
			chatIndex = "-" + chatIndex;
			
			if (chatIndex.equals("-0"))
			{
				chatIndex = "";
			}
			
			String text = HtmCache.getInstance().getHtm("data/html/gmshop/gmshop" + chatIndex + ".htm");
			
			if (command.startsWith("bbsgmshop"))
			{
				text = text.replaceAll("custom_gmshop", "bbs_bbsgmshop");
				text = text.replaceAll("<title>", "<br><center>");
				text = text.replaceAll("</title>", "</center>");
				BaseBBSManager.separateAndSend(text, player, (Config.LIFEDRAIN_CUSTOM ? true : false));
			}
			else
			{
				NpcHtmlMessage htm = new NpcHtmlMessage(1);
				htm.setHtml(text);
				player.sendPacket(htm);
			}
		}
		else if (parameters.startsWith("multisell"))
		{
			returnHtm(command, "Chat", player);
			
			try
			{
				int listId = Integer.parseInt(parameters.substring(9).trim());
				MultiSellListContainer list = L2Multisell.getInstance().getList(listId);
				
				if (list != null && list.getNpcId() != null && list.getNpcId().equals(String.valueOf("shop")))
				{
					player.setTempAccess(true);
					L2Multisell.getInstance().SeparateAndSend(listId, player, false, 0);
				}
				else
				{
					player.sendMessage("This list does not exist");
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				player.sendMessage("This list does not exist");
			}
		}
		else if (parameters.startsWith("exc_multisell"))
		{
			returnHtm(command, "Chat", player);
			
			try
			{
				int listId = Integer.parseInt(parameters.substring(13).trim());
				player.setTempAccess(true);
				L2Multisell.getInstance().SeparateAndSend(listId, player, true, 0);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				player.sendMessage("This list does not exist");
			}
		}
		else if (parameters.startsWith("spec_exchange_multisell"))
		{
			try
			{
				int listId = Integer.parseInt(parameters.substring(23).trim());
				player.setTempAccess(true);
				L2Multisell.getInstance().SeparateAndSend(listId, player, true, 0);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				player.sendMessage("This list does not exist");
			}
		}
		else if (parameters.startsWith("Buy"))
		{
			player.setTempAccess(true);
			showBuyWindow(player, Integer.parseInt(parameters.substring(3).trim()));
		}
		else if (parameters.startsWith("Sell"))
		{
			try
			{
				showSellWindow(player);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		else if (parameters.startsWith("DepositP"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.setActiveWarehouse(player.getWarehouse());
			player.tempInvetoryDisable();
			player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.PRIVATE));
		}
		else if (parameters.startsWith("WithdrawP"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.setActiveWarehouse(player.getWarehouse());
			
			if (player.getActiveWarehouse().getSize() == 0)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
				return;
			}
			player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE));
		}
		else if (parameters.startsWith("WithdrawC"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE));
				return;
			}
			
			if (player.getClan().getLevel() == 0)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
			}
			else
			{
				player.setActiveWarehouse(player.getClan().getWarehouse());
				player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN));
			}
		}
		else if (parameters.startsWith("DepositC"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			if (player.getClan() != null)
			{
				if (player.getClan().getLevel() == 0)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
				}
				else
				{
					player.setActiveWarehouse(player.getClan().getWarehouse());
					player.tempInvetoryDisable();
					
					WareHouseDepositList dl = new WareHouseDepositList(player, WareHouseDepositList.CLAN);
					player.sendPacket(dl);
					dl = null;
				}
			}
		}
		else if (parameters.startsWith("WithdrawF"))
		{
			if (Config.ALLOW_FREIGHT)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				if (Config.DEBUG)
				{
					LOG.debug("Showing freightened items");
				}
				
				PcFreight freight = player.getFreight();
				
				if (freight != null)
				{
					if (freight.getSize() > 0)
					{
						freight.setActiveLocation(0);
						
						player.setActiveWarehouse(freight);
						player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.FREIGHT));
					}
					else
					{
						player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
					}
				}
				else
				{
					if (Config.DEBUG)
					{
						LOG.debug("no items freightened");
					}
				}
				freight = null;
			}
		}
		else if (parameters.startsWith("DepositF"))
		{
			if (Config.ALLOW_FREIGHT)
			{
				if (player.getAccountChars().size() == 0)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
				}
				// One or more chars other than this player for this account
				else
				{
					
					Map<Integer, String> chars = player.getAccountChars();
					
					if (chars.size() < 1)
					{
						player.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					
					player.sendPacket(new PackageToList(chars));
					chars = null;
					
					if (Config.DEBUG)
					{
						LOG.debug("Showing destination chars to freight - char src: " + player.getName());
					}
				}
			}
		}
		else if (parameters.startsWith("FreightChar"))
		{
			if (Config.ALLOW_FREIGHT)
			{
				final int startOfId = command.lastIndexOf("_") + 1;
				final String id = command.substring(startOfId);
				showDepositWindowFreight(player, Integer.parseInt(id));
			}
		}
		else if (parameters.startsWith("Draw"))
		{
			final L2HennaInstance[] henna = HennaTreeTable.getInstance().getAvailableHenna(player.getClassId());
			final HennaEquipList hel = new HennaEquipList(player, henna);
			player.sendPacket(hel);
			
			player.sendPacket(new ItemList(player, false));
		}
		else if (parameters.startsWith("RemoveList"))
		{
			showRemoveChat(player);
		}
		else if (parameters.startsWith("Remove "))
		{
			if (!player.getClient().getFloodProtectors().getTransaction().tryPerformAction("HennaRemove"))
			{
				return;
			}
			
			final int slot = Integer.parseInt(parameters.substring(7));
			player.removeHenna(slot);
			
			player.sendPacket(new ItemList(player, false));
			
		}
		else if (parameters.startsWith("Augment"))
		{
			final int cmdChoice = Integer.parseInt(parameters.substring(8, 9).trim());
			switch (cmdChoice)
			{
				case 1:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED));
					player.sendPacket(new ExShowVariationMakeWindow());
					break;
				case 2:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION));
					player.sendPacket(new ExShowVariationCancelWindow());
					break;
			}
		}
		else if (parameters.startsWith("settitlecolor"))
		{
			free = Config.RON_CUSTOM && player.getPremiumService() >= 1;
			
			if (!free)
			{
				if (item == null || item.getCount() < Config.PREM_TITLE_COLOR)
				{
					player.sendMessage("You don't have enough " + currencyName + ".");
					player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					return;
				}
				
				player.destroyItem("Consume", item.getObjectId(), Config.PREM_TITLE_COLOR, null, true);
			}
			
			player.getAppearance().setTitleColor(Integer.decode("0x" + parameters.substring(13).trim()));
			player.sendMessage("Your title color has been changed.");
			player.broadcastUserInfo();
		}
		else if (parameters.startsWith("setcolor"))
		{
			free = Config.RON_CUSTOM && player.getPremiumService() >= 1;
			
			if (!free)
			{
				if (item == null || item.getCount() < Config.PREM_NAME_COLOR)
				{
					player.sendMessage("You don't have enough " + currencyName + ".");
					player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					return;
				}
				
				player.destroyItem("Consume", item.getObjectId(), Config.PREM_NAME_COLOR, null, true);
			}
			
			player.getAppearance().setNameColor(Integer.decode("0x" + parameters.substring(8).trim()));
			player.sendMessage("Your name color has been changed.");
			player.broadcastUserInfo();
		}
		else if (parameters.equals("addclanskills"))
		{
			if (player.getClan() == null)
			{
				player.sendMessage("You don't have a clan.");
				return;
			}
			
			if (item == null || item.getCount() < Config.PREM_CLAN_SKILLS)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_CLAN_SKILLS, null, true);
			
			addClanSkill(player, 370, 3); // Clan Vitality
			addClanSkill(player, 371, 3); // Clan Spirituality
			addClanSkill(player, 372, 3); // Clan Essence
			addClanSkill(player, 373, 3); // Clan Lifeblood
			addClanSkill(player, 374, 3); // Clan Morale
			addClanSkill(player, 375, 3); // Clan Clarity
			addClanSkill(player, 376, 3); // Clan Might
			addClanSkill(player, 377, 3); // Clan Aegis
			addClanSkill(player, 378, 3); // Clan Empowerment
			addClanSkill(player, 379, 3); // Clan Magic Protection
			addClanSkill(player, 380, 3); // Clan Guidance
			addClanSkill(player, 381, 3); // Clan Agility
			addClanSkill(player, 382, 3); // Clan Withstand-Attack
			addClanSkill(player, 383, 3); // Clan Shield Boost
			addClanSkill(player, 384, 3); // Clan Cyclonic Resistance
			addClanSkill(player, 385, 3); // Clan Magmatic Resistance
			addClanSkill(player, 386, 3); // Clan Fortitude
			addClanSkill(player, 387, 3); // Clan Freedom
			addClanSkill(player, 388, 3); // Clan Vigilance
			addClanSkill(player, 389, 3); // Clan March
			addClanSkill(player, 390, 3); // Clan Luck
			addClanSkill(player, 391, 1); // Clan Imperium
			
			player.sendMessage("All clan skills have been added.");
			for (L2PcInstance member : player.getClan().getOnlineMembers(""))
			{
				member.sendSkillList();
			}
		}
		else if (parameters.startsWith("addclanskillsbylevel"))
		{
			int chosenLevel = Integer.parseInt(parameters.substring(20).trim());
			
			int price = Config.PREM_CLAN_SKILLS;
			if (player.getClan() == null)
			{
				player.sendMessage("You don't have a clan.");
				return;
			}
			
			switch (chosenLevel)
			{
				case 6:
					price = 140;
					break;
				case 7:
					price = 250;
					break;
				case 8:
					price = Config.PREM_CLAN_SKILLS;
					break;
			}
			
			if (item == null || item.getCount() < price)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			if (player.destroyItem("Consume", item.getObjectId(), price, null, true))
			{
				switch (chosenLevel)
				{
					case 6:
						// lvl 5 skills
						addClanSkill(player, 391, 1); // Clan Imperium
						addClanSkill(player, 373, 1); // Clan Lifeblood
						addClanSkill(player, 379, 1); // Clan Magic Protection
						addClanSkill(player, 370, 1); // Clan Vitality
						// lvl 6 skills
						addClanSkill(player, 377, 1); // Clan Aegis
						addClanSkill(player, 376, 1); // Clan Might
						addClanSkill(player, 374, 1); // Clan Morale
						addClanSkill(player, 383, 1); // Clan Shield Boost
						addClanSkill(player, 371, 1); // Clan Spirituality
						break;
					case 7:
						// lvl 5 skills
						addClanSkill(player, 391, 1); // Clan Imperium
						addClanSkill(player, 373, 2); // Clan Lifeblood
						addClanSkill(player, 379, 2); // Clan Magic Protection
						addClanSkill(player, 370, 2); // Clan Vitality
						// lvl 6 skills
						addClanSkill(player, 377, 2); // Clan Aegis
						addClanSkill(player, 376, 2); // Clan Might
						addClanSkill(player, 374, 1); // Clan Morale
						addClanSkill(player, 383, 1); // Clan Shield Boost
						addClanSkill(player, 371, 2); // Clan Spirituality
						// lvl 7 skills
						addClanSkill(player, 384, 1); // Clan Cyclonic Resistance
						addClanSkill(player, 386, 1); // Clan Fortitude
						addClanSkill(player, 387, 1); // Clan Freedom
						addClanSkill(player, 380, 1); // Clan Guidance
						addClanSkill(player, 390, 1); // Clan Luck
						addClanSkill(player, 385, 1); // Clan Magmatic Resistance
						addClanSkill(player, 388, 1); // Clan Vigilance
						addClanSkill(player, 382, 1); // Clan Withstand-Attack
						break;
					case 8:
						addClanSkill(player, 370, 3); // Clan Vitality
						addClanSkill(player, 371, 3); // Clan Spirituality
						addClanSkill(player, 372, 3); // Clan Essence
						addClanSkill(player, 373, 3); // Clan Lifeblood
						addClanSkill(player, 374, 3); // Clan Morale
						addClanSkill(player, 375, 3); // Clan Clarity
						addClanSkill(player, 376, 3); // Clan Might
						addClanSkill(player, 377, 3); // Clan Aegis
						addClanSkill(player, 378, 3); // Clan Empowerment
						addClanSkill(player, 379, 3); // Clan Magic Protection
						addClanSkill(player, 380, 3); // Clan Guidance
						addClanSkill(player, 381, 3); // Clan Agility
						addClanSkill(player, 382, 3); // Clan Withstand-Attack
						addClanSkill(player, 383, 3); // Clan Shield Boost
						addClanSkill(player, 384, 3); // Clan Cyclonic Resistance
						addClanSkill(player, 385, 3); // Clan Magmatic Resistance
						addClanSkill(player, 386, 3); // Clan Fortitude
						addClanSkill(player, 387, 3); // Clan Freedom
						addClanSkill(player, 388, 3); // Clan Vigilance
						addClanSkill(player, 389, 3); // Clan March
						addClanSkill(player, 390, 3); // Clan Luck
						addClanSkill(player, 391, 1); // Clan Imperium
						break;
				}
			}
			
			player.sendMessage("Clan skills have been added.");
			for (L2PcInstance member : player.getClan().getOnlineMembers(""))
			{
				member.sendSkillList();
			}
		}
		else if (parameters.startsWith("setclanlevel"))
		{
			if (player.getClan() == null)
			{
				player.sendMessage("You don't have a clan.");
				return;
			}
			
			int chosenLevel = Integer.parseInt(parameters.substring(12).trim());
			int price = Config.PREM_CLAN_LEVEL;
			switch (chosenLevel)
			{
				case 6:
					price = 40;
					break;
				case 7:
					price = 80;
					break;
				case 8:
					price = Config.PREM_CLAN_LEVEL;
					break;
			}
			
			int level = player.getClan().getLevel();
			if (level == chosenLevel)
			{
				player.sendMessage("Your clan is already level: " + chosenLevel + ".");
				return;
			}
			
			if (item == null || item.getCount() < price)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), price, null, true);
			
			player.getClan().changeLevel(chosenLevel);
			player.sendMessage("Increased level up to " + chosenLevel + " for the clan: " + player.getClan().getName() + "");
		}
		else if (parameters.startsWith("rep"))
		{
			if (player.getClan() == null)
			{
				player.sendMessage("You don't have a clan.");
				return;
			}
			
			int points = player.getClan().getReputationScore();
			
			points = Integer.parseInt(parameters.substring(3).trim());
			
			if (item == null || item.getCount() < Config.PREM_CLAN_REP)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			L2Clan clan = player.getClan();
			
			if (clan.getLevel() < 5)
			{
				player.sendMessage("Your clan level must be higher than 4.");
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_CLAN_REP, null, true);
			
			clan.setReputationScore(clan.getReputationScore() + points, true);
			player.sendMessage("You " + (points > 0 ? "add " : "remove ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Current scores are " + clan.getReputationScore());
			player.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));
			return;
		}
		else if (parameters.startsWith("setPremiumWeek"))
		{
			if (item == null || item.getCount() < Config.PREM_WEEK)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			if (player.getPremiumService() >= 1)
			{
				player.sendMessage("You already have The Premium Account!");
			}
			else
			{
				player.destroyItem("Consume", item.getObjectId(), Config.PREM_WEEK, null, true);
				player.setPremiumService(1);
				updateDatabasePremium(player, 7 * 24L * 60L * 60L * 1000L);
				player.sendMessage("Congratulation! You are The Premium account now.");
				player.sendPacket(new ExShowScreenMessage("Congratulation! You are The Premium account now.", 4000, 0x07, false));
				PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
				player.sendPacket(playSound);
				
				if (Config.PREMIUM_NAME_COLOR_ENABLED && player.getPremiumService() >= 1)
				{
					player.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
				}
				
				if (Config.PREMIUM_BUFF_MULTIPLIER > 0)
				{
					player.restoreEffects();
				}
				
				player.broadcastUserInfo();
			}
		}
		else if (parameters.startsWith("setPremiumMonth"))
		{
			if (item == null || item.getCount() < Config.PREM_MONTH)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			if (player.getPremiumService() >= 1)
			{
				player.sendMessage("You already have The Premium Account!");
			}
			else
			{
				player.destroyItem("Consume", item.getObjectId(), Config.PREM_MONTH, null, true);
				player.setPremiumService(1);
				updateDatabasePremium(player, 30 * 24L * 60L * 60L * 1000L);
				player.sendMessage("Congratulation! You are The Premium account now.");
				player.sendPacket(new ExShowScreenMessage("Congratulation! You are The Premium account now.", 4000, 0x07, false));
				PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
				player.sendPacket(playSound);
				
				if (Config.PREMIUM_NAME_COLOR_ENABLED && player.getPremiumService() >= 1)
				{
					player.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
				}
				
				if (Config.PREMIUM_BUFF_MULTIPLIER > 0)
				{
					player.restoreEffects();
				}
				
				player.broadcastUserInfo();
			}
		}
		else if (parameters.startsWith("nokarma"))
		{
			if (player.getKarma() == 0)
			{
				player.sendMessage("You don't have karma!");
				return;
			}
			
			if (player.isCursedWeaponEquiped())
			{
				player.sendMessage("You can not use this command at the momment!");
				return;
			}
			
			if (item == null || item.getCount() < Config.PREM_NOKARMA)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_NOKARMA, null, true);
			setTargetKarma(player, 0);
			player.sendMessage("Karma has been cleaned.");
		}
		else if (parameters.startsWith("clear_pk_count"))
		{
			if (item == null || item.getCount() < 60)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), 60, null, true);
			
			player.setPkKills(0);
			player.sendMessage("Your PK points cleared.");
		}
		else if (parameters.startsWith("premium_set"))
		{
			String cmd = st.nextToken();
			int days = 0;
			int price = 0;
			
			days = Integer.parseInt(st.nextToken());
			price = Integer.parseInt(st.nextToken());
			
			if (cmd != null)
			{
				if (item == null || item.getCount() < price)
				{
					player.sendMessage("You don't have enough " + currencyName + ".");
					player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					return;
				}
				
				if (player.getPremiumService() >= 1)
				{
					player.sendMessage("You already have the Premium Account.");
				}
				else
				{
					player.destroyItem("Consume", item.getObjectId(), price, null, true);
					player.setPremiumService(1);
					
					updateDatabasePremium(player, days * 24L * 60L * 60L * 1000L);
					
					player.sendMessage("Congratulation! You are the Premium Account now.");
					player.sendPacket(new ExShowScreenMessage("Congratulation! You are the Premium Account now.", 4000, 0x02, false));
					PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
					player.sendPacket(playSound);
					
					if (Config.PREMIUM_NAME_COLOR_ENABLED && player.getPremiumService() >= 1)
					{
						player.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
					}
					
					if (Config.PREMIUM_BUFF_MULTIPLIER > 0)
					{
						player.restoreEffects();
					}
					
					player.broadcastUserInfo();
				}
			}
			return;
		}
		else if (parameters.startsWith("delevel"))
		{
			String delevelNumber = parameters.substring(7).trim();
			
			int playerLvl = player.getLevel();
			int lvl1 = playerLvl - Integer.parseInt(delevelNumber);
			String addLvl = String.valueOf(lvl1);
			byte lvl = Byte.parseByte(addLvl);
			
			// if (player.isSubClassActive())
			// {
			// player.sendMessage("You can't do de-level on sub-class.");
			// return;
			// }
			
			if (Integer.parseInt(delevelNumber) < 1)
			{
				return;
			}
			
			if ((playerLvl - Integer.parseInt(delevelNumber)) > 1)
			{
				if (item == null || item.getCount() < (1 * Integer.parseInt(delevelNumber)))
				{
					player.sendMessage("You don't have enough " + currencyName + ".");
					player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					return;
				}
				
				player.destroyItem("Consume", item.getObjectId(), 1 * Integer.parseInt(delevelNumber), null, true);
				
				long lostExp = 0;
				// final byte maxLvl = ExperienceData.getInstance().getMaxLevel();
				final long pXp = player.getStat().getExp();
				final long tXp = ExperienceData.getInstance().getExpForLevel(lvl);
				
				if (pXp > tXp)
				{
					// level down
					player.getStat().removeExpAndSp(pXp - tXp, 0);
				}
				else if (pXp < tXp)
				{
					// level up
					player.getStat().addExpAndSp(tXp - pXp, 0);
				}
				
				player.getStat().addExp(-lostExp);
				
				player.sendMessage("Your level has been changed to " + player.getLevel() + ".");
				player.refreshOverloaded();
				player.refreshExpertisePenalty();
				player.refreshMasteryPenality();
				player.refreshMasteryWeapPenality();
				player.sendPacket(new EtcStatusUpdate(player));
				player.sendPacket(new UserInfo(player));
				return;
			}
			
			player.sendMessage("Your number: " + delevelNumber + " is too big.");
		}
		else if (parameters.startsWith("changeaccount"))
		{
			String account = parameters.substring(13).trim();
			
			if (account.length() < 3 || account.length() > 16 || !Util.isAlphaNumeric(account) || !isValidName(account))
			{
				player.sendMessage("This account name " + account + " is invalid.");
				return;
			}
			
			if (item == null || player.getInventory().getItemByItemId(currency).getCount() < Config.CHANGEACCOUNT)
			{
				player.sendMessage("You don't have enough " + L2Item.getItemNameById(Config.CUSTOM_ITEM_ID) + ".");
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.CHANGEACCOUNT, null, true);
			
			player.setAccountName(account);
			player.store();
			
			player.sendMessage("Your character has been transferred to account: " + account);
			player.sendMessage("You can logout now.");
			player.broadcastUserInfo();
		}
		else if (parameters.startsWith("changename"))
		{
			String name = parameters.substring(10).trim();
			
			if (item == null || item.getCount() < 10)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			if (name.length() < 3 || name.length() > 16 || !Util.isAlphaNumeric(name) || !isValidName(name))
			{
				player.sendMessage("This name: " + name + " is invalid.");
				return;
			}
			
			if (CharNameTable.getInstance().doesCharNameExist(name))
			{
				player.sendMessage("This name " + name + " already exists.");
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), 10, null, true);
			
			L2World.getInstance().removeFromAllPlayers(player);
			player.setName(name);
			player.store();
			L2World.getInstance().addPlayerToWorld(player);
			
			if (player.isInParty())
			{
				// Delete party window for other party members
				player.getParty().broadcastToPartyMembers(player, new PartySmallWindowDeleteAll());
				for (final L2PcInstance member : player.getParty().getPartyMembers())
				{
					// And re-add
					if (member != player)
					{
						member.sendPacket(new PartySmallWindowAll(player, player.getParty()));
					}
				}
			}
			
			if (player.getClan() != null)
			{
				player.getClan().updateClanMember(player);
				player.getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(player));
				player.sendPacket(new PledgeShowMemberListAll(player.getClan(), player));
			}
			
			player.sendMessage("Your name has been changed.");
			player.broadcastUserInfo();
		}
		else if (parameters.startsWith("changeclanname"))
		{
			String name = parameters.substring(14).trim();
			
			if (player.getClan() == null)
			{
				player.sendMessage("You don't have a clan.");
				return;
			}
			
			String oldName = player.getClan().getName();
			
			if (name == null || name.length() == 0)
			{
				player.sendMessage("Please enter a clan name.");
				return;
			}
			
			if (item == null || item.getCount() < 85)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), 85, null, true);
			
			L2Clan clan = ClanTable.getInstance().renameClan(player, name);
			if (clan != null)
			{
				player.sendMessage("Clan " + oldName + " renamed to: " + name);
			}
		}
		else if (parameters.startsWith("Remove "))
		{
			if (!player.getClient().getFloodProtectors().getTransaction().tryPerformAction("HennaRemove"))
			{
				return;
			}
			
			final int slot = Integer.parseInt(command.substring(7));
			player.removeHenna(slot);
			
			player.sendPacket(new ItemList(player, false));
		}
		else if (parameters.startsWith("EnchantSkillList"))
		{
			player.setTempAccess(true);
			showEnchantSkillList(player);
		}
		else if (parameters.startsWith("item_enchanter_index"))
		{
			enchantIndex(player);
		}
		else if (parameters.startsWith("item_enchanter_page"))
		{
			loadItemsForEnchanting(player);
		}
		else if (parameters.startsWith("item_enchanter"))
		{
			String param2 = parameters.substring(15);
			
			enchantItem(player, Integer.valueOf(param2));
		}
		else if (parameters.startsWith("Link"))
		{
			final String path = parameters.substring(5).trim();
			
			if (path.indexOf("..") != -1)
			{
				return;
			}
			
			String filename = "/data/html/" + path;
			NpcHtmlMessage html = new NpcHtmlMessage(player.getLastQuestNpcObject());
			html.setFile(filename);
			player.sendPacket(html);
		}
	}
	
	private void updateDatabasePremium(L2PcInstance player, long premiumTime)
	{
		Connection con = null;
		try
		{
			if (player == null)
			{
				return;
			}
			
			player.setPremiumExpire(System.currentTimeMillis() + premiumTime);
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement("REPLACE INTO account_premium (account_name, premium_service, enddate) VALUES (?,?,?)");
			stmt.setString(1, player.getAccountName());
			stmt.setInt(2, 1);
			stmt.setLong(3, premiumTime == 0 ? 0 : System.currentTimeMillis() + premiumTime);
			stmt.execute();
			stmt.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("[MerchantInstance] Error: could not update database: ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void showRemoveChat(final L2PcInstance player)
	{
		TextBuilder html1 = new TextBuilder("<html><body>");
		html1.append("Select symbol you would like to remove:<br><br>");
		boolean hasHennas = false;
		
		for (int i = 1; i <= 3; i++)
		{
			final L2HennaInstance henna = player.getHennas(i);
			
			if (henna != null)
			{
				hasHennas = true;
				html1.append("<a action=\"bypass -h custom_gmshop Remove " + i + "\">" + henna.getName() + "</a><br>");
			}
		}
		
		if (!hasHennas)
		{
			html1.append("You don't have any symbol to remove!");
		}
		
		html1.append("</body></html>");
		insertObjectIdAndShowChatWindow(player, html1.toString());
	}
	
	private void showBuyWindow(L2PcInstance player, int val)
	{
		double taxRate = 0;
		player.tempInvetoryDisable();
		
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		
		if (list != null && list.getNpcId().equals(String.valueOf("shop")))
		{
			BuyList bl = new BuyList(list, player.getAdena(), taxRate);
			player.sendPacket(bl);
		}
		else
		{
			player.sendMessage("This list does not exist");
			LOG.warn("possible client hacker: " + player.getName() + " attempting to buy from GM shop! (GMShop) buylist id:" + val);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void addClanSkill(final L2PcInstance activeChar, final int id, final int level)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
		if (skill != null)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED);
			sm.addSkillName(id);
			activeChar.sendPacket(sm);
			activeChar.getClan().broadcastToOnlineMembers(sm);
			activeChar.getClan().addNewSkill(skill);
			activeChar.getClan().broadcastToOnlineMembers(new PledgeSkillList(activeChar.getClan()));
		}
	}
	
	public void insertObjectIdAndShowChatWindow(final L2PcInstance player, String content)
	{
		NpcHtmlMessage npcReply = new NpcHtmlMessage(1);
		npcReply.setHtml(content);
		player.sendPacket(npcReply);
	}
	
	private void setTargetKarma(final L2PcInstance player, final int newKarma)
	{
		player.setKarma(newKarma);
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.KARMA, newKarma);
		player.sendPacket(su);
	}
	
	private void showSellWindow(final L2PcInstance player)
	{
		player.sendPacket(new SellList(player));
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void showDepositWindowFreight(final L2PcInstance player, final int obj_Id)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		L2PcInstance destChar = L2PcInstance.load(obj_Id);
		if (destChar == null)
		{
			LOG.warn("Error retrieving a target object for char " + player.getName() + " - using freight");
			return;
		}
		
		PcFreight freight = destChar.getFreight();
		freight.setActiveLocation(0);
		player.setActiveWarehouse(freight);
		player.tempInvetoryDisable();
		destChar.deleteMe();
		
		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.FREIGHT));
	}
	
	public void showPledgeSkillList(L2PcInstance player)
	{
		if (player.getClan() == null)
		{
			return;
		}
		
		L2PledgeSkillLearn[] skills = SkillTreeTable.getInstance().getAvailablePledgeSkills(player);
		AquireSkillList asl = new AquireSkillList(AquireSkillList.skillType.Clan);
		int counts = 0;
		
		for (L2PledgeSkillLearn s : skills)
		{
			int cost = s.getRepCost();
			counts++;
			
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}
		
		if (counts == 0)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			
			if (player.getClan().getLevel() < 8)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN);
				sm.addNumber(player.getClan().getLevel() + 1);
				player.sendPacket(sm);
			}
			else
			{
				TextBuilder sb = new TextBuilder();
				sb.append("<html><body>");
				sb.append("You've learned all skills available for your Clan.<br>");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
			}
		}
		else
		{
			player.sendPacket(asl);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private boolean isValidName(final String text)
	{
		boolean result = true;
		final String test = text;
		Pattern pattern;
		
		try
		{
			pattern = Pattern.compile(Config.CNAME_TEMPLATE);
		}
		catch (final PatternSyntaxException e) // case of illegal pattern
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("Shop Character name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		
		final Matcher regexp = pattern.matcher(test);
		if (!regexp.matches())
		{
			result = false;
		}
		
		return result;
	}
	
	@Override
	public String[] getBypassBbsCommands()
	{
		return new String[]
		{
			"bbsgmshop"
		};
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"gmshop"
		};
	}
	
	public void returnHtm(String command, String param, L2PcInstance player)
	{
		if (_visitedPages.get(player.getObjectId()) != null)
		{
			handleCommand(command, player, param + " " + _visitedPages.get(player.getObjectId()));
		}
	}
	
	public void countdown(L2PcInstance player)
	{
		try
		{
			_secondsDc = 5;
			while (_secondsDc > 0)
			{
				
				int _seconds;
				
				_seconds = _secondsDc;
				
				// announce only every minute after 10 minutes left and every second after 20 seconds
				if (_seconds <= 5)
				{
					player.sendMessage("You will be disconnected in: " + _seconds);
				}
				
				_secondsDc--;
				
				int delay = 1000; // milliseconds
				Thread.sleep(delay);
			}
		}
		catch (InterruptedException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void enchantIndex(L2PcInstance player)
	{
		String filename = "start.htm";
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(path + filename);
		player.sendPacket(html);
	}
	
	public void loadItemsForEnchanting(L2PcInstance player)
	{
		sendEnchantHtml(player);
	}
	
	public void enchantItem(L2PcInstance player, int objid)
	{
		if (player.getInventory().getInventoryItemCount(PowerPackConfig.GMSHOP_ENCHNANT_ITEM_ID) < PowerPackConfig.GMSHOP_ENCHNANT_ITEM_ID_COUNT)
		{
			player.sendMessage("You don't have enough required items.");
		}
		else
		{
			L2ItemInstance itemEnchant = player.getInventory().getItemByObjectId(objid);
			itemEnchant.setEnchantLevel(itemEnchant.getEnchantLevel() + 1);
			player.destroyItemByItemId("ItemEnchanter", PowerPackConfig.GMSHOP_ENCHNANT_ITEM_ID, PowerPackConfig.GMSHOP_ENCHNANT_ITEM_ID_COUNT, null, true);
			
			SystemMessage sm;
			String msg;
			if (itemEnchant.getEnchantLevel() == 0)
			{
				sm = new SystemMessage(SystemMessageId.S1_SUCCESSFULLY_ENCHANTED);
				sm.addItemName(itemEnchant.getItemId());
				player.sendPacket(sm);
				msg = "Your +" + itemEnchant.getItemName() + " has been successfully enchanted.";
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED);
				sm.addNumber(itemEnchant.getEnchantLevel());
				sm.addItemName(itemEnchant.getItemId());
				player.sendPacket(sm);
				msg = "Your +" + itemEnchant.getEnchantLevel() + " " + itemEnchant.getItemName() + " has been successfully enchanted.";
			}
			
			player.sendPacket(new ExShowScreenMessage(msg, 2000, 2, false));
			
			StatusUpdate su = new StatusUpdate(player.getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
			player.sendPacket(su);
			
			player.sendPacket(new EnchantResult(itemEnchant.getEnchantLevel()));
			player.sendPacket(new ItemList(player, false));
			player.broadcastUserInfo();
			
			sendEnchantHtml(player);
		}
	}
	
	public void getItems(L2PcInstance player)
	{
		for (L2ItemInstance item : player.getInventory().getItems())
		{
			if (item != null && item.isEquipped())
			{
				if (item.getEnchantLevel() < PowerPackConfig.GMSHOP_ENCHNANT_MAX && item.getItem().getCrystalType() != L2Item.CRYSTAL_NONE && !item.isHeroItem())
				{
					list.add(item);
				}
			}
		}
	}
	
	public void sendEnchantHtml(L2PcInstance player)
	{
		list = new ArrayList<>();
		
		String text = "";
		String bgcolor = "";
		boolean flip = true;
		
		getItems(player);
		
		for (L2ItemInstance i : list)
		{
			flip = !flip;
			bgcolor = "";
			
			final int itemId = i.getItemId();
			String name = i.getItemName();
			String Action = "custom_gmshop item_enchanter " + i.getObjectId();
			
			String enchantLevel = String.valueOf(i.getEnchantLevel() + 1);
			
			if (flip == true)
			{
				bgcolor = "bgcolor=000000";
			}
			
			text += "<table " + bgcolor + "><tr><td width=40><button action=\"bypass -h " + Action + "\" width=32 height=32 back=" + L2Item.getItemIcon(itemId) + " fore=" + L2Item.getItemIcon(itemId) + "></td><td width=220><table width=300><tr><td><font color=799BB0>+" + enchantLevel + " " + name
				+ "</font></td><td></td></tr><tr><td><font color=B09B79> " + PowerPackConfig.GMSHOP_ENCHNANT_ITEM_ID_COUNT + " " + L2Item.getItemNameById(PowerPackConfig.GMSHOP_ENCHNANT_ITEM_ID) + "</font></td></tr></table></td></tr></table>";
			
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(path + filename);
		html.replace("%list%", text);
		player.sendPacket(html);
	}
	
	public static void showEnchantSkillList(L2PcInstance player)
	{
		if (player.getClassId().getId() < 88)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(0);
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("You must have 3rd class change quest completed.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return;
		}
		
		L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(player);
		ExEnchantSkillList esl = new ExEnchantSkillList();
		int counts = 0;
		
		for (final L2EnchantSkillLearn s : skills)
		{
			final L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if (sk == null)
			{
				continue;
			}
			
			counts++;
			
			esl.addSkill(s.getId(), s.getLevel(), s.getSpCost(), s.getExp());
		}
		
		if (counts == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT));
			NpcHtmlMessage html = new NpcHtmlMessage(0);
			final int level = player.getLevel();
			
			if (level < 74)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN);
				sm.addNumber(level);
				player.sendPacket(sm);
			}
			else
			{
				TextBuilder sb = new TextBuilder();
				sb.append("<html><body>");
				sb.append("You've learned all skills for your class.<br>");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
			}
		}
		else
		{
			player.sendPacket(esl);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}

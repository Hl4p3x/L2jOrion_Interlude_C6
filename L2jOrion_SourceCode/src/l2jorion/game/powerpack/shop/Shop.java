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
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.controllers.TradeController;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.CharNameTable;
import l2jorion.game.datatables.sql.HennaTreeTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.handler.IBBSHandler;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.L2Clan;
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
import l2jorion.game.network.serverpackets.PledgeShowMemberListAll;
import l2jorion.game.network.serverpackets.PledgeShowMemberListUpdate;
import l2jorion.game.network.serverpackets.PledgeSkillList;
import l2jorion.game.network.serverpackets.SellList;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
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

public class Shop implements IVoicedCommandHandler, ICustomByPassHandler, IBBSHandler
{
	private static Logger LOG = LoggerFactory.getLogger(Shop.class);
	
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
		
		if (activeChar.isSitting())
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
		
		if (command.compareTo(PowerPackConfig.GMSHOP_COMMAND) == 0)
		{
			String index = "";
			if (params != null && params.length() != 0)
			{
				if (!params.equals("0"))
				{
					index = "-" + params;
				}
			}
			
			String text = HtmCache.getInstance().getHtm("data/html/gmshop/gmshop" + index + ".htm");
			NpcHtmlMessage htm = new NpcHtmlMessage(activeChar.getLastQuestNpcObject());
			htm.setHtml(text);
			activeChar.sendPacket(htm);
		}
		
		return false;
	}
	
	private static String[] _CMD =
	{
		"gmshop"
	};
	
	@Override
	public String[] getByPassCommands()
	{
		return _CMD;
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
		String currencyName = getItemNameById(currency);
		
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
		
		if (parameters.startsWith("multisell"))
		{
			try
			{
				int listId = Integer.parseInt(parameters.substring(9).trim());
				MultiSellListContainer list = L2Multisell.getInstance().getList(listId);
				
				if (list != null && list.getNpcId() != null && list.getNpcId().equals(String.valueOf("shop")))
				{
					player.setTempAccessBuy(true);
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
		else if (parameters.startsWith("Buy"))
		{
			player.setTempAccessBuy(true);
			showBuyWindow(player, Integer.parseInt(parameters.substring(3).trim()));
		}
		else if (parameters.startsWith("exc_multisell"))
		{
			try
			{
				int listId = Integer.parseInt(parameters.substring(13).trim());
				player.setTempAccessBuy(true);
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
		// TODO STARTS CUSTOM COMMAND
		else if (parameters.startsWith("settitlecolor"))
		{
			if (item == null || item.getCount() < Config.PREM_TITLE_COLOR)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_TITLE_COLOR, null, true);
			player.getAppearance().setTitleColor(Integer.decode("0x" + parameters.substring(13).trim()));
			player.sendMessage("Your title color has been changed.");
			player.broadcastUserInfo();
		}
		else if (parameters.startsWith("setcolor"))
		{
			if (item == null || item.getCount() < Config.PREM_NAME_COLOR)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_NAME_COLOR, null, true);
			player.getAppearance().setNameColor(Integer.decode("0x" + parameters.substring(8).trim()));
			player.sendMessage("Your name color has been changed.");
			player.broadcastUserInfo();
		}
		else if (parameters.startsWith("addclanskills"))
		{
			if (player.getClan() == null)
			{
				player.sendMessage("You don't have clan.");
				return;
			}
			int level = player.getClan().getLevel();
			if (level == 4)
			{
				player.sendMessage("Your clan must be level: 8.");
				return;
			}
			
			if (item == null || item.getCount() < Config.PREM_CLAN_SKILLS)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_CLAN_SKILLS, null, true);
			
			addskill(player, 370, 3);
			addskill(player, 371, 3);
			addskill(player, 372, 3);
			addskill(player, 373, 3);
			addskill(player, 374, 3);
			addskill(player, 375, 3);
			addskill(player, 376, 3);
			addskill(player, 377, 3);
			addskill(player, 378, 3);
			addskill(player, 379, 3);
			addskill(player, 380, 3);
			addskill(player, 381, 3);
			addskill(player, 382, 3);
			addskill(player, 383, 3);
			addskill(player, 384, 3);
			addskill(player, 385, 3);
			addskill(player, 386, 3);
			addskill(player, 387, 3);
			addskill(player, 388, 3);
			addskill(player, 389, 3);
			addskill(player, 390, 3);
			addskill(player, 391, 1);
			player.sendMessage("All clan skills have been added.");
			
			player.getInventory().reloadEquippedItems();
		}
		else if (parameters.startsWith("setclanlevel"))
		{
			if (player.getClan() == null)
			{
				player.sendMessage("You don't have clan.");
				return;
			}
			
			int level = player.getClan().getLevel();
			if (level >= 8)
			{
				player.sendMessage("Your clan is already level: 8.");
				return;
			}
			
			level = Integer.parseInt(parameters.substring(12).trim());
			
			if (item == null || item.getCount() < Config.PREM_CLAN_LEVEL)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_CLAN_LEVEL, null, true);
			if (level >= 0 && level < 9)
			{
				player.getClan().changeLevel(level);
				player.sendMessage("Increased level up to " + level + " for the clan: " + player.getClan().getName() + "");
				return;
			}
		}
		else if (parameters.startsWith("rep"))
		{
			if (player.getClan() == null)
			{
				player.sendMessage("You don't have clan.");
				return;
			}
			
			int points = player.getClan().getReputationScore();
			
			points = Integer.parseInt(parameters.substring(3).trim());
			
			// if (points > 10000)
			// {
			// return;
			// }
			
			if (item == null || item.getCount() < Config.PREM_CLAN_REP)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_CLAN_REP, null, true);
			L2Clan clan = player.getClan();
			
			if (clan.getLevel() < 5)
			{
				player.sendMessage("Your clan level must be higher than 4.");
				return;
			}
			
			clan.setReputationScore(clan.getReputationScore() + points, true);
			player.sendMessage("You " + (points > 0 ? "add " : "remove ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Current scores are " + clan.getReputationScore());
			return;
		}
		else if (parameters.startsWith("setPremiumWeek"))
		{
			if (item == null || item.getCount() < Config.PREM_WEEK)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			if (player.getPremiumService() == 1)
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
				
				if (Config.PREMIUM_NAME_COLOR_ENABLED && player.getPremiumService() == 1)
				{
					player.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
				}
				
				player.broadcastUserInfo();
			}
		}
		else if (parameters.startsWith("setPremiumMonth"))
		{
			if (item == null || item.getCount() < Config.PREM_MONTH)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			if (player.getPremiumService() == 1)
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
				
				if (Config.PREMIUM_NAME_COLOR_ENABLED && player.getPremiumService() == 1)
				{
					player.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
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
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_NOKARMA, null, true);
			setTargetKarma(player, 0);
			player.sendMessage("Karma has been cleaned.");
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
					player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 1000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					return;
				}
				
				if (player.getPremiumService() == 1)
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
					
					if (Config.PREMIUM_NAME_COLOR_ENABLED && player.getPremiumService() == 1)
					{
						player.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
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
			int lvl1 = playerLvl + Integer.parseInt("1") - Integer.parseInt(delevelNumber);
			String addLvl = "" + lvl1;
			byte lvl = Byte.parseByte(addLvl);
			
			if (player.isSubClassActive())
			{
				player.sendMessage("You can't do de-level on sub-class.");
				return;
			}
			
			if (Integer.parseInt(delevelNumber) < 1)
			{
				return;
			}
			
			if ((playerLvl - Integer.parseInt(delevelNumber)) > 1)
			{
				if (item == null || item.getCount() < (1 * Integer.parseInt(delevelNumber)))
				{
					player.sendMessage("You don't have enough " + currencyName + ".");
					player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 1000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					return;
				}
				
				player.destroyItem("Consume", item.getObjectId(), 1 * Integer.parseInt(delevelNumber), null, true);
				
				long lostExp = 0;
				final byte maxLvl = ExperienceData.getInstance().getMaxLevel();
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
				
				if (playerLvl < maxLvl)
				{
					lostExp = Math.round((player.getStat().getExpForLevel(playerLvl + 1) - player.getStat().getExpForLevel(playerLvl)) * 1 / 100);
				}
				else
				{
					lostExp = Math.round((player.getStat().getExpForLevel(maxLvl) - player.getStat().getExpForLevel(maxLvl - 1)) * 1 / 100);
				}
				
				player.getStat().addExp(-lostExp);
				
				player.sendMessage("Your level has been changed to " + player.getLevel() + ".");
				return;
			}
			
			player.sendMessage("Your number: " + delevelNumber + " is too big.");
		}
		else if (parameters.startsWith("changename"))
		{
			String name = parameters.substring(10).trim();
			
			if (item == null || item.getCount() < 10)
			{
				player.sendMessage("You don't have enough " + currencyName + ".");
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + currencyName + ".", 1000, 2, false));
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
		// TODO custom configs ends
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
		else if (parameters.startsWith("Chat"))
		{
			String chatIndex = parameters.substring(4).trim();
			
			chatIndex = "-" + chatIndex;
			
			if (chatIndex.equals("-0"))
			{
				chatIndex = "";
			}
			
			String text = HtmCache.getInstance().getHtm("data/html/gmshop/gmshop" + chatIndex + ".htm");
			
			NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
			htm.setHtml(text);
			player.sendPacket(htm);
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
	
	private void addskill(final L2PcInstance activeChar, final int id, final int level)
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
			
			for (final L2PcInstance member : activeChar.getClan().getOnlineMembers(""))
			{
				member.sendSkillList();
			}
			return;
		}
		
		activeChar.sendMessage("Error: there is no such skill.");
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
			// Something went wrong!
			if (Config.DEBUG)
			{
				LOG.warn("Error retrieving a target object for char " + player.getName() + " - using freight.");
			}
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
				html = null;
				sb = null;
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
	
	public String getItemNameById(int itemId)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		
		String itemName = "NoName";
		
		if (itemId != 0)
		{
			itemName = item.getName();
		}
		
		return itemName;
	}
	
	@Override
	public String[] getBBSCommands()
	{
		return _CMD;
	}
}

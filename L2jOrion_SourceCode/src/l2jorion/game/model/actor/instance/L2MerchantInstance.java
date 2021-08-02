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
package l2jorion.game.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.controllers.TradeController;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.HennaTreeTable;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2PledgeSkillLearn;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2TradeList;
import l2jorion.game.model.PcFreight;
import l2jorion.game.model.multisell.L2Multisell;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.AquireSkillList;
import l2jorion.game.network.serverpackets.BuyList;
import l2jorion.game.network.serverpackets.EnchantResult;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.HennaEquipList;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PackageToList;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.PledgeSkillList;
import l2jorion.game.network.serverpackets.Ride;
import l2jorion.game.network.serverpackets.SellList;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.game.network.serverpackets.WareHouseDepositList;
import l2jorion.game.network.serverpackets.WareHouseWithdrawalList;
import l2jorion.game.network.serverpackets.WearList;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class L2MerchantInstance extends L2FolkInstance
{
	private static Logger LOG = LoggerFactory.getLogger(L2MerchantInstance.class);
	
	public L2MerchantInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public String getHtmlPath(L2PcInstance player, final int npcId, final int val)
	{
		String pom = "";
		
		if (npcId == PowerPackConfig.GMSHOP_NPC)
		{
			if (val == 0)
			{
				pom = "gmshop";
			}
			else
			{
				pom = "gmshop" + "-" + val;
			}
			
			if (!PowerPackConfig.GMSHOP_ENABLED)
			{
				return "data/html/disabled.htm";
			}
			
			return "data/html/gmshop/" + pom + ".htm";
		}
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/merchant/" + pom + ".htm";
	}
	
	private void showWearWindow(final L2PcInstance player, final int val)
	{
		player.tempInvetoryDisable();
		
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		
		if (list != null)
		{
			WearList bl = new WearList(list, player.getAdena(), player.getExpertiseIndex());
			player.sendPacket(bl);
		}
		else
		{
			LOG.warn("no buylist with id:" + val);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Show buy window.
	 * @param player the player
	 * @param val the val
	 */
	private void showBuyWindow(final L2PcInstance player, final int val)
	{
		double taxRate = 0;
		
		if (getIsInTown())
		{
			taxRate = getCastle().getTaxRate();
		}
		
		player.tempInvetoryDisable();
		
		if (Config.DEBUG)
		{
			LOG.debug("Showing buylist");
		}
		
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		
		if (list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
		{
			BuyList bl = new BuyList(list, player.getAdena(), taxRate);
			player.sendPacket(bl);
		}
		else
		{
			LOG.warn("possible client hacker: " + player.getName() + " attempting to buy from GM shop! (L2MechantInstance)");
			LOG.warn("buylist id:" + val);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Show sell window.
	 * @param player the player
	 */
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
		if (Config.ALT_GAME_FREIGHTS)
		{
			freight.setActiveLocation(0);
		}
		else
		{
			freight.setActiveLocation(getWorldRegion().hashCode());
		}
		player.setActiveWarehouse(freight);
		player.tempInvetoryDisable();
		destChar.deleteMe();
		
		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.FREIGHT));
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		if (player.getActiveEnchantItem() != null)
		{
			player.sendPacket(new EnchantResult(0));
		}
		
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command
		final int currency = Config.CUSTOM_ITEM_ID;
		final L2ItemInstance item = player.getInventory().getItemByItemId(currency);
		
		if (actualCommand.equalsIgnoreCase("Buy"))
		{
			if (st.countTokens() < 1)
			{
				return;
			}
			
			player.setTempAccessBuy(false);
			
			final int val = Integer.parseInt(st.nextToken());
			showBuyWindow(player, val);
		}
		else if (actualCommand.equalsIgnoreCase("Sell"))
		{
			showSellWindow(player);
		}
		else if (actualCommand.equalsIgnoreCase("RentPet"))
		{
			if (Config.ALLOW_RENTPET)
			{
				if (st.countTokens() < 1)
				{
					showRentPetWindow(player);
				}
				else
				{
					final int val = Integer.parseInt(st.nextToken());
					tryRentPet(player, val);
				}
			}
		}
		else if (actualCommand.equalsIgnoreCase("Wear") && Config.ALLOW_WEAR)
		{
			if (st.countTokens() < 1)
			{
				return;
			}
			
			final int val = Integer.parseInt(st.nextToken());
			showWearWindow(player, val);
		}
		else if (actualCommand.equalsIgnoreCase("Multisell"))
		{
			if (st.countTokens() < 1)
			{
				return;
			}
			
			player.setTempAccessBuy(false);
			
			final int val = Integer.parseInt(st.nextToken());
			L2Multisell.getInstance().SeparateAndSend(val, player, false, getCastle().getTaxRate());
		}
		else if (actualCommand.equalsIgnoreCase("Exc_Multisell"))
		{
			if (st.countTokens() < 1)
			{
				return;
			}
			
			player.setTempAccessBuy(false);
			
			final int val = Integer.parseInt(st.nextToken());
			L2Multisell.getInstance().SeparateAndSend(val, player, true, getCastle().getTaxRate());
		}
		else if (command.startsWith("WithdrawP"))
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
		else if (command.equals("DepositP"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.setActiveWarehouse(player.getWarehouse());
			player.tempInvetoryDisable();
			player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.PRIVATE));
		}
		else if (command.equals("WithdrawC"))
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
		else if (command.equals("DepositC"))
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
		else if (command.startsWith("WithdrawF"))
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
						if (Config.ALT_GAME_FREIGHTS)
						{
							freight.setActiveLocation(0);
						}
						else
						{
							freight.setActiveLocation(getWorldRegion().hashCode());
						}
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
		else if (command.startsWith("DepositF"))
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
		else if (command.startsWith("FreightChar"))
		{
			if (Config.ALLOW_FREIGHT)
			{
				final int startOfId = command.lastIndexOf("_") + 1;
				final String id = command.substring(startOfId);
				showDepositWindowFreight(player, Integer.parseInt(id));
			}
		}
		else if (command.equals("Draw"))
		{
			final L2HennaInstance[] henna = HennaTreeTable.getInstance().getAvailableHenna(player.getClassId());
			final HennaEquipList hel = new HennaEquipList(player, henna);
			player.sendPacket(hel);
			
			player.sendPacket(new ItemList(player, false));
		}
		else if (command.equals("RemoveList"))
		{
			showRemoveChat(player);
		}
		else if (actualCommand.equalsIgnoreCase("settitlecolor"))
		{
			String val = "";
			if (st.hasMoreTokens())
			{
				
				val = st.nextToken();
			}
			else
			{
				player.sendMessage("Something is wrong.");
			}
			if (item == null || player.getInventory().getItemByItemId(currency).getCount() < Config.PREM_TITLE_COLOR)
			{
				player.sendMessage("You don't have enough " + Config.ALT_SERVER_CUSTOM_ITEM_NAME + ".");
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_TITLE_COLOR, null, true);
			player.getAppearance().setTitleColor(Integer.decode("0x" + val));
			player.sendMessage("Your title color has been changed.");
			player.broadcastUserInfo();
			st = null;
		}
		else if (actualCommand.equalsIgnoreCase("setcolor"))
		{
			String val = "";
			if (st.hasMoreTokens())
			{
				
				val = st.nextToken();
			}
			else
			{
				player.sendMessage("Something is wrong.");
			}
			if (item == null || player.getInventory().getItemByItemId(currency).getCount() < Config.PREM_NAME_COLOR)
			{
				player.sendMessage("You don't have enough " + Config.ALT_SERVER_CUSTOM_ITEM_NAME + ".");
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_NAME_COLOR, null, true);
			player.getAppearance().setNameColor(Integer.decode("0x" + val));
			player.sendMessage("Your name color has been changed.");
			player.broadcastUserInfo();
			st = null;
		}
		else if (command.equals("addclanskills"))
		{
			if (player.getClan() == null)
			{
				player.sendMessage("You don't have clan.");
				return;
			}
			int level = player.getClan().getLevel();
			if (level <= 4)
			{
				player.sendMessage("You must have clan level 5 or higher.");
				return;
			}
			// Add clan skills
			if (item == null || player.getInventory().getItemByItemId(currency).getCount() < Config.PREM_CLAN_SKILLS)
			{
				player.sendMessage("You don't have enough " + Config.ALT_SERVER_CUSTOM_ITEM_NAME + ".");
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_CLAN_SKILLS, null, true);
			if (player.getClan() != null)
			{
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
				player.sendMessage("Clan skills have been added.");
			}
			// Reload passive skills from armors / jewels / weapons
			player.getInventory().reloadEquippedItems();
		}
		else if (actualCommand.equalsIgnoreCase("setclanlevel"))
		{
			if (player.getClan() == null)
			{
				player.sendMessage("You don't have clan.");
				return;
			}
			String parameter = null;
			if (st.hasMoreTokens())
			{
				parameter = st.nextToken(); // clanname|nothing|nothing|level|rep_points
			}
			
			int level = player.getClan().getLevel();
			if (level >= 8)
			{
				player.sendMessage("Your clan is already Lv 8.");
				return;
			}
			try
			{
				
				level = Integer.parseInt(parameter);
				
			}
			catch (final NumberFormatException nfe)
			{
				player.sendMessage("Level incorrect.");
			}
			
			if (item == null || player.getInventory().getItemByItemId(currency).getCount() < Config.PREM_CLAN_LEVEL)
			{
				player.sendMessage("You don't have enough " + Config.ALT_SERVER_CUSTOM_ITEM_NAME + ".");
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_CLAN_LEVEL, null, true);
			if (level >= 0 && level < 9)
			{
				
				player.getClan().changeLevel(level);
				player.sendMessage("You set level " + level + " for clan " + player.getClan().getName());
				return;
			}
		}
		else if (actualCommand.equalsIgnoreCase("rep"))
		{
			if (player.getClan() == null)
			{
				player.sendMessage("You don't have clan.");
				return;
			}
			String parameter = null;
			int points = player.getClan().getReputationScore();
			if (st.hasMoreTokens())
			{
				parameter = st.nextToken(); // clanname|nothing|nothing|level|rep_points
			}
			
			try
			{
				
				points = Integer.parseInt(parameter);
				
			}
			catch (final NumberFormatException nfe)
			{
				player.sendMessage("Level incorrect.");
			}
			
			if (item == null || player.getInventory().getItemByItemId(currency).getCount() < Config.PREM_CLAN_REP)
			{
				player.sendMessage("You don't have enough " + Config.ALT_SERVER_CUSTOM_ITEM_NAME + ".");
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_CLAN_REP, null, true);
			L2Clan clan = player.getClan();
			
			if (clan.getLevel() < 5)
			{
				player.sendMessage("Only clans of level 5 or above may receive reputation points.");
				return;
			}
			
			clan.setReputationScore(clan.getReputationScore() + points, true);
			player.sendMessage("You " + (points > 0 ? "add " : "remove ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Current scores are " + clan.getReputationScore());
			clan = null;
			return;
		}
		else if (command.equals("setsex"))
		{
			if (item == null || player.getInventory().getItemByItemId(currency).getCount() < Config.PREM_SET_SEX)
			{
				player.sendMessage("You don't have enough " + Config.ALT_SERVER_CUSTOM_ITEM_NAME + ".");
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_SET_SEX, null, true);
			player.getAppearance().setSex(player.getAppearance().getSex() ? false : true);
			L2PcInstance.setSexDB(player, 1);
			player.sendMessage("Your gender has been changed.");
			player.decayMe();
			player.spawnMe(player.getX(), player.getY(), player.getZ());
			player.broadcastUserInfo();
			return;
		}
		else if (command.equals("setPremiumWeek"))
		{
			if (item == null || player.getInventory().getItemByItemId(currency).getCount() < Config.PREM_WEEK)
			{
				player.sendMessage("You don't have enough " + Config.ALT_SERVER_CUSTOM_ITEM_NAME + ".");
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
				player.sendMessage("Congratulation! You're The Premium account now.");
				player.sendPacket(new ExShowScreenMessage("Congratulation! You're The Premium account now.", 4000, 0x07, false));
				PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
				player.sendPacket(playSound);
				if (Config.PREMIUM_NAME_COLOR_ENABLED && getPremiumService() == 1)
				{
					player.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
				}
				player.sendPacket(new UserInfo(player));
				player.broadcastUserInfo();
			}
		}
		else if (command.equals("setPremiumMonth"))
		{
			if (item == null || player.getInventory().getItemByItemId(currency).getCount() < Config.PREM_MONTH)
			{
				player.sendMessage("You don't have enough " + Config.ALT_SERVER_CUSTOM_ITEM_NAME + ".");
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
				if (Config.PREMIUM_NAME_COLOR_ENABLED && getPremiumService() == 1)
				{
					player.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
				}
				player.sendPacket(new UserInfo(player));
				player.broadcastUserInfo();
			}
		}
		else if (command.equals("nokarma"))
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
			if (item == null || player.getInventory().getItemByItemId(currency).getCount() < Config.PREM_NOKARMA)
			{
				player.sendMessage("You don't have enough " + Config.ALT_SERVER_CUSTOM_ITEM_NAME + ".");
				return;
			}
			
			player.destroyItem("Consume", item.getObjectId(), Config.PREM_NOKARMA, null, true);
			setTargetKarma(player, 0);
			player.sendMessage("Karma has been cleaned.");
		}
		else if (command.startsWith("Remove "))
		{
			if (!player.getClient().getFloodProtectors().getTransaction().tryPerformAction("HennaRemove"))
			{
				return;
			}
			
			final int slot = Integer.parseInt(command.substring(7));
			player.removeHenna(slot);
			
			player.sendPacket(new ItemList(player, false));
			
		}
		else if (command.startsWith("Quest"))
		{
			String quest = "";
			try
			{
				quest = command.substring(5).trim();
			}
			catch (final IndexOutOfBoundsException ioobe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					ioobe.printStackTrace();
				}
			}
			if (quest.length() == 0)
			{
				showQuestWindowGeneral(player);
			}
			else
			{
				showQuestWindowSingle(player, quest);
			}
		}
		else if (command.equalsIgnoreCase("learn_clan_skills"))
		{
			showPledgeSkillList(player);
		}
		else
		{
			super.onBypassFeedback(player, command);
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
				html1.append("<a action=\"bypass -h npc_%objectId%_Remove " + i + "\">" + henna.getName() + "</a><br>");
			}
		}
		if (!hasHennas)
		{
			html1.append("You don't have any symbol to remove!");
		}
		
		html1.append("</body></html>");
		insertObjectIdAndShowChatWindow(player, html1.toString());
		html1 = null;
	}
	
	/**
	 * Show rent pet window.
	 * @param player the player
	 */
	public void showRentPetWindow(final L2PcInstance player)
	{
		if (!Config.LIST_PET_RENT_NPC.contains(getTemplate().npcId))
		{
			return;
		}
		
		TextBuilder html1 = new TextBuilder("<html><body>Pet Manager:<br>");
		html1.append("You can rent a wyvern or strider for adena.<br>My prices:<br1>");
		html1.append("<table border=0><tr><td>Ride</td></tr>");
		html1.append("<tr><td>Wyvern</td><td>Strider</td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 1\">30 sec/1800 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 11\">30 sec/900 adena</a></td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 2\">1 min/7200 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 12\">1 min/3600 adena</a></td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 3\">10 min/720000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 13\">10 min/360000 adena</a></td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 4\">30 min/6480000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 14\">30 min/3240000 adena</a></td></tr>");
		html1.append("</table>");
		html1.append("</body></html>");
		
		insertObjectIdAndShowChatWindow(player, html1.toString());
		html1 = null;
	}
	
	/**
	 * Try rent pet.
	 * @param player the player
	 * @param val the val
	 */
	public void tryRentPet(final L2PcInstance player, int val)
	{
		if (player == null || player.getPet() != null || player.isMounted() || player.isRentedPet())
		{
			return;
		}
		if (!player.disarmWeapons())
		{
			return;
		}
		
		int petId;
		double price = 1;
		final int cost[] =
		{
			1800,
			7200,
			720000,
			6480000
		};
		final int ridetime[] =
		{
			30,
			60,
			600,
			1800
		};
		
		if (val > 10)
		{
			petId = 12526;
			val -= 10;
			price /= 2;
		}
		else
		{
			petId = 12621;
		}
		
		if (val < 1 || val > 4)
		{
			return;
		}
		
		price *= cost[val - 1];
		final int time = ridetime[val - 1];
		
		if (!player.reduceAdena("Rent", (int) price, player.getLastFolkNPC(), true))
		{
			return;
		}
		
		Ride mount = new Ride(player.getObjectId(), Ride.ACTION_MOUNT, petId);
		player.broadcastPacket(mount);
		
		player.setMountType(mount.getMountType());
		player.startRentPet(time);
		mount = null;
	}
	
	@Override
	public void onActionShift(final L2GameClient client)
	{
		L2PcInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (player.getAccessLevel().isGm())
		{
			player.setTarget(this);
			
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			my = null;
			
			if (isAutoAttackable(player))
			{
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
				su = null;
			}
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder html1 = new TextBuilder("<html><body><table border=0>");
			html1.append("<tr><td>Current Target:</td></tr>");
			html1.append("<tr><td><br></td></tr>");
			
			html1.append("<tr><td>Object ID: " + getObjectId() + "</td></tr>");
			html1.append("<tr><td>Template ID: " + getTemplate().npcId + "</td></tr>");
			html1.append("<tr><td><br></td></tr>");
			
			html1.append("<tr><td>HP: " + getCurrentHp() + "</td></tr>");
			html1.append("<tr><td>MP: " + getCurrentMp() + "</td></tr>");
			html1.append("<tr><td>Level: " + getLevel() + "</td></tr>");
			html1.append("<tr><td><br></td></tr>");
			
			html1.append("<tr><td>Class: " + getClass().getName() + "</td></tr>");
			html1.append("<tr><td><br></td></tr>");
			
			// changed by terry 2005-02-22 21:45
			html1.append("</table><table><tr><td><button value=\"Edit NPC\" action=\"bypass -h admin_edit_npc " + getTemplate().npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			html1.append("<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
			html1.append("<tr><td><button value=\"Show DropList\" action=\"bypass -h admin_show_droplist " + getTemplate().npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
			html1.append("<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
			html1.append("</table>");
			
			if (player.isGM())
			{
				html1.append("<button value=\"View Shop\" action=\"bypass -h admin_showShop " + getTemplate().npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></br>");
				html1.append("<button value=\"Lease next week\" action=\"bypass -h npc_" + getObjectId() + "_Lease\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
				html1.append("<button value=\"Abort current leasing\" action=\"bypass -h npc_" + getObjectId() + "_Lease next\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
				html1.append("<button value=\"Manage items\" action=\"bypass -h npc_" + getObjectId() + "_Lease manage\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			}
			
			html1.append("</body></html>");
			
			html.setHtml(html1.toString());
			player.sendPacket(html);
			html = null;
			html1 = null;
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player = null;
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
			stmt = null;
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
			con = null;
		}
	}
	
	private void setTargetKarma(final L2PcInstance player, final int newKarma)
	{
		player.setKarma(newKarma);
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.KARMA, newKarma);
		player.sendPacket(su);
		su = null;
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
	
	public void showPledgeSkillList(L2PcInstance player)
	{
		if (Config.DEBUG)
		{
			LOG.warn("PledgeSkillList activated on: " + getObjectId());
		}
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
}

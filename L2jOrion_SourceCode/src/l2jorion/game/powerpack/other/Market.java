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
package l2jorion.game.powerpack.other;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import l2jorion.game.community.manager.MailBBSManager;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.ItemMarketTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.xml.AugmentationData;
import l2jorion.game.handler.IBBSHandler;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2ItemMarketModel;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.taskmanager.AttackStanceTaskManager;
import l2jorion.game.templates.L2EtcItemType;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class Market implements IVoicedCommandHandler, ICustomByPassHandler, IBBSHandler
{
	public static Logger LOG = LoggerFactory.getLogger(Market.class);
	
	private static Map<String, Integer> _priceItems = new ConcurrentHashMap<>();
	
	private static Market _instance = null;
	
	private final int ITEMS_PER_PAGE = 8;
	
	private final int ALL = 0x00;
	private final int WEAPON = 0x01;
	private final int ARMOR = 0x02;
	private final int RECIPE = 0x03;
	private final int SHOTS = 0x04;
	private final int BOOK = 0x05;
	private final int OTHER = 0x06;
	private final int MATERIAL = 0x07;
	
	// Item Grade
	private final int NO_G = 0x00;
	private final int D_G = 0x01;
	private final int C_G = 0x02;
	private final int B_G = 0x03;
	private final int A_G = 0x04;
	private final int S_G = 0x05;
	private final int AUG = 0x06;
	private final int ALL_G = 0x07;
	
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[]
		{
			PowerPackConfig.MARKET_COMMAND
		};
	}
	
	public static Market getInstance()
	{
		if (_instance == null)
		{
			_instance = new Market();
		}
		
		return _instance;
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
			msg = "Market is not available when you sit.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("ALL"))
		{
			msg = "Market is not available in this area.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("CURSED") && activeChar.isCursedWeaponEquiped())
		{
			msg = "Market is not available with the cursed weapon.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("ATTACK") && AttackStanceTaskManager.getInstance().getAttackStanceTask(activeChar))
		{
			msg = "Market is not available during the battle.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("DUNGEON") && activeChar.isIn7sDungeon())
		{
			msg = "Market is not available in the catacomb and necropolis.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("RB") && activeChar.isInsideZone(ZoneId.ZONE_NOSUMMONFRIEND))
		{
			msg = "Market is not available in this area.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("PVP") && activeChar.isInsideZone(ZoneId.ZONE_PVP))
		{
			msg = "Market is not available in this area.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("PEACE") && activeChar.isInsideZone(ZoneId.ZONE_PEACE))
		{
			msg = "Market is not available in this area.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("SIEGE") && activeChar.isInsideZone(ZoneId.ZONE_SIEGE))
		{
			msg = "Market is not available in this area.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("OLYMPIAD") && (activeChar.isInOlympiadMode() || activeChar.isInsideZone(ZoneId.ZONE_OLY) || OlympiadManager.getInstance().isRegistered(activeChar) || OlympiadManager.getInstance().isRegisteredInComp(activeChar)))
		{
			msg = "Market is not available at Olympiad.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("EVENT") && (activeChar._inEvent))
		{
			msg = "Market is not available at the opening event.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("TVT") && activeChar._inEventTvT && TvT.is_started())
		{
			msg = "Market is not available in TVT.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("CTF") && activeChar._inEventCTF && CTF.is_started())
		{
			msg = "Market is not available in CTF.";
		}
		else if (PowerPackConfig.MARKET_EXCLUDE_ON.contains("DM") && activeChar._inEventDM && DM.is_started())
		{
			msg = "Market is not available in DM.";
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
		
		if (PowerPackConfig.MARKET_USECOMMAND && command.equalsIgnoreCase(PowerPackConfig.MARKET_COMMAND))
		{
			showMsgWindow(activeChar);
		}
		
		return false;
	}
	
	private static String[] _CMD =
	{
		"market"
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
		
		if (parameters == null || parameters.length() == 0)
		{
			return;
		}
		
		if (!checkAllowed(player))
		{
			return;
		}
		
		L2NpcInstance marketNpc = null;
		
		if (!PowerPackConfig.MARKET_USEBBS && !PowerPackConfig.MARKET_USECOMMAND)
		{
			if (player.getTarget() != null && player.getTarget() instanceof L2NpcInstance)
			{
				marketNpc = (L2NpcInstance) player.getTarget();
			}
			
			if (marketNpc == null)
			{
				return;
			}
			
			if (!player.isInsideRadius(marketNpc, L2NpcInstance.INTERACTION_DISTANCE, false, false))
			{
				return;
			}
		}
		
		List<L2ItemMarketModel> list = null;
		List<L2ItemMarketModel> searchList = null;
		
		StringTokenizer st = new StringTokenizer(parameters, " ");
		String actualCommand = st.nextToken();
		
		if ("Search".equalsIgnoreCase(actualCommand))
		{
			if (st.hasMoreTokens())
			{
				String itemName = "";
				itemName = st.nextToken();
				
				if (itemName.isEmpty() || itemName.length() < 3)
				{
					sendMsg("Minimum 3 chars.", player);
					player.sendPacket(new ExShowScreenMessage("Minimum 3 chars.", 1000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					return;
				}
				
				int bitmask = Integer.valueOf(st.nextToken());
				int pgId = 0;
				
				if (st.hasMoreTokens())
				{
					pgId = Integer.valueOf(st.nextToken());
				}
				
				searchList = ItemMarketTable.getInstance().getSearchItems(itemName);
				
				if (searchList != null)
				{
					searchList = filterItemType(bitmask, searchList);
					showItemList(searchList, pgId, player, bitmask);
				}
				else
				{
					sendMsg("Nothing found.", player);
					return;
				}
			}
		}
		if ("Private".equalsIgnoreCase(actualCommand))
		{
			list = getItemList(player);
			int pId = 0;
			if (st.hasMoreTokens())
			{
				pId = Integer.valueOf(st.nextToken());
			}
			showPrivateItemList(list, pId, player);
		}
		else if ("See".equalsIgnoreCase(actualCommand))
		{
			if (st.hasMoreTokens())
			{
				int bitmask = Integer.valueOf(st.nextToken());
				int pgId = 0;
				if (st.hasMoreTokens())
				{
					pgId = Integer.valueOf(st.nextToken());
				}
				list = ItemMarketTable.getInstance().getAllItems();
				if (list != null)
				{
					list = filterItemType(bitmask, list);
					showItemList(list, pgId, player, bitmask);
				}
				else
				{
					sendMsg("There are no items for you", player);
					return;
				}
			}
		}
		else if ("BuyItem".equalsIgnoreCase(actualCommand))
		{
			if (st.hasMoreTokens())
			{
				int itemObjId = Integer.parseInt(st.nextToken());
				buyItem(player, itemObjId);
			}
		}
		else if ("AddItem".equalsIgnoreCase(actualCommand))
		{
			if (st.hasMoreTokens())
			{
				int itemObjId = Integer.valueOf(st.nextToken());
				if (st.hasMoreTokens())
				{
					int count = Integer.valueOf(st.nextToken());
					
					if (st.hasMoreTokens())
					{
						int price = Integer.valueOf(st.nextToken());
						
						int augmentationId = Integer.valueOf(st.nextToken());
						int augmentationSkill = Integer.valueOf(st.nextToken());
						int augmentationSkillLevel = Integer.valueOf(st.nextToken());
						String augmentationBonus = st.nextToken();
						
						int priceItem = Integer.valueOf(st.nextToken());
						
						L2ItemInstance item = player.getInventory().getItemByObjectId(itemObjId);
						list = getItemList(player);
						
						if (canAddItem(item, count, list, player))
						{
							player.destroyItem("Market Add", item.getObjectId(), count, null, true);
							addItem(player, item, count, priceItem, price, augmentationId, augmentationSkill, augmentationSkillLevel, augmentationBonus);
						}
						else
						{
							sendMsg("Unable to add this item.", player);
							player.sendPacket(new ExShowScreenMessage("Unable to add this item.", 1000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
						}
					}
				}
			}
		}
		else if ("ListInv".equalsIgnoreCase(actualCommand))
		{
			int pageId = 0;
			if (st.hasMoreTokens())
			{
				pageId = Integer.valueOf(st.nextToken());
			}
			showInvList(player, pageId);
		}
		else if ("ItemInfo".equalsIgnoreCase(actualCommand))
		{
			if (st.hasMoreTokens())
			{
				int pgId = Integer.valueOf(st.nextToken());
				if (st.hasMoreTokens())
				{
					int bitmask = Integer.valueOf(st.nextToken());
					if (st.hasMoreTokens())
					{
						int itemObjId = Integer.valueOf(st.nextToken());
						L2ItemMarketModel mrktItem = ItemMarketTable.getInstance().getItem(itemObjId);
						if (mrktItem != null)
						{
							showItemInfo(mrktItem, bitmask, pgId, player);
						}
					}
				}
			}
		}
		else if ("Main".equalsIgnoreCase(actualCommand))
		{
			showMsgWindow(player);
		}
		// TODO addItem.htm
		else if ("SelectItem".equalsIgnoreCase(actualCommand))
		{
			if (st.hasMoreTokens())
			{
				int itemObjId = Integer.valueOf(st.nextToken());
				
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				
				String filename = "data/html/market/addItem.htm";
				L2ItemInstance item = player.getInventory().getItemByObjectId(itemObjId);
				if (item.getCount() > 1)
				{
					filename = "data/html/market/addItemQuantity.htm";
				}
				
				html.setFile(filename);
				
				int augmentationId = 0;
				int augmentationSkill = 0;
				int augmentationSkillLevel = 0;
				
				String stats1 = "";
				String stats2 = "";
				String stats3 = "";
				String stats4 = "";
				
				String augmentationBonus = "-";
				
				if (item.isAugmented())
				{
					augmentationId = item.getAugmentation().getAugmentationId();
					
					if (item.getAugmentation().getBonus() != null)
					{
						if (item.getAugmentation().getBonus()._stats.length > 0)
						{
							stats1 = (item.getAugmentation().getBonus()._stats[0]) + ":_+" + (item.getAugmentation().getBonus()._values[0]);
							stats1 = stats1.toLowerCase();
							stats1 = stats1.substring(0, 1).toUpperCase() + stats1.substring(1);
							stats1 = stats1 + "<br1>";
						}
						
						if (item.getAugmentation().getBonus()._stats.length > 1)
						{
							stats2 = (item.getAugmentation().getBonus()._stats[1]) + ":_+" + (item.getAugmentation().getBonus()._values[1]);
							stats2 = stats2.toLowerCase();
							stats2 = stats2.substring(0, 1).toUpperCase() + stats2.substring(1);
							stats2 = stats2 + "<br1>";
						}
						
						if (item.getAugmentation().getBonus()._stats.length > 2)
						{
							stats3 = (item.getAugmentation().getBonus()._stats[2]) + ":_+" + (item.getAugmentation().getBonus()._values[2]);
							stats3 = stats3.toLowerCase();
							stats3 = stats3.substring(0, 1).toUpperCase() + stats3.substring(1);
							stats3 = stats3 + "<br1>";
						}
						
						if (item.getAugmentation().getBonus()._stats.length > 3)
						{
							stats4 = (item.getAugmentation().getBonus()._stats[3]) + ":_+" + (item.getAugmentation().getBonus()._values[3]);
							stats4 = stats4.toLowerCase();
							stats4 = stats4.substring(0, 1).toUpperCase() + stats4.substring(1);
							stats4 = stats4 + "<br1>";
						}
					}
					
					augmentationBonus = stats1 + "" + stats2 + "" + stats3 + "" + stats4;
					
					if (item.getAugmentation().getSkill() != null)
					{
						augmentationSkill = item.getAugmentation().getSkill().getId();
						augmentationSkillLevel = item.getAugmentation().getSkill().getLevel();
					}
				}
				
				TextBuilder htm = new TextBuilder();
				for (String itemId : PowerPackConfig.PRICE_ITEMS.split(","))
				{
					String itemName = getItemNameById(Integer.parseInt(itemId));
					itemName = itemName.replaceAll(" ", "_");
					
					htm.append(itemName + ";");
					
					_priceItems.put(itemName, Integer.parseInt(itemId));
				}
				html.replace("%priceItems%", htm.toString());
				
				html.replace("%itemIcon%", getItemIcon(item.getItemId()));
				
				if (item.getEnchantLevel() > 0)
				{
					html.replace("%itemName%", "<font color=3399ff>+" + item.getEnchantLevel() + "</font> " + (item.isAugmented() ? "Augmented" : "") + " " + item.getItemName());
				}
				else
				{
					html.replace("%itemName%", (item.isAugmented() ? "Augmented" : "") + " " + item.getItemName());
				}
				
				html.replace("%count%", Util.formatAdena(item.getCount()));
				html.replace("%grade%", getGradeByCrystal(String.valueOf(item.getItem().getCrystalType())));
				html.replace("%crystal%", item.getItem().getCrystalType());
				html.replace("%itemObjId%", String.valueOf(itemObjId));
				
				html.replace("%augmentationId%", String.valueOf(augmentationId));
				html.replace("%augmentationSkill%", String.valueOf(augmentationSkill));
				html.replace("%augmentationSkillLevel%", String.valueOf(augmentationSkillLevel));
				html.replace("%augmentationBonus%", augmentationBonus);
				
				player.sendPacket(html);
			}
		}
		else if ("ItemInfo2".equalsIgnoreCase(actualCommand))
		{
			if (st.hasMoreTokens())
			{
				int pgId = Integer.valueOf(st.nextToken());
				if (st.hasMoreTokens())
				{
					int itemObjId = Integer.valueOf(st.nextToken());
					L2ItemMarketModel mrktItem = ItemMarketTable.getInstance().getItem(itemObjId);
					if (mrktItem != null)
					{
						showItemInfo2(mrktItem, pgId, player);
					}
				}
			}
		}
		else if ("TakeItem".equalsIgnoreCase(actualCommand))
		{
			if (st.hasMoreTokens())
			{
				int itemObjId = Integer.valueOf(st.nextToken());
				L2ItemMarketModel mrktItem = ItemMarketTable.getInstance().getItem(itemObjId);
				if (mrktItem != null && player.getObjectId() == mrktItem.getOwnerId())
				{
					ItemMarketTable.getInstance().removeItemFromMarket(mrktItem.getOwnerId(), mrktItem.getItemObjId(), mrktItem.getCount());
					
					L2ItemInstance item = ItemTable.getInstance().createItem("Market Remove", mrktItem.getItemId(), mrktItem.getCount(), player);
					
					item.setEnchantLevel(mrktItem.getEnchLvl());
					
					player.getInventory().addItem("Market Buy", item, player, null);
					
					if (mrktItem.getCount() > 1)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(mrktItem.getItemId());
						sm.addNumber(mrktItem.getCount());
						player.sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
						sm.addItemName(mrktItem.getItemId());
						player.sendPacket(sm);
					}
					
					if (mrktItem.getAugmentationId() > 0)
					{
						L2Skill skill = null;
						
						if (mrktItem.getAugmentationSkill() > 0)
						{
							skill = SkillTable.getInstance().getInfo(mrktItem.getAugmentationSkill(), mrktItem.getAugmentationSkillLevel());
						}
						
						item.setAugmentation(AugmentationData.getInstance().generateAugmentationForMarket(item, mrktItem.getAugmentationId(), skill));
					}
					
					if (mrktItem.getCount() > 1)
					{
						sendMsg("Removed succesfully: <font color=\"LEVEL\">" + mrktItem.getItemName() + "</font> (" + Util.formatAdena(mrktItem.getCount()) + ").", player);
					}
					else
					{
						if (mrktItem.getEnchLvl() > 0)
						{
							sendMsg("Removed succesfully: <font color=3399ff>+" + mrktItem.getEnchLvl() + "</font> <font color=\"LEVEL\">" + (item.isAugmented() ? "Augmented" : "") + " " + mrktItem.getItemName() + "</font>.", player);
						}
						else
						{
							sendMsg("Removed succesfully: <font color=\"LEVEL\">" + (item.isAugmented() ? "Augmented" : "") + " " + mrktItem.getItemName() + "</font>.", player);
						}
					}
					
					player.sendPacket(new PlaySound("ItemSound3.ItemSound3.sys_exchange_success"));
					
					final StatusUpdate su = new StatusUpdate(player.getObjectId());
					su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
					player.sendPacket(su);
					player.sendPacket(new ItemList(player, true));
				}
			}
		}
		// TODO ComfirmAdd
		else if ("ComfirmAdd".equalsIgnoreCase(actualCommand))
		{
			if (st.hasMoreTokens())
			{
				int itemObjId = Integer.valueOf(st.nextToken());
				L2ItemInstance item = player.getInventory().getItemByObjectId(itemObjId);
				if (item == null)
				{
					return;
				}
				
				if (st.hasMoreTokens())
				{
					int count = Integer.valueOf(st.nextToken());
					if (count <= 0 || item.getCount() < count)
					{
						sendMsg("Item count must be a valid value.", player);
						return;
					}
					
					if (st.hasMoreTokens())
					{
						int price = Integer.valueOf(st.nextToken());
						if (price <= 0)
						{
							sendMsg("Price must be a valid value.", player);
							return;
						}
						
						String augmentationId = st.nextToken();
						String augmentationSkill = st.nextToken();
						String augmentationSkillLevel = st.nextToken();
						String augmentationBonus = st.nextToken();
						
						String priceItem = st.nextToken();
						
						String priceItemName = priceItem.replaceAll("_", " ");
						
						String priceItemId = "" + _priceItems.get(priceItem);
						
						player.sendPacket(ActionFailed.STATIC_PACKET);
						String filename = "data/html/market/comfirm.htm";
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile(filename);
						
						html.replace("%priceItemName%", "" + priceItemName);
						html.replace("%priceItem%", "" + priceItemId);
						html.replace("%count%", "" + count);
						
						if (item.getEnchantLevel() > 0)
						{
							html.replace("%itemName%", "<font color=3399ff>+" + item.getEnchantLevel() + "</font> " + (item.isAugmented() ? "Augmented" : "") + " " + item.getItemName());
						}
						else
						{
							html.replace("%itemName%", (item.isAugmented() ? "Augmented" : "") + " " + item.getItemName());
						}
						
						html.replace("%itemIcon%", getItemIcon(item.getItemId()));
						html.replace("%price%", Util.formatAdena(price));
						html.replace("%iprice%", price);
						html.replace("%itemObjId%", String.valueOf(itemObjId));
						
						html.replace("%augmentationId%", String.valueOf(augmentationId));
						html.replace("%augmentationSkill%", String.valueOf(augmentationSkill));
						html.replace("%augmentationSkillLevel%", String.valueOf(augmentationSkillLevel));
						html.replace("%augmentationBonus%", augmentationBonus);
						player.sendPacket(html);
					}
				}
			}
		}
		else if ("Link".equalsIgnoreCase(actualCommand))
		{
			final String path = parameters.substring(5).trim();
			String filename = "/data/html/" + path;
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile(filename);
			player.sendPacket(html);
		}
	}
	
	// TODO showMsgWindow
	public void showMsgWindow(L2PcInstance player)
	{
		int playerItems = ItemMarketTable.getInstance().getYourItemsCount(player.getObjectId());
		int allItems = ItemMarketTable.getInstance().getMarketItemsCount();
		int sellers = ItemMarketTable.getInstance().getSellersCount();
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		String filename = "data/html/market/market.htm";
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		
		if (playerItems > 0)
		{
			html.replace("%playerItems%", "<font color=00ff00>" + playerItems + "</font>");
		}
		else
		{
			html.replace("%playerItems%", "<font color=ff0000>" + playerItems + "</font>");
		}
		
		if (allItems > 0)
		{
			html.replace("%allItems%", "<font color=00ff00>" + allItems + "</font>");
		}
		else
		{
			html.replace("%allItems%", "<font color=ff0000>" + allItems + "</font>");
		}
		
		if (sellers > 0)
		{
			html.replace("%sellers%", "<font color=00ff00>" + sellers + "</font>");
		}
		else
		{
			html.replace("%sellers%", "<font color=ff0000>" + sellers + "</font>");
		}
		
		TextBuilder reply = new TextBuilder("");
		
		List<L2ItemMarketModel> list = ItemMarketTable.getInstance().getLatest();
		int MaxItems = 4;
		int pageId = 0;
		int mask = 7;
		String link = "";
		
		if (list != null && !list.isEmpty())
		{
			int itemStart = 0;
			
			if (list.size() > MaxItems)
			{
				itemStart = list.size() - MaxItems;
			}
			
			int itemEnd = list.size();
			
			int color = 1;
			for (int i = itemStart; i < itemEnd; i++)
			{
				L2ItemMarketModel mrktItem = list.get(i);
				if (mrktItem == null)
				{
					continue;
				}
				
				if (mrktItem.getOwnerId() == player.getObjectId())
				{
					link = ("ItemInfo2 " + pageId + " " + mrktItem.getItemObjId());
				}
				else
				{
					link = ("ItemInfo " + pageId + " " + mask + " " + mrktItem.getItemObjId());
				}
				
				String itemIcon = getItemIcon(mrktItem.getItemId());
				
				if (color == 1)
				{
					reply.append("<table width=300 bgcolor=000000 border=0><tr>");
					color = 2;
				}
				else
				{
					reply.append("<table width=300 border=0><tr>");
					color = 1;
				}
				
				reply.append("<td valign=top width=35><button value=\"\" action=\"bypass -h custom_market " + link + "\" width=32 height=32 back=\"" + itemIcon + "\" fore=\"" + itemIcon + "\"></td>");
				reply.append("<td valign=top width=235>");
				reply.append("<table border=0 width=100%>");
				
				if (mrktItem.getCount() > 1)
				{
					reply.append("<tr><td width=235><a action=\"bypass -h custom_market " + link + "\">" + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + " (" + Util.formatAdena(mrktItem.getCount()) + ")</a> </td><td> " + getGrade(mrktItem.getItemGrade())
						+ "</td></tr>");
				}
				else
				{
					if (mrktItem.getEnchLvl() > 0)
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market " + link + "\">+" + mrktItem.getEnchLvl() + " " + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + "</a> </td><td>  " + getGrade(mrktItem.getItemGrade()) + "</td></tr>");
					}
					else
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market " + link + "\">" + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + "</a> </td><td> " + getGrade(mrktItem.getItemGrade()) + "</td></tr>");
					}
				}
				reply.append("<tr><td><font color=\"A2A0A2\">Price:</font> <font color=\"LEVEL\">" + Util.formatAdena(mrktItem.getPrice()) + " " + getItemNameById(mrktItem.getPriceItem()) + "</font></td><td></td></tr></table></td>");
				reply.append("</tr></table>");
			}
		}
		else
		{
			reply.append("<br><center><font color=ff0000>List is empty.</font></center>");
		}
		
		html.replace("%bottom%", reply.toString());
		player.sendPacket(html);
	}
	
	// TODO Add Item
	private void addItem(L2PcInstance player, L2ItemInstance item, int count, int priceItem, int price, int augmentationId, int augmentationSkill, int augmentationSkillLevel, String augmentationBonus)
	{
		L2ItemMarketModel itemModel = new L2ItemMarketModel();
		itemModel.setOwnerId(player.getObjectId());
		itemModel.setOwnerName(player.getName());
		itemModel.setItemObjId(item.getObjectId());
		itemModel.setItemId(item.getItemId());
		itemModel.setPriceItem(priceItem);
		itemModel.setPrice(price);
		itemModel.setCount(count);
		
		itemModel.setAugmentationId(augmentationId);
		itemModel.setAugmentationSkill(augmentationSkill);
		itemModel.setAugmentationSkillLevel(augmentationSkillLevel);
		
		augmentationBonus = augmentationBonus.replaceAll("_", " ");
		itemModel.setAugmentationBonus(augmentationBonus);
		
		itemModel.setItemType(item.getItem().getItemType().toString());
		itemModel.setEnchLvl(item.getEnchantLevel());
		itemModel.setItemName(item.getItemName());
		itemModel.setItemGrade(item.getItem().getCrystalType());
		
		if (item.isEquipable())
		{
			if (item.getItemType() == L2WeaponType.NONE)
			{
				itemModel.setL2Type("Armor");
			}
			else if (item.getItemType() == L2WeaponType.SWORD)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.BLUNT)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.DAGGER)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.BOW)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.POLE)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.ETC)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.FIST)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.DUAL)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.DUALFIST)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.BIGSWORD)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.PET)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.ROD)
			{
				itemModel.setL2Type("Weapon");
			}
			else if (item.getItemType() == L2WeaponType.BIGBLUNT)
			{
				itemModel.setL2Type("Weapon");
			}
			else
			{
				itemModel.setL2Type("Armor");
			}
		}
		else
		{
			if (item.getItemType() == L2EtcItemType.MATERIAL)
			{
				itemModel.setL2Type("Material");
			}
			else if (item.getItemType() == L2EtcItemType.RECEIPE)
			{
				itemModel.setL2Type("Recipe");
			}
			else if (item.getItemType() == L2EtcItemType.SPELLBOOK)
			{
				itemModel.setL2Type("Spellbook");
			}
			else if (item.getItemType() == L2EtcItemType.SHOT)
			{
				itemModel.setL2Type("Shot");
			}
			else
			{
				itemModel.setL2Type("Other");
			}
		}
		
		ItemMarketTable.getInstance().addItemToMarket(itemModel, player);
		
		if (count > 1)
		{
			sendMsg("Added: <font color=\"00ff00\">" + item.getItemName() + "</font> (" + Util.formatAdena(count) + ").", player);
		}
		else
		{
			if (item.getEnchantLevel() > 0)
			{
				sendMsg("Added: <font color=3399ff>+" + item.getEnchantLevel() + "</font> <font color=\"00ff00\">" + (item.isAugmented() ? "Augmented" : "") + " " + item.getItemName() + "</font>.", player);
			}
			else
			{
				sendMsg("Added: <font color=\"00ff00\">" + (item.isAugmented() ? "Augmented" : "") + " " + item.getItemName() + "</font>.", player);
			}
		}
		
		final StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.sendPacket(new ItemList(player, true));
	}
	
	private boolean canAddItem(L2ItemInstance item, int count, List<L2ItemMarketModel> list, L2PcInstance activeChar)
	{
		if (activeChar != null && activeChar.getActiveTradeList() != null)
		{
			return false;
		}
		
		if (activeChar != null && activeChar.isProcessingTransaction())
		{
			return false;
		}
		
		if (list != null && !list.isEmpty())
		{
			for (L2ItemMarketModel model : list)
			{
				if (model != null)
				{
					String item1 = String.valueOf(model.getItemObjId());
					String item2 = String.valueOf(item.getObjectId());
					if (item1.contains(item2))
					{
						return false;
					}
				}
			}
		}
		
		return (item.getItemType() != L2EtcItemType.HERB && item.getCount() >= count && item.getItem().getDuration() == -1 && item.isTradeableItem() && !item.isEquipped());
	}
	
	private boolean canAddItem(L2ItemInstance item)
	{
		return canAddItem(item, 0, null, null);
	}
	
	private String getItemIcon(int itemId)
	{
		return ItemMarketTable.getInstance().getItemIcon(itemId);
	}
	
	private List<L2ItemMarketModel> getItemList(L2PcInstance player)
	{
		return ItemMarketTable.getInstance().getItemsByOwnerId(player.getObjectId());
	}
	
	// TODO BuyItem
	private void buyItem(L2PcInstance player, int itemObjId)
	{
		L2ItemMarketModel mrktItem = ItemMarketTable.getInstance().getItem(itemObjId);
		
		if (mrktItem != null)
		{
			int itemId = mrktItem.getItemId();
			int itemCount = mrktItem.getCount();
			int price = mrktItem.getPrice();
			
			L2ItemInstance currency = player.getInventory().getItemByItemId(mrktItem.getPriceItem());
			
			if (currency == null || currency.getCount() < price)
			{
				sendMsg("You don't have enough <font color=\"ff0000\">" + getItemNameById(mrktItem.getPriceItem()) + "</font>.", player);
				player.sendPacket(new ExShowScreenMessage("You don't have enough " + getItemNameById(mrktItem.getPriceItem()) + ".", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			if (currency.getCount() >= price)
			{
				// Remove item from market
				ItemMarketTable.getInstance().removeItemFromMarket(mrktItem.getOwnerId(), mrktItem.getItemObjId(), itemCount);
				
				// Destroy currency
				player.destroyItem("Destroy currency", currency.getObjectId(), price, null, true);
				
				// Send money for seller
				L2PcInstance seller = L2World.getInstance().getPlayer(mrktItem.getOwnerName());
				if (seller != null && seller.isOnline() == 1)
				{
					seller.getInventory().addItem("Market Sell", currency.getItemId(), price, seller, null);
					MailBBSManager.getInstance().sendMail(mrktItem.getOwnerName(), "Market", "" + player.getName() + " bought your an item: " + mrktItem.getItemName() + " from the Market. " + "You've got: " + Util.formatAdena(price) + " " + currency.getItemName()
						+ ". Check your inventory!", player);
					
					if (price > 1)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(currency.getItemId());
						sm.addNumber(price);
						seller.sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
						sm.addItemName(currency.getItemId());
						seller.sendPacket(sm);
					}
				}
				else
				{
					addItemForOffliner(mrktItem.getOwnerId(), IdFactory.getInstance().getNextId(), currency.getItemId(), price);
					MailBBSManager.getInstance().sendMail(mrktItem.getOwnerName(), "Market", "" + player.getName() + " bought your an item: " + mrktItem.getItemName() + " from the Market. " + "You've got: " + Util.formatAdena(price) + " " + currency.getItemName()
						+ ". Check your inventory!", player);
				}
				// Send item for buyer
				L2ItemInstance item = ItemTable.getInstance().createItem("Market Buy", itemId, itemCount, player);
				
				if (mrktItem.getEnchLvl() > 0)
				{
					item.setEnchantLevel(mrktItem.getEnchLvl());
				}
				
				player.getInventory().addItem("Market Buy", item, player, null);
				
				if (itemCount > 1)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
					sm.addItemName(itemId);
					sm.addNumber(itemCount);
					player.sendPacket(sm);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
					sm.addItemName(itemId);
					player.sendPacket(sm);
				}
				
				if (mrktItem.getAugmentationId() > 0)
				{
					L2Skill skill = null;
					
					if (mrktItem.getAugmentationSkill() > 0)
					{
						skill = SkillTable.getInstance().getInfo(mrktItem.getAugmentationSkill(), mrktItem.getAugmentationSkillLevel());
					}
					
					item.setAugmentation(AugmentationData.getInstance().generateAugmentationForMarket(item, mrktItem.getAugmentationId(), skill));
				}
				
				if (itemCount > 1)
				{
					sendMsg("Bought: <font color=\"LEVEL\">" + mrktItem.getItemName() + "</font> (" + Util.formatAdena(itemCount) + ").", player);
				}
				else
				{
					if (mrktItem.getEnchLvl() > 0)
					{
						sendMsg("Bought: <font color=3399ff>+" + mrktItem.getEnchLvl() + "</font> <font color=\"LEVEL\">" + (item.isAugmented() ? "Augmented" : "") + " " + mrktItem.getItemName() + "</font>.", player);
					}
					else
					{
						sendMsg("Bought: <font color=\"LEVEL\">" + (item.isAugmented() ? "Augmented" : "") + " " + mrktItem.getItemName() + "</font>.", player);
					}
				}
				
				player.sendPacket(new PlaySound("ItemSound3.ItemSound3.sys_exchange_success"));
				
				final StatusUpdate su = new StatusUpdate(player.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
				player.sendPacket(su);
				player.sendPacket(new ItemList(player, true));
				return;
			}
		}
	}
	
	private List<L2ItemMarketModel> filterItemType(int mask, List<L2ItemMarketModel> list)
	{
		List<L2ItemMarketModel> mrktList = new FastList<>();
		int itype = mask >> 3;
		switch (itype)
		{
			case ALL:
				return filterItemGrade(mask, list);
			case WEAPON:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getL2Type().equalsIgnoreCase("Weapon"))
						{
							mrktList.add(model);
						}
					}
				}
				return filterItemGrade(mask, mrktList);
			case ARMOR:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getL2Type().equalsIgnoreCase("Armor"))
						{
							mrktList.add(model);
						}
					}
				}
				return filterItemGrade(mask, mrktList);
			case RECIPE:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getL2Type().equalsIgnoreCase("Recipe"))
						{
							mrktList.add(model);
						}
					}
				}
				return mrktList;
			case BOOK:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getL2Type().equalsIgnoreCase("Spellbook"))
						{
							mrktList.add(model);
						}
					}
				}
				return mrktList;
			case SHOTS:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getL2Type().equalsIgnoreCase("Shot"))
						{
							mrktList.add(model);
						}
					}
				}
				return filterItemGrade(mask, mrktList);
			case OTHER:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getL2Type().equalsIgnoreCase("Other"))
						{
							mrktList.add(model);
						}
					}
				}
				return filterItemGrade(mask, mrktList);
			case MATERIAL:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getL2Type().equalsIgnoreCase("Material"))
						{
							mrktList.add(model);
						}
					}
				}
				return mrktList;
		}
		return filterItemGrade(mask, list);
	}
	
	private List<L2ItemMarketModel> filterItemGrade(int mask, List<L2ItemMarketModel> list)
	{
		List<L2ItemMarketModel> mrktList = new FastList<>();
		int igrade = mask & 7;
		switch (igrade)
		{
			case ALL_G:
				return list;
			case NO_G:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getItemGrade() == NO_G)
						{
							mrktList.add(model);
						}
					}
				}
				return mrktList;
			case D_G:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getItemGrade() == D_G)
						{
							mrktList.add(model);
						}
					}
				}
				return mrktList;
			case C_G:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getItemGrade() == C_G)
						{
							mrktList.add(model);
						}
					}
				}
				return mrktList;
			case B_G:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getItemGrade() == B_G)
						{
							mrktList.add(model);
						}
					}
				}
				return mrktList;
			case A_G:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getItemGrade() == A_G)
						{
							mrktList.add(model);
						}
					}
				}
				return mrktList;
			case S_G:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getItemGrade() == S_G)
						{
							mrktList.add(model);
						}
					}
				}
				return mrktList;
			case AUG:
				for (L2ItemMarketModel model : list)
				{
					if (model != null)
					{
						if (model.getAugmentationId() > 0)
						{
							mrktList.add(model);
						}
					}
				}
				return mrktList;
		}
		return list;
	}
	
	private List<L2ItemInstance> filterInventory(L2ItemInstance[] inv)
	{
		List<L2ItemInstance> filteredInventory = new FastList<>();
		for (L2ItemInstance item : inv)
		{
			if (canAddItem(item))
			{
				filteredInventory.add(item);
			}
		}
		return filteredInventory;
	}
	
	private List<L2ItemMarketModel> filterList(List<L2ItemMarketModel> list, L2PcInstance player)
	{
		List<L2ItemMarketModel> filteredList = new FastList<>();
		if (!list.isEmpty())
		{
			for (L2ItemMarketModel model : list)
			{
				if (model != null && model.getOwnerId() != player.getObjectId())
				{
					filteredList.add(model);
				}
			}
		}
		return filteredList;
	}
	
	private void showInvList(L2PcInstance player, int pageId)
	{
		int itemsOnPage = ITEMS_PER_PAGE;
		List<L2ItemInstance> list = filterInventory(player.getInventory().getItems());
		int pages = list.size() / itemsOnPage;
		
		if (list.isEmpty())
		{
			sendMsg("Your inventory is empty.", player);
			return;
		}
		
		if (list.size() > pages * itemsOnPage)
		{
			pages++;
		}
		
		if (pageId > pages)
		{
			pageId = pages;
		}
		
		int itemStart = pageId * itemsOnPage;
		int itemEnd = list.size();
		if (itemEnd - itemStart > itemsOnPage)
		{
			itemEnd = itemStart + itemsOnPage;
		}
		NpcHtmlMessage npcReply = new NpcHtmlMessage(1);
		TextBuilder reply = new TextBuilder("<html><body>");
		reply.append("<center>Items in Inventory</center><br>");
		reply.append("<img src=\"l2ui.SquareGray\" width=\"298\" height=\"1\">");
		
		int color = 1;
		for (int i = itemStart; i < itemEnd; i++)
		{
			L2ItemInstance item = list.get(i);
			if (item == null)
			{
				continue;
			}
			// TODO
			String itemIcon = getItemIcon(item.getItemId());
			
			if (color == 1)
			{
				reply.append("<table width=300 border=0 bgcolor=000000><tr>");
				reply.append("<td valign=top width=35><button value=\"\" action=\"bypass -h custom_market SelectItem " + item.getObjectId() + "\" width=32 height=32 back=\"" + itemIcon + "\" fore=\"" + itemIcon + "\"></td>");
				reply.append("<td valign=top width=235>");
				reply.append("<table border=0 width=100%>");
				
				if (item.getCount() > 1)
				{
					reply.append("<tr><td width=235><a action=\"bypass -h custom_market SelectItem " + item.getObjectId() + "\">" + item.getItemName() + " (" + Util.formatAdena(item.getCount()) + ")</a> </td><td> " + getGradeByCrystal(String.valueOf(item.getItem().getCrystalType())) + "</td></tr>");
				}
				else
				{
					if (item.getEnchantLevel() > 0)
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market SelectItem " + item.getObjectId() + "\">+" + item.getEnchantLevel() + " " + (item.isAugmented() ? "Augmented" : "") + " " + item.getItemName() + "</a> </td><td>  "
							+ getGradeByCrystal(String.valueOf(item.getItem().getCrystalType())) + "</td></tr>");
					}
					else
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market SelectItem " + item.getObjectId() + "\">" + (item.isAugmented() ? "Augmented" : "") + " " + item.getItemName() + "</a> </td><td> " + getGradeByCrystal(String.valueOf(item.getItem().getCrystalType()))
							+ "</td></tr>");
					}
				}
				
				reply.append("</table></td>");
				reply.append("</tr></table>");
				color = 2;
			}
			else
			{
				reply.append("<table width=300 border=0><tr>");
				reply.append("<td valign=top width=35><button value=\"\" action=\"bypass -h custom_market" + " SelectItem " + item.getObjectId() + "\" width=32 height=32 back=\"" + itemIcon + "\" fore=\"" + itemIcon + "\"></td>");
				reply.append("<td valign=top width=235>");
				reply.append("<table border=0 width=100%>");
				
				if (item.getCount() > 1)
				{
					reply.append("<tr><td width=235><a action=\"bypass -h custom_market SelectItem " + item.getObjectId() + "\">" + item.getItemName() + " (" + Util.formatAdena(item.getCount()) + ")</a> </td><td> " + getGradeByCrystal(String.valueOf(item.getItem().getCrystalType())) + "</td></tr>");
				}
				else
				{
					if (item.getEnchantLevel() > 0)
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market SelectItem " + item.getObjectId() + "\">+" + item.getEnchantLevel() + " " + (item.isAugmented() ? "Augmented" : "") + " " + item.getItemName() + "</a> </td><td>  "
							+ getGradeByCrystal(String.valueOf(item.getItem().getCrystalType())) + "</td></tr>");
					}
					else
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market SelectItem " + item.getObjectId() + "\">" + (item.isAugmented() ? "Augmented" : "") + " " + item.getItemName() + "</a> </td><td> " + getGradeByCrystal(String.valueOf(item.getItem().getCrystalType()))
							+ "</td></tr>");
					}
				}
				
				reply.append("</table></td>");
				reply.append("</tr></table>");
				color = 1;
			}
		}
		reply.append("<img src=\"l2ui.SquareGray\" width=\"298\" height=\"1\">");
		reply.append("<table width=300><tr>");
		reply.append("<td width=66><button value=\"Back\" action=\"bypass -h custom_market" + ((pageId == 0) ? " Main " : " ListInv ") + (pageId - 1) + "\" width=66 height=16 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></td>");
		reply.append("<td width=138></td>");
		reply.append("<td width=66>" + ((pageId + 1 < pages) ? "<button value=\"Next\" action=\"bypass -h custom_market" + " ListInv " + (pageId + 1) + "\" width=66 height=16 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\">" : "") + "</td>");
		reply.append("</tr></table>");
		reply.append("</body></html>");
		npcReply.setHtml(reply.toString());
		player.sendPacket(npcReply);
	}
	
	private void showItemList(List<L2ItemMarketModel> list, int pageId, L2PcInstance player, int mask)
	{
		int itemsOnPage = ITEMS_PER_PAGE;
		list = filterList(list, player);
		if (list.isEmpty())
		{
			sendMsg("There are no items for you", player);
			return;
		}
		
		int pages = list.size() / itemsOnPage;
		if (list.size() > pages * itemsOnPage)
		{
			pages++;
		}
		
		if (pageId > pages)
		{
			pageId = pages;
		}
		
		int itemStart = pageId * itemsOnPage;
		int itemEnd = list.size();
		if (itemEnd - itemStart > itemsOnPage)
		{
			itemEnd = itemStart + itemsOnPage;
		}
		
		NpcHtmlMessage npcReply = new NpcHtmlMessage(1);
		TextBuilder reply = new TextBuilder("<html><body>");
		reply.append("<img src=\"l2ui.SquareGray\" width=\"298\" height=\"1\">");
		
		int color = 1;
		for (int i = itemStart; i < itemEnd; i++)
		{
			L2ItemMarketModel mrktItem = list.get(i);
			if (mrktItem == null)
			{
				continue;
			}
			
			if (mrktItem.getOwnerId() == player.getObjectId())
			{
				continue;
			}
			
			int _price = mrktItem.getPrice();
			if (_price == 0)
			{
				continue;
			}
			
			String itemIcon = getItemIcon(mrktItem.getItemId());
			
			if (color == 1)
			{
				reply.append("<table width=300 bgcolor=000000 border=0><tr>");
				reply.append("<td valign=top width=35><button value=\"\" action=\"bypass -h custom_market" + " ItemInfo " + pageId + " " + mask + " " + mrktItem.getItemObjId() + "\" width=32 height=32 back=\"" + itemIcon + "\" fore=\"" + itemIcon + "\"></td>");
				reply.append("<td valign=top width=235>");
				reply.append("<table border=0 width=100%>");
				
				if (mrktItem.getCount() > 1)
				{
					reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo " + pageId + " " + mask + " " + mrktItem.getItemObjId() + "\">" + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + " (" + Util.formatAdena(mrktItem.getCount())
						+ ")</a> </td><td> " + getGrade(mrktItem.getItemGrade()) + "</td></tr>");
				}
				else
				{
					if (mrktItem.getEnchLvl() > 0)
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo " + pageId + " " + mask + " " + mrktItem.getItemObjId() + "\">+" + mrktItem.getEnchLvl() + " " + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName()
							+ "</a> </td><td>  " + getGrade(mrktItem.getItemGrade()) + "</td></tr>");
					}
					else
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo " + pageId + " " + mask + " " + mrktItem.getItemObjId() + "\">" + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + "</a> </td><td> "
							+ getGrade(mrktItem.getItemGrade()) + "</td></tr>");
					}
				}
				
				reply.append("<tr><td><font color=\"A2A0A2\">Price:</font> <font color=\"LEVEL\">" + Util.formatAdena(mrktItem.getPrice()) + " " + getItemNameById(mrktItem.getPriceItem()) + "</font></td><td></td></tr></table></td>");
				reply.append("</tr></table>");
				color = 2;
			}
			else
			{
				reply.append("<table width=300 border=0><tr>");
				reply.append("<td valign=top width=35><button value=\"\" action=\"bypass -h custom_market" + " ItemInfo " + pageId + " " + mask + " " + mrktItem.getItemObjId() + "\" width=32 height=32 back=\"" + itemIcon + "\" fore=\"" + itemIcon + "\"></td>");
				reply.append("<td valign=top width=235>");
				reply.append("<table border=0 width=100%>");
				
				if (mrktItem.getCount() > 1)
				{
					reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo " + pageId + " " + mask + " " + mrktItem.getItemObjId() + "\">" + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + " (" + Util.formatAdena(mrktItem.getCount())
						+ ")</a> </td><td> " + getGrade(mrktItem.getItemGrade()) + "</td></tr>");
				}
				else
				{
					if (mrktItem.getEnchLvl() > 0)
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo " + pageId + " " + mask + " " + mrktItem.getItemObjId() + "\">+" + mrktItem.getEnchLvl() + " " + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName()
							+ "</a> </td><td>  " + getGrade(mrktItem.getItemGrade()) + "</td></tr>");
					}
					else
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo " + pageId + " " + mask + " " + mrktItem.getItemObjId() + "\">" + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + "</a> </td><td> "
							+ getGrade(mrktItem.getItemGrade()) + "</td></tr>");
					}
				}
				
				reply.append("<tr><td><font color=\"A2A0A2\">Price:</font> <font color=\"LEVEL\">" + Util.formatAdena(mrktItem.getPrice()) + " " + getItemNameById(mrktItem.getPriceItem()) + "</font></td><td></td></tr></table></td>");
				reply.append("</tr></table>");
				color = 1;
			}
		}
		
		reply.append("<img src=\"l2ui.SquareGray\" width=\"298\" height=\"1\">");
		reply.append("<table width=100%><tr>");
		reply.append("<td width=66><button value=\"Back\" action=\"bypass -h custom_market" + ((pageId == 0) ? " Main " : " See ") + mask + " " + (pageId - 1) + "\" width=66 height=16 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></td>");
		reply.append("<td width=138></td>");
		reply.append("<td width=66>" + ((pageId + 1 < pages) ? "<button value=\"Next\" action=\"bypass -h custom_market" + " See " + mask + " " + (pageId + 1) + "\" width=66 height=16 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\">" : "") + "</td>");
		reply.append("</tr></table>");
		
		reply.append("</body></html>");
		npcReply.setHtml(reply.toString());
		player.sendPacket(npcReply);
	}
	
	private void showItemInfo(L2ItemMarketModel mrktItem, int mask, int pageId, L2PcInstance player)
	{
		NpcHtmlMessage npcReply = new NpcHtmlMessage(1);
		TextBuilder reply = new TextBuilder("<html><body>");
		reply.append("<center>Item Info</center>");
		reply.append("<table width=100%><tr>");
		reply.append("<td width=66><button value=\"Back\" action=\"bypass -h custom_market" + " See " + mask + " " + pageId + "\" width=66 height=16 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></td>");
		reply.append("<td width=138></td>");
		reply.append("</tr></table>");
		reply.append("<img src=\"l2ui.SquareGray\" width=\"298\" height=\"1\">");
		
		reply.append("<table width=300 border=0 bgcolor=000000><tr>");
		reply.append("<td valign=top width=35><img src=" + getItemIcon(mrktItem.getItemId()) + " width=32 height=32 align=left></td>");
		reply.append("<td valign=top width=235>");
		reply.append("<table border=0 width=100%>");
		
		if (mrktItem.getEnchLvl() > 0)
		{
			reply.append("<tr><td><font color=\"3399ff\">+" + mrktItem.getEnchLvl() + "</font> " + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + " (" + Util.formatAdena(mrktItem.getCount()) + ")</td></tr>");
		}
		else
		{
			reply.append("<tr><td>" + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + " (" + Util.formatAdena(mrktItem.getCount()) + ")</td></tr>");
		}
		
		reply.append("<tr><td><font color=\"A2A0A2\">Price:</font> <font color=\"LEVEL\">" + Util.formatAdena(mrktItem.getPrice()) + " " + getItemNameById(mrktItem.getPriceItem()) + "</font></td></tr>");
		reply.append("<tr><td><font color=\"A2A0A2\"><br1>Seller:</font> <font color=\"B09878\">" + mrktItem.getOwnerName() + "</font></td></tr>");
		
		if (mrktItem.getAugmentationId() > 0)
		{
			reply.append("<tr><td><br><font color=009900>Augmentation effects:</font></td></tr>");
			reply.append("<tr><td>" + mrktItem.getAugmentationBonus() + "</td></tr>");
			L2Skill skill = SkillTable.getInstance().getInfo(mrktItem.getAugmentationSkill(), mrktItem.getAugmentationSkillLevel());
			if (mrktItem.getAugmentationSkill() != 0)
			{
				reply.append("<tr><td>" + skill.getName() + " Level: " + skill.getLevel() + " " + (skill.isActive() ? "(Active)" : "(Passive)") + "</td></tr>");
			}
			else
			{
				reply.append("<tr><td>Item Skill: -</td></tr>");
			}
		}
		
		reply.append("<tr><td><button value=\"Buy\" action=\"bypass -h custom_market" + " BuyItem " + mrktItem.getItemObjId() + " " + mrktItem.getPrice() + "\" width=66 height=16 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></td></tr></table></td>");
		
		reply.append("</tr></table>");
		reply.append("<img src=\"l2ui.SquareGray\" width=\"298\" height=\"1\">");
		reply.append("</body></html>");
		npcReply.setHtml(reply.toString());
		player.sendPacket(npcReply);
	}
	
	private void showPrivateItemList(List<L2ItemMarketModel> list, int pageId, L2PcInstance player)
	{
		int itemsOnPage = ITEMS_PER_PAGE;
		
		if (list == null || list.isEmpty())
		{
			sendMsg("There are no items for you", player);
			return;
		}
		
		int pages = list.size() / itemsOnPage;
		if (list.size() > pages * itemsOnPage)
		{
			pages++;
		}
		
		if (pageId > pages)
		{
			pageId = pages;
		}
		int itemStart = pageId * itemsOnPage;
		int itemEnd = list.size();
		if (itemEnd - itemStart > itemsOnPage)
		{
			itemEnd = itemStart + itemsOnPage;
		}
		
		NpcHtmlMessage npcReply = new NpcHtmlMessage(1);
		TextBuilder reply = new TextBuilder("<html><body>");
		reply.append("<center>Your items in market</center><br>");
		reply.append("<img src=\"l2ui.SquareGray\" width=\"298\" height=\"1\">");
		
		int color = 1;
		for (int i = itemStart; i < itemEnd; i++)
		{
			L2ItemMarketModel mrktItem = list.get(i);
			if (mrktItem == null)
			{
				continue;
			}
			
			int _price = mrktItem.getPrice();
			if (_price == 0)
			{
				continue;
			}
			
			String itemIcon = getItemIcon(mrktItem.getItemId());
			
			if (color == 1)
			{
				reply.append("<table width=300 border=0 bgcolor=000000><tr>");
				reply.append("<td valign=top width=35><button value=\"\" action=\"bypass -h custom_market" + " ItemInfo2 " + pageId + " " + mrktItem.getItemObjId() + "\" width=32 height=32 back=\"" + itemIcon + "\" fore=\"" + itemIcon + "\"></td>");
				reply.append("<td valign=top width=235>");
				reply.append("<table border=0 width=100%>");
				
				if (mrktItem.getCount() > 1)
				{
					reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo2 " + pageId + " " + mrktItem.getItemObjId() + "\">" + mrktItem.getItemName() + " (" + Util.formatAdena(mrktItem.getCount()) + ")</a> </td><td> " + getGrade(mrktItem.getItemGrade()) + "</td></tr>");
				}
				else
				{
					if (mrktItem.getEnchLvl() > 0)
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo2 " + pageId + " " + mrktItem.getItemObjId() + "\">+" + mrktItem.getEnchLvl() + " " + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + "</a> </td><td>  "
							+ getGrade(mrktItem.getItemGrade()) + "</td></tr>");
					}
					else
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo2 " + pageId + " " + mrktItem.getItemObjId() + "\">" + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + "</a> </td><td> " + getGrade(mrktItem.getItemGrade())
							+ "</td></tr>");
					}
				}
				
				reply.append("<tr><td><font color=\"A2A0A2\">Price:</font> <font color=\"LEVEL\">" + Util.formatAdena(mrktItem.getPrice()) + " " + getItemNameById(mrktItem.getPriceItem()) + "</font></td><td></td></tr></table></td>");
				reply.append("</tr></table>");
				color = 2;
			}
			else
			{
				reply.append("<table width=300 border=0><tr>");
				reply.append("<td valign=top width=35><button value=\"\" action=\"bypass -h custom_market" + " ItemInfo2 " + pageId + " " + mrktItem.getItemObjId() + "\" width=32 height=32 back=\"" + itemIcon + "\" fore=\"" + itemIcon + "\"></td>");
				reply.append("<td valign=top width=235>");
				reply.append("<table border=0 width=100%>");
				
				if (mrktItem.getCount() > 1)
				{
					reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo2 " + pageId + " " + mrktItem.getItemObjId() + "\">" + mrktItem.getItemName() + " (" + Util.formatAdena(mrktItem.getCount()) + ")</a> </td><td> " + getGrade(mrktItem.getItemGrade()) + "</td></tr>");
				}
				else
				{
					if (mrktItem.getEnchLvl() > 0)
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo2 " + pageId + " " + mrktItem.getItemObjId() + "\">+" + mrktItem.getEnchLvl() + " " + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + "</a> </td><td>  "
							+ getGrade(mrktItem.getItemGrade()) + "</td></tr>");
					}
					else
					{
						reply.append("<tr><td width=235><a action=\"bypass -h custom_market ItemInfo2 " + pageId + " " + mrktItem.getItemObjId() + "\">" + (mrktItem.getAugmentationId() > 0 ? "Augmented" : "") + " " + mrktItem.getItemName() + "</a> </td><td> " + getGrade(mrktItem.getItemGrade())
							+ "</td></tr>");
					}
				}
				
				reply.append("<tr><td><font color=\"A2A0A2\">Price:</font> <font color=\"LEVEL\">" + Util.formatAdena(mrktItem.getPrice()) + " " + getItemNameById(mrktItem.getPriceItem()) + "</font></td><td></td></tr></table></td>");
				reply.append("</tr></table>");
				color = 1;
			}
		}
		reply.append("<img src=\"l2ui.SquareGray\" width=\"298\" height=\"1\">");
		reply.append("<table width=300><tr>");
		reply.append("<td width=66><button value=\"Back\" action=\"bypass -h custom_market" + ((pageId == 0) ? " Main " : " Private ") + (pageId - 1) + "\" width=66 height=16 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></td>");
		reply.append("<td width=138></td>");
		reply.append("<td width=66>" + ((pageId + 1 < pages) ? "<button value=\"Next\" action=\"bypass -h custom_market" + " Private " + (pageId + 1) + "\" width=66 height=16 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\">" : "") + "</td>");
		reply.append("</tr></table>");
		
		reply.append("</body></html>");
		npcReply.setHtml(reply.toString());
		player.sendPacket(npcReply);
	}
	
	private void sendMsg(String message, L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		NpcHtmlMessage npcReply = new NpcHtmlMessage(1);
		TextBuilder reply = new TextBuilder("<html><body>");
		reply.append(message);
		reply.append("<br><a action=\"bypass -h custom_market " + "Main\">Back</a>");
		reply.append("</body></html>");
		npcReply.setHtml(reply.toString());
		player.sendPacket(npcReply);
	}
	
	private void showItemInfo2(L2ItemMarketModel mrktItem, int pageId, L2PcInstance player)
	{
		NpcHtmlMessage npcReply = new NpcHtmlMessage(1);
		TextBuilder reply = new TextBuilder("<html><body>");
		reply.append("<center>Info</center>");
		
		reply.append("<table width=300><tr>");
		reply.append("<td width=66><button value=\"Back\" action=\"bypass -h custom_market" + " Private " + pageId + "\" width=66 height=16 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></td>");
		reply.append("<td width=138></td>");
		reply.append("</tr></table>");
		reply.append("<img src=\"l2ui.SquareGray\" width=\"298\" height=\"1\">");
		
		reply.append("<table width=300 border=0 bgcolor=000000><tr>");
		reply.append("<td valign=top width=35><img src=" + getItemIcon(mrktItem.getItemId()) + " width=32 height=32 align=left></td>");
		reply.append("<td valign=top width=235>");
		reply.append("<table border=0 width=100%>");
		
		if (mrktItem.getEnchLvl() > 0)
		{
			reply.append("<tr><td><font color=\"B09878\">+" + mrktItem.getEnchLvl() + "</font> " + mrktItem.getItemName() + " (" + Util.formatAdena(mrktItem.getCount()) + ")</td></tr>");
		}
		else
		{
			reply.append("<tr><td>" + mrktItem.getItemName() + " (" + Util.formatAdena(mrktItem.getCount()) + ")</td></tr>");
		}
		
		reply.append("<tr><td><font color=\"A2A0A2\">Price:</font> <font color=\"LEVEL\">" + Util.formatAdena(mrktItem.getPrice()) + " " + getItemNameById(mrktItem.getPriceItem()) + "</font></td></tr>");
		reply.append("<tr><td><button value=\"Remove\" action=\"bypass -h custom_market" + " TakeItem " + mrktItem.getItemObjId() + "\" width=66 height=16 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></td></tr></table></td>");
		reply.append("</tr></table>");
		reply.append("<img src=\"l2ui.SquareGray\" width=\"298\" height=\"1\">");
		reply.append("</body></html>");
		npcReply.setHtml(reply.toString());
		player.sendPacket(npcReply);
	}
	
	private String getGrade(int grade)
	{
		switch (grade)
		{
			case D_G:
				return "<img src=\"symbol.grade_d\" width=\"12\" height=\"12\">";
			case C_G:
				return "<img src=\"symbol.grade_c\" width=\"12\" height=\"12\">";
			case B_G:
				return "<img src=\"symbol.grade_b\" width=\"12\" height=\"12\">";
			case A_G:
				return "<img src=\"symbol.grade_a\" width=\"12\" height=\"12\">";
			case S_G:
				return "<img src=\"symbol.grade_s\" width=\"12\" height=\"12\">";
			default:
				return "";
		}
	}
	
	private String getGradeByCrystal(String grade)
	{
		switch (grade)
		{
			case "1":
				return "<img src=\"symbol.grade_d\" width=\"12\" height=\"12\">";
			case "2":
				return "<img src=\"symbol.grade_c\" width=\"12\" height=\"12\">";
			case "3":
				return "<img src=\"symbol.grade_b\" width=\"12\" height=\"12\">";
			case "4":
				return "<img src=\"symbol.grade_a\" width=\"12\" height=\"12\">";
			case "5":
				return "<img src=\"symbol.grade_s\" width=\"12\" height=\"12\">";
			default:
				return "";
		}
	}
	
	public void addItemForOffliner(int ownerId, int objectId, int itemId, int itemCount)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO items Values (?,?,?,?,?,?,?,?,?,?,?,?,?)");
			
			statement.setInt(1, ownerId);
			statement.setInt(2, objectId);
			statement.setInt(3, itemId);
			statement.setInt(4, itemCount);
			statement.setInt(5, 0);// enchant level
			statement.setString(6, "INVENTORY");
			statement.setInt(7, 0);
			statement.setInt(8, 0);
			statement.setInt(9, 0);
			statement.setInt(10, 0);
			statement.setInt(11, 0);
			statement.setInt(12, 0);
			statement.setInt(13, -1);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOG.error("Error while adding a new time into DB " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
		}
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

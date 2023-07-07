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

import java.util.ArrayList;
import java.util.StringTokenizer;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.controllers.TradeController;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.CastleManorManager;
import l2jorion.game.managers.CastleManorManager.SeedProduction;
import l2jorion.game.model.L2TradeList;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.BuyList;
import l2jorion.game.network.serverpackets.BuyListSeed;
import l2jorion.game.network.serverpackets.ExShowCropInfo;
import l2jorion.game.network.serverpackets.ExShowManorDefaultInfo;
import l2jorion.game.network.serverpackets.ExShowProcureCropDetail;
import l2jorion.game.network.serverpackets.ExShowSeedInfo;
import l2jorion.game.network.serverpackets.ExShowSellCropList;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class L2ManorManagerInstance extends L2MerchantInstance
{
	
	private static Logger LOG = LoggerFactory.getLogger(L2ManorManagerInstance.class);
	
	public L2ManorManagerInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		player.setLastFolkNPC(this);
		
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
				
				// If player is a lord of this manor, alternative message from NPC
				if (CastleManorManager.getInstance().isDisabled())
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/npcdefault.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
				}
				else if (!player.isGM() // Player is not GM
					&& getCastle() != null && getCastle().getCastleId() > 0 // Verification of castle
					&& player.getClan() != null // Player have clan
					&& getCastle().getOwnerId() == player.getClanId() // Player's clan owning the castle
					&& player.isClanLeader() // Player is clan leader of clan (then he is the lord)
				)
				{
					showMessageWindow(player, "manager-lord.htm");
				}
				else
				{
					showMessageWindow(player, "manager.htm");
				}
			}
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void showBuyWindow(final L2PcInstance player, final String val)
	{
		final double taxRate = 0;
		player.tempInvetoryDisable();
		
		L2TradeList list = TradeController.getInstance().getBuyList(Integer.parseInt(val));
		
		if (list != null)
		{
			list.getItems().get(0).setCount(1);
			final BuyList bl = new BuyList(list, player.getAdena(), taxRate);
			player.sendPacket(bl);
		}
		else
		{
			LOG.info("possible client hacker: " + player.getName() + " attempting to buy from GM shop! (L2ManorManagerIntance)");
			LOG.info("buylist id:" + val);
		}
		
		list = null;
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		if (player.getLastFolkNPC() == null || player.getLastFolkNPC().getObjectId() != getObjectId())
		{
			return;
		}
		
		if (command.startsWith("manor_menu_select"))
		{
			// input string format:
			// manor_menu_select?ask=X&state=Y&time=X
			
			if (CastleManorManager.getInstance().isUnderMaintenance())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.sendPacket(new SystemMessage(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE));
				return;
			}
			
			String params = command.substring(command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			final int ask = Integer.parseInt(st.nextToken().split("=")[1]);
			final int state = Integer.parseInt(st.nextToken().split("=")[1]);
			final int time = Integer.parseInt(st.nextToken().split("=")[1]);
			
			int castleId;
			if (state == -1)
			{
				castleId = getCastle().getCastleId();
			}
			else
			{
				// info for requested manor
				castleId = state;
			}
			
			switch (ask)
			{ // Main action
				case 1: // Seed purchase
					if (castleId != getCastle().getCastleId())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.HERE_YOU_CAN_BUY_ONLY_SEEDS_OF_S1_MANOR));
					}
					else
					{
						L2TradeList tradeList = new L2TradeList(0);
						ArrayList<SeedProduction> seeds = getCastle().getSeedProduction(CastleManorManager.PERIOD_CURRENT);
						
						for (final SeedProduction s : seeds)
						{
							final L2ItemInstance item = ItemTable.getInstance().createDummyItem(s.getId());
							int price = s.getPrice();
							if (price < (item.getReferencePrice() / 2))
							{
								price = item.getReferencePrice();
							}
							
							item.setPriceToSell(price);
							item.setCount(s.getCanProduce());
							if (item.getCount() > 0 && item.getPriceToSell() > 0)
							{
								tradeList.addItem(item);
							}
						}
						
						BuyListSeed bl = new BuyListSeed(tradeList, castleId, player.getAdena());
						player.sendPacket(bl);
					}
					break;
				case 2: // Crop sales
					player.sendPacket(new ExShowSellCropList(player, castleId, getCastle().getCropProcure(CastleManorManager.PERIOD_CURRENT)));
					break;
				case 3: // Current seeds (Manor info)
					if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
					{
						player.sendPacket(new ExShowSeedInfo(castleId, null));
					}
					else
					{
						player.sendPacket(new ExShowSeedInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getSeedProduction(time)));
					}
					break;
				case 4: // Current crops (Manor info)
					if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
					{
						player.sendPacket(new ExShowCropInfo(castleId, null));
					}
					else
					{
						player.sendPacket(new ExShowCropInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getCropProcure(time)));
					}
					break;
				case 5: // Basic info (Manor info)
					player.sendPacket(new ExShowManorDefaultInfo());
					break;
				case 6: // Buy harvester
					showBuyWindow(player, "3" + getNpcId());
					break;
				case 9: // Edit sales (Crop sales)
					player.sendPacket(new ExShowProcureCropDetail(state));
					break;
			}
			params = null;
			st = null;
		}
		else if (command.startsWith("help"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // discard first
			String filename = "manor_client_help00" + st.nextToken() + ".htm";
			showMessageWindow(player, filename);
			st = null;
			filename = null;
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public String getHtmlPath()
	{
		return "data/html/manormanager/";
	}
	
	@Override
	public String getHtmlPath(L2PcInstance player, final int npcId, final int val)
	{
		return "data/html/manormanager/manager.htm"; // Used only in parent method
		// to return from "Territory status"
		// to initial screen.
	}
	
	private void showMessageWindow(final L2PcInstance player, final String filename)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(getHtmlPath() + filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
		html = null;
	}
}

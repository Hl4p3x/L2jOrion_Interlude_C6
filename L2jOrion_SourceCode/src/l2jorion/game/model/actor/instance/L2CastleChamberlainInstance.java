/* L2jOrion Project - www.l2jorion.com 
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

import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.controllers.TradeController;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.CastleManorManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2TradeList;
import l2jorion.game.model.PcInventory;
import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.BuyList;
import l2jorion.game.network.serverpackets.ExShowCropInfo;
import l2jorion.game.network.serverpackets.ExShowCropSetting;
import l2jorion.game.network.serverpackets.ExShowManorDefaultInfo;
import l2jorion.game.network.serverpackets.ExShowSeedInfo;
import l2jorion.game.network.serverpackets.ExShowSeedSetting;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.util.Broadcast;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class L2CastleChamberlainInstance extends L2FolkInstance
{
	private static Logger LOG = LoggerFactory.getLogger(L2CastleChamberlainInstance.class);
	
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;
	protected static final int COND_CLAN_MEMBER = 3;
	
	private int _preHour = 6;
	
	public L2CastleChamberlainInstance(final int objectId, final L2NpcTemplate template)
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
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			my = null;
			
			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				// Like L2OFF player must rotate to the Npc
				MoveToPawn sp = new MoveToPawn(player, this, L2NpcInstance.INTERACTION_DISTANCE);
				player.sendPacket(sp);
				Broadcast.toKnownPlayers(player, sp);
				
				showMessageWindow(player);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		// BypassValidation Exploit plug.
		if (player.getLastFolkNPC().getObjectId() != getObjectId())
		{
			return;
		}
		
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command
		
		final int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE)
		{
			return;
		}
		
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			return;
		}
		else if (condition == COND_OWNER)
		{
			String val = "";
			if (st.countTokens() >= 1)
			{
				val = st.nextToken();
			}
			
			if (actualCommand.equalsIgnoreCase("banish_foreigner"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_DISMISS) == L2Clan.CP_CS_DISMISS)
				{
					getCastle().banishForeigners(); // Move non-clan members off castle area
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("list_siege_clans"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_MANAGE_SIEGE) == L2Clan.CP_CS_MANAGE_SIEGE)
				{
					getCastle().getSiege().listRegisterClan(player); // List current register clan
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("receive_report"))
			{
				if (player.isClanLeader())
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain-report.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					final L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
					html.replace("%clanname%", clan.getName());
					html.replace("%clanleadername%", clan.getLeaderName());
					html.replace("%castlename%", getCastle().getName());
					{
						final int currentPeriod = SevenSigns.getInstance().getCurrentPeriod();
						switch (currentPeriod)
						{
							case SevenSigns.PERIOD_COMP_RECRUITING:
								html.replace("%ss_event%", "Quest Event Initialization");
								break;
							case SevenSigns.PERIOD_COMPETITION:
								html.replace("%ss_event%", "Competition (Quest Event)");
								break;
							case SevenSigns.PERIOD_COMP_RESULTS:
								html.replace("%ss_event%", "Quest Event Results");
								break;
							case SevenSigns.PERIOD_SEAL_VALIDATION:
								html.replace("%ss_event%", "Seal Validation");
								break;
						}
					}
					{
						final int sealOwner1 = SevenSigns.getInstance().getSealOwner(1);
						switch (sealOwner1)
						{
							case SevenSigns.CABAL_NULL:
								html.replace("%ss_avarice%", "Not in Possession");
								break;
							case SevenSigns.CABAL_DAWN:
								html.replace("%ss_avarice%", "Lords of Dawn");
								break;
							case SevenSigns.CABAL_DUSK:
								html.replace("%ss_avarice%", "Revolutionaries of Dusk");
								break;
						}
					}
					{
						final int sealOwner2 = SevenSigns.getInstance().getSealOwner(2);
						switch (sealOwner2)
						{
							case SevenSigns.CABAL_NULL:
								html.replace("%ss_gnosis%", "Not in Possession");
								break;
							case SevenSigns.CABAL_DAWN:
								html.replace("%ss_gnosis%", "Lords of Dawn");
								break;
							case SevenSigns.CABAL_DUSK:
								html.replace("%ss_gnosis%", "Revolutionaries of Dusk");
								break;
						}
					}
					{
						final int sealOwner3 = SevenSigns.getInstance().getSealOwner(3);
						switch (sealOwner3)
						{
							case SevenSigns.CABAL_NULL:
								html.replace("%ss_strife%", "Not in Possession");
								break;
							case SevenSigns.CABAL_DAWN:
								html.replace("%ss_strife%", "Lords of Dawn");
								break;
							case SevenSigns.CABAL_DUSK:
								html.replace("%ss_strife%", "Revolutionaries of Dusk");
								break;
						}
					}
					player.sendPacket(html);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("items"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_USE_FUNCTIONS) == L2Clan.CP_CS_USE_FUNCTIONS)
				{
					if (val == "")
					{
						return;
					}
					
					player.tempInvetoryDisable();
					
					player.setTempAccessBuy(false);
					
					if (Config.DEBUG)
					{
						LOG.debug("Showing chamberlain buylist");
					}
					int buy;
					{
						final int castleId = getCastle().getCastleId();
						final int circlet = CastleManager.getInstance().getCircletByCastleId(castleId);
						final PcInventory s = player.getInventory();
						if (s.getItemByItemId(circlet) == null)
						{
							buy = Integer.parseInt(val + "1");
						}
						else
						{
							buy = Integer.parseInt(val + "2");
						}
					}
					L2TradeList list = TradeController.getInstance().getBuyList(buy);
					if (list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
					{
						final BuyList bl = new BuyList(list, player.getAdena(), 0);
						player.sendPacket(bl);
					}
					else
					{
						LOG.warn("player: " + player.getName() + " attempting to buy from chamberlain that don't have buylist!");
						LOG.warn("buylist id:" + buy);
					}
					list = null;
					
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					html = null;
					return;
				}
			}
			else if (actualCommand.equalsIgnoreCase("manage_vault"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_TAXES) == L2Clan.CP_CS_TAXES)
				{
					String filename = "data/html/chamberlain/chamberlain-vault.htm";
					int amount = 0;
					if (val.equalsIgnoreCase("deposit"))
					{
						try
						{
							amount = Integer.parseInt(st.nextToken());
						}
						catch (final NoSuchElementException e)
						{
							if (Config.ENABLE_ALL_EXCEPTIONS)
							{
								e.printStackTrace();
							}
						}
						if (amount > 0 && (long) getCastle().getTreasury() + amount < Integer.MAX_VALUE)
						{
							if (player.reduceAdena("Castle", amount, this, true))
							{
								getCastle().addToTreasuryNoTax(amount);
							}
							else
							{
								sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
							}
						}
					}
					else if (val.equalsIgnoreCase("withdraw"))
					{
						try
						{
							amount = Integer.parseInt(st.nextToken());
						}
						catch (final NoSuchElementException e)
						{
							if (Config.ENABLE_ALL_EXCEPTIONS)
							{
								e.printStackTrace();
							}
						}
						if (amount > 0)
						{
							if (getCastle().getTreasury() < amount)
							{
								filename = "data/html/chamberlain/chamberlain-vault-no.htm";
							}
							else
							{
								if (getCastle().addToTreasuryNoTax(-1 * amount))
								{
									player.addAdena("Castle", amount, this, true);
								}
							}
						}
					}
					
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					html.replace("%tax_income%", Util.formatAdena(getCastle().getTreasury()));
					html.replace("%withdraw_amount%", Util.formatAdena(amount));
					player.sendPacket(html);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manor"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_MANOR_ADMIN) == L2Clan.CP_CS_MANOR_ADMIN)
				{
					String filename = "";
					if (CastleManorManager.getInstance().isDisabled())
					{
						filename = "data/html/npcdefault.htm";
					}
					else
					{
						final int cmd = Integer.parseInt(val);
						switch (cmd)
						{
							case 0:
								filename = "data/html/chamberlain/manor/manor.htm";
								break;
							case 4:
								filename = "data/html/chamberlain/manor/manor_help00" + st.nextToken() + ".htm";
								break;
							default:
								filename = "data/html/chamberlain/chamberlain-no.htm";
								break;
						}
					}
					if (filename.length() != 0)
					{
						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(filename);
						html.replace("%objectId%", String.valueOf(getObjectId()));
						html.replace("%npcname%", getName());
						player.sendPacket(html);
						html = null;
						filename = null;
					}
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
				return;
			}
			else if (command.startsWith("manor_menu_select"))
			{// input string format:
				// manor_menu_select?ask=X&state=Y&time=X
				if ((player.getClanPrivileges() & L2Clan.CP_CS_MANOR_ADMIN) == L2Clan.CP_CS_MANOR_ADMIN)
				{
					if (CastleManorManager.getInstance().isUnderMaintenance())
					{
						player.sendPacket(ActionFailed.STATIC_PACKET);
						player.sendPacket(new SystemMessage(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE));
						return;
					}
					
					String params = command.substring(command.indexOf("?") + 1);
					StringTokenizer str = new StringTokenizer(params, "&");
					final int ask = Integer.parseInt(str.nextToken().split("=")[1]);
					final int state = Integer.parseInt(str.nextToken().split("=")[1]);
					final int time = Integer.parseInt(str.nextToken().split("=")[1]);
					
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
						case 7: // Edit seed setup
							if (getCastle().isNextPeriodApproved())
							{
								player.sendPacket(new SystemMessage(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
							}
							else
							{
								player.sendPacket(new ExShowSeedSetting(getCastle().getCastleId()));
							}
							break;
						case 8: // Edit crop setup
							if (getCastle().isNextPeriodApproved())
							{
								player.sendPacket(new SystemMessage(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
							}
							else
							{
								player.sendPacket(new ExShowCropSetting(getCastle().getCastleId()));
							}
							break;
					}
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
			}
			else if (actualCommand.equalsIgnoreCase("operate_door")) // door control
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_OPEN_DOOR) == L2Clan.CP_CS_OPEN_DOOR)
				{
					if (!val.isEmpty())
					{
						final boolean open = Integer.parseInt(val) == 1;
						while (st.hasMoreTokens())
						{
							getCastle().openCloseDoor(player, Integer.parseInt(st.nextToken()), open);
						}
					}
					
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/" + getTemplate().npcId + "-d.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("tax_set")) // tax rates control
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CS_TAXES) == L2Clan.CP_CS_TAXES)
				{
					if (!val.isEmpty())
					{
						getCastle().setTaxPercent(player, Integer.parseInt(val));
					}
					
					TextBuilder msg = new TextBuilder("<html><body>");
					msg.append(getName() + ":<br>");
					msg.append("Current tax rate: " + getCastle().getTaxPercent() + "%<br>");
					msg.append("<table>");
					msg.append("<tr>");
					msg.append("<td>Change tax rate to:</td>");
					msg.append("<td><edit var=\"value\" width=40><br>");
					msg.append("<button value=\"Adjust\" action=\"bypass -h npc_%objectId%_tax_set $value\" width=80 height=15></td>");
					msg.append("</tr>");
					msg.append("</table>");
					msg.append("</center>");
					msg.append("</body></html>");
					
					sendHtmlMessage(player, msg.toString());
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain-tax.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%tax%", String.valueOf(getCastle().getTaxPercent()));
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("return"))
			{
				sendFileMessage(player, "data/html/chamberlain/chamberlain.htm");
			}
			else if (actualCommand.equalsIgnoreCase("manage_functions"))
			{
				if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				{
					return;
				}
				sendFileMessage(player, "data/html/chamberlain/manage.htm");
			}
			else if (actualCommand.equalsIgnoreCase("castle_devices"))
			{
				if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				{
					return;
				}
				
				sendFileMessage(player, "data/html/chamberlain/devices.htm");
			}
			else if (actualCommand.equalsIgnoreCase("doors_update"))
			{
				if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				{
					return;
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (val.isEmpty())
				{
					html.setFile("data/html/chamberlain/" + getNpcId() + "-gu.htm");
				}
				else
				{
					html.setFile("data/html/chamberlain/doors-update.htm");
					html.replace("%id%", val);
					html.replace("%type%", st.nextToken());
				}
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
			}
			else if (actualCommand.equalsIgnoreCase("doors_choose_upgrade"))
			{
				if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				{
					return;
				}
				
				final String id = val;
				final String type = st.nextToken();
				final String level = st.nextToken();
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/doors-confirm.htm");
				html.replace("%objectId%", getObjectId());
				html.replace("%id%", id);
				html.replace("%level%", level);
				html.replace("%type%", type);
				html.replace("%price%", getDoorCost(Integer.parseInt(type), Integer.parseInt(level)));
				player.sendPacket(html);
			}
			else if (actualCommand.equalsIgnoreCase("doors_confirm_upgrade"))
			{
				if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				{
					return;
				}
				
				final int type = Integer.parseInt(st.nextToken());
				final int level = Integer.parseInt(st.nextToken());
				final int price = getDoorCost(type, level);
				
				if (price == 0)
				{
					return;
				}
				
				final int id = Integer.parseInt(val);
				final L2DoorInstance door = getCastle().getDoor(id);
				if (door == null)
				{
					return;
				}
				
				final int currentHpRatio = door.getStat().getUpgradeHpRatio();
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				
				if (currentHpRatio >= level)
				{
					html.setFile("data/html/chamberlain/doors-already-updated.htm");
					html.replace("%level%", currentHpRatio * 100);
				}
				else if (!player.reduceAdena("doors_upgrade", price, player, true))
				{
					html.setFile("data/html/chamberlain/not-enough-adena.htm");
				}
				else
				{
					getCastle().upgradeDoor(id, level, true);
					
					html.setFile("data/html/chamberlain/doors-success.htm");
				}
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
			}
			else if (actualCommand.equalsIgnoreCase("traps_update"))
			{
				if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				{
					return;
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (val.isEmpty())
				{
					html.setFile("data/html/chamberlain/" + getNpcId() + "-tu.htm");
				}
				else
				{
					html.setFile("data/html/chamberlain/traps-update" + ((getCastle().getName().equalsIgnoreCase("aden")) ? "1" : "") + ".htm");
					html.replace("%trapIndex%", val);
				}
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
			}
			else if (actualCommand.equalsIgnoreCase("traps_choose_upgrade"))
			{
				if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				{
					return;
				}
				
				final String trapIndex = val;
				final String level = st.nextToken();
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/traps-confirm.htm");
				html.replace("%objectId%", getObjectId());
				html.replace("%trapIndex%", trapIndex);
				html.replace("%level%", level);
				html.replace("%price%", getTrapCost(Integer.parseInt(level)));
				player.sendPacket(html);
			}
			else if (actualCommand.equalsIgnoreCase("traps_confirm_upgrade"))
			{
				if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				{
					return;
				}
				
				final int level = Integer.parseInt(st.nextToken());
				final int price = getTrapCost(level);
				
				if (price == 0)
				{
					return;
				}
				
				final int trapIndex = Integer.parseInt(val);
				final int currentLevel = getCastle().getTrapUpgradeLevel(trapIndex);
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				
				if (currentLevel >= level)
				{
					html.setFile("data/html/chamberlain/traps-already-updated.htm");
					html.replace("%level%", currentLevel);
				}
				else if (!player.reduceAdena("traps_upgrade", price, player, true))
				{
					html.setFile("data/html/chamberlain/not-enough-adena.htm");
				}
				else
				{
					getCastle().setTrapUpgrade(trapIndex, level, true);
					
					html.setFile("data/html/chamberlain/traps-success.htm");
				}
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
			}
			else if (actualCommand.equalsIgnoreCase("siege_change")) // set siege time
			{
				if (!validatePrivileges(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					return;
				}
				
				if (getCastle().getSiege().getSiegeRegistrationEndDate() < Calendar.getInstance().getTimeInMillis())
				{
					sendFileMessage(player, "data/html/chamberlain/siegetime1.htm");
				}
				else if (getCastle().getSiege().isTimeRegistrationOver())
				{
					sendFileMessage(player, "data/html/chamberlain/siegetime2.htm");
				}
				else
				{
					sendFileMessage(player, "data/html/chamberlain/siegetime3.htm");
				}
			}
			else if (actualCommand.equalsIgnoreCase("siege_time_set")) // set preDay
			{
				switch (Integer.parseInt(val))
				{
					case 1:
						_preHour = Integer.parseInt(st.nextToken());
						break;
					
					default:
						break;
				}
				
				if (_preHour != 6)
				{
					getCastle().getSiegeDate().set(Calendar.HOUR_OF_DAY, _preHour + 12);
					
					// now store the changed time and finished next Siege Time registration
					getCastle().getSiege().endTimeRegistration(false);
					sendFileMessage(player, "data/html/chamberlain/siegetime8.htm");
					return;
				}
				
				sendFileMessage(player, "data/html/chamberlain/siegetime6.htm");
			}
		}
		super.onBypassFeedback(player, command);
	}
	
	private void sendHtmlMessage(final L2PcInstance player, final String htmlMessage)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setHtml(htmlMessage);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		html.replace("%time%", getCastle().getSiegeDate().getTime().toString());
		player.sendPacket(html);
	}
	
	private void sendFileMessage(L2PcInstance player, String htmlMessage)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(htmlMessage);
		html.replace("%objectId%", getObjectId());
		html.replace("%npcId%", getNpcId());
		html.replace("%npcname%", getName());
		html.replace("%time%", getCastle().getSiegeDate().getTime().toString());
		player.sendPacket(html);
	}
	
	private void showMessageWindow(final L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/chamberlain/chamberlain-no.htm";
		
		final int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			{
				filename = "data/html/chamberlain/chamberlain-busy.htm"; // Busy because of siege
			}
			else if (condition == COND_OWNER)
			{
				filename = "data/html/chamberlain/chamberlain.htm"; // Owner message window
			}
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	protected int validateCondition(final L2PcInstance player)
	{
		if (getCastle() != null && player.getClan() != null)
		{
			if (getCastle().getSiege().getIsInProgress())
			{
				return COND_BUSY_BECAUSE_OF_SIEGE;
			}
			
			if (getCastle().getOwnerId() == player.getClanId())
			{
				if (player.isClanLeader())
				{
					return COND_OWNER;
				}
				
				return COND_CLAN_MEMBER;
			}
		}
		return COND_ALL_FALSE;
	}
	
	private boolean validatePrivileges(L2PcInstance player, int privilege)
	{
		if ((player.getClanPrivileges() & privilege) != privilege)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/chamberlain/noprivs.htm");
			player.sendPacket(html);
			return false;
		}
		return true;
	}
	
	/**
	 * Retrieve the price of the door, following its type, required level of upgrade and current Seven Signs state.
	 * @param type : The type of doors (1: normal gates, 2: metallic gates, 3: walls).
	 * @param level : The required level of upgrade (x2, x3 or x5 HPs).
	 * @return The price modified by Seal of Strife state (-20% if Dawn is winning, x3 if Dusk is winning).
	 */
	private static int getDoorCost(int type, int level)
	{
		int price = 0;
		
		switch (type)
		{
			case 1:
				switch (level)
				{
					case 2:
						price = 300000;
						break;
					case 3:
						price = 400000;
						break;
					case 5:
						price = 500000;
						break;
				}
				break;
			
			case 2:
				switch (level)
				{
					case 2:
						price = 750000;
						break;
					case 3:
						price = 900000;
						break;
					case 5:
						price = 1000000;
						break;
				}
				break;
			
			case 3:
				switch (level)
				{
					case 2:
						price = 1600000;
						break;
					case 3:
						price = 1800000;
						break;
					case 5:
						price = 2000000;
						break;
				}
				break;
		}
		
		switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_DUSK:
				price *= 3;
				break;
			
			case SevenSigns.CABAL_DAWN:
				price *= 0.8;
				break;
		}
		
		return price;
	}
	
	private static int getTrapCost(int level)
	{
		int price = 0;
		
		switch (level)
		{
			case 1:
				price = 3000000;
				break;
			
			case 2:
				price = 4000000;
				break;
			
			case 3:
				price = 5000000;
				break;
			
			case 4:
				price = 6000000;
				break;
		}
		
		switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_DUSK:
				price *= 3;
				break;
			
			case SevenSigns.CABAL_DAWN:
				price *= 0.8;
				break;
		}
		
		return price;
	}
}

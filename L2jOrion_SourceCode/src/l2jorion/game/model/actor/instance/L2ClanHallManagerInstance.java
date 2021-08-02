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

import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.controllers.TradeController;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.TeleportLocationTable;
import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.L2TeleportLocation;
import l2jorion.game.model.L2TradeList;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.BuyList;
import l2jorion.game.network.serverpackets.ClanHallDecoration;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.network.serverpackets.WareHouseDepositList;
import l2jorion.game.network.serverpackets.WareHouseWithdrawalList;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.util.Broadcast;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class L2ClanHallManagerInstance extends L2FolkInstance
{
	private static Logger LOG = LoggerFactory.getLogger(L2ClanHallManagerInstance.class);
	
	protected static final int COND_OWNER_FALSE = 0;
	protected static final int COND_ALL_FALSE = 1;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 2;
	protected static final int COND_OWNER = 3;
	
	private int _clanHallId = -1;
	
	public L2ClanHallManagerInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		final int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE)
		{
			return;
		}
		else if (condition == COND_OWNER)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command
			String val = "";
			if (st.countTokens() >= 1)
			{
				val = st.nextToken();
			}
			
			if (actualCommand.equalsIgnoreCase("banish_foreigner"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CH_DISMISS) == L2Clan.CP_CH_DISMISS)
				{
					if (val.equalsIgnoreCase("list"))
					{
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/clanHallManager/banish-list.htm");
						sendHtmlMessage(player, html);
					}
					else if (val.equalsIgnoreCase("banish"))
					{
						getClanHall().banishForeigners();
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/clanHallManager/banish.htm");
						sendHtmlMessage(player, html);
					}
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile("data/html/clanHallManager/not_authorized.htm");
					sendHtmlMessage(player, html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manage_vault"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) == L2Clan.CP_CL_VIEW_WAREHOUSE)
				{
					if (val.equalsIgnoreCase("deposit"))
					{
						showVaultWindowDeposit(player);
					}
					else if (val.equalsIgnoreCase("withdraw"))
					{
						showVaultWindowWithdraw(player);
					}
					else
					{
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/clanHallManager/vault.htm");
						sendHtmlMessage(player, html);
					}
				}
				else
				{
					player.sendMessage("You are not authorized to do this!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("door"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CH_OPEN_DOOR) == L2Clan.CP_CH_OPEN_DOOR)
				{
					if (val.equalsIgnoreCase("open"))
					{
						getClanHall().openCloseDoors(true);
					}
					else if (val.equalsIgnoreCase("close"))
					{
						getClanHall().openCloseDoors(false);
					}
					else
					{
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/clanHallManager/door.htm");
						sendHtmlMessage(player, html);
						html = null;
					}
				}
				else
				{
					player.sendMessage("You are not authorized to do this!");
				}
			}
			else if (actualCommand.equalsIgnoreCase("functions"))
			{
				if (val.equalsIgnoreCase("tele"))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(1);
					if (getClanHall().getFunction(ClanHall.FUNC_TELEPORT) == null)
					{
						html.setFile("data/html/clanHallManager/chamberlain-nac.htm");
					}
					else
					{
						html.setFile("data/html/clanHallManager/tele" + getClanHall().getLocation() + getClanHall().getFunction(ClanHall.FUNC_TELEPORT).getLvl() + ".htm");
					}
					sendHtmlMessage(player, html);
				}
				else if (val.equalsIgnoreCase("item_creation"))
				{
					if (getClanHall().getFunction(ClanHall.FUNC_ITEM_CREATE) == null)
					{
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/clanHallManager/chamberlain-nac.htm");
						sendHtmlMessage(player, html);
						return;
					}
					
					if (st.countTokens() < 1)
					{
						return;
					}
					
					player.setTempAccessBuy(false);
					
					int valbuy = Integer.parseInt(st.nextToken()) + getClanHall().getFunction(ClanHall.FUNC_ITEM_CREATE).getLvl() * 100000;
					showBuyWindow(player, valbuy);
				}
				else if (val.equalsIgnoreCase("support"))
				{
					
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					if (getClanHall().getFunction(ClanHall.FUNC_SUPPORT) == null)
					{
						html.setFile("data/html/clanHallManager/chamberlain-nac.htm");
					}
					else
					{
						html.setFile("data/html/clanHallManager/support" + getClanHall().getFunction(ClanHall.FUNC_SUPPORT).getLvl() + ".htm");
						html.replace("%mp%", String.valueOf(getCurrentMp()));
					}
					sendHtmlMessage(player, html);
					html = null;
				}
				else if (val.equalsIgnoreCase("back"))
				{
					showMessageWindow(player);
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile("data/html/clanHallManager/functions.htm");
					if (getClanHall().getFunction(ClanHall.FUNC_RESTORE_EXP) != null)
					{
						html.replace("%xp_regen%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl()) + "%");
					}
					else
					{
						html.replace("%xp_regen%", "0");
					}
					if (getClanHall().getFunction(ClanHall.FUNC_RESTORE_HP) != null)
					{
						html.replace("%hp_regen%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_HP).getLvl()) + "%");
					}
					else
					{
						html.replace("%hp_regen%", "0");
					}
					if (getClanHall().getFunction(ClanHall.FUNC_RESTORE_MP) != null)
					{
						html.replace("%mp_regen%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_MP).getLvl()) + "%");
					}
					else
					{
						html.replace("%mp_regen", "0");
					}
					sendHtmlMessage(player, html);
				}
			}
			else if (actualCommand.equalsIgnoreCase("manage"))
			{
				if ((player.getClanPrivileges() & L2Clan.CP_CH_SET_FUNCTIONS) == L2Clan.CP_CH_SET_FUNCTIONS)
				{
					if (val.equalsIgnoreCase("recovery"))
					{
						if (st.countTokens() >= 1)
						{
							if (getClanHall().getOwnerId() == 0)
							{
								player.sendMessage("This clan Hall have no owner, you cannot change configuration");
								return;
							}
							val = st.nextToken();
							if (val.equalsIgnoreCase("hp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									val = st.nextToken();
									final int percent = Integer.valueOf(val);
									switch (percent)
									{
										case 0:
											fee = 0;
											break;
										case 20:
											fee = Config.CH_HPREG1_FEE;
											break;
										case 40:
											fee = Config.CH_HPREG2_FEE;
											break;
										case 80:
											fee = Config.CH_HPREG3_FEE;
											break;
										case 100:
											fee = Config.CH_HPREG4_FEE;
											break;
										case 120:
											fee = Config.CH_HPREG5_FEE;
											break;
										case 140:
											fee = Config.CH_HPREG6_FEE;
											break;
										case 160:
											fee = Config.CH_HPREG7_FEE;
											break;
										case 180:
											fee = Config.CH_HPREG8_FEE;
											break;
										case 200:
											fee = Config.CH_HPREG9_FEE;
											break;
										case 220:
											fee = Config.CH_HPREG10_FEE;
											break;
										case 240:
											fee = Config.CH_HPREG11_FEE;
											break;
										case 260:
											fee = Config.CH_HPREG12_FEE;
											break;
										default:
											fee = Config.CH_HPREG13_FEE;
											break;
									}
									if (!getClanHall().updateFunctions(ClanHall.FUNC_RESTORE_HP, percent, fee, Config.CH_HPREG_FEE_RATIO, (getClanHall().getFunction(ClanHall.FUNC_RESTORE_HP) == null)))
									{
										player.sendMessage("You don't have enough adena in your clan's warehouse");
									}
									else
									{
										revalidateDeco(player);
									}
								}
							}
							else if (val.equalsIgnoreCase("mp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									if (Config.DEBUG)
									{
										LOG.warn("Mp editing invoked");
									}
									val = st.nextToken();
									final int percent = Integer.valueOf(val);
									switch (percent)
									{
										case 0:
											fee = 0;
											break;
										case 5:
											fee = Config.CH_MPREG1_FEE;
											break;
										case 10:
											fee = Config.CH_MPREG2_FEE;
											break;
										case 15:
											fee = Config.CH_MPREG3_FEE;
											break;
										case 30:
											fee = Config.CH_MPREG4_FEE;
											break;
										default:
											fee = Config.CH_MPREG5_FEE;
											break;
									}
									if (!getClanHall().updateFunctions(ClanHall.FUNC_RESTORE_MP, percent, fee, Config.CH_MPREG_FEE_RATIO, (getClanHall().getFunction(ClanHall.FUNC_RESTORE_MP) == null)))
									{
										player.sendMessage("You don't have enough adena in your clan's warehouse");
									}
									else
									{
										revalidateDeco(player);
									}
								}
							}
							else if (val.equalsIgnoreCase("exp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									val = st.nextToken();
									final int percent = Integer.valueOf(val);
									switch (percent)
									{
										case 0:
											fee = 0;
											break;
										case 5:
											fee = Config.CH_EXPREG1_FEE;
											break;
										case 10:
											fee = Config.CH_EXPREG2_FEE;
											break;
										case 15:
											fee = Config.CH_EXPREG3_FEE;
											break;
										case 25:
											fee = Config.CH_EXPREG4_FEE;
											break;
										case 35:
											fee = Config.CH_EXPREG5_FEE;
											break;
										case 40:
											fee = Config.CH_EXPREG6_FEE;
											break;
										default:
											fee = Config.CH_EXPREG7_FEE;
											break;
									}
									if (!getClanHall().updateFunctions(ClanHall.FUNC_RESTORE_EXP, percent, fee, Config.CH_EXPREG_FEE_RATIO, (getClanHall().getFunction(ClanHall.FUNC_RESTORE_EXP) == null)))
									{
										player.sendMessage("You don't have enough adena in your clan's warehouse");
									}
									else
									{
										revalidateDeco(player);
									}
								}
							}
						}
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/clanHallManager/edit_recovery" + getClanHall().getGrade() + ".htm");
						if (getClanHall().getFunction(ClanHall.FUNC_RESTORE_HP) != null)
						{
							html.replace("%hp%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_HP).getLvl()) + "%");
							html.replace("%hpPrice%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_HP).getLease()));
							html.replace("%hpDate%", format.format(getClanHall().getFunction(ClanHall.FUNC_RESTORE_HP).getEndTime()));
							html.replace("%hpRate%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_HP).getRate() / 86400000));
						}
						else
						{
							html.replace("%hp%", "0");
							html.replace("%hpPrice%", "0");
							html.replace("%hpDate%", "0");
							html.replace("%hpRate%", String.valueOf(Config.CH_HPREG_FEE_RATIO / 86400000));
						}
						if (getClanHall().getFunction(ClanHall.FUNC_RESTORE_EXP) != null)
						{
							html.replace("%exp%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl()) + "%");
							html.replace("%expPrice%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_EXP).getLease()));
							html.replace("%expDate%", format.format(getClanHall().getFunction(ClanHall.FUNC_RESTORE_EXP).getEndTime()));
							html.replace("%expRate%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_EXP).getRate() / 86400000));
						}
						else
						{
							html.replace("%exp%", "0");
							html.replace("%expPrice%", "0");
							html.replace("%expDate%", "0");
							html.replace("%expRate%", String.valueOf(Config.CH_EXPREG_FEE_RATIO / 86400000));
						}
						if (getClanHall().getFunction(ClanHall.FUNC_RESTORE_MP) != null)
						{
							html.replace("%mp%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_MP).getLvl()) + "%");
							html.replace("%mpPrice%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_MP).getLease()));
							html.replace("%mpDate%", format.format(getClanHall().getFunction(ClanHall.FUNC_RESTORE_MP).getEndTime()));
							html.replace("%mpRate%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_RESTORE_MP).getRate() / 86400000));
						}
						else
						{
							html.replace("%mp%", "0");
							html.replace("%mpPrice%", "0");
							html.replace("%mpDate%", "0");
							html.replace("%mpRate%", String.valueOf(Config.CH_MPREG_FEE_RATIO / 86400000));
						}
						sendHtmlMessage(player, html);
						html = null;
					}
					else if (val.equalsIgnoreCase("other"))
					{
						if (st.countTokens() >= 1)
						{
							if (getClanHall().getOwnerId() == 0)
							{
								player.sendMessage("This clan Hall have no owner, you cannot change configuration");
								return;
							}
							val = st.nextToken();
							if (val.equalsIgnoreCase("item"))
							{
								if (st.countTokens() >= 1)
								{
									if (getClanHall().getOwnerId() == 0)
									{
										player.sendMessage("This clan Hall have no owner, you cannot change configuration");
										return;
									}
									val = st.nextToken();
									int fee;
									final int lvl = Integer.valueOf(val);
									switch (lvl)
									{
										case 0:
											fee = 0;
											break;
										case 1:
											fee = Config.CH_ITEM1_FEE;
											break;
										case 2:
											fee = Config.CH_ITEM2_FEE;
											break;
										default:
											fee = Config.CH_ITEM3_FEE;
											break;
									}
									if (!getClanHall().updateFunctions(ClanHall.FUNC_ITEM_CREATE, lvl, fee, Config.CH_ITEM_FEE_RATIO, (getClanHall().getFunction(ClanHall.FUNC_ITEM_CREATE) == null)))
									{
										player.sendMessage("You don't have enough adena in your clan's warehouse");
									}
									else
									{
										revalidateDeco(player);
									}
								}
							}
							else if (val.equalsIgnoreCase("tele"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									val = st.nextToken();
									final int lvl = Integer.valueOf(val);
									switch (lvl)
									{
										case 0:
											fee = 0;
											break;
										case 1:
											fee = Config.CH_TELE1_FEE;
											break;
										default:
											fee = Config.CH_TELE2_FEE;
											break;
									}
									if (!getClanHall().updateFunctions(ClanHall.FUNC_TELEPORT, lvl, fee, Config.CH_TELE_FEE_RATIO, (getClanHall().getFunction(ClanHall.FUNC_TELEPORT) == null)))
									{
										player.sendMessage("You don't have enough adena in your clan's warehouse");
									}
									else
									{
										revalidateDeco(player);
									}
								}
							}
							else if (val.equalsIgnoreCase("support"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									val = st.nextToken();
									final int lvl = Integer.valueOf(val);
									switch (lvl)
									{
										case 0:
											fee = 0;
											break;
										case 1:
											fee = Config.CH_SUPPORT1_FEE;
											break;
										case 2:
											fee = Config.CH_SUPPORT2_FEE;
											break;
										case 3:
											fee = Config.CH_SUPPORT3_FEE;
											break;
										case 4:
											fee = Config.CH_SUPPORT4_FEE;
											break;
										case 5:
											fee = Config.CH_SUPPORT5_FEE;
											break;
										case 6:
											fee = Config.CH_SUPPORT6_FEE;
											break;
										case 7:
											fee = Config.CH_SUPPORT7_FEE;
											break;
										default:
											fee = Config.CH_SUPPORT8_FEE;
											break;
									}
									
									if (!getClanHall().updateFunctions(ClanHall.FUNC_SUPPORT, lvl, fee, Config.CH_SUPPORT_FEE_RATIO, (getClanHall().getFunction(ClanHall.FUNC_SUPPORT) == null)))
									{
										player.sendMessage("You don't have enough adena in your clan's warehouse");
									}
									else
									{
										revalidateDeco(player);
									}
								}
							}
						}
						
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/clanHallManager/edit_other" + getClanHall().getGrade() + ".htm");
						
						if (getClanHall().getFunction(ClanHall.FUNC_TELEPORT) != null)
						{
							html.replace("%tele%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_TELEPORT).getLvl()));
							html.replace("%telePrice%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_TELEPORT).getLease()));
							html.replace("%teleDate%", format.format(getClanHall().getFunction(ClanHall.FUNC_TELEPORT).getEndTime()));
							html.replace("%teleRate%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_TELEPORT).getRate() / 86400000));
						}
						else
						{
							html.replace("%tele%", "0");
							html.replace("%telePrice%", "0");
							html.replace("%teleDate%", "0");
							html.replace("%teleRate%", String.valueOf(Config.CH_TELE_FEE_RATIO / 86400000));
						}
						
						if (getClanHall().getFunction(ClanHall.FUNC_SUPPORT) != null)
						{
							html.replace("%support%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_SUPPORT).getLvl()));
							html.replace("%supportPrice%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_SUPPORT).getLease()));
							html.replace("%supportDate%", format.format(getClanHall().getFunction(ClanHall.FUNC_SUPPORT).getEndTime()));
							html.replace("%supportRate%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_SUPPORT).getRate() / 86400000));
						}
						else
						{
							html.replace("%support%", "0");
							html.replace("%supportPrice%", "0");
							html.replace("%supportDate%", "0");
							html.replace("%supportRate%", String.valueOf(Config.CH_SUPPORT_FEE_RATIO / 86400000));
						}
						
						if (getClanHall().getFunction(ClanHall.FUNC_ITEM_CREATE) != null)
						{
							html.replace("%item%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_ITEM_CREATE).getLvl()));
							html.replace("%itemPrice%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_ITEM_CREATE).getLease()));
							html.replace("%itemDate%", format.format(getClanHall().getFunction(ClanHall.FUNC_ITEM_CREATE).getEndTime()));
							html.replace("%itemRate%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_ITEM_CREATE).getRate() / 86400000));
						}
						else
						{
							html.replace("%item%", "0");
							html.replace("%itemPrice%", "0");
							html.replace("%itemDate%", "0");
							html.replace("%itemRate%", String.valueOf(Config.CH_ITEM_FEE_RATIO / 86400000));
						}
						sendHtmlMessage(player, html);
					}
					else if (val.equalsIgnoreCase("deco"))
					{
						if (st.countTokens() >= 1)
						{
							if (getClanHall().getOwnerId() == 0)
							{
								player.sendMessage("This clan Hall have no owner, you cannot change configuration");
								return;
							}
							val = st.nextToken();
							if (val.equalsIgnoreCase("curtains"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									val = st.nextToken();
									final int lvl = Integer.valueOf(val);
									switch (lvl)
									{
										case 0:
											fee = 0;
											break;
										case 1:
											fee = Config.CH_CURTAIN1_FEE;
											break;
										default:
											fee = Config.CH_CURTAIN2_FEE;
											break;
									}
									if (!getClanHall().updateFunctions(ClanHall.FUNC_DECO_CURTAINS, lvl, fee, Config.CH_CURTAIN_FEE_RATIO, (getClanHall().getFunction(ClanHall.FUNC_DECO_CURTAINS) == null)))
									{
										player.sendMessage("You don't have enough adena in your clan's warehouse");
									}
									else
									{
										revalidateDeco(player);
									}
								}
							}
							else if (val.equalsIgnoreCase("porch"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									val = st.nextToken();
									final int lvl = Integer.valueOf(val);
									switch (lvl)
									{
										case 0:
											fee = 0;
											break;
										case 1:
											fee = Config.CH_FRONT1_FEE;
											break;
										default:
											fee = Config.CH_FRONT2_FEE;
											break;
									}
									if (!getClanHall().updateFunctions(ClanHall.FUNC_DECO_FRONTPLATEFORM, lvl, fee, Config.CH_FRONT_FEE_RATIO, (getClanHall().getFunction(ClanHall.FUNC_DECO_FRONTPLATEFORM) == null)))
									{
										player.sendMessage("You don't have enough adena in your clan's warehouse");
									}
									else
									{
										revalidateDeco(player);
									}
								}
							}
						}
						final NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/clanHallManager/deco.htm");
						if (getClanHall().getFunction(ClanHall.FUNC_DECO_CURTAINS) != null)
						{
							html.replace("%curtain%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_DECO_CURTAINS).getLvl()));
							html.replace("%curtainPrice%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_DECO_CURTAINS).getLease()));
							html.replace("%curtainDate%", format.format(getClanHall().getFunction(ClanHall.FUNC_DECO_CURTAINS).getEndTime()));
							html.replace("%curtainRate%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_DECO_CURTAINS).getRate() / 86400000));
						}
						else
						{
							html.replace("%curtain%", "0");
							html.replace("%curtainPrice%", "0");
							html.replace("%curtainDate%", "0");
							html.replace("%curtainRate%", String.valueOf(Config.CH_CURTAIN_FEE_RATIO / 86400000));
						}
						if (getClanHall().getFunction(ClanHall.FUNC_DECO_FRONTPLATEFORM) != null)
						{
							html.replace("%porch%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_DECO_FRONTPLATEFORM).getLvl()));
							html.replace("%porchPrice%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_DECO_FRONTPLATEFORM).getLease()));
							html.replace("%porchDate%", format.format(getClanHall().getFunction(ClanHall.FUNC_DECO_FRONTPLATEFORM).getEndTime()));
							html.replace("%porchRate%", String.valueOf(getClanHall().getFunction(ClanHall.FUNC_DECO_FRONTPLATEFORM).getRate() / 86400000));
						}
						else
						{
							html.replace("%porch%", "0");
							html.replace("%porchPrice%", "0");
							html.replace("%porchDate%", "0");
							html.replace("%porchRate%", String.valueOf(Config.CH_FRONT_FEE_RATIO / 86400000));
						}
						sendHtmlMessage(player, html);
					}
					else if (val.equalsIgnoreCase("back"))
					{
						showMessageWindow(player);
					}
					else
					{
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile("data/html/clanHallManager/manage.htm");
						sendHtmlMessage(player, html);
					}
				}
				else
				{
					player.sendMessage("You are not authorized to do this!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("support"))
			{
				setTarget(player);
				L2Skill skill;
				if (val == "")
				{
					return;
				}
				
				try
				{
					final int skill_id = Integer.parseInt(val);
					try
					{
						int skill_lvl = 0;
						if (st.countTokens() >= 1)
						{
							skill_lvl = Integer.parseInt(st.nextToken());
						}
						skill = SkillTable.getInstance().getInfo(skill_id, skill_lvl);
						if (skill.getSkillType() == SkillType.SUMMON)
						{
							player.doCast(skill);
						}
						else
						{
							if (!(skill.getMpConsume() + skill.getMpInitialConsume() > getCurrentMp()))
							{
								doCast(skill);
							}
							else
							{
								player.sendMessage("The Clanhall Managers MP is to low.");
							}
						}
						if (getClanHall().getFunction(ClanHall.FUNC_SUPPORT) == null)
						{
							return;
						}
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						if (getClanHall().getFunction(ClanHall.FUNC_SUPPORT).getLvl() == 0)
						{
							return;
						}
						html.setFile("data/html/clanHallManager/support" + getClanHall().getFunction(ClanHall.FUNC_SUPPORT).getLvl() + ".htm");
						html.replace("%mp%", String.valueOf(getCurrentMp()));
						sendHtmlMessage(player, html);
						skill = null;
						html = null;
					}
					catch (final Exception e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
						{
							e.printStackTrace();
						}
						
						player.sendMessage("Invalid skill level!");
					}
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					player.sendMessage("Invalid skill!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("goto"))
			{
				final int whereTo = Integer.parseInt(val);
				doTeleport(player, whereTo);
				return;
			}
			st = null;
			actualCommand = null;
			val = null;
		}
		format = null;
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		player.setLastFolkNPC(this);
		
		if (!canTarget(player))
		{
			return;
		}
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			
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
	
	private void sendHtmlMessage(final L2PcInstance player, NpcHtmlMessage html)
	{
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		html.replace("%npcId%", String.valueOf(getNpcId()));
		player.sendPacket(html);
	}
	
	private void showMessageWindow(final L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/clanHallManager/chamberlain-no.htm";
		
		final int condition = validateCondition(player);
		if (condition == COND_OWNER)
		{
			filename = "data/html/clanHallManager/chamberlain.htm";// Owner message window
		}
		if (condition == COND_OWNER_FALSE)
		{
			filename = "data/html/clanHallManager/chamberlain-of.htm";
		}
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	protected int validateCondition(final L2PcInstance player)
	{
		if (getClanHall() == null)
		{
			return COND_ALL_FALSE;
		}
		
		if (player.getClan() != null)
		{
			if (getClanHall().getOwnerId() == player.getClanId())
			{
				return COND_OWNER;
			}
			return COND_OWNER_FALSE;
		}
		return COND_ALL_FALSE;
	}
	
	@Override
	public final ClanHall getClanHall()
	{
		if (_clanHallId < 0)
		{
			ClanHall temp = ClanHallManager.getInstance().getNearbyClanHall(getX(), getY(), 500);
			if (temp == null)
			{
				temp = CHSiegeManager.getInstance().getNearbyClanHall(this);
			}
			
			if (temp != null)
			{
				_clanHallId = temp.getId();
			}
			
			if (_clanHallId < 0)
			{
				return null;
			}
		}
		return ClanHallManager.getInstance().getClanHallsById(_clanHallId);
	}
	
	private void showVaultWindowDeposit(final L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getClan().getWarehouse());
		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.CLAN)); // Or Clan Hall??
	}
	
	private void showVaultWindowWithdraw(final L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getClan().getWarehouse());
		player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN)); // Or Clan Hall ??
	}
	
	private void doTeleport(final L2PcInstance player, final int val)
	{
		L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
		if (list != null)
		{
			if (SiegeManager.getInstance().getSiege(list.getLocX(), list.getLocY(), list.getLocZ()) != null)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NO_PORT_THAT_IS_IN_SIGE));
				return;
			}
			else if (player.reduceAdena("Teleport", list.getPrice(), this, true))
			{
				if (Config.DEBUG)
				{
					LOG.warn("Teleporting player " + player.getName() + " for CH to new location: " + list.getLocX() + ":" + list.getLocY() + ":" + list.getLocZ());
				}
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ());
			}
		}
		else
		{
			LOG.warn("No teleport destination with id:" + val);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
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
		
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		
		if (list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
		{
			BuyList bl = new BuyList(list, player.getAdena(), taxRate);
			player.sendPacket(bl);
		}
		else
		{
			LOG.warn("possible client hacker: " + player.getName() + " attempting to buy from shop! (L2ClanHallManagerInstance)");
			LOG.warn("buylist id:" + val);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void revalidateDeco(final L2PcInstance player)
	{
		ClanHallDecoration bl = new ClanHallDecoration(ClanHallManager.getInstance().getAbstractHallByOwner(player.getClan()));
		player.sendPacket(bl);
	}
}
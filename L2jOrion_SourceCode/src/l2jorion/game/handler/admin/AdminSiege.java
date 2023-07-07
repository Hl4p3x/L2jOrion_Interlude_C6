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
package l2jorion.game.handler.admin;

import java.util.Calendar;
import java.util.StringTokenizer;

import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.AuctionManager;
import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;
import l2jorion.game.model.zone.type.L2ClanHallZone;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.util.Util;
import l2jorion.util.StringUtil;

public class AdminSiege implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		// Castle commands
		"admin_siege",
		"admin_add_attacker",
		"admin_add_defender",
		"admin_add_guard",
		"admin_list_siege_clans",
		"admin_clear_siege_list",
		"admin_move_defenders",
		"admin_spawn_doors",
		"admin_endsiege",
		"admin_startsiege",
		"admin_setsiegetime",
		"admin_setcastle",
		"admin_removecastle",
		// Clan hall commands
		"admin_clanhall",
		"admin_clanhallset",
		"admin_clanhalldel",
		"admin_clanhallopendoors",
		"admin_clanhallclosedoors",
		"admin_clanhallteleportself"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		command = st.nextToken(); // Get actual command
		
		// Get castle
		Castle castle = null;
		ClanHall clanhall = null;
		if (st.hasMoreTokens())
		{
			L2PcInstance player = null;
			if ((activeChar.getTarget() != null) && activeChar.getTarget() instanceof L2PcInstance)
			{
				player = activeChar.getTarget().getActingPlayer();
			}
			
			String val = st.nextToken();
			if (command.startsWith("admin_clanhall"))
			{
				if (Util.isDigit(val))
				{
					clanhall = ClanHallManager.getInstance().getClanHallsById(Integer.parseInt(val));
					L2Clan clan = null;
					switch (command)
					{
						case "admin_clanhallset":
							if ((player == null) || (player.getClan() == null))
							{
								activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
								return false;
							}
							
							if (clanhall.getOwnerId() > 0)
							{
								activeChar.sendMessage("This Clan Hall is not free!");
								return false;
							}
							
							clan = player.getClan();
							if (clan.getHasHideout() > 0)
							{
								activeChar.sendMessage("You have already a Clan Hall!");
								return false;
							}
							
							if (!clanhall.isSiegableHall())
							{
								ClanHallManager.getInstance().setOwner(clanhall.getClanHallId(), clan);
								if (AuctionManager.getInstance().getAuction(clanhall.getClanHallId()) != null)
								{
									AuctionManager.getInstance().getAuction(clanhall.getClanHallId()).deleteAuctionFromDB();
								}
							}
							else
							{
								clanhall.setOwner(clan);
								clan.setHasHideout(clanhall.getClanHallId());
							}
							break;
						case "admin_clanhalldel":
							
							if (!clanhall.isSiegableHall())
							{
								if (!ClanHallManager.getInstance().isFree(clanhall.getClanHallId()))
								{
									ClanHallManager.getInstance().setFree(clanhall.getClanHallId());
									AuctionManager.getInstance().initNPC(clanhall.getClanHallId());
								}
								else
								{
									activeChar.sendMessage("This Clan Hall is already free!");
								}
							}
							else
							{
								final int oldOwner = clanhall.getOwnerId();
								if (oldOwner > 0)
								{
									clanhall.free();
									clan = ClanTable.getInstance().getClan(oldOwner);
									if (clan != null)
									{
										clan.setHasHideout(0);
										clan.broadcastClanStatus();
									}
								}
							}
							break;
						case "admin_clanhallopendoors":
							clanhall.openCloseDoors(true);
							break;
						case "admin_clanhallclosedoors":
							clanhall.openCloseDoors(false);
							break;
						case "admin_clanhallteleportself":
							L2ClanHallZone zone = clanhall.getZone();
							
							if (zone != null)
							{
								activeChar.teleToLocation(zone.getSpawnLoc(), true);
							}
							break;
						default:
							if (!clanhall.isSiegableHall())
							{
								showClanHallPage(activeChar, clanhall);
							}
							else
							{
								showSiegableHallPage(activeChar, (SiegableHall) clanhall);
							}
							break;
					}
				}
			}
			else
			{
				castle = CastleManager.getInstance().getCastle(val);
				switch (command)
				{
					case "admin_add_attacker":
						if (player == null)
						{
							activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
						}
						else
						{
							castle.getSiege().registerAttacker(player, true);
						}
						showSiegePage(activeChar, castle.getName());
						break;
					case "admin_add_defender":
						if (player == null)
						{
							activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
						}
						else
						{
							castle.getSiege().registerDefender(player, true);
						}
						showSiegePage(activeChar, castle.getName());
						break;
					case "admin_add_guard":
						if (st.hasMoreTokens())
						{
							val = st.nextToken();
							if (Util.isDigit(val))
							{
								castle.getSiege().getSiegeGuardManager().addSiegeGuard(activeChar, Integer.parseInt(val));
								break;
							}
						}
						// If doesn't have more tokens or token is not a number.
						activeChar.sendMessage("Usage: //add_guard castle npcId");
						break;
					case "admin_clear_siege_list":
						castle.getSiege().clearSiegeClan();
						showSiegePage(activeChar, castle.getName());
						break;
					case "admin_endsiege":
						castle.getSiege().endSiege();
						showSiegePage(activeChar, castle.getName());
						break;
					case "admin_list_siege_clans":
						castle.getSiege().listRegisterClan(activeChar);
						showSiegePage(activeChar, castle.getName());
						break;
					case "admin_move_defenders":
						activeChar.sendMessage("Not implemented yet.");
						break;
					case "admin_setcastle":
						if ((player == null) || (player.getClan() == null))
						{
							activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
						}
						else
						{
							castle.setOwner(player.getClan());
						}
						showSiegePage(activeChar, castle.getName());
						break;
					case "admin_removecastle":
						final L2Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
						if (clan != null)
						{
							castle.removeOwner(clan);
						}
						else
						{
							activeChar.sendMessage("Unable to remove castle.");
						}
						showSiegePage(activeChar, castle.getName());
						break;
					case "admin_setsiegetime":
						if (st.hasMoreTokens())
						{
							final Calendar cal = Calendar.getInstance();
							cal.setTimeInMillis(castle.getSiegeDate().getTimeInMillis());
							
							val = st.nextToken();
							
							if ("month".equals(val))
							{
								int month = cal.get(Calendar.MONTH) + Integer.parseInt(st.nextToken());
								if ((cal.getActualMinimum(Calendar.MONTH) > month) || (cal.getActualMaximum(Calendar.MONTH) < month))
								{
									activeChar.sendMessage("Unable to change Siege Date - Incorrect month value only " + cal.getActualMinimum(Calendar.MONTH) + "-" + cal.getActualMaximum(Calendar.MONTH) + " is accepted!");
									return false;
								}
								cal.set(Calendar.MONTH, month);
							}
							else if ("day".equals(val))
							{
								int day = Integer.parseInt(st.nextToken());
								if ((cal.getActualMinimum(Calendar.DAY_OF_MONTH) > day) || (cal.getActualMaximum(Calendar.DAY_OF_MONTH) < day))
								{
									activeChar.sendMessage("Unable to change Siege Date - Incorrect day value only " + cal.getActualMinimum(Calendar.DAY_OF_MONTH) + "-" + cal.getActualMaximum(Calendar.DAY_OF_MONTH) + " is accepted!");
									return false;
								}
								cal.set(Calendar.DAY_OF_MONTH, day);
							}
							else if ("hour".equals(val))
							{
								int hour = Integer.parseInt(st.nextToken());
								if ((cal.getActualMinimum(Calendar.HOUR_OF_DAY) > hour) || (cal.getActualMaximum(Calendar.HOUR_OF_DAY) < hour))
								{
									activeChar.sendMessage("Unable to change Siege Date - Incorrect hour value only " + cal.getActualMinimum(Calendar.HOUR_OF_DAY) + "-" + cal.getActualMaximum(Calendar.HOUR_OF_DAY) + " is accepted!");
									return false;
								}
								cal.set(Calendar.HOUR_OF_DAY, hour);
							}
							else if ("min".equals(val))
							{
								int min = Integer.parseInt(st.nextToken());
								if ((cal.getActualMinimum(Calendar.MINUTE) > min) || (cal.getActualMaximum(Calendar.MINUTE) < min))
								{
									activeChar.sendMessage("Unable to change Siege Date - Incorrect minute value only " + cal.getActualMinimum(Calendar.MINUTE) + "-" + cal.getActualMaximum(Calendar.MINUTE) + " is accepted!");
									return false;
								}
								cal.set(Calendar.MINUTE, min);
							}
							
							if (cal.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
							{
								activeChar.sendMessage("Unable to change Siege Date");
							}
							else if (cal.getTimeInMillis() != castle.getSiegeDate().getTimeInMillis())
							{
								castle.getSiegeDate().setTimeInMillis(cal.getTimeInMillis());
								castle.getSiege().saveSiegeDate();
								activeChar.sendMessage("Castle siege time for castle " + castle.getName() + " has been changed.");
							}
						}
						showSiegeTimePage(activeChar, castle);
						break;
					case "admin_spawn_doors":
						castle.spawnDoor();
						showSiegePage(activeChar, castle.getName());
						break;
					case "admin_startsiege":
						castle.getSiege().startSiege();
						showSiegePage(activeChar, castle.getName());
						break;
					default:
						showSiegePage(activeChar, castle.getName());
						break;
				}
			}
		}
		else
		{
			showCastleSelectPage(activeChar);
		}
		return true;
	}
	
	/**
	 * Show castle select page.
	 * @param activeChar the active char
	 */
	private void showCastleSelectPage(L2PcInstance activeChar)
	{
		int i = 0;
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(1);
		adminReply.setFile("data/html/admin/castles.htm");
		final StringBuilder cList = new StringBuilder(500);
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle != null)
			{
				String name = castle.getName();
				StringUtil.append(cList, "<td fixwidth=90><a action=\"bypass -h admin_siege ", name, "\">", name, "</a></td>");
				i++;
			}
			if (i > 2)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%castles%", cList.toString());
		cList.setLength(0);
		i = 0;
		for (SiegableHall hall : CHSiegeManager.getInstance().getConquerableHalls().values())
		{
			if (hall != null)
			{
				StringUtil.append(cList, "<td fixwidth=90><a action=\"bypass -h admin_chsiege_siegablehall ", String.valueOf(hall.getClanHallId()), "\">", hall.getName(), "</a></td>");
				i++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%siegableHalls%", cList.toString());
		cList.setLength(0);
		i = 0;
		for (ClanHall clanhall : ClanHallManager.getInstance().getClanHalls().values())
		{
			if (clanhall != null)
			{
				StringUtil.append(cList, "<td fixwidth=134><a action=\"bypass -h admin_clanhall ", String.valueOf(clanhall.getClanHallId()), "\">", clanhall.getName(), "</a></td>");
				i++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%clanhalls%", cList.toString());
		cList.setLength(0);
		i = 0;
		for (ClanHall clanhall : ClanHallManager.getInstance().getFreeClanHalls().values())
		{
			if (clanhall != null)
			{
				StringUtil.append(cList, "<td fixwidth=134><a action=\"bypass -h admin_clanhall ", String.valueOf(clanhall.getClanHallId()), "\">", clanhall.getName(), "</a></td>");
				i++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%freeclanhalls%", cList.toString());
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Show the siege page.
	 * @param activeChar the active char
	 * @param castleName the castle name
	 */
	private void showSiegePage(L2PcInstance activeChar, String castleName)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(1);
		adminReply.setFile("data/html/admin/castle.htm");
		adminReply.replace("%castleName%", castleName);
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Show the siege time page.
	 * @param activeChar the active char
	 * @param castle the castle
	 */
	private void showSiegeTimePage(L2PcInstance activeChar, Castle castle)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(1);
		adminReply.setFile("data/html/admin/castlesiegetime.htm");
		adminReply.replace("%castleName%", castle.getName());
		adminReply.replace("%time%", castle.getSiegeDate().getTime().toString());
		final Calendar newDay = Calendar.getInstance();
		boolean isSunday = false;
		
		if (newDay.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
		{
			isSunday = true;
		}
		else
		{
			newDay.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		}
		
		// if (!SevenSigns.getInstance().isDateInSealValidPeriod(newDay))
		// {
		// newDay.add(Calendar.DAY_OF_MONTH, 7);
		// }
		
		if (isSunday)
		{
			adminReply.replace("%sundaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%sunday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
			newDay.add(Calendar.DAY_OF_MONTH, 13);
			adminReply.replace("%saturdaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%saturday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
		}
		else
		{
			adminReply.replace("%saturdaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%saturday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
			newDay.add(Calendar.DAY_OF_MONTH, 1);
			adminReply.replace("%sundaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%sunday%", String.valueOf(newDay.get(Calendar.MONTH) + "/" + String.valueOf(newDay.get(Calendar.DAY_OF_MONTH))));
		}
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Show the clan hall page.
	 * @param activeChar the active char
	 * @param clanhall the clan hall
	 */
	private void showClanHallPage(L2PcInstance activeChar, ClanHall clanhall)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(1);
		adminReply.setFile("data/html/admin/clanhall.htm");
		adminReply.replace("%clanhallName%", clanhall.getName());
		adminReply.replace("%clanhallId%", String.valueOf(clanhall.getClanHallId()));
		final L2Clan owner = ClanTable.getInstance().getClan(clanhall.getOwnerId());
		adminReply.replace("%clanhallOwner%", (owner == null) ? "None" : owner.getName());
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Show the siegable hall page.
	 * @param activeChar the active char
	 * @param hall the siegable hall
	 */
	private void showSiegableHallPage(L2PcInstance activeChar, SiegableHall hall)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(1);
		msg.setFile("data/html/admin/siegablehall.htm");
		msg.replace("%clanhallId%", String.valueOf(hall.getClanHallId()));
		msg.replace("%clanhallName%", hall.getName());
		if (hall.getOwnerId() > 0)
		{
			final L2Clan owner = ClanTable.getInstance().getClan(hall.getOwnerId());
			msg.replace("%clanhallOwner%", (owner != null) ? owner.getName() : "No Owner");
		}
		else
		{
			msg.replace("%clanhallOwner%", "No Owner");
		}
		activeChar.sendPacket(msg);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}

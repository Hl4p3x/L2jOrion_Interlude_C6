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

import l2jorion.Config;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.hallsiege.ClanHallSiegeEngine;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SiegeInfo;

public class AdminCHSiege implements IAdminCommandHandler
{
	
	private static final String[] COMMANDS =
	{
		"admin_chsiege_siegablehall",
		"admin_chsiege_startSiege",
		"admin_chsiege_endsSiege",
		"admin_chsiege_setSiegeDate",
		"admin_chsiege_addAttacker",
		"admin_chsiege_removeAttacker",
		"admin_chsiege_clearAttackers",
		"admin_chsiege_listAttackers",
		"admin_chsiege_forwardSiege"
	};
	
	@Override
	public String[] getAdminCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		final String[] split = command.split(" ");
		SiegableHall hall = null;
		
		if (Config.ALT_DEV_NO_QUESTS)
		{
			activeChar.sendMessage("AltDevNoQuests = true; Clan Hall Sieges are disabled!");
			return false;
		}
		
		if (split.length < 2)
		{
			activeChar.sendMessage("You have to specify the hall id at least");
			return false;
		}
		if ((hall = getHall(split[1], activeChar)) == null)
		{
			activeChar.sendMessage("Couldnt find he desired siegable hall (" + split[1] + ")");
			return false;
		}
		if (hall.getSiege() == null)
		{
			activeChar.sendMessage("The given hall dont have any attached siege!");
			return false;
		}
		
		if (split[0].equals(COMMANDS[1]))
		{
			if (hall.isInSiege())
			{
				activeChar.sendMessage("The requested clan hall is alredy in siege!");
			}
			else
			{
				L2Clan owner = ClanTable.getInstance().getClan(hall.getOwnerId());
				if (owner != null)
				{
					hall.free();
					owner.setHasHideout(0);
					hall.addAttacker(owner);
				}
				hall.getSiege().startSiege();
			}
		}
		else if (split[0].equals(COMMANDS[2]))
		{
			if (!hall.isInSiege())
			{
				activeChar.sendMessage("The requested clan hall isnt in siege!");
			}
			else
			{
				hall.getSiege().endSiege();
			}
		}
		else if (split[0].equals(COMMANDS[3]))
		{
			if (!hall.isRegistering())
			{
				activeChar.sendMessage("Cannot change siege date while hall is in siege");
			}
			else if (split.length < 3)
			{
				activeChar.sendMessage("The date format is incorrect. Try again.");
			}
			else
			{
				String[] rawDate = split[2].split(";");
				if (rawDate.length < 2)
				{
					activeChar.sendMessage("You have to specify this format DD-MM-YYYY;HH:MM");
				}
				else
				{
					String[] day = rawDate[0].split("-");
					String[] hour = rawDate[1].split(":");
					if ((day.length < 3) || (hour.length < 2))
					{
						activeChar.sendMessage("Incomplete day, hour or both!");
					}
					else
					{
						int d = parseInt(day[0]);
						int month = parseInt(day[1]) - 1;
						int year = parseInt(day[2]);
						int h = parseInt(hour[0]);
						int min = parseInt(hour[1]);
						if (((month == 2) && (d > 28)) || (d > 31) || (d <= 0) || (month <= 0) || (month > 12) || (year < Calendar.getInstance().get(Calendar.YEAR)))
						{
							activeChar.sendMessage("Wrong day/month/year gave!");
						}
						else if ((h <= 0) || (h > 24) || (min < 0) || (min >= 60))
						{
							activeChar.sendMessage("Wrong hour/minutes gave!");
						}
						else
						{
							Calendar c = Calendar.getInstance();
							c.set(Calendar.YEAR, year);
							c.set(Calendar.MONTH, month);
							c.set(Calendar.DAY_OF_MONTH, d);
							c.set(Calendar.HOUR_OF_DAY, h);
							c.set(Calendar.MINUTE, min);
							c.set(Calendar.SECOND, 0);
							
							if (c.getTimeInMillis() > System.currentTimeMillis())
							{
								activeChar.sendMessage(hall.getName() + " siege: " + c.getTime().toString());
								hall.setNextSiegeDate(c.getTimeInMillis());
								hall.getSiege().updateSiege();
								hall.updateDb();
							}
							else
							{
								activeChar.sendMessage("The given time is in the past!");
							}
						}
					}
					
				}
			}
		}
		else if (split[0].equals(COMMANDS[4]))
		{
			if (hall.isInSiege())
			{
				activeChar.sendMessage("The clan hall is in siege, cannot add attackers now.");
				return false;
			}
			
			L2Clan attacker = null;
			if (split.length < 3)
			{
				L2Object rawTarget = activeChar.getTarget();
				L2PcInstance target = null;
				if (rawTarget == null)
				{
					activeChar.sendMessage("You must target a clan member of the attacker!");
				}
				else if (!(rawTarget instanceof L2PcInstance))
				{
					activeChar.sendMessage("You must target a player with clan!");
				}
				else if ((target = (L2PcInstance) rawTarget).getClan() == null)
				{
					activeChar.sendMessage("Your target does not have any clan!");
				}
				else if (hall.getSiege().checkIsAttacker(target.getClan()))
				{
					activeChar.sendMessage("Your target's clan is alredy participating!");
				}
				else
				{
					attacker = target.getClan();
				}
			}
			else
			{
				L2Clan rawClan = ClanTable.getInstance().getClanByName(split[2]);
				if (rawClan == null)
				{
					activeChar.sendMessage("The given clan does not exist!");
				}
				else if (hall.getSiege().checkIsAttacker(rawClan))
				{
					activeChar.sendMessage("The given clan is alredy participating!");
				}
				else
				{
					attacker = rawClan;
				}
			}
			
			if (attacker != null)
			{
				hall.addAttacker(attacker);
			}
		}
		else if (split[0].equals(COMMANDS[5]))
		{
			if (hall.isInSiege())
			{
				activeChar.sendMessage("The clan hall is in siege, cannot remove attackers now.");
				return false;
			}
			
			if (split.length < 3)
			{
				L2Object rawTarget = activeChar.getTarget();
				L2PcInstance target = null;
				if (rawTarget == null)
				{
					activeChar.sendMessage("You must target a clan member of the attacker!");
				}
				else if (!(rawTarget instanceof L2PcInstance))
				{
					activeChar.sendMessage("You must target a player with clan!");
				}
				else if ((target = (L2PcInstance) rawTarget).getClan() == null)
				{
					activeChar.sendMessage("Your target does not have any clan!");
				}
				else if (!hall.getSiege().checkIsAttacker(target.getClan()))
				{
					activeChar.sendMessage("Your target's clan is not participating!");
				}
				else
				{
					hall.removeAttacker(target.getClan());
				}
			}
			else
			{
				L2Clan rawClan = ClanTable.getInstance().getClanByName(split[2]);
				if (rawClan == null)
				{
					activeChar.sendMessage("The given clan does not exist!");
				}
				else if (!hall.getSiege().checkIsAttacker(rawClan))
				{
					activeChar.sendMessage("The given clan is not participating!");
				}
				else
				{
					hall.removeAttacker(rawClan);
				}
			}
		}
		else if (split[0].equals(COMMANDS[6]))
		{
			if (hall.isInSiege())
			{
				activeChar.sendMessage("The requested hall is in siege right now, cannot clear attacker list!");
			}
			else
			{
				hall.getSiege().getAttackers().clear();
			}
		}
		else if (split[0].equals(COMMANDS[7]))
		{
			activeChar.sendPacket(new SiegeInfo(hall));
		}
		else if (split[0].equals(COMMANDS[8]))
		{
			ClanHallSiegeEngine siegable = hall.getSiege();
			siegable.cancelSiegeTask();
			switch (hall.getSiegeStatus())
			{
				case REGISTERING:
					siegable.prepareOwner();
					break;
				case WAITING_BATTLE:
					siegable.startSiege();
					break;
				case RUNNING:
					siegable.endSiege();
					break;
			}
		}
		
		sendSiegableHallPage(activeChar, split[1], hall);
		return false;
	}
	
	private SiegableHall getHall(String id, L2PcInstance gm)
	{
		int ch = parseInt(id);
		if (ch == 0)
		{
			gm.sendMessage("Wrong clan hall id, unparseable id!");
			return null;
		}
		
		SiegableHall hall = CHSiegeManager.getInstance().getSiegableHall(ch);
		
		if (hall == null)
		{
			gm.sendMessage("Couldnt find the clan hall.");
		}
		
		return hall;
	}
	
	private int parseInt(String st)
	{
		int val = 0;
		try
		{
			val = Integer.parseInt(st);
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
		}
		return val;
	}
	
	private void sendSiegableHallPage(L2PcInstance activeChar, String hallId, SiegableHall hall)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(1);
		msg.setFile("data/html/admin/siegablehall.htm");
		msg.replace("%clanhallId%", hallId);
		msg.replace("%clanhallName%", hall.getName());
		if (hall.getOwnerId() > 0)
		{
			L2Clan owner = ClanTable.getInstance().getClan(hall.getOwnerId());
			if (owner != null)
			{
				msg.replace("%clanhallOwner%", owner.getName());
			}
			else
			{
				msg.replace("%clanhallOwner%", "No Owner");
			}
		}
		else
		{
			msg.replace("%clanhallOwner%", "No Owner");
		}
		activeChar.sendPacket(msg);
	}
	
}

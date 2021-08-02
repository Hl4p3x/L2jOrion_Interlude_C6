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

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.AdminForgePacket;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;

public class AdminPForge implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_forge",
		"admin_forge2",
		"admin_forge3"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (command.equals("admin_forge"))
		{
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_forge2"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				String format = st.nextToken();
				showPage2(activeChar, format);
				
				st = null;
				format = null;
			}
			catch (final Exception ex)
			{
				ex.printStackTrace();
				activeChar.sendMessage("Usage: //forge2 format");
			}
		}
		else if (command.startsWith("admin_forge3"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				String format = st.nextToken();
				
				boolean broadcast = false;
				
				if (format.toLowerCase().equals("broadcast"))
				{
					format = st.nextToken();
					broadcast = true;
				}
				
				AdminForgePacket sp = new AdminForgePacket();
				
				for (int i = 0; i < format.length(); i++)
				{
					String val = st.nextToken();
					
					if (val.toLowerCase().equals("$objid"))
					{
						val = String.valueOf(activeChar.getObjectId());
					}
					else if (val.toLowerCase().equals("$tobjid"))
					{
						val = String.valueOf(activeChar.getTarget().getObjectId());
					}
					else if (val.toLowerCase().equals("$bobjid"))
					{
						if (activeChar.getBoat() != null)
						{
							val = String.valueOf(activeChar.getBoat().getObjectId());
						}
					}
					else if (val.toLowerCase().equals("$clanid"))
					{
						val = String.valueOf(activeChar.getCharId());
					}
					else if (val.toLowerCase().equals("$allyid"))
					{
						val = String.valueOf(activeChar.getAllyId());
					}
					else if (val.toLowerCase().equals("$tclanid"))
					{
						val = String.valueOf(((L2PcInstance) activeChar.getTarget()).getCharId());
					}
					else if (val.toLowerCase().equals("$tallyid"))
					{
						val = String.valueOf(((L2PcInstance) activeChar.getTarget()).getAllyId());
					}
					else if (val.toLowerCase().equals("$x"))
					{
						val = String.valueOf(activeChar.getX());
					}
					else if (val.toLowerCase().equals("$y"))
					{
						val = String.valueOf(activeChar.getY());
					}
					else if (val.toLowerCase().equals("$z"))
					{
						val = String.valueOf(activeChar.getZ());
					}
					else if (val.toLowerCase().equals("$heading"))
					{
						val = String.valueOf(activeChar.getHeading());
					}
					else if (val.toLowerCase().equals("$tx"))
					{
						val = String.valueOf(activeChar.getTarget().getX());
					}
					else if (val.toLowerCase().equals("$ty"))
					{
						val = String.valueOf(activeChar.getTarget().getY());
					}
					else if (val.toLowerCase().equals("$tz"))
					{
						val = String.valueOf(activeChar.getTarget().getZ());
					}
					else if (val.toLowerCase().equals("$theading"))
					{
						val = String.valueOf(((L2PcInstance) activeChar.getTarget()).getHeading());
					}
					
					sp.addPart(format.getBytes()[i], val);
					val = null;
				}
				if (broadcast)
				{
					activeChar.broadcastPacket(sp);
				}
				else
				{
					activeChar.sendPacket(sp);
				}
				showPage3(activeChar, format, command);
				
				format = null;
				st = null;
				sp = null;
			}
			catch (final Exception ex)
			{
				ex.printStackTrace();
			}
		}
		return true;
	}
	
	private void showMainPage(final L2PcInstance activeChar)
	{
		AdminHelpPage.showHelpPage(activeChar, "pforge1.htm");
	}
	
	private void showPage2(final L2PcInstance activeChar, final String format)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/pforge2.htm");
		adminReply.replace("%format%", format);
		TextBuilder replyMSG = new TextBuilder();
		
		for (int i = 0; i < format.length(); i++)
		{
			replyMSG.append(format.charAt(i) + " : <edit var=\"v" + i + "\" width=100><br1>");
		}
		
		adminReply.replace("%valueditors%", replyMSG.toString());
		replyMSG.clear();
		
		for (int i = 0; i < format.length(); i++)
		{
			replyMSG.append(" \\$v" + i);
		}
		
		adminReply.replace("%send%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
		
		replyMSG = null;
		adminReply = null;
	}
	
	private void showPage3(final L2PcInstance activeChar, final String format, final String command)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/pforge3.htm");
		adminReply.replace("%format%", format);
		adminReply.replace("%command%", command);
		activeChar.sendPacket(adminReply);
		adminReply = null;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
}

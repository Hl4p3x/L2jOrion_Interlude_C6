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
package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;

public class AdminCTFEngine implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_ctf",
		"admin_ctf_name",
		"admin_ctf_desc",
		"admin_ctf_join_loc",
		"admin_ctf_edit",
		"admin_ctf_control",
		"admin_ctf_minlvl",
		"admin_ctf_maxlvl",
		"admin_ctf_tele_npc",
		"admin_ctf_tele_team",
		"admin_ctf_tele_flag",
		"admin_ctf_npc",
		"admin_ctf_npc_pos",
		"admin_ctf_reward",
		"admin_ctf_reward_amount",
		"admin_ctf_team_add",
		"admin_ctf_team_remove",
		"admin_ctf_team_pos",
		"admin_ctf_team_color",
		"admin_ctf_team_flag",
		"admin_ctf_join",
		"admin_ctf_teleport",
		"admin_ctf_start",
		"admin_ctf_startevent",
		"admin_ctf_abort",
		"admin_ctf_finish",
		"admin_ctf_sit",
		"admin_ctf_dump",
		"admin_ctf_save",
		"admin_ctf_load",
		"admin_ctf_jointime",
		"admin_ctf_eventtime",
		"admin_ctf_autoevent",
		"admin_ctf_minplayers",
		"admin_ctf_maxplayers",
		"admin_ctf_interval"
	};
	
	private enum CommandEnum
	{
		admin_ctf,
		admin_ctf_name,
		admin_ctf_desc,
		admin_ctf_join_loc,
		admin_ctf_edit,
		admin_ctf_control,
		admin_ctf_minlvl,
		admin_ctf_maxlvl,
		admin_ctf_tele_npc,
		admin_ctf_tele_team,
		admin_ctf_tele_flag,
		admin_ctf_npc,
		admin_ctf_npc_pos,
		admin_ctf_reward,
		admin_ctf_reward_amount,
		admin_ctf_team_add,
		admin_ctf_team_remove,
		admin_ctf_team_pos,
		admin_ctf_team_color,
		admin_ctf_team_flag,
		admin_ctf_join,
		admin_ctf_teleport,
		admin_ctf_start,
		admin_ctf_startevent,
		admin_ctf_abort,
		admin_ctf_finish,
		admin_ctf_sit,
		admin_ctf_dump,
		admin_ctf_save,
		admin_ctf_load,
		admin_ctf_jointime,
		admin_ctf_eventtime,
		admin_ctf_autoevent,
		admin_ctf_minplayers,
		admin_ctf_maxplayers,
		admin_ctf_interval
	}
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		/*
		 * if(!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel())){ return false; } if(Config.GMAUDIT) { Logger _logAudit = Logger.getLogger("gmaudit"); LogRecord record = new LogRecord(Level.INFO, command); record.setParameters(new Object[] { "GM: " +
		 * activeChar.getName(), " to target [" + activeChar.getTarget() + "] " }); _logAudit.LOGGER(record); }
		 */
		
		final StringTokenizer st = new StringTokenizer(command);
		
		final CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
			return false;
		
		switch (comm)
		{
			case admin_ctf:
			{
				showMainPage(activeChar);
				return true;
			}
			case admin_ctf_name:
			{
				
				if (st.hasMoreTokens())
				{
					
					if (CTF.set_eventName(st.nextToken()))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
				}
				activeChar.sendMessage("Usage: //ctf_name <event_name>");
				return false;
				
			}
			case admin_ctf_desc:
			{
				if (st.hasMoreTokens())
				{
					
					if (CTF.set_eventDesc(st.nextToken()))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
					
				}
				activeChar.sendMessage("Usage: //ctf_desc <event_descr>");
				return false;
				
			}
			case admin_ctf_join_loc:
				
				if (st.hasMoreTokens())
				{
					
					if (CTF.set_joiningLocationName(st.nextToken()))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
					
				}
				activeChar.sendMessage("Usage: //ctf_join_loc <event_loc_name>");
				return false;
				
			case admin_ctf_edit:
			{
				showEditPage(activeChar);
				return true;
			}
			case admin_ctf_control:
			{
				showControlPage(activeChar);
				return true;
			}
			case admin_ctf_minlvl:
				
				if (st.hasMoreTokens())
				{
					
					final String lvl_s = st.nextToken();
					int lvl = 0;
					
					try
					{
						lvl = Integer.parseInt(lvl_s);
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //ctf_minlvl <min_lvl_value>");
						return false;
					}
					
					if (!CTF.checkMinLevel(lvl))
					{
						activeChar.sendMessage("Cannot perform requested operation, Min lvl must be lower then Max");
						return false;
					}
					
					if (CTF.set_minlvl(lvl))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
					
				}
				activeChar.sendMessage("Usage: //ctf_minlvl <min_lvl_value>");
				return false;
			case admin_ctf_maxlvl:
				if (st.hasMoreTokens())
				{
					
					final String lvl_s = st.nextToken();
					int lvl = 0;
					
					try
					{
						lvl = Integer.parseInt(lvl_s);
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //ctf_maxlvl <max_lvl_value>");
						return false;
					}
					
					if (!CTF.checkMaxLevel(lvl))
					{
						activeChar.sendMessage("Cannot perform requested operation, Max lvl must be higher then Min");
						return false;
					}
					
					if (CTF.set_maxlvl(lvl))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
					
				}
				activeChar.sendMessage("Usage: //ctf_maxlvl <min_lvl_value>");
				return false;
				
			case admin_ctf_tele_npc:
			{
				activeChar.teleToLocation(CTF.get_npcLocation(), false);
				showMainPage(activeChar);
			}
			case admin_ctf_tele_team:
				if (st.hasMoreTokens())
				{
					for (final String team : CTF._teams)
					{
						if (team.equals(st.nextToken()))
						{
							final int index = CTF._teams.indexOf(team);
							activeChar.teleToLocation(CTF._teamsX.get(index), CTF._teamsY.get(index), CTF._teamsZ.get(index));
							return true;
						}
					}
					activeChar.sendMessage("Usage: //ctf_tele_team <team_name>");
					showMainPage(activeChar);
					return false;
				}
				activeChar.sendMessage("Usage: //ctf_tele_team <team_name>");
				return false;
				
			case admin_ctf_tele_flag:
				
				if (st.hasMoreTokens())
				{
					for (final String team : CTF._teams)
					{
						if (team.equals(st.nextToken()))
						{
							final int index = CTF._teams.indexOf(team);
							activeChar.teleToLocation(CTF._flagsX.get(index), CTF._flagsY.get(index), CTF._flagsZ.get(index));
							return true;
						}
					}
					activeChar.sendMessage("Usage: //ctf_tele_flag <team_name>");
					showMainPage(activeChar);
					return false;
				}
				activeChar.sendMessage("Usage: //ctf_tele_flag <team_name>");
				return false;
				
			case admin_ctf_npc:
				
				if (st.hasMoreTokens())
				{
					
					int id = 0;
					
					try
					{
						id = Integer.valueOf(st.nextToken());
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //ctf_npc <npc_id>");
						return false;
					}
					
					if (CTF.set_npcId(id))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
					
				}
				activeChar.sendMessage("Usage: //ctf_npc <npc_id>");
				return false;
				
			case admin_ctf_npc_pos:
				CTF.setNpcPos(activeChar);
				showMainPage(activeChar);
				return true;
			case admin_ctf_reward:
				
				if (st.hasMoreTokens())
				{
					
					int id = 0;
					
					try
					{
						id = Integer.valueOf(st.nextToken());
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //ctf_reward <reward_id>");
						return false;
					}
					
					if (CTF.set_rewardId(id))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
					
				}
				activeChar.sendMessage("Usage: //ctf_reward <reward_id>");
				return false;
				
			case admin_ctf_reward_amount:
				
				if (st.hasMoreTokens())
				{
					
					int amount = 0;
					
					try
					{
						amount = Integer.valueOf(st.nextToken());
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //ctf_reward_amount <reward_amount>");
						return false;
					}
					
					if (CTF.set_rewardAmount(amount))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
					
				}
				activeChar.sendMessage("Usage: //ctf_reward_amount <reward_amount>");
				return false;
				
			case admin_ctf_team_add:
				
				if (st.hasMoreTokens())
				{
					
					CTF.addTeam(st.nextToken());
					showMainPage(activeChar);
					return true;
				}
				activeChar.sendMessage("Usage: //ctf_team_add <team_name>");
				return false;
				
			case admin_ctf_team_remove:
				
				if (st.hasMoreTokens())
				{
					
					CTF.removeTeam(st.nextToken());
					showMainPage(activeChar);
					return true;
				}
				activeChar.sendMessage("Usage: //ctf_team_remove <team_name>");
				return false;
				
			case admin_ctf_team_pos:
				
				if (st.hasMoreTokens())
				{
					
					CTF.setTeamPos(st.nextToken(), activeChar);
					showMainPage(activeChar);
					return true;
				}
				activeChar.sendMessage("Usage: //ctf_team_pos <team_name>");
				return false;
				
			case admin_ctf_team_color:
				
				if (st.countTokens() == 2)
				{
					
					final String color_s = st.nextToken();
					
					int color = 0;
					
					try
					{
						
						color = Integer.decode("0x" + color_s);
						
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //ctf_team_color <colorHex> <teamName>");
						return false;
					}
					
					final String team = st.nextToken();
					
					CTF.setTeamColor(team, color);
					showMainPage(activeChar);
					return true;
				}
				activeChar.sendMessage("Usage: //ctf_team_color <colorHex> <teamName>");
				return false;
				
			case admin_ctf_team_flag:
				
				if (st.hasMoreTokens())
				{
					
					CTF.setTeamFlag(st.nextToken(), activeChar);
					showMainPage(activeChar);
					return true;
				}
				activeChar.sendMessage("Usage: //ctf_team_flag <teamName>");
				return false;
				
			case admin_ctf_join:
				if (CTF.startJoin())
				{
					showMainPage(activeChar);
					return true;
				}
				activeChar.sendMessage("Cannot startJoin, check LOGGER for info..");
				return false;
			case admin_ctf_teleport:
				CTF.startTeleport();
				showMainPage(activeChar);
				return true;
			case admin_ctf_start:
				if (CTF.startEvent())
				{
					showMainPage(activeChar);
					return true;
				}
				activeChar.sendMessage("Cannot startEvent, check LOGGER for info..");
				return false;
				
			case admin_ctf_startevent:
				CTF.eventOnceStart();
				showMainPage(activeChar);
				return true;
				
			case admin_ctf_abort:
				activeChar.sendMessage("Aborting event");
				CTF.abortEvent();
				showMainPage(activeChar);
				return true;
			case admin_ctf_finish:
				CTF.finishEvent();
				showMainPage(activeChar);
				return true;
			case admin_ctf_sit:
				CTF.sit();
				showMainPage(activeChar);
				return true;
			case admin_ctf_dump:
				CTF.dumpData();
				return true;
				
			case admin_ctf_save:
				CTF.saveData();
				showMainPage(activeChar);
				return true;
			case admin_ctf_load:
				CTF.loadData();
				showMainPage(activeChar);
				return true;
			case admin_ctf_jointime:
				
				if (st.hasMoreTokens())
				{
					
					final String time_s = st.nextToken();
					
					int time = 0;
					
					try
					{
						
						time = Integer.parseInt(time_s);
						
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //ctf_jointime <minutes>");
						return false;
					}
					
					if (CTF.set_joinTime(time))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
					
				}
				activeChar.sendMessage("Usage: //ctf_jointime <minutes>");
				return false;
			case admin_ctf_eventtime:
				
				if (st.hasMoreTokens())
				{
					
					final String time_s = st.nextToken();
					
					int time = 0;
					
					try
					{
						
						time = Integer.parseInt(time_s);
						
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //ctf_eventtime <minutes>");
						return false;
					}
					
					if (CTF.set_eventTime(time))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
				}
				activeChar.sendMessage("Usage: //ctf_eventtime <minutes>");
				return false;
			case admin_ctf_autoevent:
				if (CTF.get_joinTime() > 0 && CTF.get_eventTime() > 0)
				{
					CTF.autoEvent();
					showMainPage(activeChar);
					return true;
				}
				activeChar.sendMessage("Cannot perform requested operation, times not defined");
				return false;
			case admin_ctf_interval:
				
				if (st.hasMoreTokens())
				{
					
					final String time_s = st.nextToken();
					
					int time = 0;
					
					try
					{
						
						time = Integer.parseInt(time_s);
						
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //ctf_interval <minutes>");
						return false;
					}
					
					if (CTF.set_intervalBetweenMatches(time))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
				}
				activeChar.sendMessage("Usage: //ctf_interval <minutes>");
				return false;
				
			case admin_ctf_minplayers:
				
				if (st.hasMoreTokens())
				{
					
					final String min_s = st.nextToken();
					
					int min = 0;
					
					try
					{
						
						min = Integer.parseInt(min_s);
						
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //ctf_minplayers <number>");
						return false;
					}
					
					if (CTF.set_minPlayers(min))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
					
				}
				activeChar.sendMessage("Usage: //ctf_minplayers <number>");
				return false;
				
			case admin_ctf_maxplayers:
				
				if (st.hasMoreTokens())
				{
					
					final String max_s = st.nextToken();
					
					int max = 0;
					
					try
					{
						
						max = Integer.parseInt(max_s);
						
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //ctf_maxplayers <number>");
						return false;
					}
					
					if (CTF.set_maxPlayers(max))
					{
						showMainPage(activeChar);
						return true;
					}
					activeChar.sendMessage("Cannot perform requested operation, event in progress");
					return false;
				}
				activeChar.sendMessage("Usage: //ctf_maxplayers <number>");
				return false;
				
			default:
				return false;
		}
		
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	public void showEditPage(final L2PcInstance activeChar)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		final TextBuilder replyMSG = new TextBuilder("<html><body>");
		
		replyMSG.append("<center><font color=\"LEVEL\">[CTF Engine]</font></center><br><br><br>");
		replyMSG.append("<table><tr><td><edit var=\"input1\" width=\"125\"></td><td><edit var=\"input2\" width=\"125\"></td></tr></table>");
		replyMSG.append("<table border=\"0\"><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Name\" action=\"bypass -h admin_ctf_name $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Description\" action=\"bypass -h admin_ctf_desc $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Join Location\" action=\"bypass -h admin_ctf_join_loc $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Max lvl\" action=\"bypass -h admin_ctf_maxlvl $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Min lvl\" action=\"bypass -h admin_ctf_minlvl $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Max players\" action=\"bypass -h admin_ctf_maxplayers $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Min players\" action=\"bypass -h admin_ctf_minplayers $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"NPC\" action=\"bypass -h admin_ctf_npc $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"NPC Pos\" action=\"bypass -h admin_ctf_npc_pos\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Reward\" action=\"bypass -h admin_ctf_reward $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Reward Amount\" action=\"bypass -h admin_ctf_reward_amount $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Join Time\" action=\"bypass -h admin_ctf_jointime $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Event Time\" action=\"bypass -h admin_ctf_eventtime $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Interval Time\" action=\"bypass -h admin_ctf_interval $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Team Add\" action=\"bypass -h admin_ctf_team_add $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Team Color\" action=\"bypass -h admin_ctf_team_color $input1 $input2\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Team Pos\" action=\"bypass -h admin_ctf_team_pos $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Team Flag\" action=\"bypass -h admin_ctf_team_flag $input1 $input2\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Team Remove\" action=\"bypass -h admin_ctf_team_remove $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br>");
		replyMSG.append("<br><center><button value=\"Back\" action=\"bypass -h admin_ctf\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	public void showControlPage(final L2PcInstance activeChar)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		final TextBuilder replyMSG = new TextBuilder("<html><body>");
		
		replyMSG.append("<center><font color=\"LEVEL\">[CTF Engine]</font></center><br><br><br>");
		replyMSG.append("<table border=\"0\"><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Join\" action=\"bypass -h admin_ctf_join\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Teleport\" action=\"bypass -h admin_ctf_teleport\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Start\" action=\"bypass -h admin_ctf_start\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><table><tr>");
		replyMSG.append("<table border=\"0\"><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"StartEventOnceTime\" action=\"bypass -h admin_ctf_startevent\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Abort\" action=\"bypass -h admin_ctf_abort\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Finish\" action=\"bypass -h admin_ctf_finish\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Sit Force\" action=\"bypass -h admin_ctf_sit\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Dump\" action=\"bypass -h admin_ctf_dump\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Save\" action=\"bypass -h admin_ctf_save\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Load\" action=\"bypass -h admin_ctf_load\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Auto Event\" action=\"bypass -h admin_ctf_autoevent\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br>");
		replyMSG.append("<br><center><button value=\"Back\" action=\"bypass -h admin_ctf\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	public void showMainPage(final L2PcInstance activeChar)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		final TextBuilder replyMSG = new TextBuilder("<html><body>");
		
		replyMSG.append("<center><font color=\"LEVEL\">[CTF Engine]</font></center><br><br><br>");
		
		replyMSG.append("<table><tr>");
		// if(!CTF._joining && !CTF._started && !CTF._teleport)
		if (!CTF.is_inProgress())
			replyMSG.append("<td width=\"100\"><button value=\"Edit\" action=\"bypass -h admin_ctf_edit\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Control\" action=\"bypass -h admin_ctf_control\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br>");
		
		replyMSG.append("<br><font color=\"LEVEL\">Current event...</font><br1>");
		replyMSG.append("Name:&nbsp;<font color=\"00FF00\">" + CTF.get_eventName() + "</font><br1>");
		replyMSG.append("Description:&nbsp;<font color=\"00FF00\">" + CTF.get_eventDesc() + "</font><br1>");
		replyMSG.append("Joining location name:&nbsp;<font color=\"00FF00\">" + CTF.get_joiningLocationName() + "</font><br1>");
		
		final Location npc_loc = CTF.get_npcLocation();
		
		replyMSG.append("Joining NPC ID:&nbsp;<font color=\"00FF00\">" + CTF.get_npcId() + " on pos " + npc_loc._x + "," + npc_loc._y + "," + npc_loc._z + "</font><br1>");
		replyMSG.append("<button value=\"Tele->NPC\" action=\"bypass -h admin_ctf_tele_npc\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
		replyMSG.append("Reward ID:&nbsp;<font color=\"00FF00\">" + CTF.get_rewardId() + "</font><br1>");
		if (ItemTable.getInstance().getTemplate(CTF.get_rewardId()) != null)
			replyMSG.append("Reward Item:&nbsp;<font color=\"00FF00\">" + ItemTable.getInstance().getTemplate(CTF.get_rewardId()).getName() + "</font><br1>");
		else
			replyMSG.append("Reward Item:&nbsp;<font color=\"00FF00\">(unknown)</font><br1>");
		replyMSG.append("Reward Amount:&nbsp;<font color=\"00FF00\">" + CTF.get_rewardAmount() + "</font><br>");
		replyMSG.append("Min lvl:&nbsp;<font color=\"00FF00\">" + CTF.get_minlvl() + "</font><br1>");
		replyMSG.append("Max lvl:&nbsp;<font color=\"00FF00\">" + CTF.get_maxlvl() + "</font><br><br>");
		replyMSG.append("Min Players:&nbsp;<font color=\"00FF00\">" + CTF.get_minPlayers() + "</font><br1>");
		replyMSG.append("Max Players:&nbsp;<font color=\"00FF00\">" + CTF.get_maxPlayers() + "</font><br>");
		replyMSG.append("Joining Time:&nbsp;<font color=\"00FF00\">" + CTF.get_joinTime() + "</font><br1>");
		replyMSG.append("Event Time:&nbsp;<font color=\"00FF00\">" + CTF.get_eventTime() + "</font><br>");
		if (CTF._teams != null && !CTF._teams.isEmpty())
			replyMSG.append("<font color=\"LEVEL\">Current teams:</font><br1>");
		replyMSG.append("<center><table border=\"0\">");
		
		for (final String team : CTF._teams)
		{
			replyMSG.append("<tr><td width=\"100\">Name: <font color=\"FF0000\">" + team + "</font>");
			
			if (Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE"))
				replyMSG.append("&nbsp;(" + CTF.teamPlayersCount(team) + " joined)");
			else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
			{
				if (CTF.is_teleport() || CTF.is_started())
					replyMSG.append("&nbsp;(" + CTF.teamPlayersCount(team) + " in)");
			}
			replyMSG.append("</td></tr><tr><td>");
			
			String c = Integer.toHexString(CTF._teamColors.get(CTF._teams.indexOf(team)));
			while (c.length() < 6)
				c = "0" + c;
			replyMSG.append("Color: <font color=\"00FF00\">0x" + c.toUpperCase() + "</font><font color=\"" + c + "\"> =) </font>");
			
			replyMSG.append("</td></tr><tr><td>");
			replyMSG.append("<button value=\"Tele->Team\" action=\"bypass -h admin_ctf_tele_team " + team + "\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("</td></tr><tr><td>");
			replyMSG.append(CTF._teamsX.get(CTF._teams.indexOf(team)) + ", " + CTF._teamsY.get(CTF._teams.indexOf(team)) + ", " + CTF._teamsZ.get(CTF._teams.indexOf(team)));
			replyMSG.append("</td></tr><tr><td>");
			replyMSG.append("Flag Id: <font color=\"00FF00\">" + CTF._flagIds.get(CTF._teams.indexOf(team)) + "</font>");
			replyMSG.append("</td></tr><tr><td>");
			replyMSG.append("<button value=\"Tele->Flag\" action=\"bypass -h admin_ctf_tele_flag " + team + "\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("</td></tr><tr><td>");
			replyMSG.append(CTF._flagsX.get(CTF._teams.indexOf(team)) + ", " + CTF._flagsY.get(CTF._teams.indexOf(team)) + ", " + CTF._flagsZ.get(CTF._teams.indexOf(team)) + "</td></tr>");
			// if(!CTF._joining && !CTF._started && !CTF._teleport)
			if (!CTF.is_inProgress())
				replyMSG.append("<tr><td width=\"60\"><button value=\"Remove\" action=\"bypass -h admin_ctf_team_remove " + team + "\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr><tr></tr>");
		}
		
		replyMSG.append("</table></center>");
		
		// if(!CTF._joining && !CTF._started && !CTF._teleport)
		if (!CTF.is_inProgress())
		{
			if (CTF.checkStartJoinOk())
			{
				replyMSG.append("<br1>");
				replyMSG.append("Event is now set up. Press JOIN to start the registration.<br1>");
				replyMSG.append("                           <button value=\"Join\" action=\"bypass -h admin_ctf_join\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br1>");
				replyMSG.append("<br>");
			}
			else
			{
				replyMSG.append("<br1>");
				replyMSG.append("Event is NOT set up. Press <font color=\"LEVEL\">EDIT</font> to create a new event, or <font color=\"LEVEL\">CONTROL</font> to load an existing event.<br1>");
				replyMSG.append("<br>");
			}
		}
		else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && !CTF.is_started())
		{
			replyMSG.append("<br1>");
			replyMSG.append(CTF._playersShuffle.size() + " players participating. Waiting to shuffle in teams(done on teleport)!");
			replyMSG.append("<br><br>");
		}
		
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
}
/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.handler.voice;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javolution.text.TextBuilder;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.templates.L2Item;

public class ExpireItems implements IVoicedCommandHandler, ICustomByPassHandler
{
	private static String[] _voicedCommands =
	{
		"expireitem"
	};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance player, String parameter)
	{
		if (player == null)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("expireitem"))
		{
			showHtm(player);
		}
		
		return true;
	}
	
	public static int getMaxPageNumber(int objectsSize, int pageSize)
	{
		return objectsSize / pageSize + (objectsSize % pageSize == 0 ? 0 : 1);
	}
	
	public static void showHtm(L2PcInstance player)
	{
		showHtm(player, 0);
	}
	
	public static void showHtm(L2PcInstance player, int page)
	{
		// Page limit
		int pageLimit = 8;
		// List
		List<L2ItemInstance> list = Stream.of(player.getInventory().getItems()).sorted(Comparator.comparingInt(item -> item.getMana())).filter(item -> item != null && item.getMana() > 0).collect(Collectors.toList());
		// Calculate page number
		final int max = getMaxPageNumber(list.size(), pageLimit);
		page = page > max ? max : page < 1 ? 1 : page;
		// Cut list up to page number
		list = list.subList((page - 1) * pageLimit, Math.min(page * pageLimit, list.size()));
		
		String filename = "data/html/mods/expireitems/main.htm";
		NpcHtmlMessage htm = new NpcHtmlMessage(1);
		htm.setFile(filename);
		TextBuilder reply = new TextBuilder("");
		int color = 1;
		int number = 0;
		
		for (L2ItemInstance item : list)
		{
			if (item != null && item.getMana() > 0)
			{
				number++;
				
				int time = 0;
				String timeText = "";
				
				if (item.getMana() > 1440)
				{
					time = (item.getMana() / 60 / 24);
					timeText = "days";
				}
				else if (item.getMana() == 1440)
				{
					time = (item.getMana() / 60 / 24);
					timeText = "day";
				}
				else if (item.getMana() < 1440)
				{
					if (item.getMana() > 60)
					{
						time = (item.getMana() / 60);
						timeText = "hours";
					}
					else if (item.getMana() == 60)
					{
						time = (item.getMana() / 60);
						timeText = "hour";
					}
					else if (item.getMana() < 60)
					{
						time = (item.getMana());
						timeText = "minutes";
					}
				}
				
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
				
				reply.append("<td valign=top width=35><img src=\"" + L2Item.getItemIcon(item.getItemId()) + "\" width=32 height=32></img></td>");
				reply.append("<td valign=top width=235>");
				reply.append("<table border=0 width=100%>");
				reply.append("<tr><td width=235>" + item.getItemName() + "</td><td></td></tr>");
				reply.append("<tr><td><font color=\"A2A0A2\">Time left:</font> <font color=\"00FF00\">" + time + "</font> " + timeText + "</td><td></td></tr></table>");
				reply.append("</td></tr></table>");
			}
		}
		
		if (number == 0)
		{
			reply.append("<center>List is empty</center>");
		}
		else
		{
			reply.append("<table width=300><tr>");
			reply.append("<td align=left width=100>" + (page > 1 ? "<button value=\"Prev\" action=\"bypass custom_expire_main " + (page - 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
			reply.append("<td align=center width=100>Page: " + page + " / " + max + "</td>");
			reply.append("<td align=right width=100>" + (page < max ? "<button value=\"Next\" action=\"bypass custom_expire_main " + (page + 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
			reply.append("</tr></table>");
		}
		
		htm.replace("%items%", reply.toString());
		player.sendPacket(htm);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"expire_main"
		};
	}
	
	private enum BysspassCmd
	{
		expire_main
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		BysspassCmd comm = BysspassCmd.valueOf(command);
		
		if (comm == null)
		{
			return;
		}
		
		switch (comm)
		{
			case expire_main:
			{
				showHtm(player, Integer.parseInt(parameters));
				break;
			}
		}
	}
}

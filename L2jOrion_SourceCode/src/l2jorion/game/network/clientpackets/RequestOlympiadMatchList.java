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
package l2jorion.game.network.clientpackets;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.olympiad.Olympiad;
import l2jorion.game.model.olympiad.OlympiadGameManager;
import l2jorion.game.model.olympiad.OlympiadGameTask;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.util.StringUtil;

public final class RequestOlympiadMatchList extends PacketClient
{
	private static final String _C__D0_13_REQUESTOLYMPIADMATCHLIST = "[C] D0:13 RequestOlympiadMatchList";
	
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null || !activeChar.inObserverMode())
		{
			return;
		}
		
		int i = 0;
		
		final StringBuilder sb = new StringBuilder(1500);
		for (OlympiadGameTask task : OlympiadGameManager.getInstance().getOlympiadTasks())
		{
			StringUtil.append(sb, "<tr><td fixwidth=15><a action=\"bypass arenachange ", i, "\">", ++i, "</a></td><td fixwidth=80>");
			
			if (task.isGameStarted())
			{
				if (task.isInTimerTime())
				{
					StringUtil.append(sb, "&$907;"); // Counting In Progress
				}
				else if (task.isBattleStarted())
				{
					StringUtil.append(sb, "&$829;"); // In Progress
				}
				else
				{
					StringUtil.append(sb, "&$908;"); // Terminate
				}
				
				StringUtil.append(sb, "</td><td>", task.getGame().getPlayerNames()[0], "&nbsp; / &nbsp;", task.getGame().getPlayerNames()[1]);
			}
			else
			{
				StringUtil.append(sb, "&$906;", "</td><td>&nbsp;"); // Initial State
			}
			
			StringUtil.append(sb, "</td><td><font color=\"aaccff\"></font></td></tr>");
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "olympiad_arena_observe_list.htm");
		html.replaceNM("%list%", sb.toString());
		activeChar.sendPacket(html);
	}
	
	@Override
	public String getType()
	{
		return _C__D0_13_REQUESTOLYMPIADMATCHLIST;
	}
}
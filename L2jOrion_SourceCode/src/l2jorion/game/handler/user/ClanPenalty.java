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

package l2jorion.game.handler.user;

import java.text.SimpleDateFormat;

import javolution.text.TextBuilder;
import l2jorion.game.handler.IUserCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;

/**
 * Support for clan penalty user command.
 * 
 * @author Tempy
 */
public class ClanPenalty implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		100
	};

	/* (non-Javadoc)
	 * @see l2jorion.game.handler.IUserCommandHandler#useUserCommand(int, l2jorion.game.model.L2PcInstance)
	 */
	@Override
	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if(id != COMMAND_IDS[0])
			return false;

		boolean penalty = false;

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		TextBuilder htmlContent = new TextBuilder("<html><body>");

		htmlContent.append("<center><table width=270 border=0 bgcolor=111111>");
		htmlContent.append("<tr><td width=170>Penalty</td>");
		htmlContent.append("<td width=100 align=center>Expiration Date</td></tr>");
		htmlContent.append("</table><table width=270 border=0><tr>");

		if(activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			htmlContent.append("<td width=170>Unable to join a clan.</td>");
			htmlContent.append("<td width=100 align=center>" + format.format(activeChar.getClanJoinExpiryTime()) + "</td>");
			penalty = true;
		}
		if(activeChar.getClanCreateExpiryTime() > System.currentTimeMillis())
		{
			htmlContent.append("<td width=170>Unable to create a clan.</td>");
			htmlContent.append("<td width=100 align=center>" + format.format(activeChar.getClanCreateExpiryTime()) + "</td>");
			penalty = true;
		}
		if(activeChar.getClan() != null && activeChar.getClan().getCharPenaltyExpiryTime() > System.currentTimeMillis())
		{
			htmlContent.append("<td width=170>Unable to invite a clan member.</td>");
			htmlContent.append("<td width=100 align=center>");
			htmlContent.append(format.format(activeChar.getClan().getCharPenaltyExpiryTime()));
			htmlContent.append("</td>");
			penalty = true;
		}
		if(!penalty)
		{
			htmlContent.append("<td width=170>No penalty is imposed.</td>");
			htmlContent.append("<td width=100 align=center> </td>");
		}

		htmlContent.append("</tr></table><img src=\"L2UI.SquareWhite\" width=270 height=1>");
		htmlContent.append("</center></body></html>");

		NpcHtmlMessage penaltyHtml = new NpcHtmlMessage(0);
		penaltyHtml.setHtml(htmlContent.toString());
		activeChar.sendPacket(penaltyHtml);

		penaltyHtml = null;
		htmlContent = null;
		return true;
	}

	/* (non-Javadoc)
	 * @see l2jorion.game.handler.IUserCommandHandler#getUserCommandList()
	 */
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}

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

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
public class SellBuffs implements IVoicedCommandHandler
{
	private static String[] _voicedCommands = {"sellbuffs","cancelsellbuffs"};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String parameter)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (command.equals(_voicedCommands[0]))
		{
			if (activeChar.isSellBuff())
			{
				activeChar.sendMessage("You're already selling.");
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (activeChar.isDead() || activeChar.isAlikeDead())
			{
				activeChar.sendMessage("You are dead, you can't sell at the moment.");
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (!Config.SELLBUFF_SELLING_EVERYWHERE && activeChar.isInsideZone(ZoneId.ZONE_NOSTORE))
			{
				activeChar.sendMessage("You can sell buffs only in trade zone.");
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (activeChar.getPvpFlag() > 0 || activeChar.isInCombat() || activeChar.getKarma() > 0)
			{
				activeChar.sendMessage("You are in combat mode, you can't sell at the moment.");
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (!Config.LIST_ALLOWED_CLASSES.contains(activeChar.getClassId().getId()))
			{
				activeChar.sendMessage("This class can not sell buffs.");
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			TextBuilder tb = new TextBuilder(0);
			tb.append("<html><body><center>");
			tb.append("<table><tr>");
			tb.append("<td><img src=\"icon.etc_alphabet_b_i00\" width=32 height=32 align=left></td><td><img src=\"icon.etc_alphabet_u_i00\" width=32 height=32 align=left></td>");
			tb.append("<td><img src=\"icon.etc_alphabet_f_i00\" width=32 height=32 align=left></td><td><img src=\"icon.etc_alphabet_f_i00\" width=32 height=32 align=left></td><td><img src=\"icon.etc_alphabet_s_i00\" width=32 height=32 align=left></td>");
			tb.append("</tr></table><br>");
			tb.append("<p><font color=LEVEL>Price for each buff:</font></p>");
			tb.append("<p><edit var=\"pri\" width=75 height=21></p>");
			tb.append("<button value=\"Confirm\" action=\"bypass -h actr $pri\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
			tb.append("</center><br>");
			tb.append("</body></html>");
			NpcHtmlMessage n = new NpcHtmlMessage(0);
			n.setHtml(tb.toString());
			activeChar.sendPacket(n);
		}
		else if (command.equals(_voicedCommands[1]))
		{
			if (activeChar.isSellBuff())
			{
				activeChar.setSellBuff(false);
				activeChar.standUp();
				activeChar.setTeam(0);
				//activeChar.getAppearance().setNameColor(activeChar.getOldNameColor());
				activeChar.setTitle(activeChar.getOldTitle());
				activeChar.broadcastUserInfo();
				activeChar.broadcastTitleInfo();
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}

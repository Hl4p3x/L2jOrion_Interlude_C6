/*
 * L2jOrion Project - www.l2jorion.com 
 * 
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
package l2jorion.game.model.actor.instance;

import javolution.text.TextBuilder;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.util.Broadcast;

/**
 * @author xAddytzu moded by Bobi
 */
public class L2RaidBossManagerInstance extends L2NpcInstance
{
	public L2RaidBossManagerInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		setIsRaid(true);
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (!canTarget(player))
			return;
		
		// Check if the L2PcInstance already target the L2Npc
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			final MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			
			// Send a Server->Client packet ValidateLocation to correct the L2Npc position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2Npc
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
				
				showChatWindow(player);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(new ActionFailed());
	}
	
	@Override
	public void showChatWindow(final L2PcInstance player, final int val)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(this.getObjectId());
		msg.setHtml(rbWindow(player));
		msg.replace("%objectId%", String.valueOf(this.getObjectId()));
		player.sendPacket(msg);
	}
	
	private String rbWindow(final L2PcInstance player)
	{
		final TextBuilder tb = new TextBuilder();
		tb.append("<html><title>L2 Raidboss Manager</title><body>");
		tb.append("<center>");
		tb.append("<br>");
		tb.append("<font color=\"999999\">Raidboss Manager</font><br>");
		tb.append("<img src=\"L2UI.SquareGray\" width=\"200\" height=\"1\"><br>");
		tb.append("Welcome " + player.getName() + "<br>");
		tb.append("<table width=\"85%\"><tr><td>We gatekeepers use the will of the gods to open the doors of time and space and teleport others. Which door would you like to open?</td></tr></table><br>");
		
		tb.append("<img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\"></center><br>");
		tb.append("<table width=180>");
		tb.append("<tr>");
		tb.append("<td><center><a action=\"bypass -h npc_%objectId%_RaidbossLvl_40\">Raidboss Level (40-45)</a></center></td>");
		tb.append("</tr>");
		tb.append("<tr>");
		tb.append("<td><center><a action=\"bypass -h npc_%objectId%_RaidbossLvl_45\">Raidboss Level (45-50)</a></center></td>");
		tb.append("</tr>");
		tb.append("<tr>");
		tb.append("<td><center><a action=\"bypass -h npc_%objectId%_RaidbossLvl_50\">Raidboss Level (50-55)</a></center></td>");
		tb.append("</tr>");
		tb.append("<tr>");
		tb.append("<td><center><a action=\"bypass -h npc_%objectId%_RaidbossLvl_55\">Raidboss Level (55-60)</a></center></td>");
		tb.append("</tr>");
		tb.append("<tr>");
		tb.append("<td><center><a action=\"bypass -h npc_%objectId%_RaidbossLvl_60\">Raidboss Level (60-65)</a></center></td>");
		tb.append("</tr>");
		tb.append("<tr>");
		tb.append("<td><center><a action=\"bypass -h npc_%objectId%_RaidbossLvl_65\">Raidboss Level (65-70)</a></center></td>");
		tb.append("</tr>");
		tb.append("<tr>");
		tb.append("<td><center><a action=\"bypass -h npc_%objectId%_RaidbossLvl_70\">Raidboss Level (70-75)</a></center></td>");
		tb.append("</tr>");
		tb.append("</table>");
		tb.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		tb.append("<font color=\"999999\">Gates of Fire</font></center>");
		tb.append("</body></html>");
		return tb.toString();
	}
}
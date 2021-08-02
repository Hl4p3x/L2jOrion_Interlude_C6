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

import java.util.StringTokenizer;

import l2jorion.game.managers.SiegeManager;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;

public final class L2ObservationInstance extends L2FolkInstance
{
	public L2ObservationInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("observeSiege"))
		{
			String val = command.substring(13);
			StringTokenizer st = new StringTokenizer(val);
			st.nextToken();
			
			if (OlympiadManager.getInstance().isRegistered(player) || player.isInOlympiadMode())
			{
				player.sendMessage("You already participated in Olympiad!");
				return;
			}
			
			if (player._inEventTvT || player._inEventDM || player._inEventCTF)
			{
				player.sendMessage("You already participated in Event!");
				return;
			}
			
			if (player.isInCombat() || player.getPvpFlag() > 0)
			{
				player.sendMessage("You are in combat now!");
				return;
			}
			
			if (SiegeManager.getInstance().getSiege(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())) != null)
			{
				doObserve(player, val);
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_VIEW_SIEGE));
			}
		}
		else if (command.startsWith("observe"))
		{
			if (OlympiadManager.getInstance().isRegistered(player) || player.isInOlympiadMode())
			{
				player.sendMessage("You already participated in Olympiad!");
				return;
			}
			
			if (player._inEventTvT || player._inEventDM || player._inEventCTF)
			{
				player.sendMessage("You already participated in Event!");
				return;
			}
			
			if (player.isInCombat() || player.getPvpFlag() > 0)
			{
				player.sendMessage("You are in combat now!");
				return;
			}
			
			doObserve(player, command.substring(8));
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	@Override
	public String getHtmlPath(L2PcInstance player, final int npcId, final int val)
	{
		String pom = "";
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/observation/" + pom + ".htm";
	}
	
	private void doObserve(L2PcInstance player, String val)
	{
		StringTokenizer st = new StringTokenizer(val);
		
		final int cost = Integer.parseInt(st.nextToken());
		
		final int x = Integer.parseInt(st.nextToken());
		final int y = Integer.parseInt(st.nextToken());
		final int z = Integer.parseInt(st.nextToken());
		
		if (player.reduceAdena("Broadcast", cost, this, true))
		{
			// enter mode
			player.enterObserverMode(x, y, z);
			final ItemList il = new ItemList(player, false);
			player.sendPacket(il);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}

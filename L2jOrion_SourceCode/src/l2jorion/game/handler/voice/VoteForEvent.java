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

import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.random.Rnd;

public class VoteForEvent implements IVoicedCommandHandler, ICustomByPassHandler
{
	public static int voteTvT = 0, voteCTF = 0, voteDM = 0;
	
	private static String[] _voicedCommands =
	{
		"event"
	};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String parameters)
	{
		if (command.equalsIgnoreCase("event"))
		{
			showHtm(activeChar);
		}
		return true;
	}
	
	private static void showHtm(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/mods/event.htm");
		htm.setHtml(text);
		
		if (player.isVoteMenuOn())
		{
			htm.replace("%menuon%", "<button value=\"\" action=\"bypass custom_event_menu\" width=15 height=15 back=\"L2UI.CheckB﻿ox_checked\" fore=\"L2UI.CheckBox_checked\">");
		}
		else
		{
			htm.replace("%menuon%", "<button value=\"\" action=\"bypass custom_event_menu\" width=15 height=15 back=\"L2UI.CheckBox\" fore=\"L2UI.CheckBox\">");
		}
		
		htm.replace("%tvt%", voteTvT);
		htm.replace("%ctf%", voteCTF);
		htm.replace("%dm%", voteDM);
		
		player.sendPacket(htm);
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"event_menu",
			"event_tvt",
			"event_dm",
			"event_ctf"
		};
	}
	
	private enum CommandEnum
	{
		event_menu,
		event_tvt,
		event_dm,
		event_ctf
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		CommandEnum comm = CommandEnum.valueOf(command);
		
		if (comm == null)
		{
			return;
		}
		
		switch (comm)
		{
			case event_menu:
				if (player.isVoteMenuOn())
				{
					player.setVoteMenuOn(false);
				}
				else
				{
					player.setVoteMenuOn(true);
				}
				showHtm(player);
				break;
			case event_tvt:
				if (player.isVotedForEvent())
				{
					player.sendMessage("You already voted.");
					player.sendPacket(new ExShowScreenMessage("You already voted.", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					showHtm(player);
					return;
				}
				
				voteTvT += 1;
				player.setVotedForEvent(true);
				showHtm(player);
				break;
			case event_dm:
				if (player.isVotedForEvent())
				{
					player.sendMessage("You already voted.");
					player.sendPacket(new ExShowScreenMessage("You already voted.", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					showHtm(player);
					return;
				}
				
				voteCTF += 1;
				player.setVotedForEvent(true);
				showHtm(player);
				break;
			case event_ctf:
				if (player.isVotedForEvent())
				{
					player.sendMessage("You already voted.");
					player.sendPacket(new ExShowScreenMessage("You already voted.", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					showHtm(player);
					return;
				}
				
				voteDM += 1;
				player.setVotedForEvent(true);
				showHtm(player);
				break;
		}
	}
	
	protected static class startVoting implements Runnable
	{
		@Override
		public void run()
		{
			waiter(300);
		}
	}
	
	public static void Voting()
	{
		ThreadPoolManager.getInstance().executeAi(new startVoting());
	}
	
	public static void waiter(final long interval)
	{
		final long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000);
		
		while (startWaiterTime + interval > System.currentTimeMillis())
		{
			seconds--;
			switch (seconds)
			{
				case 3600:
				case 1800:
				case 900:
				case 600:
				case 300:
					Announcements.getInstance().gameAnnounceToAll("SYS: Vote for the event! Command: .event");
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						player.setVotedForEvent(false);
						showHtm(player);
					}
					break;
				case 240:
				case 180:
				case 120:
					Announcements.getInstance().gameAnnounceToAll("SYS: " + seconds / 60 + " minutes till voting finish!");
					break;
				case 60:
					Announcements.getInstance().gameAnnounceToAll("SYS: " + seconds / 60 + " minute till voting finish!");
					break;
				case 30:
				case 15:
				case 10:
				case 3:
				case 2:
					Announcements.getInstance().gameAnnounceToAll("SYS: " + seconds + " seconds till voting finish!");
					break;
				case 1:
					Announcements.getInstance().gameAnnounceToAll("SYS: " + seconds + " second till voting finish!");
					break;
				case 0:
					Announcements.getInstance().gameAnnounceToAll("SYS: Voting finished! Starting the event...");
					startEvent();
					break;
			}
			
			final long startOneSecondWaiterStartTime = System.currentTimeMillis();
			
			while (startOneSecondWaiterStartTime + 1000 > System.currentTimeMillis())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (final InterruptedException ie)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						ie.printStackTrace();
					}
				}
			}
		}
	}
	
	public static void startEvent()
	{
		if ((voteTvT > voteCTF) && (voteTvT > voteDM))
		{
			TvT.autoEvent();
		}
		else if ((voteCTF > voteTvT) && (voteCTF > voteDM))
		{
			CTF.autoEvent();
		}
		else if ((voteDM > voteTvT) && (voteDM > voteCTF))
		{
			DM.autoEvent();
		}
		else
		{
			int getRandom = Rnd.get(1, 3);
			
			switch (getRandom)
			{
				case 1:
					TvT.autoEvent();
					break;
				case 2:
					CTF.autoEvent();
					break;
				case 3:
					DM.autoEvent();
					break;
			}
		}
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}

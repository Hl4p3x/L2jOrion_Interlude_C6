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
package l2jorion.game.model.entity.event;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.util.EventData;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

/**
 * This class ...
 * @version $Revision: 1.3.4.1 $ $Date: 2005/03/27 15:29:32 $
 */
public class L2Event
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2Event.class);
	
	public static String eventName = "";
	public static int teamsNumber = 0;
	public static final HashMap<Integer, String> names = new HashMap<>();
	public static final LinkedList<String> participatingPlayers = new LinkedList<>();
	public static final HashMap<Integer, LinkedList<String>> players = new HashMap<>();
	public static int id = 12760;
	public static final LinkedList<String> npcs = new LinkedList<>();
	public static boolean active = false;
	public static final HashMap<String, EventData> connectionLossData = new HashMap<>();
	
	public static int getTeamOfPlayer(final String name)
	{
		for (int i = 1; i <= players.size(); i++)
		{
			LinkedList<String> temp = players.get(i);
			Iterator<String> it = temp.iterator();
			
			while (it.hasNext())
			{
				if (it.next().equals(name))
					return i;
			}
			
			temp = null;
			it = null;
		}
		return 0;
	}
	
	public static String[] getTopNKillers(final int N)
	{
		// this will return top N players sorted by kills, first element in the array will be the one with more kills
		final String[] killers = new String[N];
		String playerTemp = "";
		
		int kills = 0;
		
		final LinkedList<String> killersTemp = new LinkedList<>();
		
		for (int k = 0; k < N; k++)
		{
			kills = 0;
			
			for (int i = 1; i <= teamsNumber; i++)
			{
				LinkedList<String> temp = players.get(i);
				Iterator<String> it = temp.iterator();
				
				while (it.hasNext())
				{
					try
					{
						L2PcInstance player = L2World.getInstance().getPlayer(it.next());
						
						if (!killersTemp.contains(player.getName()))
						{
							if (player.kills.size() > kills)
							{
								kills = player.kills.size();
								playerTemp = player.getName();
							}
						}
						
						player = null;
					}
					catch (final Exception e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
							e.printStackTrace();
					}
				}
				
				temp = null;
				it = null;
			}
			
			killersTemp.add(playerTemp);
		}
		
		for (int i = 0; i < N; i++)
		{
			kills = 0;
			Iterator<String> it = killersTemp.iterator();
			
			while (it.hasNext())
			{
				try
				{
					L2PcInstance player = L2World.getInstance().getPlayer(it.next());
					
					if (player.kills.size() > kills)
					{
						kills = player.kills.size();
						playerTemp = player.getName();
					}
					
					player = null;
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
				}
			}
			
			killers[i] = playerTemp;
			killersTemp.remove(playerTemp);
			
			it = null;
		}
		
		playerTemp = null;
		
		return killers;
	}
	
	public static void showEventHtml(final L2PcInstance player, final String objectid)
	{
		FileInputStream fis = null;
		BufferedInputStream buff = null;
		DataInputStream in = null;
		InputStreamReader isr = null;
		BufferedReader inbr = null;
		
		try
		{
			final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			
			fis = new FileInputStream("data/events/" + eventName);
			buff = new BufferedInputStream(fis);
			in = new DataInputStream(buff);
			isr = new InputStreamReader(in);
			inbr = new BufferedReader(isr);
			
			final TextBuilder replyMSG = new TextBuilder("<html><body>");
			replyMSG.append("<center><font color=\"LEVEL\">" + eventName + "</font><font color=\"FF0000\"> bY " + inbr.readLine() + "</font></center><br>");
			
			replyMSG.append("<br>" + inbr.readLine());
			
			if (L2Event.participatingPlayers.contains(player.getName()))
			{
				replyMSG.append("<br><center>You are already in the event players list !!</center></body></html>");
			}
			else
			{
				replyMSG.append("<br><center><button value=\"Participate !! \" action=\"bypass -h npc_" + objectid + "_event_participate\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");
			}
			
			adminReply.setHtml(replyMSG.toString());
			player.sendPacket(adminReply);
			
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.error(e.getMessage());
		}
		finally
		{
			
			if (inbr != null)
				try
				{
					inbr.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			
			if (isr != null)
				try
				{
					isr.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			
			if (in != null)
				try
				{
					in.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			
			if (buff != null)
				try
				{
					buff.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			
			if (fis != null)
				try
				{
					fis.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			
		}
	}
	
	public static void spawn(final L2PcInstance target, final int npcid)
	{
		
		L2NpcTemplate template1 = NpcTable.getInstance().getTemplate(npcid);
		
		try
		{
			// L2MonsterInstance mob = new L2MonsterInstance(template1);
			
			L2Spawn spawn = new L2Spawn(template1);
			
			spawn.setLocx(target.getX() + 50);
			spawn.setLocy(target.getY() + 50);
			spawn.setLocz(target.getZ());
			spawn.setAmount(1);
			spawn.setHeading(target.getHeading());
			spawn.setRespawnDelay(1);
			
			SpawnTable.getInstance().addNewSpawn(spawn, false);
			
			spawn.init();
			spawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			spawn.getLastSpawn().setName("event inscriptor");
			spawn.getLastSpawn().setTitle(L2Event.eventName);
			spawn.getLastSpawn().isEventMob = true;
			spawn.getLastSpawn().isAggressive();
			spawn.getLastSpawn().decayMe();
			spawn.getLastSpawn().spawnMe(spawn.getLastSpawn().getX(), spawn.getLastSpawn().getY(), spawn.getLastSpawn().getZ());
			
			spawn.getLastSpawn().broadcastPacket(new MagicSkillUser(spawn.getLastSpawn(), spawn.getLastSpawn(), 1034, 1, 1, 1));
			
			npcs.add(String.valueOf(spawn.getLastSpawn().getObjectId()));
			
			spawn = null;
			
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.error(e.getMessage());
		}
		
		template1 = null;
		
	}
	
	public static void announceAllPlayers(final String text)
	{
		CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, "", text);
		
		for (final L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			player.sendPacket(cs);
		}
		
		cs = null;
	}
	
	public static boolean isOnEvent(final L2PcInstance player)
	{
		
		for (int k = 0; k < L2Event.teamsNumber; k++)
		{
			Iterator<String> it = L2Event.players.get(k + 1).iterator();
			
			boolean temp = false;
			
			while (it.hasNext())
			{
				temp = player.getName().equalsIgnoreCase(it.next());
				
				if (temp)
					return true;
			}
			
			it = null;
		}
		return false;
		
	}
	
	public static void inscribePlayer(final L2PcInstance player)
	{
		try
		{
			L2Event.participatingPlayers.add(player.getName());
			player.eventKarma = player.getKarma();
			player.eventX = player.getX();
			player.eventY = player.getY();
			player.eventZ = player.getZ();
			player.eventPkKills = player.getPkKills();
			player.eventPvpKills = player.getPvpKills();
			player.eventTitle = player.getTitle();
			player.kills.clear();
			player.atEvent = true;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.error("error when signing in the event:" + e.getMessage(), e);
		}
	}
	
	public static void restoreChar(final L2PcInstance player)
	{
		try
		{
			player.eventX = connectionLossData.get(player.getName()).eventX;
			player.eventY = connectionLossData.get(player.getName()).eventY;
			player.eventZ = connectionLossData.get(player.getName()).eventZ;
			player.eventKarma = connectionLossData.get(player.getName()).eventKarma;
			player.eventPvpKills = connectionLossData.get(player.getName()).eventPvpKills;
			player.eventPkKills = connectionLossData.get(player.getName()).eventPkKills;
			player.eventTitle = connectionLossData.get(player.getName()).eventTitle;
			player.kills = connectionLossData.get(player.getName()).kills;
			player.eventSitForced = connectionLossData.get(player.getName()).eventSitForced;
			player.atEvent = true;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
		}
	}
	
	public static void restoreAndTeleChar(final L2PcInstance target)
	{
		try
		{
			restoreChar(target);
			target.setTitle(target.eventTitle);
			target.setKarma(target.eventKarma);
			target.setPvpKills(target.eventPvpKills);
			target.setPkKills(target.eventPkKills);
			target.teleToLocation(target.eventX, target.eventY, target.eventZ);
			target.kills.clear();
			target.eventSitForced = false;
			target.atEvent = false;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
		}
	}
}

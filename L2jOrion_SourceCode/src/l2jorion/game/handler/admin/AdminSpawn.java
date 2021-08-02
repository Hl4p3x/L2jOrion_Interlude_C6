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
package l2jorion.game.handler.admin;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.datatables.sql.TeleportLocationTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.DayNightSpawnManager;
import l2jorion.game.managers.RaidBossSpawnManager;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class AdminSpawn implements IAdminCommandHandler
{
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_show_spawns",
		"admin_spawn",
		"admin_spawn_monster",
		"admin_spawn_index",
		"admin_unspawnall",
		"admin_respawnall",
		"admin_spawn_reload",
		"admin_npc_index",
		"admin_spawn_once",
		"admin_show_npcs",
		"admin_teleport_reload",
		"admin_night",
		"admin_day",
		"admin_sendHome"
	};
	
	public static Logger LOG = LoggerFactory.getLogger(AdminSpawn.class.getName());
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_show_spawns"))
		{
			AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
		}
		else if (command.startsWith("admin_spawn_index"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			
			try
			{
				st.nextToken();
				
				int level = Integer.parseInt(st.nextToken());
				int from = 0;
				
				try
				{
					from = Integer.parseInt(st.nextToken());
				}
				catch (NoSuchElementException nsee)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						nsee.printStackTrace();
					}
				}
				
				showMonsters(activeChar, level, from);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
			}
			
			st = null;
		}
		else if (command.equals("admin_show_npcs"))
		{
			AdminHelpPage.showHelpPage(activeChar, "npcs.htm");
		}
		else if (command.startsWith("admin_npc_index"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			
			try
			{
				st.nextToken();
				String letter = st.nextToken();
				
				int from = 0;
				
				try
				{
					from = Integer.parseInt(st.nextToken());
				}
				catch (NoSuchElementException nsee)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						nsee.printStackTrace();
					}
				}
				
				showNpcs(activeChar, letter, from);
				
				letter = null;
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				AdminHelpPage.showHelpPage(activeChar, "npcs.htm");
			}
		}
		else if (command.startsWith("admin_sendHome"))
		{
			L2NpcInstance target = (L2NpcInstance) activeChar.getTarget();
			
			StringTokenizer st = new StringTokenizer(command, " ");
			
			if (st.countTokens() > 2)
			{
				String cmd = st.nextToken();
				String x = st.nextToken();
				String y = st.nextToken();
				String z = st.nextToken();
				
				if (cmd != null && target != null && target.getSpawn() != null)
				{
					target.teleToLocation(Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z), false);
				}
			}
		}
		else if (command.startsWith("admin_spawn") || command.startsWith("admin_spawn_monster"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				String cmd = st.nextToken();
				String id = st.nextToken();
				int mobCount = 1;
				int respawnTime = 1;
				if (st.hasMoreTokens())
				{
					mobCount = Integer.parseInt(st.nextToken());
				}
				if (st.hasMoreTokens())
				{
					respawnTime = Integer.parseInt(st.nextToken());
				}
				
				if (cmd.equalsIgnoreCase("admin_spawn_once"))
				{
					spawnMonster(activeChar, id, respawnTime, mobCount, false);
				}
				else
				{
					spawnMonster(activeChar, id, respawnTime, mobCount, true);
				}
			}
			catch (Exception e)
			{ // Case of wrong or missing monster data
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
			}
		}
		// Command for unspawn all Npcs on Server, use //repsawnall to respawn the npc
		else if (command.startsWith("admin_unspawnall"))
		{
			for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NPC_SERVER_NOT_OPERATING));
			}
			RaidBossSpawnManager.getInstance().DataSave();
			DayNightSpawnManager.getInstance().DataSave();
			L2World.getInstance().deleteVisibleNpcSpawns();
			GmListTable.broadcastMessageToGMs("NPC Unspawn completed!");
		}
		else if (command.startsWith("admin_day"))
		{
			DayNightSpawnManager.getInstance().spawnDayCreatures();
		}
		else if (command.startsWith("admin_night"))
		{
			DayNightSpawnManager.getInstance().spawnNightCreatures();
		}
		else if (command.startsWith("admin_respawnall") || command.startsWith("admin_spawn_reload"))
		{
			// make sure all spawns are deleted
			RaidBossSpawnManager.getInstance().DataSave();
			DayNightSpawnManager.getInstance().DataSave();
			L2World.getInstance().deleteVisibleNpcSpawns();
			// now respawn all
			NpcTable.getInstance().reloadAllNpc();
			SpawnTable.getInstance().reloadAll();
			RaidBossSpawnManager.getInstance().reloadBosses();
			GmListTable.broadcastMessageToGMs("NPC Respawn completed!");
		}
		else if (command.startsWith("admin_teleport_reload"))
		{
			TeleportLocationTable.getInstance().reloadAll();
			GmListTable.broadcastMessageToGMs("Teleport List Table reloaded.");
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void spawnMonster(L2PcInstance activeChar, String monsterId, int respawnTime, int mobCount, boolean permanent)
	{
		L2Object target = activeChar.getTarget();
		if (target == null)
		{
			target = activeChar;
		}
		if (target != activeChar && activeChar.getAccessLevel().isGm())
		{
			target = activeChar;
		}
		
		L2NpcTemplate template1;
		if (monsterId.matches("[0-9]*"))
		{
			// First parameter was an ID number
			int monsterTemplate = Integer.parseInt(monsterId);
			template1 = NpcTable.getInstance().getTemplate(monsterTemplate);
		}
		else
		{
			// First parameter wasn't just numbers so go by name not ID
			monsterId = monsterId.replace('_', ' ');
			template1 = NpcTable.getInstance().getTemplateByName(monsterId);
		}
		
		if (template1 == null)
		{
			activeChar.sendMessage("Attention, wrong NPC ID/Name");
			return;
		}
		
		try
		{
			L2Spawn spawn = new L2Spawn(template1);
			if (Config.SAVE_GMSPAWN_ON_CUSTOM)
			{
				spawn.setCustom(true);
			}
			spawn.setLocx(target.getX());
			spawn.setLocy(target.getY());
			spawn.setLocz(target.getZ());
			spawn.setAmount(mobCount);
			spawn.setHeading(activeChar.getHeading());
			spawn.setNoRandomLoc(true);
			spawn.setRespawnDelay(respawnTime);
			
			if (RaidBossSpawnManager.getInstance().getValidTemplate(spawn.getNpcid()) != null)
			{
				RaidBossSpawnManager.getInstance().addNewSpawn(spawn, 0, template1.getStatsSet().getDouble("baseHpMax"), template1.getStatsSet().getDouble("baseMpMax"), permanent);
			}
			else
			{
				SpawnTable.getInstance().addNewSpawn(spawn, permanent);
			}
			
			spawn.init();
			
			if (!permanent)
			{
				spawn.stopRespawn();
			}
			
			activeChar.sendMessage("Created " + template1.name + " on " + target.getObjectId());
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
		}
	}
	
	private void showMonsters(L2PcInstance activeChar, int level, int from)
	{
		TextBuilder tb = new TextBuilder();
		
		L2NpcTemplate[] mobs = NpcTable.getInstance().getAllMonstersOfLevel(level);
		
		// Start
		tb.append("<html><title>Spawn Monster:</title><body><p> Level " + level + ":<br>Total Npc's : " + mobs.length + "<br>");
		String end1 = "<br><center><button value=\"Next\" action=\"bypass -h admin_spawn_index " + level + " $from$\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>";
		String end2 = "<br><center><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>";
		
		// Loop
		boolean ended = true;
		for (int i = from; i < mobs.length; i++)
		{
			String txt = "<a action=\"bypass -h admin_spawn_monster " + mobs[i].npcId + "\">" + mobs[i].name + "</a><br1>";
			
			if (tb.length() + txt.length() + end2.length() > 8192)
			{
				end1 = end1.replace("$from$", "" + i);
				ended = false;
				
				break;
			}
			
			tb.append(txt);
			txt = null;
		}
		
		// End
		if (ended)
		{
			tb.append(end2);
		}
		else
		{
			tb.append(end1);
		}
		
		activeChar.sendPacket(new NpcHtmlMessage(5, tb.toString()));
		
		end1 = null;
		end2 = null;
		mobs = null;
		tb = null;
	}
	
	private void showNpcs(L2PcInstance activeChar, String starting, int from)
	{
		TextBuilder tb = new TextBuilder();
		L2NpcTemplate[] mobs = NpcTable.getInstance().getAllNpcStartingWith(starting);
		
		// Start
		tb.append("<html><title>Spawn Monster:</title><body><p> There are " + mobs.length + " Npcs whose name starts with " + starting + ":<br>");
		String end1 = "<br><center><button value=\"Next\" action=\"bypass -h admin_npc_index " + starting + " $from$\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>";
		String end2 = "<br><center><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>";
		
		// Loop
		boolean ended = true;
		for (int i = from; i < mobs.length; i++)
		{
			String txt = "<a action=\"bypass -h admin_spawn_monster " + mobs[i].npcId + "\">" + mobs[i].name + "</a><br1>";
			
			if (tb.length() + txt.length() + end2.length() > 8192)
			{
				end1 = end1.replace("$from$", "" + i);
				ended = false;
				
				break;
			}
			tb.append(txt);
			txt = null;
		}
		// End
		if (ended)
		{
			tb.append(end2);
		}
		else
		{
			tb.append(end1);
		}
		
		activeChar.sendPacket(new NpcHtmlMessage(5, tb.toString()));
		
		tb = null;
		mobs = null;
		end1 = null;
		end2 = null;
	}
}

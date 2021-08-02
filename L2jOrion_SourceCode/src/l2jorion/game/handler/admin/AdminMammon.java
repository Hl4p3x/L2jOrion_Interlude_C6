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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package l2jorion.game.handler.admin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.managers.RaidBossSpawnManager;
import l2jorion.game.model.actor.instance.AutoSpawnInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.model.spawn.AutoSpawn;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;

public class AdminMammon implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_mammon_find",
		"admin_mammon_respawn",
		"admin_list_spawns",
		"admin_list_spawns2",
		"admin_msg"
	};
	
	private final boolean _isSealValidation = SevenSigns.getInstance().isSealValidationPeriod();
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		int npcId = 0;
		int teleportIndex = -1;
		
		AutoSpawnInstance blackSpawnInst = AutoSpawn.getInstance().getAutoSpawnInstance(SevenSigns.MAMMON_BLACKSMITH_ID, false);
		AutoSpawnInstance merchSpawnInst = AutoSpawn.getInstance().getAutoSpawnInstance(SevenSigns.MAMMON_MERCHANT_ID, false);
		
		if (command.startsWith("admin_mammon_find"))
		{
			try
			{
				if (command.length() > 17)
				{
					teleportIndex = Integer.parseInt(command.substring(18));
				}
			}
			catch (final Exception NumberFormatException)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					NumberFormatException.printStackTrace();
				}
				
				activeChar.sendMessage("Usage: //mammon_find [teleportIndex] (where 1 = Blacksmith, 2 = Merchant)");
			}
			
			if (!_isSealValidation)
			{
				activeChar.sendMessage("The competition period is currently in effect.");
				return true;
			}
			
			if (blackSpawnInst != null)
			{
				L2NpcInstance[] blackInst = blackSpawnInst.getNPCInstanceList();
				if (blackInst.length > 0)
				{
					final int x1 = blackInst[0].getX(), y1 = blackInst[0].getY(), z1 = blackInst[0].getZ();
					activeChar.sendMessage("Blacksmith of Mammon: " + x1 + " " + y1 + " " + z1);
					
					if (teleportIndex == 1)
					{
						activeChar.teleToLocation(x1, y1, z1, true);
					}
				}
			}
			else
			{
				activeChar.sendMessage("Blacksmith of Mammon isn't registered for spawn.");
			}
			
			if (merchSpawnInst != null)
			{
				L2NpcInstance[] merchInst = merchSpawnInst.getNPCInstanceList();
				
				if (merchInst.length > 0)
				{
					final int x2 = merchInst[0].getX(), y2 = merchInst[0].getY(), z2 = merchInst[0].getZ();
					
					activeChar.sendMessage("Merchant of Mammon: " + x2 + " " + y2 + " " + z2);
					
					if (teleportIndex == 2)
					{
						activeChar.teleToLocation(x2, y2, z2, true);
					}
				}
			}
			else
			{
				activeChar.sendMessage("Merchant of Mammon isn't registered for spawn.");
			}
		}
		
		else if (command.startsWith("admin_mammon_respawn"))
		{
			if (!_isSealValidation)
			{
				activeChar.sendMessage("The competition period is currently in effect.");
				return true;
			}
			
			if (merchSpawnInst != null)
			{
				final long merchRespawn = AutoSpawn.getInstance().getTimeToNextSpawn(merchSpawnInst);
				activeChar.sendMessage("The Merchant of Mammon will respawn in " + merchRespawn / 60000 + " minute(s).");
			}
			else
			{
				activeChar.sendMessage("Merchant of Mammon isn't registered for spawn.");
			}
			
			if (blackSpawnInst != null)
			{
				final long blackRespawn = AutoSpawn.getInstance().getTimeToNextSpawn(blackSpawnInst);
				activeChar.sendMessage("The Blacksmith of Mammon will respawn in " + blackRespawn / 60000 + " minute(s).");
			}
			else
			{
				activeChar.sendMessage("Blacksmith of Mammon isn't registered for spawn.");
			}
		}
		
		else if (command.startsWith("admin_list_spawns"))
		{
			boolean GrandBoss = false;
			boolean RaidBoss = false;
			
			String[] params = command.split(" ");
			
			Pattern pattern = Pattern.compile("[0-9]*");
			Matcher regexp = pattern.matcher(params[1]);
			boolean matches = regexp.matches();
			
			if (matches)
			{
				npcId = Integer.parseInt(params[1]);
			}
			else
			{
				params[1] = params[1].replace('_', ' ');
				L2NpcTemplate template = NpcTable.getInstance().getTemplateByName(params[1]);
				if (template != null)
				{
					npcId = template.getNpcId();
				}
				else
				{
					activeChar.sendMessage("Name not found:" + params[1]);
					return false;
				}
			}
			
			if (params.length > 2)
			{
				teleportIndex = Integer.parseInt(params[2]);
			}
			
			if (npcId > 0)
			{
				L2NpcTemplate creature = NpcTable.getInstance().getTemplate(npcId);
				if (creature == null)
				{
					activeChar.sendMessage("Wrong Id:" + npcId);
					return false;
				}
				
				if (NpcTable.getInstance().getTemplate(npcId).getType().contains("L2GrandBoss"))
				{
					GrandBoss = true;
				}
				else if (NpcTable.getInstance().getTemplate(npcId).getType().contains("L2RaidBoss"))
				{
					RaidBoss = true;
				}
				
				if (GrandBoss)
				{
					GrandBossManager.getInstance().findGrandBoss(activeChar, npcId, teleportIndex);
				}
				else if (RaidBoss)
				{
					RaidBossSpawnManager.getInstance().findRaidBoss(activeChar, npcId, teleportIndex);
				}
				else
				{
					SpawnTable.getInstance().findNPCInstances(activeChar, npcId, teleportIndex);
				}
			}
		}
		else if (command.startsWith("admin_msg"))
		{
			int msgId = -1;
			
			try
			{
				msgId = Integer.parseInt(command.substring(10).trim());
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Command format: //msg <SYSTEM_MSG_ID>");
				return true;
			}
			activeChar.sendPacket(new SystemMessage(msgId));
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
}

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

import java.io.File;
import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.controllers.TradeController;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.NpcWalkerRoutesTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.TeleportLocationTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.AchievementManager;
import l2jorion.game.managers.DatatablesManager;
import l2jorion.game.managers.Manager;
import l2jorion.game.managers.QuestManager;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.multisell.L2Multisell;
import l2jorion.game.scripting.L2ScriptEngineManager;

public class AdminReload implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_reload"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (command.startsWith("admin_reload"))
		{
			sendReloadPage(activeChar);
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			
			if (!st.hasMoreTokens())
			{
				activeChar.sendMessage("Usage:  //reload <type>");
				return false;
			}
			
			try
			{
				final String type = st.nextToken();
				
				if (type.equals("multisell"))
				{
					L2Multisell.getInstance().reload();
					sendReloadPage(activeChar);
					activeChar.sendMessage("Multisell reloaded.");
				}
				else if (type.startsWith("teleport"))
				{
					TeleportLocationTable.getInstance().reloadAll();
					sendReloadPage(activeChar);
					activeChar.sendMessage("Teleport location table reloaded.");
				}
				else if (type.startsWith("skill"))
				{
					SkillTable.getInstance().reload();
					sendReloadPage(activeChar);
					activeChar.sendMessage("Skills reloaded.");
				}
				else if (type.equals("npc"))
				{
					NpcTable.getInstance().reloadAllNpc();
					sendReloadPage(activeChar);
					activeChar.sendMessage("Npcs reloaded.");
				}
				else if (type.startsWith("htm"))
				{
					HtmCache.getInstance().reload();
					sendReloadPage(activeChar);
					activeChar.sendMessage("HTML Cache: " + HtmCache.getInstance().getMemoryUsage() + " megabytes on " + HtmCache.getInstance().getLoadedFiles() + " files loaded");
				}
				else if (type.startsWith("item"))
				{
					ItemTable.getInstance().reload();
					sendReloadPage(activeChar);
					activeChar.sendMessage("Item templates reloaded");
				}
				else if (type.startsWith("instancemanager"))
				{
					Manager.reloadAll();
					sendReloadPage(activeChar);
					activeChar.sendMessage("All instance manager has been reloaded");
				}
				else if (type.startsWith("npcwalkers"))
				{
					NpcWalkerRoutesTable.getInstance().load();
					sendReloadPage(activeChar);
					activeChar.sendMessage("All NPC walker routes have been reloaded");
				}
				else if (type.startsWith("quests"))
				{
					final String folder = "quests";
					QuestManager.getInstance().reload(folder);
					sendReloadPage(activeChar);
					activeChar.sendMessage("Quests Reloaded.");
				}
				else if (type.startsWith("npcbuffers"))
				{
					DatatablesManager.reloadAll();
					sendReloadPage(activeChar);
					activeChar.sendMessage("All Buffer skills tables have been reloaded");
				}
				else if (type.equals("configs"))
				{
					Config.load();
					sendReloadPage(activeChar);
					activeChar.sendMessage("Server Config Reloaded.");
				}
				else if (type.equals("tradelist"))
				{
					TradeController.reload();
					sendReloadPage(activeChar);
					activeChar.sendMessage("TradeList Table reloaded.");
				}
				else if (type.equals("dbs"))
				{
					DatatablesManager.reloadAll();
					sendReloadPage(activeChar);
					activeChar.sendMessage("BufferSkillsTable reloaded.");
					activeChar.sendMessage("NpcBufferSkillIdsTable reloaded.");
					activeChar.sendMessage("AccessLevels reloaded.");
					activeChar.sendMessage("AdminCommandAccessRights reloaded.");
					activeChar.sendMessage("GmListTable reloaded.");
					activeChar.sendMessage("ClanTable reloaded.");
					activeChar.sendMessage("AugmentationData reloaded.");
					activeChar.sendMessage("HelperBuffTable reloaded.");
				}
				else if (type.startsWith("scripts_custom"))
				{
					try
					{
						final File custom_scripts_dir = new File(Config.DATAPACK_ROOT + "/data/scripts/custom");
						L2ScriptEngineManager.getInstance().executeAllScriptsInDirectory(custom_scripts_dir, true, 3);
						
					}
					catch (final Exception ioe)
					{
						activeChar.sendMessage("Failed loading " + Config.DATAPACK_ROOT + "/data/scripts/custom scripts, no script going to be loaded");
						ioe.printStackTrace();
					}
					
				}
				else if (type.startsWith("zone"))
				{
					ZoneManager.getInstance().reload();
					activeChar.sendMessage("Zones reloaded.");
				}
				else if (type.startsWith("ach"))
				{
					AchievementManager.getInstance().reload();
					activeChar.sendMessage("Achievement data has been reloaded.");
				}
				/*
				 * else if (type.startsWith("scripts_faenor")) { try { FaenorScriptEngine.getInstance().reloadPackages(); } catch (final Exception ioe) { activeChar.sendMessage("Failed loading faenor scripts, no script going to be loaded"); ioe.printStackTrace(); } }
				 */
				activeChar.sendMessage("WARNING: There are several known issues regarding this feature. Reloading server data during runtime is STRONGLY NOT RECOMMENDED for live servers, just for developing environments.");
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Usage:  //reload <type>");
			}
		}
		return true;
	}
	
	private void sendReloadPage(final L2PcInstance activeChar)
	{
		AdminHelpPage.showSubMenuPage(activeChar, "reload_menu.htm");
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}

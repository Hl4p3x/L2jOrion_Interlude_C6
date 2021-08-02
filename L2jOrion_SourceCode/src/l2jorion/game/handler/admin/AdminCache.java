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
package l2jorion.game.handler.admin;

import java.io.File;
import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.cache.CrestCache;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;

public class AdminCache implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_cache_htm_rebuild",
		"admin_cache_htm_reload",
		"admin_cache_reload_path",
		"admin_cache_reload_file",
		"admin_cache_crest_rebuild",
		"admin_cache_crest_reload",
		"admin_cache_crest_fix"
	};
	
	private enum CommandEnum
	{
		admin_cache_htm_rebuild,
		admin_cache_htm_reload,
		admin_cache_reload_path,
		admin_cache_reload_file,
		admin_cache_crest_rebuild,
		admin_cache_crest_reload,
		admin_cache_crest_fix
	}
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		
		final CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
		{
			return false;
		}
		
		switch (comm)
		{
			case admin_cache_htm_reload:
			case admin_cache_htm_rebuild:
				HtmCache.getInstance().reload(Config.DATAPACK_ROOT);
				activeChar.sendMessage("HTML Cache: " + HtmCache.getInstance().getMemoryUsage() + " MB on " + HtmCache.getInstance().getLoadedFiles() + " file(s) loaded.");
				return true;
			
			case admin_cache_reload_path:
				if (st.hasMoreTokens())
				{
					final String path = st.nextToken();
					HtmCache.getInstance().reloadPath(new File(Config.DATAPACK_ROOT, path));
					activeChar.sendMessage("HTML Cache: " + HtmCache.getInstance().getMemoryUsage() + " MB in " + HtmCache.getInstance().getLoadedFiles() + " file(s) loaded.");
					return true;
				}
				activeChar.sendMessage("Usage: //cache_reload_path <path>");
				return false;
			case admin_cache_reload_file:
				
				if (st.hasMoreTokens())
				{
					
					String path = st.nextToken();
					if (HtmCache.getInstance().loadFile(new File(Config.DATAPACK_ROOT, path)) != null)
					{
						activeChar.sendMessage("HTML Cache: file was loaded");
						path = null;
					}
					else
					{
						activeChar.sendMessage("HTML Cache: file can't be loaded");
						path = null;
					}
					return true;
				}
				activeChar.sendMessage("Usage: //cache_reload_file <relative_path/file>");
				return false;
			
			case admin_cache_crest_rebuild:
			case admin_cache_crest_reload:
				CrestCache.getInstance().reload();
				activeChar.sendMessage("Crests have been reloaded.");
				return true;
			
			default:
			{
				return false;
			}
		}
		
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}

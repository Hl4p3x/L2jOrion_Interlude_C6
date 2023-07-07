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

import l2jorion.Config;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.managers.RaidBossSpawnManager;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class AdminDelete implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_delete"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (command.equals("admin_delete"))
		{
			handleDelete(activeChar);
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleDelete(final L2PcInstance activeChar)
	{
		L2Object obj = activeChar.getTarget();
		
		if (obj != null && obj instanceof L2NpcInstance)
		{
			final L2NpcInstance target = (L2NpcInstance) obj;
			target.deleteMe();
			
			L2Spawn spawn = target.getSpawn();
			
			if (!(Config.ALT_DEV_NO_SPAWNS))
			{
				if (spawn != null)
				{
					spawn.stopRespawn();
					
					if (RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcid()) && !spawn.is_customBossInstance())
					{
						RaidBossSpawnManager.getInstance().deleteSpawn(spawn, false);
					}
					else
					{
						boolean update_db = true;
						if (GrandBossManager.getInstance().isDefined(spawn.getNpcid()) && spawn.is_customBossInstance()) // if custom grandboss instance, it's not saved on database
						{
							update_db = false;
						}
						
						SpawnTable.getInstance().deleteSpawn(spawn, update_db);
					}
				}
			}
			
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
			sm.addString("Deleted: " + target.getName() + " Object Id: " + target.getObjectId());
			activeChar.sendPacket(sm);
		}
		else
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
			sm.addString("Incorrect target.");
			activeChar.sendPacket(sm);
		}
	}
}

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

import l2jorion.game.datatables.csv.DoorTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Castle;

public class AdminDoorControl implements IAdminCommandHandler
{
	private static DoorTable _doorTable;
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_open",
		"admin_close",
		"admin_openall",
		"admin_closeall"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		_doorTable = DoorTable.getInstance();
		
		L2Object target2 = null;
		
		if (command.startsWith("admin_close ")) // id
		{
			try
			{
				final int doorId = Integer.parseInt(command.substring(12));
				
				if (_doorTable.getDoor(doorId) != null)
				{
					_doorTable.getDoor(doorId).closeMe();
				}
				else
				{
					for (final Castle castle : CastleManager.getInstance().getCastles())
						if (castle.getDoor(doorId) != null)
						{
							castle.getDoor(doorId).closeMe();
						}
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Wrong ID door.");
				e.printStackTrace();
				return false;
			}
		}
		else if (command.equals("admin_close")) // target
		{
			target2 = activeChar.getTarget();
			
			if (target2 instanceof L2DoorInstance)
			{
				((L2DoorInstance) target2).closeMe();
			}
			else
			{
				activeChar.sendMessage("Incorrect target.");
			}
		}
		else if (command.startsWith("admin_open ")) // id
		{
			try
			{
				final int doorId = Integer.parseInt(command.substring(11));
				
				if (_doorTable.getDoor(doorId) != null)
				{
					_doorTable.getDoor(doorId).openMe();
				}
				else
				{
					for (final Castle castle : CastleManager.getInstance().getCastles())
					{
						if (castle.getDoor(doorId) != null)
						{
							castle.getDoor(doorId).openMe();
						}
					}
				}
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Wrong ID door.");
				e.printStackTrace();
				return false;
			}
		}
		else if (command.equals("admin_open")) // target
		{
			target2 = activeChar.getTarget();
			
			if (target2 instanceof L2DoorInstance)
			{
				((L2DoorInstance) target2).openMe();
			}
			else
			{
				activeChar.sendMessage("Incorrect target.");
			}
			
			target2 = null;
		}
		
		else if (command.equals("admin_closeall"))
		{
			try
			{
				for (final L2DoorInstance door : _doorTable.getDoors())
				{
					door.closeMe();
				}
				
				for (final Castle castle : CastleManager.getInstance().getCastles())
				{
					for (final L2DoorInstance door : castle.getDoors())
					{
						door.closeMe();
					}
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
				return false;
			}
		}
		else if (command.equals("admin_openall"))
		{
			try
			{
				for (final L2DoorInstance door : _doorTable.getDoors())
				{
					door.openMe();
				}
				
				for (final Castle castle : CastleManager.getInstance().getCastles())
				{
					for (final L2DoorInstance door : castle.getDoors())
					{
						door.openMe();
					}
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}

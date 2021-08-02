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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2jorion.Config;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class AdminRepairChar implements IAdminCommandHandler
{
	private static Logger LOG = LoggerFactory.getLogger(AdminRepairChar.class);
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_restore",
		"admin_repair"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		handleRepair(command);
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleRepair(final String command)
	{
		String[] parts = command.split(" ");
		
		if (parts.length != 2)
		{
			return;
		}
		
		String cmd = "UPDATE characters SET x=-84318, y=244579, z=-3730 WHERE char_name=?";
		Connection connection = null;
		
		try
		{
			connection = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement(cmd);
			statement.setString(1, parts[1]);
			statement.execute();
			DatabaseUtils.close(statement);
			
			statement = connection.prepareStatement("SELECT obj_id FROM characters where char_name=?");
			statement.setString(1, parts[1]);
			ResultSet rset = statement.executeQuery();
			
			int objId = 0;
			
			if (rset.next())
			{
				objId = rset.getInt(1);
			}
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
			
			if (objId == 0)
			{
				CloseUtil.close(connection);
				return;
			}
			
			statement = connection.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=?");
			statement.setInt(1, objId);
			statement.execute();
			DatabaseUtils.close(statement);
			
			statement = connection.prepareStatement("UPDATE items SET loc=\"INVENTORY\" WHERE owner_id=?");
			statement.setInt(1, objId);
			statement.execute();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("Could not repair char:", e);
		}
		finally
		{
			CloseUtil.close(connection);
		}
	}
}

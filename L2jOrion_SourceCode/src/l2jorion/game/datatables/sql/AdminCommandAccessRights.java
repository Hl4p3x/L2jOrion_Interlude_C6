package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.AccessLevel;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class AdminCommandAccessRights
{
	protected static final Logger LOG = LoggerFactory.getLogger(AdminCommandAccessRights.class);
	
	private static AdminCommandAccessRights _instance = null;
	
	private final Map<String, Integer> adminCommandAccessRights = new FastMap<>();
	
	private AdminCommandAccessRights()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement stmt = con.prepareStatement("SELECT * FROM admin_command_access_rights");
			final ResultSet rset = stmt.executeQuery();
			String adminCommand = null;
			int accessLevels = 1;
			
			while (rset.next())
			{
				adminCommand = rset.getString("adminCommand");
				accessLevels = rset.getInt("accessLevels");
				adminCommandAccessRights.put(adminCommand, accessLevels);
			}
			DatabaseUtils.close(rset);
			stmt.close();
		}
		catch (final SQLException e)
		{
			LOG.error("Admin Access Rights: Error loading from database", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		LOG.info("AdminCommandAccessRights: Loaded " + adminCommandAccessRights.size() + " access rights");
	}
	
	public static AdminCommandAccessRights getInstance()
	{
		return _instance == null ? (_instance = new AdminCommandAccessRights()) : _instance;
	}
	
	public static void reload()
	{
		_instance = null;
		getInstance();
	}
	
	public int accessRightForCommand(final String command)
	{
		int out = -1;
		
		if (adminCommandAccessRights.containsKey(command))
		{
			out = adminCommandAccessRights.get(command);
		}
		
		return out;
	}
	
	public boolean hasAccess(final String adminCommand, final AccessLevel playerAccessLevel)
	{
		if (playerAccessLevel.getLevel() <= 0)
		{
			return false;
		}
		
		if (!playerAccessLevel.isGm())
		{
			return false;
		}
		
		if (playerAccessLevel.getLevel() == Config.MASTERACCESS_LEVEL)
		{
			return true;
		}
		
		String command = adminCommand;
		if (adminCommand.indexOf(" ") != -1)
		{
			command = adminCommand.substring(0, adminCommand.indexOf(" "));
		}
		
		int commandAccessLevel = 0;
		if (adminCommandAccessRights.get(command) != null)
		{
			commandAccessLevel = adminCommandAccessRights.get(command);
		}
		
		if (commandAccessLevel == 0)
		{
			LOG.warn("AdminCommandAccessRights: No rights found for admin command: " + command);
			return false;
		}
		else if (commandAccessLevel >= playerAccessLevel.getLevel())
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}
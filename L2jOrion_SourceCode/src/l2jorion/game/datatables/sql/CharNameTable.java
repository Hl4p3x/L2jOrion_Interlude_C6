package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class CharNameTable
{
	private final static Logger LOG = LoggerFactory.getLogger(CharNameTable.class);
	
	private final Map<Integer, String> _chars;
	private final Map<Integer, Integer> _accessLevels;
	
	protected CharNameTable()
	{
		_chars = new HashMap<>();
		_accessLevels = new HashMap<>();
	}
	
	private static CharNameTable _instance;
	
	public static CharNameTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new CharNameTable();
		}
		return _instance;
	}
	
	public synchronized boolean doesCharNameExist(String name)
	{
		boolean result = true;
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT account_name FROM characters WHERE char_name=?");
			statement.setString(1, name);
			final ResultSet rset = statement.executeQuery();
			result = rset.next();
			
			statement.close();
			rset.close();
		}
		catch (SQLException e)
		{
			LOG.error("could not check existing charname " + " " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		return result;
	}
	
	public synchronized boolean doesClanNameExist(String name)
	{
		boolean result = true;
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT clan_name FROM clan_data WHERE clan_name=?");
			statement.setString(1, name);
			final ResultSet rset = statement.executeQuery();
			result = rset.next();
			
			statement.close();
			rset.close();
		}
		catch (SQLException e)
		{
			LOG.error("could not check existing clan name " + " " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		return result;
	}
	
	public int accountCharNumber(String account)
	{
		Connection con = null;
		int number = 0;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT COUNT(char_name) FROM characters WHERE account_name=?");
			statement.setString(1, account);
			final ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				number = rset.getInt(1);
			}
			
			statement.close();
			rset.close();
		}
		catch (SQLException e)
		{
			LOG.error("could not check existing char number");
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		return number;
	}
	
	public int ipCharNumber(String ip)
	{
		Connection con = null;
		int number = 0;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT count(char_name) FROM " + Config.LOGINSERVER_DB + ".accounts a, " + Config.GAMESERVER_DB + ".characters c where a.login = c.account_name and a.lastIP=?");
			statement.setString(1, ip);
			final ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				number = rset.getInt(1);
			}
			
			statement.close();
			rset.close();
		}
		catch (SQLException e)
		{
			LOG.error("could not check existing char number");
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		return number;
	}
	
	public final String getNameById(int id)
	{
		Connection con = null;
		if (id <= 0)
		{
			return null;
		}
		
		String name = _chars.get(id);
		if (name != null)
		{
			return name;
		}
		
		int accessLevel = 0;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT char_name,accesslevel FROM characters WHERE obj_Id=?");
			statement.setInt(1, id);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				name = rset.getString(1);
				accessLevel = rset.getInt(2);
			}
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.error("Could not check existing char id: " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (name != null && !name.isEmpty())
		{
			_chars.put(id, name);
			_accessLevels.put(id, accessLevel);
			return name;
		}
		
		return null;
	}
	
	public final int getIdByName(String name)
	{
		if (name == null || name.isEmpty())
		{
			return -1;
		}
		
		for (Map.Entry<Integer, String> entry : _chars.entrySet())
		{
			if (entry.getValue().equalsIgnoreCase(name))
			{
				return entry.getKey();
			}
		}
		
		int id = -1;
		int accessLevel = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT obj_Id,accesslevel FROM characters WHERE char_name=?");
			statement.setString(1, name);
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				id = rset.getInt(1);
				accessLevel = rset.getInt(2);
			}
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.error("Could not check existing char name: " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (id > 0)
		{
			_chars.put(id, name);
			_accessLevels.put(id, accessLevel);
			return id;
		}
		
		return -1;
	}
	
	public final int getAccessLevelById(int objectId)
	{
		if (getNameById(objectId) != null)
		{
			return _accessLevels.get(objectId);
		}
		
		return 0;
	}
}

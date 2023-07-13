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
package l2jorion.util.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.mariadb.jdbc.MariaDbPoolDataSource;

import l2jorion.Config;
import l2jorion.game.thread.ThreadPoolManager;

public class L2DatabaseFactory
{
	private static final Logger LOG = Logger.getLogger(L2DatabaseFactory.class.getName());
	
	private static L2DatabaseFactory _instance;
	private static final MariaDbPoolDataSource dataSource = new MariaDbPoolDataSource(Config.DATABASE_URL + "&user=" + Config.DATABASE_LOGIN + "&password=" + Config.DATABASE_PASSWORD + "&maxPoolSize=" + Config.DATABASE_MAX_CONNECTIONS);
	
	public static void init()
	{
		// Test if connection is valid.
		try
		{
			dataSource.getConnection().close();
			LOG.info("Database: Initialized.");
		}
		catch (Exception e)
		{
			LOG.info("Database: Problem on initialize. " + e);
		}
	}
	
	public void shutdown()
	{
		try
		{
			dataSource.close();
		}
		catch (Exception e)
		{
			LOG.severe("Error while closing the database connection pool." + e);
		}
	}
	
	// TODO Drop that shit
	public final String safetyString(final String... whatToCheck)
	{
		final char braceLeft;
		final char braceRight;
		
		braceLeft = '`';
		braceRight = '`';
		
		int length = 0;
		
		for (final String word : whatToCheck)
		{
			length += word.length() + 4;
		}
		
		final StringBuilder sbResult = new StringBuilder(length);
		
		for (final String word : whatToCheck)
		{
			if (sbResult.length() > 0)
			{
				sbResult.append(", ");
			}
			
			sbResult.append(braceLeft);
			sbResult.append(word);
			sbResult.append(braceRight);
		}
		
		return sbResult.toString();
	}
	
	public static L2DatabaseFactory getInstance()
	{
		if (_instance == null)
		{
			_instance = new L2DatabaseFactory();
		}
		
		return _instance;
	}
	
	public Connection getConnection()
	{
		Connection con = null;
		while (con == null)
		{
			try
			{
				con = dataSource.getConnection();
			}
			catch (final SQLException e)
			{
				LOG.severe("L2DatabaseFactory: Connection failed, trying again..." + e);
			}
		}
		return con;
	}
	
	public Connection getConnection(boolean checkclose)
	{
		Connection con = null;
		while (con == null)
		{
			try
			{
				con = dataSource.getConnection();
				if (checkclose && Config.DATABASE_CONNECTION_TIMEOUT > 0)
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new ConnectionCloser(con, new RuntimeException()), Config.DATABASE_CONNECTION_TIMEOUT);
				}
			}
			catch (final SQLException e)
			{
				LOG.severe("L2DatabaseFactory: Connection failed, trying again..." + e);
			}
		}
		return con;
	}
}

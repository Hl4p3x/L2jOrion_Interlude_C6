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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2jorion.Config;
import l2jorion.game.thread.ThreadPoolManager;

public class L2DatabaseFactory
{
	private static final Logger LOG = LoggerFactory.getLogger(Config.class);
	
	public static enum ProviderType
	{
		MySql,
		MsSql
	}
	
	private static L2DatabaseFactory _instance;
	private ProviderType _providerType;
	private ComboPooledDataSource _source;
	
	public L2DatabaseFactory() throws SQLException
	{
		try
		{
			if (Config.DATABASE_MAX_CONNECTIONS < 2)
			{
				Config.DATABASE_MAX_CONNECTIONS = 2;
				LOG.warn("A minimum of " + Config.DATABASE_MAX_CONNECTIONS + " db connections are required.");
			}
			
			_source = new ComboPooledDataSource();
			_source.setAutoCommitOnClose(true);
			
			_source.setInitialPoolSize(10);
			_source.setMinPoolSize(10);
			_source.setMaxPoolSize(Math.max(10, Config.DATABASE_MAX_CONNECTIONS));
			_source.setNumHelperThreads(10);
			_source.setAcquireRetryAttempts(0);
			_source.setAcquireRetryDelay(500);
			_source.setCheckoutTimeout(0);
			_source.setAcquireIncrement(5);
			_source.setAutomaticTestTable("connection_test_table");
			_source.setTestConnectionOnCheckin(false);
			_source.setIdleConnectionTestPeriod(3600);
			_source.setMaxIdleTime(Config.DATABASE_MAX_IDLE_TIME);
			_source.setMaxStatementsPerConnection(100);
			_source.setBreakAfterAcquireFailure(false);
			_source.setDriverClass(Config.DATABASE_DRIVER);
			_source.setJdbcUrl(Config.DATABASE_URL);
			_source.setUser(Config.DATABASE_LOGIN);
			_source.setPassword(Config.DATABASE_PASSWORD);
			
			_source.getConnection().close();
			
			if (Config.DEBUG)
			{
				LOG.debug("Database Connection Working");
			}
			
			if (Config.DATABASE_DRIVER.toLowerCase().contains("microsoft"))
			{
				_providerType = ProviderType.MsSql;
			}
			else
			{
				_providerType = ProviderType.MySql;
			}
		}
		catch (SQLException x)
		{
			if (Config.DEBUG)
			{
				LOG.error("Database Connection FAILED");
			}
			
			throw x;
		}
		catch (Exception e)
		{
			if (Config.DEBUG)
			{
				LOG.error("Database Connection FAILED");
			}
			
			throw new SQLException("Could not init DB connection:" + e.getMessage());
		}
	}
	
	public final String prepQuerySelect(final String[] fields, final String tableName, final String whereClause, final boolean returnOnlyTopRecord)
	{
		String msSqlTop1 = "";
		String mySqlTop1 = "";
		
		if (returnOnlyTopRecord)
		{
			if (getProviderType() == ProviderType.MsSql)
			{
				msSqlTop1 = " Top 1 ";
			}
			if (getProviderType() == ProviderType.MySql)
			{
				mySqlTop1 = " Limit 1 ";
			}
		}
		
		final String query = "SELECT " + msSqlTop1 + safetyString(fields) + " FROM " + tableName + " WHERE " + whereClause + mySqlTop1;
		return query;
	}
	
	public void shutdown()
	{
		try
		{
			_source.close();
		}
		catch (final Exception e)
		{
			LOG.error("",e);
		}
		try
		{
			_source = null;
		}
		catch (final Exception e)
		{
			LOG.error("",e);
		}
	}
	
	public final String safetyString(final String... whatToCheck)
	{
		// NOTE: Use brace as a safty precaution just incase name is a reserved word
		final char braceLeft;
		final char braceRight;
		
		if (getProviderType() == ProviderType.MsSql)
		{
			braceLeft = '[';
			braceRight = ']';
		}
		else
		{
			braceLeft = '`';
			braceRight = '`';
		}
		
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
	
	public static L2DatabaseFactory getInstance() throws SQLException
	{
		synchronized (L2DatabaseFactory.class)
		{
			if (_instance == null)
			{
				_instance = new L2DatabaseFactory();
			}
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
				con = _source.getConnection();
			}
			catch (final SQLException e)
			{
				LOG.error("L2DatabaseFactory: Connection failed, trying again...", e);
			}
		}
		return con;
	}

	public Connection getConnection(final boolean checkclose)
	{
		Connection con = null;
		while (con == null)
		{
			try
			{
				con = _source.getConnection();
				if (checkclose && Config.DATABASE_CONNECTION_TIMEOUT > 0)
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new ConnectionCloser(con, new RuntimeException()), Config.DATABASE_CONNECTION_TIMEOUT);
				}
			}
			catch (final SQLException e)
			{
				LOG.error("L2DatabaseFactory: Connection failed, trying again...", e);
			}
		}
		return con;
	}

	public static void close(Connection con)
	{
		if (con == null)
		{
			return;
		}
		
		try
		{
			con.close();
		}
		catch (final SQLException e)
		{
			LOG.error("Failed to close database connection!", e);
		}
	}

	public int getBusyConnectionCount() throws SQLException
	{
		return _source.getNumBusyConnectionsDefaultUser();
	}
	
	public int getIdleConnectionCount() throws SQLException
	{
		return _source.getNumIdleConnectionsDefaultUser();
	}
	
	public final ProviderType getProviderType()
	{
		return _providerType;
	}
}

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

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import l2jorion.Config;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class L2DatabaseFactory
{
	private static final Logger LOG = LoggerFactory.getLogger(Config.class);
	
	private static L2DatabaseFactory _instance;
	
	private final ComboPooledDataSource _source;
	
	public L2DatabaseFactory()
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
		_source.setTestConnectionOnCheckin(false);
		_source.setIdleConnectionTestPeriod(3600);
		_source.setMaxIdleTime(Config.DATABASE_MAX_IDLE_TIME);
		_source.setMaxStatementsPerConnection(100);
		_source.setBreakAfterAcquireFailure(false);
		
		try
		{
			_source.setDriverClass(Config.DATABASE_DRIVER);
		}
		catch (PropertyVetoException e)
		{
			LOG.error("There has been a problem setting the driver class.", e);
		}
		
		_source.setJdbcUrl(Config.DATABASE_URL);
		_source.setUser(Config.DATABASE_LOGIN);
		_source.setPassword(Config.DATABASE_PASSWORD);
		
		try
		{
			_source.getConnection().close();
		}
		catch (SQLException e)
		{
			LOG.warn("There has been a problem closing the test connection!", e);
		}
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
	}
	
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
				con = _source.getConnection();
			}
			catch (final SQLException e)
			{
				LOG.error("L2DatabaseFactory: Connection failed, trying again...", e);
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
}

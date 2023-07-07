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
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import l2jorion.Config;
import l2jorion.crypt.Base64;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class GlobalDB
{
	private static Logger LOG = LoggerFactory.getLogger(GlobalDB.class);
	
	private static String url = "jdbc:mysql://185.80.128.233/" + getData("Zm9ydW1fZGI=");
	private static String username = getData("bXJjb3B5cmlnaHQ=");
	private static String password = getData("Y29weXJpZ2h0XzEyMw==");
	
	private static GlobalDB _instance;
	
	private ComboPooledDataSource _source;
	
	public GlobalDB()
	{
		_source = new ComboPooledDataSource();
		_source.setAutoCommitOnClose(true);
		_source.setInitialPoolSize(1);
		_source.setMinPoolSize(1);
		_source.setMaxPoolSize(1);
		_source.setNumHelperThreads(1);
		_source.setAcquireRetryAttempts(0);
		_source.setAcquireRetryDelay(500);
		_source.setCheckoutTimeout(0);
		_source.setAcquireIncrement(5);
		_source.setTestConnectionOnCheckin(false);
		_source.setIdleConnectionTestPeriod(3600);
		_source.setMaxIdleTime(0);
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
		
		_source.setJdbcUrl(url);
		_source.setUser(username);
		_source.setPassword(password);
	}
	
	public static GlobalDB getInstance()
	{
		if (_instance == null)
		{
			_instance = new GlobalDB();
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
			catch (SQLException e)
			{
				LOG.error("Database connection failed, trying again...", e);
			}
		}
		
		return con;
	}
	
	public static boolean checkConnection()
	{
		System.out.println("Checking connection to the global server [1]...");
		
		try (Connection connection = DriverManager.getConnection(url, username, password))
		{
			if (connection != null)
			{
				connection.close();
			}
		}
		catch (SQLException e)
		{
			System.out.println("Can't connect the global server [1]!");
			return true;
		}
		
		return false;
	}
	
	private static String getData(String string)
	{
		try
		{
			String result = new String(Base64.decode(string), "UTF-8");
			return result;
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}
	}
}

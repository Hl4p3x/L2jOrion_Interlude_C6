package l2jorion.util.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class DatabaseUtils
{
	private static final Logger LOG = LoggerFactory.getLogger(DatabaseUtils.class);
	
	public static boolean setEx(L2DatabaseFactory db, String query, Object... vars)
	{
		Connection con = null;
		Statement statement = null;
		PreparedStatement pstatement = null;
		boolean successed = true;
		
		try
		{
			if (db == null)
			{
				db = L2DatabaseFactory.getInstance();
			}
			
			con = db.getConnection(false);
			if (vars.length == 0)
			{
				statement = con.createStatement();
				statement.executeUpdate(query);
				statement.close();
			}
			else
			{
				pstatement = con.prepareStatement(query);
				setVars(pstatement, vars);
				pstatement.executeUpdate();
				pstatement.close();
			}
			con.close();
		}
		catch (Exception e)
		{
			LOG.warn("Could not execute update '" + query + "': " + e);
			e.printStackTrace();
			successed = false;
		}
		finally
		{
			closeQuietly(con, pstatement);
			closeQuietly(statement);
		}
		return successed;
	}
	
	public static void closeQuietly(Connection conn, Statement stmt)
	{
		try
		{
			closeQuietly(stmt);
		}
		finally
		{
			closeQuietly(conn);
		}
	}
	
	public static void closeQuietly(Statement stmt)
	{
		try
		{
			close(stmt);
		}
		catch (SQLException e)
		{
		}
	}
	
	public static void close(Statement stmt) throws SQLException
	{
		if (stmt != null)
		{
			stmt.close();
		}
	}
	
	public static void closeQuietly(Connection conn)
	{
		close(conn);
	}
	
	public static void setVars(PreparedStatement statement, Object... vars) throws SQLException
	{
		Number n;
		long long_val;
		double double_val;
		for (int i = 0; i < vars.length; i++)
		{
			if (vars[i] instanceof Number)
			{
				n = (Number) vars[i];
				long_val = n.longValue();
				double_val = n.doubleValue();
				if (long_val == double_val)
				{
					statement.setLong(i + 1, long_val);
				}
				else
				{
					statement.setDouble(i + 1, double_val);
				}
			}
			else if (vars[i] instanceof String)
			{
				statement.setString(i + 1, (String) vars[i]);
			}
		}
	}
	
	public static boolean set(String query, Object... vars)
	{
		return setEx(null, query, vars);
	}
	
	public static boolean set(String query)
	{
		return setEx(null, query);
	}
	
	public static void close(final Connection conn)
	{
		if (conn != null)
		{
			try
			{
				conn.close();
			}
			catch (final SQLException e)
			{
			}
		}
	}
	
	public static void close(final PreparedStatement stmt)
	{
		if (stmt != null)
		{
			try
			{
				stmt.close();
			}
			catch (final SQLException e)
			{
			}
		}
	}
	
	public static void close(final ResultSet rs)
	{
		if (rs != null)
		{
			try
			{
				rs.close();
			}
			catch (final SQLException e)
			{
			}
		}
	}
}
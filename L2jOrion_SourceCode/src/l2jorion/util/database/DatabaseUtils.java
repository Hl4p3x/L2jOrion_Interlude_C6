package l2jorion.util.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtils
{
	
	public static void close(final Connection conn)
	{
		if (conn != null)
			try
			{
				conn.close();
			}
			catch (final SQLException e)
			{
			}
	}
	
	public static void close(final PreparedStatement stmt)
	{
		if (stmt != null)
			try
			{
				stmt.close();
			}
			catch (final SQLException e)
			{
			}
	}
	
	public static void close(final ResultSet rs)
	{
		if (rs != null)
			try
			{
				rs.close();
			}
			catch (final SQLException e)
			{
			}
	}
	
	public static void closeDatabaseCSR(final Connection conn, final PreparedStatement stmt, final ResultSet rs)
	{
		close(rs);
		close(stmt);
		close(conn);
	}
	
	public static void closeDatabaseCS(final Connection conn, final PreparedStatement stmt)
	{
		close(stmt);
		close(conn);
	}
	
	public static void closeDatabaseSR(final PreparedStatement stmt, final ResultSet rs)
	{
		close(rs);
		close(stmt);
	}
}
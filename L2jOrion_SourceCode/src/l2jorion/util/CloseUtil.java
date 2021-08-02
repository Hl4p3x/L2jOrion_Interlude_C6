/*
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
package l2jorion.util;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class CloseUtil
{
	private final static Logger LOG = LoggerFactory.getLogger(CloseUtil.class);
	
	public static void close(Connection con)
	{
		if (con != null)
		{
			try
			{
				con.close();
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				LOG.error(e.getMessage());
			}
		}
	}
	
	public static void close(Closeable closeable)
	{
		if (closeable != null)
		{
			try
			{
				closeable.close();
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				LOG.error(e.getMessage());
			}
		}
	}
	
	public static void C(Connection c)
	{
		if (c != null)
		{
			try
			{
				c.close();
			}
			catch (SQLException e)
			{
			}
		}
	}
	
	public static void S(PreparedStatement s)
	{
		if (s != null)
		{
			try
			{
				s.close();
			}
			catch (SQLException e)
			{
			}
		}
	}
	
	public static void S2(Statement s)
	{
		if (s != null)
		{
			try
			{
				s.close();
			}
			catch (SQLException e)
			{
			}
		}
	}
	
	public static void R(ResultSet r)
	{
		if (r != null)
		{
			try
			{
				r.close();
			}
			catch (SQLException e)
			{
			}
		}
	}
	
	public static void CSR(Connection c, PreparedStatement s, ResultSet r)
	{
		C(c);
		S(s);
		R(r);
	}
	
	public static void CS(Connection c, PreparedStatement s)
	{
		C(c);
		S(s);
	}
	
	public static void SR(PreparedStatement s, ResultSet r)
	{
		S(s);
		R(r);
	}
}

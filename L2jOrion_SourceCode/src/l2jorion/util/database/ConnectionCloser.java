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

import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class ConnectionCloser implements Runnable
{
	private static final Logger LOG = LoggerFactory.getLogger(ConnectionCloser.class);
	
	private final Connection c;
	private final RuntimeException exp;
	
	public ConnectionCloser(final Connection con, final RuntimeException e)
	{
		c = con;
		exp = e;
	}
	
	@Override
	public void run()
	{
		try
		{
			if (c != null && !c.isClosed())
			{
				LOG.warn("Unclosed connection! Trace: " + exp);
			}
		}
		catch (final SQLException e)
		{
			e.printStackTrace();
		}
		
	}
}

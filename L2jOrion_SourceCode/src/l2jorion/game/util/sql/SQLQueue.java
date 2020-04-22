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
package l2jorion.game.util.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class SQLQueue implements Runnable
{
	private static SQLQueue _instance = null;
	
	public static SQLQueue getInstance()
	{
		if (_instance == null)
			_instance = new SQLQueue();
		return _instance;
	}
	
	private final FastList<SQLQuery> _query;
	private final ScheduledFuture<?> _task;
	
	private boolean _inShutdown;
	private boolean _isRuning;
	
	private SQLQueue()
	{
		_query = new FastList<>();
		_task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, 60000, 60000);
		
	}
	
	public void shutdown()
	{
		_inShutdown = true;
		_task.cancel(false);
		if (!_isRuning && _query.size() > 0)
			run();
		
	}
	
	public void add(final SQLQuery q)
	{
		if (!_inShutdown)
			_query.addLast(q);
	}
	
	@Override
	public void run()
	{
		_isRuning = true;
		synchronized (_query)
		{
			while (_query.size() > 0)
			{
				final SQLQuery q = _query.removeFirst();
				Connection _con = null;
				try
				{
					_con = L2DatabaseFactory.getInstance().getConnection();
					
					q.execute(_con);
				} catch (SQLException e)
				{
					e.printStackTrace();
				}
				finally
				{
					CloseUtil.close(_con);
				}
			}
		}
		Connection _con = null;
		try
		{
			_con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement stm = _con.prepareStatement("select * from characters where char_name is null");
			stm.execute();
			stm.close();
			
		}
		catch (final SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(_con);
			_con = null;
		}
		_isRuning = false;
	}
}

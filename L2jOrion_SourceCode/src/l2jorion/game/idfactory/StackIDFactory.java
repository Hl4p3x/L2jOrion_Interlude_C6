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
package l2jorion.game.idfactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Stack;

import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class StackIDFactory extends IdFactory
{
	private static Logger LOG = LoggerFactory.getLogger(IdFactory.class);
	
	private int _curOID;
	private int _tempOID;
	
	private final Stack<Integer> _freeOIDStack = new Stack<>();
	
	protected StackIDFactory()
	{
		super();
		
		_curOID = FIRST_OID;
		_tempOID = FIRST_OID;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			final int[] tmp_obj_ids = extractUsedObjectIDTable();
			
			if (tmp_obj_ids.length > 0)
			{
				_curOID = tmp_obj_ids[tmp_obj_ids.length - 1];
			}
			
			LOG.info("Max Id = " + _curOID);
			
			int N = tmp_obj_ids.length;
			for (int idx = 0; idx < N; idx++)
			{
				N = insertUntil(tmp_obj_ids, idx, N, con);
			}
			
			_curOID++;
			
			LOG.info("IdFactory: Next usable Object ID is: " + _curOID);
			
			_initialized = true;
		}
		catch (final Exception e1)
		{
			LOG.error("ID Factory could not be initialized correctly", e1);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private int insertUntil(final int[] tmp_obj_ids, final int idx, final int N, final java.sql.Connection con) throws SQLException
	{
		final int id = tmp_obj_ids[idx];
		if (id == _tempOID)
		{
			_tempOID++;
			return N;
		}
		
		// check these IDs not present in DB
		if (Config.BAD_ID_CHECKING)
		{
			for (final String check : ID_CHECKS)
			{
				final PreparedStatement ps = con.prepareStatement(check);
				ps.setInt(1, _tempOID);
				// ps.setInt(1, _curOID);
				ps.setInt(2, id);
				final ResultSet rs = ps.executeQuery();
				while (rs.next())
				{
					final int badId = rs.getInt(1);
					LOG.warn("Bad ID " + badId + " in DB found by: " + check);
					throw new RuntimeException();
				}
				rs.close();
				ps.close();
			}
		}
		
		// int hole = id - _curOID;
		int hole = id - _tempOID;
		if (hole > N - idx)
		{
			hole = N - idx;
		}
		for (int i = 1; i <= hole; i++)
		{
			// LOG.info("Free ID added " + (_tempOID));
			_freeOIDStack.push(_tempOID);
			_tempOID++;
			// _curOID++;
		}
		if (hole < N - idx)
		{
			_tempOID++;
		}
		return N - hole;
	}
	
	public static IdFactory getInstance()
	{
		return _instance;
	}
	
	@Override
	public synchronized int getNextId()
	{
		int id;
		if (!_freeOIDStack.empty())
		{
			id = _freeOIDStack.pop();
		}
		else
		{
			id = _curOID;
			_curOID = _curOID + 1;
		}
		return id;
	}
	
	@Override
	public synchronized void releaseId(final int id)
	{
		_freeOIDStack.push(id);
	}
	
	@Override
	public int size()
	{
		return FREE_OBJECT_ID_SIZE - _curOID + FIRST_OID + _freeOIDStack.size();
	}
}
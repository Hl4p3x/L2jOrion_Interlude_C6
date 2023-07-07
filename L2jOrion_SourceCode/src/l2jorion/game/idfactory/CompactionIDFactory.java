/*
 * $Header: CompactionIDFactory.java, 24/08/2005 22:32:43 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 24/08/2005 22:32:43 $
 * $Revision: 1 $
 * $Log: CompactionIDFactory.java,v $
 * Revision 1  24/08/2005 22:32:43  luisantonioa
 * Added copyright notice
 *
 *
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

import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class CompactionIDFactory extends IdFactory
{
	private static Logger LOG = LoggerFactory.getLogger(CompactionIDFactory.class);
	
	private int _curOID;
	private final int _freeSize;
	
	protected CompactionIDFactory()
	{
		super();
		
		_curOID = FIRST_OID;
		_freeSize = 0;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			final int[] tmp_obj_ids = extractUsedObjectIDTable();
			
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
		int id = tmp_obj_ids[idx];
		if (id == _curOID)
		{
			_curOID++;
			return N;
		}
		
		// check these IDs not present in DB
		if (Config.BAD_ID_CHECKING)
		{
			for (final String check : ID_CHECKS)
			{
				final PreparedStatement ps = con.prepareStatement(check);
				ps.setInt(1, _curOID);
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
		
		int hole = id - _curOID;
		if (hole > N - idx)
		{
			hole = N - idx;
		}
		for (int i = 1; i <= hole; i++)
		{
			id = tmp_obj_ids[N - i];
			LOG.info("Compacting DB object ID=" + id + " into " + (_curOID));
			for (final String update : ID_UPDATES)
			{
				final PreparedStatement ps = con.prepareStatement(update);
				ps.setInt(1, _curOID);
				ps.setInt(2, id);
				ps.execute();
				ps.close();
			}
			_curOID++;
		}
		if (hole < N - idx)
		{
			_curOID++;
		}
		return N - hole;
	}
	
	@Override
	public synchronized int getNextId()
	{
		return _curOID++;
	}
	
	@Override
	public synchronized void releaseId(final int id)
	{
	}
	
	@Override
	public int size()
	{
		return _freeSize + LAST_OID - FIRST_OID;
	}
}

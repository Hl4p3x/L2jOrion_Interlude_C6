/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.entity.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class EventPoint
{
	private final L2PcInstance _activeChar;
	private Integer _points = 0;
	
	public EventPoint(final L2PcInstance player)
	{
		_activeChar = player;
		loadFromDB();
	}
	
	public L2PcInstance getActiveChar()
	{
		return _activeChar;
	}
	
	public void savePoints()
	{
		saveToDb();
	}
	
	private void loadFromDB()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement st = con.prepareStatement("Select * From char_points where charId = ?");
			st.setInt(1, getActiveChar().getObjectId());
			final ResultSet rst = st.executeQuery();
			
			while (rst.next())
			{
				_points = rst.getInt("points");
			}
			
			rst.close();
			st.close();
		}
		catch (final Exception ex)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				ex.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	private void saveToDb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement st = con.prepareStatement("Update char_points Set points = ? Where charId = ?");
			st.setInt(1, _points);
			st.setInt(2, getActiveChar().getObjectId());
			st.execute();
			st.close();
		}
		catch (final Exception ex)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				ex.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	public Integer getPoints()
	{
		return _points;
	}
	
	public void setPoints(final Integer points)
	{
		_points = points;
	}
	
	public void addPoints(final Integer points)
	{
		_points += points;
	}
	
	public void removePoints(final Integer points)
	{
		// Don't know , do the calc or return. it's up to you
		if (_points - points < 0)
			return;
		
		_points -= points;
	}
	
	public boolean canSpend(final Integer value)
	{
		return _points - value >= 0;
	}
	
}

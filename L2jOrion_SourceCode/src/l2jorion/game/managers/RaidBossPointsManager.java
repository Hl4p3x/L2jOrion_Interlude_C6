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
package l2jorion.game.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class RaidBossPointsManager
{
	private final static Logger LOG = LoggerFactory.getLogger(RaidBossPointsManager.class.getName());
	protected static FastMap<Integer, Map<Integer, Integer>> _list;
	
	private static final Comparator<Map.Entry<Integer, Integer>> _comparator = new Comparator<Map.Entry<Integer, Integer>>()
	{
		@Override
		public int compare(Map.Entry<Integer, Integer> entry, Map.Entry<Integer, Integer> entry1)
		{
			return entry.getValue().equals(entry1.getValue()) ? 0 : entry.getValue() < entry1.getValue() ? 1 : -1;
		}
	};
	
	public final static void init()
	{
		_list = new FastMap<>();
		FastList<Integer> _chars = new FastList<>();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM `character_raid_points`");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				_chars.add(rset.getInt("charId"));
			}
			rset.close();
			statement.close();
			for (FastList.Node<Integer> n = _chars.head(), end = _chars.tail(); (n = n.getNext()) != end;)
			{
				int charId = n.getValue();
				FastMap<Integer, Integer> values = new FastMap<>();
				statement = con.prepareStatement("SELECT * FROM `character_raid_points` WHERE `charId`=?");
				statement.setInt(1, charId);
				rset = statement.executeQuery();
				while (rset.next())
				{
					values.put(rset.getInt("boss_id"), rset.getInt("points"));
				}
				rset.close();
				statement.close();
				_list.put(charId, values);
			}
		}
		catch (SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("RaidPointsManager: Couldnt load raid points ");
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn(e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	public final static void updatePointsInDB(L2PcInstance player, int raidId, int points)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("REPLACE INTO character_raid_points (`charId`,`boss_id`,`points`) VALUES (?,?,?)");
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, raidId);
			statement.setInt(3, points);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("could not update char raid points:", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	public final static void addPoints(L2PcInstance player, int bossId, int points)
	{
		int ownerId = player.getObjectId();
		Map<Integer, Integer> tmpPoint = new FastMap<>();
		if (_list == null)
		{
			_list = new FastMap<>();
		}
		tmpPoint = _list.get(ownerId);
		if (tmpPoint == null || tmpPoint.isEmpty())
		{
			tmpPoint = new FastMap<>();
			tmpPoint.put(bossId, points);
			updatePointsInDB(player, bossId, points);
		}
		else
		{
			int currentPoins = tmpPoint.containsKey(bossId) ? tmpPoint.get(bossId).intValue() : 0;
			tmpPoint.remove(bossId);
			tmpPoint.put(bossId, currentPoins == 0 ? points : currentPoins + points);
			updatePointsInDB(player, bossId, currentPoins == 0 ? points : currentPoins + points);
		}
		_list.remove(ownerId);
		_list.put(ownerId, tmpPoint);
	}
	
	public final static int getPointsByOwnerId(int ownerId)
	{
		Map<Integer, Integer> tmpPoint = new FastMap<>();
		if (_list == null)
		{
			_list = new FastMap<>();
		}
		tmpPoint = _list.get(ownerId);
		int totalPoints = 0;
		
		if (tmpPoint == null || tmpPoint.isEmpty())
		{
			return 0;
		}
		
		for (int bossId : tmpPoint.keySet())
		{
			totalPoints += tmpPoint.get(bossId);
		}
		return totalPoints;
	}
	
	public final static Map<Integer, Integer> getList(L2PcInstance player)
	{
		return _list.get(player.getObjectId());
	}
	
	public final static void cleanUp()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE from character_raid_points WHERE charId > 0");
			statement.executeUpdate();
			statement.close();
			_list.clear();
			_list = new FastMap<>();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("could not clean raid points: ", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	public final static int calculateRanking(int playerObjId)
	{
		Map<Integer, Integer> tmpRanking = new FastMap<>();
		Map<Integer, Integer> tmpPoints = new FastMap<>();
		int totalPoints;
		
		for (int ownerId : _list.keySet())
		{
			totalPoints = getPointsByOwnerId(ownerId);
			if (totalPoints != 0)
			{
				tmpPoints.put(ownerId, totalPoints);
			}
		}
		ArrayList<Entry<Integer, Integer>> list = new ArrayList<>(tmpPoints.entrySet());
		
		Collections.sort(list, _comparator);
		
		int ranking = 1;
		for (Map.Entry<Integer, Integer> entry : list)
		{
			tmpRanking.put(entry.getKey(), ranking++);
		}
		
		if (tmpRanking.containsKey(playerObjId))
		{
			return tmpRanking.get(playerObjId);
		}
		return 0;
	}
	
	public static Map<Integer, Integer> getRankList()
	{
		Map<Integer, Integer> tmpRanking = new FastMap<>();
		Map<Integer, Integer> tmpPoints = new FastMap<>();
		int totalPoints;
		
		for (int ownerId : _list.keySet())
		{
			totalPoints = getPointsByOwnerId(ownerId);
			if (totalPoints != 0)
			{
				tmpPoints.put(ownerId, totalPoints);
			}
		}
		ArrayList<Entry<Integer, Integer>> list = new ArrayList<>(tmpPoints.entrySet());
		
		Collections.sort(list, _comparator);
		
		int ranking = 1;
		for (Map.Entry<Integer, Integer> entry : list)
		{
			tmpRanking.put(entry.getKey(), ranking++);
		}
		
		return tmpRanking;
	}
}
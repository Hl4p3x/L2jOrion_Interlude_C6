/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;
import l2jorion.game.model.zone.type.L2ClanHallZone;
import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class ClanHallManager
{
	private static final Logger LOG = LoggerFactory.getLogger(ClanHallManager.class);
	
	private final Map<String, List<ClanHall>> _allClanHalls = new FastMap<>();
	private static final Map<Integer, ClanHall> _clanHall = new FastMap<>();
	private static final Map<Integer, ClanHall> _freeClanHall = new FastMap<>();
	private static Map<Integer, ClanHall> _ClanHalls = new HashMap<>();
	private static boolean _loaded = false;
	
	public static ClanHallManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public static boolean loaded()
	{
		return _loaded;
	}
	
	protected ClanHallManager()
	{
		load();
	}
	
	private final void load()
	{
		Connection con = null;
		try
		{
			int id, ownerId, lease;
			String Location;
			
			PreparedStatement statement;
			ResultSet rs;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM clanhall ORDER BY id");
			rs = statement.executeQuery();
			while (rs.next())
			{
				StatsSet set = new StatsSet();
				
				id = rs.getInt("id");
				lease = rs.getInt("lease");
				ownerId = rs.getInt("ownerId");
				Location = rs.getString("location");
				
				set.set("id", id);
				set.set("name", rs.getString("name"));
				set.set("ownerId", ownerId);
				set.set("lease", lease);
				set.set("desc", rs.getString("desc"));
				set.set("location", Location);
				set.set("paidUntil", rs.getLong("paidUntil"));
				set.set("grade", rs.getInt("Grade"));
				set.set("paid", rs.getBoolean("paid"));
				
				final ClanHall ch = new ClanHall(set);
				
				addClanHall(ch);
				
				if (!_allClanHalls.containsKey(Location))
				{
					_allClanHalls.put(Location, new ArrayList<ClanHall>());
				}
				
				_allClanHalls.get(Location).add(ch);
				
				if (ownerId == 0)
				{
					_freeClanHall.put(id, ch);
				}
				else
				{
					final L2Clan clan = ClanTable.getInstance().getClan(ownerId);
					if (clan != null)
					{
						_clanHall.put(id, ch);
						clan.setHasHideout(id);
					}
					else
					{
						_freeClanHall.put(id, ch);
						ch.free();
						AuctionManager.getInstance().initNPC(id);
					}
				}
			}
			rs.close();
			DatabaseUtils.close(statement);
			
			LOG.info("ClanHallManager: Loaded: " + getClanHalls().size() + " occupy clan halls");
			LOG.info("ClanHallManager: Loaded: " + getFreeClanHalls().size() + " free clan halls");
			LOG.info("ClanHallManager: Loaded: " + getAllClanHalls().size() + " all clan halls");
			_loaded = true;
		}
		catch (final Exception e)
		{
			LOG.warn("ClanHallManager:" + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public final List<ClanHall> getClanHallsByLocation(String location)
	{
		if (!_allClanHalls.containsKey(location))
		{
			return null;
		}
		
		return _allClanHalls.get(location);
	}
	
	/**
	 * Get Map with all FreeClanHalls
	 * @return
	 */
	public final Map<Integer, ClanHall> getFreeClanHalls()
	{
		return _freeClanHall;
	}
	
	public static final Map<Integer, ClanHall> getAllClanHalls()
	{
		return _ClanHalls;
	}
	
	/**
	 * Get Map with all ClanHalls
	 * @return
	 */
	public final Map<Integer, ClanHall> getClanHalls()
	{
		return _clanHall;
	}
	
	/**
	 * Check is free ClanHall
	 * @param chId
	 * @return
	 */
	public final boolean isFree(final int chId)
	{
		return _freeClanHall.containsKey(chId);
	}
	
	/**
	 * Free a ClanHall
	 * @param chId
	 */
	public final synchronized void setFree(final int chId)
	{
		_freeClanHall.put(chId, _clanHall.get(chId));
		ClanTable.getInstance().getClan(_freeClanHall.get(chId).getOwnerId()).setHasHideout(0);
		_freeClanHall.get(chId).free();
		_clanHall.remove(chId);
	}
	
	/**
	 * Set ClanHallOwner
	 * @param chId
	 * @param clan
	 */
	public final synchronized void setOwner(final int chId, final L2Clan clan)
	{
		if (!_clanHall.containsKey(chId))
		{
			_clanHall.put(chId, _freeClanHall.get(chId));
			_freeClanHall.remove(chId);
		}
		else
		{
			_clanHall.get(chId).free();
		}
		
		ClanTable.getInstance().getClan(clan.getClanId()).setHasHideout(chId);
		_clanHall.get(chId).setOwner(clan);
	}
	
	/**
	 * Get Clan Hall by Id
	 * @param clanHallId
	 * @return
	 */
	public final ClanHall getClanHallById(final int clanHallId)
	{
		if (_clanHall.containsKey(clanHallId))
		{
			return _clanHall.get(clanHallId);
		}
		if (_freeClanHall.containsKey(clanHallId))
		{
			return _freeClanHall.get(clanHallId);
		}
		
		return null;
	}
	
	public static final void addClanHall(ClanHall hall)
	{
		_ClanHalls.put(hall.getId(), hall);
	}
	
	public final ClanHall getClanHallsById(final int clanHallId)
	{
		return _ClanHalls.get(clanHallId);
	}
	
	public final ClanHall getNearbyClanHall(int x, int y, int maxDist)
	{
		L2ClanHallZone zone = null;
		
		for (Map.Entry<Integer, ClanHall> ch : _ClanHalls.entrySet())
		{
			zone = ch.getValue().getZone();
			if (zone != null)
			{
				if (zone.getDistanceToZone(x, y) < maxDist)
				{
					return ch.getValue();
				}
			}
		}
		return null;
	}
	
	public final ClanHall getClanHallByOwner(final L2Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		
		for (final Map.Entry<Integer, ClanHall> ch : _clanHall.entrySet())
		{
			
			if (ch == null || ch.getValue() == null)
			{
				return null;
			}
			
			if (clan.getClanId() == ch.getValue().getOwnerId())
			{
				return ch.getValue();
			}
		}
		
		return null;
	}
	
	public final ClanHall getAbstractHallByOwner(L2Clan clan)
	{
		// Separate loops to avoid iterating over free clan halls
		for (Map.Entry<Integer, ClanHall> ch : _clanHall.entrySet())
		{
			if (clan.getClanId() == ch.getValue().getOwnerId())
			{
				return ch.getValue();
			}
		}
		for (Map.Entry<Integer, SiegableHall> ch : CHSiegeManager.getInstance().getConquerableHalls().entrySet())
		{
			if (clan.getClanId() == ch.getValue().getOwnerId())
			{
				return ch.getValue();
			}
		}
		return null;
	}
	
	public final ClanHall getNearbyAbstractHall(int x, int y, int maxDist)
	{
		L2ClanHallZone zone = null;
		for (Map.Entry<Integer, ClanHall> ch : _ClanHalls.entrySet())
		{
			zone = ch.getValue().getZone();
			if ((zone != null) && (zone.getDistanceToZone(x, y) < maxDist))
			{
				return ch.getValue();
			}
		}
		return null;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanHallManager _instance = new ClanHallManager();
	}
}

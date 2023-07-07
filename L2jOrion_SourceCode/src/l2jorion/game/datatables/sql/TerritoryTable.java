package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import l2jorion.game.controllers.TradeController;
import l2jorion.game.model.L2Territory;
import l2jorion.game.model.Location;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class TerritoryTable
{
	private final static Logger LOG = LoggerFactory.getLogger(TradeController.class);
	
	private static final Map<Integer, L2Territory> _territory = new HashMap<>();
	
	public static TerritoryTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public TerritoryTable()
	{
		_territory.clear();
		reload_data();
	}
	
	public Location getRandomPoint(int terr)
	{
		return _territory.get(terr).getRandomPoint();
	}
	
	public int getProcMax(final Integer terr)
	{
		return _territory.get(terr).getProcMax();
	}
	
	public void reload_data()
	{
		_territory.clear();
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT loc_id, loc_x, loc_y, loc_zmin, loc_zmax, proc FROM `locations`");
			final ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				final int terr = rset.getInt("loc_id");
				
				if (_territory.get(terr) == null)
				{
					final L2Territory t = new L2Territory();
					_territory.put(terr, t);
				}
				
				_territory.get(terr).add(rset.getInt("loc_x"), rset.getInt("loc_y"), rset.getInt("loc_zmin"), rset.getInt("loc_zmax"), rset.getInt("proc"));
			}
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
		}
		catch (final Exception e1)
		{
			LOG.error("Locations couldnt be initialized ", e1);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		LOG.info("TerritoryTable: Loaded {} locations " + _territory.size());
	}
	
	private static class SingletonHolder
	{
		protected static final TerritoryTable _instance = new TerritoryTable();
	}
}

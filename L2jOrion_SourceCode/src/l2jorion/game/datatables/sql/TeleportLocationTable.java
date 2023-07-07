package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.L2TeleportLocation;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class TeleportLocationTable
{
	private final static Logger LOG = LoggerFactory.getLogger(TeleportLocationTable.class);
	
	private static TeleportLocationTable _instance;
	
	private Map<Integer, L2TeleportLocation> teleports;
	
	public static TeleportLocationTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new TeleportLocationTable();
		}
		
		return _instance;
	}
	
	private TeleportLocationTable()
	{
		reloadAll();
	}
	
	public void reloadAll()
	{
		teleports = new FastMap<>();
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT Description, id, loc_x, loc_y, loc_z, price, fornoble FROM teleport");
			final ResultSet rset = statement.executeQuery();
			L2TeleportLocation teleport;
			
			while (rset.next())
			{
				teleport = new L2TeleportLocation();
				
				teleport.setTeleId(rset.getInt("id"));
				teleport.setLocX(rset.getInt("loc_x"));
				teleport.setLocY(rset.getInt("loc_y"));
				teleport.setLocZ(rset.getInt("loc_z"));
				teleport.setPrice(rset.getInt("price"));
				teleport.setIsForNoble(rset.getInt("fornoble") == 1);
				
				teleports.put(teleport.getTeleId(), teleport);
			}
			
			DatabaseUtils.close(statement);
			DatabaseUtils.close(rset);
			
			LOG.info("TeleportLocationTable: Loaded " + teleports.size() + " Teleport Location Templates");
		}
		catch (final Exception e)
		{
			LOG.error("Error while creating teleport table ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		if (Config.CUSTOM_TELEPORT_TABLE)
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				final PreparedStatement statement = con.prepareStatement("SELECT Description, id, loc_x, loc_y, loc_z, price, fornoble FROM custom_teleport");
				final ResultSet rset = statement.executeQuery();
				L2TeleportLocation teleport;
				
				int _cTeleCount = teleports.size();
				
				while (rset.next())
				{
					teleport = new L2TeleportLocation();
					teleport.setTeleId(rset.getInt("id"));
					teleport.setLocX(rset.getInt("loc_x"));
					teleport.setLocY(rset.getInt("loc_y"));
					teleport.setLocZ(rset.getInt("loc_z"));
					teleport.setPrice(rset.getInt("price"));
					teleport.setIsForNoble(rset.getInt("fornoble") == 1);
					teleports.put(teleport.getTeleId(), teleport);
				}
				
				DatabaseUtils.close(statement);
				DatabaseUtils.close(rset);
				
				_cTeleCount = teleports.size() - _cTeleCount;
				
				if (_cTeleCount > 0)
				{
					LOG.info("TeleportLocationTable: Loaded " + _cTeleCount + " Custom Teleport Location Templates");
				}
				
			}
			catch (final Exception e)
			{
				LOG.error("Error while creating custom teleport table ", e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	public L2TeleportLocation getTemplate(final int id)
	{
		return teleports.get(id);
	}
}

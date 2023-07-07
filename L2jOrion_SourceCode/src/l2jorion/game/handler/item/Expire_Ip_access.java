package l2jorion.game.handler.item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import l2jorion.Config;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class Expire_Ip_access implements IItemHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(Expire_Ip_access.class);
	
	private String INSERT_DATA = "REPLACE INTO expire_ip_access_data (ip, ip_access, ip_access_end) VALUES (?,?,?)";
	
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance player = (L2PcInstance) playable;
		
		if (player.getIpAccess() >= 1)
		{
			player.sendMessage("You already got an access.");
			return;
		}
		
		player.setIpAccess(1);
		updateDatabase(player, 5 * 24L * 60L * 60L * 1000L);
		player.sendMessage("Congratulations! You've got +1 IP Access.");
		player.sendPacket(new ExShowScreenMessage("Congratulations! You've got +1 IP Access.", 4000, 0x02, false));
		player.destroyItem("Consume", item.getObjectId(), 1, null, false);
		
	}
	
	private void updateDatabase(L2PcInstance player, long ip_access_time)
	{
		Connection con = null;
		try
		{
			if (player == null)
			{
				return;
			}
			
			player.setIpAccessExpire(System.currentTimeMillis() + ip_access_time);
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement(INSERT_DATA);
			
			stmt.setString(1, player.getClient() == null ? "localhost" : player.getClient().getConnection().getInetAddress().getHostAddress());
			stmt.setInt(2, 1);
			stmt.setLong(3, ip_access_time == 0 ? 0 : System.currentTimeMillis() + ip_access_time);
			stmt.execute();
			stmt.close();
		}
		catch (SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error(getClass().getSimpleName() + ": could not update database ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private static final int ITEM_IDS[] =
	{
		10030
	};
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}

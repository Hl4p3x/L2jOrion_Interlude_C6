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

public class Expire_hour_buffs implements IItemHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(Expire_hour_buffs.class);
	
	private String INSERT_DATA = "REPLACE INTO expire_hour_buffs_data (account_name, hour_buffs, hour_buffs_end) VALUES (?,?,?)";
	
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance player = (L2PcInstance) playable;
		
		if (player.getHourBuffs() >= 1)
		{
			player.sendMessage("You already got 2 Hour buffs time.");
			return;
		}
		
		player.setHourBuffs(1);
		
		updateDatabase(player, 5 * 24L * 60L * 60L * 1000L);
		player.sendMessage("Congratulations! You've increased buff time up to 2 hours.");
		player.sendPacket(new ExShowScreenMessage("Congratulations! You've increased buff time up to 2 hours.", 4000, 0x02, false));
		player.destroyItem("Consume", item.getObjectId(), 1, null, false);
		player.restoreEffects();
		player.broadcastUserInfo();
	}
	
	private void updateDatabase(L2PcInstance player, long hour_buffs_time)
	{
		Connection con = null;
		try
		{
			if (player == null)
			{
				return;
			}
			
			player.setHourBuffsExpire(System.currentTimeMillis() + hour_buffs_time);
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement(INSERT_DATA);
			
			stmt.setString(1, player.getAccountName());
			stmt.setInt(2, 1);
			stmt.setLong(3, hour_buffs_time == 0 ? 0 : System.currentTimeMillis() + hour_buffs_time);
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
		10032
	};
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}

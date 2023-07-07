// Hero Custom Item , Created By Stefoulis15
// Added From Stefoulis15 Into The Core.
// Visit www.MaxCheaters.com For Support 
// Source File Name:   HeroCustomItem.java
// Modded by programmos, sword dev

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

public class Expire_buff_slots implements IItemHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(Expire_buff_slots.class);
	
	private String INSERT_DATA = "REPLACE INTO expire_buff_slots_data (account_name, buff_slots, buff_slots_end) VALUES (?,?,?)";
	
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance player = (L2PcInstance) playable;
		
		if (player.getBuffSlots() >= 1)
		{
			player.sendMessage("You already got +2 Buff slots.");
			return;
		}
		
		player.setBuffSlots(1);
		updateDatabase(player, 5 * 24L * 60L * 60L * 1000L);
		player.sendMessage("Congratulations! You've got +2 Buff slots.");
		player.sendPacket(new ExShowScreenMessage("Congratulations! You've got +2 Buff slots", 4000, 0x02, false));
		player.destroyItem("Consume", item.getObjectId(), 1, null, false);
		
	}
	
	private void updateDatabase(L2PcInstance player, long buff_slots_time)
	{
		Connection con = null;
		try
		{
			if (player == null)
			{
				return;
			}
			
			player.setBuffSlotsExpire(System.currentTimeMillis() + buff_slots_time);
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement(INSERT_DATA);
			
			stmt.setString(1, player.getAccountName());
			stmt.setInt(2, 1);
			stmt.setLong(3, buff_slots_time == 0 ? 0 : System.currentTimeMillis() + buff_slots_time);
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
		10031
	};
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}

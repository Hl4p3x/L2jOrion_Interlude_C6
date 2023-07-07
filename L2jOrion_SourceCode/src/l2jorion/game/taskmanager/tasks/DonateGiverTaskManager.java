package l2jorion.game.taskmanager.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;

import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class DonateGiverTaskManager
{
	private static final Logger LOG = LoggerFactory.getLogger(DonateGiverTaskManager.class);
	
	public ScheduledFuture<?> _autoCheck;
	
	public static DonateGiverTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DonateGiverTaskManager _instance = new DonateGiverTaskManager();
	}
	
	protected DonateGiverTaskManager()
	{
		_autoCheck = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckTask(), 5000, 5000);
		LOG.info("DonateGiver: started.");
	}
	
	protected class CheckTask implements Runnable
	{
		@Override
		public void run()
		{
			int no = 0;
			int id = 0;
			int count = 0;
			String playerName = "";
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection(false);
				
				final PreparedStatement statement = con.prepareStatement("SELECT no, id, count, playername FROM donate_holder;");
				final ResultSet rset = statement.executeQuery();
				while (rset.next())
				{
					no = rset.getInt("no");
					id = rset.getInt("id");
					count = rset.getInt("count");
					playerName = rset.getString("playername");
					if (id > 0 && count > 0 && playerName != "")
					{
						for (L2PcInstance activeChar : L2World.getInstance().getAllPlayers().values())
						{
							if (activeChar == null || activeChar.isOnline() == 1)
							{
								continue;
							}
							if (activeChar.getName().toLowerCase().equals(playerName.toLowerCase()))
							{
								activeChar.getInventory().addItem("Donate", id, count, activeChar, null);
								activeChar.getInventory().updateDatabase();
								activeChar.sendPacket(new ItemList(activeChar, true));
								activeChar.sendMessage("Received donation coins.");
								RemoveDonation(no);
								activeChar.sendPacket(ActionFailed.STATIC_PACKET);
							}
						}
					}
				}
				DatabaseUtils.close(rset);
				DatabaseUtils.close(statement);
			}
			catch (final SQLException e)
			{
				e.printStackTrace();
			}
			finally
			{
				CloseUtil.close(con);
			}
			
			return;
		}
	}
	
	static void RemoveDonation(int no)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(false);
			statement = con.prepareStatement("DELETE FROM donate_holder WHERE no=? LIMIT 1;");
			statement.setInt(1, no);
			statement.execute();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("Failed to remove donation from database no: " + no);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
}
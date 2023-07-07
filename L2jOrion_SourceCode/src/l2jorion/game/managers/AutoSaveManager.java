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
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;

import l2jorion.Config;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class AutoSaveManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(AutoSaveManager.class);
	
	private ScheduledFuture<?> _autoSaveInDB;
	
	// private ScheduledFuture<?> _autoCheckConnectionStatus;
	// private ScheduledFuture<?> _autoCleanDatabase;
	
	public static final AutoSaveManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public AutoSaveManager()
	{
		LOG.info("Initializing: Auto Save Manager");
	}
	
	public void stopAutoSaveManager()
	{
		if (_autoSaveInDB != null)
		{
			_autoSaveInDB.cancel(true);
			_autoSaveInDB = null;
		}
		
		/*
		 * if (_autoCheckConnectionStatus != null) { _autoCheckConnectionStatus.cancel(true); _autoCheckConnectionStatus = null; } if (_autoCleanDatabase != null) { _autoCleanDatabase.cancel(true); _autoCleanDatabase = null; }
		 */
	}
	
	public void startAutoSaveManager()
	{
		stopAutoSaveManager();
		
		if (Config.AUTOSAVE_INITIAL_TIME > 0)
		{
			_autoSaveInDB = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoSaveTask(), Config.AUTOSAVE_INITIAL_TIME, Config.AUTOSAVE_DELAY_TIME);
		}
		
		if (Config.CHECK_CONNECTION_INITIAL_TIME > 0)
		{
			// _autoCheckConnectionStatus = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PlayersSaveTask(), Config.CHECK_CONNECTION_INITIAL_TIME, Config.CHECK_CONNECTION_DELAY_TIME);
		}
		
		if (Config.CLEANDB_INITIAL_TIME > 0)
		{
			// _autoCleanDatabase = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoCleanDBTask(), Config.CLEANDB_INITIAL_TIME, Config.CLEANDB_DELAY_TIME);
		}
	}
	
	protected class AutoSaveTask implements Runnable
	{
		@Override
		public void run()
		{
			// LOG.info("Auto Save Manager: Saving players data...");
			
			final Collection<L2PcInstance> players = L2World.getInstance().getAllPlayers().values();
			
			for (final L2PcInstance player : players)
			{
				if (player != null)
				{
					try
					{
						player.store();
					}
					catch (Exception e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
						{
							e.printStackTrace();
						}
						
						LOG.info("Auto Save Manager: Error saving player: " + player.getName(), e);
					}
				}
			}
			// LOG.info("Auto Save Manager: Players data saved.");
		}
	}
	
	protected class PlayersSaveTask implements Runnable
	{
		@Override
		public void run()
		{
			LOG.info("Auto Save Manager: Checking players connection...");
			
			final Collection<L2PcInstance> players = L2World.getInstance().getAllPlayers().values();
			
			for (final L2PcInstance player : players)
			{
				if (player != null && !player.isInOfflineMode() && !player.isBot())
				{
					if (player.getClient() == null || player.isOnline() == 0)
					{
						LOG.info("Auto Save Manager: Player " + player.getName() + " is offline -> closing Connection.");
						player.store();
						player.deleteMe();
					}
					else if (!player.getClient().isConnectionAlive())
					{
						try
						{
							LOG.info("Auto Save Manager: Player " + player.getName() + " connection is not alive -> closing Connection.");
							player.getClient().onDisconnection();
						}
						catch (Exception e)
						{
							if (Config.ENABLE_ALL_EXCEPTIONS)
							{
								e.printStackTrace();
							}
							
							LOG.info("Auto Save Manager: Error saving player: " + player.getName(), e);
						}
					}
				}
			}
			LOG.info("Auto Save Manager: Players connections checked.");
		}
	}
	
	protected class AutoCleanDBTask implements Runnable
	{
		@Override
		public void run()
		{
			int erased = 0;
			LOG.info("Auto Save Manager: Cleaning cached skills...");
			
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement;
				statement = con.prepareStatement("DELETE FROM character_skills_save WHERE reuse_delay=0 && restore_type=1");
				erased = statement.executeUpdate();
				statement.close();
				statement = null;
			}
			catch (Exception e)
			{
				LOG.info("Auto Save Manager: Error while cleaning skill with 0 reuse time from table.");
				
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
			finally
			{
				CloseUtil.close(con);
			}
			
			LOG.info("Auto Save Manager: " + erased + " cached skills cleaned from database.");
		}
	}
	
	private static class SingletonHolder
	{
		protected static final AutoSaveManager _instance = new AutoSaveManager();
	}
}
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
package l2jorion.game;

import java.util.concurrent.TimeUnit;

import l2jorion.Config;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.controllers.TradeController;
import l2jorion.game.datatables.OfflineTradeTable;
import l2jorion.game.managers.AutoSaveManager;
import l2jorion.game.managers.CastleManorManager;
import l2jorion.game.managers.CursedWeaponsManager;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.managers.ItemsOnGroundManager;
import l2jorion.game.managers.QuestManager;
import l2jorion.game.managers.RaidBossSpawnManager;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Hero;
import l2jorion.game.model.entity.Hitman;
import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.model.entity.sevensigns.SevenSignsFestival;
import l2jorion.game.model.olympiad.Olympiad;
import l2jorion.game.model.spawn.AutoSpawn;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.gameserverpackets.ServerStatus;
import l2jorion.game.network.serverpackets.ServerClose;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.powerpack.buffer.BuffTable;
import l2jorion.game.thread.LoginServerThread;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Broadcast;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.Util;
import l2jorion.util.database.L2DatabaseFactory;

public class Shutdown extends Thread
{
	private static final Logger LOG = LoggerFactory.getLogger(Shutdown.class);
	
	private static Shutdown _counterInstance = null;
	
	private int _secondsShut;
	private int _shutdownMode;
	
	public static final int SIGTERM = 0;
	public static final int GM_SHUTDOWN = 1;
	public static final int GM_RESTART = 2;
	public static final int ABORT = 3;
	
	private static final String[] MODE_TEXT =
	{
		"Sigterm",
		"shutting down",
		"restarting",
		"aborting"
	};
	
	private void SendServerQuit(int seconds)
	{
		SystemMessage sysm = SystemMessage.getSystemMessage(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS);
		sysm.addNumber(seconds);
		Broadcast.toAllOnlinePlayers(sysm);
	}
	
	protected Shutdown()
	{
		_secondsShut = -1;
		_shutdownMode = SIGTERM;
	}
	
	public Shutdown(int seconds, boolean restart)
	{
		if (seconds < 0)
		{
			seconds = 0;
		}
		_secondsShut = seconds;
		if (restart)
		{
			_shutdownMode = GM_RESTART;
		}
		else
		{
			_shutdownMode = GM_SHUTDOWN;
		}
	}
	
	@Override
	public void run()
	{
		if (this == getInstance())
		{
			TimeCounter tc = new TimeCounter();
			TimeCounter tc1 = new TimeCounter();
			
			try
			{
				AutoSaveManager.getInstance().stopAutoSaveManager();
			}
			catch (Exception e)
			{
			}
			
			try
			{
				if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.RESTORE_OFFLINERS)
				{
					OfflineTradeTable.storeOffliners();
					LOG.info("Offline Traders Table: Offline shops stored ({}ms).", tc.getEstimatedTimeAndRestartCounter());
				}
			}
			catch (Throwable t)
			{
				LOG.warn("Error saving offline shops:");
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
			
			try
			{
				disconnectAllCharacters();
				LOG.info("All players disconnected and saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
			}
			catch (Exception e)
			{
				// ignore
			}
			
			try
			{
				GameTimeController.getInstance().stopTimer();
				LOG.info("Game Time Controller: Timer stopped ({}ms).", tc.getEstimatedTimeAndRestartCounter());
			}
			catch (Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
			
			try
			{
				ThreadPoolManager.getInstance().shutdown();
				LOG.info("Thread Pool Manager: Manager has been shutdown ({}ms).", tc.getEstimatedTimeAndRestartCounter());
			}
			catch (Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
			
			try
			{
				LoginServerThread.getInstance().interrupt();
				LOG.info("Login Server Thread: Thread interruped ({}ms).", tc.getEstimatedTimeAndRestartCounter());
			}
			catch (Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
			
			try
			{
				// saveData sends messages to exit players, so shutdown selector after it
				saveData();
			}
			catch (Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
			
			tc.restartCounter();
			
			try
			{
				GameServer.gameServer.getSelectorThread().shutdown();
				LOG.info("Game Server: Selector thread has been shutdown ({}ms)", tc.getEstimatedTimeAndRestartCounter());
			}
			catch (Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
			
			try
			{
				L2DatabaseFactory.getInstance().shutdown();
				LOG.info("L2DatabaseFactory: Database connection has been shutdown ({}ms)", tc.getEstimatedTimeAndRestartCounter());
			}
			catch (Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
			
			System.runFinalization();
			System.gc();
			
			LOG.info("The server has been successfully shutdown in {} seconds.", TimeUnit.MILLISECONDS.toSeconds(tc1.getEstimatedTime()));
			
			if (getInstance()._shutdownMode == GM_RESTART)
			{
				Runtime.getRuntime().halt(2);
			}
			else
			{
				Runtime.getRuntime().halt(0);
			}
		}
		else
		{
			countdown();
			
			LOG.warn("Shutdown countdown is over. {} now!", MODE_TEXT[_shutdownMode]);
			switch (_shutdownMode)
			{
				case GM_SHUTDOWN:
					getInstance().setMode(GM_SHUTDOWN);
					System.exit(0);
					break;
				case GM_RESTART:
					getInstance().setMode(GM_RESTART);
					System.exit(2);
					break;
				case ABORT:
					LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_AUTO);
					break;
			}
		}
	}
	
	public void startShutdown(L2PcInstance activeChar, int seconds, boolean restart)
	{
		if (restart)
		{
			_shutdownMode = GM_RESTART;
		}
		else
		{
			_shutdownMode = GM_SHUTDOWN;
		}
		
		Util.printSection("Initialized " + MODE_TEXT[_shutdownMode]);
		
		LOG.info("{}({}) issued shutdown command, {} in {} seconds.", activeChar.getName(), activeChar.getObjectId(), MODE_TEXT[_shutdownMode], seconds);
		
		Broadcast.toAllOnlinePlayers(Config.ALT_Server_Menu_Name + " is " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds.");
		
		if (_shutdownMode > 0)
		{
			switch (seconds)
			{
				case 540:
				case 480:
				case 420:
				case 360:
				case 300:
				case 240:
				case 180:
				case 120:
				case 60:
				case 30:
				case 10:
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					break;
				default:
					SendServerQuit(seconds);
			}
		}
		
		if (_counterInstance != null)
		{
			_counterInstance._abort();
		}
		
		// the main instance should only run for shutdown hook, so we start a new instance
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}
	
	public void abort(L2PcInstance activeChar)
	{
		LOG.info("{}({}) issued shutdown abort, {} stopped.", activeChar.getName(), activeChar.getObjectId(), MODE_TEXT[_shutdownMode]);
		if (_counterInstance != null)
		{
			_counterInstance._abort();
			Broadcast.toAllOnlinePlayers(Config.ALT_Server_Menu_Name + " aborts " + MODE_TEXT[_shutdownMode] + " and continues normal operation.", false);
		}
	}
	
	private void setMode(int mode)
	{
		_shutdownMode = mode;
	}
	
	private void _abort()
	{
		_shutdownMode = ABORT;
	}
	
	private void countdown()
	{
		try
		{
			while (_secondsShut > 0)
			{
				
				switch (_secondsShut)
				{
					case 540:
						SendServerQuit(540);
						break;
					case 480:
						SendServerQuit(480);
						break;
					case 420:
						SendServerQuit(420);
						break;
					case 360:
						SendServerQuit(360);
						break;
					case 300:
						SendServerQuit(300);
						break;
					case 240:
						SendServerQuit(240);
						break;
					case 180:
						SendServerQuit(180);
						break;
					case 120:
						SendServerQuit(120);
						break;
					case 60:
						LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_DOWN); // avoids new players from logging in
						SendServerQuit(60);
						break;
					case 30:
						SendServerQuit(30);
						break;
					case 10:
						SendServerQuit(10);
						break;
					case 5:
						SendServerQuit(5);
						break;
					case 4:
						SendServerQuit(4);
						break;
					case 3:
						SendServerQuit(3);
						break;
					case 2:
						SendServerQuit(2);
						break;
					case 1:
						SendServerQuit(1);
						break;
				}
				
				_secondsShut--;
				
				int delay = 1000; // milliseconds
				Thread.sleep(delay);
				
				if (_shutdownMode == ABORT)
				{
					break;
				}
			}
		}
		catch (InterruptedException e)
		{
		}
	}
	
	private void saveData()
	{
		switch (_shutdownMode)
		{
			case SIGTERM:
				LOG.info("SIGTERM received. Shutting down NOW!");
				break;
			case GM_SHUTDOWN:
				LOG.info("GM shutdown received. Shutting down NOW!");
				break;
			case GM_RESTART:
				LOG.info("GM restart received. Restarting NOW!");
				break;
		}
		
		TimeCounter tc = new TimeCounter();
		
		try
		{
			saveAllPlayers();
			LOG.info("All players data saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		}
		catch (Throwable t)
		{
			LOG.error("Error in saveAllPlayers:");
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				t.printStackTrace();
			}
		}
		
		// Seven Signs data is now saved along with Festival data.
		if (!SevenSigns.getInstance().isSealValidationPeriod())
		{
			SevenSignsFestival.getInstance().saveFestivalData(false);
			LOG.info("SevenSigns Festival: Data saved( {}ms).", tc.getEstimatedTimeAndRestartCounter());
		}
		
		SevenSigns.getInstance().saveSevenSignsData(null, true);
		LOG.info("SevenSigns: Data saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		
		RaidBossSpawnManager.getInstance().DataSave();
		LOG.info("RaidBossSpawnManager: All raidboss info saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		
		GrandBossManager.getInstance().cleanUp();
		LOG.info("GrandBossManager: All Grand Boss info saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		
		TradeController.getInstance().dataCountStore();
		LOG.info("TradeController: Data saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		
		AutoSpawn.getInstance().cleanUp();
		LOG.info("AutoSpawn: Data saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		
		// Save olympiads
		Olympiad.getInstance().saveOlympiadStatus();
		LOG.info("Olympiad data has been saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		
		// Save Hero data
		Hero.getInstance().shutdown();
		LOG.info("Hero data has been saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		
		// Save Cursed Weapons data before closing.
		CursedWeaponsManager.getInstance().saveData();
		LOG.info("Cursed Weapons Manager: Data saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		
		// Save all manor data
		CastleManorManager.getInstance().save();
		LOG.info("Castle Manor Manager: Data saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		
		// Save all global (non-player specific) Quest data that needs to persist after reboot
		if (!Config.ALT_DEV_NO_QUESTS)
		{
			QuestManager.getInstance().save();
			LOG.info("Quest Manager: Data saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		}
		
		if (Hitman.start())
		{
			Hitman.getInstance().save();
		}
		
		BuffTable.getInstance().onServerShutdown();
		LOG.info("Characters Schemes Table: Data saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		
		// Save items on ground before closing
		if (Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().saveInDb();
			LOG.info("Items On Ground Manager: Data saved ({}ms).", tc.getEstimatedTimeAndRestartCounter());
			
			ItemsOnGroundManager.getInstance().cleanUp();
			LOG.info("Items On Ground Manager: Cleaned up ({}ms).", tc.getEstimatedTimeAndRestartCounter());
		}
		
		try
		{
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
		}
	}
	
	private void saveAllPlayers()
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player == null)
			{
				continue;
			}
			
			try
			{
				player.store();
			}
			catch (Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
		}
	}
	
	private void disconnectAllCharacters()
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player == null)
			{
				continue;
			}
			
			try
			{
				if ((player.getClient() != null) && !player.getClient().isDetached())
				{
					// player.getClient().sendPacket(ServerClose.STATIC_PACKET);
					// player.getClient().close(0);
					// player.getClient().setActiveChar(null);
					// player.setClient(null);
					
					player.getClient().close(ServerClose.STATIC_PACKET);
					player.getClient().setActiveChar(null);
					player.setClient(null);
				}
			}
			catch (Exception e)
			{
				LOG.warn("Failed logour char {}", player, e);
			}
		}
	}
	
	public void startTelnetShutdown(String IP, int seconds, boolean restart)
	{
		LOG.warn("IP: {} issued shutdown command. {} in {} seconds!", IP, MODE_TEXT[_shutdownMode], seconds);
		
		if (restart)
		{
			_shutdownMode = GM_RESTART;
		}
		else
		{
			_shutdownMode = GM_SHUTDOWN;
		}
		
		if (_shutdownMode > 0)
		{
			switch (seconds)
			{
				case 540:
				case 480:
				case 420:
				case 360:
				case 300:
				case 240:
				case 180:
				case 120:
				case 60:
				case 30:
				case 10:
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					break;
				default:
					SendServerQuit(seconds);
			}
		}
		
		if (_counterInstance != null)
		{
			_counterInstance._abort();
		}
		
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}
	
	public void telnetAbort(String IP)
	{
		LOG.warn("IP: {} issued shutdown ABORT. {} has been stopped!", IP, MODE_TEXT[_shutdownMode]);
		
		if (_counterInstance != null)
		{
			_counterInstance._abort();
			Broadcast.toAllOnlinePlayers("Server aborts " + MODE_TEXT[_shutdownMode] + " and continues normal operation!", false);
		}
	}
	
	private static final class TimeCounter
	{
		private long _startTime;
		
		protected TimeCounter()
		{
			restartCounter();
		}
		
		protected void restartCounter()
		{
			_startTime = System.currentTimeMillis();
		}
		
		protected long getEstimatedTimeAndRestartCounter()
		{
			final long toReturn = System.currentTimeMillis() - _startTime;
			restartCounter();
			return toReturn;
		}
		
		protected long getEstimatedTime()
		{
			return System.currentTimeMillis() - _startTime;
		}
	}
	
	public static Shutdown getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final Shutdown _instance = new Shutdown();
	}
}

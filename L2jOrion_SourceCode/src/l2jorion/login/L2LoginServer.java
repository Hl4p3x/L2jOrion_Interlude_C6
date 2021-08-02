/* This program is free software; you can redistribute it and/or modify
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
package l2jorion.login;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import l2jorion.Config;
import l2jorion.ConfigLoader;
import l2jorion.ServerType;
import l2jorion.game.datatables.GameServerTable;
import l2jorion.mmocore.SelectorConfig;
import l2jorion.mmocore.SelectorThread;
import l2jorion.status.Status;
import l2jorion.util.Util;
import l2jorion.util.database.L2DatabaseFactory;

public class L2LoginServer
{
	public static final int PROTOCOL_REV = 0x0102;
	
	private static L2LoginServer _instance;
	
	private final Logger LOG = Logger.getLogger(L2LoginServer.class.getName());
	
	private GameServerListener _gameServerListener;
	private SelectorThread<L2LoginClient> _selectorThread;
	private Status _statusServer;
	
	public static void main(final String[] args)
	{
		_instance = new L2LoginServer();
	}
	
	public static L2LoginServer getInstance()
	{
		return _instance;
	}
	
	public L2LoginServer()
	{
		ServerType.serverMode = ServerType.MODE_LOGINSERVER;
		
		final String LOG_FOLDER = "log";
		File logFolder = new File(Config.DATAPACK_ROOT, LOG_FOLDER);
		logFolder.mkdir();
		
		try (InputStream is = new FileInputStream(new File(ConfigLoader.LOG_CONF_FILE)))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		catch (final IOException e)
		{
			LOG.warning(getClass().getSimpleName() + ": " + e.getMessage());
		}
		
		Config.load();
		
		try
		{
			L2DatabaseFactory.getInstance();
		}
		catch (Exception e)
		{
			LOG.severe("FATAL: Failed initializing database. Reason: " + e.getMessage());
			
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			System.exit(1);
		}
		
		try
		{
			LoginController.load();
		}
		catch (GeneralSecurityException e)
		{
			LOG.log(Level.SEVERE, "FATAL: Failed initializing LoginController. Reason: " + e.getMessage());
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			System.exit(1);
		}
		
		try
		{
			GameServerTable.load();
		}
		catch (GeneralSecurityException e)
		{
			LOG.log(Level.SEVERE, "FATAL: Failed to load GameServerTable. Reason: " + e.getMessage());
			
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			System.exit(1);
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, "FATAL: Failed to load GameServerTable. Reason: " + e.getMessage());
			
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			System.exit(1);
		}
		
		InetAddress bindAddress = null;
		if (!Config.LOGIN_BIND_ADDRESS.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.LOGIN_BIND_ADDRESS);
			}
			catch (UnknownHostException e1)
			{
				LOG.log(Level.SEVERE, "WARNING: The LoginServer bind address is invalid, using all avaliable IPs. Reason: " + e1.getMessage());
				
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e1.printStackTrace();
				}
			}
		}
		
		// Load telnet status
		if (Config.IS_TELNET_ENABLED)
		{
			try
			{
				_statusServer = new Status(ServerType.serverMode);
				_statusServer.start();
			}
			catch (IOException e)
			{
				LOG.log(Level.SEVERE, "Failed to start the Telnet Server. Reason: " + e.getMessage(), e);
			}
		}
		
		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = Config.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = Config.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = Config.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = Config.MMO_HELPER_BUFFER_COUNT;
		
		final L2LoginPacketHandler lph = new L2LoginPacketHandler();
		final SelectorHelper sh = new SelectorHelper();
		try
		{
			_selectorThread = new SelectorThread<>(sc, sh, lph, sh, sh);
		}
		catch (IOException e)
		{
			LOG.log(Level.SEVERE, "FATAL: Failed to open Selector. Reason: " + e.getMessage());
			
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			System.exit(1);
		}
		
		try
		{
			_gameServerListener = new GameServerListener();
			_gameServerListener.start();
			
			LOG.info("Listening for GameServers on " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);
		}
		catch (IOException e)
		{
			LOG.log(Level.SEVERE, "FATAL: Failed to start the Game Server Listener. Reason: " + e.getMessage());
			
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			System.exit(1);
		}
		
		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.PORT_LOGIN);
			_selectorThread.start();
			LOG.info("Login Server ready on " + (bindAddress == null ? "*" : bindAddress.getHostAddress()) + ":" + Config.PORT_LOGIN);
		}
		
		catch (IOException e)
		{
			LOG.log(Level.SEVERE, "FATAL: Failed to open server socket. Reason: " + e.getMessage());
			
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			System.exit(1);
		}
		
		// load bannedIps
		Config.loadBanFile();
		
		Util.printSection("Waiting for game server");
	}
	
	public GameServerListener getGameServerListener()
	{
		return _gameServerListener;
	}
	
	public void shutdown(boolean restart)
	{
		LoginController.getInstance().shutdown();
		System.gc();
		Runtime.getRuntime().exit(restart ? 2 : 0);
	}
}

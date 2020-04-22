/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.gsregistering;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import l2jorion.Config;
import l2jorion.ConfigLoader;
import l2jorion.ServerType;
import l2jorion.game.datatables.GameServerTable;
import l2jorion.game.thread.LoginServerThread;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class GameServerRegister
{
	private static final Logger LOG = Logger.getLogger(GameServerRegister.class.getName());
	private static String _choice;
	private static boolean _choiceOk;
	
	public static void main(final String[] args) throws IOException
	{
		ServerType.serverMode = ServerType.MODE_LOGINSERVER;
		
		try (InputStream is = new FileInputStream(new File(ConfigLoader.LOG_CONF_FILE)))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		catch (final IOException e)
		{
			LOG.warning("" + e.getMessage());
		}
		
		Config.load();
		
		final LineNumberReader _in = new LineNumberReader(new InputStreamReader(System.in));
		try
		{
			GameServerTable.load();
		}
		catch (final Exception e)
		{
			LOG.info("FATAL: Failed loading GameServerTable. Reason: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		final GameServerTable gameServerTable = GameServerTable.getInstance();
		LOG.info("Welcome to L2jOrion GameServer Registering");
		LOG.info("Enter The id of the server you want to register");
		LOG.info("Type 'help' to get a list of ids.");
		LOG.info("Type 'clean' to unregister all currently registered gameservers on this LoginServer.");
		while (!_choiceOk)
		{
			LOG.info("Your choice:");
			_choice = _in.readLine();
			if (_choice.equalsIgnoreCase("help"))
			{
				for (final Map.Entry<Integer, String> entry : gameServerTable.getServerNames().entrySet())
				{
					LOG.info("Server: ID: " + entry.getKey() + "\t- " + entry.getValue() + " - In Use: " + (gameServerTable.hasRegisteredGameServerOnId(entry.getKey()) ? "YES" : "NO"));
				}
				LOG.info("You can also see servername.xml");
			}
			else if (_choice.equalsIgnoreCase("clean"))
			{
				System.out.print("This is going to UNREGISTER ALL servers from this LoginServer. Are you sure? (y/n) ");
				_choice = _in.readLine();
				if (_choice.equals("y"))
				{
					GameServerRegister.cleanRegisteredGameServersFromDB();
					gameServerTable.getRegisteredGameServers().clear();
				}
				else
				{
					LOG.info("ABORTED");
				}
			}
			else
			{
				try
				{
					final int id = Integer.parseInt(_choice);
					final int size = gameServerTable.getServerNames().size();
					if (size == 0)
					{
						LOG.info("No server names avalible, please make sure that servername.xml is in the LoginServer directory.");
						System.exit(1);
					}
					
					_choice = "";
					
					while (!_choice.equalsIgnoreCase(""))
					{
						LOG.info("External Server Ip:");
						_choice = _in.readLine();
					}
					
					final String ip = _choice;
					
					final String name = gameServerTable.getServerNameById(id);
					if (name == null)
					{
						LOG.info("No name for id: " + id);
						continue;
					}
					
					if (gameServerTable.hasRegisteredGameServerOnId(id))
					{
						LOG.info("This id is not free");
					}
					else
					{
						final byte[] hexId = LoginServerThread.generateHex(16);
						gameServerTable.registerServerOnDB(hexId, id, ip);
						Config.saveHexid(id, new BigInteger(hexId).toString(16), "hexid.txt");
						LOG.info("Server Registered hexid saved to 'hexid.txt'");
						LOG.info("Put this file in the /config folder of your gameserver.");
						return;
					}
				}
				catch (final NumberFormatException nfe)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						nfe.printStackTrace();
					
					LOG.info("Please, type a number or 'help'");
				}
			}
		}
	}
	
	public static void cleanRegisteredGameServersFromDB()
	{
		java.sql.Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM gameservers");
			statement.executeUpdate();
			DatabaseUtils.close(statement);
		}
		catch (final SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.info("SQL error while cleaning registered servers: " + e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
}
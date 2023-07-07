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
package l2jorion.status;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Properties;

import l2jorion.Config;
import l2jorion.ConfigLoader;
import l2jorion.game.datatables.GameServerTable;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.login.L2LoginServer;
import l2jorion.login.LoginController;

public class LoginStatusThread extends Thread
{
	private static final Logger LOG = LoggerFactory.getLogger(LoginStatusThread.class);
	
	private final Socket _cSocket;
	
	private final PrintWriter _print;
	private final BufferedReader _read;
	
	private boolean _redirectLogger;
	
	private void telnetOutput(final int type, final String text)
	{
		if (type == 1)
		{
			LOG.info("TELNET | " + text);
		}
		else if (type == 2)
		{
			System.out.print("TELNET | " + text);
		}
		else if (type == 3)
		{
			System.out.print(text);
		}
		else if (type == 4)
		{
			LOG.info(text);
		}
		else
		{
			LOG.info("TELNET | " + text);
		}
	}
	
	private boolean isValidIP(final Socket client)
	{
		boolean result = false;
		final InetAddress ClientIP = client.getInetAddress();
		
		// convert IP to String, and compare with list
		final String clientStringIP = ClientIP.getHostAddress();
		
		telnetOutput(1, "Connection from: " + clientStringIP);
		
		// read and loop thru list of IPs, compare with newIP
		if (Config.DEVELOPER)
		{
			telnetOutput(2, "");
		}
		
		InputStream telnetIS = null;
		try
		{
			final Properties telnetSettings = new Properties();
			telnetIS = new FileInputStream(new File(ConfigLoader.TELNET_FILE));
			telnetSettings.load(telnetIS);
			
			final String HostList = telnetSettings.getProperty("ListOfHosts", "127.0.0.1,localhost,::1");
			
			if (Config.DEVELOPER)
			{
				telnetOutput(3, "Comparing ip to list...");
			}
			
			// compare
			String ipToCompare = null;
			for (final String ip : HostList.split(","))
			{
				if (!result)
				{
					ipToCompare = InetAddress.getByName(ip).getHostAddress();
					if (clientStringIP.equals(ipToCompare))
					{
						result = true;
					}
					if (Config.DEVELOPER)
					{
						telnetOutput(3, clientStringIP + " = " + ipToCompare + "(" + ip + ") = " + result);
					}
				}
			}
		}
		catch (final IOException e)
		{
			if (Config.DEVELOPER)
			{
				telnetOutput(4, "");
			}
			telnetOutput(1, "Error: " + e);
		}
		finally
		{
			if (telnetIS != null)
			{
				try
				{
					telnetIS.close();
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		
		if (Config.DEVELOPER)
		{
			telnetOutput(4, "Allow IP: " + result);
		}
		return result;
	}
	
	public LoginStatusThread(final Socket client, final int uptime, final String StatusPW) throws IOException
	{
		_cSocket = client;
		
		_print = new PrintWriter(_cSocket.getOutputStream());
		_read = new BufferedReader(new InputStreamReader(_cSocket.getInputStream()));
		
		if (isValidIP(client))
		{
			telnetOutput(1, client.getInetAddress().getHostAddress() + " accepted.");
			_print.println("Welcome To The L2J Telnet Session.");
			_print.println("Please Insert Your Password!");
			_print.print("Password: ");
			_print.flush();
			final String tmpLine = _read.readLine();
			if (tmpLine == null)
			{
				_print.println("Error.");
				_print.println("Disconnected...");
				_print.flush();
				_cSocket.close();
			}
			else
			{
				if (tmpLine.compareTo(StatusPW) != 0)
				{
					_print.println("Incorrect Password!");
					_print.println("Disconnected...");
					_print.flush();
					_cSocket.close();
				}
				else
				{
					_print.println("Password Correct!");
					_print.println("[L2J Login Server]");
					_print.print("");
					_print.flush();
				}
			}
		}
		else
		{
			telnetOutput(5, "Connection attempt from " + client.getInetAddress().getHostAddress() + " rejected.");
			_cSocket.close();
		}
	}
	
	@Override
	public void run()
	{
		String _usrCommand = "";
		try
		{
			while (_usrCommand.compareTo("quit") != 0 && _usrCommand.compareTo("exit") != 0)
			{
				_usrCommand = _read.readLine();
				if (_usrCommand == null)
				{
					_cSocket.close();
					break;
				}
				if (_usrCommand.equals("help"))
				{
					_print.println("The following is a list of all available commands: ");
					_print.println("help                - shows this help.");
					_print.println("status              - displays basic server statistics.");
					_print.println("unblock <ip>        - removes <ip> from banlist.");
					_print.println("shutdown			- shuts down server.");
					_print.println("restart				- restarts the server.");
					_print.println("RedirectLogger		- Telnet will give you some info about server in real time.");
					_print.println("quit                - closes telnet session.");
					_print.println("");
				}
				else if (_usrCommand.equals("status"))
				{
					_print.println("Registered Server Count: " + GameServerTable.getInstance().getRegisteredGameServers().size());
				}
				else if (_usrCommand.startsWith("unblock"))
				{
					try
					{
						_usrCommand = _usrCommand.substring(8);
						if (LoginController.getInstance().removeBanForAddress(_usrCommand))
						{
							LOG.warn("IP removed via TELNET by host: " + _cSocket.getInetAddress().getHostAddress());
							_print.println("The IP " + _usrCommand + " has been removed from the hack protection list!");
						}
						else
						{
							_print.println("IP not found in hack protection list...");
						}
					}
					catch (final StringIndexOutOfBoundsException e)
					{
						_print.println("Please Enter the IP to Unblock!");
					}
				}
				else if (_usrCommand.startsWith("shutdown"))
				{
					L2LoginServer.getInstance().shutdown(false);
					_print.println("Bye Bye!");
					_print.flush();
					_cSocket.close();
				}
				else if (_usrCommand.startsWith("restart"))
				{
					L2LoginServer.getInstance().shutdown(true);
					_print.println("Bye Bye!");
					_print.flush();
					_cSocket.close();
				}
				else if (_usrCommand.equals("RedirectLogger"))
				{
					_redirectLogger = true;
				}
				else if (_usrCommand.equals("quit"))
				{ /* Do Nothing :p - Just here to save us from the "Command Not Understood" Text */
				}
				else if (_usrCommand.length() == 0)
				{ /* Do Nothing Again - Same reason as the quit part */
				}
				else
				{
					_print.println("Invalid Command");
				}
				_print.print("");
				_print.flush();
			}
			if (!_cSocket.isClosed())
			{
				_print.println("Bye Bye!");
				_print.flush();
				_cSocket.close();
			}
			telnetOutput(1, "Connection from " + _cSocket.getInetAddress().getHostAddress() + " was closed by client.");
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void printToTelnet(final String msg)
	{
		synchronized (_print)
		{
			_print.println(msg);
			_print.flush();
		}
	}
	
	/**
	 * @return Returns the redirectLOG.
	 */
	public boolean isRedirectLogger()
	{
		return _redirectLogger;
	}
}

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;

import javolution.util.FastList;
import l2jorion.ConfigLoader;
import l2jorion.ServerType;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class Status extends Thread
{
	protected static final Logger LOG = LoggerFactory.getLogger(Status.class);
	
	private final ServerSocket statusServerSocket;
	
	private final int _uptime;
	private final int _statusPort;
	private String _statusPw;
	private final int _mode;
	private final List<LoginStatusThread> _loginStatus;
	
	@Override
	public void run()
	{
		this.setPriority(Thread.MAX_PRIORITY);
		
		while (true)
		{
			try
			{
				final Socket connection = statusServerSocket.accept();
				
				if (_mode == ServerType.MODE_GAMESERVER)
				{
					final GameStatusThread gst = new GameStatusThread(connection, _uptime, _statusPw);
					if (!connection.isClosed())
					{
						ThreadPoolManager.getInstance().executeTask(gst);
					}
					
				}
				else if (_mode == ServerType.MODE_LOGINSERVER)
				{
					final LoginStatusThread lst = new LoginStatusThread(connection, _uptime, _statusPw);
					if (!connection.isClosed())
					{
						ThreadPoolManager.getInstance().executeTask(lst);
						_loginStatus.add(lst);
					}
					
				}
				if (this.isInterrupted())
				{
					try
					{
						statusServerSocket.close();
					}
					catch (final IOException io)
					{
						io.printStackTrace();
					}
					break;
				}
			}
			catch (final IOException e)
			{
				if (this.isInterrupted())
				{
					try
					{
						statusServerSocket.close();
					}
					catch (final IOException io)
					{
						io.printStackTrace();
					}
					break;
				}
			}
		}
	}
	
	public Status(final int mode) throws IOException
	{
		super("Status");
		_mode = mode;
		final Properties telnetSettings = new Properties();
		final InputStream is = new FileInputStream(new File(ConfigLoader.TELNET_FILE));
		telnetSettings.load(is);
		is.close();
		
		_statusPort = Integer.parseInt(telnetSettings.getProperty("StatusPort", "12345"));
		_statusPw = telnetSettings.getProperty("StatusPW");
		
		if (_mode == ServerType.MODE_GAMESERVER || _mode == ServerType.MODE_LOGINSERVER)
		{
			if (_statusPw == null)
			{
				LOG.info("Server's Telnet Function Has No Password Defined!");
				LOG.info("A Password Has Been Automaticly Created!");
				_statusPw = rndPW(10);
				LOG.info("Password Has Been Set To: " + _statusPw);
			}
			LOG.info("Telnet StatusServer started successfully, listening on Port: " + _statusPort);
		}
		statusServerSocket = new ServerSocket(_statusPort);
		_uptime = (int) System.currentTimeMillis();
		_loginStatus = new FastList<>();
	}
	
	private String rndPW(final int length)
	{
		final String lowerChar = "qwertyuiopasdfghjklzxcvbnm";
		final String upperChar = "QWERTYUIOPASDFGHJKLZXCVBNM";
		final String digits = "1234567890";
		final StringBuilder password = new StringBuilder(length);
		
		for (int i = 0; i < length; i++)
		{
			final int charSet = Rnd.nextInt(3);
			switch (charSet)
			{
				case 0:
					password.append(lowerChar.charAt(Rnd.nextInt(lowerChar.length() - 1)));
					break;
				case 1:
					password.append(upperChar.charAt(Rnd.nextInt(upperChar.length() - 1)));
					break;
				case 2:
					password.append(digits.charAt(Rnd.nextInt(digits.length() - 1)));
					break;
			}
		}
		return password.toString();
	}
	
	public void sendMessageToTelnets(final String msg)
	{
		final List<LoginStatusThread> lsToRemove = new FastList<>();
		for (final LoginStatusThread ls : _loginStatus)
		{
			if (ls.isInterrupted())
			{
				lsToRemove.add(ls);
			}
			else
			{
				ls.printToTelnet(msg);
			}
		}
	}
}

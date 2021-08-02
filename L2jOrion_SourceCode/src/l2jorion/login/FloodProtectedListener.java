/* L2jOrion Project - www.l2jorion.com 
 * 
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
package l2jorion.login;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public abstract class FloodProtectedListener extends Thread
{
	private final Logger LOG = LoggerFactory.getLogger(FloodProtectedListener.class);
	private final Map<String, ForeignConnection> _floodProtection = new FastMap<>();
	private final String _listenIp;
	private final int _port;
	private ServerSocket _serverSocket;
	
	public FloodProtectedListener(final String listenIp, final int port) throws IOException
	{
		_port = port;
		_listenIp = listenIp;
		
		if (_listenIp.equals("*"))
		{
			_serverSocket = new ServerSocket(_port);
		}
		else
		{
			_serverSocket = new ServerSocket(_port, 50, InetAddress.getByName(_listenIp));
		}
	}
	
	@Override
	public void run()
	{
		Socket connection = null;
		
		while (true)
		{
			try
			{
				connection = _serverSocket.accept();
				if (Config.FLOOD_PROTECTION)
				{
					ForeignConnection fConnection = _floodProtection.get(connection.getInetAddress().getHostAddress());
					
					if (fConnection != null)
					{
						fConnection.connectionNumber += 1;
						if (fConnection.connectionNumber > Config.FAST_CONNECTION_LIMIT && System.currentTimeMillis() - fConnection.lastConnection < Config.NORMAL_CONNECTION_TIME || System.currentTimeMillis() - fConnection.lastConnection < Config.FAST_CONNECTION_TIME || fConnection.connectionNumber > Config.MAX_CONNECTION_PER_IP)
						{
							fConnection.lastConnection = System.currentTimeMillis();
							connection.close();
							
							fConnection.connectionNumber -= 1;
							
							if (!fConnection.isFlooding)
							{
								LOG.warn("Potential Flood from " + connection.getInetAddress().getHostAddress());
							}
							
							fConnection.isFlooding = true;
							continue;
						}
						
						if (fConnection.isFlooding) // if connection was flooding server but now passed the check
						{
							fConnection.isFlooding = false;
							LOG.info(connection.getInetAddress().getHostAddress() + " is not considered as flooding anymore.");
						}
						
						fConnection.lastConnection = System.currentTimeMillis();
						fConnection = null;
					}
					else
					{
						fConnection = new ForeignConnection(System.currentTimeMillis());
						_floodProtection.put(connection.getInetAddress().getHostAddress(), fConnection);
						fConnection = null;
					}
				}
				addClient(connection);
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					e.printStackTrace();
				try
				{
					if (connection != null)
					{
						connection.close();
					}
				}
				catch (final Exception e2)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e2.printStackTrace();
					
				}
				if (isInterrupted())
				{
					// shutdown?
					try
					{
						_serverSocket.close();
					}
					catch (final IOException io)
					{
						LOG.error("fixme:unhandled exception", io);
					}
					break;
				}
			}
		}
	}
	
	protected static class ForeignConnection
	{
		public int connectionNumber;
		public long lastConnection;
		public boolean isFlooding = false;
		
		/**
		 * @param time
		 */
		public ForeignConnection(final long time)
		{
			lastConnection = time;
			connectionNumber = 1;
		}
	}
	
	public abstract void addClient(Socket s);
	
	public void removeFloodProtection(final String ip)
	{
		if (!Config.FLOOD_PROTECTION)
			return;
		
		ForeignConnection fConnection = _floodProtection.get(ip);
		
		if (fConnection != null)
		{
			fConnection.connectionNumber -= 1;
			
			if (fConnection.connectionNumber == 0)
			{
				_floodProtection.remove(ip);
			}
		}
		else
		{
			LOG.warn("Removing a flood protection for a GameServer that was not in the connection map??? :" + ip);
		}
	}
	
	public void close()
	{
		try
		{
			_serverSocket.close();
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}
}

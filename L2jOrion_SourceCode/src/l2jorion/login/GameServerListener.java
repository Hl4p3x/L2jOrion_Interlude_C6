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
import java.net.Socket;
import java.util.List;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class GameServerListener extends FloodProtectedListener
{
	private static Logger LOG = LoggerFactory.getLogger(GameServerListener.class);
	private static List<GameServerThread> _gameServers = new FastList<>();
	
	public GameServerListener() throws IOException
	{
		super(Config.GAME_SERVER_LOGIN_HOST, Config.GAME_SERVER_LOGIN_PORT);
	}
	
	/**
	 * @see l2jorion.login.FloodProtectedListener#addClient(java.net.Socket)
	 */
	@Override
	public void addClient(final Socket s)
	{
		if (Config.DEBUG)
		{
			LOG.info("Received gameserver connection from: " + s.getInetAddress().getHostAddress());
		}
		
		final GameServerThread gst = new GameServerThread(s);
		gst.start();
		_gameServers.add(gst);
		
	}
	
	public void removeGameServer(final GameServerThread gst)
	{
		_gameServers.remove(gst);
	}
}

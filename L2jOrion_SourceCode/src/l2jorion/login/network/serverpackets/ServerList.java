/*
 * L2jOrion Project - www.l2jorion.com 
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
package l2jorion.login.network.serverpackets;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javolution.util.FastList;
import l2jorion.game.datatables.GameServerTable;
import l2jorion.game.datatables.GameServerTable.GameServerInfo;
import l2jorion.login.L2LoginClient;
import l2jorion.login.network.gameserverpackets.ServerStatus;

public final class ServerList extends L2LoginServerPacket
{
	private final List<ServerData> _servers;
	private final int _lastServer;
	
	class ServerData
	{
		protected String _ip;
		protected int _port;
		protected boolean _pvp;
		protected int _currentPlayers;
		protected int _maxPlayers;
		protected boolean _testServer;
		protected boolean _brackets;
		protected boolean _clock;
		protected int _status;
		protected int _serverId;
		
		ServerData(final String pIp, final int pPort, final boolean pPvp, final boolean pTestServer, final int pCurrentPlayers, final int pMaxPlayers, final boolean pBrackets, final boolean pClock, final int pStatus, final int pServer_id)
		{
			_ip = pIp;
			_port = pPort;
			_pvp = pPvp;
			_testServer = pTestServer;
			_currentPlayers = pCurrentPlayers;
			_maxPlayers = pMaxPlayers;
			_brackets = pBrackets;
			_clock = pClock;
			_status = pStatus;
			_serverId = pServer_id;
		}
	}
	
	public ServerList(final L2LoginClient client)
	{
		_servers = new FastList<>();
		_lastServer = client.getLastServer();
		
		for (final GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
		{
			if (gsi.getStatus() == ServerStatus.STATUS_GM_ONLY && client.getAccessLevel() >= 100)
			{
				// Server is GM-Only but you've got GM Status
				addServer(client.usesInternalIPas() ? gsi.getInternalHost() : gsi.getExternalHost(), gsi.getPortas(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), gsi.getStatus(), gsi.getId());
			}
			else if (gsi.getStatus() != ServerStatus.STATUS_GM_ONLY)
			{
				// Server is not GM-Only
				addServer(client.usesInternalIPas() ? gsi.getInternalHost() : gsi.getExternalHost(), gsi.getPortas(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), gsi.getStatus(), gsi.getId());
			}
			else
			{
				// Server's GM-Only and you've got no GM-Status
				addServer(client.usesInternalIPas() ? gsi.getInternalHost() : gsi.getExternalHost(), gsi.getPortas(), gsi.isPvp(), gsi.isTestServer(), gsi.getCurrentPlayerCount(), gsi.getMaxPlayers(), gsi.isShowingBrackets(), gsi.isShowingClock(), ServerStatus.STATUS_DOWN, gsi.getId());
			}
		}
	}
	
	public void addServer(final String ip, final int port, final boolean pvp, final boolean testServer, final int currentPlayer, final int maxPlayer, final boolean brackets, final boolean clock, final int status, final int server_id)
	{
		_servers.add(new ServerData(ip, port, pvp, testServer, currentPlayer, maxPlayer, brackets, clock, status, server_id));
	}
	
	@Override
	public void write()
	{
		writeC(0x04);
		writeC(_servers.size());
		writeC(_lastServer);
		
		for (final ServerData server : _servers)
		{
			writeC(server._serverId); // server id
			
			try
			{
				final InetAddress i4 = InetAddress.getByName(server._ip);
				
				byte[] raw = i4.getAddress();
				
				writeC(raw[0] & 0xff);
				writeC(raw[1] & 0xff);
				writeC(raw[2] & 0xff);
				writeC(raw[3] & 0xff);
			}
			catch (final UnknownHostException e)
			{
				e.printStackTrace();
				writeC(127);
				writeC(0);
				writeC(0);
				writeC(1);
			}
			
			writeD(server._port);
			writeC(0x00); // age limit
			writeC(server._pvp ? 0x01 : 0x00);
			writeH(server._currentPlayers);
			writeH(server._maxPlayers);
			writeC(server._status == ServerStatus.STATUS_DOWN ? 0x00 : 0x01);
			
			int bits = 0;
			
			if (server._testServer)
			{
				bits |= 0x04;
			}
			
			if (server._clock)
			{
				bits |= 0x02;
			}
			
			writeD(bits);
			writeC(server._brackets ? 0x01 : 0x00);
		}
	}
	
	@Override
	public String getType()
	{
		return "ServerList";
	}
}

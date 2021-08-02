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
package l2jorion.login;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;
import l2jorion.Config;
import l2jorion.crypt.NewCrypt;
import l2jorion.game.datatables.GameServerTable;
import l2jorion.game.datatables.GameServerTable.GameServerInfo;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.login.network.gameserverpackets.BlowFishKey;
import l2jorion.login.network.gameserverpackets.ChangeAccessLevel;
import l2jorion.login.network.gameserverpackets.GameServerAuth;
import l2jorion.login.network.gameserverpackets.PlayerAuthRequest;
import l2jorion.login.network.gameserverpackets.PlayerInGame;
import l2jorion.login.network.gameserverpackets.PlayerLogout;
import l2jorion.login.network.gameserverpackets.ServerStatus;
import l2jorion.login.network.loginserverpackets.AuthResponse;
import l2jorion.login.network.loginserverpackets.InitLS;
import l2jorion.login.network.loginserverpackets.KickPlayer;
import l2jorion.login.network.loginserverpackets.LoginServerFail;
import l2jorion.login.network.loginserverpackets.PlayerAuthResponse;
import l2jorion.login.network.serverpackets.ServerBasePacket;
import l2jorion.util.Util;

public class GameServerThread extends Thread
{
	protected static final Logger LOG = LoggerFactory.getLogger(GameServerThread.class);
	private final Socket _connection;
	private InputStream _in;
	private OutputStream _out;
	private final RSAPublicKey _publicKey;
	private final RSAPrivateKey _privateKey;
	private NewCrypt _blowfish;
	private byte[] _blowfishKey;
	
	private final String _connectionIp;
	
	private GameServerInfo _gsi;
	
	private final Set<String> _accountsOnGameServer = new FastSet<>();
	
	private String _connectionIPAddress;
	
	@Override
	public void run()
	{
		boolean checkTime = true;
		final long time = System.currentTimeMillis();
		_connectionIPAddress = _connection.getInetAddress().getHostAddress();
		
		if (GameServerThread.isBannedGameserverIP(_connectionIPAddress))
		{
			LOG.info("GameServerRegistration: IP Address " + _connectionIPAddress + " is on Banned IP list.");
			forceClose(LoginServerFail.REASON_IP_BANNED);
			return;
		}
		
		InitLS startPacket = new InitLS(_publicKey.getModulus().toByteArray());
		try
		{
			sendPacket(startPacket);
			
			int lengthHi = 0;
			int lengthLo = 0;
			int length = 0;
			boolean checksumOk = false;
			while (true)
			{
				if (time - System.currentTimeMillis() > 10000 && checkTime)
				{
					_connection.close();
					break;
				}
				
				try
				{
					lengthLo = _in.read();
					lengthHi = _in.read();
					length = lengthHi * 256 + lengthLo;
				}
				catch (final IOException e)
				{
					lengthHi = -1;
				}
				
				if (lengthHi < 0 || _connection.isClosed())
				{
					LOG.info("GameServerThread: Login terminated the connection");
					break;
				}
				
				byte[] data = new byte[length - 2];
				
				int receivedBytes = 0;
				int newBytes = 0;
				
				while (newBytes != -1 && receivedBytes < length - 2)
				{
					newBytes = _in.read(data, 0, length - 2);
					receivedBytes = receivedBytes + newBytes;
				}
				
				if (receivedBytes != length - 2)
				{
					LOG.warn("Incomplete Packet is sent to the server, closing connection.(LS)");
					break;
				}
				
				// decrypt if we have a key
				data = _blowfish.decrypt(data);
				checksumOk = NewCrypt.verifyChecksum(data);
				
				if (!checksumOk)
				{
					LOG.warn("Incorrect packet checksum, closing connection (LS)");
					return;
				}
				
				final int packetType = data[0] & 0xff;
				switch (packetType)
				{
					case 00:
						checkTime = false;
						onReceiveBlowfishKey(data);
						break;
					case 01:
						onGameServerAuth(data);
						break;
					case 02:
						onReceivePlayerInGame(data);
						break;
					case 03:
						onReceivePlayerLogOut(data);
						break;
					case 04:
						onReceiveChangeAccessLevel(data);
						break;
					case 05:
						onReceivePlayerAuthRequest(data);
						break;
					case 06:
						onReceiveServerStatus(data);
						break;
					default:
						LOG.warn("Unknown Opcode (" + Integer.toHexString(packetType).toUpperCase() + ") from GameServer, closing connection.");
						forceClose(LoginServerFail.NOT_AUTHED);
				}
				
			}
		}
		catch (final IOException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			String serverName = getServerId() != -1 ? "[" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) : "(" + _connectionIPAddress + ")";
			String msg = "GameServer " + serverName + ": Connection lost: " + e.getMessage();
			LOG.info(msg);
		}
		finally
		{
			if (isAuthed())
			{
				_gsi.setDown();
				LOG.info("Server: " + getServerId() + " " + GameServerTable.getInstance().getServerNameById(getServerId()) + " is now set as disconnected.");
			}
			
			L2LoginServer.getInstance().getGameServerListener().removeGameServer(this);
			L2LoginServer.getInstance().getGameServerListener().removeFloodProtection(_connectionIp);
		}
	}
	
	private void onReceiveBlowfishKey(final byte[] data)
	{
		BlowFishKey bfk = new BlowFishKey(data, _privateKey);
		_blowfishKey = bfk.getKey();
		_blowfish = new NewCrypt(_blowfishKey);
		
		if (Config.DEBUG)
		{
			LOG.info("New BlowFish key received, Blowfih Engine initialized:");
		}
	}
	
	private void onGameServerAuth(final byte[] data) throws IOException
	{
		GameServerAuth gsa = new GameServerAuth(data);
		
		if (Config.DEBUG)
		{
			LOG.info("Auth request received");
		}
		
		handleRegProcess(gsa);
		
		if (isAuthed())
		{
			AuthResponse ar = new AuthResponse(getGameServerInfo().getId());
			sendPacket(ar);
			
			if (Config.DEBUG)
			{
				LOG.info("Authed: id: " + getGameServerInfo().getId());
			}
			ar = null;
		}
		
		gsa = null;
	}
	
	private void onReceivePlayerInGame(final byte[] data)
	{
		if (isAuthed())
		{
			PlayerInGame pig = new PlayerInGame(data);
			List<String> newAccounts = pig.getAccounts();
			
			for (final String account : newAccounts)
			{
				_accountsOnGameServer.add(account);
				
				if (Config.DEBUG)
				{
					LOG.info("Account " + account + " logged in GameServer: [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()));
				}
			}
			
			pig = null;
			newAccounts = null;
			
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}
	
	private void onReceivePlayerLogOut(final byte[] data)
	{
		if (isAuthed())
		{
			PlayerLogout plo = new PlayerLogout(data);
			_accountsOnGameServer.remove(plo.getAccount());
			
			if (Config.DEBUG)
			{
				LOG.info("Player " + plo.getAccount() + " logged out from gameserver [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()));
			}
			plo = null;
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}
	
	private void onReceiveChangeAccessLevel(final byte[] data)
	{
		if (isAuthed())
		{
			ChangeAccessLevel cal = new ChangeAccessLevel(data);
			LoginController.getInstance().setAccountAccessLevel(cal.getAccount(), cal.getLevel());
			LOG.info("Changed " + cal.getAccount() + " access level to " + cal.getLevel());
			cal = null;
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}
	
	private void onReceivePlayerAuthRequest(final byte[] data) throws IOException
	{
		if (isAuthed())
		{
			PlayerAuthRequest par = new PlayerAuthRequest(data);
			PlayerAuthResponse authResponse;
			
			if (Config.DEBUG)
			{
				LOG.info("auth request received for Player " + par.getAccount());
			}
			
			SessionKey key = LoginController.getInstance().getKeyForAccount(par.getAccount());
			
			if (key != null && key.equals(par.getKey()))
			{
				if (Config.DEBUG)
				{
					LOG.info("auth request: OK");
				}
				
				LoginController.getInstance().removeAuthedLoginClient(par.getAccount());
				authResponse = new PlayerAuthResponse(par.getAccount(), true);
			}
			else
			{
				if (Config.DEBUG)
				{
					LOG.info("auth request: NO");
					LOG.info("session key from self: " + key);
					LOG.info("session key sent: " + par.getKey());
				}
				authResponse = new PlayerAuthResponse(par.getAccount(), false);
			}
			sendPacket(authResponse);
			
			par = null;
			authResponse = null;
			key = null;
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}
	
	private void onReceiveServerStatus(final byte[] data)
	{
		if (isAuthed())
		{
			if (Config.DEBUG)
			{
				LOG.info("ServerStatus received");
			}
			/* ServerStatus ss = */new ServerStatus(data, getServerId()); // server status
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}
	
	private void handleRegProcess(final GameServerAuth gameServerAuth)
	{
		GameServerTable gameServerTable = GameServerTable.getInstance();
		
		final int id = gameServerAuth.getDesiredID();
		final byte[] hexId = gameServerAuth.getHexID();
		
		GameServerInfo gsi = gameServerTable.getRegisteredGameServerById(id);
		// is there a gameserver registered with this id?
		if (gsi != null)
		{
			// does the hex id match?
			if (Arrays.equals(gsi.getHexId(), hexId))
			{
				// check to see if this GS is already connected
				synchronized (gsi)
				{
					if (gsi.isAuthed())
					{
						forceClose(LoginServerFail.REASON_ALREADY_LOGGED8IN);
					}
					else
					{
						attachGameServerInfo(gsi, gameServerAuth);
					}
				}
			}
			else
			{
				// there is already a server registered with the desired id and different hex id
				// try to register this one with an alternative id
				if (Config.ACCEPT_NEW_GAMESERVER && gameServerAuth.acceptAlternateID())
				{
					gsi = new GameServerInfo(id, hexId, this);
					
					if (gameServerTable.registerWithFirstAvaliableId(gsi))
					{
						attachGameServerInfo(gsi, gameServerAuth);
						gameServerTable.registerServerOnDB(gsi);
					}
					else
					{
						forceClose(LoginServerFail.REASON_NO_FREE_ID);
					}
				}
				else
				{
					// server id is already taken, and we cant get a new one for you
					forceClose(LoginServerFail.REASON_WRONG_HEXID);
				}
			}
		}
		else
		{
			// can we register on this id?
			if (Config.ACCEPT_NEW_GAMESERVER)
			{
				gsi = new GameServerInfo(id, hexId, this);
				
				if (gameServerTable.register(id, gsi))
				{
					attachGameServerInfo(gsi, gameServerAuth);
					gameServerTable.registerServerOnDB(gsi);
				}
				else
				{
					// some one took this ID meanwhile
					forceClose(LoginServerFail.REASON_ID_RESERVED);
				}
			}
			else
			{
				forceClose(LoginServerFail.REASON_WRONG_HEXID);
			}
		}
		
		gameServerTable = null;
		gsi = null;
	}
	
	public boolean hasAccountOnGameServer(final String account)
	{
		return _accountsOnGameServer.contains(account);
	}
	
	public int getPlayerCount()
	{
		return _accountsOnGameServer.size();
	}
	
	/**
	 * Attachs a GameServerInfo to this Thread
	 * <li>Updates the GameServerInfo values based on GameServerAuth packet</li>
	 * <li><b>Sets the GameServerInfo as Authed</b></li>
	 * @param gsi The GameServerInfo to be attached.
	 * @param gameServerAuth The server info.
	 */
	private void attachGameServerInfo(final GameServerInfo gsi, final GameServerAuth gameServerAuth)
	{
		setGameServerInfo(gsi);
		gsi.setGameServerThread(this);
		gsi.setPort(gameServerAuth.getPort());
		setGameHosts(gameServerAuth.getExternalHost(), gameServerAuth.getInternalHost());
		gsi.setMaxPlayers(gameServerAuth.getMaxPlayers());
		gsi.setAuthed(true);
	}
	
	private void forceClose(final int reason)
	{
		LoginServerFail lsf = new LoginServerFail(reason);
		
		try
		{
			sendPacket(lsf);
		}
		catch (final IOException e)
		{
			LOG.error("GameServerThread: Failed kicking banned server", e);
		}
		
		try
		{
			_connection.close();
		}
		catch (final IOException e)
		{
			LOG.error("GameServerThread: Failed disconnecting banned server, server already disconnected", e);
		}
		
		lsf = null;
	}
	
	/**
	 * @param gameServerauth
	 */
	/*
	 * private void handleRegisterationProcess(GameServerAuth gameServerauth) { try { GameServerTable gsTableInstance = GameServerTable.getInstance(); if (gsTableInstance.isARegisteredServer(gameServerauth.getHexID())) { if (Config.DEBUG) { LOG.info("Valid HexID"); } _server_id =
	 * gsTableInstance.getServerIDforHex(gameServerauth.getHexID()); if (gsTableInstance.isServerAuthed(_server_id)) { LoginServerFail lsf = new LoginServerFail(LoginServerFail.REASON_ALREADY_LOGGED8IN); sendPacket(lsf); _connection.close(); return; } _gamePort = gameServerauth.getPort();
	 * setGameHosts(gameServerauth.getExternalHost(), gameServerauth.getInternalHost()); _max_players = gameServerauth.getMaxPlayers(); _hexID = gameServerauth.getHexID(); //gsTableInstance.addServer(this); } else if (Config.ACCEPT_NEW_GAMESERVER) { if (Config.DEBUG) { LOG.info("New HexID"); }
	 * if(!gameServerauth.acceptAlternateID()) { if(gsTableInstance.isIDfree(gameServerauth.getDesiredID())) { if (Config.DEBUG)LOG.info("Desired ID is Valid"); _server_id = gameServerauth.getDesiredID(); _gamePort = gameServerauth.getPort(); setGameHosts(gameServerauth.getExternalHost(),
	 * gameServerauth.getInternalHost()); _max_players = gameServerauth.getMaxPlayers(); _hexID = gameServerauth.getHexID(); gsTableInstance.createServer(this); //gsTableInstance.addServer(this); } else { LoginServerFail lsf = new LoginServerFail(LoginServerFail.REASON_ID_RESERVED); sendPacket(lsf);
	 * _connection.close(); return; } } else { int id; if(!gsTableInstance.isIDfree(gameServerauth.getDesiredID())) { id = gsTableInstance.findFreeID(); if (Config.DEBUG)LOG.info("Affected New ID:"+id); if(id < 0) { LoginServerFail lsf = new LoginServerFail(LoginServerFail.REASON_NO_FREE_ID);
	 * sendPacket(lsf); _connection.close(); return; } } else { id = gameServerauth.getDesiredID(); if (Config.DEBUG)LOG.info("Desired ID is Valid"); } _server_id = id; _gamePort = gameServerauth.getPort(); setGameHosts(gameServerauth.getExternalHost(), gameServerauth.getInternalHost());
	 * _max_players = gameServerauth.getMaxPlayers(); _hexID = gameServerauth.getHexID(); gsTableInstance.createServer(this); //gsTableInstance.addServer(this); } } else { LOG.info("Wrong HexID"); LoginServerFail lsf = new LoginServerFail(LoginServerFail.REASON_WRONG_HEXID); sendPacket(lsf);
	 * _connection.close(); return; } } catch (IOException e) { LOG.info("Error while registering GameServer "+GameServerTable.getInstance().serverNames.get(_server_id)+" (ID:"+_server_id+")"); } }
	 */
	
	/**
	 * @param ipAddress
	 * @return
	 */
	public static boolean isBannedGameserverIP(final String ipAddress)
	{
		return false;
	}
	
	public GameServerThread(final Socket con)
	{
		_connection = con;
		_connectionIp = con.getInetAddress().getHostAddress();
		try
		{
			_in = _connection.getInputStream();
			_out = new BufferedOutputStream(_connection.getOutputStream());
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
		
		final KeyPair pair = GameServerTable.getInstance().getKeyPair();
		_privateKey = (RSAPrivateKey) pair.getPrivate();
		_publicKey = (RSAPublicKey) pair.getPublic();
		_blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
	}
	
	/**
	 * @param sl
	 * @throws IOException
	 */
	private void sendPacket(final ServerBasePacket sl) throws IOException
	{
		byte[] data = sl.getContent();
		NewCrypt.appendChecksum(data);
		
		if (Config.DEBUG)
		{
			LOG.debug("[S] " + sl.getClass().getSimpleName() + ":\n" + Util.printData(data));
		}
		data = _blowfish.crypt(data);
		
		final int len = data.length + 2;
		synchronized (_out)
		{
			_out.write(len & 0xff);
			_out.write(len >> 8 & 0xff);
			_out.write(data);
			_out.flush();
		}
		
		data = null;
	}
	
	public void kickPlayer(final String account)
	{
		KickPlayer kp = new KickPlayer(account);
		try
		{
			sendPacket(kp);
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
		
		kp = null;
	}
	
	public void setGameHosts(final String gameExternalHost, final String gameInternalHost)
	{
		String oldInternal = _gsi.getInternalHost();
		String oldExternal = _gsi.getExternalHost();
		
		_gsi.setExternalHost(gameExternalHost);
		_gsi.setInternalIp(gameInternalHost);
		
		if (!gameExternalHost.equals("*"))
		{
			try
			{
				_gsi.setExternalIp(InetAddress.getByName(gameExternalHost).getHostAddress());
			}
			catch (final UnknownHostException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("Couldn't resolve hostname \"" + gameExternalHost + "\"");
			}
		}
		else
		{
			_gsi.setExternalIp(_connectionIp);
		}
		
		if (!gameInternalHost.equals("*"))
		{
			try
			{
				_gsi.setInternalIp(InetAddress.getByName(gameInternalHost).getHostAddress());
			}
			catch (final UnknownHostException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("Couldn't resolve hostname \"" + gameInternalHost + "\"");
			}
		}
		else
		{
			_gsi.setInternalIp(_connectionIp);
		}
		
		LOG.info("Connected to gameserver: " + getServerId() + " " + GameServerTable.getInstance().getServerNameById(getServerId()));
		
		if (oldInternal == null || !oldInternal.equalsIgnoreCase(gameInternalHost))
		{
			LOG.info("InternalIP: " + gameInternalHost);
		}
		
		if (oldExternal == null || !oldExternal.equalsIgnoreCase(gameExternalHost))
		{
			LOG.info("ExternalIP: " + gameExternalHost);
		}
		
	}
	
	public boolean isAuthed()
	{
		if (getGameServerInfo() == null)
		{
			return false;
		}
		
		return getGameServerInfo().isAuthed();
	}
	
	public void setGameServerInfo(final GameServerInfo gsi)
	{
		_gsi = gsi;
	}
	
	public GameServerInfo getGameServerInfo()
	{
		return _gsi;
	}
	
	/**
	 * @return Returns the connectionIpAddress.
	 */
	public String getConnectionIpAddress()
	{
		return _connectionIPAddress;
	}
	
	private int getServerId()
	{
		if (getGameServerInfo() != null)
		{
			return getGameServerInfo().getId();
		}
		
		return -1;
	}
}

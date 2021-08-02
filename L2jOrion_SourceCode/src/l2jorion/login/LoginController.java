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
package l2jorion.login;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;

import javax.crypto.Cipher;

import javolution.util.FastCollection.Record;
import l2jorion.Config;
import l2jorion.crypt.Base64;
import l2jorion.crypt.ScrambledKeyPair;
import l2jorion.game.datatables.GameServerTable;
import l2jorion.game.datatables.GameServerTable.GameServerInfo;
import l2jorion.login.network.gameserverpackets.ServerStatus;
import l2jorion.login.network.serverpackets.LoginFail.LoginFailReason;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.Util;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;
import javolution.util.FastList;
import javolution.util.FastMap;

public class LoginController
{
	protected class ConnectionChecker extends Thread
	{
		@Override
		public void run()
		{
			for(;;)
			{
				long now = System.currentTimeMillis();
				if(_stopNow) break;
				for(final L2LoginClient cl : _clients) try
				{
					if (cl!=null && now - cl.getConnectionStartTime() > Config.SESSION_TTL)
					{
						LOG.info("Closing "+cl.getIntetAddress()+" because idle time too long");
						cl.close(LoginFailReason.REASON_TEMP_PASS_EXPIRED);
					}
					else
					{
						_clients.remove(cl);
					}
				}
				catch(Exception e)
				{
					if(Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					
				}
				try
				{ 
					Thread.sleep(2500);
				}
				catch(Exception e)
				{
					if(Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					
				}
			}
		}
	}
	protected static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

	private static LoginController _instance;
	
	/** Time before kicking the client if he didnt logged yet */
	private final static int LOGIN_TIMEOUT = 60 * 1000;

	/** Clients that are on the LS but arent assocated with a account yet */
	protected FastList<L2LoginClient> _clients = new FastList<>();

	/** Authed Clients on LoginServer */
	protected FastMap<String, L2LoginClient> _loginServerClients = new FastMap<String, L2LoginClient>().shared();

	private Map<InetAddress, BanInfo> _bannedIps = new FastMap<InetAddress, BanInfo>().shared();

	private Map<InetAddress, FailedLoginAttempt> _hackProtection;
	protected ScrambledKeyPair[] _keyPairs;

	protected byte[][] _blowfishKeys;
	private static final int BLOWFISH_KEYS = 20;

	public static void load() throws GeneralSecurityException
	{
		if(_instance == null)
		{
			_instance = new LoginController();
		}
		else
			throw new IllegalStateException("LoginController can only be loaded a single time.");
	}

	public static LoginController getInstance()
	{
		return _instance;
	}

	private LoginController() throws GeneralSecurityException
	{
		Util.printSection("LoginController");

		_hackProtection = new FastMap<>();

		_keyPairs = new ScrambledKeyPair[10];
		KeyPairGenerator keygen = null;

		keygen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
		keygen.initialize(spec);

		//generate the initial set of keys
		for(int i = 0; i < 10; i++)
		{
			_keyPairs[i] = new ScrambledKeyPair(keygen.generateKeyPair());
		}

		LOG.info("Cached 10 KeyPairs for RSA communication");

		testCipher((RSAPrivateKey) _keyPairs[0]._pair.getPrivate());

		// Store keys for blowfish communication
		generateBlowFishKeys();

		spec = null;
		keygen = null;
		new ConnectionChecker().start();
	}

	/**
	 * This is mostly to force the initialization of the Crypto Implementation, avoiding it being done on runtime when
	 * its first needed.<BR>
	 * In short it avoids the worst-case execution time on runtime by doing it on loading.
	 * 
	 * @param key Any private RSA Key just for testing purposes.
	 * @throws GeneralSecurityException if a underlying exception was thrown by the Cipher
	 */
	private void testCipher(RSAPrivateKey key) throws GeneralSecurityException
	{
		// avoid worst-case execution, KenM
		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
		rsaCipher.init(Cipher.DECRYPT_MODE, key);
		rsaCipher = null;
	}

	protected boolean _stopNow = false;
	
	public void shutdown()
	{
		_stopNow = true;
		try
		{
		Thread.sleep(10000);
		} catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
		}
	}

	private void generateBlowFishKeys()
	{
		_blowfishKeys = new byte[BLOWFISH_KEYS][16];

		for(int i = 0; i < BLOWFISH_KEYS; i++)
		{
			for(int j = 0; j < _blowfishKeys[i].length; j++)
			{
				_blowfishKeys[i][j] = (byte) (Rnd.nextInt(255) + 1);
			}
		}
		LOG.info("Stored " + _blowfishKeys.length + " keys for Blowfish communication");
	}

	/**
	 * @return Returns a random key
	 */
	public byte[] getBlowfishKey()
	{
		return _blowfishKeys[(int) (Math.random() * BLOWFISH_KEYS)];
	}

	public void addLoginClient(L2LoginClient client)
	{
		if (_clients.size() >= Config.MAX_LOGINSESSIONS)
		{
			for (L2LoginClient cl : _clients) try
			{
				cl.close(LoginFailReason.REASON_DUAL_BOX);
			}
			catch(Exception e)
			{
				if(Config.ENABLE_ALL_EXCEPTIONS)
					e.printStackTrace();
				
			}
		}
		synchronized (_clients)
		{
			_clients.add(client);
		}
	}

	public void removeLoginClient(L2LoginClient client)
	{
		if(_clients.contains(client))
		synchronized (_clients)
		{
		try
		{
			_clients.remove(client);
		}
		catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
		}
		}
	}

	public SessionKey assignSessionKeyToClient(String account, L2LoginClient client)
	{
		SessionKey key;

		key = new SessionKey(Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt());
		_loginServerClients.put(account, client);
		return key;
	}

	public void removeAuthedLoginClient(String account)
	{
		try {
			_loginServerClients.remove(account);
			
		} catch(Exception e) {
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
		}
	}

	public boolean isAccountInLoginServer(String account)
	{
		return _loginServerClients.containsKey(account);
	}

	public L2LoginClient getAuthedClient(String account)
	{
		return _loginServerClients.get(account);
	}

	public static enum AuthLoginResult
	{
		INVALID_PASSWORD,
		ACCOUNT_BANNED,
		ALREADY_ON_LS,
		ALREADY_ON_GS,
		AUTH_SUCCESS
	}
	
	public AuthLoginResult tryAuthLogin(String account, String password, L2LoginClient client)
	{
		AuthLoginResult ret = AuthLoginResult.INVALID_PASSWORD;
		// check auth
		if (loginValid(account, password, client))
		{
			// login was successful, verify presence on Gameservers
			ret = AuthLoginResult.ALREADY_ON_GS;
			
			if (!isAccountInAnyGameServer(account))
			{
				// account isnt on any GS verify LS itself
				ret = AuthLoginResult.ALREADY_ON_LS;
				
				// dont allow 2 simultaneous login
				synchronized (_loginServerClients)
				{
					if(!_loginServerClients.containsKey(account))
					{
						_loginServerClients.put(account, client);
						ret = AuthLoginResult.AUTH_SUCCESS;
						
						// remove him from the non-authed list
						removeLoginClient(client);
					}
				}
			}
		}
		else
		{
			if(client.getAccessLevel() < 0)
			{
				ret = AuthLoginResult.ACCOUNT_BANNED;
			}
		}
		return ret;
	}

	/**
	 * Adds the address to the ban list of the login server, with the given duration.
	 * 
	 * @param address The Address to be banned.
	 * @param expiration Timestamp in miliseconds when this ban expires
	 * @throws UnknownHostException if the address is invalid.
	 */
	public void addBanForAddress(String address, long expiration) throws UnknownHostException
	{

		InetAddress netAddress = InetAddress.getByName(address);
		_bannedIps.put(netAddress, new BanInfo(netAddress, expiration));
		netAddress = null;
	}

	/**
	 * Adds the address to the ban list of the login server, with the given duration.
	 * 
	 * @param address The Address to be banned.
	 * @param duration is miliseconds
	 */
	public void addBanForAddress(InetAddress address, long duration)
	{
		_bannedIps.put(address, new BanInfo(address, System.currentTimeMillis() + duration));
	}

	public boolean isBannedAddress(InetAddress address)
	{
		BanInfo bi = _bannedIps.get(address);
		if(bi != null)
		{
			if(bi.hasExpired())
			{
				_bannedIps.remove(address);
				return false;
			}
			return true;
		}
		return false;
	}

	public Map<InetAddress, BanInfo> getBannedIps()
	{
		return _bannedIps;
	}

	/**
	 * Remove the specified address from the ban list
	 * 
	 * @param address The address to be removed from the ban list
	 * @return true if the ban was removed, false if there was no ban for this ip
	 */
	public boolean removeBanForAddress(InetAddress address)
	{
		return _bannedIps.remove(address) != null;
	}

	/**
	 * Remove the specified address from the ban list
	 * 
	 * @param address The address to be removed from the ban list
	 * @return true if the ban was removed, false if there was no ban for this ip or the address was invalid.
	 */
	public boolean removeBanForAddress(String address)
	{
		try
		{
			return this.removeBanForAddress(InetAddress.getByName(address));
		}
		catch(UnknownHostException e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			return false;
		}
	}
	
	public SessionKey getKeyForAccount(String account)
	{
		L2LoginClient client = _loginServerClients.get(account);
		
		if(client != null)
			return client.getSessionKey();
		
		return null;
	}
	
	public int getOnlinePlayerCount(int serverId)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
		
		if(gsi != null && gsi.isAuthed())
			return gsi.getCurrentPlayerCount();
		
		return 0;
	}

	public boolean isAccountInAnyGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		
		for(GameServerInfo gsi : serverList)
		{
			GameServerThread gst = gsi.getGameServerThread();
			
			if (gst != null && gst.hasAccountOnGameServer(account))
				return true;
		}
		return false;
	}

	public GameServerInfo getAccountOnGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		
		for(GameServerInfo gsi : serverList)
		{
			GameServerThread gst = gsi.getGameServerThread();
			
			if(gst != null && gst.hasAccountOnGameServer(account))
				return gsi;

		}
		return null;
	}

	public int getTotalOnlinePlayerCount()
	{
		int total = 0;
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		
		for(GameServerInfo gsi : serverList)
		{
			if(gsi.isAuthed())
			{
				total += gsi.getCurrentPlayerCount();
			}
		}
		return total;
	}

	public int getMaxAllowedOnlinePlayers(int id)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(id);

		if(gsi != null)
			return gsi.getMaxPlayers();
		
		return 0;
	}
	
	/**
	 * @param client 
	 * @param serverId 
	 * @return
	 */
	public boolean isLoginPossible(L2LoginClient client, int serverId)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
		int access = client.getAccessLevel();
		if(gsi != null && gsi.isAuthed())
		{
			boolean loginOk = gsi.getCurrentPlayerCount() < gsi.getMaxPlayers() && gsi.getStatus() != ServerStatus.STATUS_GM_ONLY || access >= 100;
			if(loginOk && client.getLastServer() != serverId)
			{
				Connection con = null;
				
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					
					String stmt = "UPDATE accounts SET lastServer = ? WHERE login = ?";
					PreparedStatement statement = con.prepareStatement(stmt);
					statement.setInt(1, serverId);
					statement.setString(2, client.getAccount());
					statement.executeUpdate();
					statement.close();
					statement = null;
				}
				catch(Exception e)
				{
					if(Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					
					LOG.warn("Could not set lastServer: " + e);
				}
				finally
				{
					CloseUtil.close(con);
					con = null;
				}
			}
			return loginOk;
		}
		return false;
	}

	public void setAccountAccessLevel(String account, int banLevel)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			String stmt = "UPDATE accounts SET access_level=? WHERE login=?";
			PreparedStatement statement = con.prepareStatement(stmt);
			statement.setInt(1, banLevel);
			statement.setString(2, account);
			statement.executeUpdate();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("Could not set accessLevel: " + e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}

	public boolean isGM(String user)
	{
		boolean ok = false;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT access_level FROM accounts WHERE login=?");
			statement.setString(1, user);
			ResultSet rset = statement.executeQuery();

			if(rset.next())
			{
				int accessLevel = rset.getInt(1);

				if(accessLevel >= 100)
				{
					ok = true;
				}
			}

			rset.close();
			statement.close();
			statement = null;
			rset = null;
		}
		catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("could not check gm state:" + e);
			ok = false;
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		return ok;
	}

	/**
	 * <p>
	 * This method returns one of the cached {@link ScrambledKeyPair ScrambledKeyPairs} for communication with Login
	 * Clients.
	 * </p>
	 * 
	 * @return a scrambled keypair
	 */
	public ScrambledKeyPair getScrambledRSAKeyPair()
	{
		return _keyPairs[Rnd.nextInt(10)];
	}

	/**
	 * user name is not case sensitive any more
	 * 
	 * @param user
	 * @param password
	 * @param client 
	 * @return
	 */
	public synchronized boolean loginValid(String user, String password, L2LoginClient client)
	{
		boolean ok = false;
		InetAddress address = client.getConnection().getInetAddress();
		// log it anyway
		Log.add("'" + (user == null ? "null" : user) + "' " + (address == null ? "null" : address.getHostAddress()), "logins_ip");
		
		// player disconnected meanwhile
		if (address == null)
			return false;
		
		Connection con = null;
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] raw = password.getBytes("UTF-8");
			byte[] hash = md.digest(raw);
			
			byte[] expected = null;
			int access = 0;
			int lastServer = 1;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT password, access_level, lastServer FROM accounts WHERE login=?");
			statement.setString(1, user);
			ResultSet rset = statement.executeQuery();
			
			if (rset.next())
			{
				expected = Base64.decode(rset.getString("password"));
				access = rset.getInt("access_level");
				lastServer = rset.getInt("lastServer");
				
				if (lastServer <= 0)
				{
					lastServer = 1; // minServerId is 1 in Interlude
				}
				
				if (Config.DEBUG)
				{
					LOG.info("Account already exists.");
				}
			}
			
			rset.close();
			statement.close();
			rset = null;
			statement = null;
			
			// if account doesnt exists
			if (expected == null)
			{
				if (Config.AUTO_CREATE_ACCOUNTS)
				{
					if ((user != null) && (user.length()) >= 2 && (user.length() <= 14))
					{
						statement = con.prepareStatement("INSERT INTO accounts (login,password,lastactive,access_level,lastIP) values(?,?,?,?,?)");
						statement.setString(1, user);
						statement.setString(2, Base64.encodeBytes(hash));
						statement.setLong(3, System.currentTimeMillis());
						statement.setInt(4, 0);
						statement.setString(5, address.getHostAddress());
						statement.execute();
						statement.close();
						statement = null;
						//LOG.info("Created new account : " + user + " on IP : " + address.getHostAddress());
						String text = "Account: " + user + " IP: " + address.getHostAddress();
						Log.add(text, "NewAccounts");
						CloseUtil.close(con);
						con = null;
						return true;

					}
					LOG.warn("Invalid username creation/use attempt: " + user);
					CloseUtil.close(con);
					con = null;
					return false;
				}
				
				LOG.warn("Account missing for user "+user+" IP: "+address.getHostAddress());
				CloseUtil.close(con);
				con = null;
				return false;
			}
			
			// is this account banned?
			if (access < 0)
			{
				client.setAccessLevel(access);
				CloseUtil.close(con);
				con = null;
				return false;
			}
			
			// check password hash
			ok = true;
			for (int i = 0; i < expected.length; i++)
			{
				if(hash[i] != expected[i])
				{
					ok = false;
					break;
				}
			}
			
			PreparedStatement preparedstatement1 = con.prepareStatement(new StringBuilder().append(new StringBuilder().append("SELECT * FROM accounts WHERE login='")).append(user).append("';".toString()).toString());
			ResultSet resultset1 = preparedstatement1.executeQuery();
			resultset1.next();
			if ((!address.getHostAddress().equalsIgnoreCase(resultset1.getString("lastIP"))) && (resultset1.getBoolean("IPBlock")))
			{
				ok = false;
				LOG.warn(new StringBuilder().append(new StringBuilder().append("IP Blocker: failed attempt: account ")).append(user).append(" ip: ").append(address.getHostAddress()).toString());
			}
			
			if (ok)
			{
				client.setAccessLevel(access);
				client.setLastServer(lastServer);
				statement = con.prepareStatement("UPDATE accounts SET lastactive=?, lastIP=? WHERE login=?");
				statement.setLong(1, System.currentTimeMillis());
				statement.setString(2, address.getHostAddress());
				statement.setString(3, user);
				statement.execute();
				statement.close();
				statement = null;
			}
		}
		catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("Could not check password:" + e);
			ok = false;
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}

		if(!ok)
		{
			Log.add("'" + user + "' " + address.getHostAddress(), "logins_ip_fails");

			FailedLoginAttempt failedAttempt = _hackProtection.get(address);

			int failedCount;
			if(failedAttempt == null)
			{
				_hackProtection.put(address, new FailedLoginAttempt(address, password));
				failedCount = 1;
			}
			else
			{
				failedAttempt.increaseCounter(password);
				failedCount = failedAttempt.getCount();
			}

			if (failedCount >= Config.LOGIN_TRY_BEFORE_BAN)
			{
				LOG.info("Banning '" + address.getHostAddress() + "' for " + Config.LOGIN_BLOCK_AFTER_BAN + " seconds due to " + failedCount + " invalid user/pass attempts");
				this.addBanForAddress(address, Config.LOGIN_BLOCK_AFTER_BAN * 1000);
			}

			failedAttempt = null;
		}
		else
		{
			_hackProtection.remove(address);
			Log.add("'" + user + "' " + address.getHostAddress(), "logins_ip");
		}

		address = null;

		return ok;
	}

	public boolean loginBanned(String user)
	{
		boolean ok = false;

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT access_level FROM accounts WHERE login=?");
			statement.setString(1, user);
			ResultSet rset = statement.executeQuery();

			if(rset.next())
			{
				int accessLevel = rset.getInt(1);

				if(accessLevel < 0)
				{
					ok = true;
				}
			}

			rset.close();
			statement.close();
			rset = null;
			statement = null;
		}
		catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			// digest algo not found ??
			// out of bounds should not be possible
			LOG.warn("could not check ban state:" + e);
			ok = false;
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}

		return ok;
	}

	class FailedLoginAttempt
	{
		//private InetAddress _ipAddress;
		private int _count;
		private long _lastAttempTime;
		private String _lastPassword;

		public FailedLoginAttempt(InetAddress address, String lastPassword)
		{
			//_ipAddress = address;
			_count = 1;
			_lastAttempTime = System.currentTimeMillis();
			_lastPassword = lastPassword;
		}

		public void increaseCounter(String password)
		{
			if(!_lastPassword.equals(password))
			{
				// check if theres a long time since last wrong try
				if(System.currentTimeMillis() - _lastAttempTime < 300 * 1000)
				{
					_count++;
				}
				else
				{
					// restart the status
					_count = 1;

				}
				_lastPassword = password;
				_lastAttempTime = System.currentTimeMillis();
			}
			else
			//trying the same password is not brute force
			{
				_lastAttempTime = System.currentTimeMillis();
			}
		}
		
		public int getCount()
		{
			return _count;
		}
	}
	
	class BanInfo
	{
		private InetAddress _ipAddress;
		// Expiration
		private long _expiration;
		
		public BanInfo(InetAddress ipAddress, long expiration)
		{
			_ipAddress = ipAddress;
			_expiration = expiration;
		}
		
		public InetAddress getAddress()
		{
			return _ipAddress;
		}
		
		public boolean hasExpired()
		{
			return System.currentTimeMillis() > _expiration && _expiration > 0;
		}
	}
	
	class PurgeThread extends Thread
	{
		@Override
		public void run()
		{
			for(;;)
			{
				synchronized (_clients)
				{
					for(Record e = _clients.head(), end = _clients.tail(); (e = e.getNext()) != end;)
					{
						L2LoginClient client = _clients.valueOf(e);
						if(client.getConnectionStartTime() + LOGIN_TIMEOUT >= System.currentTimeMillis())
						{
							client.close(LoginFailReason.REASON_ACCESS_FAILED);
						}
					}
				}
				
				synchronized (_loginServerClients)
				{
					for(FastMap.Entry<String, L2LoginClient> e = _loginServerClients.head(), end = _loginServerClients.tail(); (e = e.getNext()) != end;)
					{
						L2LoginClient client = e.getValue();
						if(client.getConnectionStartTime() + LOGIN_TIMEOUT >= System.currentTimeMillis())
						{
							client.close(LoginFailReason.REASON_ACCESS_FAILED);
						}
					}
				}
				
				try
				{
					Thread.sleep(2 * LOGIN_TIMEOUT);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}

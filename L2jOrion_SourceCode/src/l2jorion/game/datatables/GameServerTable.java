package l2jorion.game.datatables;

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import l2jorion.ConfigLoader;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.login.GameServerThread;
import l2jorion.login.network.gameserverpackets.ServerStatus;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;
import l2jorion.util.xml.IXmlReader;

public class GameServerTable implements IXmlReader
{
	private static Logger LOG = LoggerFactory.getLogger(GameServerTable.class);
	
	private static final Map<Integer, String> SERVER_NAMES = new HashMap<>();
	private static final Map<Integer, GameServerInfo> GAME_SERVER_TABLE = new HashMap<>();
	private static final int KEYS_SIZE = 10;
	private KeyPair[] _keyPairs;
	
	public GameServerTable()
	{
		load();
		LOG.info("Loaded " + SERVER_NAMES.size() + " server names");
		
		loadRegisteredGameServers();
		LOG.info("Loaded " + GAME_SERVER_TABLE.size() + " registered Game Servers");
		
		loadRSAKeys();
		LOG.info("Cached " + _keyPairs.length + " RSA keys for Game Server communication.");
	}
	
	@Override
	public void load()
	{
		SERVER_NAMES.clear();
		parseDatapackFile(ConfigLoader.SERVER_NAME_FILE);
		LOG.info("Loaded " + SERVER_NAMES.size() + " server names.");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		final NodeList servers = doc.getElementsByTagName("server");
		for (int s = 0; s < servers.getLength(); s++)
		{
			SERVER_NAMES.put(parseInteger(servers.item(s).getAttributes(), "id"), parseString(servers.item(s).getAttributes(), "name"));
		}
	}
	
	private void loadRSAKeys()
	{
		try
		{
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4));
			_keyPairs = new KeyPair[KEYS_SIZE];
			for (int i = 0; i < KEYS_SIZE; i++)
			{
				_keyPairs[i] = keyGen.genKeyPair();
			}
		}
		catch (Exception e)
		{
			LOG.error("Error loading RSA keys for Game Server communication!");
		}
	}
	
	private void loadRegisteredGameServers()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement ps = con.createStatement();
			ResultSet rs = ps.executeQuery("SELECT * FROM gameservers"))
		{
			int id;
			while (rs.next())
			{
				id = rs.getInt("server_id");
				GAME_SERVER_TABLE.put(id, new GameServerInfo(id, stringToHex(rs.getString("hexid"))));
			}
		}
		catch (Exception e)
		{
			LOG.error("Error loading registered game servers!");
		}
	}
	
	public Map<Integer, GameServerInfo> getRegisteredGameServers()
	{
		return GAME_SERVER_TABLE;
	}
	
	public GameServerInfo getRegisteredGameServerById(int id)
	{
		return GAME_SERVER_TABLE.get(id);
	}
	
	public boolean hasRegisteredGameServerOnId(int id)
	{
		return GAME_SERVER_TABLE.containsKey(id);
	}
	
	public boolean registerWithFirstAvaliableId(GameServerInfo gsi)
	{
		// avoid two servers registering with the same "free" id
		synchronized (GAME_SERVER_TABLE)
		{
			for (Integer entry : SERVER_NAMES.keySet())
			{
				if (!GAME_SERVER_TABLE.containsKey(entry))
				{
					GAME_SERVER_TABLE.put(entry, gsi);
					gsi.setId(entry);
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean register(int id, GameServerInfo gsi)
	{
		synchronized (GAME_SERVER_TABLE)
		{
			if (!GAME_SERVER_TABLE.containsKey(id))
			{
				GAME_SERVER_TABLE.put(id, gsi);
				gsi.setId(id);
				return true;
			}
		}
		return false;
	}
	
	public void registerServerOnDB(GameServerInfo gsi)
	{
		registerServerOnDB(gsi.getHexId(), gsi.getId(), gsi.getExternalHost());
	}
	
	public void registerServerOnDB(byte[] hexId, int id, String externalHost)
	{
		register(id, new GameServerInfo(id, hexId));
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO gameservers (hexid,server_id,host) values (?,?,?)"))
		{
			ps.setString(1, hexToString(hexId));
			ps.setInt(2, id);
			ps.setString(3, externalHost);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOG.error("Error while saving gameserver!");
		}
	}
	
	public String getServerNameById(int id)
	{
		return getServerNames().get(id);
	}
	
	public Map<Integer, String> getServerNames()
	{
		return SERVER_NAMES;
	}
	
	public KeyPair getKeyPair()
	{
		return _keyPairs[Rnd.nextInt(10)];
	}
	
	private byte[] stringToHex(String string)
	{
		return new BigInteger(string, 16).toByteArray();
	}
	
	private String hexToString(byte[] hex)
	{
		if (hex == null)
		{
			return "null";
		}
		
		return new BigInteger(hex).toString(16);
	}
	
	public static class GameServerInfo
	{
		// auth
		private int _id;
		private final byte[] _hexId;
		private boolean _isAuthed;
		
		// status
		private GameServerThread _gst;
		private int _status;
		
		// network
		private String _internalIpas;
		private String _externalIpas;
		private String _externalHostas;
		private int _portas;
		
		// config
		private final boolean _isPvp = true;
		private boolean _isTestServer;
		private boolean _isShowingClock;
		private boolean _isShowingBrackets;
		private int _maxPlayers;
		
		public GameServerInfo(int id, byte[] hexId, GameServerThread gst)
		{
			_id = id;
			_hexId = hexId;
			_gst = gst;
			_status = ServerStatus.STATUS_DOWN;
		}
		
		public GameServerInfo(int id, byte[] hexId)
		{
			this(id, hexId, null);
		}
		
		public void setId(int id)
		{
			_id = id;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public byte[] getHexId()
		{
			return _hexId;
		}
		
		public void setAuthed(boolean isAuthed)
		{
			_isAuthed = isAuthed;
		}
		
		public boolean isAuthed()
		{
			return _isAuthed;
		}
		
		public void setGameServerThread(GameServerThread gst)
		{
			_gst = gst;
		}
		
		public GameServerThread getGameServerThread()
		{
			return _gst;
		}
		
		public void setStatus(int status)
		{
			_status = status;
		}
		
		public int getStatus()
		{
			return _status;
		}
		
		public int getCurrentPlayerCount()
		{
			if (_gst == null)
			{
				return 0;
			}
			
			return _gst.getPlayerCount();
		}
		
		public void setInternalIp(String internalIpas)
		{
			_internalIpas = internalIpas;
		}
		
		public String getInternalHost()
		{
			return _internalIpas;
		}
		
		public void setExternalIp(String externalIpas)
		{
			_externalIpas = externalIpas;
		}
		
		public String getExternalIpas()
		{
			return _externalIpas;
		}
		
		public void setExternalHost(String externalHostas)
		{
			_externalHostas = externalHostas;
		}
		
		public String getExternalHost()
		{
			return _externalHostas;
		}
		
		public int getPortas()
		{
			return _portas;
		}
		
		public void setPort(int portas)
		{
			_portas = portas;
		}
		
		public void setMaxPlayers(int maxPlayers)
		{
			_maxPlayers = maxPlayers;
		}
		
		public int getMaxPlayers()
		{
			return _maxPlayers;
		}
		
		public boolean isPvp()
		{
			return _isPvp;
		}
		
		public void setTestServer(boolean val)
		{
			_isTestServer = val;
		}
		
		public boolean isTestServer()
		{
			return _isTestServer;
		}
		
		public void setShowingClock(boolean clock)
		{
			_isShowingClock = clock;
		}
		
		public boolean isShowingClock()
		{
			return _isShowingClock;
		}
		
		public void setShowingBrackets(boolean val)
		{
			_isShowingBrackets = val;
		}
		
		public boolean isShowingBrackets()
		{
			return _isShowingBrackets;
		}
		
		public void setDown()
		{
			setAuthed(false);
			setPort(0);
			setGameServerThread(null);
			setStatus(ServerStatus.STATUS_DOWN);
		}
	}
	
	public static GameServerTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GameServerTable INSTANCE = new GameServerTable();
	}
}

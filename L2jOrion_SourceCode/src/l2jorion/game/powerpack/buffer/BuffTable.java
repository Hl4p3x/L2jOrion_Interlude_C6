package l2jorion.game.powerpack.buffer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javolution.util.FastMap;
import javolution.util.FastMap.Entry;
import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class BuffTable
{
	private static Logger LOG = LoggerFactory.getLogger(BuffTable.class);
	
	private static final String SQL_DELETE_SCHEME = "DELETE FROM buff_schemes WHERE ownerId=?";
	private static final String SQL_INSERT_SCHEME = "INSERT INTO buff_schemes (ownerId, skill_id, skill_level, premium, voter, useItem, itemId, itemCount, scheme) VALUES (?,?,?,?,?,?,?,?,?)";
	
	private FastMap<String, ArrayList<Buff>> _buffs = new FastMap<>();
	private FastMap<Integer, FastMap<String, ArrayList<Scheme>>> _schemesTable = new FastMap<>();

	private static BuffTable _instance = null;
	
	public static BuffTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new BuffTable();
		}
		return _instance;
	}
	
	private BuffTable()
	{
		_schemesTable.clear();
		BuffTableLoad();
	}
	
	public class Buff
	{
		public int _skillId;
		public int _skillLevel;
		public boolean _force;
		public int _minLevel;
		public int _maxLevel;
		public boolean _premium;
		public boolean _voter;
		public boolean _useItem;
		public int _itemId;
		public int _itemCount;
		
		public Buff(ResultSet r) throws SQLException
		{
			_skillId = r.getInt(2);
			_skillLevel = r.getInt(3);
			_force = r.getInt(4) == 1;
			_minLevel = r.getInt(5);
			_maxLevel = r.getInt(6);
			_premium = r.getInt(7) == 1;
			_voter = r.getInt(8) == 1;
			_useItem = r.getInt(9) == 1;
			_itemId = r.getInt(10);
			_itemCount = r.getInt(11);
			
			if (_itemCount == -1)
			{
				_itemCount = PowerPackConfig.BUFFER_PRICE;
			}
		}
		
		public boolean checkLevel(L2PcInstance player)
		{
			return (_minLevel == 0 || player.getLevel() >= _minLevel) && (_maxLevel == 0 || player.getLevel() <= _maxLevel);
		}
		
		public boolean checkPrice(L2PcInstance player)
		{
			return (_itemCount == 0 || player.getInventory().getAdena() >= _itemCount);
		}
	}
	
	private void BuffTableLoad()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = con.prepareStatement("select name,skill_id,skill_level,skill_force,char_min_level,char_max_level,premium,voter,useItem,itemId,itemCount,id from buff_templates");
			ResultSet rs = stm.executeQuery();
			while(rs.next())
			{
				if (_buffs.get(rs.getString(1)) == null)
				{
					_buffs.put(rs.getString(1), new ArrayList<Buff>());
				}
				
				ArrayList<Buff> a = _buffs.get(rs.getString(1));
				Buff new_buff = new Buff(rs);
				a.add(new_buff);
			}
			rs.close();
			stm.close();
			LOG.info("Buffer: Loaded " + _buffs.size() + " buff templates");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
			LOG.error("Error while loading buffs. Please, check buff_templates table.");
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void onServerShutdown()
	{
		if (PowerPackConfig.NPCBUFFER_STORE_SCHEMES)
		{
			clearDB();
			saveDataToDB();
		}
	}
	
	public void onPlayerLogin(final int playerId)
	{
		if (_schemesTable.get(playerId) == null)
		{
			LoadSchemes(playerId);
		}
	}
	
	public static class Scheme
	{
		public int _ownerId;
		public int _skillId;
		public int _skillLevel;
		public boolean _premium;
		public boolean _voter;
		public boolean _useItem;
		public int _itemId;
		public int _itemCount;
		public String _scheme;
		
		public Scheme(int ownerId, int skillId, int skillLevel, boolean premium, boolean voter, boolean useItem, int itemId, int itemCount, String scheme)
		{
			_ownerId = ownerId;
			_skillId = skillId;
			_skillLevel = skillLevel;
			_premium = premium;
			_voter = voter;
			_useItem = useItem;
			_itemId = itemId;
			_itemCount = itemCount;
			_scheme = scheme;
		}
		
		public Scheme(ResultSet r) throws SQLException
		{
			_ownerId = r.getInt(1);
			_skillId = r.getInt(2);
			_skillLevel = r.getInt(3);
			_premium = r.getInt(4) == 1;
			_voter = r.getInt(5) == 1;
			_useItem = r.getInt(6) == 1;
			_itemId = r.getInt(7);
			_itemCount = r.getInt(8);
			
			if (_itemCount == -1)
			{
				_itemCount = PowerPackConfig.BUFFER_PRICE;
			}
			
			_scheme = r.getString(9);
		}
	}
	
	private void LoadSchemes(final int objectId)
	{
		//int schemes = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = con.prepareStatement("SELECT * FROM buff_schemes WHERE ownerId=?");
			stm.setInt(1, objectId);
			final ResultSet rs = stm.executeQuery();
			
			final FastMap<String, ArrayList<Scheme>> map = new FastMap<>();
			
			while(rs.next())
			{
				final String scheme = rs.getString("scheme");
				
				if (!map.containsKey(scheme) && map.size() <= PowerPackConfig.NPCBUFFER_MAX_SCHEMES)
				{
					map.put(scheme, new ArrayList<Scheme>());
					//schemes++;
				}
				
				if (map.get(scheme) != null && map.get(scheme).size() < PowerPackConfig.NPCBUFFER_MAX_SKILLS)
				{
					map.get(scheme).add(new Scheme(rs));
				}
			}
			
			if (!map.isEmpty())
			{
				_schemesTable.put(objectId, map);
			}
			
			rs.close();
			stm.close();
			
			//LOG.info("Loaded: " + schemes + " schemes of players.");
		}
		catch(Exception e)
		{
			e.printStackTrace();

			LOG.error("Error while loading buffs scheme. Please, check buff_schemes table.");
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void clearDB()
	{
		if (_schemesTable.isEmpty())
		{
			return;
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			for (Entry<Integer, FastMap<String, ArrayList<Scheme>>> e = _schemesTable.head(), end = _schemesTable.tail(); (e = e.getNext()) != end;)
			{
				final PreparedStatement statement = con.prepareStatement(SQL_DELETE_SCHEME);
				statement.setInt(1, e.getKey());
				statement.execute();
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("BuffTable: Error while trying to delete schemess");
		}
		finally
		{
			CloseUtil.close(con);
			
		}
	}
	
	public void saveDataToDB()
	{
		if (_schemesTable.isEmpty())
		{
			return;
		}
		
		Connection con = null;
		int count = 0;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			for (Entry<Integer, FastMap<String, ArrayList<Scheme>>> e = _schemesTable.head(), end = _schemesTable.tail(); (e = e.getNext()) != end;)
			{
				// each profile
				if (e.getValue() == null || e.getValue().isEmpty())
				{
					continue;
				}
				
				for (Entry<String, ArrayList<Scheme>> a = e.getValue().head(), enda = e.getValue().tail(); (a = a.getNext()) != enda;)
				{
					if (a.getValue() == null || a.getValue().isEmpty())
					{
						continue;
					}
					
					// each skill
					for (final Scheme sk : a.getValue())
					{
						final PreparedStatement statement = con.prepareStatement(SQL_INSERT_SCHEME);
						statement.setInt(1, sk._ownerId);
						statement.setInt(2, sk._skillId);
						statement.setInt(3, sk._skillLevel);
						statement.setBoolean(4, sk._premium);
						statement.setBoolean(5, sk._voter);
						statement.setBoolean(6, sk._useItem);
						statement.setInt(7, sk._itemId);
						statement.setInt(8, sk._itemCount);
						statement.setString(9, sk._scheme);
						statement.execute();
					}
				}
				count++;
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("CharSchemesTable: Error while trying to delete schemes");
		}
		finally
		{
			CloseUtil.close(con);
			
			LOG.info("Char Scheme Table: Saved " + String.valueOf(count + " scheme(s)"));
		}
	}
	
	public void saveSchemeToDB(L2PcInstance player, String scheme, Buff buff)
	{
		int objId = player.getObjectId();
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement(SQL_INSERT_SCHEME);
			statement.setInt(1, objId);
			statement.setInt(2, buff._skillId);
			statement.setInt(3, buff._skillLevel);
			
			if (buff._premium)
			{
				statement.setInt(4, 1);
			}
			else
			{
				statement.setInt(4, 0);
			}
			
			if (buff._voter)
			{
				statement.setInt(5, 1);
			}
			else
			{
				statement.setInt(5, 0);
			}
			
			if (buff._useItem)
			{
				statement.setInt(6, 1);
			}
			else
			{
				statement.setInt(6, 0);
			}
			
			statement.setInt(7, buff._itemId);
			statement.setInt(8, buff._itemCount);
			
			statement.setString(9, scheme);
			statement.execute();
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("BuffTable: Error while trying to save scheme.");
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public ArrayList<Buff> getBuffsForName(String name)
	{
		ArrayList<Buff> output = new ArrayList<>();
		
		if ((name == null) || name.equals("all"))
		{
			for (ArrayList<Buff> actual:_buffs.values())
			{
				output.addAll(actual);
			}
		}
		else
		{
			if (_buffs.get(name) != null)
			{
				output = _buffs.get(name);
			}
		}
		return output;
	}
	
	public ArrayList<Scheme> getScheme(final int playerid, final String scheme_key)
	{
		if (_schemesTable.get(playerid) == null)
		{
			return null;
		}
		
		return _schemesTable.get(playerid).get(scheme_key);
	}
	
	public void setScheme(final int playerId, final String schemeKey, final ArrayList<Scheme> list)
	{
		_schemesTable.get(playerId).put(schemeKey, list);
	}
	
	public FastMap<String, ArrayList<Scheme>> getAllSchemes(final int playerId)
	{
		return _schemesTable.get(playerId);
	}
	
	public FastMap<Integer, FastMap<String, ArrayList<Scheme>>> getSchemesTable()
	{
		return _schemesTable;
	}
}

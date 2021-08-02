package l2jorion.game.powerpack.engrave;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.database.L2DatabaseFactory;

public class EngraveManager
{
	protected Connection _con = null;
	protected static final Logger LOG = LoggerFactory.getLogger(EngraveManager.class);
	private PreparedStatement LOG_UPDATE;
	private PreparedStatement LOG_DELETE;
	private PreparedStatement ITEM_INSERT;
	private PreparedStatement ITEM_DELETE;
	protected PreparedStatement LOG_READ;
	private PreparedStatement LOG_CLEANUP0;
	private PreparedStatement LOG_CLEANUP1;
	private PreparedStatement LOG_CLEANUP2;
	private final Map<Integer, int[]> _engravedItems;
	
	protected EngraveManager()
	{
		_engravedItems = new FastMap<>();
		try
		{
			_con = L2DatabaseFactory.getInstance().getConnection();
			LOG_UPDATE = _con.prepareStatement("insert into engraved_log values(?,?,?,?,?,?)");
			LOG_DELETE = _con.prepareStatement("delete from engraved_log where object_id=?");
			ITEM_INSERT = _con.prepareStatement("insert into engraved_items values (?,?,?)");
			ITEM_DELETE = _con.prepareStatement("delete from engraved_items where object_id=?");
			LOG_READ = _con.prepareStatement("select FROM_UNIXTIME(actiondate, '%d-%m %h:%i'),process,form_char,to_char from engravedLOGGER where object_id=? order by actiondate");
			LOG_CLEANUP0 = _con.prepareStatement("select object_id from engraved_items where engraver_id=? and " + "object_id not in (select object_id from items union all select object_id from itemsonground)");
			LOG_CLEANUP1 = _con.prepareStatement("delete from engraved_log where object_id in " + "(select object_id from engraved_items where engraver_id=? and " + "object_id not in (select object_id from items union all select object_id from itemsonground)) ");
			LOG_CLEANUP2 = _con.prepareStatement("delete from engraved_items where engraver_id=? and " + "object_id not in (select object_id from items union all select object_id from itemsonground)");
			PreparedStatement stm;
			stm = _con.prepareStatement("select object_id,engraver_id,item_id from engraved_items");
			final ResultSet rs = stm.executeQuery();
			while (rs.next())
			{
				_engravedItems.put(rs.getInt(1), new int[]
				{
					rs.getInt(2),
					rs.getInt(3)
				});
			}
			rs.close();
			stm.close();
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized (_con)
					{
						try
						{
							LOG_READ.setInt(1, 0);
							LOG_READ.execute();
						}
						catch (final Exception e)
						{
							if (Config.ENABLE_ALL_EXCEPTIONS)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}, 300000, 300000);
			new EngraveQuest();
			if (Config.EXTENDERS.get(L2ItemInstance.class.getName()) == null)
			{
				Config.EXTENDERS.put(L2ItemInstance.class.getName(), new FastList<String>());
			}
			if (!Config.EXTENDERS.get(L2ItemInstance.class.getName()).contains(EngraveExtender.class.getName()))
			{
				Config.EXTENDERS.get(L2ItemInstance.class.getName()).add(EngraveExtender.class.getName());
			}
			LOG.info("...Loaded " + _engravedItems.size() + " engraved items");
		}
		catch (final Exception e)
		{
			LOG.error("...Database error " + e);
			e.printStackTrace();
		}
		
	}
	
	public ArrayList<String> getLog(final int objectId)
	{
		final ArrayList<String> result = new ArrayList<>();
		synchronized (_con)
		{
			try
			{
				LOG_READ.setInt(1, objectId);
				final ResultSet rs = LOG_READ.executeQuery();
				while (rs.next())
				{
					result.add("<tr><td>" + rs.getString(1) + "</td><td>" + rs.getString(2) + "</td><td>" + rs.getString(3) + "</td><td>" + rs.getString(4) + "</td></tr>");
				}
				rs.close();
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("EngraveManager: Unable to store item LOGGER " + e);
			}
		}
		return result;
	}
	
	public void engraveItem(final L2ItemInstance item, final L2PcInstance engraver)
	{
		synchronized (_engravedItems)
		{
			_engravedItems.put(item.getObjectId(), new int[]
			{
				engraver.getObjectId(),
				item.getItemId()
			});
			synchronized (_con)
			{
				try
				{
					ITEM_INSERT.setInt(1, item.getObjectId());
					ITEM_INSERT.setInt(2, item.getItemId());
					ITEM_INSERT.setInt(3, engraver.getObjectId());
					ITEM_INSERT.execute();
					EngraveExtender ext = (EngraveExtender) item.getExtender(EngraveExtender.class.getSimpleName());
					if (ext == null)
					{
						ext = new EngraveExtender(item);
						item.addExtender(ext);
					}
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					LOG.warn("EngraveManager: Unable to store item LOGGER " + e);
				}
			}
		}
	}
	
	public boolean isEngraved(final int objectId)
	{
		synchronized (_engravedItems)
		{
			return _engravedItems.keySet().contains(objectId);
		}
	}
	
	public int getEngraver(final int objectId)
	{
		synchronized (_engravedItems)
		{
			if (_engravedItems.get(objectId) != null)
			{
				return _engravedItems.get(objectId)[0];
			}
			return 0;
		}
	}
	
	public int[] getItemInfo(final int objectId)
	{
		synchronized (_engravedItems)
		{
			return _engravedItems.get(objectId);
		}
	}
	
	public int getEngraver(final L2ItemInstance item)
	{
		return getEngraver(item.getObjectId());
	}
	
	public boolean isEngraved(final L2ItemInstance item)
	{
		return isEngraved(item.getObjectId());
	}
	
	public ArrayList<int[]> getMyEngravement(final L2PcInstance player)
	{
		synchronized (_engravedItems)
		{
			final ArrayList<int[]> result = new ArrayList<>();
			for (final int key : _engravedItems.keySet())
			{
				if (_engravedItems.get(key)[0] == player.getObjectId())
				{
					result.add(new int[]
					{
						key,
						_engravedItems.get(key)[1]
					});
				}
			}
			return result;
		}
	}
	
	public void removeEngravement(final int objectId)
	{
		synchronized (_engravedItems)
		{
			_engravedItems.remove(objectId);
			synchronized (_con)
			{
				try
				{
					LOG_DELETE.setInt(1, objectId);
					LOG_DELETE.execute();
					ITEM_DELETE.setInt(1, objectId);
					ITEM_DELETE.execute();
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					LOG.warn("EngraveManager: Unable to store item LOGGER " + e);
				}
			}
		}
	}
	
	public void cleanup(final int ownerId)
	{
		synchronized (_con)
		{
			try
			{
				LOG_CLEANUP0.setInt(1, ownerId);
				final ResultSet rs = LOG_CLEANUP0.executeQuery();
				while (rs.next())
				{
					_engravedItems.remove(rs.getInt(1));
				}
				rs.close();
				LOG_CLEANUP1.setInt(1, ownerId);
				LOG_CLEANUP1.execute();
				LOG_CLEANUP2.setInt(1, ownerId);
				LOG_CLEANUP2.execute();
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("EngraveManager: Unable to cleanup " + e);
			}
		}
	}
	
	public void removeEngravement(final L2ItemInstance item)
	{
		removeEngravement(item.getObjectId());
	}
	
	public void logAction(final L2ItemInstance item, final L2Character actor, final L2Character reference, final String process)
	{
		synchronized (_con)
		{
			try
			{
				LOG_UPDATE.setInt(1, item.getObjectId());
				LOG_UPDATE.setLong(2, System.currentTimeMillis() / 1000);
				LOG_UPDATE.setString(3, process);
				LOG_UPDATE.setString(4, item.getItemName());
				LOG_UPDATE.setString(5, actor == null ? "" : actor.getName());
				LOG_UPDATE.setString(6, reference == null ? "" : reference.getName());
				
				if ((actor != null) && (reference != null))
				{
					LOG_UPDATE.setString(5, actor.getName());
					LOG_UPDATE.setString(6, reference.getName());
				}
				else if ((reference != null) && (process.compareToIgnoreCase("pickup") == 0))
				{
					LOG_UPDATE.setString(6, reference.getName());
					LOG_UPDATE.setString(5, "");
				}
				else if ((actor != null) && (process.compareToIgnoreCase("drop") == 0))
				{
					LOG_UPDATE.setString(5, actor.getName());
					LOG_UPDATE.setString(6, "");
				}
				
				LOG_UPDATE.execute();
			}
			catch (final Exception e)
			{
				LOG.warn("EngraveManager: Unable to store item LOGGER " + e);
				e.printStackTrace();
			}
		}
	}
	
	public static EngraveManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EngraveManager _instance = new EngraveManager();
	}
}

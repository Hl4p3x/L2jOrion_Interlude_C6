package l2jorion.game.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.model.L2TradeList;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class TradeController
{
	private static Logger LOG = LoggerFactory.getLogger(TradeController.class);
	private static TradeController _instance;
	
	private int _nextListId;
	private final Map<Integer, L2TradeList> _lists;
	private final Map<Integer, L2TradeList> _listsTaskItem;
	
	/** Task launching the function for restore count of Item (Clan Hall) */
	public class RestoreCount implements Runnable
	{
		private final int _timer;
		
		public RestoreCount(final int time)
		{
			_timer = time;
		}
		
		@Override
		public void run()
		{
			try
			{
				restoreCount(_timer);
				dataTimerSave(_timer);
				ThreadPoolManager.getInstance().scheduleGeneral(new RestoreCount(_timer), (long) _timer * 60 * 60 * 1000);
			}
			catch (final Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
		}
	}
	
	public static TradeController getInstance()
	{
		if (_instance == null)
		{
			_instance = new TradeController();
		}
		return _instance;
	}
	
	private TradeController()
	{
		_lists = new HashMap<>();
		_listsTaskItem = new HashMap<>();
		final File buylistData = new File(Config.DATAPACK_ROOT, "data/buylists.csv");
		
		if (buylistData.exists())
		{
			LOG.warn("Do, please, remove buylists from data folder and use SQL buylist instead");
			String line = null;
			int dummyItemCount = 0;
			
			FileReader reader = null;
			BufferedReader buff = null;
			LineNumberReader lnr = null;
			
			try
			{
				reader = new FileReader(buylistData);
				buff = new BufferedReader(reader);
				lnr = new LineNumberReader(buff);
				
				while ((line = lnr.readLine()) != null)
				{
					if (line.trim().length() == 0 || line.startsWith("#"))
					{
						continue;
					}
					dummyItemCount += parseList(line);
				}
				
				if (Config.DEBUG)
				{
					LOG.debug("created " + dummyItemCount + " Dummy-Items for buylists");
				}
				
				LOG.info("TradeController: Loaded " + _lists.size() + " buy lists");
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("error while creating trade controller in line: " + (lnr == null ? 0 : lnr.getLineNumber()), e);
				
			}
			finally
			{
				
				if (lnr != null)
				{
					try
					{
						lnr.close();
					}
					catch (final Exception e1)
					{
						e1.printStackTrace();
					}
				}
				
				if (buff != null)
				{
					try
					{
						buff.close();
					}
					catch (final Exception e1)
					{
						e1.printStackTrace();
					}
				}
				
				if (reader != null)
				{
					try
					{
						reader.close();
					}
					catch (final Exception e1)
					{
						e1.printStackTrace();
					}
				}
				
			}
			
		}
		else
		{
			LOG.debug("No buylists were found in data folder, using SQL buylist instead");
			Connection con = null;
			
			int dummyItemCount = 0;
			boolean LimitedItem = false;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement1 = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
				{
					"shop_id",
					"npc_id"
				}) + " FROM merchant_shopids");
				
				ResultSet rset1 = statement1.executeQuery();
				
				while (rset1.next())
				{
					PreparedStatement statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
					{
						"item_id",
						"price",
						"shop_id",
						"order",
						"count",
						"time",
						"currentCount"
					}) + " FROM merchant_buylists WHERE shop_id=? ORDER BY " + L2DatabaseFactory.getInstance().safetyString(new String[]
					{
						"order"
					}) + " ASC");
					
					statement.setString(1, String.valueOf(rset1.getInt("shop_id")));
					ResultSet rset = statement.executeQuery();
					if (rset.next())
					{
						LimitedItem = false;
						dummyItemCount++;
						L2TradeList buy1 = new L2TradeList(rset1.getInt("shop_id"));
						
						int itemId = rset.getInt("item_id");
						int price = rset.getInt("price");
						int count = rset.getInt("count");
						int currentCount = rset.getInt("currentCount");
						int time = rset.getInt("time");
						
						L2ItemInstance item = ItemTable.getInstance().createDummyItem(itemId);
						
						if (item == null)
						{
							DatabaseUtils.close(rset);
							DatabaseUtils.close(statement);
							
							rset = null;
							statement = null;
							continue;
						}
						
						if (count > -1)
						{
							item.setCountDecrease(true);
							LimitedItem = true;
						}
						
						if (!rset1.getString("npc_id").equals("gm") && price < (item.getReferencePrice() / 2))
						{
							LOG.warn("TradeList:" + buy1.getListId() + " itemId: " + itemId + " has an ADENA sell price lower than reference price. Automatically updating it...");
							price = item.getReferencePrice();
						}
						
						item.setPriceToSell(price);
						item.setTime(time);
						item.setInitCount(count);
						
						if (currentCount > -1)
						{
							item.setCount(currentCount);
						}
						else
						{
							item.setCount(count);
						}
						
						buy1.addItem(item);
						item = null;
						buy1.setNpcId(rset1.getString("npc_id"));
						
						try
						{
							while (rset.next())
							{
								dummyItemCount++;
								itemId = rset.getInt("item_id");
								price = rset.getInt("price");
								count = rset.getInt("count");
								time = rset.getInt("time");
								currentCount = rset.getInt("currentCount");
								final L2ItemInstance item2 = ItemTable.getInstance().createDummyItem(itemId);
								
								if (item2 == null)
								{
									continue;
								}
								
								if (count > -1)
								{
									item2.setCountDecrease(true);
									LimitedItem = true;
								}
								
								if (!rset1.getString("npc_id").equals("gm") && price < item2.getReferencePrice() / 2)
								{
									
									LOG.warn("L2TradeList " + buy1.getListId() + " itemId  " + itemId + " has an ADENA sell price lower then reference price.. Automatically Updating it..");
									price = item2.getReferencePrice();
								}
								
								item2.setPriceToSell(price);
								
								item2.setTime(time);
								item2.setInitCount(count);
								if (currentCount > -1)
								{
									item2.setCount(currentCount);
								}
								else
								{
									item2.setCount(count);
								}
								buy1.addItem(item2);
							}
						}
						catch (final Exception e)
						{
							if (Config.ENABLE_ALL_EXCEPTIONS)
							{
								e.printStackTrace();
							}
							
							LOG.warn("TradeController: Problem with buylist " + buy1.getListId() + " item " + itemId);
						}
						
						if (LimitedItem)
						{
							_listsTaskItem.put(buy1.getListId(), buy1);
						}
						else
						{
							_lists.put(buy1.getListId(), buy1);
						}
						
						_nextListId = Math.max(_nextListId, buy1.getListId() + 1);
					}
					
					DatabaseUtils.close(rset);
					DatabaseUtils.close(statement);
				}
				rset1.close();
				statement1.close();
				
				if (Config.DEBUG)
				{
					LOG.debug("created " + dummyItemCount + " Dummy-Items for buylists");
				}
				
				LOG.info("TradeController: Loaded " + _lists.size() + " buy lists");
				LOG.info("TradeController: Loaded " + _listsTaskItem.size() + " limited buy lists");
				
				try
				{
					int time = 0;
					long savetimer = 0;
					final long currentMillis = System.currentTimeMillis();
					
					PreparedStatement statement2 = con.prepareStatement("SELECT DISTINCT time, savetimer FROM merchant_buylists WHERE time <> 0 ORDER BY time");
					ResultSet rset2 = statement2.executeQuery();
					
					while (rset2.next())
					{
						time = rset2.getInt("time");
						savetimer = rset2.getLong("savetimer");
						if (savetimer - currentMillis > 0)
						{
							ThreadPoolManager.getInstance().scheduleGeneral(new RestoreCount(time), savetimer - System.currentTimeMillis());
						}
						else
						{
							ThreadPoolManager.getInstance().scheduleGeneral(new RestoreCount(time), 0);
						}
					}
					rset2.close();
					statement2.close();
				}
				catch (final Exception e)
				{
					LOG.warn("TradeController: Could not restore Timer for Item count.");
					e.printStackTrace();
				}
			}
			catch (final Exception e)
			{
				// problem with initializing spawn, go to next one
				LOG.warn("TradeController: Buylists could not be initialized.");
				e.printStackTrace();
			}
			finally
			{
				CloseUtil.close(con);
			}
			
			if (Config.CUSTOM_MERCHANT_TABLES)
			{
				try
				{
					final int initialSize = _lists.size();
					con = L2DatabaseFactory.getInstance().getConnection();
					
					PreparedStatement statement1 = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
					{
						"shop_id",
						"npc_id"
					}) + " FROM custom_merchant_shopids");
					
					ResultSet rset1 = statement1.executeQuery();
					
					while (rset1.next())
					{
						PreparedStatement statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
						{
							"item_id",
							"price",
							"shop_id",
							"order",
							"count",
							"time",
							"currentCount"
						}) + " FROM custom_merchant_buylists WHERE shop_id=? ORDER BY " + L2DatabaseFactory.getInstance().safetyString(new String[]
						{
							"order"
						}) + " ASC");
						
						statement.setString(1, String.valueOf(rset1.getInt("shop_id")));
						ResultSet rset = statement.executeQuery();
						
						if (rset.next())
						{
							LimitedItem = false;
							dummyItemCount++;
							L2TradeList buy1 = new L2TradeList(rset1.getInt("shop_id"));
							int itemId = rset.getInt("item_id");
							int price = rset.getInt("price");
							int count = rset.getInt("count");
							int currentCount = rset.getInt("currentCount");
							int time = rset.getInt("time");
							L2ItemInstance item = ItemTable.getInstance().createDummyItem(itemId);
							if (item == null)
							{
								DatabaseUtils.close(rset);
								DatabaseUtils.close(statement);
								
								rset = null;
								statement = null;
								continue;
							}
							
							if (count > -1)
							{
								item.setCountDecrease(true);
								LimitedItem = true;
							}
							
							if (!rset1.getString("npc_id").equals("gm") && price < (item.getReferencePrice() / 2))
							{
								LOG.warn("TradeList:" + buy1.getListId() + " itemId: " + itemId + " has an ADENA sell price lower than reference price. Automatically updating it...");
								price = item.getReferencePrice();
							}
							
							item.setPriceToSell(price);
							item.setTime(time);
							item.setInitCount(count);
							
							if (currentCount > -1)
							{
								item.setCount(currentCount);
							}
							else
							{
								item.setCount(count);
							}
							
							buy1.addItem(item);
							item = null;
							buy1.setNpcId(rset1.getString("npc_id"));
							
							try
							{
								while (rset.next())
								{
									dummyItemCount++;
									itemId = rset.getInt("item_id");
									price = rset.getInt("price");
									count = rset.getInt("count");
									time = rset.getInt("time");
									currentCount = rset.getInt("currentCount");
									L2ItemInstance item2 = ItemTable.getInstance().createDummyItem(itemId);
									if (item2 == null)
									{
										continue;
									}
									if (count > -1)
									{
										item2.setCountDecrease(true);
										LimitedItem = true;
									}
									
									if (!rset1.getString("npc_id").equals("gm") && price < item2.getReferencePrice() / 2)
									{
										LOG.warn("TradeList:" + buy1.getListId() + " itemId: " + itemId + " has an ADENA sell price lower than reference price. Automatically updating it...");
										price = item2.getReferencePrice();
									}
									
									item2.setPriceToSell(price);
									item2.setTime(time);
									item2.setInitCount(count);
									if (currentCount > -1)
									{
										item2.setCount(currentCount);
									}
									else
									{
										item2.setCount(count);
									}
									buy1.addItem(item2);
									
									item2 = null;
								}
							}
							catch (final Exception e)
							{
								if (Config.ENABLE_ALL_EXCEPTIONS)
								{
									e.printStackTrace();
								}
								
								LOG.warn("TradeController: Problem with buylist " + buy1.getListId() + " item " + itemId);
							}
							if (LimitedItem)
							{
								_listsTaskItem.put(buy1.getListId(), buy1);
							}
							else
							{
								_lists.put(buy1.getListId(), buy1);
							}
							_nextListId = Math.max(_nextListId, buy1.getListId() + 1);
							
							buy1 = null;
						}
						
						DatabaseUtils.close(rset);
						DatabaseUtils.close(statement);
					}
					rset1.close();
					statement1.close();
					
					if (Config.DEBUG)
					{
						LOG.debug("created " + dummyItemCount + " Dummy-Items for buylists");
					}
					
					int list = (_lists.size() - initialSize);
					if (list > 0)
					{
						LOG.info("TradeController: Loaded " + list + " custom buy lists");
					}
					
					/**
					 * Restore Task for reinitialyze count of buy item
					 */
					try
					{
						int time = 0;
						long savetimer = 0;
						final long currentMillis = System.currentTimeMillis();
						
						PreparedStatement statement2 = con.prepareStatement("SELECT DISTINCT time, savetimer FROM custom_merchant_buylists WHERE time <> 0 ORDER BY time");
						ResultSet rset2 = statement2.executeQuery();
						
						while (rset2.next())
						{
							time = rset2.getInt("time");
							savetimer = rset2.getLong("savetimer");
							if (savetimer - currentMillis > 0)
							{
								ThreadPoolManager.getInstance().scheduleGeneral(new RestoreCount(time), savetimer - System.currentTimeMillis());
							}
							else
							{
								ThreadPoolManager.getInstance().scheduleGeneral(new RestoreCount(time), 0);
							}
						}
						rset2.close();
						statement2.close();
						
						rset2 = null;
						statement2 = null;
						
					}
					catch (Exception e)
					{
						LOG.warn("TradeController: Could not restore Timer for Item count.");
						e.printStackTrace();
					}
				}
				catch (final Exception e)
				{
					// problem with initializing spawn, go to next one
					LOG.warn("TradeController: Buylists could not be initialized.");
					e.printStackTrace();
				}
				finally
				{
					CloseUtil.close(con);
					con = null;
				}
			}
		}
	}
	
	private int parseList(final String line)
	{
		int itemCreated = 0;
		StringTokenizer st = new StringTokenizer(line, ";");
		
		final int listId = Integer.parseInt(st.nextToken());
		L2TradeList buy1 = new L2TradeList(listId);
		while (st.hasMoreTokens())
		{
			final int itemId = Integer.parseInt(st.nextToken());
			int price = Integer.parseInt(st.nextToken());
			final L2ItemInstance item = ItemTable.getInstance().createDummyItem(itemId);
			
			if (price < (item.getReferencePrice() / 2))
			{
				LOG.warn("TradeList:" + listId + " itemId:" + itemId + " has an ADENA sell price lower than reference price. Automatically ipdating it...");
				price = item.getReferencePrice();
			}
			
			item.setPriceToSell(price);
			buy1.addItem(item);
			itemCreated++;
		}
		
		_lists.put(Integer.valueOf(buy1.getListId()), buy1);
		
		return itemCreated;
	}
	
	public L2TradeList getBuyList(final int listId)
	{
		if (_lists.get(Integer.valueOf(listId)) != null)
		{
			return _lists.get(Integer.valueOf(listId));
		}
		
		return _listsTaskItem.get(Integer.valueOf(listId));
	}
	
	public List<L2TradeList> getBuyListByNpcId(final int npcId)
	{
		final List<L2TradeList> lists = new ArrayList<>();
		for (final L2TradeList list : _lists.values())
		{
			if (list.getNpcId().startsWith("gm"))
			{
				continue;
			}
			
			if (list.getNpcId().startsWith("shop"))
			{
				continue;
			}
			
			if (npcId == Integer.parseInt(list.getNpcId()))
			{
				lists.add(list);
			}
		}
		for (final L2TradeList list : _listsTaskItem.values())
		{
			if (list.getNpcId().startsWith("gm"))
			{
				continue;
			}
			
			if (list.getNpcId().startsWith("shop"))
			{
				continue;
			}
			
			if (npcId == Integer.parseInt(list.getNpcId()))
			{
				lists.add(list);
			}
		}
		return lists;
	}
	
	public List<L2TradeList> getBuyListById(String npcId)
	{
		final List<L2TradeList> lists = new ArrayList<>();
		for (final L2TradeList list : _lists.values())
		{
			if (list.getNpcId().startsWith("gm"))
			{
				continue;
			}
			
			if (list.getNpcId().startsWith("shop"))
			{
				lists.add(list);
			}
			
			if (npcId.contains(list.getNpcId()))
			{
				lists.add(list);
			}
		}
		for (final L2TradeList list : _listsTaskItem.values())
		{
			if (list.getNpcId().startsWith("gm"))
			{
				continue;
			}
			
			if (list.getNpcId().startsWith("shop"))
			{
				lists.add(list);
			}
			
			if (npcId.contains(list.getNpcId()))
			{
				lists.add(list);
			}
		}
		return lists;
	}
	
	protected void restoreCount(final int time)
	{
		if (_listsTaskItem == null)
		{
			return;
		}
		
		for (final L2TradeList list : _listsTaskItem.values())
		{
			list.restoreCount(time);
		}
	}
	
	protected void dataTimerSave(final int time)
	{
		final long timerSave = System.currentTimeMillis() + (time * 60 * 60 * 1000);
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("UPDATE merchant_buylists SET savetimer =? WHERE time =?");
			statement.setLong(1, timerSave);
			statement.setInt(2, time);
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			LOG.warn("TradeController: Could not update Timer save in Buylist");
		}
	}
	
	public void dataCountStore()
	{
		int listId;
		if (_listsTaskItem == null)
		{
			return;
		}
		
		PreparedStatement statement;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			for (L2TradeList list : _listsTaskItem.values())
			{
				if (list == null)
				{
					continue;
				}
				
				listId = list.getListId();
				
				for (final L2ItemInstance Item : list.getItems())
				{
					if (Item.getCount() < Item.getInitCount()) // needed?
					{
						statement = con.prepareStatement("UPDATE merchant_buylists SET currentCount=? WHERE item_id=? AND shop_id=?");
						statement.setInt(1, Item.getCount());
						statement.setInt(2, Item.getItemId());
						statement.setInt(3, listId);
						statement.executeUpdate();
						statement.close();
					}
				}
			}
		}
		catch (final Exception e)
		{
			LOG.warn("TradeController: Could not store Count Item");
		}
	}
	
	/**
	 * @return
	 */
	public synchronized int getNextId()
	{
		return _nextListId++;
	}
	
	/**
	 * This will reload buylists info from DataBase
	 */
	public static void reload()
	{
		_instance = new TradeController();
	}
}

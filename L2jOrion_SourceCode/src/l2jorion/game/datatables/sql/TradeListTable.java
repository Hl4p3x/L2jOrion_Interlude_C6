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
package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.game.model.L2TradeList;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class TradeListTable
{
	private static final Logger LOG = LoggerFactory.getLogger(TradeListTable.class);
	private static TradeListTable _instance;
	
	private int _nextListId;
	private final FastMap<Integer, L2TradeList> _lists;
	
	/** Task launching the function for restore count of Item (Clan Hall) */
	private class RestoreCount implements Runnable
	{
		private final int timer;
		
		public RestoreCount(final int time)
		{
			timer = time;
		}
		
		@Override
		public void run()
		{
			restoreCount(timer);
			dataTimerSave(timer);
			ThreadPoolManager.getInstance().scheduleGeneral(new RestoreCount(timer), (long) timer * 60 * 60 * 1000);
		}
	}
	
	public static TradeListTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new TradeListTable();
		}
		
		return _instance;
	}
	
	private TradeListTable()
	{
		_lists = new FastMap<>();
		load();
	}
	
	private void load(final boolean custom)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement1 = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
			{
				"shop_id",
				"npc_id"
			}) + " FROM " + (custom ? "custom_merchant_shopids" : "merchant_shopids"));
			final ResultSet rset1 = statement1.executeQuery();
			
			while (rset1.next())
			{
				final PreparedStatement statement = con.prepareStatement("SELECT item_id, price, shop_id, order, count, time, currentCount FROM " + (custom ? "custom_merchant_buylists" : "merchant_buylists") + " WHERE shop_id=? ORDER BY order ASC");
				statement.setString(1, String.valueOf(rset1.getInt("shop_id")));
				final ResultSet rset = statement.executeQuery();
				
				final L2TradeList buylist = new L2TradeList(rset1.getInt("shop_id"));
				buylist.setNpcId(rset1.getString("npc_id"));
				
				int _itemId = 0;
				int _itemCount = 0;
				int _price = 0;
				
				if (!buylist.isGm() && NpcTable.getInstance().getTemplate(rset1.getInt("npc_id")) == null)
				{
					LOG.warn("TradeListTable: Merchant id {} with {} buylist {} not exist. " + rset1.getString("npc_id") + " " + buylist.getListId());
				}
				
				try
				{
					while (rset.next())
					{
						_itemId = rset.getInt("item_id");
						_price = rset.getInt("price");
						
						final int count = rset.getInt("count");
						final int currentCount = rset.getInt("currentCount");
						final int time = rset.getInt("time");
						
						final L2ItemInstance buyItem = ItemTable.getInstance().createDummyItem(_itemId);
						
						if (buyItem == null)
						{
							continue;
						}
						
						_itemCount++;
						
						if (count > -1)
						{
							buyItem.setCountDecrease(true);
						}
						buyItem.setPriceToSell(_price);
						buyItem.setTime(time);
						buyItem.setInitCount(count);
						
						if (currentCount > -1)
						{
							buyItem.setCount(currentCount);
						}
						else
						{
							buyItem.setCount(count);
						}
						
						buylist.addItem(buyItem);
						
						if (!buylist.isGm() && buyItem.getReferencePrice() > _price)
						{
							LOG.warn("TradeListTable: Reference price of item {} in buylist {} higher then sell price. " + _itemId + " " + buylist.getListId());
						}
					}
				}
				catch (final Exception e)
				{
					LOG.error("TradeListTable: Problem with buylist {}. " + buylist.getListId(), e);
				}
				
				if (_itemCount > 0)
				{
					_lists.put(buylist.getListId(), buylist);
					_nextListId = Math.max(_nextListId, buylist.getListId() + 1);
				}
				else
				{
					LOG.warn("TradeListTable: Empty buylist {}." + buylist.getListId());
				}
				
				DatabaseUtils.close(statement);
				DatabaseUtils.close(rset);
			}
			rset1.close();
			statement1.close();
			
			LOG.info("TradeListTable: Loaded {} Buylists. " + _lists.size());
			/*
			 * Restore Task for reinitialize count of buy item
			 */
			try
			{
				int time = 0;
				long savetimer = 0;
				final long currentMillis = System.currentTimeMillis();
				final PreparedStatement statement2 = con.prepareStatement("SELECT DISTINCT time, savetimer FROM " + (custom ? "merchant_buylists" : "merchant_buylists") + " WHERE time <> 0 ORDER BY time");
				final ResultSet rset2 = statement2.executeQuery();
				
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
				LOG.error("TradeController: Could not restore Timer for Item count. ", e);
			}
		}
		catch (final Exception e)
		{
			// problem with initializing buylists, go to next one
			LOG.error("TradeListTable: Buylists could not be initialized. ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void load()
	{
		load(false); // not custom
		load(true); // custom
	}
	
	public void reloadAll()
	{
		_lists.clear();
		
		load();
	}
	
	public L2TradeList getBuyList(final int listId)
	{
		if (_lists.containsKey(listId))
			return _lists.get(listId);
		
		return null;
	}
	
	public FastList<L2TradeList> getBuyListByNpcId(final int npcId)
	{
		final FastList<L2TradeList> lists = new FastList<>();
		
		for (final L2TradeList list : _lists.values())
		{
			if (list.isGm())
			{
				continue;
			}
			/** if (npcId == list.getNpcId()) **/
			lists.add(list);
		}
		
		return lists;
	}
	
	protected void restoreCount(final int time)
	{
		if (_lists == null)
			return;
		
		for (final L2TradeList list : _lists.values())
		{
			list.restoreCount(time);
		}
	}
	
	protected void dataTimerSave(final int time)
	{
		Connection con = null;
		final long timerSave = System.currentTimeMillis() + (long) time * 3600000; // 60*60*1000
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("UPDATE merchant_buylists SET savetimer =? WHERE time =?");
			statement.setLong(1, timerSave);
			statement.setInt(2, time);
			statement.executeUpdate();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("TradeController: Could not update Timer save in Buylist ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void dataCountStore()
	{
		if (_lists == null)
			return;
		
		int listId;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			for (final L2TradeList list : _lists.values())
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
						DatabaseUtils.close(statement);
					}
				}
			}
		}
		catch (final Exception e)
		{
			LOG.error("TradeController: Could not store Count Item ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
}

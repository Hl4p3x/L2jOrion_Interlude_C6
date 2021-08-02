/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.game.model.L2ItemMarketModel;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class ItemMarketTable
{
	private static ItemMarketTable _instance = null;
	
	private Map<Integer, String> _itemIcons = null;
	private Map<Integer, List<L2ItemMarketModel>> _marketItems = null;
	private Map<Integer, Integer> _allItems = new FastMap<>();
	
	private List<L2ItemMarketModel> _latestItems = new FastList<>();
	
	public static Logger LOG = LoggerFactory.getLogger(ItemMarketTable.class);
	
	private ItemMarketTable()
	{
	}
	
	public static ItemMarketTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new ItemMarketTable();
		}
		
		return _instance;
	}
	
	public void load()
	{
		_marketItems = new FastMap<>();
		
		Connection con = null;
		int mrktCount = 0;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Select * From market_items Order By ownerId");
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				int ownerId = rset.getInt("ownerId");
				String ownerName = rset.getString("ownerName");
				int itemObjId = rset.getInt("itemObjId");
				int itemId = rset.getInt("itemId");
				String itemName = rset.getString("itemName");
				String itemType = rset.getString("itemType");
				String l2Type = rset.getString("shopType");
				int itemGrade = rset.getInt("itemGrade");
				int enchLvl = rset.getInt("enchLvl");
				int count = rset.getInt("count");
				int priceItem = rset.getInt("priceItem");
				int price = rset.getInt("price");
				int augmentationId = rset.getInt("augmentationId");
				int augmentationSkill = rset.getInt("augmentationSkill");
				int augmentationSkillLevel = rset.getInt("augmentationSkillLevel");
				String augmentationBonus = rset.getString("augmentationBonus");
				
				L2ItemMarketModel mrktItem = new L2ItemMarketModel();
				mrktItem.setOwnerId(ownerId);
				mrktItem.setOwnerName(ownerName);
				mrktItem.setItemObjId(itemObjId);
				mrktItem.setItemId(itemId);
				mrktItem.setItemName(itemName);
				mrktItem.setItemType(itemType);
				mrktItem.setL2Type(l2Type);
				mrktItem.setItemGrade(itemGrade);
				mrktItem.setEnchLvl(enchLvl);
				mrktItem.setCount(count);
				mrktItem.setPriceItem(priceItem);
				mrktItem.setPrice(price);
				mrktItem.setAugmentationId(augmentationId);
				mrktItem.setAugmentationSkill(augmentationSkill);
				mrktItem.setAugmentationSkillLevel(augmentationSkillLevel);
				mrktItem.setAugmentationBonus(augmentationBonus);
				
				List<L2ItemMarketModel> list = null;
				
				if (_marketItems.containsKey(ownerId))
				{
					list = _marketItems.get(ownerId);
					list.add(mrktItem);
					_marketItems.put(ownerId, list);
				}
				else
				{
					list = new FastList<>();
					list.add(mrktItem);
					_marketItems.put(ownerId, list);
				}
				
				_latestItems.add(mrktItem);
				_allItems.put(itemObjId, ownerId);
				
				mrktCount++;
			}
			
			if (mrktCount > 0)
			{
				LOG.info("Market: Loaded " + mrktCount + " items");
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			LOG.warn("Error while loading market items " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		loadIcons();
	}
	
	private void loadIcons()
	{
		_itemIcons = new FastMap<>();
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Select * From market_icons");
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				int itemId = rset.getInt("itemId");
				String itemIcon = rset.getString("itemIcon");
				_itemIcons.put(itemId, itemIcon);
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			LOG.warn("Error while loading market icons " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void addItemToMarket(L2ItemMarketModel itemToMarket, L2PcInstance owner)
	{
		synchronized (this)
		{
			if (_marketItems != null && owner != null && itemToMarket != null)
			{
				List<L2ItemMarketModel> list = _marketItems.get(owner.getObjectId());
				if (list != null)
				{
					list.add(itemToMarket);
					_marketItems.put(owner.getObjectId(), list);
				}
				else
				{
					list = new FastList<>();
					list.add(itemToMarket);
					_marketItems.put(owner.getObjectId(), list);
				}
				
				_latestItems.add(itemToMarket);
				_allItems.put(itemToMarket.getItemObjId(), owner.getObjectId());
				
				ThreadPoolManager.getInstance().scheduleGeneral(new SaveTask(itemToMarket), 500);
			}
		}
	}
	
	public void removeItemFromMarket(int ownerId, int itemObjId, int count)
	{
		L2ItemMarketModel mrktItem = getItem(itemObjId);
		List<L2ItemMarketModel> list = getItemsByOwnerId(ownerId);
		synchronized (this)
		{
			if (list != null && mrktItem != null && !list.isEmpty())
			{
				if (mrktItem.getCount() == count)
				{
					list.remove(mrktItem);
					_marketItems.put(ownerId, list);
					_allItems.remove(itemObjId);
					ThreadPoolManager.getInstance().scheduleGeneral(new DeleteTask(mrktItem), 500);
				}
				else
				{
					list.remove(mrktItem);
					mrktItem.setCount(mrktItem.getCount() - count);
					list.add(mrktItem);
					_marketItems.put(ownerId, list);
					_allItems.remove(itemObjId);
					ThreadPoolManager.getInstance().scheduleGeneral(new UpdateTask(mrktItem), 500);
				}
				
				_latestItems.remove(mrktItem);
			}
		}
	}
	
	public List<L2ItemMarketModel> getItemsByOwnerId(int ownerId)
	{
		synchronized (this)
		{
			if (_marketItems != null && !_marketItems.isEmpty())
			{
				return _marketItems.get(ownerId);
			}
		}
		return null;
	}
	
	public L2ItemMarketModel getItem(int itemObjId)
	{
		List<L2ItemMarketModel> list = getAllItems();
		synchronized (this)
		{
			for (L2ItemMarketModel model : list)
			{
				if (model.getItemObjId() == itemObjId)
				{
					return model;
				}
			}
		}
		return null;
	}
	
	public List<L2ItemMarketModel> getAllItems()
	{
		synchronized (this)
		{
			if (_marketItems != null && !_marketItems.isEmpty())
			{
				List<L2ItemMarketModel> list = new FastList<>();
				
				for (List<L2ItemMarketModel> lst : _marketItems.values())
				{
					if (lst != null && !lst.isEmpty())
					{
						for (L2ItemMarketModel auctItem : lst)
						{
							if (auctItem != null)
							{
								list.add(auctItem);
							}
						}
					}
				}
				
				return list;
			}
		}
		return null;
	}
	
	public List<L2ItemMarketModel> getLatest()
	{
		if (_latestItems != null && !_latestItems.isEmpty())
		{
			return _latestItems;
		}
		return null;
	}
	
	public List<L2ItemMarketModel> getSearchItems(String name)
	{
		synchronized (this)
		{
			if (_marketItems != null && !_marketItems.isEmpty())
			{
				List<L2ItemMarketModel> searchList = new FastList<>();
				for (List<L2ItemMarketModel> lst : _marketItems.values())
				{
					if (lst != null && !lst.isEmpty())
					{
						for (L2ItemMarketModel auctItem : lst)
						{
							if (auctItem.getItemName().toLowerCase().contains(name.toLowerCase()))
							{
								searchList.add(auctItem);
							}
						}
					}
				}
				return searchList;
			}
		}
		return null;
	}
	
	public String getItemIcon(int itemId)
	{
		if (_itemIcons != null && !_itemIcons.isEmpty())
		{
			return _itemIcons.get(itemId);
		}
		return null;
	}
	
	private static class SaveTask implements Runnable
	{
		private final L2ItemMarketModel _marketItem;
		
		public SaveTask(L2ItemMarketModel marketItem)
		{
			this._marketItem = marketItem;
		}
		
		@Override
		public void run()
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("Insert Into market_items Values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
				statement.setInt(1, _marketItem.getOwnerId());
				statement.setString(2, _marketItem.getOwnerName());
				statement.setString(3, _marketItem.getItemName());
				statement.setInt(4, _marketItem.getEnchLvl());
				statement.setInt(5, _marketItem.getItemGrade());
				statement.setString(6, _marketItem.getL2Type());
				statement.setString(7, _marketItem.getItemType());
				statement.setInt(8, _marketItem.getItemId());
				statement.setInt(9, _marketItem.getItemObjId());
				statement.setInt(10, _marketItem.getCount());
				statement.setInt(11, _marketItem.getPriceItem());
				statement.setInt(12, _marketItem.getPrice());
				statement.setInt(13, _marketItem.getAugmentationId());
				statement.setInt(14, _marketItem.getAugmentationSkill());
				statement.setInt(15, _marketItem.getAugmentationSkillLevel());
				statement.setString(16, _marketItem.getAugmentationBonus());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				LOG.warn("Error while saving market item into DB " + e.getMessage());
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	private static class DeleteTask implements Runnable
	{
		
		private final L2ItemMarketModel _marketItem;
		
		public DeleteTask(L2ItemMarketModel marketItem)
		{
			this._marketItem = marketItem;
		}
		
		@Override
		public void run()
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("Delete From market_items Where ownerId = ? AND itemObjId = ?");
				statement.setInt(1, _marketItem.getOwnerId());
				statement.setInt(2, _marketItem.getItemObjId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				LOG.warn("Error while deleting market item from DB " + e.getMessage());
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	private static class UpdateTask implements Runnable
	{
		private final L2ItemMarketModel _marketItem;
		
		public UpdateTask(L2ItemMarketModel marketItem)
		{
			this._marketItem = marketItem;
		}
		
		@Override
		public void run()
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("Update market_items Set _count = ? Where itemObjId = ? AND ownerId = ?");
				statement.setInt(1, _marketItem.getCount());
				statement.setInt(2, _marketItem.getItemObjId());
				statement.setInt(3, _marketItem.getOwnerId());
				statement.executeUpdate();
				statement.close();
			}
			catch (Exception e)
			{
				LOG.warn("Error while updating market item in DB " + e.getMessage());
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	public int getSellersCount()
	{
		return _marketItems.size();
	}
	
	public int getMarketItemsCount()
	{
		return _allItems.size();
	}
	
	public int getYourItemsCount(int objId)
	{
		if (_marketItems.get(objId) == null)
		{
			return 0;
		}
		return _marketItems.get(objId).size();
	}
}
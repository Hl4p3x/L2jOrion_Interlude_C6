package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2jorion.game.model.L2ItemMarketModel;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class ItemMarketTable
{
	public static Logger LOG = LoggerFactory.getLogger(ItemMarketTable.class);
	
	private static ItemMarketTable _instance = null;
	private int loadedItemsNumber = 0;
	private int lastItemId = 0;
	
	private Map<Integer, List<L2ItemMarketModel>> _marketItems = null;
	private Map<Integer, Integer> _allItems = new HashMap<>();
	
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
		_marketItems = new HashMap<>();
		int maxNumber = 0;
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM market_items ORDER BY ownerId");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				int id = rset.getInt("id");
				int ownerId = rset.getInt("ownerId");
				String ownerName = rset.getString("ownerName");
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
				mrktItem.setId(id);
				mrktItem.setOwnerId(ownerId);
				mrktItem.setOwnerName(ownerName);
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
				
				if (id > maxNumber)
				{
					maxNumber = id;
					setLastItemId(maxNumber);
				}
				
				List<L2ItemMarketModel> list = null;
				
				if (_marketItems.containsKey(ownerId))
				{
					list = _marketItems.get(ownerId);
					list.add(mrktItem);
					_marketItems.put(ownerId, list);
				}
				else
				{
					list = new ArrayList<>();
					list.add(mrktItem);
					_marketItems.put(ownerId, list);
				}
				
				_allItems.put(id, ownerId);
				
				loadedItemsNumber++;
			}
			
			if (loadedItemsNumber > 0)
			{
				LOG.info("Market: Loaded " + loadedItemsNumber + " items");
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
					list = new ArrayList<>();
					list.add(itemToMarket);
					_marketItems.put(owner.getObjectId(), list);
				}
				
				_allItems.put(itemToMarket.getId(), owner.getObjectId());
				
				ThreadPoolManager.getInstance().scheduleGeneral(new SaveTask(itemToMarket), 500);
			}
		}
	}
	
	public void removeItemFromMarket(int ownerId, int id, int count)
	{
		L2ItemMarketModel mrktItem = getItem(id);
		List<L2ItemMarketModel> list = getItemsByOwnerId(ownerId);
		synchronized (this)
		{
			if (list != null && mrktItem != null && !list.isEmpty())
			{
				if (mrktItem.getCount() == count)
				{
					list.remove(mrktItem);
					_marketItems.put(ownerId, list);
					_allItems.remove(id);
					ThreadPoolManager.getInstance().scheduleGeneral(new DeleteTask(mrktItem), 500);
				}
				else
				{
					list.remove(mrktItem);
					mrktItem.setCount(mrktItem.getCount() - count);
					list.add(mrktItem);
					_marketItems.put(ownerId, list);
					_allItems.remove(id);
					ThreadPoolManager.getInstance().scheduleGeneral(new UpdateTask(mrktItem), 500);
				}
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
	
	public L2ItemMarketModel getItem(int Id)
	{
		List<L2ItemMarketModel> list = getAllItems();
		synchronized (this)
		{
			for (L2ItemMarketModel model : list)
			{
				if (model.getId() == Id)
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
				List<L2ItemMarketModel> list = new ArrayList<>();
				
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
	
	public List<L2ItemMarketModel> getSearchItems(String name)
	{
		synchronized (this)
		{
			if (_marketItems != null && !_marketItems.isEmpty())
			{
				List<L2ItemMarketModel> searchList = new ArrayList<>();
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
				PreparedStatement statement = con.prepareStatement("INSERT INTO market_items Values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
				statement.setInt(1, _marketItem.getId());
				statement.setInt(2, _marketItem.getOwnerId());
				statement.setString(3, _marketItem.getOwnerName());
				statement.setString(4, _marketItem.getItemName());
				statement.setInt(5, _marketItem.getEnchLvl());
				statement.setInt(6, _marketItem.getItemGrade());
				statement.setString(7, _marketItem.getL2Type());
				statement.setString(8, _marketItem.getItemType());
				statement.setInt(9, _marketItem.getItemId());
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
				PreparedStatement statement = con.prepareStatement("DELETE FROM market_items WHERE ownerId = ? AND id = ?");
				statement.setInt(1, _marketItem.getOwnerId());
				statement.setInt(2, _marketItem.getId());
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
				PreparedStatement statement = con.prepareStatement("UPDATE market_items SET _count = ? WHERE id = ? AND ownerId = ?");
				statement.setInt(1, _marketItem.getCount());
				statement.setInt(2, _marketItem.getId());
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
	
	public void setLastItemId(int id)
	{
		lastItemId = id;
	}
	
	public int getLastItemId()
	{
		return lastItemId;
	}
}
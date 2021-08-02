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
package l2jorion.game.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.AuctionManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class Auction
{
	protected static final Logger LOG = LoggerFactory.getLogger(Auction.class);
	
	private int _id = 0;
	
	private final int _adenaId = 57;
	
	private long _endDate;
	
	private int _highestBidderId = 0;
	
	private String _highestBidderName = "";
	
	private int _highestBidderMaxBid = 0;
	
	private int _itemId = 0;
	
	private String _itemName = "";
	
	private int _itemObjectId = 0;
	
	private final int _itemQuantity = 0;
	
	private String _itemType = "";
	
	private int _sellerId = 0;
	
	private String _sellerClanName = "";
	
	private String _sellerName = "";
	
	private int _currentBid = 0;
	
	private int _startingBid = 0;
	
	public static final long MAX_ADENA = 99900000000L;
	
	private final Map<Integer, Bidder> _bidders = new FastMap<>();
	
	private static final String[] ItemTypeName =
	{
		"ClanHall"
	};
	
	public static enum ItemTypeEnum
	{
		ClanHall
	}
	
	public class Bidder
	{
		
		/** The _name. */
		private final String _name;
		
		/** The _clan name. */
		private final String _clanName;
		
		/** The _bid. */
		private int _bid;
		
		/** The _time bid. */
		private final Calendar _timeBid;
		
		/**
		 * Instantiates a new bidder.
		 * @param name the name
		 * @param clanName the clan name
		 * @param bid the bid
		 * @param timeBid the time bid
		 */
		public Bidder(final String name, final String clanName, final int bid, final long timeBid)
		{
			_name = name;
			_clanName = clanName;
			_bid = bid;
			_timeBid = Calendar.getInstance();
			_timeBid.setTimeInMillis(timeBid);
		}
		
		/**
		 * Gets the name.
		 * @return the name
		 */
		public String getName()
		{
			return _name;
		}
		
		/**
		 * Gets the clan name.
		 * @return the clan name
		 */
		public String getClanName()
		{
			return _clanName;
		}
		
		/**
		 * Gets the bid.
		 * @return the bid
		 */
		public int getBid()
		{
			return _bid;
		}
		
		/**
		 * Gets the time bid.
		 * @return the time bid
		 */
		public Calendar getTimeBid()
		{
			return _timeBid;
		}
		
		/**
		 * Sets the time bid.
		 * @param timeBid the new time bid
		 */
		public void setTimeBid(final long timeBid)
		{
			_timeBid.setTimeInMillis(timeBid);
		}
		
		/**
		 * Sets the bid.
		 * @param bid the new bid
		 */
		public void setBid(final int bid)
		{
			_bid = bid;
		}
	}
	
	/**
	 * Task Sheduler for endAuction.
	 */
	public class AutoEndTask implements Runnable
	{
		
		/**
		 * Instantiates a new auto end task.
		 */
		public AutoEndTask()
		{
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			try
			{
				endAuction();
			}
			catch (final Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					t.printStackTrace();
			}
		}
	}
	
	/**
	 * Constructor.
	 * @param auctionId the auction id
	 */
	
	public Auction(final int auctionId)
	{
		_id = auctionId;
		load();
		startAutoTask();
	}
	
	/**
	 * Instantiates a new auction.
	 * @param itemId the item id
	 * @param Clan the clan
	 * @param delay the delay
	 * @param bid the bid
	 * @param name the name
	 */
	public Auction(final int itemId, final L2Clan Clan, final long delay, final int bid, final String name)
	{
		_id = itemId;
		_endDate = System.currentTimeMillis() + delay;
		_itemId = itemId;
		_itemName = name;
		_itemType = "ClanHall";
		_sellerId = Clan.getLeaderId();
		_sellerName = Clan.getLeaderName();
		_sellerClanName = Clan.getName();
		_startingBid = bid;
	}
	
	/**
	 * Load auctions.
	 */
	private void load()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("Select * from auction where id = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				_currentBid = rs.getInt("currentBid");
				_endDate = rs.getLong("endDate");
				_itemId = rs.getInt("itemId");
				_itemName = rs.getString("itemName");
				_itemObjectId = rs.getInt("itemObjectId");
				_itemType = rs.getString("itemType");
				_sellerId = rs.getInt("sellerId");
				_sellerClanName = rs.getString("sellerClanName");
				_sellerName = rs.getString("sellerName");
				_startingBid = rs.getInt("startingBid");
			}
			
			rs.close();
			DatabaseUtils.close(statement);
			loadBid();
			statement = null;
			rs = null;
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Load bidders *.
	 */
	private void loadBid()
	{
		_highestBidderId = 0;
		_highestBidderName = "";
		_highestBidderMaxBid = 0;
		
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("SELECT bidderId, bidderName, maxBid, clan_name, time_bid FROM auction_bid WHERE auctionId = ? ORDER BY maxBid DESC");
			statement.setInt(1, getId());
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				if (rs.isFirst())
				{
					_highestBidderId = rs.getInt("bidderId");
					_highestBidderName = rs.getString("bidderName");
					_highestBidderMaxBid = rs.getInt("maxBid");
				}
				_bidders.put(rs.getInt("bidderId"), new Bidder(rs.getString("bidderName"), rs.getString("clan_name"), rs.getInt("maxBid"), rs.getLong("time_bid")));
			}
			
			rs.close();
			DatabaseUtils.close(statement);
			statement = null;
			rs = null;
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Task Manage.
	 */
	private void startAutoTask()
	{
		final long currentTime = System.currentTimeMillis();
		long taskDelay = 0;
		
		if (_endDate <= currentTime)
		{
			_endDate = currentTime + 7 * 24 * 60 * 60 * 1000;
			saveAuctionDate();
		}
		else
		{
			taskDelay = _endDate - currentTime;
		}
		
		ThreadPoolManager.getInstance().scheduleGeneral(new AutoEndTask(), taskDelay);
	}
	
	/**
	 * Gets the item type name.
	 * @param value the value
	 * @return the item type name
	 */
	public static String getItemTypeName(final ItemTypeEnum value)
	{
		return ItemTypeName[value.ordinal()];
	}
	
	/**
	 * Save Auction Data End.
	 */
	private void saveAuctionDate()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Update auction set endDate = ? where id = ?");
			statement.setLong(1, _endDate);
			statement.setInt(2, _id);
			statement.execute();
			
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.error("Exception: saveAuctionDate(): " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Set a bid.
	 * @param bidder the bidder
	 * @param bid the bid
	 */
	public synchronized void setBid(final L2PcInstance bidder, final int bid)
	{
		int requiredAdena = bid;
		
		if (getHighestBidderName().equals(bidder.getClan().getLeaderName()))
		{
			requiredAdena = bid - getHighestBidderMaxBid();
		}
		
		if ((getHighestBidderId() > 0 && bid > getHighestBidderMaxBid()) || (getHighestBidderId() == 0 && bid >= getStartingBid()))
		{
			if (takeItem(bidder, requiredAdena))
			{
				updateInDB(bidder, bid);
				bidder.getClan().setAuctionBiddedAt(_id, true);
				return;
			}
		}
		if ((bid < getStartingBid()) || (bid <= getHighestBidderMaxBid()))
			bidder.sendMessage("Bid Price must be higher");
	}
	
	/**
	 * Return Item in WHC.
	 * @param Clan the clan
	 * @param quantity the quantity
	 * @param penalty the penalty
	 */
	private void returnItem(final String Clan, int quantity, final boolean penalty)
	{
		if (penalty)
		{
			quantity *= 0.9; // take 10% tax fee if needed
		}
		
		// avoid overflow on return
		final long limit = MAX_ADENA - ClanTable.getInstance().getClanByName(Clan).getWarehouse().getAdena();
		quantity = (int) Math.min(quantity, limit);
		
		ClanTable.getInstance().getClanByName(Clan).getWarehouse().addItem("Outbidded", _adenaId, quantity, null, null);
	}
	
	/**
	 * Take Item in WHC.
	 * @param bidder the bidder
	 * @param quantity the quantity
	 * @return true, if successful
	 */
	private boolean takeItem(final L2PcInstance bidder, final int quantity)
	{
		if (bidder.getClan() != null && bidder.getClan().getWarehouse().getAdena() >= quantity)
		{
			bidder.getClan().getWarehouse().destroyItemByItemId("Buy", _adenaId, quantity, bidder, bidder);
			return true;
		}
		bidder.sendMessage("You do not have enough adena");
		return false;
	}
	
	/**
	 * Update auction in DB.
	 * @param bidder the bidder
	 * @param bid the bid
	 */
	private void updateInDB(final L2PcInstance bidder, final int bid)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			if (getBidders().get(bidder.getClanId()) != null)
			{
				statement = con.prepareStatement("UPDATE auction_bid SET bidderId=?, bidderName=?, maxBid=?, time_bid=? WHERE auctionId=? AND bidderId=?");
				statement.setInt(1, bidder.getClanId());
				statement.setString(2, bidder.getClan().getLeaderName());
				statement.setInt(3, bid);
				statement.setLong(4, System.currentTimeMillis());
				statement.setInt(5, getId());
				statement.setInt(6, bidder.getClanId());
				statement.execute();
				DatabaseUtils.close(statement);
				statement = null;
			}
			else
			{
				statement = con.prepareStatement("INSERT INTO auction_bid (id, auctionId, bidderId, bidderName, maxBid, clan_name, time_bid) VALUES (?, ?, ?, ?, ?, ?, ?)");
				statement.setInt(1, IdFactory.getInstance().getNextId());
				statement.setInt(2, getId());
				statement.setInt(3, bidder.getClanId());
				statement.setString(4, bidder.getName());
				statement.setInt(5, bid);
				statement.setString(6, bidder.getClan().getName());
				statement.setLong(7, System.currentTimeMillis());
				statement.execute();
				DatabaseUtils.close(statement);
				statement = null;
				
				if (L2World.getInstance().getPlayer(_highestBidderName) != null)
				{
					L2World.getInstance().getPlayer(_highestBidderName).sendMessage("You have been out bidded");
				}
			}
			_highestBidderId = bidder.getClanId();
			_highestBidderMaxBid = bid;
			_highestBidderName = bidder.getClan().getLeaderName();
			
			if (_bidders.get(_highestBidderId) == null)
			{
				_bidders.put(_highestBidderId, new Bidder(_highestBidderName, bidder.getClan().getName(), bid, Calendar.getInstance().getTimeInMillis()));
			}
			else
			{
				_bidders.get(_highestBidderId).setBid(bid);
				_bidders.get(_highestBidderId).setTimeBid(Calendar.getInstance().getTimeInMillis());
			}
			
			bidder.sendMessage("You have bidded successfully");
		}
		catch (final Exception e)
		{
			LOG.error("Exception: Auction.updateInDB(L2PcInstance bidder, int bid): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Remove bids.
	 */
	private void removeBids()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			statement = con.prepareStatement("DELETE FROM auction_bid WHERE auctionId=?");
			statement.setInt(1, getId());
			statement.execute();
			
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.error("Exception: Auction.deleteFromDB(): " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		for (final Bidder b : _bidders.values())
		{
			if (ClanTable.getInstance().getClanByName(b.getClanName()).getHasHideout() == 0)
				returnItem(b.getClanName(), b.getBid(), true); // 10 % tax
			else
			{
				if (L2World.getInstance().getPlayer(b.getName()) != null)
				{
					L2World.getInstance().getPlayer(b.getName()).sendMessage("Congratulation you have won ClanHall!");
				}
			}
			ClanTable.getInstance().getClanByName(b.getClanName()).setAuctionBiddedAt(0, true);
		}
		_bidders.clear();
	}
	
	/**
	 * Remove auctions.
	 */
	public void deleteAuctionFromDB()
	{
		AuctionManager.getInstance().getAuctions().remove(this);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM auction WHERE itemId=?");
			statement.setInt(1, _itemId);
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.error("Exception: Auction.deleteFromDB(): " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * End of auction.
	 */
	public void endAuction()
	{
		ClanHallManager.getInstance();
		if (ClanHallManager.loaded())
		{
			if (_highestBidderId == 0 && _sellerId == 0)
			{
				startAutoTask();
				return;
			}
			
			if (_highestBidderId == 0 && _sellerId > 0)
			{
				/**
				 * If seller haven't sell ClanHall, auction removed, THIS MUST BE CONFIRMED
				 */
				final int aucId = AuctionManager.getInstance().getAuctionIndex(_id);
				AuctionManager.getInstance().getAuctions().remove(aucId);
				
				return;
			}
			
			if (_sellerId > 0)
			{
				returnItem(_sellerClanName, _highestBidderMaxBid, true);
				returnItem(_sellerClanName, ClanHallManager.getInstance().getClanHallById(_itemId).getLease(), false);
			}
			
			deleteAuctionFromDB();
			L2Clan Clan = ClanTable.getInstance().getClanByName(_bidders.get(_highestBidderId).getClanName());
			_bidders.remove(_highestBidderId);
			Clan.setAuctionBiddedAt(0, true);
			removeBids();
			ClanHallManager.getInstance().setOwner(_itemId, Clan);
			Clan = null;
		}
		else
		{
			/** Task waiting ClanHallManager is loaded every 3s */
			ThreadPoolManager.getInstance().scheduleGeneral(new AutoEndTask(), 3000);
		}
	}
	
	/**
	 * Cancel bid.
	 * @param bidder the bidder
	 */
	public synchronized void cancelBid(final int bidder)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			statement = con.prepareStatement("DELETE FROM auction_bid WHERE auctionId=? AND bidderId=?");
			statement.setInt(1, getId());
			statement.setInt(2, bidder);
			statement.execute();
			
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.error("Exception: Auction.cancelBid(String bidder): " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		returnItem(_bidders.get(bidder).getClanName(), _bidders.get(bidder).getBid(), true);
		ClanTable.getInstance().getClanByName(_bidders.get(bidder).getClanName()).setAuctionBiddedAt(0, true);
		_bidders.clear();
		loadBid();
	}
	
	/**
	 * Cancel auction.
	 */
	public void cancelAuction()
	{
		deleteAuctionFromDB();
		removeBids();
	}
	
	/**
	 * Confirm an auction.
	 */
	public void confirmAuction()
	{
		AuctionManager.getInstance().getAuctions().add(this);
		Connection con = null;
		try
		{
			PreparedStatement statement;
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("INSERT INTO auction (id, sellerId, sellerName, sellerClanName, itemType, itemId, itemObjectId, itemName, itemQuantity, startingBid, currentBid, endDate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, getId());
			statement.setInt(2, _sellerId);
			statement.setString(3, _sellerName);
			statement.setString(4, _sellerClanName);
			statement.setString(5, _itemType);
			statement.setInt(6, _itemId);
			statement.setInt(7, _itemObjectId);
			statement.setString(8, _itemName);
			statement.setInt(9, _itemQuantity);
			statement.setInt(10, _startingBid);
			statement.setInt(11, _currentBid);
			statement.setLong(12, _endDate);
			statement.execute();
			DatabaseUtils.close(statement);
			loadBid();
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.error("Exception: Auction.load(): " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Get var auction.
	 * @return the id
	 */
	public final int getId()
	{
		return _id;
	}
	
	/**
	 * Gets the current bid.
	 * @return the current bid
	 */
	public final int getCurrentBid()
	{
		return _currentBid;
	}
	
	/**
	 * Gets the end date.
	 * @return the end date
	 */
	public final long getEndDate()
	{
		return _endDate;
	}
	
	/**
	 * Gets the highest bidder id.
	 * @return the highest bidder id
	 */
	public final int getHighestBidderId()
	{
		return _highestBidderId;
	}
	
	/**
	 * Gets the highest bidder name.
	 * @return the highest bidder name
	 */
	public final String getHighestBidderName()
	{
		return _highestBidderName;
	}
	
	/**
	 * Gets the highest bidder max bid.
	 * @return the highest bidder max bid
	 */
	public final int getHighestBidderMaxBid()
	{
		return _highestBidderMaxBid;
	}
	
	/**
	 * Gets the item id.
	 * @return the item id
	 */
	public final int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Gets the item name.
	 * @return the item name
	 */
	public final String getItemName()
	{
		return _itemName;
	}
	
	/**
	 * Gets the item object id.
	 * @return the item object id
	 */
	public final int getItemObjectId()
	{
		return _itemObjectId;
	}
	
	/**
	 * Gets the item quantity.
	 * @return the item quantity
	 */
	public final int getItemQuantity()
	{
		return _itemQuantity;
	}
	
	/**
	 * Gets the item type.
	 * @return the item type
	 */
	public final String getItemType()
	{
		return _itemType;
	}
	
	/**
	 * Gets the seller id.
	 * @return the seller id
	 */
	public final int getSellerId()
	{
		return _sellerId;
	}
	
	/**
	 * Gets the seller name.
	 * @return the seller name
	 */
	public final String getSellerName()
	{
		return _sellerName;
	}
	
	/**
	 * Gets the seller clan name.
	 * @return the seller clan name
	 */
	public final String getSellerClanName()
	{
		return _sellerClanName;
	}
	
	/**
	 * Gets the starting bid.
	 * @return the starting bid
	 */
	public final int getStartingBid()
	{
		return _startingBid;
	}
	
	/**
	 * Gets the bidders.
	 * @return the bidders
	 */
	public final Map<Integer, Bidder> getBidders()
	{
		return _bidders;
	}
}

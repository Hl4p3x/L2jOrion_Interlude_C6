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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.managers.AuctionManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.type.L2ClanHallZone;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.PledgeShowInfoUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.StatsSet;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class ClanHall
{
	protected static final Logger LOG = LoggerFactory.getLogger(ClanHall.class);
	
	private final int _clanHallId;
	private ArrayList<L2DoorInstance> _doors;
	private String _name;
	private int _ownerId;
	private L2Clan _ownerClan;
	private int _lease;
	private String _desc;
	private String _location;
	protected long _paidUntil;
	private L2ClanHallZone _zone;
	private int _grade;
	private final int _week = 604800000;
	protected boolean _isFree = true;
	private Map<Integer, ClanHallFunction> _functions;
	protected boolean _paid;
	
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_ITEM_CREATE = 2;
	public static final int FUNC_RESTORE_HP = 3;
	public static final int FUNC_RESTORE_MP = 4;
	public static final int FUNC_RESTORE_EXP = 5;
	public static final int FUNC_SUPPORT = 6;
	public static final int FUNC_DECO_FRONTPLATEFORM = 7;
	public static final int FUNC_DECO_CURTAINS = 8;
	
	public class ClanHallFunction
	{
		private final int _type;
		private int _lvl;
		protected int _fee;
		protected int _tempFee;
		private final long _rate;
		private long _endDate;
		protected boolean _inDebt;
		
		public ClanHallFunction(final int type, final int lvl, final int lease, final int tempLease, final long rate, final long time)
		{
			_type = type;
			_lvl = lvl;
			_fee = lease;
			_tempFee = tempLease;
			_rate = rate;
			_endDate = time;
			initializeTask();
		}
		
		public int getType()
		{
			return _type;
		}
		
		public int getLvl()
		{
			return _lvl;
		}
		
		public int getLease()
		{
			return _fee;
		}
		
		public long getRate()
		{
			return _rate;
		}
		
		public long getEndTime()
		{
			return _endDate;
		}
		
		public void setLvl(final int lvl)
		{
			_lvl = lvl;
		}
		
		public void setLease(final int lease)
		{
			_fee = lease;
		}
		
		public void setEndTime(final long time)
		{
			_endDate = time;
		}
		
		private void initializeTask()
		{
			if (_isFree)
			{
				return;
			}
			
			final long currentTime = System.currentTimeMillis();
			
			if (_endDate > currentTime)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(), _endDate - currentTime);
			}
			else
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(), 0);
			}
		}
		
		private class FunctionTask implements Runnable
		{
			public FunctionTask()
			{
			}
			
			@Override
			public void run()
			{
				try
				{
					if (_isFree)
					{
						return;
					}
					
					if (getOwnerClan().getWarehouse().getAdena() >= _fee)
					{
						int fee = _fee;
						boolean newfc = true;
						
						if (getEndTime() == 0 || getEndTime() == -1)
						{
							if (getEndTime() == -1)
							{
								newfc = false;
								fee = _tempFee;
							}
						}
						else
						{
							newfc = false;
						}
						
						setEndTime(System.currentTimeMillis() + getRate());
						dbSave(newfc);
						
						getOwnerClan().getWarehouse().destroyItemByItemId("CH_function_fee", 57, fee, null, null);
						
						ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(), getRate());
					}
					else
					{
						removeFunction(getType());
					}
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
		
		public void dbSave(final boolean newFunction)
		{
			Connection con = null;
			try
			{
				PreparedStatement statement;
				
				con = L2DatabaseFactory.getInstance().getConnection();
				if (newFunction)
				{
					statement = con.prepareStatement("INSERT INTO clanhall_functions (hall_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)");
					statement.setInt(1, getClanHallId());
					statement.setInt(2, getType());
					statement.setInt(3, getLvl());
					statement.setInt(4, getLease());
					statement.setLong(5, getRate());
					statement.setLong(6, getEndTime());
				}
				else
				{
					statement = con.prepareStatement("UPDATE clanhall_functions SET lvl=?, lease=?, endTime=? WHERE hall_id=? AND type=?");
					statement.setInt(1, getLvl());
					statement.setInt(2, getLease());
					statement.setLong(3, getEndTime());
					statement.setInt(4, getClanHallId());
					statement.setInt(5, getType());
				}
				statement.execute();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("Exception: ClanHall.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + e.getMessage());
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	public ClanHall(StatsSet set)
	{
		_clanHallId = set.getInteger("id");
		_name = set.getString("name");
		_ownerId = set.getInteger("ownerId");
		_desc = set.getString("desc");
		_location = set.getString("location");
		_grade = set.getInteger("grade");
		
		if (!isSiegableHall())
		{
			_paidUntil = set.getLong("paidUntil");
			_paid = set.getBool("paid");
			_lease = set.getInteger("lease");
		}
		
		_functions = new FastMap<>();
		
		if (_ownerId != 0)
		{
			L2Clan clan = ClanTable.getInstance().getClan(_ownerId);
			if (clan != null)
			{
				clan.setHasHideout(getClanHallId());
			}
			
			_isFree = false;
			
			initialyzeTask(false);
			loadFunctions();
		}
	}
	
	public final boolean getPaid()
	{
		return _paid;
	}
	
	public final int getClanHallId()
	{
		return _clanHallId;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final int getOwnerId()
	{
		return _ownerId;
	}
	
	public final int getLease()
	{
		return _lease;
	}
	
	public final String getDesc()
	{
		return _desc;
	}
	
	public final String getLocation()
	{
		return _location;
	}
	
	public final long getPaidUntil()
	{
		return _paidUntil;
	}
	
	public int getGrade()
	{
		return _grade;
	}
	
	public final ArrayList<L2DoorInstance> getDoors()
	{
		if (_doors == null)
		{
			_doors = new ArrayList<>();
		}
		return _doors;
	}
	
	public final L2DoorInstance getDoor(int doorId)
	{
		if (doorId <= 0)
		{
			return null;
		}
		
		for (L2DoorInstance door : getDoors())
		{
			if (door.getDoorId() == doorId)
			{
				return door;
			}
		}
		
		return null;
	}
	
	public ClanHallFunction getFunction(final int type)
	{
		if (_functions.get(type) != null)
		{
			return _functions.get(type);
		}
		
		return null;
	}
	
	public void setZone(L2ClanHallZone zone)
	{
		_zone = zone;
	}
	
	public L2ClanHallZone getZone()
	{
		return _zone;
	}
	
	public void free()
	{
		_ownerId = 0;
		_isFree = true;
		
		for (final Map.Entry<Integer, ClanHallFunction> fc : _functions.entrySet())
		{
			removeFunction(fc.getKey());
		}
		
		_functions.clear();
		_paidUntil = 0;
		_paid = false;
		updateDb();
	}
	
	public void setOwner(final L2Clan clan)
	{
		// Verify that this ClanHall is Free and Clan isn't null
		if (_ownerId > 0 || clan == null)
		{
			return;
		}
		
		_ownerId = clan.getClanId();
		_isFree = false;
		_paidUntil = System.currentTimeMillis();
		initialyzeTask(true);
		
		// Announce to Online member new ClanHall
		clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		
		updateDb();
	}
	
	public L2Clan getOwnerClan()
	{
		if (_ownerId == 0)
		{
			return null;
		}
		
		if (_ownerClan == null)
		{
			_ownerClan = ClanTable.getInstance().getClan(getOwnerId());
		}
		
		return _ownerClan;
	}
	
	public void openCloseDoors(final L2PcInstance activeChar, final boolean open)
	{
		if (activeChar != null && activeChar.getClanId() == getOwnerId())
		{
			openCloseDoors(open);
		}
	}
	
	public void openCloseDoors(final boolean open)
	{
		for (final L2DoorInstance door : getDoors())
		{
			if (door != null)
			{
				if (open)
				{
					door.openMe();
				}
				else
				{
					door.closeMe();
				}
			}
		}
	}
	
	public void banishForeigners()
	{
		if (_zone != null)
		{
			_zone.banishForeigners(getOwnerId());
		}
		else
		{
			LOG.error(getClass().getSimpleName() + ": Zone is null for clan hall: " + getClanHallId() + " " + getName());
		}
	}
	
	protected void loadFunctions()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Select * from clanhall_functions where hall_id = ?");
			statement.setInt(1, getClanHallId());
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				_functions.put(rs.getInt("type"), new ClanHallFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime")));
			}
			
			rs.close();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("Exception: ClanHall.loadFunctions(): " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void removeFunction(final int functionType)
	{
		_functions.remove(functionType);
		Connection con = null;
		try
		{
			PreparedStatement statement;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM clanhall_functions WHERE hall_id=? AND type=?");
			statement.setInt(1, getClanHallId());
			statement.setInt(2, functionType);
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("Exception: ClanHall.removeFunctions(int functionType): " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean updateFunctions(final int type, final int lvl, final int lease, final long rate, final boolean addNew)
	{
		if (addNew)
		{
			if (ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() < lease)
			{
				return false;
			}
			_functions.put(type, new ClanHallFunction(type, lvl, lease, 0, rate, 0));
		}
		else
		{
			if (lvl == 0 && lease == 0)
			{
				removeFunction(type);
			}
			else
			{
				final int diffLease = lease - _functions.get(type).getLease();
				
				if (Config.DEBUG)
				{
					LOG.warn("Called ClanHall.updateFunctions diffLease : " + diffLease);
				}
				
				if (diffLease > 0)
				{
					if (ClanTable.getInstance().getClan(_ownerId).getWarehouse().getAdena() < diffLease)
					{
						return false;
					}
					
					_functions.remove(type);
					_functions.put(type, new ClanHallFunction(type, lvl, lease, diffLease, rate, -1));
				}
				else
				{
					_functions.get(type).setLease(lease);
					_functions.get(type).setLvl(lvl);
					_functions.get(type).dbSave(false);
				}
			}
		}
		return true;
	}
	
	public void updateDb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			statement = con.prepareStatement("UPDATE clanhall SET ownerId=?, paidUntil=?, paid=? WHERE id=?");
			statement.setInt(1, _ownerId);
			statement.setLong(2, _paidUntil);
			statement.setInt(3, _paid ? 1 : 0);
			statement.setInt(4, _clanHallId);
			statement.execute();
			DatabaseUtils.close(statement);
		}
		catch (final SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private final void initialyzeTask(final boolean forced)
	{
		long currentTime = System.currentTimeMillis();
		if (_paidUntil > currentTime)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), _paidUntil - currentTime);
		}
		else if (!_paid && !forced)
		{
			long timeLeft = (_paidUntil + (3600000 * 24)) - currentTime;
			ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), timeLeft);
		}
		else
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), 0);
		}
	}
	
	protected class FeeTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				long _time = System.currentTimeMillis();
				
				if (_isFree)
				{
					return;
				}
				
				if (_paidUntil > _time)
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), _paidUntil - _time);
					return;
				}
				
				L2Clan Clan = ClanTable.getInstance().getClan(getOwnerId());
				
				if (!Config.L2LIMIT_CUSTOM)
				{
					if (ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() >= getLease())
					{
						if (_paidUntil != 0)
						{
							while (_paidUntil <= _time)
							{
								_paidUntil += _week;
							}
						}
						else
						{
							_paidUntil = _time + _week;
						}
						
						ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId("CH_rental_fee", 57, getLease(), null, null);
						ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), _paidUntil - _time);
						_paid = true;
						updateDb();
					}
					else
					{
						_paid = false;
						if (_time > (_paidUntil + (3600000 * 24)))
						{
							if (ClanHallManager.loaded())
							{
								AuctionManager.getInstance().initNPC(getClanHallId());
								ClanHallManager.getInstance().setFree(getClanHallId());
								Clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.THE_CLAN_HALL_FEE_IS_ONE_WEEK_OVERDUE_THEREFORE_THE_CLAN_HALL_OWNERSHIP_HAS_BEEN_REVOKED));
							}
							else
							{
								ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), 3000);
							}
						}
						else
						{
							updateDb();
							SystemMessage sm = new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
							sm.addNumber(getLease());
							Clan.broadcastToOnlineMembers(sm);
							
							long timeLeft = (_paidUntil + (3600000 * 24)) - _time;
							ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), timeLeft);
						}
					}
				}
				else
				{
					if (ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() >= getLease() && ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAA() >= 40000000)
					{
						if (_paidUntil != 0)
						{
							while (_paidUntil < _time)
							{
								_paidUntil += _week;
							}
						}
						else
						{
							_paidUntil = _time + _week;
						}
						
						ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId("CH_rental_fee", 57, getLease(), null, null);
						ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId("CH_rental_fee", 5575, getLease(), null, null);
						
						ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), _paidUntil - _time);
						_paid = true;
						updateDb();
					}
					else
					{
						_paid = false;
						if (_time > (_paidUntil + (3600000 * 24)))
						{
							if (ClanHallManager.loaded())
							{
								AuctionManager.getInstance().initNPC(getClanHallId());
								ClanHallManager.getInstance().setFree(getClanHallId());
								Clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.THE_CLAN_HALL_FEE_IS_ONE_WEEK_OVERDUE_THEREFORE_THE_CLAN_HALL_OWNERSHIP_HAS_BEEN_REVOKED));
							}
							else
							{
								ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), 3000);
							}
						}
						else
						{
							updateDb();
							SystemMessage sm = new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
							sm.addNumber(getLease());
							Clan.broadcastToOnlineMembers(sm);
							
							long timeLeft = (_paidUntil + (3600000 * 24)) - _time;
							ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), timeLeft);
						}
					}
				}
				
			}
			catch (final Exception t)
			{
				LOG.info("owner:" + getOwnerId());
				t.printStackTrace();
			}
		}
	}
	
	public void openCloseDoor(int doorId, boolean open)
	{
		openCloseDoor(getDoor(doorId), open);
	}
	
	public void openCloseDoor(L2DoorInstance door, boolean open)
	{
		if (door != null)
		{
			if (open)
			{
				door.openMe();
			}
			else
			{
				door.closeMe();
			}
		}
	}
	
	public boolean isSiegableHall()
	{
		return false;
	}
}

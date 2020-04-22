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
package l2jorion.game.model.entity.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.csv.DoorTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.CastleManorManager;
import l2jorion.game.managers.CrownManager;
import l2jorion.game.managers.CastleManorManager.CropProcure;
import l2jorion.game.managers.CastleManorManager.SeedProduction;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Manor;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.model.zone.type.L2CastleTeleportZone;
import l2jorion.game.model.zone.type.L2CastleZone;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.PledgeShowInfoUpdate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.updaters.CastleUpdater;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Castle
{
	protected static Logger LOG = LoggerFactory.getLogger(Castle.class);
	
	// =========================================================
	// Data Field
	private FastList<CropProcure> _procure = new FastList<>();
	private FastList<SeedProduction> _production = new FastList<>();
	private FastList<CropProcure> _procureNext = new FastList<>();
	private FastList<SeedProduction> _productionNext = new FastList<>();
	private boolean _isNextPeriodApproved = false;
	
	private static final String CASTLE_MANOR_DELETE_PRODUCTION = "DELETE FROM castle_manor_production WHERE castle_id=?;";
	
	private static final String CASTLE_MANOR_DELETE_PRODUCTION_PERIOD = "DELETE FROM castle_manor_production WHERE castle_id=? AND period=?;";
	
	private static final String CASTLE_MANOR_DELETE_PROCURE = "DELETE FROM castle_manor_procure WHERE castle_id=?;";
	
	private static final String CASTLE_MANOR_DELETE_PROCURE_PERIOD = "DELETE FROM castle_manor_procure WHERE castle_id=? AND period=?;";
	
	private static final String CASTLE_UPDATE_CROP = "UPDATE castle_manor_procure SET can_buy=? WHERE crop_id=? AND castle_id=? AND period=?";
	
	private static final String CASTLE_UPDATE_SEED = "UPDATE castle_manor_production SET can_produce=? WHERE seed_id=? AND castle_id=? AND period=?";
	
	// =========================================================
	// Data Field
	private int _castleId = 0;
	private final List<L2DoorInstance> _doors = new FastList<>();
	private final List<String> _doorDefault = new FastList<>();
	private String _name = "";
	private int _ownerId = 0;
	private Siege _siege = null;
	private Calendar _siegeDate;
	private int _siegeDayOfWeek = 7; // Default to saturday
	private int _siegeHourOfDay = 20; // Default to 8 pm server time
	private int _taxPercent = 0;
	private double _taxRate = 0;
	private int _treasury = 0;
	private L2CastleZone _zone;
	private L2CastleTeleportZone _teleZone;
	private L2Clan _formerOwner = null;
	private int _nbArtifact = 1;
	private final int[] _gate =
	{
		Integer.MIN_VALUE,
		0,
		0
	};
	private final Map<Integer, Integer> _engrave = new FastMap<>();
	
	// =========================================================
	// Constructor
	public Castle(final int castleId)
	{
		_castleId = castleId;
		
		if (_castleId == 7 || castleId == 9)
		{
			_nbArtifact = 2;
		}
		load();
		loadDoor();
	}
	
	// =========================================================
	// Method - Public
	
	public void Engrave(final L2Clan clan, final int objId)
	{
		_engrave.put(objId, clan.getClanId());
		
		if (_engrave.size() == _nbArtifact)
		{
			boolean rst = true;
			
			for (final int id : _engrave.values())
			{
				if (id != clan.getClanId())
				{
					rst = false;
				}
			}
			
			if (rst)
			{
				_engrave.clear();
				setOwner(clan);
			}
			else
			{
				getSiege().announceToPlayer("Clan " + clan.getName() + " has finished to engrave one of the rulers.", true);
			}
		}
		else
		{
			getSiege().announceToPlayer("Clan " + clan.getName() + " has finished to engrave one of the rulers.", true);
		}
	}
	
	// This method add to the treasury
	/**
	 * Add amount to castle instance's treasury (warehouse).
	 * @param amount
	 */
	public void addToTreasury(int amount)
	{
		if (getOwnerId() <= 0)
			return;
		
		if (_name.equalsIgnoreCase("Schuttgart") || _name.equalsIgnoreCase("Goddard"))
		{
			Castle rune = CastleManager.getInstance().getCastle("rune");
			if (rune != null)
			{
				final int runeTax = (int) (amount * rune.getTaxRate());
				
				if (rune.getOwnerId() > 0)
				{
					rune.addToTreasury(runeTax);
				}
				
				amount -= runeTax;
			}
			
			rune = null;
		}
		if (!_name.equalsIgnoreCase("aden") && !_name.equalsIgnoreCase("Rune") && !_name.equalsIgnoreCase("Schuttgart") && !_name.equalsIgnoreCase("Goddard")) // If current castle instance is not Aden, Rune, Goddard or Schuttgart.
		{
			Castle aden = CastleManager.getInstance().getCastle("aden");
			
			if (aden != null)
			{
				final int adenTax = (int) (amount * aden.getTaxRate()); // Find out what Aden gets from the current castle instance's income
				
				if (aden.getOwnerId() > 0)
				{
					aden.addToTreasury(adenTax); // Only bother to really add the tax to the treasury if not npc owned
				}
				
				amount -= adenTax; // Subtract Aden's income from current castle instance's income
			}
			
			aden = null;
		}
		
		addToTreasuryNoTax(amount);
	}
	
	/**
	 * Add amount to castle instance's treasury (warehouse), no tax paying.
	 * @param amount
	 * @return
	 */
	public boolean addToTreasuryNoTax(int amount)
	{
		if (getOwnerId() <= 0)
			return false;
		
		if (amount < 0)
		{
			amount *= -1;
			
			if (_treasury < amount)
				return false;
			
			_treasury -= amount;
		}
		else
		{
			if ((long) _treasury + amount > Integer.MAX_VALUE)
			{
				_treasury = Integer.MAX_VALUE;
			}
			else
			{
				_treasury += amount;
			}
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Update castle set treasury = ? where id = ?");
			statement.setInt(1, getTreasury());
			statement.setInt(2, getCastleId());
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
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
		return true;
	}
	
	/**
	 * Move non clan members off castle area and to nearest town.<BR>
	 * <BR>
	 */
	public void banishForeigners()
	{
		_zone.banishForeigners(getOwnerId());
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return true if object is inside the zone
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		return getZone().isInsideZone(x, y, z);
	}
	
	public L2CastleZone getZone()
	{
		return _zone;
	}
	
	/**
	 * Sets this castles zone
	 * @param zone
	 */
	public void setZone(final L2CastleZone zone)
	{
		_zone = zone;
	}
	
	
	public void setTeleZone(final L2CastleTeleportZone zone)
	{
		_teleZone = zone;
	}
	
	public L2CastleTeleportZone getTeleZone()
	{
		return _teleZone;
	}
	
	/**
	 * Get the objects distance to this castle
	 * @param obj
	 * @return
	 */
	public double getDistance(final L2Object obj)
	{
		return _zone.getDistanceToZone(obj);
	}
	
	public void closeDoor(final L2PcInstance activeChar, final int doorId)
	{
		openCloseDoor(activeChar, doorId, false);
	}
	
	public void openDoor(final L2PcInstance activeChar, final int doorId)
	{
		openCloseDoor(activeChar, doorId, true);
	}
	
	public void openCloseDoor(final L2PcInstance activeChar, final int doorId, final boolean open)
	{
		if (activeChar.getClanId() != getOwnerId())
			return;
		
		L2DoorInstance door = getDoor(doorId);
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
		
		door = null;
	}
	
	// This method is used to begin removing all castle upgrades
	public void removeUpgrade()
	{
		removeDoorUpgrade();
	}
	
	// This method updates the castle tax rate
	public void setOwner(final L2Clan clan)
	{
		// Remove old owner
		if (getOwnerId() > 0 && (clan == null || clan.getClanId() != getOwnerId()))
		{
			L2Clan oldOwner = ClanTable.getInstance().getClan(getOwnerId()); // Try to find clan instance
			
			if (oldOwner != null)
			{
				if (_formerOwner == null)
				{
					_formerOwner = oldOwner;
					if (Config.REMOVE_CASTLE_CIRCLETS)
					{
						CastleManager.getInstance().removeCirclet(_formerOwner, getCastleId());
					}
				}
				oldOwner.setHasCastle(0); // Unset has castle flag for old owner
				Announcements.getInstance().announceToAll(oldOwner.getName() + " has lost " + getName() + " castle!");
				
				// remove crowns
				CrownManager.getInstance().checkCrowns(oldOwner);
			}
			
			oldOwner = null;
		}
		
		updateOwnerInDB(clan); // Update in database
		
		if (getSiege().getIsInProgress())
		{
			getSiege().midVictory(); // Mid victory phase of siege
		}
		
		updateClansReputation();
	}
	
	public void removeOwner(final L2Clan clan)
	{
		if (clan != null)
		{
			_formerOwner = clan;
			
			if (Config.REMOVE_CASTLE_CIRCLETS)
			{
				CastleManager.getInstance().removeCirclet(_formerOwner, getCastleId());
			}
			
			clan.setHasCastle(0);
			
			Announcements.getInstance().announceToAll(clan.getName() + " has lost " + getName() + " castle");
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		}
		
		updateOwnerInDB(null);
		
		if (getSiege().getIsInProgress())
		{
			getSiege().midVictory();
		}
		
		updateClansReputation();
	}
	
	// This method updates the castle tax rate
	public void setTaxPercent(final L2PcInstance activeChar, final int taxPercent)
	{
		int maxTax;
		
		switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_DAWN:
				maxTax = 25;
				break;
			case SevenSigns.CABAL_DUSK:
				maxTax = 5;
				break;
			default: // no owner
				maxTax = 15;
		}
		
		if (taxPercent < 0 || taxPercent > maxTax)
		{
			activeChar.sendMessage("Tax value must be between 0 and " + maxTax + ".");
			return;
		}
		
		setTaxPercent(taxPercent);
		activeChar.sendMessage(getName() + " castle tax changed to " + taxPercent + "%.");
	}
	
	public void setTaxPercent(final int taxPercent)
	{
		_taxPercent = taxPercent;
		_taxRate = _taxPercent / 100.0;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Update castle set taxPercent = ? where id = ?");
			statement.setInt(1, taxPercent);
			statement.setInt(2, getCastleId());
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
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
	 * Respawn all doors on castle grounds<BR>
	 * <BR>
	 */
	public void spawnDoor()
	{
		spawnDoor(false);
	}
	
	/**
	 * Respawn all doors on castle grounds
	 * @param isDoorWeak
	 */
	public void spawnDoor(final boolean isDoorWeak)
	{
		for (int i = 0; i < getDoors().size(); i++)
		{
			L2DoorInstance door = getDoors().get(i);
			if (door.getCurrentHp() <= 0)
			{
				door.decayMe(); // Kill current if not killed already
				door = DoorTable.parseList(_doorDefault.get(i));
				
				if (isDoorWeak)
				{
					door.setCurrentHpDirect(door.getMaxHp() / 2);
				}
				else
				{
					door.setCurrentHpDirect(door.getMaxHp());
				}
				
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				getDoors().set(i, door);
			}
			else if (door.getOpen())
			{
				door.closeMe();
			}
		}
		loadDoorUpgrade(); // Check for any upgrade the doors may have
	}
	
	// This method upgrade door
	public void upgradeDoor(final int doorId, final int hp, final int pDef, final int mDef)
	{
		final L2DoorInstance door = getDoor(doorId);
		if (door == null)
			return;
		
		if (door.getDoorId() == doorId)
		{
			door.setCurrentHpDirect(door.getMaxHp() + hp);
			
			saveDoorUpgrade(doorId, hp, pDef, mDef);
			return;
		}
	}
	
	// =========================================================
	// Method - Private
	// This method loads castle
	private void load()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("Select * from castle where id = ?");
			statement.setInt(1, getCastleId());
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				_name = rs.getString("name");
				
				_siegeDate = Calendar.getInstance();
				_siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
				
				_siegeDayOfWeek = rs.getInt("siegeDayOfWeek");
				if (_siegeDayOfWeek < 1 || _siegeDayOfWeek > 7)
				{
					_siegeDayOfWeek = 7;
				}
				
				_siegeHourOfDay = rs.getInt("siegeHourOfDay");
				if (_siegeHourOfDay < 0 || _siegeHourOfDay > 23)
				{
					_siegeHourOfDay = 20;
				}
				
				_taxPercent = rs.getInt("taxPercent");
				_treasury = rs.getInt("treasury");
			}
			
			rs.close();
			DatabaseUtils.close(statement);
			
			_taxRate = _taxPercent / 100.0;
			
			statement = con.prepareStatement("Select clan_id from clan_data where hasCastle = ?");
			statement.setInt(1, getCastleId());
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				_ownerId = rs.getInt("clan_id");
			}
			
			if (getOwnerId() > 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(getOwnerId()); // Try to find clan instance
				ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000); // Schedule owner tasks to start running
			}
			
			rs.close();
			DatabaseUtils.close(statement);
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
	
	// This method loads castle door data from database
	private void loadDoor()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Select * from castle_door where castleId = ?");
			statement.setInt(1, getCastleId());
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				// Create list of the door default for use when respawning dead doors
				_doorDefault.add(rs.getString("name") + ";" + rs.getInt("id") + ";" + rs.getInt("x") + ";" + rs.getInt("y") + ";" + rs.getInt("z") + ";" + rs.getInt("range_xmin") + ";" + rs.getInt("range_ymin") + ";" + rs.getInt("range_zmin") + ";" + rs.getInt("range_xmax") + ";" + rs.getInt("range_ymax") + ";" + rs.getInt("range_zmax") + ";" + rs.getInt("hp") + ";" + rs.getInt("pDef") + ";" + rs.getInt("mDef"));
				
				L2DoorInstance door = DoorTable.parseList(_doorDefault.get(_doorDefault.size() - 1));
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				_doors.add(door);
				DoorTable.getInstance().putDoor(door);
			}
			
			rs.close();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	// This method loads castle door upgrade data from database
	private void loadDoorUpgrade()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Select * from castle_doorupgrade where doorId in (Select Id from castle_door where castleId = ?)");
			statement.setInt(1, getCastleId());
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				upgradeDoor(rs.getInt("id"), rs.getInt("hp"), rs.getInt("pDef"), rs.getInt("mDef"));
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
	
	private void removeDoorUpgrade()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("delete from castle_doorupgrade where doorId in (select id from castle_door where castleId=?)");
			statement.setInt(1, getCastleId());
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
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
	
	private void saveDoorUpgrade(final int doorId, final int hp, final int pDef, final int mDef)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO castle_doorupgrade (doorId, hp, pDef, mDef) values (?,?,?,?)");
			statement.setInt(1, doorId);
			statement.setInt(2, hp);
			statement.setInt(3, pDef);
			statement.setInt(4, mDef);
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
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
	
	private void updateOwnerInDB(final L2Clan clan)
	{
		if (clan != null)
		{
			_ownerId = clan.getClanId(); // Update owner id property
		}
		else
		{
			_ownerId = 0; // Remove owner
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			// ============================================================================
			// NEED TO REMOVE HAS CASTLE FLAG FROM CLAN_DATA
			// SHOULD BE CHECKED FROM CASTLE TABLE
			statement = con.prepareStatement("UPDATE clan_data SET hasCastle=0 WHERE hasCastle=?");
			statement.setInt(1, getCastleId());
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
			
			statement = con.prepareStatement("UPDATE clan_data SET hasCastle=? WHERE clan_id=?");
			statement.setInt(1, getCastleId());
			statement.setInt(2, getOwnerId());
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
			// ============================================================================
			
			// Announce to clan memebers
			if (clan != null)
			{
				clan.setHasCastle(getCastleId()); // Set has castle flag for new owner
				Announcements.getInstance().announceToAll(clan.getName() + " has taken " + getName() + " castle!");
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
				// give crowns
				CrownManager.getInstance().checkCrowns(clan);
				
				ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000); // Schedule owner tasks to start running
			}
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
	
	// =========================================================
	// Property
	public final int getCastleId()
	{
		return _castleId;
	}
	
	public final L2DoorInstance getDoor(final int doorId)
	{
		if (doorId <= 0)
			return null;
		
		for (int i = 0; i < getDoors().size(); i++)
		{
			L2DoorInstance door = getDoors().get(i);
			
			if (door.getDoorId() == doorId)
				return door;
			
			door = null;
		}
		return null;
	}
	
	public final List<L2DoorInstance> getDoors()
	{
		return _doors;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final int getOwnerId()
	{
		return _ownerId;
	}
	
	public final Siege getSiege()
	{
		if (_siege == null)
		{
			_siege = new Siege(new Castle[]
			{
				this
			});
		}
		
		return _siege;
	}
	
	public final Calendar getSiegeDate()
	{
		return _siegeDate;
	}
	
	public final int getSiegeDayOfWeek()
	{
		return _siegeDayOfWeek;
	}
	
	public final int getSiegeHourOfDay()
	{
		return _siegeHourOfDay;
	}
	
	public final int getTaxPercent()
	{
		return _taxPercent;
	}
	
	public final double getTaxRate()
	{
		return _taxRate;
	}
	
	public final int getTreasury()
	{
		return _treasury;
	}
	
	public FastList<SeedProduction> getSeedProduction(final int period)
	{
		return period == CastleManorManager.PERIOD_CURRENT ? _production : _productionNext;
	}
	
	public FastList<CropProcure> getCropProcure(final int period)
	{
		return period == CastleManorManager.PERIOD_CURRENT ? _procure : _procureNext;
	}
	
	public void setSeedProduction(final FastList<SeedProduction> seed, final int period)
	{
		if (period == CastleManorManager.PERIOD_CURRENT)
		{
			_production = seed;
		}
		else
		{
			_productionNext = seed;
		}
	}
	
	public void setCropProcure(final FastList<CropProcure> crop, final int period)
	{
		if (period == CastleManorManager.PERIOD_CURRENT)
		{
			_procure = crop;
		}
		else
		{
			_procureNext = crop;
		}
	}
	
	public synchronized SeedProduction getSeed(final int seedId, final int period)
	{
		for (final SeedProduction seed : getSeedProduction(period))
		{
			if (seed.getId() == seedId)
				return seed;
		}
		return null;
	}
	
	public synchronized CropProcure getCrop(final int cropId, final int period)
	{
		for (final CropProcure crop : getCropProcure(period))
		{
			if (crop.getId() == cropId)
				return crop;
		}
		return null;
	}
	
	public int getManorCost(final int period)
	{
		FastList<CropProcure> procure;
		FastList<SeedProduction> production;
		
		if (period == CastleManorManager.PERIOD_CURRENT)
		{
			procure = _procure;
			production = _production;
		}
		else
		{
			procure = _procureNext;
			production = _productionNext;
		}
		
		int total = 0;
		
		if (production != null)
		{
			for (final SeedProduction seed : production)
			{
				total += L2Manor.getInstance().getSeedBuyPrice(seed.getId()) * seed.getStartProduce();
			}
		}
		
		if (procure != null)
		{
			for (final CropProcure crop : procure)
			{
				total += crop.getPrice() * crop.getStartAmount();
			}
		}
		
		procure = null;
		production = null;
		
		return total;
	}
	
	// save manor production data
	public void saveSeedData()
	{
		Connection con = null;
		PreparedStatement statement;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION);
			statement.setInt(1, getCastleId());
			
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
			
			if (_production != null)
			{
				int count = 0;
				
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[_production.size()];
				
				for (final SeedProduction s : _production)
				{
					values[count] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_CURRENT + ")";
					count++;
				}
				
				if (values.length > 0)
				{
					query += values[0];
					for (int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}
					statement = con.prepareStatement(query);
					statement.execute();
					DatabaseUtils.close(statement);
					statement = null;
				}
				
				query = null;
				values = null;
			}
			
			if (_productionNext != null)
			{
				int count = 0;
				
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[_productionNext.size()];
				
				for (final SeedProduction s : _productionNext)
				{
					values[count] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_NEXT + ")";
					count++;
				}
				
				if (values.length > 0)
				{
					query += values[0];
					
					for (int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}
					
					statement = con.prepareStatement(query);
					statement.execute();
					DatabaseUtils.close(statement);
					statement = null;
				}
				
				query = null;
				values = null;
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	// save manor production data for specified period
	public void saveSeedData(final int period)
	{
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION_PERIOD);
			statement.setInt(1, getCastleId());
			statement.setInt(2, period);
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
			
			FastList<SeedProduction> prod = null;
			prod = getSeedProduction(period);
			
			if (prod != null)
			{
				int count = 0;
				
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[prod.size()];
				
				for (final SeedProduction s : prod)
				{
					values[count] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + period + ")";
					count++;
				}
				if (values.length > 0)
				{
					query += values[0];
					for (int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}
					statement = con.prepareStatement(query);
					statement.execute();
					DatabaseUtils.close(statement);
					statement = null;
				}
				
				query = null;
				values = null;
			}
			
			prod = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	// save crop procure data
	public void saveCropData()
	{
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE);
			statement.setInt(1, getCastleId());
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
			
			if (_procure != null)
			{
				int count = 0;
				
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[_procure.size()];
				
				for (final CropProcure cp : _procure)
				{
					values[count] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_CURRENT + ")";
					count++;
				}
				
				if (values.length > 0)
				{
					query += values[0];
					
					for (int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}
					
					statement = con.prepareStatement(query);
					statement.execute();
					DatabaseUtils.close(statement);
					statement = null;
				}
				
				query = null;
				values = null;
			}
			
			if (_procureNext != null)
			{
				int count = 0;
				
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[_procureNext.size()];
				
				for (final CropProcure cp : _procureNext)
				{
					values[count] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_NEXT + ")";
					count++;
				}
				
				if (values.length > 0)
				{
					query += values[0];
					for (int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}
					statement = con.prepareStatement(query);
					statement.execute();
					DatabaseUtils.close(statement);
					statement = null;
				}
				
				query = null;
				values = null;
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	// save crop procure data for specified period
	public void saveCropData(final int period)
	{
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE_PERIOD);
			statement.setInt(1, getCastleId());
			statement.setInt(2, period);
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
			
			FastList<CropProcure> proc = null;
			proc = getCropProcure(period);
			
			if (proc != null)
			{
				int count = 0;
				
				String query = "INSERT INTO castle_manor_procure VALUES ";
				final String values[] = new String[proc.size()];
				
				for (final CropProcure cp : proc)
				{
					values[count] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + period + ")";
					count++;
				}
				
				if (values.length > 0)
				{
					query += values[0];
					
					for (int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}
					
					statement = con.prepareStatement(query);
					statement.execute();
					DatabaseUtils.close(statement);
					statement = null;
				}
				
				query = null;
			}
			
			proc = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	public void updateCrop(final int cropId, final int amount, final int period)
	{
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement(CASTLE_UPDATE_CROP);
			statement.setInt(1, amount);
			statement.setInt(2, cropId);
			statement.setInt(3, getCastleId());
			statement.setInt(4, period);
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	public void updateSeed(final int seedId, final int amount, final int period)
	{
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement(CASTLE_UPDATE_SEED);
			statement.setInt(1, amount);
			statement.setInt(2, seedId);
			statement.setInt(3, getCastleId());
			statement.setInt(4, period);
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	public boolean isNextPeriodApproved()
	{
		return _isNextPeriodApproved;
	}
	
	public void setNextPeriodApproved(final boolean val)
	{
		_isNextPeriodApproved = val;
	}
	
	public void updateClansReputation()
	{
		if (_formerOwner != null)
		{
			if (_formerOwner != ClanTable.getInstance().getClan(getOwnerId()))
			{
				final int maxreward = Math.max(0, _formerOwner.getReputationScore());
				_formerOwner.setReputationScore(_formerOwner.getReputationScore() - 1000, true);
				
				L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
				
				if (owner != null)
				{
					owner.setReputationScore(owner.getReputationScore() + Math.min(1000, maxreward), true);
					owner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(owner));
				}
				
				owner = null;
			}
			else
			{
				_formerOwner.setReputationScore(_formerOwner.getReputationScore() + 500, true);
			}
			
			_formerOwner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_formerOwner));
		}
		else
		{
			L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
			
			if (owner != null)
			{
				owner.setReputationScore(owner.getReputationScore() + 1000, true);
				owner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(owner));
			}
			
			owner = null;
		}
	}
	
	public void createClanGate(final int x, final int y, final int z)
	{
		_gate[0] = x;
		_gate[1] = y;
		_gate[2] = z;
	}
	
	/** Optimized as much as possible. */
	public void destroyClanGate()
	{
		_gate[0] = Integer.MIN_VALUE;
	}
	
	/**
	 * This method must always be called before using gate coordinate retrieval methods! Optimized as much as possible.
	 * @return is a Clan Gate available
	 */
	
	public boolean isGateOpen()
	{
		return _gate[0] != Integer.MIN_VALUE;
	}
	
	public int getGateX()
	{
		return _gate[0];
	}
	
	public int getGateY()
	{
		return _gate[1];
	}
	
	public int getGateZ()
	{
		return _gate[2];
	}
	
	public void oustAllPlayers()
	{
		if (Config.DEBUG && _teleZone != null)
		{
			LOG.info("Castle Teleport Zone ID: " + _teleZone.getId());
			LOG.info("Players Number in Castle Teleport Zone: " + _teleZone.getAllPlayers().size());
			for (final L2Character actual : _teleZone.getAllPlayers())
			{
				LOG.info("	Player Name: " + actual.getName());
			}
		}
		getTeleZone().oustAllPlayers();
	}
	
	/**
	 * @return
	 */
	public boolean isSiegeInProgress()
	{
		if (_siege != null)
		{
			return _siege.getIsInProgress();
		}
		
		return false;
	}
}

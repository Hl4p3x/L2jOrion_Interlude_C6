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
package l2jorion.game.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javolution.util.FastList;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.knownlist.VehicleKnownList;
import l2jorion.game.model.actor.stat.VehicleStat;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.game.templates.L2CharTemplate;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Util;

public abstract class L2Vehicle extends L2Character
{
	protected int _dockId = 0;
	protected final FastList<L2PcInstance> _passengers = new FastList<>();
	protected Location _oustLoc = null;
	private Runnable _engine = null;
	
	protected VehiclePathPoint[] _currentPath = null;
	protected int _runState = 0;
	
	public L2Vehicle(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
		getKnownList();
		getStat();
		setIsFlying(true);
	}
	
	public boolean isBoat()
	{
		return false;
	}
	
	public boolean canBeControlled()
	{
		return _engine == null;
	}
	
	public void registerEngine(Runnable r)
	{
		_engine = r;
	}
	
	public void runEngine(int delay)
	{
		if (_engine != null)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(_engine, delay);
		}
	}
	
	public void executePath(VehiclePathPoint[] path)
	{
		_runState = 0;
		_currentPath = path;
		
		if ((_currentPath != null) && (_currentPath.length > 0))
		{
			final VehiclePathPoint point = _currentPath[0];
			if (point.moveSpeed > 0)
			{
				getStat().setMoveSpeed(point.moveSpeed);
			}
			
			if (point.rotationSpeed > 0)
			{
				getStat().setRotationSpeed(point.rotationSpeed);
			}
			
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(point.x, point.y, point.z, 0));
			return;
		}
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	}
	
	@Override
	public boolean moveToNextRoutePoint()
	{
		_move = null;
		
		if (_currentPath != null)
		{
			_runState++;
			if (_runState < _currentPath.length)
			{
				final VehiclePathPoint point = _currentPath[_runState];
				if (!isMovementDisabled())
				{
					if (point.moveSpeed == 0)
					{
						teleToLocation(point.x, point.y, point.z, false);
						_currentPath = null;
					}
					else
					{
						if (point.moveSpeed > 0)
						{
							getStat().setMoveSpeed(point.moveSpeed);
						}
						if (point.rotationSpeed > 0)
						{
							getStat().setRotationSpeed(point.rotationSpeed);
						}
						
						MoveData m = new MoveData();
						m.disregardingGeodata = false;
						m.onGeodataPathIndex = -1;
						m._xDestination = point.x;
						m._yDestination = point.y;
						m._zDestination = point.z;
						m._heading = 0;
						
						final double dx = point.x - getX();
						final double dy = point.y - getY();
						final double distance = Math.sqrt((dx * dx) + (dy * dy));
						
						if (distance > 1) // vertical movement heading check
						{
							setHeading(Util.calculateHeadingFrom(getX(), getY(), point.x, point.y));
						}
						
						m._moveStartTime = GameTimeController.getInstance().getGameTicks();
						_move = m;
						
						GameTimeController.getInstance().registerMovingObject(this);
						return true;
					}
				}
			}
			else
			{
				_currentPath = null;
			}
		}
		
		runEngine(10);
		return false;
	}
	
	@Override
	public final VehicleKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof VehicleKnownList))
		{
			setKnownList(new VehicleKnownList(this));
		}
		
		return (VehicleKnownList) super.getKnownList();
	}
	
	@Override
	public final VehicleStat getStat()
	{
		if (super.getStat() == null || !(super.getStat() instanceof VehicleStat))
		{
			setStat(new VehicleStat(this));
		}
		
		return (VehicleStat) super.getStat();
	}
	
	public boolean isInDock()
	{
		return _dockId > 0;
	}
	
	public int getDockId()
	{
		return _dockId;
	}
	
	public void setInDock(int d)
	{
		_dockId = d;
	}
	
	public void setOustLoc(Location loc)
	{
		_oustLoc = loc;
	}
	
	public Location getOustLoc()
	{
		return _oustLoc != null ? _oustLoc : MapRegionTable.getInstance().getTeleToLocation(this, MapRegionTable.TeleportWhereType.Town);
	}
	
	public void oustPlayers()
	{
		// Use iterator because oustPlayer will try to remove player from _passengers
		final Iterator<L2PcInstance> iter = _passengers.iterator();
		while (iter.hasNext())
		{
			L2PcInstance player = iter.next();
			
			if (player != null)
			{
				oustPlayer(player);
			}
			
			iter.remove();
		}
	}
	
	public void oustPlayer(L2PcInstance player)
	{
		player.setVehicle(null);
		player.setInVehiclePosition(null);
		removePassenger(player);
		
		player.setInsideZone(ZoneId.ZONE_PEACE, false);
		player.sendPacket(SystemMessageId.EXIT_PEACEFUL_ZONE);
	}
	
	public boolean addPassenger(L2PcInstance player)
	{
		if ((player == null) || _passengers.contains(player))
		{
			return false;
		}
		
		// already in other vehicle
		if ((player.getVehicle() != null) && (player.getVehicle() != this))
		{
			return false;
		}
		
		_passengers.add(player);
		
		player.setInsideZone(ZoneId.ZONE_PEACE, true);
		player.sendPacket(SystemMessageId.ENTER_PEACEFUL_ZONE);
		
		return true;
	}
	
	public void removePassenger(L2PcInstance player)
	{
		try
		{
			_passengers.remove(player);
		}
		catch (Exception e)
		{
		}
	}
	
	public boolean isEmpty()
	{
		return _passengers.isEmpty();
	}
	
	public List<L2PcInstance> getPassengers()
	{
		return _passengers;
	}
	
	public void broadcastToPassengers(L2GameServerPacket sm)
	{
		for (L2PcInstance player : _passengers)
		{
			if (player != null)
			{
				player.sendPacket(sm);
			}
		}
	}
	
	public void payForRide(int itemId, int count, int oustX, int oustY, int oustZ)
	{
		final Collection<L2PcInstance> passengers = getKnownList().getKnownTypeInRadius(L2PcInstance.class, 1000);
		if ((passengers != null) && !passengers.isEmpty())
		{
			L2ItemInstance ticket;
			InventoryUpdate iu;
			for (L2PcInstance player : passengers)
			{
				if (player == null)
				{
					continue;
				}
				
				if (player.isInBoat() && (player.getBoat() == this))
				{
					if (itemId > 0)
					{
						ticket = player.getInventory().getItemByItemId(itemId);
						if ((ticket == null) || (player.getInventory().destroyItem("Boat", ticket, count, player, this) == null))
						{
							player.sendPacket(SystemMessageId.NOT_CORRECT_BOAT_TICKET);
							player.teleToLocation(oustX, oustY, oustZ, true);
							continue;
						}
						iu = new InventoryUpdate();
						
						if (ticket.getCount() == 0)
						{
							iu.addRemovedItem(ticket);
						}
						else
						{
							iu.addModifiedItem(ticket);
						}
						
						player.sendPacket(iu);
					}
					addPassenger(player);
				}
			}
		}
	}
	
	@Override
	public boolean updatePosition()
	{
		final boolean result = super.updatePosition();
		
		for (L2PcInstance player : _passengers)
		{
			if ((player != null) && (player.getVehicle() == this))
			{
				player.getPosition().setXYZ(getX(), getY(), getZ());
				player.revalidateZone(false);
			}
		}
		return result;
	}
	
	@Override
	public void teleToLocation(int x, int y, int z, boolean allowRandomOffset)
	{
		if (isMoving())
		{
			stopMove(null);
		}
		
		setIsTeleporting(true);
		
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		
		for (L2PcInstance player : _passengers)
		{
			if (player != null)
			{
				player.teleToLocation(x, y, z);
			}
		}
		
		decayMe();
		setXYZ(x, y, z);
		
		onTeleported();
		revalidateZone(true);
	}
	
	@Override
	public void stopMove(Location loc)
	{
		_move = null;
		if (loc != null)
		{
			setXYZ(loc.getX(), loc.getY(), loc.getZ());
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
	}
	
	@Override
	public void deleteMe()
	{
		_engine = null;
		
		try
		{
			if (isMoving())
			{
				stopMove(null);
			}
		}
		catch (Exception e)
		{
			LOG.warn("Failed stopMove().", e);
		}
		
		try
		{
			oustPlayers();
		}
		catch (Exception e)
		{
			LOG.warn("Failed oustPlayers().", e);
		}
		
		final L2WorldRegion oldRegion = getWorldRegion();
		
		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			LOG.warn("Failed decayMe().", e);
		}
		
		if (oldRegion != null)
		{
			oldRegion.removeFromZones(this);
		}
		
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
			LOG.warn("Failed cleaning knownlist.", e);
		}
		
		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
		
		super.deleteMe();
	}
	
	@Override
	public void updateAbnormalEffect()
	{
	}
	
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public int getLevel()
	{
		return 0;
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
}

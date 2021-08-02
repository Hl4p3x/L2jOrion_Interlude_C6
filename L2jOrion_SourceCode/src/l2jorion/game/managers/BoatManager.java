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
package l2jorion.game.managers;

import java.util.Collection;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2World;
import l2jorion.game.model.VehiclePathPoint;
import l2jorion.game.model.actor.instance.L2BoatInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.game.templates.L2CharTemplate;
import l2jorion.game.templates.StatsSet;

public class BoatManager
{
	private final Map<Integer, L2BoatInstance> _boats = new FastMap<>();
	private final boolean[] _docksBusy = new boolean[3];
	
	public static final int TALKING_ISLAND = 1;
	public static final int GLUDIN_HARBOR = 2;
	public static final int RUNE_HARBOR = 3;
	
	public static final int BOAT_BROADCAST_RADIUS = 20000;
	
	public static final BoatManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected BoatManager()
	{
		for (int i = 0; i < _docksBusy.length; i++)
		{
			_docksBusy[i] = false;
		}
	}
	
	public L2BoatInstance getNewBoat(int boatId, int x, int y, int z, int heading)
	{
		if (!Config.ALLOW_BOAT)
		{
			return null;
		}
		
		StatsSet npcDat = new StatsSet();
		npcDat.set("npcId", boatId);
		npcDat.set("level", 0);
		npcDat.set("jClass", "boat");
		
		npcDat.set("baseSTR", 0);
		npcDat.set("baseCON", 0);
		npcDat.set("baseDEX", 0);
		npcDat.set("baseINT", 0);
		npcDat.set("baseWIT", 0);
		npcDat.set("baseMEN", 0);
		
		npcDat.set("baseShldDef", 0);
		npcDat.set("baseShldRate", 0);
		npcDat.set("baseAccCombat", 38);
		npcDat.set("baseEvasRate", 38);
		npcDat.set("baseCritRate", 38);
		
		npcDat.set("collision_radius", 0);
		npcDat.set("collision_height", 0);
		npcDat.set("sex", "male");
		npcDat.set("type", "");
		npcDat.set("baseAtkRange", 0);
		npcDat.set("baseMpMax", 0);
		npcDat.set("baseCpMax", 0);
		npcDat.set("rewardExp", 0);
		npcDat.set("rewardSp", 0);
		npcDat.set("basePAtk", 0);
		npcDat.set("baseMAtk", 0);
		npcDat.set("basePAtkSpd", 0);
		npcDat.set("aggroRange", 0);
		npcDat.set("baseMAtkSpd", 0);
		npcDat.set("rhand", 0);
		npcDat.set("lhand", 0);
		npcDat.set("armor", 0);
		npcDat.set("baseWalkSpd", 0);
		npcDat.set("baseRunSpd", 0);
		npcDat.set("baseHpMax", 50000);
		npcDat.set("baseHpReg", 3.e-3f);
		npcDat.set("baseMpReg", 3.e-3f);
		npcDat.set("basePDef", 100);
		npcDat.set("baseMDef", 100);
		
		L2CharTemplate template = new L2CharTemplate(npcDat);
		L2BoatInstance boat = new L2BoatInstance(IdFactory.getInstance().getNextId(), template);
		
		_boats.put(boat.getObjectId(), boat);
		
		boat.setHeading(heading);
		boat.setXYZInvisible(x, y, z);
		boat.spawnMe();
		
		return boat;
	}
	
	public L2BoatInstance getBoat(int boatId)
	{
		return _boats.get(boatId);
	}
	
	public void dockShip(int h, boolean value)
	{
		try
		{
			_docksBusy[h] = value;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
		}
	}
	
	public boolean dockBusy(int h)
	{
		try
		{
			return _docksBusy[h];
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			return false;
		}
	}
	
	public void broadcastPacket(VehiclePathPoint point1, VehiclePathPoint point2, L2GameServerPacket packet)
	{
		double dx, dy;
		final Collection<L2PcInstance> players = L2World.getInstance().getAllPlayers().values();
		for (L2PcInstance player : players)
		{
			if (player == null)
			{
				continue;
			}
			
			dx = (double) player.getX() - point1.x;
			dy = (double) player.getY() - point1.y;
			
			if (Math.sqrt((dx * dx) + (dy * dy)) < BOAT_BROADCAST_RADIUS)
			{
				player.sendPacket(packet);
			}
			else
			{
				dx = (double) player.getX() - point2.x;
				dy = (double) player.getY() - point2.y;
				
				if (Math.sqrt((dx * dx) + (dy * dy)) < BOAT_BROADCAST_RADIUS)
				{
					player.sendPacket(packet);
				}
			}
		}
	}
	
	public void broadcastPackets(VehiclePathPoint point1, VehiclePathPoint point2, L2GameServerPacket... packets)
	{
		double dx, dy;
		final Collection<L2PcInstance> players = L2World.getInstance().getAllPlayers().values();
		for (L2PcInstance player : players)
		{
			if (player == null)
			{
				continue;
			}
			
			dx = (double) player.getX() - point1.x;
			dy = (double) player.getY() - point1.y;
			
			if (Math.sqrt((dx * dx) + (dy * dy)) < BOAT_BROADCAST_RADIUS)
			{
				for (L2GameServerPacket p : packets)
				{
					player.sendPacket(p);
				}
			}
			else
			{
				dx = (double) player.getX() - point2.x;
				dy = (double) player.getY() - point2.y;
				
				if (Math.sqrt((dx * dx) + (dy * dy)) < BOAT_BROADCAST_RADIUS)
				{
					for (L2GameServerPacket p : packets)
					{
						player.sendPacket(p);
					}
				}
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final BoatManager _instance = new BoatManager();
	}
}

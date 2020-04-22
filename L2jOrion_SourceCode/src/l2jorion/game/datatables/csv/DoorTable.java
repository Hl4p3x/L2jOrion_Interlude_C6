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
package l2jorion.game.datatables.csv;

import gnu.trove.TIntObjectHashMap;
import l2jorion.Config;
import l2jorion.game.geo.pathfinding.AbstractNodeLoc;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.templates.L2CharTemplate;
import l2jorion.game.templates.StatsSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoorTable
{
	private static Logger LOG = LoggerFactory.getLogger(DoorTable.class);
	
	private final TIntObjectHashMap<L2DoorInstance> _staticItems;
	private final TIntObjectHashMap<ArrayList<L2DoorInstance>> _regions;
	
	public static DoorTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public DoorTable()
	{
		_staticItems = new TIntObjectHashMap<>();
		_regions = new TIntObjectHashMap<>();
	}
	
	public void reloadAll()
	{
		respawn();
	}
	
	public void respawn()
	{
		_staticItems.clear();
		parseData();
	}
	
	public void parseData()
	{
		LineNumberReader lnr = null;
		try
		{
			File doorData = new File(Config.DATAPACK_ROOT, "data/csv/door.csv");
			lnr = new LineNumberReader(new BufferedReader(new FileReader(doorData)));
			
			String line = null;
			LOG.info("Searching clan halls doors:");
			
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				
				L2DoorInstance door = parseList(line);
				
				putDoor(door);
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				
				ClanHall clanhall = ClanHallManager.getInstance().getNearbyClanHall(door.getX(), door.getY(), 500);
				if (clanhall != null)
				{
					clanhall.getDoors().add(door);
					door.setClanHall(clanhall);
				}
			}
			LOG.info("DoorTable: Loaded " + _staticItems.size() + " Door Templates for " + _regions.size() + " regions.");
		}
		catch (final FileNotFoundException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("door.csv is missing in data folder");
		}
		catch (final IOException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("error while creating door table " + e);
		}
		finally
		{
			try
			{
				///lnr.close();
			}
			catch (Exception e1)
			{ /* ignore problems */
			}
		}
	}
	
	public static L2DoorInstance parseList(final String line)
	{
		StringTokenizer st = new StringTokenizer(line, ";");
		L2DoorInstance door = null;
		try
		{

			String name = st.nextToken();
			final int id = Integer.parseInt(st.nextToken());
			final int x = Integer.parseInt(st.nextToken());
			final int y = Integer.parseInt(st.nextToken());
			final int z = Integer.parseInt(st.nextToken());
			final int rangeXMin = Integer.parseInt(st.nextToken());
			final int rangeYMin = Integer.parseInt(st.nextToken());
			final int rangeZMin = Integer.parseInt(st.nextToken());
			final int rangeXMax = Integer.parseInt(st.nextToken());
			final int rangeYMax = Integer.parseInt(st.nextToken());
			final int rangeZMax = Integer.parseInt(st.nextToken());
			final int hp = Integer.parseInt(st.nextToken());
			final int pdef = Integer.parseInt(st.nextToken());
			final int mdef = Integer.parseInt(st.nextToken());
			boolean unlockable = false;
			if (st.hasMoreTokens())
				unlockable = Boolean.parseBoolean(st.nextToken());
			boolean autoOpen = false;
			if (st.hasMoreTokens())
				autoOpen = Boolean.parseBoolean(st.nextToken());
			if (rangeXMin > rangeXMax)
				LOG.error("Error in door data, ID:" + id);
			if (rangeYMin > rangeYMax)
				LOG.error("Error in door data, ID:" + id);
			if (rangeZMin > rangeZMax)
				LOG.error("Error in door data, ID:" + id);
			int collisionRadius; // (max) radius for movement checks
			if (rangeXMax - rangeXMin > rangeYMax - rangeYMin)
				collisionRadius = rangeYMax - rangeYMin;
			else
				collisionRadius = rangeXMax - rangeXMin;
			
			StatsSet npcDat = new StatsSet();
			npcDat.set("npcId", id);
			npcDat.set("level", 0);
			npcDat.set("jClass", "door");
			
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
			
			npcDat.set("collision_radius", collisionRadius);
			npcDat.set("collision_height", rangeZMax - rangeZMin);
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
			npcDat.set("name", name);
			npcDat.set("baseHpMax", hp);
			npcDat.set("baseHpReg", 3.e-3f);
			npcDat.set("baseMpReg", 3.e-3f);
			npcDat.set("basePDef", pdef);
			npcDat.set("baseMDef", mdef);
			
			L2CharTemplate template = new L2CharTemplate(npcDat);
			door = new L2DoorInstance(IdFactory.getInstance().getNextId(), template, id, name, unlockable);
			door.setRange(rangeXMin, rangeYMin, rangeZMin, rangeXMax, rangeYMax, rangeZMax);
			door.setCurrentHpMp(door.getMaxHp(), door.getMaxMp());
			door.setOpen(autoOpen);
			door.setXYZInvisible(x, y, z);
			door.setMapRegion(MapRegionTable.getInstance().getMapRegion(x, y));
		}
		catch (final Exception e)
		{
			LOG.error("Error in door data, at line: " + line, e);
		}
		return door;
	}
	
	public L2DoorInstance getDoor(Integer id)
	{
		return _staticItems.get(id);
	}
	
	public void putDoor(L2DoorInstance door)
	{
		_staticItems.put(door.getDoorId(), door);
		
		if (_regions.contains(door.getMapRegion()))
			_regions.get(door.getMapRegion()).add(door);
		else
		{
			final ArrayList<L2DoorInstance> region = new ArrayList<>();
			region.add(door);
			_regions.put(door.getMapRegion(), region);
		}
	}
	
	public L2DoorInstance[] getDoors()
	{
		L2DoorInstance[] _allTemplates = _staticItems.getValues(new L2DoorInstance[_staticItems.size()]);
		return _allTemplates;
	}
	
	/**
	 * Performs a check and sets up a scheduled task for those doors that require auto opening/closing.
	 */
	public void checkAutoOpen()
	{
		for (final L2DoorInstance doorInst : getDoors())
			// Garden of Eva (every 7 minutes)
			if (doorInst.getDoorName().startsWith("goe"))
			{
				doorInst.setAutoActionDelay(420000);
			}
			// Tower of Insolence (every 5 minutes)
			else if (doorInst.getDoorName().startsWith("aden_tower"))
			{
				doorInst.setAutoActionDelay(300000);
			}
			// Cruma Tower (every 20 minutes)
			else if (doorInst.getDoorName().startsWith("cruma"))
			{
				doorInst.setAutoActionDelay(1200000);
			}
	}
	
	public boolean checkIfDoorsBetween(final AbstractNodeLoc start, final AbstractNodeLoc end)
	{
		return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
	}
	
	public boolean checkIfDoorsBetween(final int x, final int y, final int z, final int tx, final int ty, final int tz)
	{
		int region;
		try
		{
			region = MapRegionTable.getInstance().getMapRegion(x, y);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			return false;
		}
		
		for (final L2DoorInstance doorInst : getDoors())
		{
			if (doorInst.getMapRegion() != region)
			{
				continue;
			}
			if (doorInst.getXMax() == 0)
			{
				continue;
			}
			
			// line segment goes through box
			// heavy approximation disabling some shooting angles especially near 2-piece doors
			// but most calculations should stop short
			// phase 1, x
			if (x <= doorInst.getXMax() && tx >= doorInst.getXMin() || tx <= doorInst.getXMax() && x >= doorInst.getXMin())
			{
				// phase 2, y
				if (y <= doorInst.getYMax() && ty >= doorInst.getYMin() || ty <= doorInst.getYMax() && y >= doorInst.getYMin())
				{
					// phase 3, basically only z remains but now we calculate it with another formula (by rage)
					// in some cases the direct line check (only) in the beginning isn't sufficient,
					// when char z changes a lot along the path
					if (doorInst.getStatus().getCurrentHp() > 0 && !doorInst.getOpen())
					{
						final int px1 = doorInst.getXMin();
						final int py1 = doorInst.getYMin();
						final int pz1 = doorInst.getZMin();
						final int px2 = doorInst.getXMax();
						final int py2 = doorInst.getYMax();
						final int pz2 = doorInst.getZMax();
						
						final int l = tx - x;
						final int m = ty - y;
						final int n = tz - z;
						
						int dk;
						
						if ((dk = (doorInst.getA() * l + doorInst.getB() * m + doorInst.getC() * n)) == 0)
						{
							continue; // Parallel
						}
							
						final float p = (float) (doorInst.getA() * x + doorInst.getB() * y + doorInst.getC() * z + doorInst.getD()) / (float) dk;
						
						final int fx = (int) (x - l * p);
						final int fy = (int) (y - m * p);
						final int fz = (int) (z - n * p);
						
						if ((Math.min(x, tx) <= fx && fx <= Math.max(x, tx)) && (Math.min(y, ty) <= fy && fy <= Math.max(y, ty)) && (Math.min(z, tz) <= fz && fz <= Math.max(z, tz)))
						{
							
							if (((fx >= px1 && fx <= px2) || (fx >= px2 && fx <= px1)) && ((fy >= py1 && fy <= py2) || (fy >= py2 && fy <= py1)) && ((fz >= pz1 && fz <= pz2) || (fz >= pz2 && fz <= pz1)))
							{
								return true; // Door between
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	private static class SingletonHolder
	{
		protected static final DoorTable _instance = new DoorTable();
	}
}

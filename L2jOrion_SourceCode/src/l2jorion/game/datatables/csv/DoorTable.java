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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.Config;
import l2jorion.game.geo.pathfinding.AbstractNodeLoc;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.templates.L2DoorTemplate;
import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.xml.IXmlReader;

public class DoorTable implements IXmlReader
{
	private static Logger LOG = LoggerFactory.getLogger(DoorTable.class);
	
	private static final Map<String, Set<Integer>> _groups = new HashMap<>();
	private final Map<Integer, L2DoorInstance> _doors = new HashMap<>();
	private final Map<Integer, ArrayList<L2DoorInstance>> _regions = new ConcurrentHashMap<>();
	
	public DoorTable()
	{
		load();
	}
	
	public void reloadAll()
	{
		respawn();
	}
	
	public void respawn()
	{
		_groups.clear();
		_doors.clear();
		load();
	}
	
	@Override
	public void load()
	{
		_doors.clear();
		_regions.clear();
		parseDatapackFile("data/xml/doors.xml");
		LOG.info("{}: Loaded {} Door templates for {} regions", getClass().getSimpleName(), _doors.size(), _regions.size());
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		for (Node a = doc.getFirstChild(); a != null; a = a.getNextSibling())
		{
			if ("list".equalsIgnoreCase(a.getNodeName()))
			{
				for (Node b = a.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("door".equalsIgnoreCase(b.getNodeName()))
					{
						final NamedNodeMap attrs = b.getAttributes();
						final StatsSet set = new StatsSet();
						
						set.set("baseHpMax", 1); // Avoid doors without HP value created dead due to default value 0 in L2CharTemplate
						
						for (int i = 0; i < attrs.getLength(); i++)
						{
							final Node att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						makeDoor(set);
					}
				}
			}
		}
	}
	
	public void insertCollisionData(StatsSet set)
	{
		int posX, posY, nodeX, nodeY, height;
		height = set.getInteger("height");
		
		String[] pos = set.getString("node1").split(",");
		nodeX = Integer.parseInt(pos[0]);
		nodeY = Integer.parseInt(pos[1]);
		
		pos = set.getString("node2").split(",");
		posX = Integer.parseInt(pos[0]);
		posY = Integer.parseInt(pos[1]);
		
		int collisionRadius; // (max) radius for movement checks
		collisionRadius = Math.min(Math.abs(nodeX - posX), Math.abs(nodeY - posY));
		if (collisionRadius < 20)
		{
			collisionRadius = 20;
		}
		
		set.set("collisionRadius", collisionRadius);
		set.set("collisionHeight", height);
	}
	
	private void makeDoor(StatsSet set)
	{
		insertCollisionData(set);
		L2DoorTemplate template = new L2DoorTemplate(set);
		
		L2DoorInstance door = new L2DoorInstance(IdFactory.getInstance().getNextId(), template, set.getInteger("id"), set.getString("name"));
		
		door.setCurrentHpMp(door.getMaxHp(), door.getMaxMp());
		door.spawnMe(template.getX(), template.getY(), template.getZ());
		
		putDoor(door, MapRegionTable.getInstance().getMapRegionLocId(door));
	}
	
	public void putDoor(L2DoorInstance door, int region)
	{
		_doors.put(door.getDoorId(), door);
		
		if (!_regions.containsKey(region))
		{
			_regions.put(region, new ArrayList<L2DoorInstance>());
		}
		_regions.get(region).add(door);
	}
	
	public L2DoorInstance getDoor(Integer id)
	{
		return _doors.get(id);
	}
	
	public static void addDoorGroup(String groupName, int doorId)
	{
		Set<Integer> set = _groups.get(groupName);
		if (set == null)
		{
			set = new HashSet<>();
			_groups.put(groupName, set);
		}
		set.add(doorId);
	}
	
	public static Set<Integer> getDoorsByGroup(String groupName)
	{
		return _groups.get(groupName);
	}
	
	public Collection<L2DoorInstance> getDoors()
	{
		return _doors.values();
	}
	
	public boolean checkIfDoorsBetween(final AbstractNodeLoc start, final AbstractNodeLoc end)
	{
		return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
	}
	
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz)
	{
		return checkIfDoorsBetween(x, y, z, tx, ty, tz, false);
	}
	
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, boolean doubleFaceCheck)
	{
		Collection<L2DoorInstance> allDoors;
		try
		{
			allDoors = _regions.get(MapRegionTable.getInstance().getMapRegionLocId(x, y));
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return false;
		}
		
		if (allDoors == null)
		{
			return false;
		}
		
		for (final L2DoorInstance doorInst : allDoors)
		{
			// check dead and open
			if (doorInst.isDead() || doorInst.getOpen() || !doorInst.checkCollision() || (doorInst.getX(0) == 0))
			{
				continue;
			}
			
			boolean intersectFace = false;
			for (int i = 0; i < 4; i++)
			{
				int j = (i + 1) < 4 ? i + 1 : 0;
				// lower part of the multiplier fraction, if it is 0 we avoid an error and also know that the lines are parallel
				int denominator = ((ty - y) * (doorInst.getX(i) - doorInst.getX(j))) - ((tx - x) * (doorInst.getY(i) - doorInst.getY(j)));
				if (denominator == 0)
				{
					continue;
				}
				
				// multipliers to the equations of the lines. If they are lower than 0 or bigger than 1, we know that segments don't intersect
				float multiplier1 = (float) (((doorInst.getX(j) - doorInst.getX(i)) * (y - doorInst.getY(i))) - ((doorInst.getY(j) - doorInst.getY(i)) * (x - doorInst.getX(i)))) / denominator;
				float multiplier2 = (float) (((tx - x) * (y - doorInst.getY(i))) - ((ty - y) * (x - doorInst.getX(i)))) / denominator;
				if ((multiplier1 >= 0) && (multiplier1 <= 1) && (multiplier2 >= 0) && (multiplier2 <= 1))
				{
					int intersectZ = Math.round(z + (multiplier1 * (tz - z)));
					// now checking if the resulting point is between door's min and max z
					if ((intersectZ > doorInst.getZMin()) && (intersectZ < doorInst.getZMax()))
					{
						if (!doubleFaceCheck || intersectFace)
						{
							return true;
						}
						intersectFace = true;
					}
				}
			}
		}
		return false;
	}
	
	public static DoorTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DoorTable _instance = new DoorTable();
	}
}

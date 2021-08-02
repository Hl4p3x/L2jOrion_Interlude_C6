/*
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
package l2jorion.game.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.datatables.sql.CharNameTable;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class L2World
{
	private static Logger LOG = LoggerFactory.getLogger(L2World.class);
	
	public static final int SHIFT_BY = 12;
	
	private static final int TILE_SIZE = 32768;
	
	public static final int TILE_X_MIN = 11;
	public static final int TILE_Y_MIN = 10;
	public static final int TILE_X_MAX = 26;
	public static final int TILE_Y_MAX = 26;
	public static final int TILE_ZERO_COORD_X = 20;
	public static final int TILE_ZERO_COORD_Y = 18;
	public static final int MAP_MIN_X = (TILE_X_MIN - TILE_ZERO_COORD_X) * TILE_SIZE;
	public static final int MAP_MIN_Y = (TILE_Y_MIN - TILE_ZERO_COORD_Y) * TILE_SIZE;
	
	public static final int MAP_MAX_X = ((TILE_X_MAX - TILE_ZERO_COORD_X) + 1) * TILE_SIZE;
	public static final int MAP_MAX_Y = ((TILE_Y_MAX - TILE_ZERO_COORD_Y) + 1) * TILE_SIZE;
	
	public static final int OFFSET_X = Math.abs(MAP_MIN_X >> SHIFT_BY);
	public static final int OFFSET_Y = Math.abs(MAP_MIN_Y >> SHIFT_BY);
	
	private static final int REGIONS_X = (MAP_MAX_X >> SHIFT_BY) + OFFSET_X;
	private static final int REGIONS_Y = (MAP_MAX_Y >> SHIFT_BY) + OFFSET_Y;
	
	private final Map<Integer, L2PcInstance> _allPlayers = new ConcurrentHashMap<>();
	private final Map<Integer, L2Object> _allObjects = new ConcurrentHashMap<>();
	private final Map<Integer, L2PetInstance> _petsInstance = new ConcurrentHashMap<>();
	
	private L2WorldRegion[][] _worldRegions;
	
	protected L2World()
	{
		initRegions();
	}
	
	public void storeObject(L2Object object)
	{
		_allObjects.put(object.getObjectId(), object);
	}
	
	public long timeStoreObject(L2Object object)
	{
		long time = System.currentTimeMillis();
		_allObjects.put(object.getObjectId(), object);
		time -= System.currentTimeMillis();
		return time;
	}
	
	public void removeObject(L2Object object)
	{
		_allObjects.remove(object.getObjectId());
	}
	
	public void removeObjects(List<L2Object> list)
	{
		for (L2Object o : list)
		{
			_allObjects.remove(o.getObjectId());
		}
	}
	
	public void removeObjects(L2Object[] objects)
	{
		for (L2Object o : objects)
		{
			_allObjects.remove(o.getObjectId());
		}
	}
	
	public long timeRemoveObject(L2Object object)
	{
		long time = System.currentTimeMillis();
		_allObjects.remove(object.getObjectId());
		time -= System.currentTimeMillis();
		return time;
	}
	
	public L2Object findObject(int oID)
	{
		return _allObjects.get(oID);
	}
	
	public long timeFindObject(int objectID)
	{
		long time = System.nanoTime();
		_allObjects.get(objectID);
		time = System.nanoTime() - time;
		return time;
	}
	
	public final Map<Integer, L2Object> getAllVisibleObjects()
	{
		return _allObjects;
	}
	
	public final int getAllVisibleObjectsCount()
	{
		return _allObjects.size();
	}
	
	public FastList<L2PcInstance> getAllGMs()
	{
		return GmListTable.getInstance().getAllGms(true);
	}
	
	public Map<Integer, L2PcInstance> getAllPlayers()
	{
		return _allPlayers;
	}
	
	public Collection<L2PcInstance> getPlayers()
	{
		return _allPlayers.values();
	}
	
	public int getAllPlayersCount()
	{
		return _allPlayers.size() + (Config.FAKE_ONLINE);
	}
	
	public L2PcInstance getPlayer(String name)
	{
		return getPlayer(CharNameTable.getInstance().getIdByName(name));
	}
	
	public L2PcInstance getPlayer(int playerObjId)
	{
		for (L2PcInstance actual : _allPlayers.values())
		{
			if (actual.getObjectId() == playerObjId)
			{
				return actual;
			}
		}
		return null;
	}
	
	public Collection<L2PetInstance> getAllPets()
	{
		return _petsInstance.values();
	}
	
	public L2PetInstance getPet(int ownerId)
	{
		return _petsInstance.get(Integer.valueOf(ownerId));
	}
	
	public L2PetInstance addPet(int ownerId, L2PetInstance pet)
	{
		return _petsInstance.put(Integer.valueOf(ownerId), pet);
	}
	
	public void removePet(int ownerId)
	{
		_petsInstance.remove(Integer.valueOf(ownerId));
	}
	
	public void removePet(L2PetInstance pet)
	{
		_petsInstance.values().remove(pet);
	}
	
	public void addVisibleObject(L2Object object, L2WorldRegion newRegion)
	{
		if (!newRegion.isActive())
		{
			return;
		}
		
		List<L2Object> visibles = getVisibleObjects(object);
		
		for (L2Object visible : visibles)
		{
			if (visible == null)
			{
				continue;
			}
			
			visible.getKnownList().addKnownObject(object);
			object.getKnownList().addKnownObject(visible);
		}
	}
	
	public void removeVisibleObject(L2Object object, L2WorldRegion oldRegion)
	{
		if (object == null)
		{
			return;
		}
		
		if (oldRegion != null)
		{
			oldRegion.removeVisibleObject(object);
			
			for (L2WorldRegion reg : oldRegion.getSurroundingRegions())
			{
				Collection<L2Object> vObj = reg.getVisibleObjects().values();
				for (L2Object obj : vObj)
				{
					if (obj != null)
					{
						obj.getKnownList().removeKnownObject(object);
						object.getKnownList().removeKnownObject(obj);
					}
				}
			}
			
			object.getKnownList().removeAllKnownObjects();
			
			if (object instanceof L2PcInstance)
			{
				if (!((L2PcInstance) object).isTeleporting())
				{
					removeFromAllPlayers((L2PcInstance) object);
				}
			}
		}
	}
	
	public void addPlayerToWorld(L2PcInstance cha)
	{
		_allPlayers.put(cha.getObjectId(), cha);
	}
	
	public void removeFromAllPlayers(L2PcInstance cha)
	{
		_allPlayers.remove(cha.getObjectId());
	}
	
	public Collection<L2Object> getVisibleObjects()
	{
		return _allObjects.values();
	}
	
	public List<L2Object> getVisibleObjects(L2Object object)
	{
		L2WorldRegion reg = object.getWorldRegion();
		if (reg == null)
		{
			return null;
		}
		
		List<L2Object> result = new ArrayList<>();
		
		for (L2WorldRegion regi : reg.getSurroundingRegions())
		{
			for (L2Object obj : regi.getVisibleObjects().values())
			{
				if (obj == null || obj.equals(object))
				{
					continue; // skip our own character
				}
				if (!obj.isVisible())
				{
					continue; // skip dying objects
				}
				
				result.add(obj);
			}
		}
		
		return result;
	}
	
	public List<L2Object> getVisibleObjects(L2Object object, int radius)
	{
		if (object == null || !object.isVisible())
		{
			return new ArrayList<>();
		}
		
		int x = object.getX();
		int y = object.getY();
		int sqRadius = radius * radius;
		
		List<L2Object> result = new ArrayList<>();
		
		for (L2WorldRegion regi : object.getWorldRegion().getSurroundingRegions())
		{
			for (L2Object obj : regi.getVisibleObjects().values())
			{
				if (obj == null || obj.equals(object))
				{
					continue; // skip our own character
				}
				
				int x1 = obj.getX();
				int y1 = obj.getY();
				
				double dx = x1 - x;
				double dy = y1 - y;
				
				if (dx * dx + dy * dy < sqRadius)
				{
					result.add(obj);
				}
			}
		}
		
		return result;
	}
	
	public List<L2Object> getObjectsAround(L2Object object, int radius)
	{
		if (object == null || !object.isVisible())
		{
			return new ArrayList<>();
		}
		
		int x = object.getX();
		int y = object.getY();
		int sqRadius = radius * radius;
		
		List<L2Object> result = new ArrayList<>();
		
		for (L2WorldRegion regi : object.getWorldRegion().getSurroundingRegions())
		{
			Collection<L2Object> vObj = regi.getVisibleObjects().values();
			{
				for (L2Object _object : vObj)
				{
					if (_object == null || _object.equals(object))
					{
						continue; // skip our own character
					}
					
					if (!(_object instanceof L2Attackable))
					{
						continue;
					}
					
					if (((L2Character) _object).isDead())
					{
						continue;
					}
					
					int x1 = _object.getX();
					int y1 = _object.getY();
					
					double dx = x1 - x;
					double dy = y1 - y;
					
					if (dx * dx + dy * dy < sqRadius / 15)
					{
						result.add(_object);
						if (result.size() == 0)
						{
							if (dx * dx + dy * dy < sqRadius / 5)
							{
								result.add(_object);
								if (result.size() == 0)
								{
									if (dx * dx + dy * dy < sqRadius / 2)
									{
										result.add(_object);
										if (result.size() == 0)
										{
											if (dx * dx + dy * dy < sqRadius)
											{
												result.add(_object);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		return result;
	}
	
	public List<L2Object> getPlayersAround(L2Object object, int radius)
	{
		if (object == null || !object.isVisible())
		{
			return new ArrayList<>();
		}
		
		int x = object.getX();
		int y = object.getY();
		int sqRadius = radius * radius;
		
		List<L2Object> result = new ArrayList<>();
		
		for (L2WorldRegion regi : object.getWorldRegion().getSurroundingRegions())
		{
			Collection<L2Object> vObj = regi.getVisibleObjects().values();
			{
				for (L2Object _object : vObj)
				{
					if (_object == null || _object.equals(object))
					{
						continue; // skip our own character
					}
					
					if (!(_object instanceof L2PcInstance))
					{
						continue;
					}
					
					if (((L2Character) _object).isDead())
					{
						continue;
					}
					
					int x1 = _object.getX();
					int y1 = _object.getY();
					
					double dx = x1 - x;
					double dy = y1 - y;
					
					if (dx * dx + dy * dy < sqRadius / 6)
					{
						result.add(_object);
						if (result.size() == 0)
						{
							if (dx * dx + dy * dy < sqRadius / 3)
							{
								result.add(_object);
								if (result.size() == 0)
								{
									if (dx * dx + dy * dy < sqRadius / 2)
									{
										result.add(_object);
										if (result.size() == 0)
										{
											if (dx * dx + dy * dy < sqRadius)
											{
												result.add(_object);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		return result;
	}
	
	public List<L2Object> getVisibleObjects3D(L2Object object, int radius)
	{
		if (object == null || !object.isVisible())
		{
			return new ArrayList<>();
		}
		
		int x = object.getX();
		int y = object.getY();
		int z = object.getZ();
		int sqRadius = radius * radius;
		
		// Create an FastList in order to contain all visible L2Object
		List<L2Object> result = new ArrayList<>();
		
		// Go through visible object of the selected region
		for (L2WorldRegion regi : object.getWorldRegion().getSurroundingRegions())
		{
			Collection<L2Object> vObj = regi.getVisibleObjects().values();
			{
				for (L2Object _object : vObj)
				{
					if (_object == null || _object.equals(object))
					{
						continue; // skip our own character
					}
					
					int x1 = _object.getX();
					int y1 = _object.getY();
					int z1 = _object.getZ();
					
					long dx = x1 - x;
					long dy = y1 - y;
					long dz = z1 - z;
					
					if (dx * dx + dy * dy + dz * dz < sqRadius)
					{
						result.add(_object);
					}
				}
			}
		}
		
		return result;
	}
	
	public List<L2PlayableInstance> getVisiblePlayable(L2Object object)
	{
		L2WorldRegion reg = object.getWorldRegion();
		
		if (reg == null)
		{
			return null;
		}
		
		// Create an FastList in order to contain all visible L2Object
		List<L2PlayableInstance> result = new ArrayList<>();
		
		// Go through the FastList of region
		for (L2WorldRegion regi : reg.getSurroundingRegions())
		{
			// Create an Iterator to go through the visible L2Object of the L2WorldRegion
			Map<Integer, L2PlayableInstance> _allpls = regi.getVisiblePlayable();
			Collection<L2PlayableInstance> _playables = _allpls.values();
			// Go through visible object of the selected region
			for (L2PlayableInstance _object : _playables)
			{
				if (_object == null || _object.equals(object))
				{
					continue; // skip our own character
				}
				
				if (!_object.isVisible()) // GM invisible is different than this...
				{
					continue; // skip dying objects
				}
				
				result.add(_object);
			}
			
		}
		
		return result;
	}
	
	public L2WorldRegion getRegion(Location point)
	{
		return _worldRegions[(point.getX() >> SHIFT_BY) + OFFSET_X][(point.getY() >> SHIFT_BY) + OFFSET_Y];
	}
	
	public L2WorldRegion getRegion(int x, int y)
	{
		return _worldRegions[(x >> SHIFT_BY) + OFFSET_X][(y >> SHIFT_BY) + OFFSET_Y];
	}
	
	public L2WorldRegion[][] getAllWorldRegions()
	{
		return _worldRegions;
	}
	
	private boolean validRegion(int x, int y)
	{
		return x >= 0 && x <= REGIONS_X && y >= 0 && y <= REGIONS_Y;
	}
	
	private void initRegions()
	{
		_worldRegions = new L2WorldRegion[REGIONS_X + 1][REGIONS_Y + 1];
		
		for (int i = 0; i <= REGIONS_X; i++)
		{
			for (int j = 0; j <= REGIONS_Y; j++)
			{
				_worldRegions[i][j] = new L2WorldRegion(i, j);
			}
		}
		
		for (int x = 0; x <= REGIONS_X; x++)
		{
			for (int y = 0; y <= REGIONS_Y; y++)
			{
				for (int a = -1; a <= 1; a++)
				{
					for (int b = -1; b <= 1; b++)
					{
						if (validRegion(x + a, y + b))
						{
							_worldRegions[x + a][y + b].addSurroundingRegion(_worldRegions[x][y]);
						}
					}
				}
			}
		}
		
		LOG.info("L2World: (" + REGIONS_X + "x" + REGIONS_Y + ") World Region Grid set up");
		
	}
	
	public synchronized void deleteVisibleNpcSpawns()
	{
		LOG.info("Deleting all visible NPC's.");
		
		for (int i = 0; i <= REGIONS_X; i++)
		{
			for (int j = 0; j <= REGIONS_Y; j++)
			{
				_worldRegions[i][j].deleteVisibleNpcSpawns();
			}
		}
		LOG.info("All visible NPC's deleted.");
	}
	
	public FastList<L2PcInstance> getAccountPlayers(String account_name)
	{
		
		FastList<L2PcInstance> players_for_account = new FastList<>();
		for (L2PcInstance actual : _allPlayers.values())
		{
			if (actual.getAccountName().equals(account_name))
			{
				players_for_account.add(actual);
			}
		}
		return players_for_account;
	}
	
	public static L2World getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final L2World _instance = new L2World();
	}
}

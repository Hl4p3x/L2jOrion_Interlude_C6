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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.datatables.sql.CharNameTable;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.util.Point3D;
public final class L2World
{
	private static Logger LOG = LoggerFactory.getLogger(L2World.class);

	// Geodata min/max tiles
public static final int SHIFT_BY = 12;
	
	private static final int TILE_SIZE = 32768;
	
	/** Map dimensions */
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
	
	/** calculated offset used so top left region is 0,0 */
	public static final int OFFSET_X = Math.abs(MAP_MIN_X >> SHIFT_BY);
	public static final int OFFSET_Y = Math.abs(MAP_MIN_Y >> SHIFT_BY);
	
	/** number of regions */
	private static final int REGIONS_X = (MAP_MAX_X >> SHIFT_BY) + OFFSET_X;
	private static final int REGIONS_Y = (MAP_MAX_Y >> SHIFT_BY) + OFFSET_Y;

	private Map<Integer, L2PcInstance> _allPlayers;
	private Map<Integer, L2Object> _allObjects;
	private Map<Integer, L2PetInstance> _petsInstance;
	
	private L2WorldRegion[][] _worldRegions;
	
	private L2World()
	{
		_allPlayers = new FastMap<Integer, L2PcInstance>().shared();
		_petsInstance = new FastMap<Integer, L2PetInstance>().shared();
		_allObjects = new FastMap<Integer, L2Object>().shared();
		
		initRegions();
	}
	
	public static L2World getInstance()
	{
		return SingletonHolder._instance;
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

	/**
	 * Remove L2Object object from _allObjects of L2World.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Delete item from inventory, tranfer Item from inventory to warehouse</li> <li>Crystallize item</li> <li>
	 * Remove NPC/PC/Pet from the world</li><BR>
	 * 
	 * @param object L2Object to remove from _allObjects of L2World
	 */
	public void removeObject(L2Object object)
	{
		_allObjects.remove(object.getObjectId());
	}

	/**
	 * Removes the objects.
	 *
	 * @param list the list
	 */
	public void removeObjects(List<L2Object> list)
	{
		for(L2Object o : list)
		{
			_allObjects.remove(o.getObjectId());
		}
	}

	/**
	 * Removes the objects.
	 *
	 * @param objects the objects
	 */
	public void removeObjects(L2Object[] objects)
	{
		for(L2Object o : objects)
		{
			_allObjects.remove(o.getObjectId());
		}
	}

	/**
	 * Time remove object.
	 *
	 * @param object the object
	 * @return the long
	 */
	public long timeRemoveObject(L2Object object)
	{
		long time = System.currentTimeMillis();
		_allObjects.remove(object.getObjectId());
		time -= System.currentTimeMillis();

		return time;
	}

	/**
	 * Return the L2Object object that belongs to an ID or null if no object found.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packets : Action, AttackRequest, RequestJoinParty, RequestJoinPledge...</li><BR>
	 *
	 * @param oID Identifier of the L2Object
	 * @return the l2 object
	 */
	public L2Object findObject(int oID)
	{
		return _allObjects.get(oID);
	}

	/**
	 * Time find object.
	 *
	 * @param objectID the object id
	 * @return the long
	 */
	public long timeFindObject(int objectID)
	{
		long time = System.nanoTime();
		_allObjects.get(objectID);
		time = System.nanoTime() - time;
		return time;
	}

	/**
	 * Added by Tempy - 08 Aug 05 Allows easy retrevial of all visible objects in world. -- do not use that fucntion,
	 * its unsafe!
	 *
	 * @return the all visible objects
	 */
	public final Map<Integer, L2Object> getAllVisibleObjects()
	{
		return _allObjects;
	}
	
	/**
	 * Get the count of all visible objects in world.<br>
	 * <br>
	 * 
	 * @return count off all L2World objects
	 */
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

	/**
	 * Return how many players are online.<BR>
	 * <BR>
	 * 
	 * @return number of online players.
	 */
	public int getAllPlayersCount()
	{
		return _allPlayers.size() + (Config.FAKE_ONLINE);
	}

	/**
	 * Return the player instance corresponding to the given name.
	 * @param name Name of the player to get Instance
	 * @return the player
	 */
	public L2PcInstance getPlayer(String name)
	{
		return getPlayer(CharNameTable.getInstance().getIdByName(name));
	}
	
	/**
	 * Gets the player.
	 *
	 * @param playerObjId the player obj id
	 * @return the player
	 */
	public L2PcInstance getPlayer(int playerObjId)
	{
		for (L2PcInstance actual:_allPlayers.values())
		{
			if (actual.getObjectId() == playerObjId)
			{
				return actual;
			}
		}
		return null;
	}

	/**
	 * Return a collection containing all pets in game.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Read-only, please! </B></FONT><BR>
	 * <BR>
	 *
	 * @return the all pets
	 */
	public Collection<L2PetInstance> getAllPets()
	{
		return _petsInstance.values();
	}

	/**
	 * Return the pet instance from the given ownerId.<BR>
	 * <BR>
	 *
	 * @param ownerId ID of the owner
	 * @return the pet
	 */
	public L2PetInstance getPet(int ownerId)
	{
		return _petsInstance.get(new Integer(ownerId));
	}

	/**
	 * Add the given pet instance from the given ownerId.<BR>
	 * <BR>
	 *
	 * @param ownerId ID of the owner
	 * @param pet L2PetInstance of the pet
	 * @return the l2 pet instance
	 */
	public L2PetInstance addPet(int ownerId, L2PetInstance pet)
	{
		return _petsInstance.put(new Integer(ownerId), pet);
	}

	/**
	 * Remove the given pet instance.<BR>
	 * <BR>
	 * 
	 * @param ownerId ID of the owner
	 */
	public void removePet(int ownerId)
	{
		_petsInstance.remove(new Integer(ownerId));
	}

	/**
	 * Remove the given pet instance.<BR>
	 * <BR>
	 * 
	 * @param pet the pet to remove
	 */
	public void removePet(L2PetInstance pet)
	{
		_petsInstance.values().remove(pet);
	}

	/**
	 * Add a L2Object in the world.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2Object (including L2PcInstance) are identified in <B>_visibleObjects</B> of his current L2WorldRegion and in
	 * <B>_knownObjects</B> of other surrounding L2Characters <BR>
	 * L2PcInstance are identified in <B>_allPlayers</B> of L2World, in <B>_allPlayers</B> of his current L2WorldRegion
	 * and in <B>_knownPlayer</B> of other surrounding L2Characters <BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Add the L2Object object in _allPlayers* of L2World</li> <li>Add the L2Object object in _gmList** of
	 * GmListTable</li> <li>Add object in _knownObjects and _knownPlayer* of all surrounding L2WorldRegion L2Characters</li>
	 * <BR>
	 * <li>If object is a L2Character, add all surrounding L2Object in its _knownObjects and all surrounding
	 * L2PcInstance in its _knownPlayer</li><BR>
	 * <I>* only if object is a L2PcInstance</I><BR>
	 * <I>** only if object is a GM L2PcInstance</I><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object in _visibleObjects and _allPlayers*
	 * of L2WorldRegion (need synchronisation)</B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _allObjects and _allPlayers* of
	 * L2World (need synchronisation)</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Drop an Item</li> <li>Spawn a L2Character</li> <li>Apply Death Penalty of a L2PcInstance</li><BR>
	 * <BR>
	 *
	 * @param object L2object to add in the world
	 * @param newRegion the new region
	 */
	public void addVisibleObject(L2Object object, L2WorldRegion newRegion)
	{
		if (object instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) object;
			
			if (!player.isTeleporting())
			{
				L2PcInstance tmp = _allPlayers.get(player.getObjectId());
				if (tmp != null)
				{
					LOG.warn("Duplicate character!? Closing both characters (" + player.getName() + ")");
					player.logout();
					tmp.logout();
					return;
				}
				
				_allPlayers.put(player.getObjectId(), player);
			}
		}
		
		if (!newRegion.isActive())
		{
			return;
		}
		
		List<L2Object> visibles = getVisibleObjects(object, 3000);
		
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
	

	/**
	 * Remove a L2Object from the world.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2Object (including L2PcInstance) are identified in <B>_visibleObjects</B> of his current L2WorldRegion and in
	 * <B>_knownObjects</B> of other surrounding L2Characters <BR>
	 * L2PcInstance are identified in <B>_allPlayers</B> of L2World, in <B>_allPlayers</B> of his current L2WorldRegion
	 * and in <B>_knownPlayer</B> of other surrounding L2Characters <BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2Object object from _allPlayers* of L2World</li> <li>Remove the L2Object object from
	 * _visibleObjects and _allPlayers* of L2WorldRegion</li> <li>Remove the L2Object object from _gmList** of
	 * GmListTable</li> <li>Remove object from _knownObjects and _knownPlayer* of all surrounding L2WorldRegion
	 * L2Characters</li><BR>
	 * <li>If object is a L2Character, remove all L2Object from its _knownObjects and all L2PcInstance from its
	 * _knownPlayer</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of
	 * L2World</B></FONT><BR>
	 * <BR>
	 * <I>* only if object is a L2PcInstance</I><BR>
	 * <I>** only if object is a GM L2PcInstance</I><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Pickup an Item</li> <li>Decay a L2Character</li><BR>
	 * <BR>
	 *
	 * @param object L2object to remove from the world
	 * @param oldRegion the old region
	 */
	public void removeVisibleObject(L2Object object, L2WorldRegion oldRegion)
	{
		if (object == null)
			return;
		
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
					removeFromAllPlayers((L2PcInstance) object);
			}
		}
	}

	/**
	 * Add the L2PcInstance to _allPlayers of L2World.
	 * @param cha the cha
	 */
	public void addToAllPlayers(L2PcInstance cha)
	{
		_allPlayers.put(cha.getObjectId(), cha);
	}

	/**
	 * Remove the L2PcInstance from _allPlayers of L2World.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Remove a player fom the visible objects</li><BR>
	 *
	 * @param cha the cha
	 */
	public void removeFromAllPlayers(L2PcInstance cha)
	{
		_allPlayers.remove(cha.getObjectId());
	}
	


	/**
	 * Return all visible objects of the L2WorldRegion object's and of its surrounding L2WorldRegion.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All visible object are identified in <B>_visibleObjects</B> of their current L2WorldRegion <BR>
	 * All surrounding L2WorldRegion are identified in <B>_surroundingRegions</B> of the selected L2WorldRegion in order
	 * to scan a large area around a L2Object<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Find Close Objects for L2Character</li><BR>
	 *
	 * @param object L2object that determine the current L2WorldRegion
	 * @return the visible objects
	 */
	public List<L2Object> getVisibleObjects(L2Object object)
	{
		L2WorldRegion reg = object.getWorldRegion();
		
		if (reg == null)
			return null;
		
		// Create an FastList in order to contain all visible L2Object
		List<L2Object> result = new ArrayList<>();
		
		// Go through the FastList of region
		for (L2WorldRegion regi : reg.getSurroundingRegions())
		{
			// Go through visible objects of the selected region
			Collection<L2Object> vObj = regi.getVisibleObjects().values();
			{
				for (L2Object _object : vObj)
				{
					if (_object == null || _object.equals(object))
						continue; // skip our own character
					if (!_object.isVisible())
						continue; // skip dying objects
					
					result.add(_object);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Return all visible objects of the L2WorldRegions in the circular area (radius) centered on the object.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All visible object are identified in <B>_visibleObjects</B> of their current L2WorldRegion <BR>
	 * All surrounding L2WorldRegion are identified in <B>_surroundingRegions</B> of the selected L2WorldRegion in order
	 * to scan a large area around a L2Object<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Define the aggrolist of monster</li> <li>Define visible objects of a L2Object</li> <li>Skill : Confusion...</li>
	 * <BR>
	 *
	 * @param object L2object that determine the center of the circular area
	 * @param radius Radius of the circular area
	 * @return the visible objects
	 */
	public static List<L2Object> getVisibleObjects(L2Object object, int radius)
	{
		if (object == null || !object.isVisible())
			return new ArrayList<>();
		
		int x = object.getX();
		int y = object.getY();
		int sqRadius = radius * radius;
		
		// Create an FastList in order to contain all visible L2Object
		List<L2Object> result = new ArrayList<>();
		
		// Go through the FastList of region
		for (L2WorldRegion regi : object.getWorldRegion().getSurroundingRegions())
		{
			// Go through visible objects of the selected region
			Collection<L2Object> vObj = regi.getVisibleObjects().values();
			{
				for (L2Object _object : vObj)
				{
					if (_object == null || _object.equals(object))
						continue; // skip our own character
						
					int x1 = _object.getX();
					int y1 = _object.getY();
					
					double dx = x1 - x;
					double dy = y1 - y;
					
					if (dx * dx + dy * dy < sqRadius)
						result.add(_object);
				}
			}
		}
		
		return result;
	}
	
	public static List<L2Object> getObjectsAround(L2Object object, int radius)
	{
		if (object == null || !object.isVisible())
			return new ArrayList<>();
		
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
						continue; // skip our own character
					
					if (!(_object instanceof L2Attackable))
						continue;
					
					if (((L2Character) _object).isDead())
						continue;
						
					int x1 = _object.getX();
					int y1 = _object.getY();
					
					double dx = x1 - x;
					double dy = y1 - y;
					
					if (dx * dx + dy * dy < sqRadius/15)
					{
						result.add(_object);
						if (result.size() == 0)
						{
							if (dx * dx + dy * dy < sqRadius/5)
							{
								result.add(_object);
								if (result.size() == 0)
								{
									if (dx * dx + dy * dy < sqRadius/2)
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
	
	public static List<L2Object> getPlayersAround(L2Object object, int radius)
	{
		if (object == null || !object.isVisible())
			return new ArrayList<>();
		
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
						continue; // skip our own character
					
					if (!(_object instanceof L2PcInstance))
						continue;
					
					if (((L2Character) _object).isDead())
						continue;
						
					int x1 = _object.getX();
					int y1 = _object.getY();
					
					double dx = x1 - x;
					double dy = y1 - y;
					
					if (dx * dx + dy * dy < sqRadius/6)
					{
						result.add(_object);
						if (result.size() == 0)
						{
							if (dx * dx + dy * dy < sqRadius/3)
							{
								result.add(_object);
								if (result.size() == 0)
								{
									if (dx * dx + dy * dy < sqRadius/2)
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

	/**
	 * Return all visible objects of the L2WorldRegions in the spheric area (radius) centered on the object.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All visible object are identified in <B>_visibleObjects</B> of their current L2WorldRegion <BR>
	 * All surrounding L2WorldRegion are identified in <B>_surroundingRegions</B> of the selected L2WorldRegion in order
	 * to scan a large area around a L2Object<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Define the target list of a skill</li> <li>Define the target list of a polearme attack</li><BR>
	 * <BR>
	 *
	 * @param object L2object that determine the center of the circular area
	 * @param radius Radius of the spheric area
	 * @return the visible objects3 d
	 */
	public List<L2Object> getVisibleObjects3D(L2Object object, int radius)
	{
		if (object == null || !object.isVisible())
			return new ArrayList<>();
		
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
						continue; // skip our own character
						
					int x1 = _object.getX();
					int y1 = _object.getY();
					int z1 = _object.getZ();
					
					long dx = x1 - x;
					long dy = y1 - y;
					long dz = z1 - z;
					
					if (dx * dx + dy * dy + dz * dz < sqRadius)
						result.add(_object);
				}
			}
		}
		
		return result;
	}
	/**
	 * Return all visible players of the L2WorldRegion object's and of its surrounding L2WorldRegion.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All visible object are identified in <B>_visibleObjects</B> of their current L2WorldRegion <BR>
	 * All surrounding L2WorldRegion are identified in <B>_surroundingRegions</B> of the selected L2WorldRegion in order
	 * to scan a large area around a L2Object<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Find Close Objects for L2Character</li><BR>
	 *
	 * @param object L2object that determine the current L2WorldRegion
	 * @return the visible playable
	 */
	public List<L2PlayableInstance> getVisiblePlayable(L2Object object)
	{
		L2WorldRegion reg = object.getWorldRegion();
		
		if (reg == null)
			return null;
		
		// Create an FastList in order to contain all visible L2Object
		List<L2PlayableInstance> result = new ArrayList<>();
		
		// Go through the FastList of region
		for (L2WorldRegion regi : reg.getSurroundingRegions())
		{
			// Create an Iterator to go through the visible L2Object of the L2WorldRegion
			Map<Integer, L2PlayableInstance> _allpls = regi.getVisiblePlayable();
			Collection<L2PlayableInstance> _playables = _allpls.values();
			// Go through visible object of the selected region
			//synchronized (_allpls)
			{
				for (L2PlayableInstance _object : _playables)
				{
					if (_object == null || _object.equals(object))
						continue; // skip our own character
						
					if (!_object.isVisible()) // GM invisible is different than this...
						continue; // skip dying objects
						
					result.add(_object);
				}
			}
		}
		
		return result;
	}
	/**
	 * Calculate the current L2WorldRegions of the object according to its position (x,y).<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Set position of a new L2Object (drop, spawn...)</li> <li>Update position of a L2Object after a mouvement</li><BR>
	 *
	 * @param point the point
	 * @return the region
	 */
	public L2WorldRegion getRegion(Point3D point)
	{
		return _worldRegions[(point.getX() >> SHIFT_BY) + OFFSET_X][(point.getY() >> SHIFT_BY) + OFFSET_Y];
	}

	/**
	 * Gets the region.
	 *
	 * @param x the x
	 * @param y the y
	 * @return the region
	 */
	public L2WorldRegion getRegion(int x, int y)
	{
		return _worldRegions[(x >> SHIFT_BY) + OFFSET_X][(y >> SHIFT_BY) + OFFSET_Y];
	}

	/**
	 * Returns the whole 2d array containing the world regions used by ZoneManager.java to setup zones inside the world
	 * regions
	 *
	 * @return the all world regions
	 */
	public L2WorldRegion[][] getAllWorldRegions()
	{
		return _worldRegions;
	}

	/**
	 * Check if the current L2WorldRegions of the object is valid according to its position (x,y).<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Init L2WorldRegions</li><BR>
	 * 
	 * @param x X position of the object
	 * @param y Y position of the object
	 * @return True if the L2WorldRegion is valid
	 */
	private boolean validRegion(int x, int y)
	{
		return x >= 0 && x <= REGIONS_X && y >= 0 && y <= REGIONS_Y;
	}

	/**
	 * Init each L2WorldRegion and their surrounding table.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All surrounding L2WorldRegion are identified in <B>_surroundingRegions</B> of the selected L2WorldRegion in order
	 * to scan a large area around a L2Object<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Constructor of L2World</li><BR>
	 */
	private void initRegions()
	{
		LOG.info("L2World: Setting up World Regions");

		_worldRegions = new L2WorldRegion[REGIONS_X + 1][REGIONS_Y + 1];

		for(int i = 0; i <= REGIONS_X; i++)
		{
			for(int j = 0; j <= REGIONS_Y; j++)
			{
				_worldRegions[i][j] = new L2WorldRegion(i, j);
			}
		}

		for(int x = 0; x <= REGIONS_X; x++)
		{
			for(int y = 0; y <= REGIONS_Y; y++)
			{
				for(int a = -1; a <= 1; a++)
				{
					for(int b = -1; b <= 1; b++)
					{
						if(validRegion(x + a, y + b))
						{
							_worldRegions[x + a][y + b].addSurroundingRegion(_worldRegions[x][y]);
						}
					}
				}
			}
		}

		LOG.info("L2World: ("+ REGIONS_X + "x" + REGIONS_Y +") World Region Grid set up.");

	}

	/**
	 * Deleted all spawns in the world.
	 */
	public synchronized void deleteVisibleNpcSpawns()
	{
		LOG.info("Deleting all visible NPC's.");

		for(int i = 0; i <= REGIONS_X; i++)
		{
			for(int j = 0; j <= REGIONS_Y; j++)
			{
				_worldRegions[i][j].deleteVisibleNpcSpawns();
			}
		}
		LOG.info("All visible NPC's deleted.");
	}
	
	/**
	 * Gets the account players.
	 *
	 * @param account_name the account_name
	 * @return the account players
	 */
	public FastList<L2PcInstance> getAccountPlayers(String account_name)
	{
		
		FastList<L2PcInstance> players_for_account = new FastList<>();
		for(L2PcInstance actual:_allPlayers.values())
		{
			if(actual.getAccountName().equals(account_name))
				players_for_account.add(actual);
		}
		return players_for_account;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final L2World _instance = new L2World();
	}
}

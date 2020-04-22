/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.model.zone;

import java.util.List;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.L2GameServerPacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * Abstract base class for any zone type Handles basic operations
 * @author durgus
 */
public abstract class L2ZoneType
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2ZoneType.class);
	private final int _id;
	protected List<L2ZoneForm> _zone;
	public FastMap<Integer, L2Character> _characterList;
	protected FastMap<Integer, Integer> _zones;
	
	/** Parameters to affect specific characters */
	private boolean _checkAffected;
	
	private int _minLvl;
	private int _maxLvl;
	private int[] _race;
	private int[] _class;
	private char _classType;
	
	protected L2ZoneType(final int id)
	{
		_id = id;
		_characterList = new FastMap<Integer, L2Character>().shared();
		
		_checkAffected = false;
		
		_minLvl = 0;
		_maxLvl = 0xFF;
		
		_classType = 0;
		
		_race = null;
		_class = null;
	}
	
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Setup new parameters for this zone
	 * @param name
	 * @param value
	 */
	public void setParameter(String name, String value)
	{
		_checkAffected = true;
		
		// Minimum level
		switch (name)
		{
			case "affectedLvlMin":
				_minLvl = Integer.parseInt(value);
				break;
			// Maximum level
			case "affectedLvlMax":
				_maxLvl = Integer.parseInt(value);
				break;
			// Affected Races
			case "affectedRace":
				// Create a new array holding the affected race
				if (_race == null)
				{
					_race = new int[1];
					_race[0] = Integer.parseInt(value);
				}
				else
				{
					final int[] temp = new int[_race.length + 1];
					
					int i = 0;
					
					for (; i < _race.length; i++)
					{
						temp[i] = _race[i];
					}
					
					temp[i] = Integer.parseInt(value);
					
					_race = temp;
				}
				break;
			// Affected classes
			case "affectedClassId":
				// Create a new array holding the affected classIds
				if (_class == null)
				{
					_class = new int[1];
					_class[0] = Integer.parseInt(value);
				}
				else
				{
					final int[] temp = new int[_class.length + 1];
					
					int i = 0;
					
					for (; i < _class.length; i++)
					{
						temp[i] = _class[i];
					}
					
					temp[i] = Integer.parseInt(value);
					
					_class = temp;
				}
				break;
			// Affected class type
			case "affectedClassType":
				if (value.equals("Fighter"))
				{
					_classType = 1;
				}
				else
				{
					_classType = 2;
				}
				break;
		}
	}
	
	public void setSpawnLocs(Node node1)
	{
	}
	
	/**
	 * Checks if the given character is affected by this zone
	 * @param character
	 * @return
	 */
	private boolean isAffected(L2Character character)
	{
		// Check lvl
		if (character.getLevel() < _minLvl || character.getLevel() > _maxLvl)
			return false;
		
		if (character instanceof L2PcInstance)
		{
			// Check class type
			if (_classType != 0)
			{
				if (((L2PcInstance) character).isMageClass())
				{
					if (_classType == 1)
						return false;
				}
				else if (_classType == 2)
					return false;
			}
			
			// Check race
			if (_race != null)
			{
				boolean ok = false;
				
				for (final int element : _race)
				{
					if (((L2PcInstance) character).getRace().ordinal() == element)
					{
						ok = true;
						break;
					}
				}
				
				if (!ok)
					return false;
			}
			
			// Check class
			if (_class != null)
			{
				boolean ok = false;
				
				for (final int clas : _class)
				{
					if (((L2PcInstance) character).getClassId().ordinal() == clas)
					{
						ok = true;
						break;
					}
				}
				
				if (!ok)
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Set the zone for this L2ZoneType Instance
	 * @param zone
	 */
	public void setZone(L2ZoneForm zone)
	{
		getZones().add(zone);
	}
	
	/**
	 * Returns this zones zone form
	 * @return
	 */
	public L2ZoneForm getZone()
	{
		for (L2ZoneForm zone : getZones())
		{
			return zone;
		}
		return null;
	}
	
	public final List<L2ZoneForm> getZones()
	{
		if (_zone == null)
			_zone = new FastList<>();
		return _zone;
	}

	public boolean isInsideZone(int x, int y)
	{
		for (L2ZoneForm zone : getZones())
		{
			if (zone.isInsideZone(x, y, zone.getHighZ()))
				return true;
		}
		return false;
	}
	/**
	 * Checks if the given coordinates are within the zone
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public boolean isInsideZone(int x, int y, int z)
	{
		for (L2ZoneForm zone : getZones())
		{
			if (zone.isInsideZone(x, y, z))
				return true;
		}
		return false;
	}
	
	/**
	 * Checks if the given object is inside the zone.
	 * @param object
	 * @return
	 */
	public boolean isInsideZone(L2Object object)
	{
		return isInsideZone(object.getX(), object.getY(), object.getZ());
	}
	
	public double getDistanceToZone(int x, int y)
	{
		return getZone().getDistanceToZone(x, y);
	}
	
	public double getDistanceToZone(L2Object object)
	{
		return getZone().getDistanceToZone(object.getX(), object.getY());
	}
	
	public void revalidateInZone(L2Character character)
	{
		// If the character can't be affected by this zone return
		if (_checkAffected)
		{
			if (!isAffected(character))
				return;
		}
		
		if (Config.ZONE_DEBUG && character instanceof L2PcInstance && ((L2PcInstance) character).isGM())
		{
			
			LOG.debug("ZONE: Character " + character.getName() + " has coords: ");
			LOG.debug("ZONE: 	X: " + character.getX());
			LOG.debug("ZONE: 	Y: " + character.getY());
			LOG.debug("ZONE: 	Z: " + character.getZ());
			LOG.debug("ZONE:  -  is inside zone " + _id + "?: " + isInsideZone(character.getX(), character.getY(), character.getZ()));
			
		}
		// If the object is inside the zone...
		if (isInsideZone(character.getX(), character.getY(), character.getZ()))
		{
			// Was the character not yet inside this zone?
			if (!_characterList.containsKey(character.getObjectId()))
			{
				_characterList.put(character.getObjectId(), character);
				onEnter(character);
			}
		}
		else
		{
			// Was the character inside this zone?
			if (_characterList.containsKey(character.getObjectId()))
			{
				if (Config.ZONE_DEBUG && character instanceof L2PcInstance && character.getName() != null)
					LOG.debug("ZONE: " + "Character " + character.getName() + " removed from zone.");
				_characterList.remove(character.getObjectId());
				onExit(character);
			}
		}
		
		if (Config.ZONE_DEBUG)
		{
			for (L2Character actual : _characterList.values())
			{
				if (actual instanceof L2PcInstance)
					LOG.debug("ZONE:	 -  " + actual.getName() + " is inside zone " + _id);
			}
		}
		
	}
	
	/**
	 * Force fully removes a character from the zone Should use during teleport / logoff
	 * @param character
	 */
	public void removeCharacter(L2Character character)
	{
		if (_characterList.containsKey(character.getObjectId()))
		{
			_characterList.remove(character.getObjectId());
			onExit(character);
		}
	}
	
	/**
	 * Will scan the zones char list for the character
	 * @param character
	 * @return
	 */
	public boolean isCharacterInZone(L2Character character)
	{
		// re validate zone is not always performed, so better both checks
		if (character != null)
		{
			return _characterList.containsKey(character.getObjectId()) || isInsideZone(character.getX(), character.getY(), character.getZ());
		}
		
		return false;
		
	}
	
	protected abstract void onEnter(L2Character character);
	
	protected abstract void onExit(L2Character character);
	
	public abstract void onDieInside(L2Character character);
	
	public abstract void onReviveInside(L2Character character);
	
	/**
	 * Broadcasts packet to all players inside the zone
	 * @param packet
	 */
	public void broadcastPacket(L2GameServerPacket packet)
	{
		if (_characterList.isEmpty())
			return;
		
		for (final L2Character character : _characterList.values())
		{
			if (character instanceof L2PcInstance)
				character.sendPacket(packet);
		}
	}
	
	public FastMap<Integer, L2Character> getCharactersInside()
	{
		return _characterList;
	}
	
}

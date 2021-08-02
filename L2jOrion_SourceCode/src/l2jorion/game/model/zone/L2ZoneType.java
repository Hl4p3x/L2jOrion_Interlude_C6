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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Node;

import javolution.util.FastMap;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public abstract class L2ZoneType
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2ZoneType.class);
	
	private final int _id;
	protected L2ZoneForm _zone;
	public FastMap<Integer, L2Character> _characterList;
	protected FastMap<Integer, Integer> _zones;
	private String _name = null;
	private boolean _enabled;
	private AbstractZoneSettings _settings;
	
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
		{
			return false;
		}
		
		if (character instanceof L2PcInstance)
		{
			// Check class type
			if (_classType != 0)
			{
				if (((L2PcInstance) character).isMageClass())
				{
					if (_classType == 1)
					{
						return false;
					}
				}
				else if (_classType == 2)
				{
					return false;
				}
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
				{
					return false;
				}
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
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public void setZone(L2ZoneForm zone)
	{
		if (_zone != null)
		{
			throw new IllegalStateException("Zone already set");
		}
		_zone = zone;
	}
	
	/**
	 * Returns this zones zone form
	 * @return
	 */
	public L2ZoneForm getZone()
	{
		return _zone;
	}
	
	public boolean isInsideZone(int x, int y)
	{
		return _zone.isInsideZone(x, y, _zone.getHighZ());
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
		return _zone.isInsideZone(x, y, z);
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
		if (_checkAffected)
		{
			if (!isAffected(character))
			{
				return;
			}
		}
		
		if (isInsideZone(character))
		{
			if (!_characterList.containsKey(character.getObjectId()))
			{
				_characterList.put(character.getObjectId(), character);
				onEnter(character);
			}
		}
		else
		{
			if (_characterList.containsKey(character.getObjectId()))
			{
				_characterList.remove(character.getObjectId());
				onExit(character);
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
		{
			return;
		}
		
		for (final L2Character character : _characterList.values())
		{
			if (character instanceof L2PcInstance)
			{
				character.sendPacket(packet);
			}
		}
	}
	
	public Collection<L2Character> getCharactersInside()
	{
		return _characterList.values();
	}
	
	public List<L2PcInstance> getPlayersInside()
	{
		List<L2PcInstance> players = new ArrayList<>();
		for (L2Character ch : _characterList.values())
		{
			if ((ch != null) && ch instanceof L2PcInstance)
			{
				players.add(ch.getActingPlayer());
			}
		}
		
		return players;
	}
	
	@SuppressWarnings("unchecked")
	public final <A> List<A> getKnownTypeInside(Class<A> type)
	{
		List<A> result = new ArrayList<>();
		
		for (L2Object obj : _characterList.values())
		{
			if (type.isAssignableFrom(obj.getClass()))
			{
				result.add((A) obj);
			}
		}
		return result;
	}
	
	public AbstractZoneSettings getSettings()
	{
		return _settings;
	}
	
	public void setSettings(AbstractZoneSettings settings)
	{
		if (_settings != null)
		{
			_settings.clear();
		}
		_settings = settings;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setEnabled(boolean state)
	{
		_enabled = state;
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
}

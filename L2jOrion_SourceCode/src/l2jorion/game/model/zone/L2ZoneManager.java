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

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class L2ZoneManager
{
	private final Logger LOG = LoggerFactory.getLogger(L2ZoneManager.class);
	
	private final FastList<L2ZoneType> _zones;
	
	/**
	 * The Constructor creates an initial zone list use registerNewZone() / unregisterZone() to change the zone list
	 */
	public L2ZoneManager()
	{
		_zones = new FastList<>();
	}
	
	/**
	 * Register a new zone object into the manager
	 * @param zone
	 */
	public void registerNewZone(L2ZoneType zone)
	{
		_zones.add(zone);
	}
	
	/**
	 * Unregister a given zone from the manager (e.g. dynamic zones)
	 * @param zone
	 */
	public void unregisterZone(L2ZoneType zone)
	{
		_zones.remove(zone);
	}
	
	public void revalidateZones(L2Character character)
	{
		if (Config.ZONE_DEBUG && character != null && character instanceof L2PcInstance && character.getName() != null)
		{
			LOG.debug("ZONE: Revalidating Zone for character: " + character.getName());
		}
		
		for (L2ZoneType e : _zones)
		{
			if (e != null)
			{
				e.revalidateInZone(character);
			}
		}
	}
	
	public void removeCharacter(L2Character character)
	{
		for (final L2ZoneType e : _zones)
		{
			if (e != null)
			{
				e.removeCharacter(character);
			}
		}
	}
	
	public void onDeath(L2Character character)
	{
		for (L2ZoneType e : _zones)
		{
			if (e != null)
			{
				e.onDieInside(character);
			}
		}
	}
	
	public void onRevive(L2Character character)
	{
		for (L2ZoneType e : _zones)
		{
			if (e != null)
			{
				e.onReviveInside(character);
			}
		}
	}
	
	/**
	 * @return
	 */
	public FastList<L2ZoneType> getZones()
	{
		return _zones;
	}
}

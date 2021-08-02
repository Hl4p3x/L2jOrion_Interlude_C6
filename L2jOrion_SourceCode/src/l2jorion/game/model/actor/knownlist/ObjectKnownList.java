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
package l2jorion.game.model.actor.knownlist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.util.Util;

public class ObjectKnownList
{
	protected L2Object _activeObject;
	
	private Map<Integer, L2Object> _knownObjects;
	
	public ObjectKnownList(L2Object activeObject)
	{
		_activeObject = activeObject;
	}
	
	public boolean addKnownObject(L2Object object)
	{
		if (object == null)
		{
			return false;
		}
		
		if (knowsObject(object))
		{
			return false;
		}
		
		if (!Util.checkIfInRange(getDistanceToWatchObject(object), getActiveObject(), object, true))
		{
			return false;
		}
		
		return getKnownObjects().put(object.getObjectId(), object) == null;
	}
	
	public final boolean knowsObject(final L2Object object)
	{
		if (object == null)
		{
			return false;
		}
		
		return getActiveObject() == object || getKnownObjects().containsKey(object.getObjectId());
	}
	
	public void removeAllKnownObjects()
	{
		getKnownObjects().clear();
	}
	
	public boolean removeKnownObject(L2Object object)
	{
		if (object == null)
		{
			return false;
		}
		
		return getKnownObjects().remove(object.getObjectId()) != null;
	}
	
	public final void findObjects()
	{
		final L2WorldRegion region = getActiveObject().getWorldRegion();
		if (region == null)
		{
			return;
		}
		
		if (getActiveObject() instanceof L2PlayableInstance)
		{
			for (L2WorldRegion regi : region.getSurroundingRegions()) // offer members of this and surrounding regions
			{
				Collection<L2Object> vObj = regi.getVisibleObjects().values();
				for (L2Object _object : vObj)
				{
					if (_object != getActiveObject())
					{
						addKnownObject(_object);
						if (_object instanceof L2Character)
						{
							_object.getKnownList().addKnownObject(getActiveObject());
						}
					}
				}
			}
		}
		else if (getActiveObject() instanceof L2Character)
		{
			for (L2WorldRegion regi : region.getSurroundingRegions())
			{
				if (regi.isActive())
				{
					Collection<L2PlayableInstance> vPls = regi.getVisiblePlayable().values();
					for (L2Object _object : vPls)
					{
						if (_object != getActiveObject())
						{
							addKnownObject(_object);
						}
					}
				}
			}
		}
	}
	
	public void forgetObjects(boolean fullCheck)
	{
		Collection<L2Object> knownObjects = getKnownObjects().values();
		for (L2Object object : knownObjects)
		{
			if (object == null)
			{
				continue;
			}
			
			if (object instanceof L2Character)
			{
				if (!fullCheck && !((L2Character) object).isDead())
				{
					continue;
				}
			}
			
			if (!object.isVisible() || !Util.checkIfInRange(getDistanceToForgetObject(object), getActiveObject(), object, true))
			{
				removeKnownObject(object);
			}
		}
	}
	
	public L2Object getActiveObject()
	{
		return _activeObject;
	}
	
	public int getDistanceToForgetObject(final L2Object object)
	{
		return 0;
	}
	
	public int getDistanceToWatchObject(final L2Object object)
	{
		return 0;
	}
	
	public final Map<Integer, L2Object> getKnownObjects()
	{
		if (_knownObjects == null)
		{
			_knownObjects = new FastMap<Integer, L2Object>().shared();
		}
		return _knownObjects;
	}
	
	@SuppressWarnings("unchecked")
	public final <A> Collection<A> getKnownType(Class<A> type)
	{
		List<A> result = new ArrayList<>();
		
		for (L2Object obj : getKnownObjects().values())
		{
			if (type.isAssignableFrom(obj.getClass()))
			{
				result.add((A) obj);
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public final <A> Collection<A> getKnownTypeInRadius(Class<A> type, int radius)
	{
		List<A> result = new ArrayList<>();
		
		for (L2Object obj : getKnownObjects().values())
		{
			if (type.isAssignableFrom(obj.getClass()) && Util.checkIfInRange(radius, getActiveObject(), obj, true))
			{
				result.add((A) obj);
			}
		}
		return result;
	}
}
/*
 * $Header$
 *
 * $Author$
 * $Date$
 * $Revision$
 * $Log$
 * Revision 1.1.2.1  2005/04/08 08:03:40  luisantonioa
 * *** empty LOGGER message ***
 *
 * Revision 1.1  4/04/2005 17:15:07  luisantonioa
 * Created New Class
 *
 *
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
package l2jorion.game.datatables;

import java.util.Map;

import javolution.util.FastMap;
import l2jorion.game.model.L2Object;

/**
 * This class ...
 * @version $Revision$ $Date$
 */

public class DesireTable
{
	public static final DesireType[] DEFAULT_DESIRES =
	{
		DesireType.FEAR,
		DesireType.DISLIKE,
		DesireType.HATE,
		DesireType.DAMAGE
	};
	
	public enum DesireType
	{
		FEAR,
		DISLIKE,
		HATE,
		DAMAGE
	}
	
	class DesireValue
	{
		private float _value;
		
		DesireValue()
		{
			this(0f);
		}
		
		DesireValue(final Float pValue)
		{
			_value = pValue;
		}
		
		public void addValue(final float pValue)
		{
			_value += pValue;
		}
		
		public float getValue()
		{
			return _value;
		}
	}
	
	class Desires
	{
		private final Map<DesireType, DesireValue> _desireTable;
		
		public Desires(final DesireType... desireList)
		{
			_desireTable = new FastMap<>();
			
			for (final DesireType desire : desireList)
			{
				_desireTable.put(desire, new DesireValue());
			}
		}
		
		public DesireValue getDesireValue(final DesireType type)
		{
			return _desireTable.get(type);
		}
		
		public void addValue(final DesireType type, final float value)
		{
			DesireValue temp = getDesireValue(type);
			
			if (temp != null)
			{
				temp.addValue(value);
			}
			
			temp = null;
		}
		
		public void createDesire(final DesireType type)
		{
			_desireTable.put(type, new DesireValue());
		}
		
		public void deleteDesire(final DesireType type)
		{
			_desireTable.remove(type);
		}
	}
	
	private final Map<L2Object, Desires> _objectDesireTable;
	private final Desires _generalDesires;
	private final DesireType[] _desireTypes;
	
	public DesireTable(final DesireType... desireList)
	{
		_desireTypes = desireList;
		_objectDesireTable = new FastMap<>();
		_generalDesires = new Desires(_desireTypes);
	}
	
	public float getDesireValue(final DesireType type)
	{
		return _generalDesires.getDesireValue(type).getValue();
	}
	
	public float getDesireValue(final L2Object object, final DesireType type)
	{
		final Desires desireList = _objectDesireTable.get(object);
		
		if (desireList == null)
			return 0f;
		
		return desireList.getDesireValue(type).getValue();
	}
	
	public void addDesireValue(final DesireType type, final float value)
	{
		_generalDesires.addValue(type, value);
	}
	
	public void addDesireValue(final L2Object object, final DesireType type, final float value)
	{
		Desires desireList = _objectDesireTable.get(object);
		
		if (desireList != null)
		{
			desireList.addValue(type, value);
		}
		
		desireList = null;
	}
	
	public void createDesire(final DesireType type)
	{
		_generalDesires.createDesire(type);
	}
	
	public void deleteDesire(final DesireType type)
	{
		_generalDesires.deleteDesire(type);
	}
	
	public void createDesire(final L2Object object, final DesireType type)
	{
		Desires desireList = _objectDesireTable.get(object);
		
		if (desireList != null)
		{
			desireList.createDesire(type);
		}
		
		desireList = null;
	}
	
	public void deleteDesire(final L2Object object, final DesireType type)
	{
		Desires desireList = _objectDesireTable.get(object);
		
		if (desireList != null)
		{
			desireList.deleteDesire(type);
		}
		
		desireList = null;
	}
	
	public void addKnownObject(final L2Object object)
	{
		if (object != null)
		{
			addKnownObject(object, DesireType.DISLIKE, DesireType.FEAR, DesireType.DAMAGE, DesireType.HATE);
		}
	}
	
	public void addKnownObject(final L2Object object, final DesireType... desireList)
	{
		if (object != null)
		{
			_objectDesireTable.put(object, new Desires(desireList));
		}
	}
}

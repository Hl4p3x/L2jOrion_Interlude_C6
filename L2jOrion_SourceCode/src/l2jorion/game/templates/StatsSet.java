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
package l2jorion.game.templates;

import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class StatsSet
{
	private final Logger LOG = LoggerFactory.getLogger(StatsSet.class);
	private final Map<String, Object> _set = new FastMap<>();
	
	public final Map<String, Object> getSet()
	{
		return _set;
	}
	
	/**
	 * Add a set of couple values in the current set
	 * @param newSet : StatsSet pointing out the list of couples to add in the current set
	 */
	public synchronized void add(final StatsSet newSet)
	{
		final Map<String, Object> newMap = newSet.getSet();
		for (final String key : newMap.keySet())
		{
			final Object value = newMap.get(key);
			_set.put(key, value);
		}
	}
	
	/**
	 * Return the boolean associated to the key put in parameter ("name")
	 * @param name : String designating the key in the set
	 * @return boolean : value associated to the key
	 */
	public synchronized boolean getBool(final String name)
	{
		final Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Boolean value required, but not specified");
		if (val instanceof Boolean)
			return ((Boolean) val).booleanValue();
		try
		{
			return Boolean.parseBoolean((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Boolean value required, but found: " + val);
		}
	}
	
	/**
	 * Return the boolean associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : boolean designating the default value if value associated with the key is null
	 * @return boolean : value of the key
	 */
	public synchronized boolean getBool(final String name, final boolean deflt)
	{
		final Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Boolean)
			return ((Boolean) val).booleanValue();
		try
		{
			return Boolean.parseBoolean((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Boolean value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the int associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : byte designating the default value if value associated with the key is null
	 * @return byte : value associated to the key
	 */
	public synchronized byte getByte(final String name, final byte deflt)
	{
		final Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).byteValue();
		try
		{
			return Byte.parseByte((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Byte value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the byte associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return byte : value associated to the key
	 */
	public synchronized byte getByte(final String name)
	{
		final Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Byte value required, but not specified");
		if (val instanceof Number)
			return ((Number) val).byteValue();
		try
		{
			return Byte.parseByte((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Byte value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the short associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : short designating the default value if value associated with the key is null
	 * @return short : value associated to the key
	 */
	public synchronized short getShort(final String name, final short deflt)
	{
		final Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).shortValue();
		try
		{
			return Short.parseShort((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Short value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the short associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return short : value associated to the key
	 */
	public synchronized short getShort(final String name)
	{
		final Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Short value required, but not specified");
		if (val instanceof Number)
			return ((Number) val).shortValue();
		try
		{
			return Short.parseShort((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Short value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the int associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return int : value associated to the key
	 */
	public synchronized int getInteger(final String name)
	{
		final Object val = _set.get(name);
		if (val == null)
		{
			
			throw new IllegalArgumentException("Integer value required, but not specified");
			
		}
		if (val instanceof Number)
		{
			return ((Number) val).intValue();
		}
		try
		{
			return Integer.parseInt((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the int associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : int designating the default value if value associated with the key is null
	 * @return int : value associated to the key
	 */
	public synchronized int getInteger(final String name, final int deflt)
	{
		final Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).intValue();
		try
		{
			return Integer.parseInt((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the int[] associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter deflt.
	 * @param name : String designating the key in the set
	 * @return int[] : value associated to the key
	 */
	public synchronized int[] getIntegerArray(final String name)
	{
		final Object val = _set.get(name);
		if (val == null)
			throw new RuntimeException();
		// throw new IllegalArgumentException("Integer value required, but not specified");
		if (val instanceof Number)
		{
			final int[] result =
			{
				((Number) val).intValue()
			};
			return result;
		}
		int c = 0;
		final String[] vals = ((String) val).split(";");
		final int[] result = new int[vals.length];
		for (final String v : vals)
		{
			try
			{
				result[c] = Integer.parseInt(v);
				c++;
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					e.printStackTrace();
				
				throw new IllegalArgumentException("Integer value required, but found: " + val);
			}
		}
		return result;
	}
	
	/**
	 * Returns the long associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return long : value associated to the key
	 */
	public synchronized long getLong(final String name)
	{
		final Object val = _set.get(name);
		if (val == null)
			throw new RuntimeException();
		// throw new IllegalArgumentException("Integer value required, but not specified");
		if (val instanceof Number)
			return ((Number) val).longValue();
		try
		{
			return Long.parseLong((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the long associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : long designating the default value if value associated with the key is null
	 * @return long : value associated to the key
	 */
	public synchronized long getLong(final String name, final int deflt)
	{
		final Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).longValue();
		try
		{
			return Long.parseLong((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the float associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return float : value associated to the key
	 */
	public synchronized float getFloat(final String name)
	{
		final Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Float value required, but not specified");
		if (val instanceof Number)
			return ((Number) val).floatValue();
		try
		{
			return (float) Double.parseDouble((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the float associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : float designating the default value if value associated with the key is null
	 * @return float : value associated to the key
	 */
	public synchronized float getFloat(final String name, final float deflt)
	{
		final Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).floatValue();
		try
		{
			return (float) Double.parseDouble((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the double associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return double : value associated to the key
	 */
	public synchronized double getDouble(final String name)
	{
		final Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Float value required, but not specified");
		if (val instanceof Number)
			return ((Number) val).doubleValue();
		try
		{
			return Double.parseDouble((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the double associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : float designating the default value if value associated with the key is null
	 * @return double : value associated to the key
	 */
	public synchronized double getDouble(final String name, final float deflt)
	{
		final Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).doubleValue();
		try
		{
			return Double.parseDouble((String) val);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the String associated to the key put in parameter ("name").
	 * @param name : String designating the key in the set
	 * @return String : value associated to the key
	 */
	public synchronized String getString(final String name)
	{
		final Object val = _set.get(name);
		/*if (val == null)
			throw new IllegalArgumentException("String value required, but not specified");*/
		return String.valueOf(val);
	}
	
	/**
	 * Returns the String associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter deflt.
	 * @param name : String designating the key in the set
	 * @param deflt : String designating the default value if value associated with the key is null
	 * @return String : value associated to the key
	 */
	public synchronized String getString(final String name, final String deflt)
	{
		final Object val = _set.get(name);
		if (val == null)
			return deflt;
		return String.valueOf(val);
	}
	
	/**
	 * Returns an enumeration of &lt;T&gt; from the set
	 * @param <T> : Class of the enumeration returned
	 * @param name : String designating the key in the set
	 * @param enumClass : Class designating the class of the value associated with the key in the set
	 * @return Enum<T>
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T extends Enum<T>> T getEnum(final String name, final Class<T> enumClass)
	{
		final Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but not specified");
		if (enumClass.isInstance(val))
			return (T) val;
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(val));
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + "required, but found: " + val);
		}
	}
	
	/**
	 * Returns an enumeration of &lt;T&gt; from the set. If the enumeration is empty, the method returns the value of the parameter "deflt".
	 * @param <T> : Class of the enumeration returned
	 * @param name : String designating the key in the set
	 * @param enumClass : Class designating the class of the value associated with the key in the set
	 * @param deflt : <T> designating the value by default
	 * @return Enum<T>
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T extends Enum<T>> T getEnum(final String name, final Class<T> enumClass, final T deflt)
	{
		final Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (enumClass.isInstance(val))
			return (T) val;
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(val));
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + "required, but found: " + val);
		}
	}
	
	/**
	 * Add the String hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : String corresponding to the value associated with the key
	 */
	public synchronized void set(final String name, final String value)
	{
		_set.put(name, value);
	}
	
	/**
	 * Add the boolean hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : boolean corresponding to the value associated with the key
	 */
	public synchronized void set(final String name, final boolean value)
	{
		_set.put(name, value);
	}
	
	/**
	 * Add the int hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : int corresponding to the value associated with the key
	 */
	public synchronized void set(final String name, final int value)
	{
		_set.put(name, value);
	}
	
	/**
	 * Add the double hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : double corresponding to the value associated with the key
	 */
	public synchronized void set(final String name, final double value)
	{
		_set.put(name, value);
	}
	
	/**
	 * Add the long hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : double corresponding to the value associated with the key
	 */
	public synchronized void set(final String name, final long value)
	{
		_set.put(name, value);
	}
	
	/**
	 * Add the Enum hold in param "value" for the key "name"
	 * @param name : String designating the key in the set
	 * @param value : Enum corresponding to the value associated with the key
	 */
	public synchronized void set(final String name, final Enum<?> value)
	{
		_set.put(name, value);
	}
	
	/**
	 * Safe version of "set". Expected values are within [min, max[<br>
	 * Add the int hold in param "value" for the key "name".
	 * @param name : String designating the key in the set
	 * @param value : int corresponding to the value associated with the key
	 * @param min
	 * @param max
	 * @param reference
	 */
	public synchronized void safeSet(final String name, final int value, final int min, final int max, final String reference)
	{
		assert !((min <= max && (value < min || value >= max)));
		
		if (min <= max && (value < min || value >= max))
		{
			LOG.info("[StatsSet][safeSet] Incorrect value: " + value + "for: " + name + "Ref: " + reference);
		}
		
		set(name, value);
	}
	
	@Override
	public String toString()
	{
		return "StatsSet [_set=" + _set + "]";
	}
	
}

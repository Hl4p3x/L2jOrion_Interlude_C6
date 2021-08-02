/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,	FITNESS	FOR	A   PARTICULAR	PURPOSE	AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package l2jorion.logger.helpers;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2jorion.logger.Marker;

public class BasicMarker implements Marker
{
	private static final long serialVersionUID = -2849567615646933777L;
	private final String _name;
	private List<Marker> referenceList = new CopyOnWriteArrayList<>();
	
	public BasicMarker(String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException("A marker name cannot be null");
		}
		_name = name;
	}

	@Override
	public String getName()
	{
		return _name;
	}

	@Override
	public void add(Marker reference)
	{
		if (reference == null)
		{
			throw new IllegalArgumentException("A null value cannot be added to a Marker as reference.");
		}
		
		// no point in adding the reference multiple times
		if (contains(reference))
		{
			return;
		}
		else if (reference.contains(this))
		{
			return;
		}
		else
		{
			referenceList.add(reference);
		}
	}

	@Override
	public boolean hasReferences()
	{
		return (referenceList.size() > 0);
	}

	@Override
	public boolean hasChildren()
	{
		return hasReferences();
	}

	@Override
	public Iterator<Marker> iterator()
	{
		return referenceList.iterator();
	}

	@Override
	public boolean remove(Marker referenceToRemove)
	{
		return referenceList.remove(referenceToRemove);
	}

	@Override
	public boolean contains(Marker other)
	{
		if (other == null)
		{
			throw new IllegalArgumentException("Other cannot be null");
		}
		
		if (equals(other))
		{
			return true;
		}
		
		if (hasReferences())
		{
			for (Marker ref : referenceList)
			{
				if (ref.contains(other))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This method is mainly used with Expression Evaluators.
	 */
	@Override
	public boolean contains(String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException("Other cannot be null");
		}

		if (_name.equals(name))
		{
			return true;
		}

		if (hasReferences())
		{
			for (Marker ref : referenceList)
			{
				if (ref.contains(name))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static String OPEN = "[ ";
	private static String CLOSE = " ]";
	private static String SEP = ", ";

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if (obj == null)
		{
			return false;
		}
		
		if (!(obj instanceof Marker))
		{
			return false;
		}

		final Marker other = (Marker) obj;
		return _name.equals(other.getName());
	}

	@Override
	public int hashCode()
	{
		return _name.hashCode();
	}

	@Override
	public String toString()
	{
		if (!hasReferences())
		{
			return getName();
		}
		Iterator<Marker> it = this.iterator();
		Marker reference;
		StringBuilder sb = new StringBuilder(getName());
		sb.append(' ').append(OPEN);
		while (it.hasNext())
		{
			reference = it.next();
			sb.append(reference.getName());
			if (it.hasNext())
			{
				sb.append(SEP);
			}
		}
		sb.append(CLOSE);

		return sb.toString();
	}
}

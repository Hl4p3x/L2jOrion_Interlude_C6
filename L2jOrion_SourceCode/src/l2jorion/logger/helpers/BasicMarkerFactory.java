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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import l2jorion.logger.IMarkerFactory;
import l2jorion.logger.Marker;

public class BasicMarkerFactory implements IMarkerFactory
{
	private final ConcurrentMap<String, Marker> markerMap = new ConcurrentHashMap<>();
	
	public BasicMarkerFactory() 
	{
	}
	
	@Override
	public Marker getMarker(String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException("Marker name cannot be null");
		}
		
		Marker marker = markerMap.get(name);
		if (marker == null)
		{
			marker = new BasicMarker(name);
			Marker oldMarker = markerMap.putIfAbsent(name, marker);
			if (oldMarker != null)
			{
				marker = oldMarker;
			}
		}
		return marker;
	}
	
	@Override
	public boolean exists(String name)
	{
		if (name == null)
		{
			return false;
		}
		return markerMap.containsKey(name);
	}

	@Override
	public boolean detachMarker(String name)
	{
		if (name == null)
		{
			return false;
		}
		return (markerMap.remove(name) != null);
	}

	@Override
	public Marker getDetachedMarker(String name)
	{
		return new BasicMarker(name);
	}

}

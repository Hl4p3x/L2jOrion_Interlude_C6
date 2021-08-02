/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package l2jorion.util;

import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import l2jorion.Config;
import l2jorion.mmocore.IAcceptFilter;

public class IPv4Filter implements IAcceptFilter, Runnable
{
	private final HashMap<Integer, Flood> _ipFloodMap;
	private static final long SLEEP_TIME = 10;
	
	public IPv4Filter()
	{
		_ipFloodMap = new HashMap<>();
		final Thread t = new Thread(this);
		t.setName(getClass().getSimpleName());
		t.setDaemon(true);
		t.start();
	}
	
	private static final int hash(final byte[] ip)
	{
		return ip[0] & 0xFF | ip[1] << 8 & 0xFF00 | ip[2] << 16 & 0xFF0000 | ip[3] << 24 & 0xFF000000;
	}
	
	protected static final class Flood
	{
		long lastAccess;
		int trys;
		
		Flood()
		{
			lastAccess = System.currentTimeMillis();
			trys = 0;
		}
	}
	
	@Override
	public boolean accept(final SocketChannel sc)
	{
		final InetAddress addr = sc.socket().getInetAddress();
		final int h = hash(addr.getAddress());
		
		final long current = System.currentTimeMillis();
		Flood f;
		synchronized (_ipFloodMap)
		{
			f = _ipFloodMap.get(h);
		}
		if (f != null)
		{
			if (f.trys == -1)
			{
				f.lastAccess = current;
				return false;
			}
			
			if (f.lastAccess + 1000 > current)
			{
				f.lastAccess = current;
				
				if (f.trys >= 3)
				{
					f.trys = -1;
					return false;
				}
				
				f.trys++;
			}
			else
			{
				f.lastAccess = current;
			}
		}
		else
		{
			synchronized (_ipFloodMap)
			{
				_ipFloodMap.put(h, new Flood());
			}
		}
		
		return true;
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			final long reference = System.currentTimeMillis() - (1000 * 300);
			synchronized (_ipFloodMap)
			{
				final Iterator<Entry<Integer, Flood>> it = _ipFloodMap.entrySet().iterator();
				while (it.hasNext())
				{
					final Flood f = it.next().getValue();
					if (f.lastAccess < reference)
					{
						it.remove();
					}
				}
			}
			
			try
			{
				Thread.sleep(SLEEP_TIME);
			}
			catch (final InterruptedException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				return;
			}
		}
	}
	
}
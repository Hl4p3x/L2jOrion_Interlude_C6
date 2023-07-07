/*
 * $Header: BitSetIDFactory.java, 24/08/2005 22:44:26 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 24/08/2005 22:44:26 $
 * $Revision: 1 $
 * $Log: BitSetIDFactory.java,v $
 * Revision 1  24/08/2005 22:44:26  luisantonioa
 * Added copyright notice
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
package l2jorion.game.idfactory;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

import l2jorion.Config;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.PrimeFinder;

public class BitSetIDFactory extends IdFactory
{
	private static Logger LOG = LoggerFactory.getLogger(BitSetIDFactory.class);
	
	private BitSet _freeIds;
	private AtomicInteger _freeIdCount;
	private AtomicInteger _nextFreeId;
	
	public class BitSetCapacityCheck implements Runnable
	{
		@Override
		public void run()
		{
			if (reachingBitSetCapacity())
			{
				increaseBitSetCapacity();
			}
		}
		
	}
	
	protected BitSetIDFactory()
	{
		super();
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new BitSetCapacityCheck(), 30000, 30000);
		initialize();
		
		LOG.info("IdFactory: " + _freeIds.size() + " id's available");
	}
	
	public synchronized void initialize()
	{
		try
		{
			_freeIds = new BitSet(PrimeFinder.nextPrime(100000));
			_freeIds.clear();
			_freeIdCount = new AtomicInteger(FREE_OBJECT_ID_SIZE);
			
			for (final int usedObjectId : extractUsedObjectIDTable())
			{
				final int objectID = usedObjectId - FIRST_OID;
				if (objectID < 0)
				{
					if (Config.DEBUG)
					{
						LOG.warn("Object ID " + usedObjectId + " in DB is less than minimum ID of " + FIRST_OID);
					}
					continue;
				}
				_freeIds.set(usedObjectId - FIRST_OID);
				_freeIdCount.decrementAndGet();
			}
			
			_nextFreeId = new AtomicInteger(_freeIds.nextClearBit(0));
			_initialized = true;
		}
		catch (final Exception e)
		{
			_initialized = false;
			LOG.error("BitSet ID Factory could not be initialized correctly", e);
			e.printStackTrace();
		}
	}
	
	@Override
	public synchronized void releaseId(final int objectID)
	{
		if ((objectID - FIRST_OID) > -1)
		{
			_freeIds.clear(objectID - FIRST_OID);
			_freeIdCount.incrementAndGet();
		}
		else
		{
			LOG.warn("BitSet ID Factory: release objectID " + objectID + " failed (< " + FIRST_OID + ")");
		}
	}
	
	@Override
	public synchronized int getNextId()
	{
		final int newID = _nextFreeId.get();
		_freeIds.set(newID);
		_freeIdCount.decrementAndGet();
		
		int nextFree = _freeIds.nextClearBit(newID);
		
		if (nextFree < 0)
		{
			nextFree = _freeIds.nextClearBit(0);
		}
		if (nextFree < 0)
		{
			if (_freeIds.size() < FREE_OBJECT_ID_SIZE)
			{
				increaseBitSetCapacity();
			}
			else
			{
				throw new NullPointerException("Ran out of valid Id's.");
			}
		}
		
		_nextFreeId.set(nextFree);
		
		return newID + FIRST_OID;
	}
	
	@Override
	public synchronized int size()
	{
		return _freeIdCount.get();
	}
	
	protected synchronized int usedIdCount()
	{
		return (size() - FIRST_OID);
	}
	
	protected synchronized boolean reachingBitSetCapacity()
	{
		return PrimeFinder.nextPrime(usedIdCount() * 11 / 10) > _freeIds.size();
	}
	
	protected synchronized void increaseBitSetCapacity()
	{
		final BitSet newBitSet = new BitSet(PrimeFinder.nextPrime(usedIdCount() * 11 / 10));
		newBitSet.or(_freeIds);
		_freeIds = newBitSet;
	}
}

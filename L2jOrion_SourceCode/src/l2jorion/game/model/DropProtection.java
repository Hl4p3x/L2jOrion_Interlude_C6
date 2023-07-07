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
package l2jorion.game.model;

import java.util.concurrent.ScheduledFuture;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.thread.ThreadPoolManager;

public class DropProtection implements Runnable
{
	private volatile boolean _isProtected = false;
	private L2PcInstance _owner = null;
	private ScheduledFuture<?> _task = null;
	
	private static final long PROTECTED_MILLIS_TIME = 15000;
	
	@Override
	public synchronized void run()
	{
		_isProtected = false;
		_owner = null;
		_task = null;
	}
	
	public boolean isProtected()
	{
		return _isProtected;
	}
	
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	public synchronized boolean tryPickUp(final L2PcInstance actor)
	{
		if (!_isProtected)
		{
			return true;
		}
		
		if (_owner == actor)
		{
			return true;
		}
		
		if (_owner.getParty() != null && _owner.getParty() == actor.getParty())
		{
			return true;
		}
		
		if (_owner.getClan() != null && _owner.getClan() == actor.getClan())
		{
			return true;
		}
		
		return false;
	}
	
	public boolean tryPickUp(final L2PetInstance pet)
	{
		return tryPickUp(pet.getOwner());
	}
	
	public synchronized void unprotect()
	{
		if (_task != null)
		{
			_task.cancel(false);
		}
		
		_isProtected = false;
		_owner = null;
		_task = null;
	}
	
	public synchronized void protect(final L2PcInstance player)
	{
		unprotect();
		
		_isProtected = true;
		
		if ((_owner = player) == null)
		{
			throw new NullPointerException("Trying to protect dropped item to null owner");
		}
		
		_task = ThreadPoolManager.getInstance().scheduleGeneral(this, PROTECTED_MILLIS_TIME);
	}
}

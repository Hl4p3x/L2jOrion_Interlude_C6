/*
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
package l2jorion.game.model.entity;

import l2jorion.game.model.actor.instance.L2PcInstance;

public class PlayerToAssasinate
{
	private int _objectId;
	private int _clientId;
	private String _name;
	private int _bounty;
	private boolean _online;
	private boolean _pendingDelete;
	
	public PlayerToAssasinate(L2PcInstance target, int clientId, int bounty)
	{
		_objectId = target.getObjectId();
		_clientId = clientId;
		_name = target.getName();
		_bounty = bounty;
		_online = target.isOnline() == 1;
	}
	
	public PlayerToAssasinate(int objectId, int clientId, int bounty, String name)
	{
		_objectId = objectId;
		_clientId = clientId;
		_name = name;
		_bounty = bounty;
		_online = false;
	}
	
	public void setObjectId(int objectId)
	{
		_objectId = objectId;
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setBounty(int vol)
	{
		_bounty = vol;
	}
	
	public void incBountyBy(int vol)
	{
		_bounty += vol;
	}
	
	public void decBountyBy(int vol)
	{
		_bounty -= vol;
	}
	
	public int getBounty()
	{
		return _bounty;
	}
	
	public void setOnline(boolean online)
	{
		_online = online;
	}
	
	public boolean isOnline()
	{
		return _online;
	}
	
	public void setClientId(int clientId)
	{
		_clientId = clientId;
	}
	
	public int getClientId()
	{
		return _clientId;
	}
	
	public void setPendingDelete(boolean pendingDelete)
	{
		_pendingDelete = pendingDelete;
	}
	
	public boolean isPendingDelete()
	{
		return _pendingDelete;
	}
}
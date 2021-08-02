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

import java.util.List;

import javolution.util.FastList;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExManagePartyRoomMember;
import l2jorion.game.network.serverpackets.SystemMessage;

public class PartyMatchRoom
{
	private final int _id;
	private String _title;
	private int _loot;
	private int _minlvl;
	private int _maxlvl;
	private int _maxmem;
	private final List<L2PcInstance> _members = new FastList<>();
	
	public PartyMatchRoom(final int id, final String title, final int loot, final int minlvl, final int maxlvl, final int maxmem, final L2PcInstance owner)
	{
		_id = id;
		_title = title;
		_loot = loot;
		_minlvl = minlvl;
		_maxlvl = maxlvl;
		_maxmem = maxmem;
		_members.add(owner);
	}
	
	public List<L2PcInstance> getPartyMembers()
	{
		return _members;
	}
	
	public void addMember(final L2PcInstance player)
	{
		_members.add(player);
	}
	
	public void deleteMember(final L2PcInstance player)
	{
		if (player != getOwner())
		{
			_members.remove(player);
			notifyMembersAboutExit(player);
		}
		else if (_members.size() == 1)
		{
			PartyMatchRoomList.getInstance().deleteRoom(_id);
		}
		else
		{
			changeLeader(_members.get(1));
			deleteMember(player);
		}
	}
	
	public void notifyMembersAboutExit(final L2PcInstance player)
	{
		for (final L2PcInstance _member : getPartyMembers())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_LEFT_PARTY_ROOM);
			sm.addString(player.getName());
			_member.sendPacket(sm);
			_member.sendPacket(new ExManagePartyRoomMember(player, this, 2));
		}
	}
	
	public void changeLeader(final L2PcInstance newLeader)
	{
		// Get current leader
		final L2PcInstance oldLeader = _members.get(0);
		// Remove new leader
		if (_members.contains(newLeader))
		{
			_members.remove(newLeader);
		}
		
		// Move him to first position
		if (!_members.isEmpty())
		{
			_members.set(0, newLeader);
		}
		else
		{
			_members.add(newLeader);
		}
		
		// Add old leader as normal member
		if (oldLeader != null && oldLeader != newLeader)
		{
			_members.add(oldLeader);
		}
		
		// Broadcast change
		for (final L2PcInstance member : getPartyMembers())
		{
			member.sendPacket(new ExManagePartyRoomMember(newLeader, this, 1));
			member.sendPacket(new ExManagePartyRoomMember(oldLeader, this, 1));
			member.sendPacket(new SystemMessage(SystemMessageId.PARTY_ROOM_LEADER_CHANGED));
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
	public L2PcInstance getOwner()
	{
		return _members.get(0);
	}
	
	public int getMembers()
	{
		return _members.size();
	}
	
	public int getLootType()
	{
		return _loot;
	}
	
	public void setLootType(final int loot)
	{
		_loot = loot;
	}
	
	public int getMinLvl()
	{
		return _minlvl;
	}
	
	public void setMinLvl(final int minlvl)
	{
		_minlvl = minlvl;
	}
	
	public int getMaxLvl()
	{
		return _maxlvl;
	}
	
	public void setMaxLvl(final int maxlvl)
	{
		_maxlvl = maxlvl;
	}
	
	public int getLocation()
	{
		return MapRegionTable.getInstance().getMapRegion(_members.get(0)).getBbs();
	}
	
	public int getMaxMembers()
	{
		return _maxmem;
	}
	
	public void setMaxMembers(final int maxmem)
	{
		_maxmem = maxmem;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public void setTitle(final String title)
	{
		_title = title;
	}
}
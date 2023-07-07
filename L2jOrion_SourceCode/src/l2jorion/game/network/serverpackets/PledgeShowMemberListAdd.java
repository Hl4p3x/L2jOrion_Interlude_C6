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
package l2jorion.game.network.serverpackets;

import l2jorion.Config;
import l2jorion.game.model.L2ClanMember;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class PledgeShowMemberListAdd extends PacketServer
{
	private static final String _S__55_PLEDGESHOWMEMBERLISTADD = "[S] 55 PledgeShowMemberListAdd";
	
	private String _name;
	private int _lvl;
	private int _classId;
	private int _isOnline;
	private int _pledgeType;
	
	public PledgeShowMemberListAdd(final L2PcInstance player)
	{
		_name = player.getName();
		_lvl = player.getLevel();
		_classId = player.getClassId().getId();
		_isOnline = player.isOnline() == 1 ? player.getObjectId() : 0;
		_pledgeType = player.getPledgeType();
	}
	
	public PledgeShowMemberListAdd(final L2ClanMember cm)
	{
		try
		{
			_name = cm.getName();
			_lvl = cm.getLevel();
			_classId = cm.getClassId();
			_isOnline = cm.isOnline() ? cm.getObjectId() : 0;
			_pledgeType = cm.getPledgeType();
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x55);
		writeS(_name);
		writeD(_lvl);
		writeD(_classId);
		writeD(0);
		writeD(1);
		writeD(_isOnline); // 1=online 0=offline
		writeD(_pledgeType);
	}
	
	@Override
	public String getType()
	{
		return _S__55_PLEDGESHOWMEMBERLISTADD;
	}
	
}

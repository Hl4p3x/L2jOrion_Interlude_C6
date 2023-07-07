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

import l2jorion.game.model.L2Clan;
import l2jorion.game.network.PacketServer;

public class ManagePledgePower extends PacketServer
{
	private static final String _S__30_MANAGEPLEDGEPOWER = "[S] 30 ManagePledgePower";
	
	private final int _action;
	private final L2Clan _clan;
	private final int _rank;
	private int _privs;
	
	public ManagePledgePower(final L2Clan clan, final int action, final int rank)
	{
		_clan = clan;
		_action = action;
		_rank = rank;
	}
	
	@Override
	protected final void writeImpl()
	{
		if (_action == 1)
		{
			_privs = _clan.getRankPrivs(_rank);
		}
		else
		{
			return;
		}
		writeC(0x30);
		writeD(0);
		writeD(0);
		writeD(_privs);
	}
	
	@Override
	public String getType()
	{
		return _S__30_MANAGEPLEDGEPOWER;
	}
	
}

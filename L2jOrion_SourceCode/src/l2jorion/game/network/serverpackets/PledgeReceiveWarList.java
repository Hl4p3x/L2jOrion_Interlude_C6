/* L2jOrion Project - www.l2jorion.com 
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

import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.L2Clan;
import l2jorion.game.network.PacketServer;

public class PledgeReceiveWarList extends PacketServer
{
	private static final String _S__FE_3E_PLEDGERECEIVEWARELIST = "[S] FE:3E PledgeReceiveWarList";
	
	private final L2Clan _clan;
	private final int _tab;
	
	public PledgeReceiveWarList(final L2Clan clan, final int tab)
	{
		_clan = clan;
		_tab = tab;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x3e);
		
		writeD(_tab); // type : 0 = Declared, 1 = Under Attack
		writeD(0x00); // page
		writeD(_tab == 0 ? _clan.getWarList().size() : _clan.getAttackerList().size());
		for (final Integer i : _tab == 0 ? _clan.getWarList() : _clan.getAttackerList())
		{
			final L2Clan clan = ClanTable.getInstance().getClan(i);
			if (clan == null)
			{
				continue;
			}
			
			writeS(clan.getName());
			writeD(_tab); // ??
			writeD(_tab); // ??
		}
	}
	
	@Override
	public String getType()
	{
		return _S__FE_3E_PLEDGERECEIVEWARELIST;
	}
}

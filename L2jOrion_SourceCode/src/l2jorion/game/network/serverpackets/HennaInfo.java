/*
 * $Header$
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
package l2jorion.game.network.serverpackets;

import l2jorion.game.model.actor.instance.L2HennaInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public final class HennaInfo extends PacketServer
{
	private static final String _S__E4_HennaInfo = "[S] E4 HennaInfo";
	
	private final L2PcInstance _activeChar;
	private final L2HennaInstance[] _hennas = new L2HennaInstance[3];
	private final int _count;
	
	public HennaInfo(final L2PcInstance player)
	{
		_activeChar = player;
		
		int j = 0;
		for (int i = 0; i < 3; i++)
		{
			final L2HennaInstance h = _activeChar.getHennas(i + 1);
			if (h != null)
			{
				_hennas[j++] = h;
			}
		}
		_count = j;
	}
	
	@Override
	protected final void writeImpl()
	{
		
		writeC(0xe4);
		
		writeC(_activeChar.getHennaStatINT()); // equip INT
		writeC(_activeChar.getHennaStatSTR()); // equip STR
		writeC(_activeChar.getHennaStatCON()); // equip CON
		writeC(_activeChar.getHennaStatMEN()); // equip MEM
		writeC(_activeChar.getHennaStatDEX()); // equip DEX
		writeC(_activeChar.getHennaStatWIT()); // equip WIT
		
		writeD(3); // slots?
		
		writeD(_count); // size
		for (int i = 0; i < _count; i++)
		{
			writeD(_hennas[i].getSymbolId());
			writeD(0x01);
		}
	}
	
	@Override
	public String getType()
	{
		return _S__E4_HennaInfo;
	}
}

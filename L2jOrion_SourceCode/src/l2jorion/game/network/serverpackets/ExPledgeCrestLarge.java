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

import l2jorion.game.network.PacketServer;

public class ExPledgeCrestLarge extends PacketServer
{
	private static final String _S__FE_28_EXPLEDGECRESTLARGE = "[S] FE:28 ExPledgeCrestLarge";
	
	private final int _crestId;
	private final byte[] _data;
	
	public ExPledgeCrestLarge(final int crestId, final byte[] data)
	{
		_crestId = crestId;
		_data = data;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x28);
		
		writeD(0x00);
		writeD(_crestId);
		writeD(_data.length);
		writeB(_data);
	}
	
	@Override
	public String getType()
	{
		return _S__FE_28_EXPLEDGECRESTLARGE;
	}
	
}

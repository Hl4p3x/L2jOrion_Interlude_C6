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

import l2jorion.game.network.PacketServer;

public class ExConfirmVariationItem extends PacketServer
{
	private static final String _S__FE_52_EXCONFIRMVARIATIONITEM = "[S] FE:52 ExConfirmVariationItem";
	
	private final int _itemObjId;
	private final int _unk1;
	private final int _unk2;
	
	public ExConfirmVariationItem(final int itemObjId)
	{
		_itemObjId = itemObjId;
		_unk1 = 1;
		_unk2 = 1;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x52);
		writeD(_itemObjId);
		writeD(_unk1);
		writeD(_unk2);
	}
	
	@Override
	public String getType()
	{
		return _S__FE_52_EXCONFIRMVARIATIONITEM;
	}
}

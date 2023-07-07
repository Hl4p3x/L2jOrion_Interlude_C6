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

public class Dice extends PacketServer
{
	private static final String _S__D4_Dice = "[S] D4 Dice";
	private final int _charObjId;
	private final int _itemId;
	private final int _number;
	private final int _x;
	private final int _y;
	private final int _z;
	
	/**
	 * 0xd4 Dice dddddd
	 * @param charObjId
	 * @param itemId
	 * @param number
	 * @param x
	 * @param y
	 * @param z
	 */
	public Dice(final int charObjId, final int itemId, final int number, final int x, final int y, final int z)
	{
		_charObjId = charObjId;
		_itemId = itemId;
		_number = number;
		_x = x;
		_y = y;
		_z = z;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xD4);
		writeD(_charObjId); // object id of player
		writeD(_itemId); // item id of dice (spade) 4625,4626,4627,4628
		writeD(_number); // number rolled
		writeD(_x); // x
		writeD(_y); // y
		writeD(_z); // z
	}
	
	@Override
	public String getType()
	{
		return _S__D4_Dice;
	}
}

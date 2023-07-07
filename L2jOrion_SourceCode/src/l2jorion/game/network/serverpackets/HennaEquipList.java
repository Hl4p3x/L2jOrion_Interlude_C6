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

public class HennaEquipList extends PacketServer
{
	private static final String _S__E2_HennaEquipList = "[S] E2 HennaEquipList";
	
	private final L2PcInstance _player;
	private final L2HennaInstance[] _hennaEquipList;
	
	public HennaEquipList(final L2PcInstance player, final L2HennaInstance[] hennaEquipList)
	{
		_player = player;
		_hennaEquipList = hennaEquipList;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xe2);
		writeD(_player.getInventory().getAdena()); // activeChar current amount of aden
		writeD(3); // available equip slot
		// writeD(10); // total amount of symbol available which depends on difference classes
		writeD(_hennaEquipList.length);
		
		for (final L2HennaInstance element : _hennaEquipList)
		{
			/*
			 * Player must have at least one dye in inventory to be able to see the henna that can be applied with it.
			 */
			if (_player.getInventory().getItemByItemId(element.getItemIdDye()) != null)
			{
				writeD(element.getSymbolId()); // symbolid
				writeD(element.getItemIdDye()); // itemid of dye
				writeD(element.getAmountDyeRequire()); // amount of dye require
				writeD(element.getPrice()); // amount of aden require
				writeD(1); // meet the requirement or not
			}
			else
			{
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__E2_HennaEquipList;
	}
	
}

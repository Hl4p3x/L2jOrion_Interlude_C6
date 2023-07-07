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

import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class ItemList extends PacketServer
{
	private static final String _S__27_ITEMLIST = "[S] 1b ItemList";
	
	private final L2ItemInstance[] _items;
	private final boolean _showWindow;
	
	public ItemList(final L2PcInstance cha, final boolean showWindow)
	{
		_items = cha.getInventory().getItems();
		_showWindow = showWindow;
	}
	
	public ItemList(final L2ItemInstance[] items, final boolean showWindow)
	{
		_items = items;
		_showWindow = showWindow;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x1b);
		writeH(_showWindow ? 0x01 : 0x00);
		
		final int count = _items.length;
		
		writeH(count);
		
		for (final L2ItemInstance item : _items)
		{
			if ((item == null) || (item.getItem() == null))
			{
				continue;
			}
			
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(item.getCount());
			writeH(item.getItem().getType2());
			writeH(item.getCustomType1());
			
			writeH(item.isEquipped() ? 0x01 : 0x00);
			writeD(item.getItem().getBodyPart());
			
			writeH(item.getEnchantLevel());
			writeH(item.getCustomType2());
			
			if (item.isAugmented())
			{
				writeD(item.getAugmentation().getAugmentationId());
			}
			else
			{
				writeD(0x00);
			}
			
			writeD(item.getMana());
		}
	}
	
	@Override
	public String getType()
	{
		return _S__27_ITEMLIST;
	}
}
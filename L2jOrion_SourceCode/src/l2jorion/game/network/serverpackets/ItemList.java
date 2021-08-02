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
import l2jorion.game.templates.L2Item;

public class ItemList extends L2GameServerPacket
{
	private static final String _S__27_ITEMLIST = "[S] 1b ItemList";
	
	private final L2ItemInstance[] _items;
	private final boolean _showWindow;
	private L2PcInstance _activeChar;
	
	public ItemList(final L2PcInstance cha, final boolean showWindow)
	{
		_activeChar = cha;
		_items = cha.getInventory().getItems();
		_showWindow = showWindow;
	}
	
	public ItemList(final L2ItemInstance[] items, final boolean showWindow)
	{
		_items = items;
		_showWindow = showWindow;
	}
	
	private static boolean isBodypart(L2Item item)
	{
		if (item.getBodyPart() == L2Item.SLOT_ALLDRESS || item.getBodyPart() == L2Item.SLOT_HEAD || item.getBodyPart() == L2Item.SLOT_FULL_ARMOR || item.getBodyPart() == L2Item.SLOT_CHEST || item.getBodyPart() == L2Item.SLOT_LEGS || item.getBodyPart() == L2Item.SLOT_GLOVES
			|| item.getBodyPart() == L2Item.SLOT_FEET)
		{
			return true;
		}
		
		return false;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x1b);
		writeH(_showWindow ? 0x01 : 0x00);
		
		final int count = _items.length;
		
		writeH(count);
		
		for (final L2ItemInstance temp : _items)
		{
			if ((temp == null) || (temp.getItem() == null))
			{
				continue;
			}
			
			writeH(temp.getItem().getType1());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getCount());
			writeH(temp.getItem().getType2());
			writeH(temp.getCustomType1());
			
			// writeH(temp.isEquipped() ? 0x01 : 0x00);
			// writeD(temp.getItem().getBodyPart());
			
			if (_activeChar != null && _activeChar.getFakeArmorObjectId() > 0)
			{
				if (temp.getObjectId() == _activeChar.getFakeArmorObjectId())
				{
					writeH(0x01);
				}
				else
				{
					writeH(temp.isEquipped() ? 0x01 : 0x00);
				}
				
				if (temp.getObjectId() == _activeChar.getFakeArmorObjectId())
				{
					writeD(temp.isFakeArmor() ? L2Item.SLOT_ALLDRESS : temp.getItem().getBodyPart());
				}
				else if (isBodypart(temp.getItem()) && temp.isEquipped() && _activeChar.getFakeArmorObjectId() > 0)
				{
					writeD(99);
				}
				else
				{
					writeD(temp.getItem().getBodyPart());
				}
			}
			else
			{
				writeH(temp.isEquipped() ? 0x01 : 0x00);
				writeD(temp.getItem().getBodyPart());
			}
			
			writeH(temp.getEnchantLevel());
			writeH(temp.getCustomType2());
			
			if (temp.isAugmented())
			{
				writeD(temp.getAugmentation().getAugmentationId());
			}
			else
			{
				writeD(0x00);
			}
			
			writeD(temp.getMana());
		}
	}
	
	@Override
	public String getType()
	{
		return _S__27_ITEMLIST;
	}
}
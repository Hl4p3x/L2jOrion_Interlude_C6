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
package l2jorion.game.model;

import java.util.List;

import javolution.util.FastList;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance.ItemLocation;

public class PcFreight extends ItemContainer
{
	private final L2PcInstance _owner; // This is the L2PcInstance that owns this Freight;
	private int _activeLocationId;
	
	public PcFreight(final L2PcInstance owner)
	{
		_owner = owner;
	}
	
	@Override
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.FREIGHT;
	}
	
	public void setActiveLocation(final int locationId)
	{
		_activeLocationId = locationId;
	}
	
	public int getactiveLocation()
	{
		return _activeLocationId;
	}
	
	@Override
	public int getSize()
	{
		int size = 0;
		
		for (final L2ItemInstance item : _items)
		{
			if (item.getEquipSlot() == 0 || _activeLocationId == 0 || item.getEquipSlot() == _activeLocationId)
			{
				size++;
			}
		}
		return size;
	}
	
	@Override
	public L2ItemInstance[] getItems()
	{
		final List<L2ItemInstance> list = new FastList<>();
		
		for (final L2ItemInstance item : _items)
		{
			if (item.getEquipSlot() == 0 || item.getEquipSlot() == _activeLocationId)
			{
				list.add(item);
			}
		}
		
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	@Override
	public L2ItemInstance getItemByItemId(final int itemId)
	{
		for (final L2ItemInstance item : _items)
			if (item.getItemId() == itemId && (item.getEquipSlot() == 0 || _activeLocationId == 0 || item.getEquipSlot() == _activeLocationId))
				return item;
		
		return null;
	}
	
	@Override
	protected void addItem(final L2ItemInstance item)
	{
		super.addItem(item);
		if (_activeLocationId > 0)
		{
			item.setLocation(item.getLocation(), _activeLocationId);
		}
	}
	
	@Override
	public void restore()
	{
		final int locationId = _activeLocationId;
		_activeLocationId = 0;
		super.restore();
		_activeLocationId = locationId;
	}
	
	@Override
	public boolean validateCapacity(final int slots)
	{
		return getSize() + slots <= _owner.GetFreightLimit();
	}
}

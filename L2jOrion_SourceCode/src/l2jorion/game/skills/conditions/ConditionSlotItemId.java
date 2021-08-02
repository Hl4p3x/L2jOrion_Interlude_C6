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
package l2jorion.game.skills.conditions;

import l2jorion.game.model.Inventory;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.skills.Env;

public final class ConditionSlotItemId extends ConditionInventory
{
	private final int _itemId;
	private final int _enchantLevel;
	
	public ConditionSlotItemId(final int slot, final int itemId, final int enchantLevel)
	{
		super(slot);
		
		_itemId = itemId;
		_enchantLevel = enchantLevel;
	}
	
	@Override
	public boolean testImpl(final Env env)
	{
		if (!(env.player instanceof L2PcInstance))
		{
			return false;
		}
		
		final Inventory inv = ((L2PcInstance) env.player).getInventory();
		
		final L2ItemInstance item = inv.getPaperdollItem(_slot);
		
		if (item == null)
		{
			return _itemId == 0;
		}
		
		return item.getItemId() == _itemId && item.getEnchantLevel() >= _enchantLevel;
	}
}

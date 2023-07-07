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

package l2jorion.game.model;

import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;

public final class L2ArmorSet
{
	private final int _chest;
	private final int _legs;
	private final int _head;
	private final int _gloves;
	private final int _feet;
	private final int _skillId;
	
	private final int _shield;
	private final int _shieldSkillId;
	
	private final int _enchant6Skill;
	
	public L2ArmorSet(final int chest, final int legs, final int head, final int gloves, final int feet, final int skill_id, final int shield, final int shield_skill_id, final int enchant6skill)
	{
		_chest = chest;
		_legs = legs;
		_head = head;
		_gloves = gloves;
		_feet = feet;
		_skillId = skill_id;
		
		_shield = shield;
		_shieldSkillId = shield_skill_id;
		
		_enchant6Skill = enchant6skill;
	}
	
	/**
	 * Checks if player have equiped all items from set (not checking shield)
	 * @param player whose inventory is being checked
	 * @return True if player equips whole set
	 */
	public boolean containAll(final L2PcInstance player)
	{
		final Inventory inv = player.getInventory();
		
		L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;
		
		if (legsItem != null)
		{
			legs = legsItem.getItemId();
		}
		if (headItem != null)
		{
			head = headItem.getItemId();
		}
		if (glovesItem != null)
		{
			gloves = glovesItem.getItemId();
		}
		if (feetItem != null)
		{
			feet = feetItem.getItemId();
		}
		
		legsItem = null;
		headItem = null;
		glovesItem = null;
		feetItem = null;
		
		return containAll(_chest, legs, head, gloves, feet);
		
	}
	
	public boolean containAll(final int chest, final int legs, final int head, final int gloves, final int feet)
	{
		if (_chest != 0 && _chest != chest)
		{
			return false;
		}
		if (_legs != 0 && _legs != legs)
		{
			return false;
		}
		if (_head != 0 && _head != head)
		{
			return false;
		}
		if (_gloves != 0 && _gloves != gloves)
		{
			return false;
		}
		if (_feet != 0 && _feet != feet)
		{
			return false;
		}
		
		return true;
	}
	
	public boolean containItem(final int slot, final int itemId)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
				return _chest == itemId;
			case Inventory.PAPERDOLL_LEGS:
				return _legs == itemId;
			case Inventory.PAPERDOLL_HEAD:
				return _head == itemId;
			case Inventory.PAPERDOLL_GLOVES:
				return _gloves == itemId;
			case Inventory.PAPERDOLL_FEET:
				return _feet == itemId;
			default:
				return false;
		}
	}
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public boolean containShield(final L2PcInstance player)
	{
		Inventory inv = player.getInventory();
		
		L2ItemInstance shieldItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		
		if (shieldItem != null && shieldItem.getItemId() == _shield)
		{
			return true;
		}
		
		inv = null;
		shieldItem = null;
		
		return false;
	}
	
	public boolean containShield(final int shield_id)
	{
		if (_shield == 0)
		{
			return false;
		}
		
		return _shield == shield_id;
	}
	
	public int getShieldSkillId()
	{
		return _shieldSkillId;
	}
	
	public int getEnchant6skillId()
	{
		return _enchant6Skill;
	}
	
	/**
	 * Checks if all parts of set are enchanted to +6 or more
	 * @param player
	 * @return
	 */
	public boolean isEnchanted6(final L2PcInstance player)
	{
		// Player don't have full set
		if (!containAll(player))
		{
			return false;
		}
		
		final Inventory inv = player.getInventory();
		
		L2ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		if (chestItem.getEnchantLevel() < 6)
		{
			return false;
		}
		if (_legs != 0 && legsItem.getEnchantLevel() < 6)
		{
			return false;
		}
		if (_gloves != 0 && glovesItem.getEnchantLevel() < 6)
		{
			return false;
		}
		if (_head != 0 && headItem.getEnchantLevel() < 6)
		{
			return false;
		}
		if (_feet != 0 && feetItem.getEnchantLevel() < 6)
		{
			return false;
		}
		
		chestItem = null;
		legsItem = null;
		headItem = null;
		glovesItem = null;
		feetItem = null;
		
		return true;
	}
}

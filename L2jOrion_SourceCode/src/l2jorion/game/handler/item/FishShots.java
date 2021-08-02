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
package l2jorion.game.handler.item;

import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;

public class FishShots implements IItemHandler
{
	// All the item IDs that this handler knows.
	private static final int[] ITEM_IDS =
	{
		6535,
		6536,
		6537,
		6538,
		6539,
		6540
	};
	private static final int[] SKILL_IDS =
	{
		2181,
		2182,
		2183,
		2184,
		2185,
		2186
	};
	
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance activeChar = (L2PcInstance) playable;
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		L2Weapon weaponItem = activeChar.getActiveWeaponItem();
		
		if (weaponInst == null || weaponItem.getItemType() != L2WeaponType.ROD)
		{
			return;
		}
		
		if (weaponInst.getChargedFishshot())
		{
			// spiritshot is already active
			return;
		}
		
		final int FishshotId = item.getItemId();
		final int grade = weaponItem.getCrystalType();
		final int count = item.getCount();
		
		weaponItem = null;
		
		if (grade == L2Item.CRYSTAL_NONE && FishshotId != 6535 || grade == L2Item.CRYSTAL_D && FishshotId != 6536 || grade == L2Item.CRYSTAL_C && FishshotId != 6537 || grade == L2Item.CRYSTAL_B && FishshotId != 6538 || grade == L2Item.CRYSTAL_A && FishshotId != 6539
			|| grade == L2Item.CRYSTAL_S && FishshotId != 6540)
		{
			// 1479 - This fishing shot is not fit for the fishing pole crystal.
			activeChar.sendPacket(new SystemMessage(SystemMessageId.WRONG_FISHINGSHOT_GRADE));
			return;
		}
		
		if (count < 1)
		{
			return;
		}
		
		weaponInst.setChargedFishshot(true);
		activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), 1, null, false);
		final L2Object oldTarget = activeChar.getTarget();
		activeChar.setTarget(activeChar);
		
		activeChar.broadcastPacket(new MagicSkillUser(activeChar, SKILL_IDS[grade], 1, 0, 0), 360000);
		activeChar.setTarget(oldTarget);
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}

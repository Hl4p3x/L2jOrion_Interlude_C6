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

import l2jorion.Config;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExAutoSoulShot;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Stats;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2Weapon;

public class SoulShots implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		5789,
		1835,
		1463,
		1464,
		1465,
		1466,
		1467
	};
	
	private static final int[] SKILL_IDS =
	{
		2039,
		2150,
		2151,
		2152,
		2153,
		2154
	};
	
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance activeChar = (L2PcInstance) playable;
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		L2Weapon weaponItem = activeChar.getActiveWeaponItem();
		final int itemId = item.getItemId();
		
		if (weaponInst == null || weaponItem.getSoulShotCount() == 0)
		{
			if (!activeChar.getAutoSoulShot().contains(itemId))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_SOULSHOTS));
			}
			return;
		}
		
		// Check for correct grade
		final int weaponGrade = weaponItem.getCrystalType();
		
		if (weaponGrade == L2Item.CRYSTAL_NONE && (itemId != 5789 && itemId != 1835 && itemId != 10010) || weaponGrade == L2Item.CRYSTAL_D && (itemId != 1463 && itemId != 10000) || weaponGrade == L2Item.CRYSTAL_C && (itemId != 1464 && itemId != 10001)
			|| weaponGrade == L2Item.CRYSTAL_B && (itemId != 1465 && itemId != 10002) || weaponGrade == L2Item.CRYSTAL_A && (itemId != 1466 && itemId != 10003) || weaponGrade == L2Item.CRYSTAL_S && (itemId != 1467 && itemId != 10004))
		{
			if (!activeChar.getAutoSoulShot().contains(itemId))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.SOULSHOTS_GRADE_MISMATCH));
			}
			return;
		}
		
		activeChar.soulShotLock.lock();
		
		try
		{
			// Check if Soulshot is already active
			if (weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
			{
				return;
			}
			
			// Consume Soulshots if player has enough of them
			final int saSSCount = (int) activeChar.getStat().calcStat(Stats.SOULSHOT_COUNT, 0, null, null);
			final int SSCount = saSSCount == 0 ? weaponItem.getSoulShotCount() : saSSCount;
			
			if ((!Config.DONT_DESTROY_SS) && (itemId != 10000 && itemId != 10002 && itemId != 10003 && itemId != 10004 && itemId != 10010))
			{
				if (!activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), SSCount, null, false))
				{
					if (activeChar.getAutoSoulShot().contains(itemId))
					{
						activeChar.removeAutoSoulShot(itemId);
						activeChar.sendPacket(new ExAutoSoulShot(itemId, 0));
						SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
						sm.addString(item.getItem().getName());
						activeChar.sendPacket(sm);
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_SOULSHOTS));
					}
					return;
				}
			}
			
			// Charge soulshot
			weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_SOULSHOT);
		}
		finally
		{
			activeChar.soulShotLock.unlock();
		}
		
		// Send message to client
		activeChar.sendPacket(new SystemMessage(SystemMessageId.ENABLED_SOULSHOT));
		
		if (!activeChar.getEffects())
		{
			activeChar.broadcastPacket(new MagicSkillUser(activeChar, activeChar, SKILL_IDS[weaponGrade], 1, 0, 0), 500);
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}

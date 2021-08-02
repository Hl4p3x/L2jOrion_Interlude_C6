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
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2BabyPetInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExAutoSoulShot;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Weapon;

public class BeastSpiritShot implements IItemHandler
{
	// All the item IDs that this handler knows.
	private static final int[] ITEM_IDS =
	{
		6646,
		6647
	};
	
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		if (playable == null)
		{
			return;
		}
		
		L2PcInstance activeOwner = null;
		
		if (playable instanceof L2Summon)
		{
			activeOwner = ((L2Summon) playable).getOwner();
			activeOwner.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_USE_ITEM));
			return;
		}
		else if (playable instanceof L2PcInstance)
		{
			activeOwner = (L2PcInstance) playable;
		}
		
		if (activeOwner == null)
		{
			return;
		}
		
		L2Summon activePet = activeOwner.getPet();
		if (activePet == null)
		{
			activeOwner.sendPacket(new SystemMessage(SystemMessageId.PETS_ARE_NOT_AVAILABLE_AT_THIS_TIME));
			return;
		}
		
		if (activePet.isDead())
		{
			activeOwner.sendPacket(new SystemMessage(SystemMessageId.SOULSHOTS_AND_SPIRITSHOTS_ARE_NOT_AVAILABLE_FOR_A_DEAD_PET));
			return;
		}
		
		final int itemId = item.getItemId();
		final boolean isBlessed = itemId == 6647;
		int shotConsumption = 1;
		
		L2ItemInstance weaponInst = null;
		L2Weapon weaponItem = null;
		
		if (activePet instanceof L2PetInstance && !(activePet instanceof L2BabyPetInstance))
		{
			weaponInst = ((L2PetInstance) activePet).getActiveWeaponInstance();
			weaponItem = ((L2PetInstance) activePet).getActiveWeaponItem();
			
			if (weaponInst == null)
			{
				activeOwner.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_SPIRITSHOTS));
				return;
			}
			
			if (weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
			{
				// SpiritShots are already active.
				return;
			}
			
			final int shotCount = item.getCount();
			shotConsumption = weaponItem.getSpiritShotCount();
			
			weaponItem = null;
			
			if (shotConsumption == 0)
			{
				activeOwner.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_SPIRITSHOTS));
				return;
			}
			
			if (!(shotCount > shotConsumption))
			{
				// Not enough SpiritShots to use.
				activeOwner.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_SPIRITHOTS_FOR_PET));
				return;
			}
			
			if (isBlessed)
			{
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
			}
			else
			{
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_SPIRITSHOT);
			}
		}
		else
		{
			if (activePet.getChargedSpiritShot() != L2ItemInstance.CHARGED_NONE)
			{
				return;
			}
			
			if (isBlessed)
			{
				activePet.setChargedSpiritShot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
			}
			else
			{
				activePet.setChargedSpiritShot(L2ItemInstance.CHARGED_SPIRITSHOT);
			}
		}
		
		if (!Config.DONT_DESTROY_SS)
		{
			if (!activeOwner.destroyItemWithoutTrace("Consume", item.getObjectId(), shotConsumption, null, false))
			{
				if (activeOwner.getAutoSoulShot().contains(itemId))
				{
					activeOwner.removeAutoSoulShot(itemId);
					activeOwner.sendPacket(new ExAutoSoulShot(itemId, 0));
					SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
					sm.addString(item.getItem().getName());
					activeOwner.sendPacket(sm);
					return;
				}
				
				activeOwner.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_SPIRITSHOTS));
				return;
			}
		}
		
		// Pet uses the power of spirit.
		activeOwner.sendPacket(new SystemMessage(SystemMessageId.PET_USE_THE_POWER_OF_SPIRIT));
		if (!activeOwner.getEffects())
		{
			activeOwner.broadcastPacket(new MagicSkillUser(activePet, activePet, isBlessed ? 2009 : 2008, 1, 0, 0), 500);
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}

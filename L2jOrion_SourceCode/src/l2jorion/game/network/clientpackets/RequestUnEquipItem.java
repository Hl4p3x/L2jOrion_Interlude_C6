/*
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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class RequestUnEquipItem extends L2GameClientPacket
{
	private static Logger LOG = LoggerFactory.getLogger(RequestUnEquipItem.class.getName());

	// cd
	private int _slot;

	/**
	 * packet type id 0x11 format: cd
	 */
	@Override
	protected void readImpl()
	{
		_slot = readD();
	}

	@Override
	protected void runImpl()
	{
		if(Config.DEBUG)
		{
			LOG.info("request unequip slot " + _slot);
		}

		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if (activeChar.isSubmitingPin())
		{
			activeChar.sendMessage("Unable to do any action while PIN is not submitted");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if(activeChar._haveFlagCTF)
		{
			activeChar.sendMessage("You can't unequip a CTF flag.");
			return;
		}

		L2ItemInstance item = activeChar.getInventory().getPaperdollItemByL2ItemId(_slot);
		if(item != null && item.isWear())
			// Wear-items are not to be unequipped
			return;

		// Prevent of unequiping a cursed weapon
		if(_slot == L2Item.SLOT_LR_HAND && activeChar.isCursedWeaponEquiped())
			// Message ?
			return;

		// Prevent player from unequipping items in special conditions
		if(activeChar.isStunned() || activeChar.isConfused() || activeChar.isParalyzed() || activeChar.isSleeping() || activeChar.isAlikeDead())
		{
			activeChar.sendMessage("Your status does not allow you to do that.");
			return;
		}

		if(/*activeChar.isAttackingNow() || */activeChar.isCastingNow() || activeChar.isCastingPotionNow())
			return;

		if(activeChar.isMoving() && activeChar.isAttackingNow() && (_slot == L2Item.SLOT_LR_HAND || _slot == L2Item.SLOT_L_HAND || _slot == L2Item.SLOT_R_HAND))
		{
			L2Object target = activeChar.getTarget();
			activeChar.setTarget(null);
			activeChar.stopMove(null);
			activeChar.setTarget(target);
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK);
		}

		// Remove augmentation bonus
		if(item != null && item.isAugmented())
		{
			item.getAugmentation().removeBoni(activeChar);
		}

		L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(_slot);

		// show the update in the inventory
		InventoryUpdate iu = new InventoryUpdate();

		for(L2ItemInstance element : unequiped)
		{
			activeChar.checkSSMatch(null, element);

			iu.addModifiedItem(element);
		}

		activeChar.sendPacket(iu);

		activeChar.broadcastUserInfo();

		// this can be 0 if the user pressed the right mouse button twice very fast
		if(unequiped.length > 0)
		{

			SystemMessage sm = null;
			if(unequiped[0].getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
				sm.addNumber(unequiped[0].getEnchantLevel());
				sm.addItemName(unequiped[0].getItemId());
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(unequiped[0].getItemId());
			}
			activeChar.sendPacket(sm);
		}
	}

	@Override
	public String getType()
	{
		return "[C] 11 RequestUnequipItem";
	}
}

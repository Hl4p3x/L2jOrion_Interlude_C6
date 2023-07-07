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

import l2jorion.game.datatables.SkillTable;
import l2jorion.game.geo.GeoData;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.SystemMessage;

public class ScrollOfResurrection implements IItemHandler
{
	// all the items ids that this handler knows
	private static final int[] ITEM_IDS =
	{
		737,
		3936,
		3959,
		6387
	};
	
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		final L2PcInstance activeChar = playable.getActingPlayer();
		
		if (!(playable instanceof L2PcInstance))
		{
			activeChar.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return;
		}
		
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			return;
		}
		
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("This item cannot be used on Olympiad Games.");
		}
		
		if (activeChar.isMovementDisabled())
		{
			return;
		}
		
		final int itemId = item.getItemId();
		final boolean humanScroll = itemId == 3936 || itemId == 3959 || itemId == 737;
		// final boolean petScroll = itemId == 6387;
		
		// SoR Animation section
		L2Character target = (L2Character) activeChar.getTarget();
		if ((target == null) || !target.isDead())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}
		
		if (!GeoData.getInstance().canSeeTarget(activeChar, target))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		L2PcInstance targetPlayer = null;
		if (target instanceof L2PcInstance)
		{
			targetPlayer = (L2PcInstance) target;
		}
		
		L2PetInstance targetPet = null;
		if (target instanceof L2PetInstance)
		{
			targetPet = (L2PetInstance) target;
		}
		
		if (targetPlayer != null || targetPet != null)
		{
			boolean condGood = true;
			
			// check target is not in a active siege zone
			Castle castle = null;
			
			if (targetPlayer != null)
			{
				castle = CastleManager.getInstance().getCastle(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
			}
			else if (targetPet != null)
			{
				castle = CastleManager.getInstance().getCastle(targetPet.getX(), targetPet.getY(), targetPet.getZ());
			}
			
			if (castle != null && castle.getSiege().getIsInProgress())
			{
				if (castle.getSiege().getFlagCount(activeChar.getClan()) > 0 || castle.getSiege().getTowerCount(activeChar.getClan()) > 0)
				{
					condGood = true;
				}
				else
				{
					condGood = false;
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE));
				}
			}
			
			if (targetPet != null)
			{
				if (targetPet.getOwner() != activeChar)
				{
					if (targetPet.getOwner().isReviveRequested())
					{
						if (targetPet.getOwner().isRevivingPet())
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
						}
						else
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_RES)); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
						}
						condGood = false;
					}
				}
			}
			else if (targetPlayer != null)
			{
				if (targetPlayer.isFestivalParticipant()) // Check to see if the current player target is in a festival.
				{
					condGood = false;
					activeChar.sendPacket(SystemMessage.sendString("You may not resurrect participants in a festival."));
				}
				if (targetPlayer.isReviveRequested())
				{
					if (targetPlayer.isRevivingPet())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
					}
					condGood = false;
				}
				else if (!humanScroll)
				{
					condGood = false;
					activeChar.sendMessage("You do not have the correct scroll.");
				}
			}
			
			if (condGood)
			{
				int skillId = 0;
				final int skillLevel = 1;
				
				switch (itemId)
				{
					case 737:
						skillId = 2014;
						break; // Scroll of Resurrection
					case 3936:
						skillId = 2049;
						break; // Blessed Scroll of Resurrection
					case 3959:
						skillId = 2062;
						break; // L2Day - Blessed Scroll of Resurrection
					case 6387:
						skillId = 2179;
						break; // Blessed Scroll of Resurrection: For Pets
				}
				
				if (skillId != 0)
				{
					final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
					activeChar.useMagic(skill, true, true);
					
					// Consume the scroll
					if (!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
					{
						return;
					}
					
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(itemId);
					activeChar.sendPacket(sm);
				}
			}
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
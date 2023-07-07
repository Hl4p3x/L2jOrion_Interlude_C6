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
package l2jorion.game.network.clientpackets;

import l2jorion.game.datatables.sql.L2PetDataTable;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.handler.ItemHandler;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.PetInfo;
import l2jorion.game.network.serverpackets.PetItemList;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestPetUseItem extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(RequestPetUseItem.class);
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getUseItem().tryPerformAction("pet use item"))
		{
			return;
		}
		
		final L2PetInstance pet = (L2PetInstance) activeChar.getPet();
		
		if (pet == null)
		{
			return;
		}
		
		final L2ItemInstance item = pet.getInventory().getItemByObjectId(_objectId);
		
		if (item == null)
		{
			return;
		}
		
		if (item.isWear())
		{
			return;
		}
		
		final int itemId = item.getItemId();
		
		if (activeChar.isAlikeDead() || pet.isDead())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addItemName(item.getItemId());
			activeChar.sendPacket(sm);
			return;
		}
		
		// check if the item matches the pet
		if (item.isEquipable())
		{
			if (L2PetDataTable.isWolf(pet.getNpcId()) && // wolf
				item.getItem().isForWolf())
			{
				useItem(pet, item, activeChar);
				return;
			}
			else if (L2PetDataTable.isHatchling(pet.getNpcId()) && // hatchlings
				item.getItem().isForHatchling())
			{
				useItem(pet, item, activeChar);
				return;
			}
			else if (L2PetDataTable.isStrider(pet.getNpcId()) && // striders
				item.getItem().isForStrider())
			{
				useItem(pet, item, activeChar);
				return;
			}
			else if (L2PetDataTable.isBaby(pet.getNpcId()) && // baby pets (buffalo, cougar, kookaboora)
				item.getItem().isForBabyPet())
			{
				useItem(pet, item, activeChar);
				return;
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.ITEM_NOT_FOR_PETS));
				return;
			}
		}
		else if (L2PetDataTable.isPetFood(itemId))
		{
			if (L2PetDataTable.isWolf(pet.getNpcId()) && L2PetDataTable.isWolfFood(itemId))
			{
				feed(activeChar, pet, item);
				return;
			}
			
			if (L2PetDataTable.isSinEater(pet.getNpcId()) && L2PetDataTable.isSinEaterFood(itemId))
			{
				feed(activeChar, pet, item);
				return;
			}
			else if (L2PetDataTable.isHatchling(pet.getNpcId()) && L2PetDataTable.isHatchlingFood(itemId))
			{
				feed(activeChar, pet, item);
				return;
			}
			else if (L2PetDataTable.isStrider(pet.getNpcId()) && L2PetDataTable.isStriderFood(itemId))
			{
				feed(activeChar, pet, item);
				return;
			}
			else if (L2PetDataTable.isWyvern(pet.getNpcId()) && L2PetDataTable.isWyvernFood(itemId))
			{
				feed(activeChar, pet, item);
				return;
			}
			else if (L2PetDataTable.isBaby(pet.getNpcId()) && L2PetDataTable.isBabyFood(itemId))
			{
				feed(activeChar, pet, item);
				return;
			}
		}
		
		final IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());
		
		if (handler != null)
		{
			useItem(pet, item, activeChar);
		}
		else
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.ITEM_NOT_FOR_PETS);
			activeChar.sendPacket(sm);
		}
		
		return;
	}
	
	private synchronized void useItem(final L2PetInstance pet, final L2ItemInstance item, final L2PcInstance activeChar)
	{
		if (item.isEquipable())
		{
			if (item.isEquipped())
			{
				pet.getInventory().unEquipItemInSlot(item.getEquipSlot());
			}
			else
			{
				pet.getInventory().equipItem(item);
			}
			
			activeChar.sendPacket(new PetItemList(pet));
			
			activeChar.sendPacket(new PetInfo(pet));
			pet.updateEffectIcons(true);
		}
		else
		{
			final IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());
			
			if (handler == null)
			{
				LOG.warn("no itemhandler registered for itemId:" + item.getItemId());
			}
			else
			{
				handler.useItem(pet, item);
			}
		}
	}
	
	private void feed(final L2PcInstance player, final L2PetInstance pet, final L2ItemInstance item)
	{
		if (pet.destroyItem("Feed", item.getObjectId(), 1, pet, false))
		{
			pet.setCurrentFed(pet.getCurrentFed() + 100);
			
			pet.checkFed();
			
			pet.broadcastPacket(new MagicSkillUser(pet, pet, 2048, 1, 0, 0));
		}
		
		player.sendPacket(new PetInfo(pet));
		pet.updateEffectIcons(true);
		pet.broadcastStatusUpdate();
	}
	
	@Override
	public String getType()
	{
		return "[C] 8a RequestPetUseItem";
	}
}

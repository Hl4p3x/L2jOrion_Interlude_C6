package l2jorion.game.handler.item;

import l2jorion.Config;
import l2jorion.game.datatables.xml.AugmentationData;
import l2jorion.game.enums.AchType;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ConfirmDlg;
import l2jorion.game.network.serverpackets.ExVariationCancelResult;
import l2jorion.game.network.serverpackets.ExVariationResult;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.game.util.Util;

public class CustomAugmentationSystem implements IItemHandler
{
	L2ItemInstance _targetItem = null;
	L2ItemInstance _refinerItem = null;
	private boolean _augmented = false;
	
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance activeChar = (L2PcInstance) playable;
		
		if (activeChar.getActiveWeaponInstance() == null)
		{
			activeChar.sendPacket(new ExVariationResult(0, 0, 0));
			activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS));
			activeChar.sendMessage("Put on your weapon.");
			return;
		}
		
		_targetItem = (L2ItemInstance) L2World.getInstance().findObject(activeChar.getActiveWeaponInstance().getObjectId());
		_refinerItem = activeChar.getInventory().getItemByItemId(item.getItemId());
		
		if (_targetItem == null || //
			_refinerItem == null || //
			_targetItem.getOwnerId() != activeChar.getObjectId() || //
			_refinerItem.getOwnerId() != activeChar.getObjectId() || //
			activeChar.getLevel() < 46) // must be lvl 46
		{
			activeChar.sendPacket(new ExVariationResult(0, 0, 0));
			activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS));
			return;
		}
		
		if (_targetItem.isAugmented())
		{
			sendDlgMessage(_targetItem.getItemName() + " is already augmented. If you want to remove it - click on OK", activeChar);
			_augmented = true;
			return;
		}
		
		sendDlgMessage(_targetItem.getItemName() + " will be augmented", activeChar);
	}
	
	public void sendDlgMessage(final String text, final L2PcInstance player)
	{
		player.dialogAugmentation = this;
		final ConfirmDlg dlg = new ConfirmDlg(1326);
		dlg.addString(text);
		player.sendPacket(dlg);
	}
	
	public void onDlgAnswer(L2PcInstance activeChar, int answer)
	{
		// Removal
		if (_augmented)
		{
			if (answer == 1)
			{
				// unequip item
				if (_targetItem.isEquipped())
				{
					activeChar.disarmWeapons();
				}
				
				// remove the augmentation
				_targetItem.removeAugmentation(activeChar);
				
				// send ExVariationCancelResult
				activeChar.sendPacket(new ExVariationCancelResult(1));
				
				// send inventory update
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(_targetItem);
				activeChar.sendPacket(iu);
				
				// send system message
				final SystemMessage sm = new SystemMessage(SystemMessageId.AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1);
				sm.addString(_targetItem.getItemName());
				activeChar.sendPacket(sm);
			}
			else
			{
				activeChar.sendMessage("Augmentation removal cancelled.");
			}
			
			_augmented = false;
			return;
		}
		
		// Augmentation
		if (answer == 1)
		{
			// unequip item
			if (_targetItem.isEquipped())
			{
				activeChar.disarmWeapons();
			}
			
			if (TryAugmentItem(activeChar, _targetItem, _refinerItem))
			{
				final int stat12 = 0x0000FFFF & _targetItem.getAugmentation().getAugmentationId();
				final int stat34 = _targetItem.getAugmentation().getAugmentationId() >> 16;
				activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
				activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED));
			}
			else
			{
				activeChar.sendPacket(new ExVariationResult(0, 0, 0));
				activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS));
			}
			
			activeChar.sendPacket(new ExVariationResult(0, 0, 0));
		}
		else
		{
			activeChar.sendMessage("Augmentation cancelled.");
		}
	}
	
	boolean TryAugmentItem(final L2PcInstance player, final L2ItemInstance targetItem, final L2ItemInstance refinerItem)
	{
		if (targetItem.isAugmented() || targetItem.isWear())
		{
			player.sendMessage("You can't augment items while you wear it");
			return false;
		}
		
		if (player.isDead())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD));
			return false;
		}
		
		if (player.isSitting())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING_DOWN));
			return false;
		}
		
		if (player.isFishing())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING));
			return false;
		}
		
		if (player.isParalyzed())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED));
			return false;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.sendMessage("You cannot augment while trading");
			return false;
		}
		
		if (player.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION));
			return false;
		}
		
		// check for the items to be in the inventory of the owner
		if (player.getInventory().getItemByObjectId(refinerItem.getObjectId()) == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong LifeStone-id.", Config.DEFAULT_PUNISH);
			return false;
		}
		
		if (player.getInventory().getItemByObjectId(targetItem.getObjectId()) == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong Weapon-id.", Config.DEFAULT_PUNISH);
			return false;
		}
		
		// if (player.getInventory().getItemByObjectId(gemstoneItem.getObjectId()) == null)
		// {
		// Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong Gemstone-id.", Config.DEFAULT_PUNISH);
		// return false;
		// }
		
		final int itemGrade = targetItem.getItem().getItemGrade();
		final int itemType = targetItem.getItem().getType2();
		final int lifeStoneId = refinerItem.getItemId();
		// final int gemstoneItemId = gemstoneItem.getItemId();
		
		// is the refiner Item a life stone?
		if (lifeStoneId < 8723 || lifeStoneId > 8762)
		{
			return false;
		}
		
		// must be a weapon, must be > d grade
		if (itemGrade < L2Item.CRYSTAL_C || itemType != L2Item.TYPE2_WEAPON || !targetItem.isDestroyable())
		{
			return false;
		}
		
		// player must be able to use augmentation
		if (player.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE || player.isDead() || player.isParalyzed() || player.isFishing() || player.isSitting())
		{
			return false;
		}
		
		// int modifyGemstoneCount = _gemstoneCount;
		final int lifeStoneLevel = getLifeStoneLevel(lifeStoneId);
		final int lifeStoneGrade = getLifeStoneGrade(lifeStoneId);
		/*
		 * switch (itemGrade) { case L2Item.CRYSTAL_C: if (player.getLevel() < 46 || gemstoneItemId != 2130) { return false; } modifyGemstoneCount = 20; break; case L2Item.CRYSTAL_B: if (player.getLevel() < 52 || gemstoneItemId != 2130) { return false; } modifyGemstoneCount = 30; break; case
		 * L2Item.CRYSTAL_A: if (player.getLevel() < 61 || gemstoneItemId != 2131) { return false; } modifyGemstoneCount = 20; break; case L2Item.CRYSTAL_S: if (player.getLevel() < 76 || gemstoneItemId != 2131) { return false; } modifyGemstoneCount = 25; break; }
		 */
		
		// check if the lifestone is appropriate for this player
		switch (lifeStoneLevel)
		{
			case 1:
				if (player.getLevel() < 46)
				{
					return false;
				}
				break;
			case 2:
				if (player.getLevel() < 49)
				{
					return false;
				}
				break;
			case 3:
				if (player.getLevel() < 52)
				{
					return false;
				}
				break;
			case 4:
				if (player.getLevel() < 55)
				{
					return false;
				}
				break;
			case 5:
				if (player.getLevel() < 58)
				{
					return false;
				}
				break;
			case 6:
				if (player.getLevel() < 61)
				{
					return false;
				}
				break;
			case 7:
				if (player.getLevel() < 64)
				{
					return false;
				}
				break;
			case 8:
				if (player.getLevel() < 67)
				{
					return false;
				}
				break;
			case 9:
				if (player.getLevel() < 70)
				{
					return false;
				}
				break;
			case 10:
				if (player.getLevel() < 76)
				{
					return false;
				}
				break;
		}
		
		// Check if player has all gemstorne on inventory
		// if (gemstoneItem.getCount() - modifyGemstoneCount < 0)
		// {
		// return false;
		// }
		
		// consume the life stone
		if (Config.SCROLL_STACKABLE)
		{
			if (!player.destroyItem("RequestRefine", refinerItem.getObjectId(), 1, null, false))
			{
				return false;
			}
		}
		else
		{
			if (!player.destroyItem("RequestRefine", refinerItem, null, false))
			{
				return false;
			}
		}
		
		// consume the gemstones
		// player.destroyItem("RequestRefine", _gemstoneItemObjId, modifyGemstoneCount, null, false);
		
		// generate augmentation
		targetItem.setAugmentation(AugmentationData.getInstance().generateRandomAugmentation(targetItem, lifeStoneLevel, lifeStoneGrade, player, true));
		
		// finish and send the inventory update packet
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		player.sendPacket(iu);
		
		final StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.getAchievement().increase(AchType.AUGMENT);
		return true;
	}
	
	private int getLifeStoneGrade(int itemId)
	{
		itemId -= 8723;
		if (itemId < 10)
		{
			return 0; // normal grade
		}
		
		if (itemId < 20)
		{
			return 1; // mid grade
		}
		
		if (itemId < 30)
		{
			return 2; // high grade
		}
		
		return 3; // top grade
	}
	
	private int getLifeStoneLevel(int itemId)
	{
		itemId -= 10 * getLifeStoneGrade(itemId);
		itemId -= 8722;
		return itemId;
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	// XXX IDS
	private static final int ITEM_IDS[] =
	{
		8723,
		8724,
		8725,
		8726,
		8727,
		8728,
		8729,
		8730,
		8731,
		8732,
		8733,
		8734,
		8735,
		8736,
		8737,
		8738,
		8739,
		8740,
		8741,
		8742,
		8743,
		8744,
		8745,
		8746,
		8747,
		8748,
		8749,
		8750,
		8751,
		8752,
		8753,
		8754,
		8755,
		8756,
		8757,
		8758,
		8759,
		8760,
		8761,
		8762
	};
}
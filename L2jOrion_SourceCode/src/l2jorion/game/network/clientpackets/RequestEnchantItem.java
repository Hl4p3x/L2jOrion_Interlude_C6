/* This program is free software; you can redistribute it and/or modify
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
import l2jorion.game.datatables.xml.AugmentScrollData;
import l2jorion.game.datatables.xml.AugmentScrollData.L2AugmentScroll;
import l2jorion.game.datatables.xml.AugmentationData;
import l2jorion.game.enums.AchType;
import l2jorion.game.model.L2Augmentation;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.base.Race;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.EnchantResult;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.util.IllegalPlayerAction;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public final class RequestEnchantItem extends L2GameClientPacket
{
	private static Logger LOG = LoggerFactory.getLogger(RequestEnchantItem.class);
	
	private static final int[] CRYSTAL_SCROLLS =
	{
		731,
		732,
		949,
		950,
		953,
		954,
		957,
		958,
		961,
		962
	};
	
	private static final int[] NORMAL_WEAPON_SCROLLS =
	{
		729,
		947,
		951,
		955,
		959
	};
	
	private static final int[] BLESSED_WEAPON_SCROLLS =
	{
		6569,
		6571,
		6573,
		6575,
		6577
	};
	
	private static final int[] CRYSTAL_WEAPON_SCROLLS =
	{
		731,
		949,
		953,
		957,
		961
	};
	
	private static final int[] NORMAL_ARMOR_SCROLLS =
	{
		730,
		948,
		952,
		956,
		960
	};
	
	private static final int[] BLESSED_ARMOR_SCROLLS =
	{
		6570,
		6572,
		6574,
		6576,
		6578
	};
	
	private static final int[] CRYSTAL_ARMOR_SCROLLS =
	{
		732,
		950,
		954,
		958,
		962
	};
	
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null || _objectId == 0)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getUseAugItem().tryPerformAction("use enchant item"))
		{
			LOG.info(activeChar.getName() + " tried flood on ITEM enchanter.");
			activeChar.setActiveEnchantItem(null);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.isSubmitingPin())
		{
			activeChar.sendMessage("Unable to do any action while PIN is not submitted");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.getActiveTradeList() != null)
		{
			activeChar.cancelActiveTrade();
			activeChar.sendMessage("Your trade canceled.");
			return;
		}
		
		// Fix enchant transactions
		if (activeChar.isProcessingTransaction())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			activeChar.setActiveEnchantItem(null);
			return;
		}
		
		if (activeChar.isOnline() == 0)
		{
			activeChar.setActiveEnchantItem(null);
			return;
		}
		
		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		L2ItemInstance scroll = activeChar.getActiveEnchantItem();
		activeChar.setActiveEnchantItem(null);
		
		if (item == null || scroll == null)
		{
			activeChar.setActiveEnchantItem(null);
			activeChar.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
			activeChar.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		handleAugmentScrolls(activeChar, item, scroll);
		
		// can't enchant rods and shadow items
		if (item.getItem().getItemType() == L2WeaponType.ROD || item.isShadowItem())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			activeChar.setActiveEnchantItem(null);
			return;
		}
		
		if (!Config.ENCHANT_HERO_WEAPON && item.getItemId() >= 6611 && item.getItemId() <= 6621)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			activeChar.setActiveEnchantItem(null);
			return;
		}
		
		if (item.isWear())
		{
			activeChar.setActiveEnchantItem(null);
			Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to enchant a weared Item", IllegalPlayerAction.PUNISH_KICK);
			return;
		}
		
		int itemType2 = item.getItem().getType2();
		boolean enchantItem = false;
		boolean blessedScroll = false;
		boolean crystalScroll = false;
		int crystalId = 0;
		
		switch (item.getItem().getCrystalType())
		{
			case L2Item.CRYSTAL_A:
				crystalId = 1461;
				switch (scroll.getItemId())
				{
					case 729:
					case 731:
					case 6569:
						if (itemType2 == L2Item.TYPE2_WEAPON)
						{
							enchantItem = true;
						}
						break;
					case 730:
					case 732:
					case 6570:
						if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
						{
							enchantItem = true;
						}
						break;
				}
				break;
			case L2Item.CRYSTAL_B:
				crystalId = 1460;
				switch (scroll.getItemId())
				{
					case 947:
					case 949:
					case 6571:
						if (itemType2 == L2Item.TYPE2_WEAPON)
						{
							enchantItem = true;
						}
						break;
					case 948:
					case 950:
					case 6572:
						if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
						{
							enchantItem = true;
						}
						break;
				}
				break;
			case L2Item.CRYSTAL_C:
				crystalId = 1459;
				switch (scroll.getItemId())
				{
					case 951:
					case 953:
					case 6573:
						if (itemType2 == L2Item.TYPE2_WEAPON)
						{
							enchantItem = true;
						}
						break;
					case 952:
					case 954:
					case 6574:
						if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
						{
							enchantItem = true;
						}
						break;
				}
				break;
			case L2Item.CRYSTAL_D:
				crystalId = 1458;
				switch (scroll.getItemId())
				{
					case 955:
					case 957:
					case 6575:
						if (itemType2 == L2Item.TYPE2_WEAPON)
						{
							enchantItem = true;
						}
						break;
					case 956:
					case 958:
					case 6576:
						if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
						{
							enchantItem = true;
						}
						break;
				}
				break;
			case L2Item.CRYSTAL_S:
				crystalId = 1462;
				switch (scroll.getItemId())
				{
					case 959:
					case 961:
					case 6577:
						if (itemType2 == L2Item.TYPE2_WEAPON)
						{
							enchantItem = true;
						}
						break;
					case 960:
					case 962:
					case 6578:
						if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
						{
							enchantItem = true;
						}
						break;
				}
				break;
		}
		
		if (!enchantItem)
		{
			return;
		}
		
		// Get the scroll type
		if (scroll.getItemId() >= 6569 && scroll.getItemId() <= 6578)
		{
			blessedScroll = true;
		}
		else
		{
			for (int crystalscroll : CRYSTAL_SCROLLS)
			{
				if (scroll.getItemId() == crystalscroll)
				{
					crystalScroll = true;
					break;
				}
			}
		}
		
		SystemMessage sm;
		int chance = 0;
		int maxEnchantLevel = 0;
		int minEnchantLevel = 0;
		
		if (item.getItem().getType2() == L2Item.TYPE2_WEAPON)
		{
			if (blessedScroll)
			{
				for (int blessedweaponscroll : BLESSED_WEAPON_SCROLLS)
				{
					if (scroll.getItemId() == blessedweaponscroll)
					{
						if (item.getEnchantLevel() >= Config.BLESS_WEAPON_ENCHANT_LEVEL.size())
						{
							chance = Config.BLESS_WEAPON_ENCHANT_LEVEL.get(Config.BLESS_WEAPON_ENCHANT_LEVEL.size());
						}
						else
						{
							chance = Config.BLESS_WEAPON_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
						}
						
						if (Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							switch (item.getItem().getCrystalType())
							{
								case L2Item.CRYSTAL_S:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_S;
									break;
								case L2Item.CRYSTAL_A:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_A;
									break;
								case L2Item.CRYSTAL_B:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_B;
									break;
								case L2Item.CRYSTAL_C:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_C;
									break;
								case L2Item.CRYSTAL_D:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_D;
									break;
							}
						}
						
						if (!Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							maxEnchantLevel = Config.ENCHANT_WEAPON_MAX;
							break;
						}
					}
				}
			}
			else if (crystalScroll)
			{
				for (int crystalweaponscroll : CRYSTAL_WEAPON_SCROLLS)
				{
					if (scroll.getItemId() == crystalweaponscroll)
					{
						if (item.getEnchantLevel() >= Config.CRYSTAL_WEAPON_ENCHANT_LEVEL.size())
						{
							chance = Config.CRYSTAL_WEAPON_ENCHANT_LEVEL.get(Config.CRYSTAL_WEAPON_ENCHANT_LEVEL.size());
						}
						else
						{
							chance = Config.CRYSTAL_WEAPON_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
						}
						
						minEnchantLevel = Config.CRYSTAL_ENCHANT_MIN;
						maxEnchantLevel = Config.CRYSTAL_ENCHANT_MAX;
						
						break;
					}
				}
			}
			else
			{ // normal scrolls
				for (int normalweaponscroll : NORMAL_WEAPON_SCROLLS)
				{
					if (scroll.getItemId() == normalweaponscroll)
					{
						if (item.getEnchantLevel() >= Config.NORMAL_WEAPON_ENCHANT_LEVEL.size())
						{
							chance = Config.NORMAL_WEAPON_ENCHANT_LEVEL.get(Config.NORMAL_WEAPON_ENCHANT_LEVEL.size());
						}
						else
						{
							chance = Config.NORMAL_WEAPON_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
						}
						
						if (Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							switch (item.getItem().getCrystalType())
							{
								case L2Item.CRYSTAL_S:
									maxEnchantLevel = Config.ENCHANT_MAX_S;
									break;
								case L2Item.CRYSTAL_A:
									maxEnchantLevel = Config.ENCHANT_MAX_A;
									break;
								case L2Item.CRYSTAL_B:
									maxEnchantLevel = Config.ENCHANT_MAX_B;
									break;
								case L2Item.CRYSTAL_C:
									maxEnchantLevel = Config.ENCHANT_MAX_C;
									break;
								case L2Item.CRYSTAL_D:
									maxEnchantLevel = Config.ENCHANT_MAX_D;
									break;
							}
						}
						
						if (!Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							maxEnchantLevel = Config.ENCHANT_WEAPON_MAX;
							break;
						}
					}
				}
			}
			
		}
		else if (item.getItem().getType2() == L2Item.TYPE2_SHIELD_ARMOR)
		{
			if (blessedScroll)
			{
				for (int blessedarmorscroll : BLESSED_ARMOR_SCROLLS)
				{
					if (scroll.getItemId() == blessedarmorscroll)
					{
						if (item.getEnchantLevel() >= Config.BLESS_ARMOR_ENCHANT_LEVEL.size())
						{
							chance = Config.BLESS_ARMOR_ENCHANT_LEVEL.get(Config.BLESS_ARMOR_ENCHANT_LEVEL.size());
						}
						else
						{
							chance = Config.BLESS_ARMOR_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
						}
						
						if (Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							switch (item.getItem().getCrystalType())
							{
								case L2Item.CRYSTAL_S:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_S;
									break;
								case L2Item.CRYSTAL_A:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_A;
									break;
								case L2Item.CRYSTAL_B:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_B;
									break;
								case L2Item.CRYSTAL_C:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_C;
									break;
								case L2Item.CRYSTAL_D:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_D;
									break;
							}
						}
						
						if (!Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							maxEnchantLevel = Config.ENCHANT_ARMOR_MAX;
							break;
						}
					}
				}
			}
			else if (crystalScroll)
			{
				for (int crystalarmorscroll : CRYSTAL_ARMOR_SCROLLS)
				{
					if (scroll.getItemId() == crystalarmorscroll)
					{
						if (item.getEnchantLevel() >= Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.size())
						{
							chance = Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.get(Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.size());
						}
						else
						{
							chance = Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
						}
						
						minEnchantLevel = Config.CRYSTAL_ENCHANT_MIN;
						maxEnchantLevel = Config.CRYSTAL_ENCHANT_MAX;
						
						break;
					}
				}
				
			}
			else
			{ // normal scrolls
				for (int normalarmorscroll : NORMAL_ARMOR_SCROLLS)
				{
					if (scroll.getItemId() == normalarmorscroll)
					{
						if (item.getEnchantLevel() >= Config.NORMAL_ARMOR_ENCHANT_LEVEL.size())
						{
							chance = Config.NORMAL_ARMOR_ENCHANT_LEVEL.get(Config.NORMAL_ARMOR_ENCHANT_LEVEL.size());
						}
						else
						{
							chance = Config.NORMAL_ARMOR_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
						}
						
						if (Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							switch (item.getItem().getCrystalType())
							{
								case L2Item.CRYSTAL_S:
									maxEnchantLevel = Config.ENCHANT_MAX_S;
									break;
								case L2Item.CRYSTAL_A:
									maxEnchantLevel = Config.ENCHANT_MAX_A;
									break;
								case L2Item.CRYSTAL_B:
									maxEnchantLevel = Config.ENCHANT_MAX_B;
									break;
								case L2Item.CRYSTAL_C:
									maxEnchantLevel = Config.ENCHANT_MAX_C;
									break;
								case L2Item.CRYSTAL_D:
									maxEnchantLevel = Config.ENCHANT_MAX_D;
									break;
							}
						}
						
						if (!Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							maxEnchantLevel = Config.ENCHANT_ARMOR_MAX;
							break;
						}
					}
				}
				
			}
			
		}
		else if (item.getItem().getType2() == L2Item.TYPE2_ACCESSORY)
		{
			if (blessedScroll)
			{
				
				for (int blessedjewelscroll : BLESSED_ARMOR_SCROLLS)
				{
					if (scroll.getItemId() == blessedjewelscroll)
					{
						if (item.getEnchantLevel() >= Config.BLESS_JEWELRY_ENCHANT_LEVEL.size())
						{
							chance = Config.BLESS_JEWELRY_ENCHANT_LEVEL.get(Config.BLESS_JEWELRY_ENCHANT_LEVEL.size());
						}
						else
						{
							chance = Config.BLESS_JEWELRY_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
						}
						
						if (Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							switch (item.getItem().getCrystalType())
							{
								case L2Item.CRYSTAL_S:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_S;
									break;
								case L2Item.CRYSTAL_A:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_A;
									break;
								case L2Item.CRYSTAL_B:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_B;
									break;
								case L2Item.CRYSTAL_C:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_C;
									break;
								case L2Item.CRYSTAL_D:
									maxEnchantLevel = Config.BLESSED_ENCHANT_MAX_D;
									break;
							}
						}
						
						if (!Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							maxEnchantLevel = Config.ENCHANT_JEWELRY_MAX;
							break;
						}
					}
				}
				
			}
			else if (crystalScroll)
			{
				for (int crystaljewelscroll : CRYSTAL_ARMOR_SCROLLS)
				{
					if (scroll.getItemId() == crystaljewelscroll)
					{
						if (item.getEnchantLevel() >= Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.size())
						{
							chance = Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.get(Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.size());
						}
						else
						{
							chance = Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
						}
						
						minEnchantLevel = Config.CRYSTAL_ENCHANT_MIN;
						maxEnchantLevel = Config.CRYSTAL_ENCHANT_MAX;
						
						break;
					}
				}
			}
			else
			{
				for (int normaljewelscroll : NORMAL_ARMOR_SCROLLS)
				{
					if (scroll.getItemId() == normaljewelscroll)
					{
						if (item.getEnchantLevel() >= Config.NORMAL_JEWELRY_ENCHANT_LEVEL.size())
						{
							chance = Config.NORMAL_JEWELRY_ENCHANT_LEVEL.get(Config.NORMAL_JEWELRY_ENCHANT_LEVEL.size());
						}
						else
						{
							chance = Config.NORMAL_JEWELRY_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
						}
						
						if (Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							switch (item.getItem().getCrystalType())
							{
								case L2Item.CRYSTAL_S:
									maxEnchantLevel = Config.ENCHANT_MAX_S;
									break;
								case L2Item.CRYSTAL_A:
									maxEnchantLevel = Config.ENCHANT_MAX_A;
									break;
								case L2Item.CRYSTAL_B:
									maxEnchantLevel = Config.ENCHANT_MAX_B;
									break;
								case L2Item.CRYSTAL_C:
									maxEnchantLevel = Config.ENCHANT_MAX_C;
									break;
								case L2Item.CRYSTAL_D:
									maxEnchantLevel = Config.ENCHANT_MAX_D;
									break;
							}
						}
						
						if (!Config.CUSTOM_ENCHANT_GRADES_SYSTEM)
						{
							maxEnchantLevel = Config.ENCHANT_JEWELRY_MAX;
							break;
						}
					}
				}
			}
			
		}
		
		if ((maxEnchantLevel != 0 && item.getEnchantLevel() >= maxEnchantLevel) || (item.getEnchantLevel()) < minEnchantLevel)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			return;
		}
		
		if (Config.SCROLL_STACKABLE)
		{
			scroll = activeChar.getInventory().destroyItem("Enchant", scroll.getObjectId(), 1, activeChar, item);
		}
		else
		{
			scroll = activeChar.getInventory().destroyItem("Enchant", scroll, activeChar, item);
		}
		
		if (scroll == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to enchant with a scroll he doesnt have", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (item.getEnchantLevel() < Config.ENCHANT_SAFE_MAX || item.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR && item.getEnchantLevel() < Config.ENCHANT_SAFE_MAX_FULL)
		{
			chance = 100;
		}
		
		int rndValue = Rnd.get(100);
		
		if (Config.ENABLE_DWARF_ENCHANT_BONUS && activeChar.getRace() == Race.dwarf)
		{
			if (activeChar.getLevel() >= Config.DWARF_ENCHANT_MIN_LEVEL)
			{
				rndValue -= Config.DWARF_ENCHANT_BONUS;
			}
		}
		
		Object aChance = item.fireEvent("calcEnchantChance", new Object[chance]);
		if (aChance != null)
		{
			chance = (Integer) aChance;
		}
		
		synchronized (item)
		{
			if (rndValue < chance)
			{
				if (item.getOwnerId() != activeChar.getObjectId())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
					return;
				}
				
				if (item.getLocation() != L2ItemInstance.ItemLocation.INVENTORY && item.getLocation() != L2ItemInstance.ItemLocation.PAPERDOLL)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
					return;
				}
				
				if (item.getEnchantLevel() == 0)
				{
					sm = new SystemMessage(SystemMessageId.S1_SUCCESSFULLY_ENCHANTED);
					sm.addItemName(item.getItemId());
					activeChar.sendPacket(sm);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item.getItemId());
					activeChar.sendPacket(sm);
				}
				
				item.setEnchantLevel(item.getEnchantLevel() + Config.CUSTOM_ENCHANT_VALUE);
				item.updateDatabase();
				
				if (activeChar.getAchievement().getCount(item.isWeapon() ? AchType.ENCHANT_WEAPON : AchType.ENCHANT_OTHER) < item.getEnchantLevel())
				{
					activeChar.getAchievement().increase(item.isWeapon() ? AchType.ENCHANT_WEAPON : AchType.ENCHANT_OTHER, item.getEnchantLevel(), false, false);
				}
				
				activeChar.getAchievement().increase(AchType.ENCHANT_SUCCESS);
			}
			else
			{
				if (crystalScroll)
				{
					sm = SystemMessage.sendString("Failed in Crystal Enchant. The enchant value of the item became: " + item.getEnchantLevel());
					activeChar.sendPacket(sm);
					activeChar.getAchievement().increase(AchType.ENCHANT_FAILED);
				}
				else if (blessedScroll)
				{
					sm = new SystemMessage(SystemMessageId.BLESSED_ENCHANT_FAILED);
					activeChar.sendPacket(sm);
					activeChar.getAchievement().increase(AchType.ENCHANT_FAILED);
				}
				else
				{
					if (item.getEnchantLevel() > 0)
					{
						sm = new SystemMessage(SystemMessageId.ENCHANTMENT_FAILED_S1_S2_EVAPORATED);
						sm.addNumber(item.getEnchantLevel());
						sm.addItemName(item.getItemId());
						activeChar.sendPacket(sm);
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.ENCHANTMENT_FAILED_S1_EVAPORATED);
						sm.addItemName(item.getItemId());
						activeChar.sendPacket(sm);
					}
					activeChar.getAchievement().increase(AchType.ENCHANT_FAILED);
				}
				
				if (!blessedScroll && !crystalScroll)
				{
					if (!Config.PROTECT_NORMAL_SCROLLS)
					{
						if (item.getEnchantLevel() > 0)
						{
							sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
							sm.addNumber(item.getEnchantLevel());
							sm.addItemName(item.getItemId());
							activeChar.sendPacket(sm);
						}
						else
						{
							sm = new SystemMessage(SystemMessageId.S1_DISARMED);
							sm.addItemName(item.getItemId());
							activeChar.sendPacket(sm);
						}
						
						if (item.isEquipped())
						{
							if (item.isAugmented())
							{
								item.getAugmentation().removeBoni(activeChar);
							}
							
							L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInSlotAndRecord(item.getEquipSlot());
							
							InventoryUpdate iu = new InventoryUpdate();
							for (L2ItemInstance element : unequiped)
							{
								iu.addModifiedItem(element);
							}
							activeChar.sendPacket(iu);
							
							activeChar.broadcastUserInfo();
						}
						
						int count = item.getCrystalCount() - (item.getItem().getCrystalCount() + 1) / 2;
						if (count < 1)
						{
							count = 1;
						}
						
						if (item.fireEvent("enchantFail", new Object[] {}) != null)
						{
							return;
						}
						
						L2ItemInstance destroyItem = activeChar.getInventory().destroyItem("Enchant", item, activeChar, null);
						if (destroyItem == null)
						{
							return;
						}
						
						L2ItemInstance crystals = activeChar.getInventory().addItem("Enchant", crystalId, count, activeChar, destroyItem);
						
						sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(crystals.getItemId());
						sm.addNumber(count);
						activeChar.sendPacket(sm);
						
						if (!Config.FORCE_INVENTORY_UPDATE)
						{
							InventoryUpdate iu = new InventoryUpdate();
							if (destroyItem.getCount() == 0)
							{
								iu.addRemovedItem(destroyItem);
							}
							else
							{
								iu.addModifiedItem(destroyItem);
							}
							iu.addItem(crystals);
							activeChar.sendPacket(iu);
						}
						else
						{
							activeChar.sendPacket(new ItemList(activeChar, true));
						}
						
						StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
						su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
						activeChar.sendPacket(su);
						
						activeChar.broadcastUserInfo();
						
						L2World world = L2World.getInstance();
						world.removeObject(destroyItem);
					}
					else
					{
						item.setEnchantLevel(0);
						item.updateDatabase();
					}
				}
				else
				{
					if (blessedScroll)
					{
						if (!Config.EXPLLOSIVE_CUSTOM)
						{
							item.setEnchantLevel(Config.BREAK_ENCHANT);
							item.updateDatabase();
						}
						else
						{
							if (item.getEnchantLevel() >= 9)
							{
								item.setEnchantLevel(item.getEnchantLevel() - 5);
							}
							else
							{
								item.setEnchantLevel(4);
							}
							
							item.updateDatabase();
						}
					}
					else if (crystalScroll)
					{
						item.setEnchantLevel(item.getEnchantLevel());
						item.updateDatabase();
					}
				}
			}
		}
		
		StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
		activeChar.sendPacket(su);
		
		activeChar.sendPacket(new EnchantResult(item.getEnchantLevel()));
		activeChar.sendPacket(new ItemList(activeChar, false));
		activeChar.broadcastUserInfo();
	}
	
	private static void handleAugmentScrolls(L2PcInstance player, L2ItemInstance item, L2ItemInstance scroll)
	{
		// get scroll augment data
		L2AugmentScroll enchant = AugmentScrollData.getInstance().getScroll(scroll);
		if (enchant != null)
		{
			if (item.getItem().getType2() != L2Item.TYPE2_WEAPON)
			{
				player.sendMessage("This scroll can be used only on weapon.");
				player.setActiveEnchantItem(null);
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			
			if (item.getItem().getCrystalType() == L2Item.CRYSTAL_NONE || item.getItem().getCrystalType() == L2Item.CRYSTAL_D || item.getItem().getCrystalType() == L2Item.CRYSTAL_C)
			{
				player.sendMessage("You can't augment this grade item.");
				player.setActiveEnchantItem(null);
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			
			if (item.isHeroItem())
			{
				player.sendMessage("You can't augment hero item " + item.getItemName() + ".");
				player.setActiveEnchantItem(null);
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			
			if (item.isAugmented())
			{
				player.sendMessage("This item is already augmented.");
				player.setActiveEnchantItem(null);
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			
			if (item.getOwnerId() != player.getObjectId())
			{
				player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
				player.setActiveEnchantItem(null);
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			
			if (!player.destroyItemByItemId("", enchant.getAugmentScrollId(), 1, null, true))
			{
				return;
			}
			
			// unequip item
			if (item.isEquipped())
			{
				L2ItemInstance[] unequipped = player.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
				InventoryUpdate iu = new InventoryUpdate();
				
				for (L2ItemInstance itm : unequipped)
				{
					iu.addModifiedItem(itm);
				}
				
				player.sendPacket(iu);
				player.broadcastUserInfo();
			}
			
			int skill = enchant.getAugmentSkillId();
			int level = enchant.getAugmentSkillLv();
			
			final L2Augmentation aug = AugmentationData.getInstance().generateAugmentationWithSkill(item, skill, level);
			item.setAugmentation(aug);
			
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(item);
			player.sendPacket(iu);
			
			StatusUpdate su = new StatusUpdate(player.getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
			player.sendPacket(su);
			
			player.broadcastUserInfo();
			player.sendSkillList();
			player.setActiveEnchantItem(null);
			player.sendPacket(EnchantResult.CANCELLED);
			player.sendMessage("You successfully augmented the weapon.");
			player.sendPacket(new PlaySound("ItemSound3.sys_enchant_success"));
			return;
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 58 RequestEnchantItem";
	}
}

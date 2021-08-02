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

import java.util.Arrays;
import java.util.List;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.handler.ItemHandler;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.EtcStatusUpdate;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.ShowCalculator;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class UseItem extends L2GameClientPacket
{
	public static Logger LOG = LoggerFactory.getLogger(UseItem.class.getName());
	
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
		if (activeChar == null)
		{
			return;
		}
		
		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		if (item == null)
		{
			return;
		}
		// Flood protect UseItem
		if (item.isPotion())
		{
			if (!getClient().getFloodProtectors().getUsePotion().tryPerformAction("use potion"))
			{
				return;
			}
		}
		else
		{
			if (!getClient().getFloodProtectors().getUseItem().tryPerformAction("use item"))
			{
				return;
			}
		}
		
		// Like L2OFF you can't use soulshots while sitting
		final int[] shots_ids =
		{
			5789,
			1835,
			1463,
			1464,
			1465,
			1466,
			1467,
			5790,
			2509,
			2510,
			2511,
			2512,
			2513,
			2514,
			3947,
			3948,
			3949,
			3950,
			3951,
			3952,
			10000, // Soulshot: D-grade
			10001, // Soulshot: C-grade
			10002, // Soulshot: B-grade
			10003, // Soulshot: A-grade
			10004, // Soulshot: S-grade
			10005, // Blessed Spiritshot: D-Grade
			10006, // Blessed Spiritshot: C-Grade
			10007, // Blessed Spiritshot: B-Grade
			10008, // Blessed Spiritshot: A-Grade
			10009, // Blessed Spiritshot: S Grade
			10010,
			10011
		};
		
		if (activeChar.isSitting() && Arrays.toString(shots_ids).contains(String.valueOf(item.getItemId())))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_AUTO_USE_LACK_OF_S1);
			sm.addItemName(item.getItemId());
			activeChar.sendPacket(sm);
			return;
		}
		
		if (activeChar.isStunned() || activeChar.isConfused() || activeChar.isParalyzed() || activeChar.isSleeping())
		{
			activeChar.sendMessage("You can't use an item right now.");
			return;
		}
		
		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.getActiveTradeList() != null)
		{
			activeChar.cancelActiveTrade();
		}
		
		if (item.isWear())
		{
			return;
		}
		
		if ((item.getItemId() == 1538 || item.getItemId() == 3958 || item.getItemId() == 5858 || item.getItemId() == 5859 || item.getItemId() == 9156) && activeChar.isArenaProtection())
		{
			activeChar.sendMessage("You can not use this item in Tournament.");
			return;
		}
		
		if (item.getItem().getType2() == L2Item.TYPE2_QUEST)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_USE_QUEST_ITEMS);
			activeChar.sendPacket(sm);
			return;
		}
		
		int itemId = item.getItemId();
		
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0 && (itemId == 736 || itemId == 1538 || itemId == 1829 || itemId == 1830 || itemId == 3958 || itemId == 5858 || itemId == 5859 || itemId == 6663 || itemId == 6664 || itemId >= 7117 && itemId <= 7135
			|| itemId >= 7554 && itemId <= 7559 || itemId == 7618 || itemId == 7619 || itemId == 10129 || itemId == 10130))
		{
			return;
		}
		
		// Items that cannot be used
		if (itemId == 57)
		{
			return;
		}
		
		if ((itemId == 5858) && (ClanHallManager.getInstance().getAbstractHallByOwner(activeChar.getClan()) == null))
		{
			activeChar.sendMessage("Blessed Scroll of Escape: Clan Hall cannot be used due to unsuitable terms.");
			return;
		}
		else if ((itemId == 5859) && (CastleManager.getInstance().getCastleByOwner(activeChar.getClan()) == null))
		{
			activeChar.sendMessage("Blessed Scroll of Escape: Castle cannot be used due to unsuitable terms.");
			return;
		}
		
		if (activeChar.isFishing() && (itemId < 6535 || itemId > 6540))
		{
			// You cannot do anything else while fishing
			SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
			getClient().getActiveChar().sendPacket(sm);
			return;
		}
		
		if (activeChar.getPkKills() > 0 && (itemId >= 7816 && itemId <= 7831))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
			SystemMessage sm2 = new SystemMessage(SystemMessageId.YOU_ARE_UNABLE_TO_EQUIP_THIS_ITEM_WHEN_YOUR_PK_COUNT_IS_GREATER_THAN_OR_EQUAL_TO_ONE);
			getClient().getActiveChar().sendPacket(sm);
			getClient().getActiveChar().sendPacket(sm2);
			return;
		}
		
		L2Clan cl = activeChar.getClan();
		if ((cl == null || cl.getHasCastle() == 0) && itemId == 7015 && Config.CASTLE_SHIELD && !activeChar.isGM())
		{
			activeChar.sendMessage("You can't equip that.");
			return;
		}
		
		// A shield that can only be used by the members of a clan that owns a clan hall.
		if ((cl == null || cl.getHasHideout() == 0) && itemId == 6902 && Config.CLANHALL_SHIELD && !activeChar.isGM())
		{
			activeChar.sendMessage("You can't equip that.");
			return;
		}
		
		// Apella armor used by clan members may be worn by a Baron or a higher level Aristocrat.
		if (itemId >= 7860 && itemId <= 7879 && Config.APELLA_ARMORS && (cl == null || activeChar.getPledgeClass() < 5) && !activeChar.isGM())
		{
			activeChar.sendMessage("You can't equip that.");
			return;
		}
		
		// Clan Oath armor used by all clan members
		if (itemId >= 7850 && itemId <= 7859 && Config.OATH_ARMORS && cl == null && !activeChar.isGM())
		{
			activeChar.sendMessage("You can't equip that.");
			return;
		}
		
		// The Lord's Crown used by castle lords only
		if (itemId == 6841 && Config.CASTLE_CROWN && (cl == null || cl.getHasCastle() == 0 || !activeChar.isClanLeader()) && !activeChar.isGM())
		{
			activeChar.sendMessage("You can't equip that.");
			return;
		}
		
		// Scroll of resurrection like L2OFF if you are casting you can't use them
		if ((itemId == 737 || itemId == 3936 || itemId == 3959 || itemId == 6387) && activeChar.isCastingNow())
		{
			return;
		}
		
		// Castle circlets used by the members of a clan that owns a castle, academy members are excluded.
		if (Config.CASTLE_CIRCLETS && (itemId >= 6834 && itemId <= 6840 || itemId == 8182 || itemId == 8183))
		{
			if (cl == null)
			{
				activeChar.sendMessage("You can't equip that.");
				return;
			}
			
			int circletId = CastleManager.getInstance().getCircletByCastleId(cl.getHasCastle());
			if (activeChar.getPledgeType() == -1 || circletId != itemId)
			{
				activeChar.sendMessage("You can't equip that.");
				return;
			}
		}
		
		// Char cannot use item when dead
		if (activeChar.isDead())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addItemName(itemId);
			getClient().getActiveChar().sendPacket(sm);
			return;
		}
		
		// Char cannot use pet items
		if (item.getItem().isForWolf() || item.getItem().isForHatchling() || item.getItem().isForStrider() || item.getItem().isForBabyPet())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_EQUIP_PET_ITEM); // You cannot equip a pet item.
			sm.addItemName(itemId);
			getClient().getActiveChar().sendPacket(sm);
			return;
		}
		
		if (item.isEquipable())
		{
			// No unequipping/equipping while the player is in special conditions
			if (activeChar.isFishing() || activeChar.isStunned() || activeChar.isSleeping() || activeChar.isParalyzed() || activeChar.isAlikeDead())
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
				return;
			}
			
			// Anti Over enchant cheat
			if (Config.MAX_ITEM_ENCHANT_KICK > 0 && !activeChar.isGM() && item.getEnchantLevel() > Config.MAX_ITEM_ENCHANT_KICK)
			{
				activeChar.sendMessage("You have been kicked for using an item overenchanted!");
				Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " has item Overenchanted! Kicked ", Config.DEFAULT_PUNISH);
				// activeChar.closeNetConnection();
				return;
			}
			
			int bodyPart = item.getItem().getBodyPart();
			
			// Like L2OFF you can't use equips while you are casting
			if ((activeChar.isCastingNow() || activeChar.isCastingPotionNow() || (activeChar._inEventCTF && activeChar._haveFlagCTF)))
			{
				if (activeChar._inEventCTF && activeChar._haveFlagCTF)
				{
					activeChar.sendMessage("This item can not be equipped when you have the flag.");
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_USE_ITEM_WHILE_USING_MAGIC);
					activeChar.sendPacket(sm);
				}
				return;
			}
			
			if (activeChar.isMounted())
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
				return;
			}
			
			// Enchants
			if (Config.PROTECTED_ENCHANT)
			{
				switch (bodyPart)
				{
					case L2Item.SLOT_LR_HAND:
					case L2Item.SLOT_L_HAND:
					case L2Item.SLOT_R_HAND:
					{
						if ((item.getEnchantLevel() > Config.NORMAL_WEAPON_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.BLESS_WEAPON_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.CRYSTAL_WEAPON_ENCHANT_LEVEL.size()) && !activeChar.isGM())
						{
							activeChar.sendMessage("You have been banned for using an item wich is over enchanted!"); // message
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " has item Overenchanted! ", Config.DEFAULT_PUNISH);
							return;
						}
						break;
					}
					case L2Item.SLOT_CHEST:
					case L2Item.SLOT_BACK:
					case L2Item.SLOT_GLOVES:
					case L2Item.SLOT_FEET:
					case L2Item.SLOT_HEAD:
					case L2Item.SLOT_FULL_ARMOR:
					case L2Item.SLOT_LEGS:
					{
						if ((item.getEnchantLevel() > Config.NORMAL_ARMOR_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.BLESS_ARMOR_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.size()) && !activeChar.isGM())
						{
							activeChar.sendMessage("You have been banned for using an item wich is over enchanted!"); // message
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " has item Overenchanted! ", Config.DEFAULT_PUNISH);
							return;
						}
						break;
					}
					case L2Item.SLOT_R_EAR:
					case L2Item.SLOT_L_EAR:
					case L2Item.SLOT_NECK:
					case L2Item.SLOT_R_FINGER:
					case L2Item.SLOT_L_FINGER:
					{
						if ((item.getEnchantLevel() > Config.NORMAL_JEWELRY_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.BLESS_JEWELRY_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.size()) && !activeChar.isGM())
						{
							activeChar.sendMessage("You have been banned for using an item wich is over enchanted!"); // message
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " has item Overenchanted! ", Config.DEFAULT_PUNISH);
							return;
						}
						break;
					}
				}
			}
			
			if (item.isFakeArmor())
			{
				// Don't allowed use fake items over the formal wear.
				List<L2ItemInstance> formal = activeChar.getInventory().getItemsByItemId(6408);
				for (L2ItemInstance tmp : formal)
				{
					if (tmp.isEquipped())
					{
						activeChar.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
						return;
					}
				}
				
				if (activeChar.getFakeArmorObjectId() == item.getObjectId())
				{
					activeChar.setFakeArmorObjectId(0);
					activeChar.setFakeArmorItemId(0);
				}
				else
				{
					activeChar.setFakeArmorObjectId(item.getObjectId());
					activeChar.setFakeArmorItemId(item.getItemId());
				}
				
				activeChar.broadcastUserInfo();
				activeChar.sendPacket(new ItemList(activeChar, false));
			}
			else
			{
				
				// Don't allow weapon/shield equipment if a cursed weapon is equiped
				if (activeChar.isCursedWeaponEquiped() && (bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND))
				{
					return;
				}
				
				// Don't allow weapon/shield hero equipment during Olimpia
				if (activeChar.isInOlympiadMode()
					&& ((bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND) && (item.getItemId() >= 6611 && item.getItemId() <= 6621 || item.getItemId() == 6842) || Config.LIST_OLY_RESTRICTED_ITEMS.contains(item.getItemId())))
				{
					return;
				}
				
				// Don't allow Hero items equipment if not a hero
				if (!activeChar.isHero() && (item.getItemId() >= 6611 && item.getItemId() <= 6621 || item.getItemId() == 6842) && !activeChar.isGM())
				{
					return;
				}
				
				if (activeChar.isMoving() && activeChar.isAttackingNow() && (bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND))
				{
					L2Object target = activeChar.getTarget();
					activeChar.setTarget(null);
					activeChar.stopMove(null);
					activeChar.setTarget(target);
					activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK);
				}
				
				// Don't allow to put formal wear
				if ((activeChar.getFakeArmorObjectId() > 0 && bodyPart != L2Item.SLOT_LR_HAND && bodyPart != L2Item.SLOT_L_HAND && bodyPart != L2Item.SLOT_R_HAND || activeChar.isCursedWeaponEquipped() && itemId == 6408))
				{
					activeChar.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
					return;
				}
				
				// Elrokian Trap like L2OFF, add skills
				if (itemId == 8763)
				{
					if (!item.isEquipped())
					{
						activeChar.addSkill(SkillTable.getInstance().getInfo(3626, 1));
						activeChar.addSkill(SkillTable.getInstance().getInfo(3627, 1));
						activeChar.addSkill(SkillTable.getInstance().getInfo(3628, 1));
						activeChar.sendSkillList();
					}
				}
				
				if (Config.TOMASZ_B_CUSTOM)
				{
					// cloak
					if (itemId == 10107)
					{
						activeChar.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_FLAME);
					}
				}
				
				// Equip or unEquip
				L2ItemInstance[] items = null;
				boolean isEquiped = item.isEquipped();
				SystemMessage sm = null;
				
				if (item.getItem().getType2() == L2Item.TYPE2_WEAPON)
				{
					// if used item is a weapon
					L2ItemInstance wep = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
					if (wep == null)
					{
						wep = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
					}
					
					activeChar.checkSSMatch(item, wep);
				}
				
				// Remove the item if it's equiped
				if (isEquiped)
				{
					// Elrokian Trap like L2OFF, remove skills
					if (itemId == 8763)
					{
						activeChar.removeSkill(3626, true);
						activeChar.removeSkill(3627, true);
						activeChar.removeSkill(3628, true);
						activeChar.sendSkillList();
					}
					
					if (item.getEnchantLevel() > 0)
					{
						sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
						sm.addNumber(item.getEnchantLevel());
						sm.addItemName(itemId);
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.S1_DISARMED);
						sm.addItemName(itemId);
					}
					
					activeChar.sendPacket(sm);
					
					// Remove augementation bonus on unequipment
					if (item.isAugmented())
					{
						item.getAugmentation().removeBoni(activeChar);
					}
					
					int slot = activeChar.getInventory().getSlotFromItem(item);
					
					// remove cupid's bow skills on unequip
					if (item.isCupidBow())
					{
						if (item.getItemId() == 9140)
						{
							activeChar.removeSkill(SkillTable.getInstance().getInfo(3261, 1));
						}
						else
						{
							activeChar.removeSkill(SkillTable.getInstance().getInfo(3260, 0));
							activeChar.removeSkill(SkillTable.getInstance().getInfo(3262, 0));
						}
					}
					
					items = activeChar.getInventory().unEquipItemInBodySlotAndRecord(slot);
				}
				else
				{
					// Restrict bow weapon for class except Cupid bow.
					if (item.getItem() instanceof L2Weapon && ((L2Weapon) item.getItem()).getItemType() == L2WeaponType.BOW && !item.isCupidBow())
					{
						
						if (Config.DISABLE_BOW_CLASSES.contains(activeChar.getClassId().getId()) && !activeChar.isInOlympiadMode())
						{
							activeChar.sendMessage("This item can not be equipped by your class!");
							activeChar.sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
					}
					
					int tempBodyPart = item.getItem().getBodyPart();
					L2ItemInstance tempItem = activeChar.getInventory().getPaperdollItemByL2ItemId(tempBodyPart);
					
					// remove augmentation stats for replaced items
					// currently weapons only..
					if (tempItem != null && tempItem.isAugmented())
					{
						tempItem.getAugmentation().removeBoni(activeChar);
					}
					
					// check if the item replaces a wear-item
					if (tempItem != null && tempItem.isWear())
					{
						return;
					}
					else if (tempBodyPart == 0x4000) // left+right hand equipment
					{
						// this may not remove left OR right hand equipment
						tempItem = activeChar.getInventory().getPaperdollItem(7);
						if (tempItem != null && tempItem.isWear())
						{
							return;
						}
						
						tempItem = activeChar.getInventory().getPaperdollItem(8);
						if (tempItem != null && tempItem.isWear())
						{
							return;
						}
					}
					else if (tempBodyPart == 0x8000) // fullbody armor
					{
						// this may not remove chest or leggins
						tempItem = activeChar.getInventory().getPaperdollItem(10);
						if (tempItem != null && tempItem.isWear())
						{
							return;
						}
						
						tempItem = activeChar.getInventory().getPaperdollItem(11);
						if (tempItem != null && tempItem.isWear())
						{
							return;
						}
					}
					
					// Left hand
					tempItem = activeChar.getInventory().getPaperdollItem(7);
					
					// Elrokian Trap like L2OFF, remove skills
					if (tempItem != null && tempItem.getItemId() == 8763)
					{
						activeChar.removeSkill(3626, true);
						activeChar.removeSkill(3627, true);
						activeChar.removeSkill(3628, true);
						activeChar.sendSkillList();
					}
					
					if (Config.TOMASZ_B_CUSTOM)
					{
						// cloack
						if (itemId == 10107)
						{
							if (activeChar.getAbnormalEffect() == L2Character.ABNORMAL_EFFECT_FLAME)
							{
								activeChar.stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_FLAME);
							}
						}
					}
					
					if (item.getEnchantLevel() > 0)
					{
						sm = new SystemMessage(SystemMessageId.S1_S2_EQUIPPED);
						sm.addNumber(item.getEnchantLevel());
						sm.addItemName(itemId);
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
						sm.addItemName(itemId);
					}
					activeChar.sendPacket(sm);
					
					// Apply augementation boni on equip
					if (item.isAugmented())
					{
						item.getAugmentation().applyBoni(activeChar);
					}
					
					// Apply cupid's bow skills on equip
					if (item.isCupidBow())
					{
						if (item.getItemId() == 9140)
						{
							activeChar.addSkill(SkillTable.getInstance().getInfo(3261, 1));
						}
						else
						{
							activeChar.addSkill(SkillTable.getInstance().getInfo(3260, 0));
						}
						
						activeChar.addSkill(SkillTable.getInstance().getInfo(3262, 0));
					}
					
					items = activeChar.getInventory().equipItemAndRecord(item);
					
					if (item.getItem() instanceof L2Weapon)
					{
						// charge Soulshot/Spiritshot like L2OFF
						activeChar.rechargeAutoSoulShot(true, true, false);
					}
					// Consume mana - will start a task if required; returns if item is not a shadow item
					item.decreaseMana(false);
				}
				
				activeChar.abortAttack();
				activeChar.sendPacket(new EtcStatusUpdate(activeChar));
				
				// if an "invisible" item has changed (Jewels, helmet),
				// we dont need to send broadcast packet to all other users
				if (!((item.getItem().getBodyPart() & L2Item.SLOT_HEAD) > 0 || (item.getItem().getBodyPart() & L2Item.SLOT_NECK) > 0 || (item.getItem().getBodyPart() & L2Item.SLOT_L_EAR) > 0 || (item.getItem().getBodyPart() & L2Item.SLOT_R_EAR) > 0
					|| (item.getItem().getBodyPart() & L2Item.SLOT_L_FINGER) > 0 || (item.getItem().getBodyPart() & L2Item.SLOT_R_FINGER) > 0))
				{
					activeChar.broadcastUserInfo();
					InventoryUpdate iu = new InventoryUpdate();
					iu.addItems(Arrays.asList(items));
					activeChar.sendPacket(iu);
				}
				else
				{
					InventoryUpdate iu = new InventoryUpdate();
					iu.addItems(Arrays.asList(items));
					activeChar.sendPacket(iu);
					activeChar.sendPacket(new UserInfo(activeChar));
				}
			}
		}
		else
		{
			L2Weapon weaponItem = activeChar.getActiveWeaponItem();
			int itemid = item.getItemId();
			if (itemid == 4393)
			{
				activeChar.sendPacket(new ShowCalculator(4393));
			}
			else if (weaponItem != null && weaponItem.getItemType() == L2WeaponType.ROD && (itemid >= 6519 && itemid <= 6527 || itemid >= 7610 && itemid <= 7613 || itemid >= 7807 && itemid <= 7809 || itemid >= 8484 && itemid <= 8486 || itemid >= 8505 && itemid <= 8513))
			{
				activeChar.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
				activeChar.broadcastUserInfo();
				ItemList il = new ItemList(activeChar, false);
				sendPacket(il);
				return;
			}
			else
			{
				IItemHandler handler = ItemHandler.getInstance().getItemHandler(itemId);
				if (handler != null)
				{
					handler.useItem(activeChar, item);
				}
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 14 UseItem";
	}
	
}
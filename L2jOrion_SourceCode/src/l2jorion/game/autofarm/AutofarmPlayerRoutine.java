package l2jorion.game.autofarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import l2jorion.Config;
import l2jorion.game.ai.CtrlEvent;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.NextAction;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.geo.GeoData;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.handler.ItemHandler;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2ShortCut;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillTargetType;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.ShowCalculator;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Util;

public class AutofarmPlayerRoutine
{
	private final L2PcInstance player;
	private L2Object committedTarget = null;
	
	public AutofarmPlayerRoutine(L2PcInstance _player)
	{
		player = _player;
	}
	
	public void executeRoutine()
	{
		checkSpoil();
		targetEligibleCreature();
		checkManaPots();
		checkHealthPots();
		attack();
		checkSpoil();
	}
	
	private void attack()
	{
		boolean shortcutsContainAttack = shotcutsContainAttack();
		if (shortcutsContainAttack)
		{
			physicalAttack();
		}
		
		useAppropriateSpell();
		
		if (shortcutsContainAttack)
		{
			physicalAttack();
		}
	}
	
	private void useAppropriateSpell()
	{
		L2Skill chanceSkill = nextAvailableSkill(getChanceSpells(), AutofarmSpellType.Chance);
		
		if (chanceSkill != null)
		{
			useMagicSkill(chanceSkill, false);
			return;
		}
		
		L2Skill lowLifeSkill = nextAvailableSkill(getLowLifeSpells(), AutofarmSpellType.LowLife);
		
		if (lowLifeSkill != null)
		{
			useMagicSkill(lowLifeSkill, false);
			return;
		}
		
		L2Skill selfSkills = nextAvailableSkill(getSelfSpells(), AutofarmSpellType.Self);
		
		if (selfSkills != null)
		{
			useMagicSkill(selfSkills, true);
			return;
		}
		
		L2Skill attackSkill = nextAvailableSkill(getAttackSpells(), AutofarmSpellType.Attack);
		
		if (attackSkill != null)
		{
			useMagicSkill(attackSkill, false);
			return;
		}
	}
	
	public boolean checkDoCastConditions(L2Skill skill)
	{
		if (skill == null || player.isSkillDisabled(skill))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (!skill.isPotion() && player.isAllSkillsDisabled())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (skill.isMagic() && player.isMuted() && !skill.isPotion())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (!skill.isMagic() && player.isPsychicalMuted() && !skill.isPotion())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (player.getCurrentMp() < player.getStat().getMpConsume(skill) + player.getStat().getMpInitialConsume(skill))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (player.getCurrentHp() <= skill.getHpConsume())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_HP));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (skill.getSkillType() == SkillType.SIGNET || skill.getSkillType() == SkillType.SIGNET_CASTTIME)
		{
			L2WorldRegion region = player.getWorldRegion();
			if (region == null)
			{
				return false;
			}
			
			boolean canCast = true;
			if (skill.getTargetType() == SkillTargetType.TARGET_GROUND)
			{
				Location wp = player.getCurrentSkillWorldPosition();
				if (!region.checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ()))
				{
					canCast = false;
				}
			}
			else if (!region.checkEffectRangeInsidePeaceZone(skill, player.getX(), player.getY(), player.getZ()))
			{
				canCast = false;
			}
			
			if (!canCast)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(skill.getId());
				player.sendPacket(sm);
				return false;
			}
		}
		
		return true;
	}
	
	public L2Skill nextAvailableSkill(List<Integer> skillIds, AutofarmSpellType spellType)
	{
		for (Integer skillId : skillIds)
		{
			L2Skill skill = player.getSkill(skillId);
			
			if (skill == null)
			{
				continue;
			}
			
			if (!checkDoCastConditions(skill))
			{
				continue;
			}
			
			if (spellType == AutofarmSpellType.Chance && getMonsterTarget() != null)
			{
				if (isSpoil(skillId))
				{
					if (monsterIsAlreadySpoiled())
					{
						continue;
					}
					return skill;
				}
				
				if (getMonsterTarget().getFirstEffect(skillId) == null)
				{
					return skill;
				}
				continue;
			}
			
			if (spellType == AutofarmSpellType.LowLife && getMonsterTarget() != null && getHpPercentage() > AutofarmConstants.lowLifePercentageThreshold)
			{
				break;
			}
			
			if (spellType == AutofarmSpellType.Self)
			{
				if (skill.isToggle() && player.getFirstEffect(skillId) == null)
				{
					return skill;
				}
				
				if (player.getFirstEffect(skillId) == null)
				{
					return skill;
				}
				
				continue;
			}
			
			return skill;
		}
		
		return null;
	}
	
	private void checkHealthPots()
	{
		if (getHpPercentage() <= AutofarmConstants.useHpPotsPercentageThreshold)
		{
			if (player.getFirstEffect(AutofarmConstants.hpPotSkillId) != null)
			{
				return;
			}
			
			L2ItemInstance hpPots = player.getInventory().getItemByItemId(AutofarmConstants.hpPotItemId);
			if (hpPots != null)
			{
				useItem(hpPots);
			}
		}
	}
	
	private void checkManaPots()
	{
		
		if (getMpPercentage() <= AutofarmConstants.useMpPotsPercentageThreshold)
		{
			L2ItemInstance mpPots = player.getInventory().getItemByItemId(AutofarmConstants.mpPotItemId);
			if (mpPots != null)
			{
				useItem(mpPots);
			}
		}
	}
	
	private void checkSpoil()
	{
		if (canBeSweepedByMe() && getMonsterTarget().isDead())
		{
			L2Skill sweeper = player.getSkill(42);
			if (sweeper == null)
			{
				return;
			}
			
			useMagicSkill(sweeper, false);
		}
	}
	
	private Double getHpPercentage()
	{
		return player.getCurrentHp() * 100.0f / player.getMaxHp();
	}
	
	private Double getMpPercentage()
	{
		return player.getCurrentMp() * 100.0f / player.getMaxMp();
	}
	
	private boolean canBeSweepedByMe()
	{
		return getMonsterTarget() != null && getMonsterTarget().isDead() && getMonsterTarget().getSpoilerId() == player.getObjectId();
	}
	
	private boolean monsterIsAlreadySpoiled()
	{
		return getMonsterTarget() != null && getMonsterTarget().getSpoilerId() != 0;
	}
	
	private static boolean isSpoil(Integer skillId)
	{
		return skillId == 254 || skillId == 302;
	}
	
	private List<Integer> getAttackSpells()
	{
		return getSpellsInSlots(AutofarmConstants.attackSlots);
	}
	
	private List<Integer> getSpellsInSlots(List<Integer> attackSlots)
	{
		return Arrays.stream(player.getShortcutList().getAllShortCuts()).filter(shortcut -> shortcut.getPage() == AutofarmConstants.shortcutsPageIndex && shortcut.getType() == L2ShortCut.TYPE_SKILL && attackSlots.contains(shortcut.getSlot())).map(L2ShortCut::getId).collect(Collectors.toList());
	}
	
	private List<Integer> getChanceSpells()
	{
		return getSpellsInSlots(AutofarmConstants.chanceSlots);
	}
	
	private List<Integer> getSelfSpells()
	{
		return getSpellsInSlots(AutofarmConstants.selfSlots);
	}
	
	private List<Integer> getLowLifeSpells()
	{
		return getSpellsInSlots(AutofarmConstants.lowLifeSlots);
	}
	
	private boolean shotcutsContainAttack()
	{
		return Arrays.stream(player.getShortcutList().getAllShortCuts()).anyMatch(shortcut -> shortcut.getType() == L2ShortCut.TYPE_ACTION && shortcut.getId() == 2);
	}
	
	private void castSpellWithAppropriateTarget(L2Skill skill, Boolean forceOnSelf)
	{
		if (forceOnSelf)
		{
			L2Object oldTarget = player.getTarget();
			player.setTarget(player);
			player.useMagic(skill, false, false);
			player.setTarget(oldTarget);
			return;
		}
		
		player.useMagic(skill, false, false);
	}
	
	private void physicalAttack()
	{
		if (!(player.getTarget() instanceof L2MonsterInstance))
		{
			return;
		}
		
		L2MonsterInstance target = (L2MonsterInstance) player.getTarget();
		
		if (target.isAutoAttackable(player))
		{
			if (GeoData.getInstance().canSeeTarget(player, target))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
				player.onActionRequest();
			}
		}
		else
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			
			if (GeoData.getInstance().canSeeTarget(player, target))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, target);
			}
		}
	}
	
	public void targetEligibleCreature()
	{
		if (committedTarget != null)
		{
			if (!committedTarget.isDead() && !player.isMoving() && !GeoData.getInstance().canSeeTarget(player, committedTarget))
			{
				player.getAI().moveToPawn(committedTarget, 0);
				return;
			}
			
			if (!committedTarget.isDead() && GeoData.getInstance().canSeeTarget(player, committedTarget))
			{
				return;
			}
			
			committedTarget = null;
			player.setTarget(null);
		}
		
		List<L2MonsterInstance> targets = null;
		
		switch (player.getAutoFarmMode().toLowerCase())
		{
			case "random":
			{
				targets = getKnownMonstersInRadius(player, player.getAutoFarmRadius(), creature -> GeoData.getInstance().canMove(player.getX(), player.getY(), player.getZ(), creature.getX(), creature.getY(), creature.getZ(), 0) && !creature.isDead());
				break;
			}
			case "radius":
			{
				targets = getKnownTypeInRadius(player, player.getAutoFarmRadius(), creature -> GeoData.getInstance().canMove(player.getX(), player.getY(), player.getZ(), creature.getX(), creature.getY(), creature.getZ(), 0)).stream().filter(x -> !x.isDead()
					&& (x.isMonster())).collect(Collectors.toList());
				break;
			}
		}
		
		if (targets == null || targets.isEmpty())
		{
			return;
		}
		
		L2Object closestTarget = targets.stream().min((o1, o2) -> (int) Util.calculateDistance(o1, o2, false)).get();
		
		committedTarget = closestTarget;
		player.setTarget(closestTarget);
	}
	
	public final List<L2MonsterInstance> getKnownMonstersInRadius(L2PcInstance player, int radius, Function<L2MonsterInstance, Boolean> condition)
	{
		final L2WorldRegion region = player.getWorldRegion();
		if (region == null)
		{
			return Collections.emptyList();
		}
		
		final List<L2MonsterInstance> result = new ArrayList<>();
		
		for (L2WorldRegion reg : region.getSurroundingRegions())
		{
			for (L2Object obj : reg.getVisibleObjects().values())
			{
				if (!(obj instanceof L2MonsterInstance) || !Util.checkIfInRange(radius, player, obj, true) || !condition.apply((L2MonsterInstance) obj))
				{
					continue;
				}
				
				result.add((L2MonsterInstance) obj);
			}
		}
		
		return result;
	}
	
	public final List<L2MonsterInstance> getKnownTypeInRadius(L2PcInstance player, int radius, Function<L2MonsterInstance, Boolean> condition)
	{
		final L2WorldRegion region = player.getWorldRegion();
		if (region == null)
		{
			return Collections.emptyList();
		}
		
		final List<L2MonsterInstance> result = new ArrayList<>();
		
		for (L2WorldRegion reg : region.getSurroundingRegions())
		{
			for (L2Object obj : reg.getVisibleObjects().values())
			{
				if (!(obj instanceof L2MonsterInstance) || obj == player || !condition.apply((L2MonsterInstance) obj) || obj.isDead() || !Util.checkIfInRange(radius, obj, player, true))
				{
					continue;
				}
				
				if (!obj.isMonster())
				{
					continue;
				}
				
				if (player.getAutoFarmDistance() != null && !obj.isInsideRadius(player.getAutoFarmDistance().getX(), player.getAutoFarmDistance().getY(), player.getAutoFarmDistance().getZ(), player.getAutoFarmRadius(), false, false))
				{
					continue;
				}
				
				result.add((L2MonsterInstance) obj);
			}
		}
		
		return result;
	}
	
	public L2MonsterInstance getMonsterTarget()
	{
		if (!(player.getTarget() instanceof L2MonsterInstance))
		{
			return null;
		}
		
		return (L2MonsterInstance) player.getTarget();
	}
	
	private void useMagicSkill(L2Skill skill, Boolean forceOnSelf)
	{
		if (skill.getSkillType() == SkillType.RECALL && !Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && player.getKarma() > 0)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (skill.isToggle() && player.isMounted())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isAttackingNow())
		{
			player.getAI().setNextAction(new NextAction(CtrlEvent.EVT_READY_TO_ACT, CtrlIntention.AI_INTENTION_CAST, () -> castSpellWithAppropriateTarget(skill, forceOnSelf)));
		}
		else
		{
			castSpellWithAppropriateTarget(skill, forceOnSelf);
		}
	}
	
	public void useItem(L2ItemInstance item)
	{
		if (item.isPotion())
		{
			if (!player.getFloodProtectors().getUsePotion().tryPerformAction("use potion"))
			{
				return;
			}
		}
		else
		{
			if (!player.getFloodProtectors().getUseItem().tryPerformAction("use item"))
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
		};
		
		if (player.isSitting() && Arrays.toString(shots_ids).contains(String.valueOf(item.getItemId())))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_AUTO_USE_LACK_OF_S1);
			sm.addItemName(item.getItemId());
			player.sendPacket(sm);
			return;
		}
		
		if (player.isStunned() || player.isConfused() || player.isParalyzed() || player.isSleeping())
		{
			player.sendMessage("You can't use an item right now.");
			return;
		}
		
		if (player.getPrivateStoreType() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.cancelActiveTrade();
		}
		
		if (item.isWear())
		{
			return;
		}
		
		if ((item.getItemId() == 1538 || item.getItemId() == 3958 || item.getItemId() == 5858 || item.getItemId() == 5859 || item.getItemId() == 9156) && player.isArenaProtection())
		{
			player.sendMessage("You can not use this item in Tournament.");
			return;
		}
		
		if (item.getItem().getType2() == L2Item.TYPE2_QUEST)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_USE_QUEST_ITEMS);
			player.sendPacket(sm);
			return;
		}
		
		int itemId = item.getItemId();
		
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && player.getKarma() > 0 && (itemId == 736 || itemId == 1538 || itemId == 1829 || itemId == 1830 || itemId == 3958 || itemId == 5858 || itemId == 5859 || itemId == 6663 || itemId == 6664 || itemId >= 7117 && itemId <= 7135
			|| itemId >= 7554 && itemId <= 7559 || itemId == 7618 || itemId == 7619 || itemId == 10129 || itemId == 10130))
		{
			return;
		}
		
		// Items that cannot be used
		if (itemId == 57)
		{
			return;
		}
		
		if ((itemId == 5858) && (ClanHallManager.getInstance().getAbstractHallByOwner(player.getClan()) == null))
		{
			player.sendMessage("Blessed Scroll of Escape: Clan Hall cannot be used due to unsuitable terms.");
			return;
		}
		else if ((itemId == 5859) && (CastleManager.getInstance().getCastleByOwner(player.getClan()) == null))
		{
			player.sendMessage("Blessed Scroll of Escape: Castle cannot be used due to unsuitable terms.");
			return;
		}
		
		if (player.isFishing() && (itemId < 6535 || itemId > 6540))
		{
			// You cannot do anything else while fishing
			SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
			player.sendPacket(sm);
			return;
		}
		
		if (player.getPkKills() > 0 && (itemId >= 7816 && itemId <= 7831))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
			SystemMessage sm2 = new SystemMessage(SystemMessageId.YOU_ARE_UNABLE_TO_EQUIP_THIS_ITEM_WHEN_YOUR_PK_COUNT_IS_GREATER_THAN_OR_EQUAL_TO_ONE);
			player.sendPacket(sm);
			player.sendPacket(sm2);
			return;
		}
		
		L2Clan cl = player.getClan();
		if ((cl == null || cl.getHasCastle() == 0) && itemId == 7015 && Config.CASTLE_SHIELD && !player.isGM())
		{
			player.sendMessage("You can't equip that.");
			return;
		}
		
		// A shield that can only be used by the members of a clan that owns a clan hall.
		if ((cl == null || cl.getHasHideout() == 0) && itemId == 6902 && Config.CLANHALL_SHIELD && !player.isGM())
		{
			player.sendMessage("You can't equip that.");
			return;
		}
		
		// Apella armor used by clan members may be worn by a Baron or a higher level Aristocrat.
		if (itemId >= 7860 && itemId <= 7879 && Config.APELLA_ARMORS && (cl == null || player.getPledgeClass() < 5) && !player.isGM())
		{
			player.sendMessage("You can't equip that.");
			return;
		}
		
		// Clan Oath armor used by all clan members
		if (itemId >= 7850 && itemId <= 7859 && Config.OATH_ARMORS && cl == null && !player.isGM())
		{
			player.sendMessage("You can't equip that.");
			return;
		}
		
		// The Lord's Crown used by castle lords only
		if (itemId == 6841 && Config.CASTLE_CROWN && (cl == null || cl.getHasCastle() == 0 || !player.isClanLeader()) && !player.isGM())
		{
			player.sendMessage("You can't equip that.");
			return;
		}
		
		// Scroll of resurrection like L2OFF if you are casting you can't use them
		if ((itemId == 737 || itemId == 3936 || itemId == 3959 || itemId == 6387) && player.isCastingNow())
		{
			return;
		}
		
		// Castle circlets used by the members of a clan that owns a castle, academy members are excluded.
		if (Config.CASTLE_CIRCLETS && (itemId >= 6834 && itemId <= 6840 || itemId == 8182 || itemId == 8183))
		{
			if (cl == null)
			{
				player.sendMessage("You can't equip that.");
				return;
			}
			
			int circletId = CastleManager.getInstance().getCircletByCastleId(cl.getHasCastle());
			if (player.getPledgeType() == -1 || circletId != itemId)
			{
				player.sendMessage("You can't equip that.");
				return;
			}
		}
		
		// Char cannot use item when dead
		if (player.isDead())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addItemName(itemId);
			player.sendPacket(sm);
			return;
		}
		
		// Char cannot use pet items
		if (item.getItem().isForWolf() || item.getItem().isForHatchling() || item.getItem().isForStrider() || item.getItem().isForBabyPet())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_EQUIP_PET_ITEM); // You cannot equip a pet item.
			sm.addItemName(itemId);
			player.sendPacket(sm);
			return;
		}
		
		if (item.isEquipable())
		{
			// No unequipping/equipping while the player is in special conditions
			if (player.isFishing() || player.isStunned() || player.isSleeping() || player.isParalyzed() || player.isAlikeDead())
			{
				player.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
				return;
			}
			
			// Anti Over enchant cheat
			if (Config.MAX_ITEM_ENCHANT_KICK > 0 && !player.isGM() && item.getEnchantLevel() > Config.MAX_ITEM_ENCHANT_KICK)
			{
				player.sendMessage("You have been kicked for using an item overenchanted!");
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " has item Overenchanted! Kicked ", Config.DEFAULT_PUNISH);
				// player.closeNetConnection();
				return;
			}
			
			int bodyPart = item.getItem().getBodyPart();
			
			// Like L2OFF you can't use equips while you are casting
			if ((player.isCastingNow() || player.isCastingPotionNow() || (player._inEventCTF && player._haveFlagCTF)))
			{
				if (player._inEventCTF && player._haveFlagCTF)
				{
					player.sendMessage("This item can not be equipped when you have the flag.");
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_USE_ITEM_WHILE_USING_MAGIC);
					player.sendPacket(sm);
				}
				return;
			}
			
			if (player.isMounted())
			{
				player.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
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
						if ((item.getEnchantLevel() > Config.NORMAL_WEAPON_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.BLESS_WEAPON_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.CRYSTAL_WEAPON_ENCHANT_LEVEL.size()) && !player.isGM())
						{
							player.sendMessage("You have been banned for using an item wich is over enchanted!"); // message
							Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " has item Overenchanted! ", Config.DEFAULT_PUNISH);
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
						if ((item.getEnchantLevel() > Config.NORMAL_ARMOR_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.BLESS_ARMOR_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.size()) && !player.isGM())
						{
							player.sendMessage("You have been banned for using an item wich is over enchanted!"); // message
							Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " has item Overenchanted! ", Config.DEFAULT_PUNISH);
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
						if ((item.getEnchantLevel() > Config.NORMAL_JEWELRY_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.BLESS_JEWELRY_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.size()) && !player.isGM())
						{
							player.sendMessage("You have been banned for using an item wich is over enchanted!"); // message
							Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " has item Overenchanted! ", Config.DEFAULT_PUNISH);
							return;
						}
						break;
					}
				}
			}
			
			// Don't allow weapon/shield equipment if a cursed weapon is equiped
			if (player.isCursedWeaponEquiped() && (bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND))
			{
				return;
			}
			
			// Don't allow weapon/shield hero equipment during Olimpia
			if (player.isInOlympiadMode() && ((bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND) && (item.getItemId() >= 6611 && item.getItemId() <= 6621 || item.getItemId() == 6842) || Config.LIST_OLY_RESTRICTED_ITEMS.contains(item.getItemId())))
			{
				return;
			}
			
			// Don't allow Hero items equipment if not a hero
			if (!player.isHero() && (item.getItemId() >= 6611 && item.getItemId() <= 6621 || item.getItemId() == 6842) && !player.isGM())
			{
				return;
			}
			
			if (player.isMoving() && player.isAttackingNow() && (bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND))
			{
				L2Object target = player.getTarget();
				player.setTarget(null);
				player.stopMove(null);
				player.setTarget(target);
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK);
			}
			
			// Don't allow to put formal wear
			if ((player.getFakeArmorObjectId() > 0 && bodyPart != L2Item.SLOT_LR_HAND && bodyPart != L2Item.SLOT_L_HAND && bodyPart != L2Item.SLOT_R_HAND || player.isCursedWeaponEquipped() && itemId == 6408))
			{
				player.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
				return;
			}
			
			// Elrokian Trap like L2OFF, add skills
			if (itemId == 8763)
			{
				if (!item.isEquipped())
				{
					player.addSkill(SkillTable.getInstance().getInfo(3626, 1));
					player.addSkill(SkillTable.getInstance().getInfo(3627, 1));
					player.addSkill(SkillTable.getInstance().getInfo(3628, 1));
					player.sendSkillList();
				}
			}
			
			if (Config.TOMASZ_B_CUSTOM)
			{
				// cloak
				if (itemId == 10107)
				{
					player.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_FLAME);
				}
			}
			
			if (player.isCastingNow())
			{
				final NextAction nextAction = new NextAction(CtrlEvent.EVT_FINISH_CASTING, CtrlIntention.AI_INTENTION_CAST, () -> player.useEquippableItem(item, true));
				player.getAI().setNextAction(nextAction);
			}
			else if (player.isAttackingNow())
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new WeaponEquipTask(item, player), TimeUnit.MILLISECONDS.convert(player.getAttackEndTime() - System.nanoTime(), TimeUnit.NANOSECONDS));
			}
			else
			{
				player.useEquippableItem(item, true);
			}
		}
		else
		{
			L2Weapon weaponItem = player.getActiveWeaponItem();
			int itemid = item.getItemId();
			if (itemid == 4393)
			{
				player.sendPacket(new ShowCalculator(4393));
			}
			else if (weaponItem != null && weaponItem.getItemType() == L2WeaponType.ROD && (itemid >= 6519 && itemid <= 6527 || itemid >= 7610 && itemid <= 7613 || itemid >= 7807 && itemid <= 7809 || itemid >= 8484 && itemid <= 8486 || itemid >= 8505 && itemid <= 8513))
			{
				player.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
				player.broadcastUserInfo();
				ItemList il = new ItemList(player, false);
				player.sendPacket(il);
				return;
			}
			else
			{
				IItemHandler handler = ItemHandler.getInstance().getItemHandler(itemId);
				if (handler != null)
				{
					handler.useItem(player, item);
				}
			}
		}
	}
	
	private static class WeaponEquipTask implements Runnable
	{
		private final L2ItemInstance item;
		private final L2PcInstance player;
		
		protected WeaponEquipTask(L2ItemInstance it, L2PcInstance character)
		{
			item = it;
			player = character;
		}
		
		@Override
		public void run()
		{
			// Equip or unEquip
			player.useEquippableItem(item, false);
		}
	}
}

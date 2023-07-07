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
package l2jorion.game.network.clientpackets;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import l2jorion.Config;
import l2jorion.game.ai.CtrlEvent;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2SummonAI;
import l2jorion.game.ai.NextAction;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2ManufactureList;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2SiegeSummonInstance;
import l2jorion.game.model.actor.instance.L2StaticObjectInstance;
import l2jorion.game.model.actor.instance.L2SummonInstance;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ChairSit;
import l2jorion.game.network.serverpackets.PrivateStoreManageListBuy;
import l2jorion.game.network.serverpackets.PrivateStoreManageListSell;
import l2jorion.game.network.serverpackets.RecipeShopManageList;
import l2jorion.game.network.serverpackets.Ride;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestActionUse extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(RequestActionUse.class);
	
	private int _actionId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	// List of Pet Actions
	private static List<Integer> _petActions = Arrays.asList(new Integer[]
	{
		15,
		16,
		17,
		21,
		22,
		23,
		52,
		53,
		54
	});
	
	@Override
	protected void readImpl()
	{
		_actionId = readD();
		_ctrlPressed = readD() == 1;
		_shiftPressed = readC() == 1;
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		// dont do anything if player is dead
		if (_actionId != 0 && activeChar.isAlikeDead())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (_actionId == 1099)
		{
			// activeChar.sendMessage("If you see this message - report it to an admin.");
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// don't do anything if player is confused
		if (activeChar.isOutOfControl())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// don't do anything if player is casting and the action is not a Pet one (skills too)
		if ((_petActions.contains(_actionId) || _actionId >= 1000))
		{
			if (Config.DEBUG)
			{
				LOG.debug(activeChar.getName() + " request Pet Action use: id " + _actionId + " ctrl:" + _ctrlPressed + " shift:" + _shiftPressed);
			}
		}
		else if (activeChar.isCastingNow())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final L2Summon pet = activeChar.getPet();
		final L2Object target = activeChar.getTarget();
		
		switch (_actionId)
		{
			case 0:
				if (activeChar.isSitting() || !activeChar.isMoving() || activeChar.isFakeDeath())
				{
					useSit(activeChar, target);
				}
				else
				{
					final NextAction nextAction = new NextAction(CtrlEvent.EVT_ARRIVED, CtrlIntention.AI_INTENTION_MOVE_TO, () -> useSit(activeChar, target));
					activeChar.getAI().setNextAction(nextAction);
				}
				break;
			case 1:
				if (activeChar.isRunning())
				{
					activeChar.setWalking();
				}
				else
				{
					activeChar.setRunning();
				}
				break;
			case 15:
			case 21: // pet follow/stop
				if (pet != null && pet.isOutOfControl())
				{
					activeChar.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
					return;
				}
				
				if (pet != null)
				{
					((L2SummonAI) pet.getAI()).notifyFollowStatusChange();
				}
				break;
			case 16:
			case 22: // pet attack
				if (target != null && pet != null && pet != target && !activeChar.isBetrayed())
				{
					if (pet.isAttackingDisabled())
					{
						if (pet.getAttackEndTime() > GameTimeController.getInstance().getGameTicks())
						{
							pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
						}
						else
						{
							return;
						}
					}
					
					if (activeChar.isInOlympiadMode() && !activeChar.isOlympiadStart())
					{
						// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
						activeChar.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					
					if (target instanceof L2PcInstance && !activeChar.getAccessLevel().allowPeaceAttack() && L2Character.isInsidePeaceZone(pet, target))
					{
						if (!activeChar.isInFunEvent() || !((L2PcInstance) target).isInFunEvent())
						{
							activeChar.sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
							return;
						}
					}
					
					if (target.isAutoAttackable(activeChar) || _ctrlPressed)
					{
						if (target instanceof L2DoorInstance)
						{
							if (((L2DoorInstance) target).isAttackable(activeChar) && pet.getNpcId() != L2SiegeSummonInstance.SWOOP_CANNON_ID)
							{
								pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
							}
						}
						// siege golem AI doesn't support attacking other than doors at the moment
						else if (pet.getNpcId() != L2SiegeSummonInstance.SIEGE_GOLEM_ID)
						{
							pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
						}
					}
				}
				break;
			case 17:
			case 23: // pet - cancel action
				if (pet != null && !pet.isMovementDisabled() && !activeChar.isBetrayed())
				{
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
					
					((L2SummonAI) pet.getAI()).notifyFollowStatusChange(); // after stop must go back to owner
				}
				
				break;
			case 19: // pet unsummon
				if (pet != null && !activeChar.isBetrayed())
				{
					// returns pet to control item
					if (pet.isDead())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED));
					}
					else if (pet.isAttackingNow() || pet.isRooted())
					{
						activeChar.sendMessage("You cannot despawn a summon during combat."); // Message like L2OFF
					}
					else if (pet.isInCombat() || activeChar.isInCombat())
					{
						activeChar.sendMessage("You cannot despawn a summon during combat."); // Message like L2OFF
					}
					else
					{
						// if it is a pet and not a summon
						if (pet instanceof L2PetInstance)
						{
							final L2PetInstance petInst = (L2PetInstance) pet;
							
							// if the pet is more than 40% fed
							if (petInst.getCurrentFed() > petInst.getMaxFed() * 0.40)
							{
								pet.unSummon(activeChar);
							}
							else
							{
								activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_RESTORE_HUNGRY_PETS));
							}
						}
					}
				}
				break;
			case 38: // pet mount
				// mount
				if (pet != null && pet.isMountable() && !activeChar.isMounted() && !activeChar.isBetrayed())
				{
					if (activeChar.isDead())
					{
						// A strider cannot be ridden when dead
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD);
						activeChar.sendPacket(msg);
					}
					else if (pet.isDead())
					{
						// A dead strider cannot be ridden.
						final SystemMessage msg = new SystemMessage(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN);
						activeChar.sendPacket(msg);
					}
					else if (pet.isInCombat() || pet.isRooted())
					{
						// A strider in battle cannot be ridden
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN);
						activeChar.sendPacket(msg);
					}
					else if (activeChar.isInCombat())
					{
						// A strider cannot be ridden while in battle
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
						activeChar.sendPacket(msg);
					}
					else if (activeChar.isInFunEvent())
					{
						// A strider cannot be ridden while in event
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
						activeChar.sendPacket(msg);
					}
					else if (activeChar.isSitting()) // Like L2OFF you can mount also during movement
					{
						// A strider can be ridden only when standing
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING);
						activeChar.sendPacket(msg);
					}
					else if (activeChar.isFishing())
					{
						// You can't mount, dismount, break and drop items while fishing
						final SystemMessage msg = new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_2);
						activeChar.sendPacket(msg);
					}
					else if (activeChar.isCursedWeaponEquiped())
					{
						// You can't mount, dismount, break and drop items while weilding a cursed weapon
						final SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
						activeChar.sendPacket(msg);
					}
					else if (!pet.isDead() && !activeChar.isMounted())
					{
						if (!activeChar.disarmWeapons())
						{
							return;
						}
						
						if (!activeChar.getFloodProtectors().getItemPetSummon().tryPerformAction("mount"))
						{
							return;
						}
						
						final Ride mount = new Ride(activeChar.getObjectId(), Ride.ACTION_MOUNT, pet.getTemplate().npcId);
						activeChar.broadcastPacket(mount);
						activeChar.setMountType(mount.getMountType());
						activeChar.setMountObjectID(pet.getControlItemId());
						pet.unSummon(activeChar);
						
						if (activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND) != null || activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND) != null)
						{
							if (activeChar.isFlying())
							{
								// Remove skill Wyvern Breath
								activeChar.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
								activeChar.sendSkillList();
							}
							
							if (activeChar.setMountType(0))
							{
								final Ride dismount = new Ride(activeChar.getObjectId(), Ride.ACTION_DISMOUNT, 0);
								activeChar.broadcastPacket(dismount);
								activeChar.setMountObjectID(0);
							}
						}
					}
				}
				else if (activeChar.isRentedPet())
				{
					activeChar.stopRentPet();
				}
				else if (activeChar.isMounted())
				{
					if (activeChar.isFlying())
					{
						// Remove skill Wyvern Breath
						activeChar.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
						activeChar.sendSkillList();
					}
					
					if (activeChar.setMountType(0))
					{
						final Ride dismount = new Ride(activeChar.getObjectId(), Ride.ACTION_DISMOUNT, 0);
						activeChar.broadcastPacket(dismount);
						activeChar.setMountObjectID(0);
						
						// Update status after unmount to avoid visual bug
						activeChar.broadcastStatusUpdate();
						activeChar.broadcastUserInfo();
					}
				}
				break;
			case 32: // Wild Hog Cannon - Mode Change
				useSkill(4230);
				break;
			case 36: // Soulless - Toxic Smoke
				useSkill(4259);
				break;
			case 37:
				
				if (activeChar.isAlikeDead())
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// Like L2OFF - You can't open Manufacture when you are in private store
				if (activeChar.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY || activeChar.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL)
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// Like L2OFF - You can't open Manufacture when you are sitting
				if (activeChar.isSitting() && activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_MANUFACTURE)
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// You can't open Manufacture when the task is launched
				if (activeChar.isSittingTaskLaunched())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (activeChar.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_MANUFACTURE)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					
					if (activeChar.isSitting())
					{
						activeChar.standUp();
					}
				}
				
				if (activeChar.getCreateList() == null)
				{
					activeChar.setCreateList(new L2ManufactureList());
				}
				
				activeChar.sendPacket(new RecipeShopManageList(activeChar, true));
				break;
			case 39: // Soulless - Parasite Burst
				useSkill(4138);
				break;
			case 41: // Wild Hog Cannon - Attack
				useSkill(4230);
				break;
			case 42: // Kai the Cat - Self Damage Shield
				useSkill(4378, activeChar);
				break;
			case 43: // Unicorn Merrow - Hydro Screw
				useSkill(4137);
				break;
			case 44: // Big Boom - Boom Attack
				useSkill(4139);
				break;
			case 45: // Unicorn Boxer - Master Recharge
				useSkill(4025, activeChar);
				break;
			case 46: // Mew the Cat - Mega Storm Strike
				useSkill(4261);
				break;
			case 47: // Silhouette - Steal Blood
				useSkill(4260);
				break;
			case 48: // Mechanic Golem - Mech. Cannon
				useSkill(4068);
				break;
			case 51:
				
				// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
				if (activeChar.isAlikeDead())
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// Like L2OFF - You can't open Manufacture when you are in private store
				if (activeChar.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY || activeChar.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL)
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// Like L2OFF - You can't open Manufacture when you are sitting
				if (activeChar.isSitting() && activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_MANUFACTURE)
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// You can't open Manufacture when the task is launched
				if (activeChar.isSittingTaskLaunched())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (activeChar.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_MANUFACTURE)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					
					if (activeChar.isSitting())
					{
						activeChar.standUp();
					}
				}
				
				if (activeChar.getCreateList() == null)
				{
					activeChar.setCreateList(new L2ManufactureList());
				}
				
				activeChar.sendPacket(new RecipeShopManageList(activeChar, false));
				break;
			case 52: // unsummon
				if (pet != null && pet instanceof L2SummonInstance)
				{
					if (pet.isInCombat() || activeChar.isInCombat())
					{
						activeChar.sendMessage("You cannot despawn a summon during combat."); // Message like L2OFF
					}
					else if (pet.isAttackingNow() || pet.isRooted())
					{
						activeChar.sendMessage("You cannot despawn a summon during combat."); // Message like L2OFF
					}
					else
					{
						pet.unSummon(activeChar);
					}
				}
				break;
			case 53: // move to target
				if (target != null && pet != null && pet != target && !pet.isMovementDisabled())
				{
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(target.getX(), target.getY(), target.getZ(), 0));
				}
				break;
			case 54: // move to target hatch/strider
				if (target != null && pet != null && pet != target && !pet.isMovementDisabled())
				{
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(target.getX(), target.getY(), target.getZ(), 0));
				}
				break;
			case 96: // Quit Party Command Channel
				LOG.info("98 Accessed");
				break;
			case 97: // Request Party Command Channel Info
				// if (!PartyCommandManager.getInstance().isPlayerInChannel(activeChar))
				// return;
				LOG.info("97 Accessed");
				// PartyCommandManager.getInstance().getActiveChannelInfo(activeChar);
				break;
			case 1000: // Siege Golem - Siege Hammer
				if (target instanceof L2DoorInstance)
				{
					useSkill(4079);
				}
				break;
			case 1001:
				break;
			case 1003: // Wind Hatchling/Strider - Wild Stun
				useSkill(4710);
				break;
			case 1004: // Wind Hatchling/Strider - Wild Defense
				useSkill(4711, activeChar);
				break;
			case 1005: // Star Hatchling/Strider - Bright Burst
				useSkill(4712);
				break;
			case 1006: // Star Hatchling/Strider - Bright Heal
				useSkill(4713, activeChar);
				break;
			case 1007: // Cat Queen - Blessing of Queen
				useSkill(4699, activeChar);
				break;
			case 1008: // Cat Queen - Gift of Queen
				useSkill(4700, activeChar);
				break;
			case 1009: // Cat Queen - Cure of Queen
				useSkill(4701);
				break;
			case 1010: // Unicorn Seraphim - Blessing of Seraphim
				useSkill(4702, activeChar);
				break;
			case 1011: // Unicorn Seraphim - Gift of Seraphim
				useSkill(4703, activeChar);
				break;
			case 1012: // Unicorn Seraphim - Cure of Seraphim
				useSkill(4704);
				break;
			case 1013: // Nightshade - Curse of Shade
				useSkill(4705);
				break;
			case 1014: // Nightshade - Mass Curse of Shade
				useSkill(4706, activeChar);
				break;
			case 1015: // Nightshade - Shade Sacrifice
				useSkill(4707);
				break;
			case 1016: // Cursed Man - Cursed Blow
				useSkill(4709);
				break;
			case 1017: // Cursed Man - Cursed Strike/Stun
				useSkill(4708);
				break;
			case 1031: // Feline King - Slash
				useSkill(5135);
				break;
			case 1032: // Feline King - Spinning Slash
				useSkill(5136);
				break;
			case 1033: // Feline King - Grip of the Cat
				useSkill(5137);
				break;
			case 1034: // Magnus the Unicorn - Whiplash
				useSkill(5138);
				break;
			case 1035: // Magnus the Unicorn - Tridal Wave
				useSkill(5139);
				break;
			case 1036: // Spectral Lord - Corpse Kaboom
				useSkill(5142);
				break;
			case 1037: // Spectral Lord - Dicing Death
				useSkill(5141);
				break;
			case 1038: // Spectral Lord - Force Curse
				useSkill(5140);
				break;
			case 1039: // Swoop Cannon - Cannon Fodder
				if (!(target instanceof L2DoorInstance))
				{
					useSkill(5110);
				}
				break;
			case 1040: // Swoop Cannon - Big Bang
				if (!(target instanceof L2DoorInstance))
				{
					useSkill(5111);
				}
				break;
			case 2000:
				if (activeChar.isSittingTaskLaunched())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (!Config.LIST_ALLOWED_CLASSES.contains(activeChar.getClassId().getId()))
				{
					activeChar.sendMessage("This class can not sell buffs.");
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					
					if (activeChar.isSitting())
					{
						activeChar.standUp();
					}
					
				}
				activeChar.sendPacket(new PrivateStoreManageListSell(activeChar, true, 57));
				break;
			case 2001:
				if (activeChar.isSittingTaskLaunched())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (!Config.LIST_ALLOWED_CLASSES.contains(activeChar.getClassId().getId()))
				{
					activeChar.sendMessage("This class can not sell buffs.");
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					
					if (activeChar.isSitting())
					{
						activeChar.standUp();
					}
					
				}
				activeChar.sendPacket(new PrivateStoreManageListSell(activeChar, true, 5575));
				break;
			case 2002:
				if (activeChar.isSittingTaskLaunched())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (!Config.LIST_ALLOWED_CLASSES.contains(activeChar.getClassId().getId()))
				{
					activeChar.sendMessage("This class can not sell buffs.");
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					
					if (activeChar.isSitting())
					{
						activeChar.standUp();
					}
					
				}
				activeChar.sendPacket(new PrivateStoreManageListSell(activeChar, true, 4037));
				break;
			case 2003:
				if (activeChar.isSittingTaskLaunched())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					
					if (activeChar.isSitting())
					{
						activeChar.standUp();
					}
					
				}
				activeChar.sendPacket(new PrivateStoreManageListSell(activeChar, 5575));
				break;
			case 2004:
				if (activeChar.isSittingTaskLaunched())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					
					if (activeChar.isSitting())
					{
						activeChar.standUp();
					}
					
				}
				activeChar.sendPacket(new PrivateStoreManageListSell(activeChar, 4037));
				break;
			case 2005:
				if (activeChar.isSittingTaskLaunched())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					
					if (activeChar.isSitting())
					{
						activeChar.standUp();
					}
				}
				activeChar.sendPacket(new PrivateStoreManageListBuy(activeChar, 5575));
				break;
			case 2006:
				if (activeChar.isSittingTaskLaunched())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					
					if (activeChar.isSitting())
					{
						activeChar.standUp();
					}
				}
				activeChar.sendPacket(new PrivateStoreManageListBuy(activeChar, 4037));
				break;
			default:
				LOG.warn(activeChar.getName() + ": unhandled action type " + _actionId);
				break;
		}
	}
	
	private void useSkill(final int skillId, final L2Object target)
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final L2Summon activeSummon = activeChar.getPet();
		
		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendMessage("Cannot use skills while trading.");
			return;
		}
		
		if (activeSummon != null && !activeChar.isBetrayed())
		{
			final Map<Integer, L2Skill> _skills = activeSummon.getTemplate().getSkills();
			
			if (_skills.size() == 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE));
				return;
			}
			
			final L2Skill skill = _skills.get(skillId);
			
			if (skill == null)
			{
				return;
			}
			
			activeSummon.setTarget(target);
			
			boolean force = _ctrlPressed;
			
			if (target instanceof L2Character)
			{
				if (activeSummon.isInsideZone(ZoneId.ZONE_PVP) && ((L2Character) target).isInsideZone(ZoneId.ZONE_PVP))
				{
					force = true;
				}
			}
			
			activeSummon.useMagic(skill, force, _shiftPressed);
		}
	}
	
	private void useSkill(final int skillId)
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		useSkill(skillId, activeChar.getTarget());
	}
	
	protected boolean useSit(L2PcInstance activeChar, L2Object target)
	{
		if (activeChar.getMountType() != 0)
		{
			return false;
		}
		
		if (!activeChar.isSitting() && target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 1 && CastleManager.getInstance().getCastle(target) != null && activeChar.isInsideRadius(target, L2StaticObjectInstance.INTERACTION_DISTANCE, false, false))
		{
			final ChairSit cs = new ChairSit(activeChar, ((L2StaticObjectInstance) target).getStaticObjectId());
			activeChar.sendPacket(cs);
			activeChar.sitDown();
			activeChar.broadcastPacket(cs);
			return true;
		}
		
		if (activeChar.isSitting() || activeChar.isFakeDeath())
		{
			activeChar.standUp();
		}
		else
		{
			activeChar.sitDown();
		}
		
		return true;
	}
	
	@Override
	public String getType()
	{
		return "[C] 45 RequestActionUse";
	}
}

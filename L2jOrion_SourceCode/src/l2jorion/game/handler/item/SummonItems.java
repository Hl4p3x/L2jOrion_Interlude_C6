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
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.PetItemsData;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.enums.AchType;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2SummonItem;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance.SkillDat;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MagicSkillLaunched;
import l2jorion.game.network.serverpackets.PetInfo;
import l2jorion.game.network.serverpackets.Ride;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;

public class SummonItems implements IItemHandler
{
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance activeChar = (L2PcInstance) playable;
		
		if (!activeChar.getFloodProtectors().getItemPetSummon().tryPerformAction("summon pet"))
		{
			playable.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// if(activeChar._inEventTvT && TvT._started && !Config.TVT_ALLOW_SUMMON)
		if (activeChar._inEventTvT && TvT.is_started() && !Config.TVT_ALLOW_SUMMON || activeChar.isInsideZone(ZoneId.ZONE_NOLANDING))
		{
			final ActionFailed af = ActionFailed.STATIC_PACKET;
			activeChar.sendPacket(af);
			return;
		}
		
		// if(activeChar._inEventDM && DM._started && !Config.DM_ALLOW_SUMMON)
		if (activeChar._inEventDM && DM.is_started() && !Config.DM_ALLOW_SUMMON)
		{
			final ActionFailed af = ActionFailed.STATIC_PACKET;
			activeChar.sendPacket(af);
			return;
		}
		
		if (activeChar._inEventCTF && CTF.is_started() && !Config.CTF_ALLOW_SUMMON)
		{
			final ActionFailed af = ActionFailed.STATIC_PACKET;
			activeChar.sendPacket(af);
			return;
		}
		
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			return;
		}
		
		if (activeChar.isParalyzed())
		{
			activeChar.sendMessage("You cannot use this while you are paralyzed.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.inObserverMode())
		{
			return;
		}
		
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}
		
		L2SummonItem sitem = PetItemsData.getInstance().getSummonItem(item.getItemId());
		
		if ((!activeChar.isGM() && activeChar.getPet() != null || activeChar.isMounted()) && sitem.isPetSummon())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_HAVE_A_PET));
			return;
		}
		
		// Like L2OFF you can't summon pet in combat
		if (activeChar.isAttackingNow() || activeChar.isInCombat())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_IN_COMBAT));
			return;
		}
		
		if (activeChar.isCursedWeaponEquiped() && sitem.isPetSummon())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE));
			return;
		}
		
		final int npcID = sitem.getNpcId();
		
		if (npcID == 0)
		{
			return;
		}
		
		L2NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(npcID);
		
		if (npcTemplate == null)
		{
			return;
		}
		
		switch (sitem.getType())
		{
			case 0: // static summons (like christmas tree)
				try
				{
					L2Spawn spawn = new L2Spawn(npcTemplate);
					spawn.setId(IdFactory.getInstance().getNextId());
					spawn.setLocx(activeChar.getX());
					spawn.setLocy(activeChar.getY());
					spawn.setLocz(activeChar.getZ());
					L2World.getInstance().storeObject(spawn.spawnOne());
					activeChar.destroyItem("Summon", item.getObjectId(), 1, null, false);
					activeChar.sendMessage("Created " + npcTemplate.name + " at x: " + spawn.getLocx() + " y: " + spawn.getLocy() + " z: " + spawn.getLocz());
					
					if (npcTemplate.getNpcId() == ChristmasTree.SPECIAL_TREE_ID)
					{
						activeChar.getAchievement().increase(AchType.SPAWN_CHRISTMAS_TREE);
					}
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					activeChar.sendMessage("Target is not ingame.");
				}
				
				break;
			case 1: // pet summons
				activeChar.setTarget(activeChar);
				// Skill 2046 used only for animation
				final L2Skill skill = SkillTable.getInstance().getInfo(2046, 1);
				activeChar.useMagic(skill, true, true);
				activeChar.sendPacket(new SystemMessage(SystemMessageId.SUMMON_A_PET));
				ThreadPoolManager.getInstance().scheduleGeneral(new PetSummonFinalizer(activeChar, npcTemplate, item), 4800);
				break;
			case 2: // wyvern
				if (!activeChar.disarmWeapons())
				{
					return;
				}
				
				final Ride mount = new Ride(activeChar.getObjectId(), Ride.ACTION_MOUNT, sitem.getNpcId());
				activeChar.sendPacket(mount);
				activeChar.broadcastPacket(mount);
				activeChar.setMountType(mount.getMountType());
				activeChar.setMountObjectID(item.getObjectId());
		}
	}
	
	static class PetSummonFeedWait implements Runnable
	{
		private final L2PcInstance _activeChar;
		private final L2PetInstance _petSummon;
		
		PetSummonFeedWait(final L2PcInstance activeChar, final L2PetInstance petSummon)
		{
			_activeChar = activeChar;
			_petSummon = petSummon;
		}
		
		@Override
		public void run()
		{
			try
			{
				if (_petSummon.getCurrentFed() <= 0)
				{
					_petSummon.unSummon(_activeChar);
				}
				else
				{
					_petSummon.startFeed(false);
				}
			}
			catch (final Throwable e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	static class PetSummonFinalizer implements Runnable
	{
		private final L2PcInstance _activeChar;
		private final L2ItemInstance _item;
		private final L2NpcTemplate _npcTemplate;
		
		PetSummonFinalizer(final L2PcInstance activeChar, final L2NpcTemplate npcTemplate, final L2ItemInstance item)
		{
			_activeChar = activeChar;
			_npcTemplate = npcTemplate;
			_item = item;
		}
		
		@Override
		public void run()
		{
			try
			{
				final SkillDat skilldat = _activeChar.getCurrentSkill();
				
				if (!_activeChar.isCastingNow() || (skilldat != null && skilldat.getSkillId() != 2046))
				{
					return;
				}
				
				_activeChar.sendPacket(new MagicSkillLaunched(_activeChar, 2046, 1));
				
				// check for summon item validity
				if (_item == null || _item.getOwnerId() != _activeChar.getObjectId() || _item.getLocation() != L2ItemInstance.ItemLocation.INVENTORY)
				{
					return;
				}
				
				final L2PetInstance petSummon = L2PetInstance.spawnPet(_npcTemplate, _activeChar, _item);
				
				if (petSummon == null)
				{
					return;
				}
				
				petSummon.setTitle(_activeChar.getName());
				
				if (!petSummon.isRespawned())
				{
					petSummon.setCurrentHp(petSummon.getMaxHp());
					petSummon.setCurrentMp(petSummon.getMaxMp());
					petSummon.getStat().setExp(petSummon.getExpForThisLevel());
					petSummon.setCurrentFed(petSummon.getMaxFed());
				}
				
				petSummon.setRunning();
				
				if (!petSummon.isRespawned())
				{
					petSummon.store();
				}
				
				_activeChar.setPet(petSummon);
				
				L2World.getInstance().storeObject(petSummon);
				petSummon.spawnMe(_activeChar.getX() + 50, _activeChar.getY() + 100, _activeChar.getZ());
				_activeChar.sendPacket(new PetInfo(petSummon));
				petSummon.startFeed(false);
				_item.setEnchantLevel(petSummon.getLevel());
				
				if (petSummon.getCurrentFed() <= 0)
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new PetSummonFeedWait(_activeChar, petSummon), 60000);
				}
				else
				{
					petSummon.startFeed(false);
				}
				
				petSummon.setFollowStatus(true);
				petSummon.setShowSummonAnimation(false);
				petSummon.broadcastStatusUpdate();
			}
			catch (final Throwable e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return PetItemsData.getInstance().itemIDs();
	}
}
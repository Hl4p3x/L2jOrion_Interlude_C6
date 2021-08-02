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
package l2jorion.game.handler.skill;

import l2jorion.Config;
import l2jorion.game.geo.GeoData;
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.model.zone.type.L2FishingZone;
import l2jorion.game.model.zone.type.L2WaterZone;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.util.Util;
import l2jorion.util.random.Rnd;

public class Fishing implements ISkillHandler
{
	private static final int MIN_BAIT_DISTANCE = 150;
	private static final int MAX_BAIT_DISTANCE = 250;
	
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.FISHING
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
			return;
		
		final L2PcInstance player = activeChar.getActingPlayer();
		
		if (!Config.ALLOWFISHING && !player.isGM())
		{
			player.sendMessage("Fishing is disabled!");
			return;
		}
		
		if (player.isFishing())
		{
			if (player.GetFishCombat() != null)
			{
				player.GetFishCombat().doDie(false);
			}
			else
			{
				player.EndFishing(false);
			}
			
			player.sendPacket(SystemMessageId.FISHING_ATTEMPT_CANCELLED);
			return;
		}
		
		// check for equiped fishing rod
		L2Weapon equipedWeapon = player.getActiveWeaponItem();
		if (((equipedWeapon == null) || (equipedWeapon.getItemType() != L2WeaponType.ROD)))
		{
			player.sendPacket(SystemMessageId.FISHING_POLE_NOT_EQUIPPED);
			return;
		}
		
		L2ItemInstance lure = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (lure == null)
		{
			player.sendPacket(SystemMessageId.BAIT_ON_HOOK_BEFORE_FISHING);
			return;
		}
		
		player.SetLure(lure);
		L2ItemInstance lure2 = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		
		if (lure2 == null || lure2.getCount() < 1) // Not enough bait.
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_BAIT);
			return;
		}
		
		if (player.isInBoat())
		{
			player.sendPacket(SystemMessageId.CANNOT_FISH_ON_BOAT);
			return;
		}
		
		if (player.isInCraftMode() || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.CANNOT_FISH_WHILE_USING_RECIPE_BOOK);
			return;
		}
		
		if (player.isInsideZone(ZoneId.ZONE_WATER))
		{
			player.sendPacket(SystemMessageId.CANNOT_FISH_UNDER_WATER);
			return;
		}
		
		// calculate a position in front of the player with a random distance
		int distance = Rnd.get(MIN_BAIT_DISTANCE, MAX_BAIT_DISTANCE);
		final double angle = Util.convertHeadingToDegree(player.getClientHeading());
		final double radian = Math.toRadians(angle);
		final double sin = Math.sin(radian);
		final double cos = Math.cos(radian);
		int baitX = (int) (player.getX() + (cos * distance));
		int baitY = (int) (player.getY() + (sin * distance));
		
		// search for fishing and water zone
		L2FishingZone fishingZone = null;
		L2WaterZone waterZone = null;
		for (final L2ZoneType zone : ZoneManager.getInstance().getZones(baitX, baitY))
		{
			if (zone instanceof L2FishingZone)
			{
				fishingZone = (L2FishingZone) zone;
			}
			else if (zone instanceof L2WaterZone)
			{
				waterZone = (L2WaterZone) zone;
			}
			
			if ((fishingZone != null) && (waterZone != null))
			{
				break;
			}
		}
		
		int baitZ = computeBaitZ(player, baitX, baitY, fishingZone, waterZone);
		if (baitZ == Integer.MIN_VALUE)
		{
			for (distance = MAX_BAIT_DISTANCE; distance >= MIN_BAIT_DISTANCE; --distance)
			{
				baitX = (int) (player.getX() + (cos * distance));
				baitY = (int) (player.getY() + (sin * distance));
				
				// search for fishing and water zone again
				fishingZone = null;
				waterZone = null;
				for (final L2ZoneType zone : ZoneManager.getInstance().getZones(baitX, baitY))
				{
					if (zone instanceof L2FishingZone)
					{
						fishingZone = (L2FishingZone) zone;
					}
					else if (zone instanceof L2WaterZone)
					{
						waterZone = (L2WaterZone) zone;
					}
					
					if ((fishingZone != null) && (waterZone != null))
					{
						break;
					}
				}
				
				baitZ = computeBaitZ(player, baitX, baitY, fishingZone, waterZone);
				if (baitZ != Integer.MIN_VALUE)
				{
					break;
				}
			}
			
			if (baitZ == Integer.MIN_VALUE)
			{
				player.sendPacket(SystemMessageId.CANNOT_FISH_HERE);
				return;
			}
		}
		
		lure2 = player.getInventory().destroyItem("Consume", player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, player, null);
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(lure2);
		player.sendPacket(iu);
		player.startFishing(baitX, baitY, baitZ);
	}
	
	/**
	 * Computes the Z of the bait.
	 * @param player the player
	 * @param baitX the bait x
	 * @param baitY the bait y
	 * @param fishingZone the fishing zone
	 * @param waterZone the water zone
	 * @return the bait z or {@link Integer#MIN_VALUE} when you cannot fish here
	 */
	private static int computeBaitZ(final L2PcInstance player, final int baitX, final int baitY, final L2FishingZone fishingZone, final L2WaterZone waterZone)
	{
		if ((fishingZone == null))
		{
			return Integer.MIN_VALUE;
		}
		
		if ((waterZone == null))
		{
			return Integer.MIN_VALUE;
		}
		
		int baitZ = waterZone.getWaterZ();
		
		if (!GeoData.getInstance().canSeeTarget(player.getX(), player.getY(), player.getZ()-80, baitX, baitY, baitZ))
		{
			return Integer.MIN_VALUE;
		}
		
		if (GeoData.getInstance().hasGeo(baitX, baitY))
		{
			if (GeoData.getInstance().getHeight(baitX, baitY, baitZ) > baitZ)
			{
				return Integer.MIN_VALUE;
			}
			
			if (GeoData.getInstance().getHeight(baitX, baitY, player.getZ()) > baitZ)
			{
				return Integer.MIN_VALUE;
			}
		}
		
		return baitZ;
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
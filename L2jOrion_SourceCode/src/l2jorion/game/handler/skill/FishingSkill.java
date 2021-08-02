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

import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Fishing;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;

public class FishingSkill implements ISkillHandler
{
	// private static Logger LOG = LoggerFactory.getLogger(SiegeFlag.class);
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.PUMPING,
		SkillType.REELING
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance player = (L2PcInstance) activeChar;
		
		L2Fishing fish = player.GetFishCombat();
		if (fish == null)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		L2Weapon weaponItem = player.getActiveWeaponItem();
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		if (weaponInst == null || weaponItem == null || weaponItem.getItemType() != L2WeaponType.ROD)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			activeChar.sendPacket(sm);
			return;
		}
		
		int SS = 1;
		int pen = 0;
		
		if (weaponInst.getChargedFishshot())
		{
			SS = 2;
		}
		
		final double gradebonus = 1 + weaponItem.getCrystalType() * 0.1;
		int dmg = (int) (skill.getPower() * gradebonus * SS);
		weaponItem = null;
		if (player.getSkillLevel(1315) <= skill.getLevel() - 2) // 1315 - Fish Expertise
		{
			player.sendPacket(new SystemMessage(SystemMessageId.REELING_PUMPING_3_LEVELS_HIGHER_THAN_FISHING_PENALTY));
			pen = 50;
			final int penatlydmg = dmg - pen;
			if (player.isGM())
			{
				player.sendMessage("Dmg w/o penalty = " + dmg);
			}
			dmg = penatlydmg;
		}
		
		if (SS > 1)
		{
			weaponInst.setChargedFishshot(false);
		}
		
		if (skill.getSkillType() == SkillType.REELING)// Realing
		{
			fish.useRealing(dmg, pen);
		}
		else
		{
			fish.usePomping(dmg, pen);
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}

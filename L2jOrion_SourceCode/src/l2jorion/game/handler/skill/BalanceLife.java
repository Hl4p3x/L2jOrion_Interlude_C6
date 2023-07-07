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
package l2jorion.game.handler.skill;

import l2jorion.Config;
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.handler.SkillHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;

public class BalanceLife implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.BALANCE_LIFE
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		try
		{
			final ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(SkillType.BUFF);
			
			if (handler != null)
			{
				handler.useSkill(activeChar, skill, targets);
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		
		L2Character target = null;
		
		L2PcInstance player = null;
		if (activeChar instanceof L2PcInstance)
		{
			player = (L2PcInstance) activeChar;
		}
		
		double fullHP = 0;
		double currentHPs = 0;
		
		for (final L2Object target2 : targets)
		{
			target = (L2Character) target2;
			
			// We should not heal if char is dead
			if (target == null || target.isDead())
			{
				continue;
			}
			
			// Avoid characters heal inside Baium lair from outside
			/*
			 * if ((activeChar.isInsideZone(12007) || target.isInsideZone(12007)) && ((GrandBossManager.getInstance().getZone(activeChar) == null && GrandBossManager.getInstance().getZone(target) != null) || (GrandBossManager.getInstance().getZone(target) == null &&
			 * GrandBossManager.getInstance().getZone(activeChar) != null))) { continue; }
			 */
			
			if (Config.PROHIBIT_HEALER_CLASS && player != null && (player.getClassId() == ClassId.cardinal || player.getClassId() == ClassId.evaSaint || player.getClassId() == ClassId.shillienSaint) && target.isInsideZone(ZoneId.ZONE_RANDOM))
			{
				continue;
			}
			
			// Player holding a cursed weapon can't be healed and can't heal
			if (target != activeChar)
			{
				if (target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquiped())
				{
					continue;
				}
				else if (player != null && player.isCursedWeaponEquiped())
				{
					continue;
				}
			}
			
			fullHP += target.getMaxHp();
			currentHPs += target.getCurrentHp();
		}
		
		final double percentHP = currentHPs / fullHP;
		
		for (final L2Object target2 : targets)
		{
			target = (L2Character) target2;
			
			if (target == null || target.isDead())
			{
				continue;
			}
			
			final double newHP = target.getMaxHp() * percentHP;
			final double totalHeal = newHP - target.getCurrentHp();
			
			target.setCurrentHp(newHP);
			
			if (totalHeal > 0)
			{
				target.setLastHealAmount((int) totalHeal);
			}
			
			StatusUpdate su = new StatusUpdate(target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			target.sendPacket(su);
			su = null;
			
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
			sm.addString("HP of the party has been balanced.");
			target.sendPacket(sm);
			sm = null;
			
		}
		target = null;
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}

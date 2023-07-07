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
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.handler.SkillHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Stats;

public class Heal implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.HEAL,
		SkillType.HEAL_PERCENT,
		SkillType.HEAL_STATIC
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		L2PcInstance player = null;
		if (activeChar instanceof L2PcInstance)
		{
			player = (L2PcInstance) activeChar;
		}
		
		final boolean bss = activeChar.checkBss();
		final boolean sps = activeChar.checkSps();
		
		ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(SkillType.BUFF);
		try
		{
			if (handler != null)
			{
				handler.useSkill(activeChar, skill, targets);
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		
		L2Character target = null;
		for (L2Object trg : targets)
		{
			target = (L2Character) trg;
			
			if (target == null || target.isDead() || target.isAlikeDead() || target.isInvul())
			{
				continue;
			}
			
			if (target instanceof L2DoorInstance)
			{
				continue;
			}
			
			if (target instanceof L2NpcInstance && ((L2NpcInstance) target).getNpcId() == 35062)
			{
				activeChar.getActingPlayer().sendMessage("You can't heal siege flags.");
				continue;
			}
			
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
			
			// Fixed about Infinity Rod skill on Raid Boss and BigBoss
			if (skill.getId() == 3598 && (target instanceof L2RaidBossInstance || target instanceof L2GrandBossInstance))
			{
				continue;
			}
			
			double hp = skill.getPower();
			
			if (skill.getSkillType() == SkillType.HEAL_PERCENT)
			{
				hp = target.getMaxHp() * hp / 100.0;
			}
			else
			{
				if (bss)
				{
					hp *= 1.5;
				}
				else if (sps)
				{
					hp *= 1.4;
				}
			}
			
			if (skill.getSkillType() == SkillType.HEAL_STATIC)
			{
				hp = skill.getPower();
			}
			else if (skill.getSkillType() != SkillType.HEAL_PERCENT)
			{
				hp *= target.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100;
			}
			
			target.setCurrentHp(hp + target.getCurrentHp());
			
			target.setLastHealAmount((int) hp);
			
			StatusUpdate su = new StatusUpdate(target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			target.sendPacket(su);
			
			if (target instanceof L2PcInstance)
			{
				SystemMessage sm;
				if (skill.getId() == 4051)
				{
					sm = new SystemMessage(SystemMessageId.REJUVENATING_HP);
				}
				else
				{
					if (activeChar instanceof L2PcInstance && activeChar != target)
					{
						sm = new SystemMessage(SystemMessageId.S2_HP_RESTORED_BY_S1);
						sm.addString(activeChar.getName());
						sm.addNumber((int) hp);
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.S1_HP_RESTORED);
						sm.addNumber((int) hp);
						
					}
				}
				target.sendPacket(sm);
			}
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
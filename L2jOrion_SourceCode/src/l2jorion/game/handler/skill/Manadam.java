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
package l2jorion.game.handler.skill;

import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Formulas;

public class Manadam implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.MANADAM
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		L2Character target = null;
		
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		boolean sps = activeChar.checkSps();
		boolean bss = activeChar.checkBss();
		
		for (L2Object target2 : targets)
		{
			target = (L2Character) target2;
			
			if (target.reflectSkill(skill))
			{
				target = activeChar;
			}
			
			boolean chance = Formulas.getInstance().calcMagicAffected(activeChar, target, skill);
			
			if (target.isInvul() || !chance)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
			}
			else
			{
				double damage = Formulas.getInstance().calcManaDam(activeChar, target, skill, sps, bss);
				
				double mp = (damage > target.getCurrentMp() ? target.getCurrentMp() : damage);
				target.reduceCurrentMp(mp);
				
				if (damage > 0)
				{
					if (target.isSleeping())
					{
						target.stopSleeping(null);
					}
				}
				
				StatusUpdate sump = new StatusUpdate(target.getObjectId());
				sump.addAttribute(StatusUpdate.CUR_MP, (int) target.getCurrentMp());
				target.sendPacket(sump);
				sump = null;
				
				SystemMessage sm = new SystemMessage(SystemMessageId.S2_MP_HAS_BEEN_DRAINED_BY_S1);
				
				if (activeChar instanceof L2NpcInstance)
				{
					int mobId = ((L2NpcInstance) activeChar).getNpcId();
					sm.addNpcName(mobId);
				}
				else if (activeChar instanceof L2Summon)
				{
					int mobId = ((L2Summon) activeChar).getNpcId();
					sm.addNpcName(mobId);
				}
				else
				{
					sm.addString(activeChar.getName());
				}
				sm.addNumber((int) mp);
				target.sendPacket(sm);
				
				target = null;
				sm = null;
				
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm2 = new SystemMessage(SystemMessageId.YOUR_OPPONENTS_MP_WAS_REDUCED_BY_S1);
					sm2.addNumber((int) mp);
					activeChar.sendPacket(sm2);
				}
			}
		}
		
		if (bss)
		{
			activeChar.removeBss();
		}
		else if (sps)
		{
			activeChar.removeSps();
		}
		
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}

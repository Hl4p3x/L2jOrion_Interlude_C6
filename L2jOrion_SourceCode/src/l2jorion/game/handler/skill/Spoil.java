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
import l2jorion.game.ai.CtrlEvent;
import l2jorion.game.enums.AchType;
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Formulas;

public class Spoil implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.SPOIL
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		if (!(activeChar instanceof L2PcInstance))
		{
			return;
		}
		
		if (targets == null)
		{
			return;
		}
		
		for (final L2Object target1 : targets)
		{
			if (!(target1 instanceof L2MonsterInstance))
			{
				continue;
			}
			
			L2Attackable target = (L2Attackable) target1;
			
			if (target.isDead())
			{
				continue;
			}
			
			if (!Config.RON_CUSTOM)
			{
				if (target.getSpoilerId() != 0)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.ALREADY_SPOILED));
					continue;
				}
			}
			
			if (!target.isDead())
			{
				if (Formulas.calcMagicSuccess(activeChar, (L2Character) target1, skill))
				{
					target.setSpoilerId(activeChar.getObjectId());
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SPOIL_SUCCESS));
					((L2PcInstance) activeChar).getAchievement().increase(AchType.SPOIL);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
					sm.addString(target.getName());
					sm.addSkillName(skill.getDisplayId());
					activeChar.sendPacket(sm);
				}
				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
			}
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}

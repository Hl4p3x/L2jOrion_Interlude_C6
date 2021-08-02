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
package l2jorion.game.skills.effects;

import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Skill.SkillTargetType;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Env;

class EffectDamOverTime extends L2Effect
{
	public EffectDamOverTime(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DMG_OVER_TIME;
	}
	
	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead())
		{
			return false;
		}
		
		double damage = calc();
		
		// Like L2OFF you can't die with DamOverTime
		if (damage >= getEffected().getCurrentHp() - 1)
		{
			if (getSkill().isToggle())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_HP);
				getEffected().sendPacket(sm);
				getEffected().removeEffect(this);
				this.exit(false);
				return false;
			}
			
			if (getSkill().getId() != 4082)
			{
				damage = getEffected().getCurrentHp() - 1;
			}
		}
		
		final boolean awake = !(getEffected() instanceof L2Attackable) && !(getSkill().getTargetType() == SkillTargetType.TARGET_SELF && getSkill().isToggle());
		
		getEffected().reduceCurrentHpByDamOverTime(damage, getEffector(), awake, this.getPeriod());
		
		return true;
	}
}
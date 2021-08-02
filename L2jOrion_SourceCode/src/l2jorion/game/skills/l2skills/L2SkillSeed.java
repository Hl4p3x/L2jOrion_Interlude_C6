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
package l2jorion.game.skills.l2skills;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.skills.effects.EffectSeed;
import l2jorion.game.templates.StatsSet;

public class L2SkillSeed extends L2Skill
{
	public L2SkillSeed(final StatsSet set)
	{
		super(set);
	}
	
	@Override
	public void useSkill(final L2Character caster, final L2Object[] targets)
	{
		if (caster.isAlikeDead())
		{
			return;
		}
		
		// Update Seeds Effects
		for (final L2Object target2 : targets)
		{
			final L2Character target = (L2Character) target2;
			if (target.isAlikeDead() && getTargetType() != SkillTargetType.TARGET_CORPSE_MOB)
			{
				continue;
			}
			
			final EffectSeed oldEffect = (EffectSeed) target.getFirstEffect(getId());
			if (oldEffect == null)
			{
				getEffects(caster, target, false, false, false);
			}
			else
			{
				oldEffect.increasePower();
			}
			
			final L2Effect[] effects = target.getAllEffects();
			for (final L2Effect effect : effects)
			{
				if (effect.getEffectType() == L2Effect.EffectType.SEED)
				{
					effect.rescheduleEffect();
				}
			}
		}
	}
}

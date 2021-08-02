/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.skills.effects;

import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.skills.Env;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class EffectNegate extends L2Effect
{
	protected static final Logger LOG = LoggerFactory.getLogger(EffectNegate.class);
	
	public EffectNegate(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.NEGATE;
	}
	
	@Override
	public void onStart()
	{
		
		final L2Skill skill = getSkill();
		
		if (skill.getNegateId() != 0)
		{
			getEffected().stopSkillEffects(skill.getNegateId());
		}
		
		for (final String negateSkillType : skill.getNegateSkillTypes())
		{
			SkillType type = null;
			try
			{
				type = SkillType.valueOf(negateSkillType);
			}
			catch (final Exception e)
			{
				//
			}
			
			if (type != null)
			{
				getEffected().stopSkillEffects(type, skill.getPower());
			}
		}
		
		for (final String negateEffectType : skill.getNegateEffectTypes())
		{
			EffectType type = null;
			try
			{
				type = EffectType.valueOf(negateEffectType);
			}
			catch (final Exception e)
			{
			}
			
			if (type != null)
			{
				getEffected().stopEffects(type);
			}
		}
		
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}

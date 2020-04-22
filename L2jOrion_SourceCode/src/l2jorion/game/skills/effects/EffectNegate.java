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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2jorion.Config;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.skills.Env;

/**
 * @author Gnat
 */
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
		
		if (Config.DEBUG)
			LOG.debug("effectNegate on " + getEffected().getName() + " with skill " + skill.getId());
		
		if (skill.getNegateId() != 0)
			getEffected().stopSkillEffects(skill.getNegateId());
		
		for (final String negateSkillType : skill.getNegateSkillTypes())
		{
			if (Config.DEBUG)
				LOG.debug("effectNegate on Type " + negateSkillType + " with power " + skill.getPower());
			
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
				getEffected().stopSkillEffects(type, skill.getPower());
		}
		
		for (final String negateEffectType : skill.getNegateEffectTypes())
		{
			if (Config.DEBUG)
				LOG.debug("effectNegate on Effect Type " + negateEffectType + " with power " + skill.getPower());
			
			EffectType type = null;
			try
			{
				type = EffectType.valueOf(negateEffectType);
			}
			catch (final Exception e)
			{
				//
			}
			
			if (type != null)
				getEffected().stopEffects(type);
		}
		
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}

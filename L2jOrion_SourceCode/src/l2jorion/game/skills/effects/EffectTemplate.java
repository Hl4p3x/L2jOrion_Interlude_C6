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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import l2jorion.Config;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.conditions.Condition;
import l2jorion.game.skills.funcs.FuncTemplate;
import l2jorion.game.skills.funcs.Lambda;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class EffectTemplate
{
	static Logger LOG = LoggerFactory.getLogger(EffectTemplate.class);
	
	private final Class<?> _func;
	private final Constructor<?> _constructor;
	
	public final Condition attachCond;
	public final Condition applayCond;
	public final Lambda lambda;
	public final int counter;
	public int period; // in seconds
	public final int abnormalEffect;
	public FuncTemplate[] funcTemplates;
	public boolean showIcon;
	
	public final String stackType;
	public final String name;
	public final float stackOrder;
	public final double effectPower;
	public final SkillType effectType;
	
	public EffectTemplate(final Condition pAttachCond, final Condition pApplayCond, final String func, final Lambda pLambda, final int pCounter, final int pPeriod, final int pAbnormalEffect, final String pStackType, final float pStackOrder, final int pShowIcon, final SkillType eType, final double ePower)
	{
		attachCond = pAttachCond;
		applayCond = pApplayCond;
		lambda = pLambda;
		name = func;
		counter = pCounter;
		period = pPeriod;
		abnormalEffect = pAbnormalEffect;
		stackType = pStackType;
		stackOrder = pStackOrder;
		showIcon = pShowIcon == 0;
		effectType = eType;
		effectPower = ePower;
		
		try
		{
			_func = Class.forName("l2jorion.game.skills.effects.Effect" + func);
		}
		catch (final ClassNotFoundException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			throw new RuntimeException(e);
		}
		
		try
		{
			_constructor = _func.getConstructor(Env.class, EffectTemplate.class);
		}
		catch (final NoSuchMethodException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			throw new RuntimeException(e);
		}
	}
	
	public L2Effect getEffect(final Env env)
	{
		if (attachCond != null && !attachCond.test(env))
		{
			return null;
		}
		
		try
		{
			final L2Effect effect = (L2Effect) _constructor.newInstance(env, this);
			
			return effect;
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (InvocationTargetException e)
		{
			LOG.warn("Error: " + _func + " effect: " + name + " skill ID: " + env.skill.getId() + " name: " + env.skill.getName() + " effector: " + env.player + " effected: " + env.target);
			e.printStackTrace();
			return null;
		}
	}
	
	public void attach(final FuncTemplate f)
	{
		if (funcTemplates == null)
		{
			funcTemplates = new FuncTemplate[]
			{
				f
			};
		}
		else
		{
			final int len = funcTemplates.length;
			final FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			funcTemplates = tmp;
		}
	}
	
}
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

import l2jorion.game.model.L2Effect;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.skills.Env;

final class EffectPhoenixBless extends L2Effect
{
	public EffectPhoenixBless(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.PHOENIX_BLESSING;
	}
	
	@Override
	public void onStart()
	{
		if (getEffected() instanceof L2PlayableInstance)
		{
			((L2PlayableInstance) getEffected()).startPhoenixBlessing(this);
		}
	}
	
	@Override
	public void onExit()
	{
		if (getEffected() instanceof L2PlayableInstance)
		{
			((L2PlayableInstance) getEffected()).stopPhoenixBlessing(this);
		}
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}

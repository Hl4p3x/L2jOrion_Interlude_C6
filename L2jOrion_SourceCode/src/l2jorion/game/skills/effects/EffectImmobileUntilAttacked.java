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
import l2jorion.game.skills.Env;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class EffectImmobileUntilAttacked extends L2Effect
{
	static final Logger LOG = LoggerFactory.getLogger(EffectImmobileUntilAttacked.class);
	
	public EffectImmobileUntilAttacked(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.IMMOBILEUNTILATTACKED;
	}
	
	@Override
	public boolean onActionTime()
	{
		getEffected().stopImmobileUntilAttacked(this);
		// just stop this effect
		return false;
	}
	
	@Override
	public void onExit()
	{
		super.onExit();
		getEffected().stopImmobileUntilAttacked(this);
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		getEffected().startImmobileUntilAttacked();
	}
}

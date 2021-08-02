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

import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Effect;
import l2jorion.game.skills.Env;

public final class EffectFusion extends L2Effect
{
	public int _effect;
	public int _maxEffect;
	
	public EffectFusion(final Env env, final EffectTemplate template)
	{
		super(env, template);
		_effect = getSkill().getLevel();
		_maxEffect = 10;
	}
	
	@Override
	public boolean onActionTime()
	{
		return true;
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.FUSION;
	}
	
	public void increaseEffect()
	{
		if (_effect < _maxEffect)
		{
			_effect++;
			updateBuff();
		}
	}
	
	public void decreaseForce()
	{
		_effect--;
		if (_effect < 1)
		{
			exit(false);
		}
		else
		{
			updateBuff();
		}
	}
	
	private void updateBuff()
	{
		exit(false);
		SkillTable.getInstance().getInfo(getSkill().getId(), _effect).getEffects(getEffector(), getEffected(), false, false, false);
	}
}

/*
+ * L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.skills.conditions;

import l2jorion.game.model.L2Effect;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.effects.EffectBattleForce;
import l2jorion.game.skills.effects.EffectSpellForce;

public class ConditionForceBuff extends Condition
{
	private static int BATTLE_FORCE = 5104;
	private static int SPELL_FORCE = 5105;
	
	private final int _battleForces;
	private final int _spellForces;
	
	public ConditionForceBuff(final int[] forces)
	{
		_battleForces = forces[0];
		_spellForces = forces[1];
	}
	
	public ConditionForceBuff(final int battle, final int spell)
	{
		_battleForces = battle;
		_spellForces = spell;
	}
	
	@Override
	public boolean testImpl(final Env env)
	{
		final int neededBattle = _battleForces;
		if (neededBattle > 0)
		{
			final L2Effect battleForce = env.player.getFirstEffect(BATTLE_FORCE);
			if (!(battleForce instanceof EffectBattleForce) || ((EffectBattleForce) battleForce).forces < neededBattle)
			{
				return false;
			}
		}
		final int neededSpell = _spellForces;
		if (neededSpell > 0)
		{
			final L2Effect spellForce = env.player.getFirstEffect(SPELL_FORCE);
			if (!(spellForce instanceof EffectSpellForce) || ((EffectSpellForce) spellForce).forces < neededSpell)
			{
				return false;
			}
		}
		return true;
	}
}

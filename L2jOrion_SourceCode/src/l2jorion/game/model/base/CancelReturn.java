/*
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
package l2jorion.game.model.base;

import java.util.Map;
import java.util.logging.Logger;

import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.effects.EffectTemplate;

/**
 * @author Vilmismeme (:D) .|.
 */
public final class CancelReturn implements Runnable
{
	protected static final Logger LOGGER = Logger.getLogger(CancelReturn.class.getName());
	
	private L2PcInstance _player;
	private Map<L2Skill, int[]> _buffs;
	
	public CancelReturn(L2PcInstance player, Map<L2Skill, int[]> buffs)
	{
		player = _player;
		buffs = _buffs;
	}
	
	@Override
	public void run()
	{
		if (_player == null || _player.isOnline() == 0)
		{
			return;
		}
		
		for (L2Skill s : _buffs.keySet())
		{
			if (s == null)
			{
				continue;
			}
			
			Env env = new Env();
			env.player = _player;
			env.target = _player;
			env.skill = s;
			
			for (EffectTemplate et : s.getEffectTemplates())
			{
				L2Effect e = et.getEffect(env);
				if (e != null)
				{
					try
					{
						e.setCount(_buffs.get(s)[0]);
						e.setFirstTime(_buffs.get(s)[1]);
					}
					catch (NullPointerException er)
					{
						LOGGER.info("Error on buff return: " + e.getSkill().getName() + " (" + e.getSkill().getId() + ") " + er);
					}
				}
			}
		}
		
		_player.getCancelledBuffs().clear();
	}
}
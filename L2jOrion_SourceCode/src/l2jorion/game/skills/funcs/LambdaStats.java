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
package l2jorion.game.skills.funcs;

import l2jorion.game.skills.Env;

public final class LambdaStats extends Lambda
{
	public enum StatsType
	{
		PLAYER_LEVEL,
		TARGET_LEVEL,
		PLAYER_MAX_HP,
		PLAYER_MAX_MP
	}
	
	private final StatsType _stat;
	
	public LambdaStats(final StatsType stat)
	{
		_stat = stat;
	}
	
	@Override
	public double calc(final Env env)
	{
		switch (_stat)
		{
			case PLAYER_LEVEL:
				if (env.player == null)
				{
					return 1;
				}
				return env.player.getLevel();
			case TARGET_LEVEL:
				if (env.target == null)
				{
					return 1;
				}
				return env.target.getLevel();
			case PLAYER_MAX_HP:
				if (env.player == null)
				{
					return 1;
				}
				return env.player.getMaxHp();
			case PLAYER_MAX_MP:
				if (env.player == null)
				{
					return 1;
				}
				return env.player.getMaxMp();
		}
		return 0;
	}
	
}

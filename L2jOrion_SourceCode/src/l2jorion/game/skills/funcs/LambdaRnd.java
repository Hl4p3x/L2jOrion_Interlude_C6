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
import l2jorion.util.random.Rnd;

public final class LambdaRnd extends Lambda
{
	
	private final Lambda _max;
	private final boolean _linear;
	
	public LambdaRnd(final Lambda max, final boolean linear)
	{
		_max = max;
		_linear = linear;
	}
	
	@Override
	public double calc(final Env env)
	{
		if (_linear)
		{
			return _max.calc(env) * Rnd.nextDouble();
		}
		return _max.calc(env) * Rnd.nextGaussian();
	}
	
}

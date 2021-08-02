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
import l2jorion.game.skills.Stats;
import l2jorion.game.skills.conditions.Condition;

public abstract class Func
{
	public final Stats stat;
	
	public final int order;
	
	public final Object funcOwner;
	
	public Condition cond;
	
	public Func(final Stats pStat, final int pOrder, final Object owner)
	{
		stat = pStat;
		order = pOrder;
		funcOwner = owner;
	}
	
	public void setCondition(final Condition pCond)
	{
		cond = pCond;
	}
	
	public abstract void calc(Env env);
}

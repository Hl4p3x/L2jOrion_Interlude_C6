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
package l2jorion.game.model;

public final class L2SkillLearn
{
	// these two build the primary key
	private final int _id;
	private final int _level;
	
	// not needed, just for easier debug
	private final String _name;
	
	private final int _spCost;
	private final int _minLevel;
	private final int _costid;
	private final int _costcount;
	
	public L2SkillLearn(final int id, final int lvl, final int minLvl, final String name, final int cost, final int costid, final int costcount)
	{
		_id = id;
		_level = lvl;
		_minLevel = minLvl;
		_name = name.intern();
		_spCost = cost;
		_costid = costid;
		_costcount = costcount;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getSpCost()
	{
		return _spCost;
	}
	
	public int getIdCost()
	{
		return _costid;
	}
	
	public int getCostCount()
	{
		return _costcount;
	}
}

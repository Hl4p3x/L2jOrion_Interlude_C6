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
package l2jorion.game.model;

import l2jorion.game.templates.StatsSet;

public class AchievementHolder
{
	private final boolean _daily;
	private final int _lvl;
	private final String _name;
	private final String _icon;
	private final String _desc;
	private final int _required;
	private final int _npcId;
	private final int _itemId;
	private final int _reward;
	private final int _count;
	
	public AchievementHolder(StatsSet set)
	{
		_daily = set.getBool("daily", false);
		_lvl = set.getInteger("lvl", 1);
		_name = set.getString("name", "Name");
		_icon = set.getString("icon", "icon.noimage");
		_desc = set.getString("desc", "Description");
		_required = set.getInteger("required", 1);
		_npcId = set.getInteger("npcId", 0);
		_itemId = set.getInteger("itemId", 0);
		_reward = set.getInteger("reward");
		_count = set.getInteger("count");
	}
	
	public final boolean isDaily()
	{
		return _daily;
	}
	
	public final int getLevel()
	{
		return _lvl;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final String getIcon()
	{
		return _icon;
	}
	
	public final String getDescription()
	{
		return _desc;
	}
	
	public final int getRequired()
	{
		return _required;
	}
	
	public final int getNpcId()
	{
		return _npcId;
	}
	
	public final int getItemId()
	{
		return _itemId;
	}
	
	public final int getRewardId()
	{
		return _reward;
	}
	
	public final int getRewardCount()
	{
		return _count;
	}
}
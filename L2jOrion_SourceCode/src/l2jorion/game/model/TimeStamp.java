/*
 * Copyright (C) 2004-2016 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model;

import l2jorion.game.model.actor.instance.L2ItemInstance;

public class TimeStamp
{
	private final int _id1;
	private final int _id2;
	private final long _reuse;
	private final long _stamp;
	private final int _group;
	
	public TimeStamp(L2Skill skill, long reuse, long systime)
	{
		_id1 = skill.getId();
		_id2 = skill.getLevel();
		_reuse = reuse;
		_stamp = systime > 0 ? systime : System.currentTimeMillis() + reuse;
		_group = -1;
	}
	
	public TimeStamp(L2ItemInstance item, long reuse, long systime)
	{
		_id1 = item.getItemId();
		_id2 = item.getObjectId();
		_reuse = reuse;
		_stamp = systime > 0 ? systime : System.currentTimeMillis() + reuse;
		_group = -1;// item.getSharedReuseGroup();
	}
	
	public long getStamp()
	{
		return _stamp;
	}
	
	public int getItemId()
	{
		return _id1;
	}
	
	public int getItemObjectId()
	{
		return _id2;
	}
	
	public int getSkillId()
	{
		return _id1;
	}
	
	public int getSkillLvl()
	{
		return _id2;
	}
	
	public long getReuse()
	{
		return _reuse;
	}
	
	public int getSharedReuseGroup()
	{
		return _group;
	}
	
	public long getRemaining()
	{
		return Math.max(_stamp - System.currentTimeMillis(), 0);
	}
	
	public boolean hasNotPassed()
	{
		return System.currentTimeMillis() < _stamp;
	}
}

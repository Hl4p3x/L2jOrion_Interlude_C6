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
package l2jorion.game.model.entity;

import java.util.ArrayList;
import java.util.List;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.type.L2ResidenceZone;
import l2jorion.game.skills.SkillHolder;

public abstract class AbstractResidence
{
	private final int _residenceId;
	private String _name;
	
	private L2ResidenceZone _zone = null;
	private final List<SkillHolder> _residentialSkills = new ArrayList<>();
	
	public AbstractResidence(int residenceId)
	{
		_residenceId = residenceId;
		// initResidentialSkills();
	}
	
	protected abstract void load();
	
	protected abstract void initResidenceZone();
	
	/*
	 * protected void initResidentialSkills() { final List<L2SkillLearn> residentialSkills = SkillTreesData.getInstance().getAvailableResidentialSkills(getResidenceId()); for (L2SkillLearn s : residentialSkills) { _residentialSkills.add(new SkillHolder(s.getSkillId(), s.getSkillLevel())); } }
	 */
	
	public final int getResidenceId()
	{
		return _residenceId;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public final void setName(String name)
	{
		_name = name;
	}
	
	public L2ResidenceZone getResidenceZone()
	{
		return _zone;
	}
	
	protected void setResidenceZone(L2ResidenceZone zone)
	{
		_zone = zone;
	}
	
	public final List<SkillHolder> getResidentialSkills()
	{
		return _residentialSkills;
	}
	
	public void giveResidentialSkills(L2PcInstance player)
	{
		if ((_residentialSkills != null) && !_residentialSkills.isEmpty())
		{
			for (SkillHolder sh : _residentialSkills)
			{
				player.addSkill(sh.getSkill(), false);
			}
		}
	}
	
	public void removeResidentialSkills(L2PcInstance player)
	{
		if ((_residentialSkills != null) && !_residentialSkills.isEmpty())
		{
			for (SkillHolder sh : _residentialSkills)
			{
				player.removeSkill(sh.getSkill(), false);
			}
		}
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof AbstractResidence) && (((AbstractResidence) obj).getResidenceId() == getResidenceId());
	}
	
	@Override
	public String toString()
	{
		return getName() + "(" + getResidenceId() + ")";
	}
}

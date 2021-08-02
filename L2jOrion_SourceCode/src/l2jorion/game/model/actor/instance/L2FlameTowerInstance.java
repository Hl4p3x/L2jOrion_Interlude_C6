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
package l2jorion.game.model.actor.instance;

import java.util.List;

import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Tower;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.templates.L2NpcTemplate;

public class L2FlameTowerInstance extends L2Tower
{
	private int _upgradeLevel = 0;
	
	private List<Integer> _zoneList;
	
	public L2FlameTowerInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		enableZones(false);
		return super.doDie(killer);
	}
	
	@Override
	public void deleteMe()
	{
		enableZones(false);
		super.deleteMe();
	}
	
	public final void enableZones(boolean state)
	{
		if ((_zoneList != null) && (_upgradeLevel != 0))
		{
			final int maxIndex = _upgradeLevel * 2;
			for (int i = 0; i < maxIndex; i++)
			{
				final L2ZoneType zone = ZoneManager.getInstance().getZoneById(_zoneList.get(i));
				if (zone != null)
				{
					zone.setEnabled(state);
				}
			}
		}
	}
	
	public final void setUpgradeLevel(int level)
	{
		_upgradeLevel = level;
	}
	
	public final void setZoneList(List<Integer> list)
	{
		_zoneList = list;
		enableZones(true);
	}
}
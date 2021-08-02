/*
 * L2jOrion Project - www.l2jorion.com 
 * 
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
package l2jorion.game.model.zone.type;

import java.util.ArrayList;
import java.util.List;

import l2jorion.Config;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.util.random.Rnd;

public class L2RandomZone extends L2ZoneType
{
	private int _id;
	private int _time;
	private boolean _autoPvpFlag = false;
	private List<Location> _locations = new ArrayList<>();
	
	public L2RandomZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(final String name, final String value)
	{
		switch (name)
		{
			case "id":
				_id = Integer.parseInt(value);
				break;
			case "time":
				_time = Integer.parseInt(value);
				break;
			case "autoPvpFlag":
				_autoPvpFlag = Boolean.parseBoolean(value);
				break;
			case "locs":
				for (String locs : value.split(";"))
				{
					_locations.add(new Location(Integer.valueOf(locs.split(",")[0]), Integer.valueOf(locs.split(",")[1]), Integer.valueOf(locs.split(",")[2])));
				}
				break;
			default:
				super.setParameter(name, value);
				break;
		}
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		character.setInsideZone(ZoneId.ZONE_RANDOM, true);
		
		if (character instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) character;
			if (Config.PROHIBIT_HEALER_CLASS && (player.getClassId() == ClassId.cardinal || player.getClassId() == ClassId.evaSaint || player.getClassId() == ClassId.shillienSaint))
			{
				player.sendMessage("You can't enter to zone with Healer Class!");
				player.sendPacket(new ExShowScreenMessage("You can't enter to zone with Healer Class!", 3000, 0x02, false));
				player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				return;
			}
			
			if (_autoPvpFlag)
			{
				player.stopPvPFlag();
				player.updatePvPFlag(1);
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(ZoneId.ZONE_RANDOM, false);
		
		if (character instanceof L2PcInstance)
		{
			if (_autoPvpFlag)
			{
				character.stopPvPFlag();
				((L2PcInstance) character).updatePvPStatus();
			}
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
	
	@Override
	public int getId()
	{
		return _id;
	}
	
	public int getTime()
	{
		return _time;
	}
	
	public Location getLoc()
	{
		return _locations.get(Rnd.get(0, _locations.size() - 1));
	}
}

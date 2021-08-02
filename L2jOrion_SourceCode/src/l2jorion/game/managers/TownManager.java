/* This program is free software; you can redistribute it and/or modify
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
package l2jorion.game.managers;

import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.type.L2TownZone;

public class TownManager
{
	private static TownManager _instance;
	
	public static final TownManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new TownManager();
		}
		
		return _instance;
	}
	
	public TownManager()
	{
	}
	
	public static final int getTownCastle(int townId)
	{
		switch (townId)
		{
			case 912:
				return 1;
			case 916:
				return 2;
			case 918:
				return 3;
			case 922:
				return 4;
			case 924:
				return 5;
			case 926:
				return 6;
			case 1538:
				return 7;
			case 1537:
				return 8;
			case 1714:
				return 9;
			default:
				return 0;
		}
	}
	
	public static final boolean townHasCastleInSiege(int townId)
	{
		int castleIndex = getTownCastle(townId);
		
		if (castleIndex > 0)
		{
			Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
			if (castle != null)
			{
				return castle.getSiege().getIsInProgress();
			}
		}
		return false;
	}
	
	public static final boolean townHasCastleInSiege(int x, int y)
	{
		return townHasCastleInSiege(MapRegionTable.getInstance().getMapRegionLocId(x, y));
	}
	
	public final L2TownZone getTown(int townId)
	{
		for (L2TownZone temp : ZoneManager.getInstance().getAllZones(L2TownZone.class))
		{
			if (temp.getTownId() == townId)
			{
				return temp;
			}
		}
		return null;
	}
	
	public final L2TownZone getTown(int x, int y, int z)
	{
		for (L2ZoneType temp : ZoneManager.getInstance().getZones(x, y, z))
		{
			if (temp instanceof L2TownZone)
			{
				return (L2TownZone) temp;
			}
		}
		return null;
	}
}

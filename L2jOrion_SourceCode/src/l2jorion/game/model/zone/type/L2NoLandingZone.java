/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.model.zone.type;

import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.ZoneId;

public class L2NoLandingZone extends L2ZoneType
{
	private boolean _IsFlyingEnable = true;

	public L2NoLandingZone(final int id)
	{
		super(id);
	}
	@Override
	public void setParameter(String name, String value)
	{
		if(name.equals("flying"))
		{
			_IsFlyingEnable = Boolean.parseBoolean(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	@Override
	protected void onEnter(final L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) character;
			if (player.isFlying() && _IsFlyingEnable)
			{
				player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				return;
			}
			character.setInsideZone(ZoneId.ZONE_NOLANDING, true);
			character.setInsideZone(ZoneId.ZONE_NOSUMMONFRIEND, true);
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			character.setInsideZone(ZoneId.ZONE_NOLANDING, false);
			character.setInsideZone(ZoneId.ZONE_NOSUMMONFRIEND, false);
		}
	}
	
	@Override
	public void onDieInside(final L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(final L2Character character)
	{
	}
}

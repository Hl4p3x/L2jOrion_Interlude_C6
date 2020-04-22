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

import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.L2ZoneType;

public class L2CustomZone extends L2ZoneType
{
	
	public L2CustomZone(final int id)
	{
		super(id);
		_IsFlyingEnable = true;
	}
	
	@Override
	public void onDieInside(final L2Character l2character)
	{
	}
	
	@Override
	public void onReviveInside(final L2Character l2character)
	{
	}
	
	@Override
	public void setParameter(final String name, final String value)
	{
		switch (name)
		{
			case "name":
				_zoneName = value;
				break;
			case "flying":
				_IsFlyingEnable = Boolean.parseBoolean(value);
				break;
			default:
				super.setParameter(name, value);
				break;
		}
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) character;
			if (!player.isGM() && player.isFlying() && !player.isInJail() && !_IsFlyingEnable)
			{
				player.teleToLocation(l2jorion.game.datatables.csv.MapRegionTable.TeleportWhereType.Town);
			}
			
			if (_zoneName.equalsIgnoreCase("tradeoff"))
			{
				player.sendMessage("Trade restrictions are involved.");
				player.setTradeDisabled(true);
			}
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) character;
			
			if (_zoneName.equalsIgnoreCase("tradeoff"))
			{
				player.sendMessage("Trade restrictions removed.");
				player.setTradeDisabled(false);
			}
		}
	}
	
	public String getZoneName()
	{
		return _zoneName;
	}
	
	public boolean isFlyingEnable()
	{
		return _IsFlyingEnable;
	}
	
	private String _zoneName;
	private boolean _IsFlyingEnable;
}

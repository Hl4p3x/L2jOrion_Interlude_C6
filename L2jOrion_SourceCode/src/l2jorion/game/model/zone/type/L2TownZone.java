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
package l2jorion.game.model.zone.type;

import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.ZoneId;

public class L2TownZone extends L2ZoneType
{
	private int _townId;
	private int _taxById;
	private boolean _noPeace;
	
	public L2TownZone(int id)
	{
		super(id);
		
		_taxById = 0;
		
		// Default peace zone
		_noPeace = false;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("townId"))
		{
			_townId = Integer.parseInt(value);
		}
		else if (name.equals("taxById"))
		{
			_taxById = Integer.parseInt(value);
		}
		else if (name.equals("noPeace"))
		{
			_noPeace = Boolean.parseBoolean(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			if (Config.BOT_PROTECTOR)
			{
				((L2PcInstance) character).stopBotChecker();
			}
			
			if (((L2PcInstance) character).getSiegeState() != 0 && Config.ZONE_TOWN == 1)
			{
				return;
			}
			
			if (((L2PcInstance) character).isGM())
			{
				((L2PcInstance) character).sendMessage("You entered " + getName());
			}
			
			((L2PcInstance) character).setLastTownName(getName());
		}
		
		if (!_noPeace && Config.ZONE_TOWN != 2)
		{
			character.setInsideZone(ZoneId.ZONE_PEACE, true);
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			if (Config.BOT_PROTECTOR)
			{
				((L2PcInstance) character).startBotChecker();
			}
		}
		
		if (!_noPeace)
		{
			character.setInsideZone(ZoneId.ZONE_PEACE, false);
		}
		
		if (character instanceof L2PcInstance)
		{
			if (((L2PcInstance) character).isGM())
			{
				((L2PcInstance) character).sendMessage("You left " + getName());
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
	
	public int getTownId()
	{
		return _townId;
	}
	
	public final int getTaxById()
	{
		return _taxById;
	}
}

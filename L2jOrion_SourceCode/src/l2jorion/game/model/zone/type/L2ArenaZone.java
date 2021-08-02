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

import l2jorion.Config;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class L2ArenaZone extends L2ZoneType
{
	// private String _arenaName;
	private final int[] _spawnLoc;
	
	public L2ArenaZone(final int id)
	{
		super(id);
		
		_spawnLoc = new int[3];
	}
	
	@Override
	public void setParameter(final String name, final String value)
	{
		switch (name)
		{
			case "spawnX":
				_spawnLoc[0] = Integer.parseInt(value);
				break;
			case "spawnY":
				_spawnLoc[1] = Integer.parseInt(value);
				break;
			case "spawnZ":
				_spawnLoc[2] = Integer.parseInt(value);
				break;
			default:
				super.setParameter(name, value);
				break;
		}
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		character.setInsideZone(ZoneId.ZONE_PVP, true);
		
		if (character instanceof L2PcInstance)
		{
			((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
			
			if (Config.BOT_PROTECTOR)
			{
				((L2PcInstance) character).stopBotChecker();
			}
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		character.setInsideZone(ZoneId.ZONE_PVP, false);
		
		if (character instanceof L2PcInstance)
		{
			((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
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
	
	public void oustAllPlayers()
	{
		if (_characterList == null)
		{
			return;
		}
		
		if (_characterList.isEmpty())
		{
			return;
		}
		
		for (final L2Character character : _characterList.values())
		{
			if (character == null)
			{
				continue;
			}
			
			if (character instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) character;
				
				if (player.isOnline() == 1)
				{
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
				
				player = null;
			}
		}
	}
	
	public final int[] getSpawnLoc()
	{
		return _spawnLoc;
	}
}

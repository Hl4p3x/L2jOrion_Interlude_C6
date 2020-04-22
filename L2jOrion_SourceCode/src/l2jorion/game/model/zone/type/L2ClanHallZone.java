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

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SiegeSummonInstance;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.siege.clanhalls.DevastatedCastle;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ClanHallDecoration;
import l2jorion.game.network.serverpackets.SystemMessage;

public class L2ClanHallZone extends L2ZoneType
{
	private int _clanHallId;
	private final int[] _spawnLoc;
	
	public L2ClanHallZone(final int id)
	{
		super(id);
		
		_spawnLoc = new int[3];
	}
	
	@Override
	public void setParameter(final String name, final String value)
	{
		switch (name)
		{
			case "clanHallId":
				_clanHallId = Integer.parseInt(value);
				// Register self to the correct clan hall
				ClanHallManager.getInstance().getClanHallById(_clanHallId).setZone(this);
				break;
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
		if (DevastatedCastle.getInstance().getIsInProgress())
		{
			character.setInsideZone(L2Character.ZONE_PVP, true);
			character.setInsideZone(L2Character.ZONE_SIEGE, true);
			
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
			}
		}
		if (character instanceof L2PcInstance)
		{
			// Set as in clan hall
			character.setInsideZone(L2Character.ZONE_CLANHALL, true);
			
			ClanHall clanHall = ClanHallManager.getInstance().getClanHallById(_clanHallId);
			
			if (clanHall == null)
				return;
			
			// Send decoration packet
			final ClanHallDecoration deco = new ClanHallDecoration(clanHall);
			((L2PcInstance) character).sendPacket(deco);
			
			// Send a message
			if (clanHall.getOwnerId() != 0 && clanHall.getOwnerId() == ((L2PcInstance) character).getClanId())
			{
				((L2PcInstance) character).sendMessage("You have entered your clan hall");
			}
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (DevastatedCastle.getInstance().getIsInProgress())
		{
			character.setInsideZone(L2Character.ZONE_PVP, false);
			character.setInsideZone(L2Character.ZONE_SIEGE, false);
			
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
				
				// Set pvp flag
				if (((L2PcInstance) character).getPvpFlag() == 0)
				{
					((L2PcInstance) character).startPvPFlag();
				}
			}
		}
		if (character instanceof L2SiegeSummonInstance)
		{
			((L2SiegeSummonInstance) character).unSummon(((L2SiegeSummonInstance) character).getOwner());
		}
		if (character instanceof L2PcInstance)
		{
			// Unset clanhall zone
			character.setInsideZone(L2Character.ZONE_CLANHALL, false);
			
			// Send a message
			if (((L2PcInstance) character).getClanId() != 0 && ClanHallManager.getInstance().getClanHallById(_clanHallId).getOwnerId() == ((L2PcInstance) character).getClanId())
			{
				((L2PcInstance) character).sendMessage("You have left your clan hall");
			}
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
	
	/**
	 * Removes all foreigners from the clan hall
	 * @param owningClanId
	 */
	public void banishForeigners(final int owningClanId)
	{
		for (final L2Character temp : _characterList.values())
		{
			if (!(temp instanceof L2PcInstance))
			{
				continue;
			}
			
			if (((L2PcInstance) temp).getClanId() == owningClanId)
			{
				continue;
			}
			
			((L2PcInstance) temp).teleToLocation(MapRegionTable.TeleportWhereType.Town);
		}
	}
	
	@Override
	public FastMap<Integer, L2Character> getCharactersInside()
	{
		return _characterList;
	}
	
	/**
	 * Get the clan hall's spawn
	 * @return
	 */
	public Location getSpawn()
	{
		return new Location(_spawnLoc[0], _spawnLoc[1], _spawnLoc[2]);
	}
	
	public void updateZoneStatusForCharactersInside()
	{
		if (DevastatedCastle.getInstance().getIsInProgress())
		{
			for (final L2Character character : _characterList.values())
			{
				try
				{
					onEnter(character);
				}
				catch (final NullPointerException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
				}
			}
		}
		else
		{
			for (final L2Character character : _characterList.values())
			{
				try
				{
					character.setInsideZone(L2Character.ZONE_PVP, false);
					character.setInsideZone(L2Character.ZONE_SIEGE, false);
					
					if (character instanceof L2PcInstance)
					{
						((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
					}
					
					if (character instanceof L2SiegeSummonInstance)
					{
						((L2SiegeSummonInstance) character).unSummon(((L2SiegeSummonInstance) character).getOwner());
					}
				}
				catch (final NullPointerException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
				}
			}
		}
	}
}

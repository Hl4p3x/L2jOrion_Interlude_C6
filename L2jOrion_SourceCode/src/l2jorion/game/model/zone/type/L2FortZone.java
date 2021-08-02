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

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.managers.FortManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SiegeSummonInstance;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class L2FortZone extends L2ZoneType
{
	private int _fortId;
	private Fort _fort;
	private final int[] _spawnLoc;
	
	public L2FortZone(final int id)
	{
		super(id);
		
		_spawnLoc = new int[3];
	}
	
	@Override
	public void setParameter(final String name, final String value)
	{
		switch (name)
		{
			case "fortId":
				_fortId = Integer.parseInt(value);
				
				// Register self to the correct fort
				_fort = FortManager.getInstance().getFortById(_fortId);
				_fort.setZone(this);
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
		if (_fort.getSiege().getIsInProgress())
		{
			character.setInsideZone(ZoneId.ZONE_PVP, true);
			character.setInsideZone(ZoneId.ZONE_SIEGE, true);
			
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
			}
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (_fort.getSiege().getIsInProgress())
		{
			character.setInsideZone(ZoneId.ZONE_PVP, false);
			character.setInsideZone(ZoneId.ZONE_SIEGE, false);
			
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
	}
	
	@Override
	public void onDieInside(final L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(final L2Character character)
	{
	}
	
	public void updateZoneStatusForCharactersInside()
	{
		if (_fort.getSiege().getIsInProgress())
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
					{
						e.printStackTrace();
					}
				}
			}
		}
		else
		{
			for (final L2Character character : _characterList.values())
			{
				try
				{
					character.setInsideZone(ZoneId.ZONE_PVP, false);
					character.setInsideZone(ZoneId.ZONE_SIEGE, false);
					
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
					{
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * Removes all foreigners from the fort
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
	
	/**
	 * Sends a message to all players in this zone
	 * @param message
	 */
	public void announceToPlayers(final String message)
	{
		for (final L2Character temp : _characterList.values())
		{
			if (temp instanceof L2PcInstance)
			{
				((L2PcInstance) temp).sendMessage(message);
			}
		}
	}
	
	/**
	 * Returns all players within this zone
	 * @return
	 */
	public FastList<L2PcInstance> getAllPlayers()
	{
		final FastList<L2PcInstance> players = new FastList<>();
		
		for (final L2Character temp : _characterList.values())
		{
			if (temp instanceof L2PcInstance)
			{
				players.add((L2PcInstance) temp);
			}
		}
		
		return players;
	}
	
	/**
	 * Get the forts defender spawn
	 * @return
	 */
	public int[] getSpawn()
	{
		return _spawnLoc;
	}
}

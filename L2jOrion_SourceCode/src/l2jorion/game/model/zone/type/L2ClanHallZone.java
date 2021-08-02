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
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SiegeSummonInstance;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.ClanHallDecoration;

public class L2ClanHallZone extends L2ResidenceZone
{
	public L2ClanHallZone(final int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(final String name, final String value)
	{
		if (name.equals("clanHallId"))
		{
			setResidenceId(Integer.parseInt(value));
			// Register self to the correct clan hall
			ClanHall hall = ClanHallManager.getInstance().getClanHallsById(getResidenceId());
			if (hall == null)
			{
				LOG.warn("L2ClanHallZone: Clan hall with id " + getResidenceId() + " does not exist!");
			}
			else
			{
				hall.setZone(this);
			}
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
			// Set as in clan hall
			character.setInsideZone(ZoneId.ZONE_CLANHALL, true);
			
			ClanHall clanHall = ClanHallManager.getInstance().getClanHallById(getResidenceId());
			
			if (clanHall == null)
			{
				return;
			}
			
			// Send decoration packet
			final ClanHallDecoration deco = new ClanHallDecoration(clanHall);
			((L2PcInstance) character).sendPacket(deco);
			
			// Send a message
			if (clanHall.getOwnerId() != 0 && clanHall.getOwnerId() == ((L2PcInstance) character).getClanId())
			{
				((L2PcInstance) character).sendMessage("You have entered your clan hall.");
			}
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (character instanceof L2SiegeSummonInstance)
		{
			((L2SiegeSummonInstance) character).unSummon(((L2SiegeSummonInstance) character).getOwner());
		}
		if (character instanceof L2PcInstance)
		{
			// Unset clanhall zone
			character.setInsideZone(ZoneId.ZONE_CLANHALL, false);
			
			// Send a message
			if (((L2PcInstance) character).getClanId() != 0 && ClanHallManager.getInstance().getClanHallsById(getResidenceId()).getOwnerId() == ((L2PcInstance) character).getClanId())
			{
				((L2PcInstance) character).sendMessage("You have left your clan hall.");
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
	 * Get the clan hall's spawn
	 * @return
	 */
	public Location getSpawn()
	{
		return new Location(0, 0, 0);
	}
	
	@Override
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
	
	public void banishNonSiegeParticipants()
	{
		for (L2PcInstance player : getPlayersInside())
		{
			if ((player != null) && player.isInHideoutSiege())
			{
				player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
		}
	}
}

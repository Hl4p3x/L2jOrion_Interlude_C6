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

import l2jorion.game.datatables.csv.MapRegionTable.TeleportWhereType;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.olympiad.OlympiadGameTask;
import l2jorion.game.model.zone.L2ZoneRespawn;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExOlympiadMatchEnd;
import l2jorion.game.network.serverpackets.ExOlympiadUserInfo;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.game.network.serverpackets.SystemMessage;

public class L2OlympiadStadiumZone extends L2ZoneRespawn
{
	OlympiadGameTask _task = null;
	
	public L2OlympiadStadiumZone(final int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(final String name, final String value)
	{
		super.setParameter(name, value);
	}
	
	@Override
	public void onEnter(L2Character character)
	{
		character.setInsideZone(ZoneId.ZONE_NOSUMMONFRIEND, true);
		character.setInsideZone(ZoneId.ZONE_NOLANDING, true);
		character.setInsideZone(ZoneId.ZONE_NORESTART, true);
		
		if (_task != null && _task.isBattleStarted())
		{
			character.setInsideZone(ZoneId.ZONE_PVP, true);
			if (character instanceof L2PcInstance)
			{
				character.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
				_task.getGame().sendOlympiadInfo(character);
			}
		}
		
		// Only participants, observers and GMs are allowed.
		final L2PcInstance player = character.getActingPlayer();
		
		if (player != null && !player.isGM() && !player.isInOlympiadMode() && !player.inObserverMode() && !player.isInArenaEvent() && !player.isArenaProtection())
		{
			final L2Summon summon = player.getPet();
			if (summon != null)
			{
				summon.unSummon(player);
			}
			
			player.teleToLocation(TeleportWhereType.Town);
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(ZoneId.ZONE_NOSUMMONFRIEND, false);
		character.setInsideZone(ZoneId.ZONE_NOLANDING, false);
		character.setInsideZone(ZoneId.ZONE_NORESTART, false);
		
		if (_task != null && _task.isBattleStarted())
		{
			character.setInsideZone(ZoneId.ZONE_PVP, false);
			
			if (character instanceof L2PcInstance)
			{
				character.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
				character.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
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
	
	public final void updateZoneStatusForCharactersInside()
	{
		if (_task == null)
		{
			return;
		}
		
		final boolean battleStarted = _task.isBattleStarted();
		final SystemMessage sm = SystemMessage.getSystemMessage((battleStarted) ? SystemMessageId.ENTERED_COMBAT_ZONE : SystemMessageId.LEFT_COMBAT_ZONE);
		
		for (L2Character character : _characterList.values())
		{
			if (battleStarted)
			{
				character.setInsideZone(ZoneId.ZONE_PVP, true);
				if (character instanceof L2PcInstance)
				{
					character.sendPacket(sm);
				}
			}
			else
			{
				character.setInsideZone(ZoneId.ZONE_PVP, false);
				if (character instanceof L2PcInstance)
				{
					character.sendPacket(sm);
					character.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
				}
			}
		}
	}
	
	public final void registerTask(OlympiadGameTask task)
	{
		_task = task;
	}
	
	public final void broadcastStatusUpdate(L2PcInstance player)
	{
		final ExOlympiadUserInfo packet = new ExOlympiadUserInfo(player);
		for (L2PcInstance plyr : getKnownTypeInside(L2PcInstance.class))
		{
			if (plyr.inObserverMode() || plyr.getOlympiadSide() != player.getOlympiadSide())
			{
				plyr.sendPacket(packet);
			}
		}
	}
	
	public final void broadcastPacketToObservers(L2GameServerPacket packet)
	{
		for (L2PcInstance player : getKnownTypeInside(L2PcInstance.class))
		{
			if (player.inObserverMode())
			{
				player.sendPacket(packet);
			}
		}
	}
}
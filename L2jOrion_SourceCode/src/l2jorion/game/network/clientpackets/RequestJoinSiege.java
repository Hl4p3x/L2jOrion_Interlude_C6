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

package l2jorion.game.network.clientpackets;

import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SiegeInfo;
import l2jorion.game.network.serverpackets.SystemMessage;

public final class RequestJoinSiege extends PacketClient
{
	private int _castleId;
	private int _isAttacker;
	private int _isJoining;
	
	@Override
	protected void readImpl()
	{
		_castleId = readD();
		_isAttacker = readD();
		_isJoining = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		
		if (player == null)
		{
			return;
		}
		
		if (!player.isClanLeader())
		{
			return;
		}
		
		L2Clan clan = player.getClan();
		if (clan == null)
		{
			return;
		}
		
		if (_castleId < 100)
		{
			final Castle castle = CastleManager.getInstance().getCastleById(_castleId);
			
			if (castle == null)
			{
				return;
			}
			
			if (_isJoining == 1)
			{
				if (System.currentTimeMillis() < player.getClan().getDissolvingExpiryTime())
				{
					player.sendPacket(new SystemMessage(SystemMessageId.CANT_PARTICIPATE_IN_SIEGE_WHILE_DISSOLUTION_IN_PROGRESS));
					return;
				}
				
				if (_isAttacker == 1)
				{
					castle.getSiege().registerAttacker(player);
				}
				else
				{
					castle.getSiege().registerDefender(player);
				}
			}
			else
			{
				castle.getSiege().removeSiegeClan(player);
			}
			
			castle.getSiege().listRegisterClan(player);
		}
		
		SiegableHall hall = CHSiegeManager.getInstance().getSiegableHall(_castleId);
		if (hall != null)
		{
			if (_isJoining == 1)
			{
				if (System.currentTimeMillis() < clan.getDissolvingExpiryTime())
				{
					player.sendPacket(SystemMessageId.CANT_PARTICIPATE_IN_SIEGE_WHILE_DISSOLUTION_IN_PROGRESS);
					return;
				}
				CHSiegeManager.getInstance().registerClan(clan, hall, player);
			}
			else
			{
				CHSiegeManager.getInstance().unRegisterClan(clan, hall, player);
			}
			player.sendPacket(new SiegeInfo(hall));
		}
		
		final Fort fort = FortManager.getInstance().getFortById(_castleId);
		if (fort != null)
		{
			if (_isJoining == 1)
			{
				if (System.currentTimeMillis() < player.getClan().getDissolvingExpiryTime())
				{
					player.sendPacket(new SystemMessage(SystemMessageId.CANT_PARTICIPATE_IN_SIEGE_WHILE_DISSOLUTION_IN_PROGRESS));
					return;
				}
				
				if (_isAttacker == 1)
				{
					fort.getSiege().registerAttacker(player);
				}
				else
				{
					fort.getSiege().registerDefender(player);
				}
			}
			else
			{
				fort.getSiege().removeSiegeClan(player);
			}
			
			fort.getSiege().listRegisterClan(player);
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] a4 RequestJoinSiege";
	}
}

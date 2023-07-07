/*
 * L2jOrion Project - www.l2jorion.com 
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

import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExAskJoinPartyRoom;
import l2jorion.game.network.serverpackets.SystemMessage;

public class RequestAskJoinPartyRoom extends PacketClient
{
	private static String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance _activeChar = getClient().getActiveChar();
		if (_activeChar == null)
		{
			return;
		}
		
		// Send PartyRoom invite request (with activeChar) name to the target
		final L2PcInstance _target = L2World.getInstance().getPlayer(_name);
		if (_target != null)
		{
			if (!_target.isProcessingRequest())
			{
				_activeChar.onTransactionRequest(_target);
				_target.sendPacket(new ExAskJoinPartyRoom(_activeChar.getName()));
			}
			else
			{
				_activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(_target.getName()));
			}
		}
		else
		{
			_activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME));
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] D0:14 RequestAskJoinPartyRoom";
	}
	
}

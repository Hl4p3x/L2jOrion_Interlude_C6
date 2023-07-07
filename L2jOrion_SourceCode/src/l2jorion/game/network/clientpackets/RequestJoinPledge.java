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
package l2jorion.game.network.clientpackets;

import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.AskJoinPledge;
import l2jorion.game.network.serverpackets.SystemMessage;

public final class RequestJoinPledge extends PacketClient
{
	private int _target;
	private int _pledgeType;
	
	@Override
	protected void readImpl()
	{
		_target = readD();
		_pledgeType = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
			return;
		
		if (!(L2World.getInstance().findObject(_target) instanceof L2PcInstance))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET));
			return;
		}
		
		final L2PcInstance target = (L2PcInstance) L2World.getInstance().findObject(_target);
		final L2Clan clan = activeChar.getClan();
		
		if (!clan.checkClanJoinCondition(activeChar, target, _pledgeType))
			return;
		
		if (!activeChar.getRequest().setRequest(target, this))
			return;
		
		final AskJoinPledge ap = new AskJoinPledge(activeChar.getObjectId(), activeChar.getClan().getName());
		target.sendPacket(ap);
	}
	
	public int getPledgeType()
	{
		return _pledgeType;
	}
	
	@Override
	public String getType()
	{
		return "[C] 24 RequestJoinPledge";
	}
}
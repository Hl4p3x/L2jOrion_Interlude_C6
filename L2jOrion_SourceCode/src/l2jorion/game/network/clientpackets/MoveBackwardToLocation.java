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

import java.nio.BufferUnderflowException;

import l2jorion.Config;
import l2jorion.bots.FakePlayer;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.StopMove;
import l2jorion.game.util.IllegalPlayerAction;
import l2jorion.game.util.Util;

public class MoveBackwardToLocation extends PacketClient
{
	private int _targetX;
	private int _targetY;
	private int _targetZ;
	
	private int _originX;
	private int _originY;
	private int _originZ;
	
	private int _moveMovement;
	
	@Override
	protected void readImpl()
	{
		_targetX = readD();
		_targetY = readD();
		_targetZ = readD();
		
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		
		try
		{
			_moveMovement = readD(); // is 0 if cursor keys are used 1 if mouse is used
		}
		catch (BufferUnderflowException e)
		{
			if (Config.L2WALKER_PROTEC)
			{
				L2PcInstance activeChar = getClient().getActiveChar();
				activeChar.sendPacket(SystemMessageId.HACKING_TOOL);
				Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " trying to use L2Walker!", IllegalPlayerAction.PUNISH_KICK);
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.isSitting())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.isTeleporting())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (_targetX == _originX && _targetY == _originY && _targetZ == _originZ)
		{
			activeChar.sendPacket(new StopMove(activeChar));
			return;
		}
		
		_targetZ += activeChar.getTemplate().getCollisionHeight();
		
		if (activeChar.getTeleMode() > 0)
		{
			if (activeChar.getTeleMode() == 1)
			{
				activeChar.setTeleMode(0);
			}
			
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			activeChar.teleToLocation(_targetX, _targetY, _targetZ, false);
			return;
		}
		
		if (_moveMovement == 0)
		{
			if (!Config.ALLOW_USE_CURSOR_FOR_WALK)
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		if (activeChar.isControllingFakePlayer())
		{
			FakePlayer fakePlayer = activeChar.getPlayerUnderControl();
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			fakePlayer.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_targetX, _targetY, _targetZ));
			return;
		}
		
		double dx = _targetX - activeChar.getX();
		double dy = _targetY - activeChar.getY();
		
		if (activeChar.isOutOfControl() || dx * dx + dy * dy > 98010000)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_targetX, _targetY, _targetZ, 0));
		if (activeChar.getTeleport())
		{
			activeChar.setLastFallingPosition(_targetX, _targetY, _targetZ);
		}
		
		// Remove queued skill on movement
		if (activeChar.getQueuedSkill() != null)
		{
			activeChar.setQueuedSkill(null, false, false);
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 01 MoveBackwardToLoc";
	}
}
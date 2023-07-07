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

import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SummonInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.ActionFailed;

public final class AttackRequest extends PacketClient
{
	private int _objectId;
	@SuppressWarnings("unused")
	private int _originX, _originY, _originZ;
	
	@SuppressWarnings("unused")
	private int _attackId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_attackId = readC(); // 0 for ctrl click - 1 for shift-click
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final L2Object target;
		
		if (activeChar.getTargetId() == _objectId)
		{
			target = activeChar.getTarget();
		}
		else
		{
			target = L2World.getInstance().findObject(_objectId);
		}
		
		if (target == null)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Players can't attack objects in the other instances except from multiverse
		if (target.getInstanceId() != activeChar.getInstanceId() && activeChar.getInstanceId() != -1)
		{
			return;
		}
		
		// Only GMs can directly attack invisible characters
		if (target instanceof L2PcInstance && ((L2PcInstance) target).getAppearance().getInvisible() && !activeChar.isGM())
		{
			return;
		}
		
		// During teleport phase, players cant do any attack
		if ((TvT.is_teleport() && activeChar._inEventTvT) || (CTF.is_teleport() && activeChar._inEventCTF) || (DM.is_teleport() && activeChar._inEventDM))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// No attacks to same team in Event
		if (TvT.is_started())
		{
			if (target instanceof L2PcInstance)
			{
				if ((activeChar._inEventTvT && ((L2PcInstance) target)._inEventTvT) && activeChar._teamNameTvT.equals(((L2PcInstance) target)._teamNameTvT))
				{
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (target instanceof L2SummonInstance)
			{
				if ((activeChar._inEventTvT && ((L2SummonInstance) target).getOwner()._inEventTvT) && activeChar._teamNameTvT.equals(((L2SummonInstance) target).getOwner()._teamNameTvT))
				{
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}
		
		if (CTF.is_started())
		{
			if (target instanceof L2PcInstance)
			{
				if ((activeChar._inEventCTF && ((L2PcInstance) target)._inEventCTF) && activeChar._teamNameCTF.equals(((L2PcInstance) target)._teamNameCTF))
				{
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (target instanceof L2SummonInstance)
			{
				if ((activeChar._inEventCTF && ((L2SummonInstance) target).getOwner()._inEventCTF) && activeChar._teamNameCTF.equals(((L2SummonInstance) target).getOwner()._teamNameCTF))
				{
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}
		
		if (activeChar.getTarget() != target)
		{
			target.onAction(activeChar);
		}
		else
		{
			if ((target.getObjectId() != activeChar.getObjectId()) && activeChar.getPrivateStoreType() == 0)
			{
				target.onForcedAttack(activeChar);
			}
			else
			{
				sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 0A AttackRequest";
	}
}
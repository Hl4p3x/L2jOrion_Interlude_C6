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

import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class Action extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(Action.class);
	private int _objectId;
	private int _originX;
	private int _originY;
	private int _originZ;
	private int _actionId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD(); // Target object Identifier
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_actionId = readC(); // Action identifier : 0-Simple click, 1-Shift click
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.DEBUG)
		{
			LOG.debug(getType() + ": " + (_actionId == 0 ? "Simple-click" : "Shift-click") + " Target object ID: " + _objectId + " orignX: " + _originX + " orignY: " + _originY + " orignZ: " + _originZ);
		}
		
		// Get the current L2PcInstance of the player
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.inObserverMode())
		{
			getClient().sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final L2Object obj;
		
		if (activeChar.getTargetId() == _objectId)
		{
			obj = activeChar.getTarget();
		}
		else
		{
			obj = L2World.getInstance().findObject(_objectId);
		}
		
		// If object requested does not exist
		// pressing e.g. pickup many times quickly would get you here
		if (obj == null)
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Players can't interact with objects in the other instances except from multiverse
		if (obj.getInstanceId() != activeChar.getInstanceId() && activeChar.getInstanceId() != -1)
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Only GMs can directly interact with invisible characters
		if (obj instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) obj;
			if (player.getAppearance().getInvisible() && !activeChar.isGM() || player.isInArenaEvent() && !activeChar.isInArenaEvent() || !player.isInArenaEvent() && activeChar.isInArenaEvent())
			{
				getClient().sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Check if the target is valid, if the player haven't a shop or isn't the requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...)
		if (activeChar.getPrivateStoreType() == 0)
		{
			switch (_actionId)
			{
				case 0:
				{
					obj.onAction(activeChar);
					break;
				}
				case 1:
				{
					if (obj instanceof L2Character && ((L2Character) obj).isAlikeDead())
					{
						obj.onAction(activeChar);
					}
					else
					{
						obj.onActionShift(getClient());
					}
					break;
				}
				default:
				{
					LOG.warn("Character: " + activeChar.getName() + " requested invalid action: " + _actionId);
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					break;
				}
			}
		}
		else
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET); // Actions prohibited when in trade
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 04 Action";
	}
}
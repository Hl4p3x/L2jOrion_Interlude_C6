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

import l2jguard.Protection;
import l2jorion.Config;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.L2GameClient.GameClientState;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.CharSelected;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class CharacterSelected extends L2GameClientPacket
{
	private static Logger LOG = LoggerFactory.getLogger(CharacterSelected.class);
	
	private int _charSlot;
	
	@SuppressWarnings("unused")
	private int _unk1, _unk2, _unk3, _unk4;
	
	@Override
	protected void readImpl()
	{
		_charSlot = readD();
		_unk1 = readH();
		_unk2 = readD();
		_unk3 = readD();
		_unk4 = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (!getClient().getFloodProtectors().getCharacterSelect().tryPerformAction("CharacterSelect"))
		{
			return;
		}
		
		// we should always be abble to acquire the lock but if we cant lock then nothing should be done (ie repeated packet)
		if (getClient().getActiveCharLock().tryLock())
		{
			try
			{
				// should always be null but if not then this is repeated packet and nothing should be done here
				if (getClient().getActiveChar() == null)
				{
					// The L2PcInstance must be created here, so that it can be attached to the L2GameClient
					if (Config.DEBUG)
					{
						LOG.debug("DEBUG " + getType() + ": selected slot:" + _charSlot);
					}
					
					// Load up character from disk
					final L2PcInstance cha = getClient().loadCharFromDisk(_charSlot);
					
					if (cha == null)
					{
						LOG.warn(getType() + ": Character could not be loaded (slot:" + _charSlot + ")");
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					
					L2World.getInstance().addPlayerToWorld(cha);
					
					if (cha.getAccessLevel().getLevel() < 0)
					{
						cha.deleteMe();
						return;
					}
					
					cha.setClient(getClient());
					getClient().setActiveChar(cha);
					
					if (Config.L2JGUARD_PROTECTION)
					{
						if (!Protection.checkPlayerWithHWID(getClient(), cha.getObjectId(), cha.getName()))
						{
							return;
						}
					}
					
					getClient().setState(GameClientState.ENTERING);
					sendPacket(new CharSelected(cha, getClient().getSessionId().playOkID1));
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				getClient().getActiveCharLock().unlock();
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 0D CharacterSelected";
	}
}
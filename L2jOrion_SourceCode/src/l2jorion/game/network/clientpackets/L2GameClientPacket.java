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
import java.util.logging.Level;
import java.util.logging.Logger;

import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.mmocore.ReceivablePacket;

public abstract class L2GameClientPacket extends ReceivablePacket<L2GameClient>
{
	protected static final Logger LOG = Logger.getLogger(L2GameClientPacket.class.getName());
	
	@Override
	protected boolean read()
	{
		try
		{
			readImpl();
			return true;
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, "Client: " + getClient().toString() + " - Failed reading: " + getType() + " ; " + e.getMessage(), e);
			
			if (e instanceof BufferUnderflowException)
			{
				getClient().onBufferUnderflow();
			}
		}
		return false;
	}
	
	protected abstract void readImpl();
	
	@Override
	public void run()
	{
		try
		{
			runImpl();
			
			if (this instanceof MoveBackwardToLocation || this instanceof AttackRequest || this instanceof RequestMagicSkillUse)
			{
				if (getClient().getActiveChar() != null)
				{
					getClient().getActiveChar().onActionRequest(); // Removes onSpawn Protection
				}
			}
		}
		catch (final Throwable t)
		{
			LOG.log(Level.SEVERE, "Client: " + getClient().toString() + " - Failed running: " + getType() + " ; " + t.getMessage(), t);
			
			if (this instanceof EnterWorld)
			{
				getClient().closeNow();
			}
		}
	}
	
	protected abstract void runImpl();
	
	protected final void sendPacket(final L2GameServerPacket gsp)
	{
		getClient().sendPacket(gsp);
	}
	
	public abstract String getType();
}
/* This program is free software; you can redistribute it and/or modify
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
package l2jorion.game.network.serverpackets;

import l2jorion.game.network.L2GameClient;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.mmocore.SendablePacket;

public abstract class L2GameServerPacket extends SendablePacket<L2GameClient>
{
	public static final Logger LOG = LoggerFactory.getLogger(L2GameServerPacket.class);
	
	@Override
	protected void write()
	{
		try
		{
			writeImpl();
			
			/*
			 * if (!(this instanceof SystemMessage) && !(this instanceof SocialAction) && !(this instanceof CreatureSay) && !(this instanceof StatusUpdate)) { if (getClient() != null && getClient().getActiveChar() != null) { if (getClient().getActiveChar().isInOlympiadMode() ||
			 * getClient().getActiveChar().inObserverMode()) { getClient().getActiveChar().sendPacket(new CreatureSay(2, Say2.HERO_VOICE, getClient().getActiveChar().getName() + " received", "" + getType())); String text = (getClient().getActiveChar().inObserverMode() ? "Spectator" : "Fighter") +
			 * " " + getClient().getActiveChar().getName() + " received packet: " + getType(); Log.addOlyLog(text, getClient().getActiveChar().getName()); } } }
			 */
		}
		catch (Throwable t)
		{
			LOG.error("Client: " + getClient().toString() + " - Failed writing: " + getType() + "");
			t.printStackTrace();
		}
	}
	
	public void runImpl()
	{
	}
	
	protected abstract void writeImpl();
	
	public abstract String getType();
}
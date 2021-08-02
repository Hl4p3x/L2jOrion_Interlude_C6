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
package l2jorion.login.network.clientpackets;

import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.login.L2LoginClient;
import l2jorion.mmocore.ReceivablePacket;

public abstract class L2LoginClientPacket extends ReceivablePacket<L2LoginClient>
{
	private static Logger LOG = LoggerFactory.getLogger(L2LoginClientPacket.class);
	
	@Override
	protected final boolean read()
	{
		try
		{
			return readImpl();
		}
		catch (final Exception e)
		{
			LOG.error("ERROR READING: " + this.getClass().getSimpleName(), e);
			return false;
		}
	}
	
	protected abstract boolean readImpl();
}

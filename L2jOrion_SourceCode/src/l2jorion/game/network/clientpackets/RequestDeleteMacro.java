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

import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public final class RequestDeleteMacro extends PacketClient
{
	private int _id;
	
	@Override
	protected void readImpl()
	{
		_id = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (getClient().getActiveChar() == null)
			return;
		
		// Macro exploit fix
		if (!getClient().getFloodProtectors().getMacro().tryPerformAction("delete macro"))
			return;
		
		getClient().getActiveChar().deleteMacro(_id);
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		sm.addString("Delete macro id=" + _id);
		sendPacket(sm);
		sm = null;
	}
	
	@Override
	public String getType()
	{
		return "[C] C2 RequestDeleteMacro";
	}
	
}

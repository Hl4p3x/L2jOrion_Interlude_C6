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

import l2jorion.game.cache.CrestCache;
import l2jorion.game.cache.CrestCache.CrestType;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.ExPledgeCrestLarge;

public final class RequestExPledgeCrestLarge extends PacketClient
{
	private int _crestId;
	
	@Override
	protected void readImpl()
	{
		_crestId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		byte[] data = CrestCache.getInstance().getCrest(CrestType.PLEDGE_LARGE, _crestId);
		
		if (data != null)
		{
			sendPacket(new ExPledgeCrestLarge(_crestId, data));
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] D0:10 RequestExPledgeCrestLarge";
	}
	
}

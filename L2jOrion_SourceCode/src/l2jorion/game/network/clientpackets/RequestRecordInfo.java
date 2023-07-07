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

import java.util.Collection;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.SpawnItemPoly;
import l2jorion.game.network.serverpackets.UserInfo;

public class RequestRecordInfo extends PacketClient
{	
	@Override
	protected void readImpl()
	{
		// trigger
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance _activeChar = getClient().getActiveChar();
		
		if (_activeChar == null)
			return;
		
		_activeChar.sendPacket(new UserInfo(_activeChar));
		
		Collection<L2Object> objs = _activeChar.getKnownList().getKnownObjects().values();
		for (L2Object object : objs)
		{
			if (object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item"))
			{
				_activeChar.sendPacket(new SpawnItemPoly(object));
			}
			else
			{
				object.sendInfo(_activeChar);
				
				if (object instanceof L2Character)
				{
					L2Character obj = (L2Character) object;
					if (obj.getAI() != null)
						obj.getAI().describeStateToPlayer(_activeChar);
				}
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[0] CF RequestRecordInfo";
	}
}

/*
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

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.SkillCoolTime;

public class RequestSkillCoolTime extends PacketClient
{
	L2GameClient _client;
	
	@Override
	public void readImpl()
	{
		_client = getClient();
	}
	
	@Override
	public void runImpl()
	{
		L2PcInstance pl = _client.getActiveChar();
		if (pl != null)
			pl.sendPacket(new SkillCoolTime(pl));
	}
	
    @Override
	public String getType()
    {
        return "[C] 0xa6 RequestSkillCoolTime";
    }
}
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

import l2jorion.game.model.L2ShortCut;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.ShortCutRegister;

public final class RequestShortCutReg extends PacketClient
{
	private int _type;
	private int _id;
	private int _slot;
	private int _page;
	private int _unk;
	
	@Override
	protected void readImpl()
	{
		_type = readD();
		int slot = readD();
		_id = readD();
		_unk = readD();
		
		_slot = slot % 12;
		_page = slot / 12;
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		switch (_type)
		{
			case 0x01: // item
			case 0x03: // action
			case 0x04: // macro
			case 0x05: // recipe
			{
				L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, -1, _unk);
				activeChar.registerShortCut(sc);
				sendPacket(new ShortCutRegister(sc));
				break;
			}
			case 0x02: // skill
			{
				int level = activeChar.getSkillLevel(_id);
				if (level > 0)
				{
					final L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, level, _unk);
					activeChar.registerShortCut(sc);
					sendPacket(new ShortCutRegister(sc));
				}
				break;
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 33 RequestShortCutReg";
	}
}
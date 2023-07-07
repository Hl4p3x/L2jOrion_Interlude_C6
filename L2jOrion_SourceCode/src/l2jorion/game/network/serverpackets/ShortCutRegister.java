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
package l2jorion.game.network.serverpackets;

import l2jorion.game.model.L2ShortCut;
import l2jorion.game.network.PacketServer;

public class ShortCutRegister extends PacketServer
{
	private static final String _S__56_SHORTCUTREGISTER = "[S] 44 ShortCutRegister";
	
	private final L2ShortCut _shortcut;
	
	public ShortCutRegister(final L2ShortCut shortcut)
	{
		_shortcut = shortcut;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x44);
		
		writeD(_shortcut.getType());
		writeD(_shortcut.getSlot() + _shortcut.getPage() * 12); // C4 Client
		
		switch (_shortcut.getType())
		{
			case L2ShortCut.TYPE_ITEM: // 1
				writeD(_shortcut.getId());
				break;
			case L2ShortCut.TYPE_SKILL: // 2
				writeD(_shortcut.getId());
				writeD(_shortcut.getLevel());
				writeC(0x00); // C5
				break;
			case L2ShortCut.TYPE_ACTION: // 3
				writeD(_shortcut.getId());
				break;
			case L2ShortCut.TYPE_MACRO: // 4
				writeD(_shortcut.getId());
				break;
			case L2ShortCut.TYPE_RECIPE: // 5
				writeD(_shortcut.getId());
				break;
			default:
				writeD(_shortcut.getId());
		}
		
		writeD(1);// ??
	}
	
	@Override
	public String getType()
	{
		return _S__56_SHORTCUTREGISTER;
	}
}

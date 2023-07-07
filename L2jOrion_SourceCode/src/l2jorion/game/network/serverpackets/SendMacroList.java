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

import l2jorion.game.model.L2Macro;
import l2jorion.game.network.PacketServer;

public class SendMacroList extends PacketServer
{
	private static final String _S__E7_SENDMACROLIST = "[S] E7 SendMacroList";
	
	private final int _rev;
	private final int _count;
	private final L2Macro _macro;
	
	public SendMacroList(final int rev, final int count, final L2Macro macro)
	{
		_rev = rev;
		_count = count;
		_macro = macro;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xE7);
		
		writeD(_rev); // macro change revision (changes after each macro edition)
		writeC(0); // unknown
		writeC(_count); // count of Macros
		writeC(_macro != null ? 1 : 0); // unknown
		
		if (_macro != null)
		{
			writeD(_macro.id); // Macro ID
			writeS(_macro.name); // Macro Name
			writeS(_macro.descr); // Desc
			writeS(_macro.acronym); // acronym
			writeC(_macro.icon); // icon
			
			writeC(_macro.commands.length); // count
			
			for (int i = 0; i < _macro.commands.length; i++)
			{
				final L2Macro.L2MacroCmd cmd = _macro.commands[i];
				writeC(i + 1); // i of count
				writeC(cmd.type); // type 1 = skill, 3 = action, 4 = shortcut
				writeD(cmd.d1); // skill id
				writeC(cmd.d2); // shortcut id
				writeS(cmd.cmd); // command name
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__E7_SENDMACROLIST;
	}
	
}

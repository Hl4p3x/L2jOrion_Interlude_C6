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

import java.util.List;

import javolution.util.FastList;
import l2jorion.game.templates.L2PcTemplate;

public class CharTemplates extends L2GameServerPacket
{
	private static final String _S__23_CHARTEMPLATES = "[S] 23 CharTemplates";
	
	private final List<L2PcTemplate> _chars = new FastList<>();
	
	public void addChar(final L2PcTemplate template)
	{
		_chars.add(template);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x17);
		writeD(_chars.size());
		
		for (final L2PcTemplate temp : _chars)
		{
			writeD(temp.race.ordinal());
			writeD(temp.classId.getId());
			writeD(0x46);
			writeD(temp.baseSTR);
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.baseDEX);
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.baseCON);
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.baseINT);
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.baseWIT);
			writeD(0x0a);
			writeD(0x46);
			writeD(temp.baseMEN);
			writeD(0x0a);
		}
	}
	
	@Override
	public String getType()
	{
		return _S__23_CHARTEMPLATES;
	}
}

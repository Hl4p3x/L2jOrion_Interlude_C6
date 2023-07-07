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
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class ShortCutInit extends PacketServer
{
	private static final String _S__57_SHORTCUTINIT = "[S] 45 ShortCutInit";
	
	private L2ShortCut[] _shortCuts;
	private L2PcInstance _activeChar;
	
	public ShortCutInit(L2PcInstance activeChar)
	{
		_activeChar = activeChar;
		
		if (_activeChar == null)
		{
			return;
		}
		
		_shortCuts = _activeChar.getAllShortCuts();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x45);
		writeD(_shortCuts.length);
		
		for (final L2ShortCut sc : _shortCuts)
		{
			writeD(sc.getType());
			writeD(sc.getSlot() + sc.getPage() * 12);
			
			switch (sc.getType())
			{
				case L2ShortCut.TYPE_ITEM:
					writeD(sc.getId());
					writeD(0x01);
					writeD(-1);
					writeD(0x00);
					writeD(0x00);
					writeH(0x00);
					writeH(0x00);
					break;
				case L2ShortCut.TYPE_SKILL:
					writeD(sc.getId());
					writeD(sc.getLevel());
					writeC(0x00); // C5
					writeD(0x01); // C6
					break;
				case L2ShortCut.TYPE_ACTION:
				case L2ShortCut.TYPE_MACRO:
				case L2ShortCut.TYPE_RECIPE:
					writeD(sc.getId());
					writeD(0x01);
					break;
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__57_SHORTCUTINIT;
	}
}

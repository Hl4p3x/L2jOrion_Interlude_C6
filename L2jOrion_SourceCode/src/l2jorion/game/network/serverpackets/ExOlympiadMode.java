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

import l2jorion.game.model.actor.instance.L2PcInstance;

/**
 * This class ...
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 * @author godson
 */
public class ExOlympiadMode extends L2GameServerPacket
{
	private static final String _S__FE_2B_OLYMPIADMODE = "[S] FE:2B ExOlympiadMode";
	private final int _mode;
	private final L2PcInstance _activeChar;
	
	/**
	 * @param mode (0 = return, 3 = spectate)
	 * @param player
	 */
	public ExOlympiadMode(int mode, L2PcInstance player)
	{
		_activeChar = player;
		_mode = mode;
	}
	
	@Override
	protected final void writeImpl()
	{
		if (_activeChar == null)
			return;
		
		if (_mode == 3)
			_activeChar.setObserverMode(true);
		
		writeC(0xfe);
		writeH(0x2b);
		writeC(_mode);
	}
	
	@Override
	public String getType()
	{
		return _S__FE_2B_OLYMPIADMODE;
	}
}

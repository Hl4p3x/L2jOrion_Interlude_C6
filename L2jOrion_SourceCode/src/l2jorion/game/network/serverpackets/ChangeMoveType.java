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

import l2jorion.game.model.L2Character;
import l2jorion.game.network.PacketServer;

public class ChangeMoveType extends PacketServer
{
	private static final String _S__3E_CHANGEMOVETYPE = "[S] 3E ChangeMoveType";
	
	public static final int WALK = 0;
	public static final int RUN = 1;
	
	private final int _charObjId;
	private final boolean _running;
	
	public ChangeMoveType(final L2Character character)
	{
		_charObjId = character.getObjectId();
		_running = character.isRunning();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x2e);
		writeD(_charObjId);
		writeD(_running ? RUN : WALK);
		writeD(0); // c2
	}
	
	@Override
	public String getType()
	{
		return _S__3E_CHANGEMOVETYPE;
	}
}

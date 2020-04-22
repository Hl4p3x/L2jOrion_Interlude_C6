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

/**
 * This class ...
 * @version $Revision: 1.3.3 $ $Date: 2009/05/12 19:06:39 $
 */
public class LeaveWorld extends L2GameServerPacket
{
	private static final String _S__96_LEAVEWORLD = "[S] 7e LeaveWorld";
	public static final LeaveWorld STATIC_PACKET = new LeaveWorld();
	
	/*
	 * private LeaveWorld() { //null }
	 */
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x7e);
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__96_LEAVEWORLD;
	}
	
}

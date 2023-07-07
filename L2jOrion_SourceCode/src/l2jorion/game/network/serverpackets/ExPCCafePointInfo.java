/* L2jOrion Project - www.l2jorion.com 
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
import l2jorion.game.network.PacketServer;

public class ExPCCafePointInfo extends PacketServer
{
	private static final String _S__FE_31_EXPCCAFEPOINTINFO = "[S] FE:31 ExPCCafePointInfo";
	
	private final L2PcInstance _character;
	
	private final int m_AddPoint;
	private int m_PeriodType;
	private final int RemainTime;
	private int PointType;
	
	public ExPCCafePointInfo(final L2PcInstance user, final int modify, final boolean add, final int hour, final boolean _double)
	{
		_character = user;
		m_AddPoint = modify;
		
		if (add)
		{
			m_PeriodType = 1;
			PointType = 1;
		}
		else
		{
			if (add && _double)
			{
				m_PeriodType = 1;
				PointType = 0;
			}
			else
			{
				m_PeriodType = 2;
				PointType = 2;
			}
		}
		
		RemainTime = hour;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x31);
		writeD(_character.getPcBangScore());
		writeD(m_AddPoint);
		writeC(m_PeriodType);
		writeD(RemainTime);
		writeC(PointType);
	}
	
	@Override
	public String getType()
	{
		return _S__FE_31_EXPCCAFEPOINTINFO;
	}
}

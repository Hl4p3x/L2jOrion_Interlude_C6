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

import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class CharSelected extends PacketServer
{
	private static final String _S__21_CHARSELECTED = "[S] 15 CharSelected";
	
	private final L2PcInstance _activeChar;
	private final int _sessionId;
	
	public CharSelected(final L2PcInstance cha, final int sessionId)
	{
		_activeChar = cha;
		_sessionId = sessionId;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x15);
		
		writeS(_activeChar.getName());
		writeD(_activeChar.getCharId());
		writeS(_activeChar.getTitle());
		writeD(_sessionId);
		writeD(_activeChar.getClanId());
		
		writeD(0x00);
		
		writeD(_activeChar.getAppearance().getSex() ? 1 : 0);
		writeD(_activeChar.getRace().ordinal());
		writeD(_activeChar.getClassId().getId());
		
		writeD(0x01);
		
		writeD(_activeChar.getX());
		writeD(_activeChar.getY());
		writeD(_activeChar.getZ());
		
		writeF(_activeChar.getCurrentHp());
		writeF(_activeChar.getCurrentMp());
		writeD(_activeChar.getSp());
		writeQ(_activeChar.getExp());
		writeD(_activeChar.getLevel());
		writeD(_activeChar.getKarma());
		writeD(_activeChar.getPkKills());
		writeD(_activeChar.getINT());
		writeD(_activeChar.getSTR());
		writeD(_activeChar.getCON());
		writeD(_activeChar.getMEN());
		writeD(_activeChar.getDEX());
		writeD(_activeChar.getWIT());
		
		for (int i = 0; i < 30; i++)
		{
			writeD(0x00);
		}
		
		writeD(0x00);
		writeD(0x00);
		
		writeD(GameTimeController.getInstance().getGameTime()); // In game time
		
		writeD(0x00);
		
		writeD(_activeChar.getClassId().getId());
		
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
	}
	
	@Override
	public String getType()
	{
		return _S__21_CHARSELECTED;
	}
}
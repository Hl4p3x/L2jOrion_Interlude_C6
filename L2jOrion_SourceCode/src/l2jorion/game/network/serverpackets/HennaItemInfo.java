/*
 * $Header$
 *
 *
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

import l2jorion.game.model.actor.instance.L2HennaInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class HennaItemInfo extends PacketServer
{
	private static final String _S__E3_HennaItemInfo = "[S] E3 HennaItemInfo";
	
	private final L2PcInstance _activeChar;
	private final L2HennaInstance _henna;
	
	public HennaItemInfo(final L2HennaInstance henna, final L2PcInstance player)
	{
		_henna = henna;
		_activeChar = player;
	}
	
	@Override
	protected final void writeImpl()
	{
		
		writeC(0xe3);
		writeD(_henna.getSymbolId()); // symbol Id
		writeD(_henna.getItemIdDye()); // item id of dye
		writeD(_henna.getAmountDyeRequire()); // total amount of dye require
		writeD(_henna.getPrice()); // total amount of aden require to draw symbol
		writeD(1); // able to draw or not 0 is false and 1 is true
		writeD(_activeChar.getAdena());
		
		writeD(_activeChar.getINT()); // current INT
		writeC(_activeChar.getINT() + _henna.getStatINT()); // equip INT
		writeD(_activeChar.getSTR()); // current STR
		writeC(_activeChar.getSTR() + _henna.getStatSTR()); // equip STR
		writeD(_activeChar.getCON()); // current CON
		writeC(_activeChar.getCON() + _henna.getStatCON()); // equip CON
		writeD(_activeChar.getMEN()); // current MEM
		writeC(_activeChar.getMEN() + _henna.getStatMEM()); // equip MEM
		writeD(_activeChar.getDEX()); // current DEX
		writeC(_activeChar.getDEX() + _henna.getStatDEX()); // equip DEX
		writeD(_activeChar.getWIT()); // current WIT
		writeC(_activeChar.getWIT() + _henna.getStatWIT()); // equip WIT
	}
	
	@Override
	public String getType()
	{
		return _S__E3_HennaItemInfo;
	}
}

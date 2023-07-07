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

import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.ClanHall.ClanHallFunction;
import l2jorion.game.network.PacketServer;

public class ClanHallDecoration extends PacketServer
{
	private static final String _S__F7_AGITDECOINFO = "[S] F7 AgitDecoInfo";
	
	private final ClanHall _clanHall;
	private ClanHallFunction _function;
	
	public ClanHallDecoration(final ClanHall ClanHall)
	{
		_clanHall = ClanHall;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xf7);
		writeD(_clanHall.getClanHallId()); // clanhall id
		// FUNC_RESTORE_HP
		_function = _clanHall.getFunction(ClanHall.FUNC_RESTORE_HP);
		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0);
		}
		else if (_clanHall.getGrade() == 0 && _function.getLvl() < 220 || _clanHall.getGrade() == 1 && _function.getLvl() < 160 || _clanHall.getGrade() == 2 && _function.getLvl() < 260 || _clanHall.getGrade() == 3 && _function.getLvl() < 300)
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		
		// FUNC_RESTORE_MP
		_function = _clanHall.getFunction(ClanHall.FUNC_RESTORE_MP);
		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0);
			writeC(0);
		}
		else if ((_clanHall.getGrade() == 0 || _clanHall.getGrade() == 1) && _function.getLvl() < 25 || _clanHall.getGrade() == 2 && _function.getLvl() < 30 || _clanHall.getGrade() == 3 && _function.getLvl() < 40)
		{
			writeC(1);
			writeC(1);
		}
		else
		{
			writeC(2);
			writeC(2);
		}
		
		// FUNC_RESTORE_EXP
		_function = _clanHall.getFunction(ClanHall.FUNC_RESTORE_EXP);
		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0);
		}
		else if (_clanHall.getGrade() == 0 && _function.getLvl() < 25 || _clanHall.getGrade() == 1 && _function.getLvl() < 30 || _clanHall.getGrade() == 2 && _function.getLvl() < 40 || _clanHall.getGrade() == 3 && _function.getLvl() < 50)
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		// FUNC_TELEPORT
		_function = _clanHall.getFunction(ClanHall.FUNC_TELEPORT);
		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0);
		}
		else if (_function.getLvl() < 2)
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		writeC(0);
		
		// CURTAINS
		_function = _clanHall.getFunction(ClanHall.FUNC_DECO_CURTAINS);
		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0);
		}
		else if (_function.getLvl() <= 1)
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		
		// FUNC_ITEM_CREATE
		_function = _clanHall.getFunction(ClanHall.FUNC_ITEM_CREATE);
		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0);
		}
		else if (_clanHall.getGrade() == 0 && _function.getLvl() < 2 || _function.getLvl() < 3)
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		
		// FUNC_SUPPORT
		_function = _clanHall.getFunction(ClanHall.FUNC_SUPPORT);
		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0);
			writeC(0);
		}
		else if (_clanHall.getGrade() == 0 && _function.getLvl() < 2 || _clanHall.getGrade() == 1 && _function.getLvl() < 4 || _clanHall.getGrade() == 2 && _function.getLvl() < 5 || _clanHall.getGrade() == 3 && _function.getLvl() < 8)
		{
			writeC(1);
			writeC(1);
		}
		else
		{
			writeC(2);
			writeC(2);
		}
		// Front Plateform
		_function = _clanHall.getFunction(ClanHall.FUNC_DECO_FRONTPLATEFORM);
		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0);
		}
		else if (_function.getLvl() <= 1)
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		
		// FUNC_ITEM_CREATE
		_function = _clanHall.getFunction(ClanHall.FUNC_ITEM_CREATE);
		if (_function == null || _function.getLvl() == 0)
		{
			writeC(0);
		}
		else if (_clanHall.getGrade() == 0 && _function.getLvl() < 2 || _function.getLvl() < 3)
		{
			writeC(1);
		}
		else
		{
			writeC(2);
		}
		writeD(0);
		writeD(0);
	}
	
	@Override
	public String getType()
	{
		return _S__F7_AGITDECOINFO;
	}
}

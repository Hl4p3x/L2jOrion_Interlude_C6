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

import java.util.ArrayList;

import l2jorion.game.model.L2Object;
import l2jorion.game.network.PacketServer;

public class StatusUpdate extends PacketServer
{
	private static final String _S__1A_STATUSUPDATE = "[S] 0e StatusUpdate";
	
	public static final int LEVEL = 0x01;
	public static final int EXP = 0x02;
	public static final int STR = 0x03;
	public static final int DEX = 0x04;
	public static final int CON = 0x05;
	public static final int INT = 0x06;
	public static final int WIT = 0x07;
	public static final int MEN = 0x08;
	
	public static final int CUR_HP = 0x09;
	public static final int MAX_HP = 0x0a;
	public static final int CUR_MP = 0x0b;
	public static final int MAX_MP = 0x0c;
	
	public static final int SP = 0x0d;
	public static final int CUR_LOAD = 0x0e;
	public static final int MAX_LOAD = 0x0f;
	
	public static final int P_ATK = 0x11;
	public static final int ATK_SPD = 0x12;
	public static final int P_DEF = 0x13;
	public static final int EVASION = 0x14;
	public static final int ACCURACY = 0x15;
	public static final int CRITICAL = 0x16;
	public static final int M_ATK = 0x17;
	public static final int CAST_SPD = 0x18;
	public static final int M_DEF = 0x19;
	public static final int PVP_FLAG = 0x1a;
	public static final int KARMA = 0x1b;
	
	public static final int CUR_CP = 0x21;
	public static final int MAX_CP = 0x22;
	
	private final ArrayList<Attribute> _attributes = new ArrayList<>();
	public int _objectId;
	
	class Attribute
	{
		public int id;
		public int value;
		
		Attribute(final int pId, final int pValue)
		{
			id = pId;
			value = pValue;
		}
	}
	
	public StatusUpdate(L2Object object)
	{
		_objectId = object.getObjectId();
	}
	
	public boolean hasAttributes()
	{
		return !_attributes.isEmpty();
	}
	
	public StatusUpdate(final int objectId)
	{
		_objectId = objectId;
	}
	
	public void addAttribute(int id, int level)
	{
		_attributes.add(new Attribute(id, level));
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x0e);
		
		writeD(_objectId);
		writeD(_attributes.size());
		
		for (Attribute temp : _attributes)
		{
			writeD(temp.id);
			writeD(temp.value);
		}
	}
	
	@Override
	public String getType()
	{
		return _S__1A_STATUSUPDATE;
	}
}

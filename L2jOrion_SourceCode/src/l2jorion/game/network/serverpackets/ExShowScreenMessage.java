/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.network.serverpackets;

import l2jorion.game.network.PacketServer;
import l2jorion.game.network.SystemMessageId;

public class ExShowScreenMessage extends PacketServer
{
	public static enum SMPOS
	{
		DUMMY,
		TOP_LEFT, // 1
		TOP_CENTER, // 2
		TOP_RIGHT, // 3
		MIDDLE_LEFT, // 4
		MIDDLE_CENTER, // 5
		MIDDLE_RIGHT, // 6
		BOTTOM_CENTER, // 7
		BOTTOM_RIGHT, // 8
	}
	
	private final int _type;
	private final int _sysMessageId;
	private final boolean _hide;
	private final int _unk2;
	private final int _unk3;
	private final boolean _fade;
	private final int _size;
	private final int _position;
	private final boolean _effect;
	private final String _text;
	private final int _time;
	
	public ExShowScreenMessage(String text, int time)
	{
		_type = 1;
		_sysMessageId = -1;
		_hide = false;
		_unk2 = 0;
		_unk3 = 0;
		_fade = false;
		_position = 0x02;
		_text = text;
		_time = time;
		_size = 0;
		_effect = false;
	}
	
	public ExShowScreenMessage(String text, int time, SMPOS pos, boolean effect)
	{
		this(text, time, pos.ordinal(), effect);
	}
	
	public ExShowScreenMessage(SystemMessageId systemMsg, int position, int time)
	{
		_type = 1;
		_sysMessageId = systemMsg.getId();
		_hide = false;
		_unk2 = 0;
		_unk3 = 0;
		_fade = false;
		_position = position;
		_text = null;
		_time = time;
		_size = 0;
		_effect = false;
	}
	
	public ExShowScreenMessage(String text, int time, int pos, boolean effect)
	{
		_type = 1;
		_sysMessageId = -1;
		_hide = false;
		_unk2 = 0;
		_unk3 = 0;
		_fade = false;
		_position = pos;
		_text = text;
		_time = time;
		_size = 0;
		_effect = effect;
	}
	
	public ExShowScreenMessage(int type, int messageId, int position, boolean hide, int size, int unk2, int unk3, boolean showEffect, int time, boolean fade, String text)
	{
		_type = type;
		_sysMessageId = messageId;
		_hide = hide;
		_unk2 = unk2;
		_unk3 = unk3;
		_fade = fade;
		_position = position;
		_text = text;
		_time = time;
		_size = size;
		_effect = showEffect;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x38);
		writeD(_type); // 0 - system messages, 1 - your defined text
		writeD(_sysMessageId); // system message id (_type must be 0 otherwise no effect)
		writeD(_position); // message position
		writeD(_hide ? 1 : 0); // hide
		writeD(_size); // font size 0 - normal, 1 - small
		writeD(_unk2); // Font type?
		writeD(_unk3); // font color RGB?
		writeD(_effect ? 1 : 0); // upper effect (0 - disabled, 1 enabled) - _position must be 2 (center) otherwise no effect
		writeD(_time); // time
		writeD(_fade ? 1 : 0); // fade effect (0 - disabled, 1 enabled)
		writeS(_text); // your text (_type must be 1, otherwise no effect)
	}
	
	@Override
	public String getType()
	{
		return "[S]FE:39 ExShowScreenMessage";
	}
}
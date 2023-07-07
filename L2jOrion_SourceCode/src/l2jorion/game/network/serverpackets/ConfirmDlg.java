/*
 * L2jOrion Project - www.l2jorion.com 
 * 
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

import java.util.Vector;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class ConfirmDlg extends PacketServer
{
	private static final String _S__ED_CONFIRMDLG = "[S] ed ConfirmDlg";
	
	private final int _messageId;
	private int _skillLvL = 1;
	private static final int TYPE_ZONE_NAME = 7;
	private static final int TYPE_SKILL_NAME = 4;
	private static final int TYPE_ITEM_NAME = 3;
	private static final int TYPE_NPC_NAME = 2;
	private static final int TYPE_NUMBER = 1;
	private static final int TYPE_TEXT = 0;
	private final Vector<Integer> _types = new Vector<>();
	private final Vector<Object> _values = new Vector<>();
	private int _time = 0;
	private int _requesterId = 0;
	
	public ConfirmDlg(final int messageId)
	{
		_messageId = messageId;
	}
	
	public ConfirmDlg addString(final String text)
	{
		_types.add(Integer.valueOf(TYPE_TEXT));
		_values.add(text);
		return this;
	}
	
	public ConfirmDlg addNumber(final int number)
	{
		_types.add(Integer.valueOf(TYPE_NUMBER));
		_values.add(Integer.valueOf(number));
		return this;
	}
	
	public ConfirmDlg addPcName(L2PcInstance pc)
	{
		return addString(pc.getName());
	}
	
	public ConfirmDlg addNpcName(final int id)
	{
		_types.add(Integer.valueOf(TYPE_NPC_NAME));
		_values.add(Integer.valueOf(1000000 + id));
		return this;
	}
	
	public ConfirmDlg addItemName(final int id)
	{
		_types.add(Integer.valueOf(TYPE_ITEM_NAME));
		_values.add(Integer.valueOf(id));
		return this;
	}
	
	public ConfirmDlg addZoneName(final int x, final int y, final int z)
	{
		_types.add(Integer.valueOf(TYPE_ZONE_NAME));
		final int[] coord =
		{
			x,
			y,
			z
		};
		_values.add(coord);
		return this;
	}
	
	public ConfirmDlg addSkillName(final int id)
	{
		return addSkillName(id, 1);
	}
	
	public ConfirmDlg addSkillName(final int id, final int lvl)
	{
		_types.add(Integer.valueOf(TYPE_SKILL_NAME));
		_values.add(Integer.valueOf(id));
		_skillLvL = lvl;
		return this;
	}
	
	public ConfirmDlg addTime(final int time)
	{
		_time = time;
		return this;
	}
	
	public ConfirmDlg addRequesterId(final int id)
	{
		_requesterId = id;
		return this;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xed);
		writeD(_messageId);
		if (_types != null && _types.size() > 0)
		{
			writeD(_types.size());
			for (int i = 0; i < _types.size(); i++)
			{
				final int t = _types.get(i).intValue();
				writeD(t);
				switch (t)
				{
					case TYPE_TEXT:
					{
						writeS((String) _values.get(i));
						break;
					}
					case TYPE_NUMBER:
					case TYPE_NPC_NAME:
					case TYPE_ITEM_NAME:
					{
						final int t1 = ((Integer) _values.get(i)).intValue();
						writeD(t1);
						break;
					}
					case TYPE_SKILL_NAME:
					{
						final int t1 = ((Integer) _values.get(i)).intValue();
						writeD(t1); // Skill Id
						writeD(_skillLvL); // Skill lvl
						break;
					}
					case TYPE_ZONE_NAME:
					{
						final int t1 = ((int[]) _values.get(i))[0];
						final int t2 = ((int[]) _values.get(i))[1];
						final int t3 = ((int[]) _values.get(i))[2];
						writeD(t1);
						writeD(t2);
						writeD(t3);
						break;
					}
				}
			}
			// timed dialog (Summon Friend skill request)
			if (_time != 0)
			{
				writeD(_time);
			}
			
			if (_requesterId != 0)
			{
				writeD(_requesterId);
			}
			
			if (_time > 0)
			{
				getClient().getActiveChar().addConfirmDlgRequestTime(_requesterId, _time);
			}
		}
		else
		{
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
		}
	}
	
	@Override
	public String getType()
	{
		return _S__ED_CONFIRMDLG;
	}
}
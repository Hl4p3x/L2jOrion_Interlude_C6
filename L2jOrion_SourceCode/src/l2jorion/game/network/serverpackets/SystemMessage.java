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

import java.util.Vector;

import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.templates.L2NpcTemplate;

public final class SystemMessage extends PacketServer
{
	private static final int TYPE_ZONE_NAME = 7;
	private static final int TYPE_SKILL_NAME = 4;
	private static final int TYPE_ITEM_NAME = 3;
	private static final int TYPE_NPC_NAME = 2;
	private static final int TYPE_NUMBER = 1;
	private static final byte TYPE_TEXT = 0;
	private int _messageId;
	private Vector<Integer> _types = new Vector<>();
	private Vector<Object> _values = new Vector<>();
	private int _skillLvL = 1;
	
	public SystemMessage(SystemMessageId messageId)
	{
		if (Config.DEBUG && messageId == SystemMessageId.TARGET_IS_INCORRECT)
		{
			Thread.dumpStack();
		}
		_messageId = messageId.getId();
	}
	
	public SystemMessage(int messageId)
	{
		_messageId = messageId;
	}
	
	public static SystemMessage sendString(String msg)
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		sm.addString(msg);
		
		return sm;
	}
	
	public SystemMessage addString(String text)
	{
		_types.add(Integer.valueOf(TYPE_TEXT));
		_values.add(text);
		return this;
	}
	
	public SystemMessage addNumber(int number)
	{
		_types.add(Integer.valueOf(TYPE_NUMBER));
		_values.add((number));
		return this;
	}
	
	public SystemMessage addNpcName(int id)
	{
		_types.add(Integer.valueOf(TYPE_NPC_NAME));
		_values.add(Integer.valueOf(1000000 + id));
		
		return this;
	}
	
	public SystemMessage addCharName(L2Character cha)
	{
		if (cha instanceof L2NpcInstance)
		{
			if (((L2NpcInstance) cha).getTemplate().serverSideName)
			{
				return addString(((L2NpcInstance) cha).getTemplate().name);
			}
			return addNpcName((L2NpcInstance) cha);
		}
		
		if (cha instanceof L2PcInstance)
		{
			return addPcName((L2PcInstance) cha);
		}
		
		if (cha instanceof L2Summon)
		{
			if (((L2Summon) cha).getTemplate().serverSideName)
			{
				return addString(((L2Summon) cha).getTemplate().name);
			}
			return addNpcName((L2Summon) cha);
		}
		
		return addString(cha.getName());
	}
	
	public SystemMessage addPcName(L2PcInstance pc)
	{
		return addString(pc.getName());
	}
	
	public SystemMessage addNpcName(L2NpcInstance npc)
	{
		return addNpcName(npc.getTemplate());
	}
	
	public SystemMessage addNpcName(L2Summon npc)
	{
		return addNpcName(npc.getNpcId());
	}
	
	public SystemMessage addNpcName(L2NpcTemplate tpl)
	{
		if (tpl.isCustom())
		{
			return addString(tpl.name);
		}
		return addNpcName(tpl.npcId);
	}
	
	public SystemMessage addItemName(L2ItemInstance item)
	{
		return addItemName(item.getItemId());
	}
	
	public SystemMessage addItemName(int id)
	{
		_types.add(Integer.valueOf(TYPE_ITEM_NAME));
		_values.add(Integer.valueOf(id));
		
		return this;
	}
	
	public SystemMessage addZoneName(int x, int y, int z)
	{
		_types.add(Integer.valueOf(TYPE_ZONE_NAME));
		int[] coord =
		{
			x,
			y,
			z
		};
		_values.add(coord);
		
		return this;
	}
	
	public SystemMessage ZoneName(int x, int y, int z)
	{
		_types.add(Integer.valueOf(TYPE_ZONE_NAME));
		int[] coord =
		{
			x,
			y,
			z
		};
		_values.add(coord);
		
		return this;
	}
	
	public SystemMessage addSkillName(int id)
	{
		return addSkillName(id, 1);
	}
	
	public SystemMessage addSkillName(int id, int lvl)
	{
		_types.add(Integer.valueOf(TYPE_SKILL_NAME));
		_values.add(Integer.valueOf(id));
		_skillLvL = lvl;
		
		return this;
	}
	
	public final SystemMessage addSkillName(final L2Skill skill)
	{
		if (skill.getId() != skill.getDisplayId())
		{
			return addString(skill.getName());
		}
		return addSkillName(skill.getId(), skill.getLevel());
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x64);
		
		writeD(_messageId);
		writeD(_types.size());
		
		for (int i = 0; i < _types.size(); i++)
		{
			int t = _types.get(i).intValue();
			
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
					int t1 = ((Integer) _values.get(i)).intValue();
					writeD(t1);
					break;
				}
				case TYPE_SKILL_NAME:
				{
					int t1 = ((Integer) _values.get(i)).intValue();
					writeD(t1); // Skill Id
					writeD(_skillLvL); // Skill lvl
					break;
				}
				case TYPE_ZONE_NAME:
				{
					int t1 = ((int[]) _values.get(i))[0];
					int t2 = ((int[]) _values.get(i))[1];
					int t3 = ((int[]) _values.get(i))[2];
					writeD(t1);
					writeD(t2);
					writeD(t3);
					break;
				}
			}
		}
	}
	
	public int getMessageID()
	{
		return _messageId;
	}
	
	public static SystemMessage getSystemMessage(SystemMessageId smId)
	{
		SystemMessage sm = new SystemMessage(smId);
		return sm;
	}
	
	@Override
	public String getType()
	{
		return "[S] 64 SystemMessage";
	}
}
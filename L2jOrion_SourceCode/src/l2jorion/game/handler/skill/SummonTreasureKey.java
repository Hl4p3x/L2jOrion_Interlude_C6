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
package l2jorion.game.handler.skill;

import l2jorion.Config;
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class SummonTreasureKey implements ISkillHandler
{
	static Logger LOG = LoggerFactory.getLogger(SummonTreasureKey.class);
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.SUMMON_TREASURE_KEY
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance player = (L2PcInstance) activeChar;
		
		try
		{
			int item_id = 0;
			
			switch (skill.getLevel())
			{
				case 1:
				{
					item_id = Rnd.get(6667, 6669);
					break;
				}
				case 2:
				{
					item_id = Rnd.get(6668, 6670);
					break;
				}
				case 3:
				{
					item_id = Rnd.get(6669, 6671);
					break;
				}
				case 4:
				{
					item_id = Rnd.get(6670, 6672);
					break;
				}
			}
			int count = Rnd.get(2, 3);
			player.addItem("Skill", item_id, count, player, false);
			SystemMessage sm;
			if (item_id > 1)
			{
				sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(item_id);
				sm.addNumber(count);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
				sm.addItemName(item_id);
			}
			player.sendPacket(sm);
			sm = null;
			
			player = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("Error using skill summon Treasure Key:" + e);
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
	
}

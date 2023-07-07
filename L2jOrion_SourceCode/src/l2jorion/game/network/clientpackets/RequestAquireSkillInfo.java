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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.SkillSpellbookTable;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.model.L2PledgeSkillLearn;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2SkillLearn;
import l2jorion.game.model.actor.instance.L2FolkInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.AquireSkillInfo;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class RequestAquireSkillInfo extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(RequestAquireSkillInfo.class);
	
	private int _id;
	private int _level;
	private int _skillType;
	
	@Override
	protected void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		final L2FolkInstance trainer = activeChar.getLastFolkNPC();
		if (trainer == null)
		{
			return;
		}
		
		if (!activeChar.isGM() && !activeChar.isInsideRadius(trainer, L2NpcInstance.INTERACTION_DISTANCE, false, false))
		{
			return;
		}
		
		boolean canteach = false;
		final L2Skill skill = SkillTable.getInstance().getInfo(_id, _level);
		if (skill == null)
		{
			if (Config.DEBUG)
			{
				LOG.warn("skill id " + _id + " level " + _level + " is undefined. aquireSkillInfo failed.");
			}
			return;
		}
		
		if (_skillType == 0)
		{
			if (!trainer.getTemplate().canTeach(activeChar.getSkillLearningClassId()))
				return; // cheater
				
			final L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(activeChar, activeChar.getSkillLearningClassId());
			
			for (final L2SkillLearn s : skills)
			{
				if (s.getId() == _id && s.getLevel() == _level)
				{
					canteach = true;
					break;
				}
			}
			
			if (!canteach)
				return; // cheater
				
			final int requiredSp = SkillTreeTable.getInstance().getSkillCost(activeChar, skill);
			final AquireSkillInfo asi = new AquireSkillInfo(skill.getId(), skill.getLevel(), requiredSp, 0);
			
			int spbId = -1;
			if (Config.DIVINE_SP_BOOK_NEEDED && skill.getId() == L2Skill.SKILL_DIVINE_INSPIRATION)
			{
				spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill, _level);
			}
			else if (Config.SP_BOOK_NEEDED && skill.getLevel() == 1)
			{
				spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill);
			}
			
			if (spbId > -1)
			{
				asi.addRequirement(99, spbId, 1, 50);
			}
			
			sendPacket(asi);
		}
		else if (_skillType == 2)
		{
			int requiredRep = 0;
			int itemId = 0;
			final L2PledgeSkillLearn[] skills = SkillTreeTable.getInstance().getAvailablePledgeSkills(activeChar);
			
			for (final L2PledgeSkillLearn s : skills)
			{
				if (s.getId() == _id && s.getLevel() == _level)
				{
					canteach = true;
					requiredRep = s.getRepCost();
					itemId = s.getItemId();
					break;
				}
			}
			
			if (!canteach)
				return; // cheater
				
			final AquireSkillInfo asi = new AquireSkillInfo(skill.getId(), skill.getLevel(), requiredRep, 2);
			
			if (Config.LIFE_CRYSTAL_NEEDED)
			{
				asi.addRequirement(1, itemId, 1, 0);
			}
			
			sendPacket(asi);
		}
		else
		// Common Skills
		{
			int costid = 0;
			int costcount = 0;
			int spcost = 0;
			
			final L2SkillLearn[] skillsc = SkillTreeTable.getInstance().getAvailableSkills(activeChar);
			
			for (final L2SkillLearn s : skillsc)
			{
				final L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				
				if (sk == null || sk != skill)
				{
					continue;
				}
				
				canteach = true;
				costid = s.getIdCost();
				costcount = s.getCostCount();
				spcost = s.getSpCost();
			}
			
			final AquireSkillInfo asi = new AquireSkillInfo(skill.getId(), skill.getLevel(), spcost, 1);
			asi.addRequirement(4, costid, costcount, 0);
			sendPacket(asi);
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 6B RequestAquireSkillInfo";
	}
}

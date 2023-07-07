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
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.model.L2EnchantSkillLearn;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2FolkInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.ExEnchantSkillInfo;

public final class RequestExEnchantSkillInfo extends PacketClient
{
	private int _skillId;
	private int _skillLvl;
	
	@Override
	protected void readImpl()
	{
		_skillId = readD();
		_skillLvl = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (_skillId <= 0 || _skillLvl <= 0)
		{
			return;
		}
		
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.getLevel() < 76)
		{
			return;
		}
		
		L2FolkInstance trainer = null;
		
		if (!activeChar.hasTempAccess())
		{
			trainer = activeChar.getLastFolkNPC();
			if (trainer == null)
			{
				return;
			}
			
			if (!activeChar.isInsideRadius(trainer, L2NpcInstance.INTERACTION_DISTANCE, false, false))
			{
				return;
			}
		}
		
		boolean canteach = false;
		
		final L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);
		if (skill == null || skill.getId() != _skillId)
		{
			return;
		}
		
		if (!activeChar.hasTempAccess() && trainer != null)
		{
			if (!trainer.getTemplate().canTeach(activeChar.getClassId()))
			{
				return; // cheater
			}
		}
		
		final L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(activeChar);
		
		for (final L2EnchantSkillLearn s : skills)
		{
			if (s.getId() == _skillId && s.getLevel() == _skillLvl)
			{
				canteach = true;
				break;
			}
		}
		
		if (!canteach)
		{
			return; // cheater
		}
		
		final int requiredSp = SkillTreeTable.getInstance().getSkillSpCost(activeChar, skill);
		final int requiredExp = SkillTreeTable.getInstance().getSkillExpCost(activeChar, skill);
		final byte rate = SkillTreeTable.getInstance().getSkillRate(activeChar, skill);
		final ExEnchantSkillInfo asi = new ExEnchantSkillInfo(skill.getId(), skill.getLevel(), requiredSp, requiredExp, rate);
		
		if (Config.ES_SP_BOOK_NEEDED && (skill.getLevel() == 101 || skill.getLevel() == 141)) // only first lvl requires book
		{
			final int spbId = 6622;
			asi.addRequirement(4, spbId, 1, 0);
		}
		sendPacket(asi);
		
	}
	
	@Override
	public String getType()
	{
		return "[C] D0:06 RequestExEnchantSkillInfo";
	}
	
}

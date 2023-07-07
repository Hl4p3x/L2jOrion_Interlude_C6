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
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.enums.AchType;
import l2jorion.game.model.L2EnchantSkillLearn;
import l2jorion.game.model.L2ShortCut;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2FolkInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ShortCutRegister;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.game.powerpack.shop.Shop;
import l2jorion.game.util.IllegalPlayerAction;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public final class RequestExEnchantSkill extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(RequestAquireSkill.class);
	
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
		final L2PcInstance player = getClient().getActiveChar();
		
		if (player == null)
		{
			return;
		}
		
		L2FolkInstance trainer = null;
		int npcid = 0;
		
		if (!player.hasTempAccess())
		{
			trainer = player.getLastFolkNPC();
			if (trainer == null)
			{
				return;
			}
			
			npcid = trainer.getNpcId();
			
			if (!player.isInsideRadius(trainer, L2NpcInstance.INTERACTION_DISTANCE, false, false))
			{
				return;
			}
		}
		
		if (player.getSkillLevel(_skillId) >= _skillLvl)
		{
			return;
		}
		
		if (player.getClassId().getId() < 88)
		{
			return;
		}
		
		if (player.getLevel() < 76)
		{
			return;
		}
		
		final L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);
		
		int counts = 0;
		int _requiredSp = 10000000;
		int _requiredExp = 100000;
		byte _rate = 0;
		// int _baseLvl = 1;
		
		final L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(player);
		
		for (final L2EnchantSkillLearn s : skills)
		{
			final L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			
			if (sk == null || sk != skill || !sk.getCanLearn(player.getClassId()) || !player.hasTempAccess() && !sk.canTeachBy(npcid))
			{
				continue;
			}
			
			counts++;
			
			_requiredSp = s.getSpCost();
			_requiredExp = s.getExp();
			_rate = s.getRate(player);
			// _baseLvl = s.getBaseLevel();
		}
		
		if (counts == 0 && !Config.ALT_GAME_SKILL_LEARN)
		{
			player.sendMessage("You are trying to learn skill that u can't.");
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!", IllegalPlayerAction.PUNISH_KICK);
			return;
		}
		
		if (player.getSp() >= _requiredSp)
		{
			if (!getClient().getFloodProtectors().getUseAugItem().tryPerformAction("use skill enchanter"))
			{
				LOG.info(player.getName() + " tried flood on SKILL enchanter.");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Like L2OFF you can't delevel during skill enchant
			final long expAfter = player.getExp() - _requiredExp;
			if (player.getExp() >= _requiredExp && expAfter >= ExperienceData.getInstance().getExpForLevel(player.getLevel()))
			{
				if (Config.ES_SP_BOOK_NEEDED && (_skillLvl == 101 || _skillLvl == 141)) // only first lvl requires book
				{
					final int spbId = 6622;
					
					final L2ItemInstance spb = player.getInventory().getItemByItemId(spbId);
					
					if (spb == null)// Haven't spellbook
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL));
						return;
					}
					
					if (Config.SCROLL_STACKABLE)
					{
						player.destroyItem("Consume", spb.getObjectId(), 1, null, true);
					}
					else
					{
						player.destroyItem("Consume", spb, null, true);
					}
				}
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DONT_HAVE_ENOUGH_EXP_TO_ENCHANT_THAT_SKILL);
				player.sendPacket(sm);
				return;
			}
		}
		else
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DONT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL);
			player.sendPacket(sm);
			return;
		}
		
		if (Rnd.get(100) <= _rate)
		{
			player.addSkill(skill, true);
			
			player.getStat().removeExpAndSp(_requiredExp, _requiredSp);
			
			final StatusUpdate su = new StatusUpdate(player.getObjectId());
			su.addAttribute(StatusUpdate.SP, player.getSp());
			player.sendPacket(su);
			
			final SystemMessage ep = new SystemMessage(SystemMessageId.EXP_DECREASED_BY_S1);
			ep.addNumber(_requiredExp);
			sendPacket(ep);
			
			final SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
			sp.addNumber(_requiredSp);
			sendPacket(sp);
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_ENCHANTING_THE_SKILL_S1);
			sm.addSkillName(_skillId);
			player.sendPacket(sm);
			
			int enchantValue = _skillLvl >= 130 ? _skillLvl - 140 : _skillLvl - 100;
			
			if (player.getAchievement().getCount(AchType.ENCHANT_SKILL) < enchantValue)
			{
				player.getAchievement().increase(AchType.ENCHANT_SKILL, enchantValue, false, false, false, 0);
			}
		}
		else
		{
			if (skill.getLevel() > 100)
			{
				_skillLvl = SkillTable.getInstance().getMaxLevel(_skillId);
				player.addSkill(SkillTable.getInstance().getInfo(_skillId, _skillLvl), true);
			}
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_ENCHANT_THE_SKILL_S1);
			sm.addSkillName(_skillId);
			player.sendPacket(sm);
		}
		
		if (!player.hasTempAccess() && trainer != null)
		{
			trainer.showEnchantSkillList(player, player.getClassId());
		}
		else
		{
			Shop.showEnchantSkillList(player);
		}
		
		player.sendPacket(new UserInfo(player));
		player.sendSkillList();
		
		// update all the shortcuts to this skill
		final L2ShortCut[] allShortCuts = player.getAllShortCuts();
		
		for (final L2ShortCut sc : allShortCuts)
		{
			if (sc.getId() == _skillId && sc.getType() == L2ShortCut.TYPE_SKILL)
			{
				final L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), _skillLvl, 1);
				player.sendPacket(new ShortCutRegister(newsc));
				player.registerShortCut(newsc);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] D0:07 RequestExEnchantSkill";
	}
}

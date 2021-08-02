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
package l2jorion.game.model.actor.instance;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.model.L2EnchantSkillLearn;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2SkillLearn;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.AquireSkillList;
import l2jorion.game.network.serverpackets.ExEnchantSkillList;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;

public class L2FolkInstance extends L2NpcInstance
{
	private final ClassId[] _classesToTeach;
	
	public L2FolkInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		_classesToTeach = template.getTeachInfo();
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		player.setLastFolkNPC(this);
		super.onAction(player);
	}
	
	public void showSkillList(final L2PcInstance player, final ClassId classId)
	{
		final int npcId = getTemplate().npcId;
		
		if (_classesToTeach == null)
		{
			if (player.isGM())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				TextBuilder sb = new TextBuilder();
				sb.append("<html><body>");
				sb.append("I cannot teach you. My class list is empty.<br> Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:" + npcId + ", Your classId:" + player.getClassId().getId() + "<br>");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
				return;
			}
			
			player.sendMessage("Error learning skills, contact the admin.");
			return;
		}
		
		if (!getTemplate().canTeach(classId))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("I cannot teach you any skills.<br> You must find your current class teachers.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return;
		}
		
		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, classId);
		AquireSkillList asl = new AquireSkillList(AquireSkillList.skillType.Usual);
		int counts = 0;
		
		for (final L2SkillLearn s : skills)
		{
			final L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			
			if (sk == null || !sk.getCanLearn(player.getClassId()) || !sk.canTeachBy(npcId))
			{
				continue;
			}
			
			final int cost = SkillTreeTable.getInstance().getSkillCost(player, sk);
			counts++;
			
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}
		
		if (counts == 0)
		{
			final int minlevel = SkillTreeTable.getInstance().getMinLevelForNewSkill(player, classId);
			
			if (minlevel > 0)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN);
				sm.addNumber(minlevel);
				player.sendPacket(sm);
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
				player.sendPacket(sm);
			}
		}
		else
		{
			player.sendPacket(asl);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void showEnchantSkillList(final L2PcInstance player, final ClassId classId)
	{
		final int npcId = getTemplate().npcId;
		
		if (_classesToTeach == null)
		{
			if (player.isGM())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				TextBuilder sb = new TextBuilder();
				sb.append("<html><body>");
				sb.append("I cannot teach you. My class list is empty.<br> Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:" + npcId + ", Your classId:" + player.getClassId().getId() + "<br>");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
				return;
			}
			
			player.sendMessage("Error learning skills, contact the admin.");
			return;
		}
		
		if (!getTemplate().canTeach(classId))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("I cannot teach you any skills.<br> You must find your current class teachers.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return;
		}
		if (player.getClassId().getId() < 88)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("You must have 3rd class change quest completed.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return;
		}
		
		L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(player);
		ExEnchantSkillList esl = new ExEnchantSkillList();
		int counts = 0;
		
		for (final L2EnchantSkillLearn s : skills)
		{
			final L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if (sk == null)
			{
				continue;
			}
			counts++;
			esl.addSkill(s.getId(), s.getLevel(), s.getSpCost(), s.getExp());
		}
		
		if (counts == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT));
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			final int level = player.getLevel();
			
			if (level < 74)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN);
				sm.addNumber(level);
				player.sendPacket(sm);
			}
			else
			{
				TextBuilder sb = new TextBuilder();
				sb.append("<html><body>");
				sb.append("You've learned all skills for your class.<br>");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
			}
		}
		else
		{
			player.sendPacket(esl);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		if (command.startsWith("SkillList"))
		{
			if (Config.ALT_GAME_SKILL_LEARN)
			{
				final String id = command.substring(9).trim();
				
				if (id.length() != 0)
				{
					player.setSkillLearningClassId(ClassId.values()[Integer.parseInt(id)]);
					showSkillList(player, ClassId.values()[Integer.parseInt(id)]);
				}
				else
				{
					boolean own_class = false;
					
					for (final ClassId cid : _classesToTeach)
					{
						if (cid.equalsOrChildOf(player.getClassId()))
						{
							own_class = true;
							break;
						}
					}
					
					String text = "<html><body><center>Skill learning:</center><br>";
					
					if (!own_class)
					{
						final String mages = player.getClassId().isMage() ? "fighters" : "mages";
						text += "Skills of your class are the easiest to learn.<br>" + "Skills of another class are harder.<br>" + "Skills for another race are even more hard to learn.<br>" + "You can also learn skills of " + mages + ", and they are" + " the hardest to learn!<br>" + "<br>";
					}
					
					// make a list of classes
					if (_classesToTeach.length != 0)
					{
						int count = 0;
						ClassId classCheck = player.getClassId();
						
						while (count == 0 && classCheck != null)
						{
							for (final ClassId cid : _classesToTeach)
							{
								if (cid.level() != classCheck.level())
								{
									continue;
								}
								
								if (SkillTreeTable.getInstance().getAvailableSkills(player, cid).length == 0)
								{
									continue;
								}
								
								text += "<a action=\"bypass -h npc_%objectId%_SkillList " + cid.getId() + "\">Learn " + cid + "'s class Skills</a><br>\n";
								count++;
							}
							classCheck = classCheck.getParent();
						}
						classCheck = null;
					}
					else
					{
						text += "No Skills.<br>";
					}
					
					text += "</body></html>";
					
					insertObjectIdAndShowChatWindow(player, text);
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			else
			{
				player.setSkillLearningClassId(player.getClassId());
				showSkillList(player, player.getClassId());
			}
		}
		else if (command.startsWith("EnchantSkillList"))
		{
			showEnchantSkillList(player, player.getClassId());
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}
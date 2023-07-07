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
package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2SkillLearn;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PledgeSkillList;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class AdminSkill implements IAdminCommandHandler
{
	private static Logger LOG = LoggerFactory.getLogger(AdminSkill.class);
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_show_skills",
		"admin_remove_skills",
		"admin_skill_list",
		"admin_skill_index",
		"admin_add_skill",
		"admin_remove_skill",
		"admin_get_skills",
		"admin_reset_skills",
		"admin_give_all_skills",
		"admin_remove_all_skills",
		"admin_add_clan_skill"
	};
	
	private static L2Skill[] adminSkills;
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (command.equals("admin_show_skills"))
		{
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_remove_skills"))
		{
			try
			{
				String val = command.substring(20);
				removeSkillsPage(activeChar, Integer.parseInt(val));
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		else if (command.startsWith("admin_skill_list"))
		{
			AdminHelpPage.showHelpPage(activeChar, "skills.htm");
		}
		else if (command.startsWith("admin_skill_index"))
		{
			try
			{
				String val = command.substring(18);
				AdminHelpPage.showHelpPage(activeChar, "skills/" + val + ".htm");
				val = null;
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		else if (command.startsWith("admin_add_skill"))
		{
			try
			{
				String val = command.substring(15);
				
				if (activeChar == activeChar.getTarget() || activeChar.getAccessLevel().isGm())
				{
					adminAddSkill(activeChar, val);
				}
				
				val = null;
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Usage: //add_skill <skill_id> <level>");
			}
		}
		else if (command.startsWith("admin_remove_skill"))
		{
			try
			{
				String id = command.substring(19);
				
				final int idval = Integer.parseInt(id);
				
				if (activeChar == activeChar.getTarget() || activeChar.getAccessLevel().isGm())
				{
					adminRemoveSkill(activeChar, idval);
				}
				
				id = null;
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Usage: //remove_skill <skill_id>");
			}
		}
		else if (command.equals("admin_get_skills"))
		{
			adminGetSkills(activeChar);
		}
		else if (command.equals("admin_reset_skills"))
		{
			if (activeChar == activeChar.getTarget() || activeChar.getAccessLevel().isGm())
			{
				adminResetSkills(activeChar);
			}
		}
		else if (command.equals("admin_give_all_skills"))
		{
			if (activeChar == activeChar.getTarget() || activeChar.getAccessLevel().isGm())
			{
				adminGiveAllSkills(activeChar);
			}
		}
		
		else if (command.equals("admin_remove_all_skills"))
		{
			if (activeChar.getTarget() instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) activeChar.getTarget();
				
				for (final L2Skill skill : player.getAllSkills())
				{
					player.removeSkill(skill);
				}
				
				activeChar.sendMessage("You removed all skills from " + player.getName());
				player.sendMessage("Admin removed all skills from you.");
				player.sendSkillList();
				player = null;
			}
		}
		else if (command.startsWith("admin_add_clan_skill"))
		{
			try
			{
				String[] val = command.split(" ");
				
				if (activeChar == activeChar.getTarget() || activeChar.getAccessLevel().isGm())
				{
					adminAddClanSkill(activeChar, Integer.parseInt(val[1]), Integer.parseInt(val[2]));
				}
				
				val = null;
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Usage: //add_clan_skill <skill_id> <level>");
			}
		}
		return true;
	}
	
	/**
	 * This function will give all the skills that the target can learn at his/her level
	 * @param activeChar the gm char
	 */
	private void adminGiveAllSkills(final L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		
		boolean countUnlearnable = true;
		int unLearnable = 0;
		int skillCounter = 0;
		
		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, player.getClassId());
		
		while (skills.length > unLearnable)
		{
			for (final L2SkillLearn s : skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				
				if (sk == null || !sk.getCanLearn(player.getClassId()))
				{
					if (countUnlearnable)
					{
						unLearnable++;
					}
					
					continue;
				}
				
				if (player.getSkillLevel(sk.getId()) == -1)
				{
					skillCounter++;
				}
				
				player.addSkill(sk, true);
				
				sk = null;
			}
			
			countUnlearnable = false;
			skills = SkillTreeTable.getInstance().getAvailableSkills(player, player.getClassId());
		}
		
		// Notify player and admin
		player.sendMessage("A GM gave you " + skillCounter + " skills.");
		activeChar.sendMessage("You gave " + skillCounter + " skills to " + player.getName());
		player.sendSkillList();
		
		skills = null;
		player = null;
		target = null;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void removeSkillsPage(final L2PcInstance activeChar, int page)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;
		}
		
		L2Skill[] skills = player.getAllSkills();
		
		final int MaxSkillsPerPage = 10;
		int MaxPages = skills.length / MaxSkillsPerPage;
		
		if (skills.length > MaxSkillsPerPage * MaxPages)
		{
			MaxPages++;
		}
		
		if (page > MaxPages)
		{
			page = MaxPages;
		}
		
		final int SkillsStart = MaxSkillsPerPage * page;
		int SkillsEnd = skills.length;
		
		if (SkillsEnd - SkillsStart > MaxSkillsPerPage)
		{
			SkillsEnd = SkillsStart + MaxSkillsPerPage;
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_show_skills\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<center>Editing <font color=\"LEVEL\">" + player.getName() + "</font></center>");
		replyMSG.append("<br><table width=270><tr><td>Lv: " + player.getLevel() + " " + player.getTemplate().className + "</td></tr></table>");
		replyMSG.append("<br><table width=270><tr><td>Note: Dont forget that modifying players skills can</td></tr>");
		replyMSG.append("<tr><td>ruin the game...</td></tr></table>");
		replyMSG.append("<br><center>Click on the skill you wish to remove:</center>");
		replyMSG.append("<br>");
		String pages = "<center><table width=270><tr>";
		
		for (int x = 0; x < MaxPages; x++)
		{
			final int pagenr = x + 1;
			pages += "<td><a action=\"bypass -h admin_remove_skills " + x + "\">Page " + pagenr + "</a></td>";
		}
		
		pages += "</tr></table></center>";
		replyMSG.append(pages);
		replyMSG.append("<br><table width=270>");
		replyMSG.append("<tr><td width=80>Name:</td><td width=60>Level:</td><td width=40>Id:</td></tr>");
		
		for (int i = SkillsStart; i < SkillsEnd; i++)
		{
			replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_remove_skill " + skills[i].getId() + "\">" + skills[i].getName() + "</a></td><td width=60>" + skills[i].getLevel() + "</td><td width=40>" + skills[i].getId() + "</td></tr>");
		}
		
		replyMSG.append("</table>");
		replyMSG.append("<br><center><table>");
		replyMSG.append("Remove skill by ID :");
		replyMSG.append("<tr><td>Id: </td>");
		replyMSG.append("<td><edit var=\"id_to_remove\" width=110></td></tr>");
		replyMSG.append("</table></center>");
		replyMSG.append("<center><button value=\"Remove skill\" action=\"bypass -h admin_remove_skill $id_to_remove\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<br><center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15></center>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void showMainPage(final L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/charskills.htm");
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%class%", player.getTemplate().className);
		activeChar.sendPacket(adminReply);
		
		adminReply = null;
		player = null;
		target = null;
	}
	
	private void adminGetSkills(final L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		
		if (player.getName().equals(activeChar.getName()))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ON_YOURSELF));
		}
		else
		{
			L2Skill[] skills = player.getAllSkills();
			adminSkills = activeChar.getAllSkills();
			
			for (final L2Skill adminSkill : adminSkills)
			{
				activeChar.removeSkill(adminSkill);
			}
			
			for (final L2Skill skill : skills)
			{
				activeChar.addSkill(skill, true);
			}
			
			activeChar.sendMessage("You now have all the skills of " + player.getName() + ".");
			activeChar.sendSkillList();
			
			skills = null;
		}
		
		showMainPage(activeChar);
		
		target = null;
		player = null;
	}
	
	private void adminResetSkills(final L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		
		if (adminSkills == null)
		{
			activeChar.sendMessage("You must get the skills of someone in order to do this.");
		}
		else
		{
			L2Skill[] skills = player.getAllSkills();
			
			for (final L2Skill skill : skills)
			{
				player.removeSkill(skill);
			}
			
			for (int i = 0; i < activeChar.getAllSkills().length; i++)
			{
				player.addSkill(activeChar.getAllSkills()[i], true);
			}
			
			for (final L2Skill skill : skills)
			{
				activeChar.removeSkill(skill);
			}
			
			for (final L2Skill adminSkill : adminSkills)
			{
				activeChar.addSkill(adminSkill, true);
			}
			
			player.sendMessage("[GM]" + activeChar.getName() + " updated your skills.");
			activeChar.sendMessage("You now have all your skills back.");
			adminSkills = null;
			activeChar.sendSkillList();
			
			skills = null;
		}
		
		showMainPage(activeChar);
		
		player = null;
		target = null;
	}
	
	private void adminAddSkill(final L2PcInstance activeChar, final String val)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			if (target == activeChar || (target != activeChar && activeChar.getAccessLevel().getLevel() < 3))
			{
				player = (L2PcInstance) target;
			}
			else
			{
				showMainPage(activeChar);
				activeChar.sendPacket(SystemMessage.sendString("You have not right to add skills to other players"));
				return;
			}
		}
		else
		{
			showMainPage(activeChar);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		
		StringTokenizer st = new StringTokenizer(val);
		
		if (st.countTokens() != 2)
		{
			showMainPage(activeChar);
		}
		else
		{
			L2Skill skill = null;
			
			try
			{
				String id = st.nextToken();
				String level = st.nextToken();
				
				final int idval = Integer.parseInt(id);
				final int levelval = Integer.parseInt(level);
				
				skill = SkillTable.getInstance().getInfo(idval, levelval);
				
				level = null;
				id = null;
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
			
			if (skill != null)
			{
				String name = skill.getName();
				player.sendMessage("Admin gave you the skill: " + name + ".");
				player.addSkill(skill, true);
				player.sendSkillList();
				
				// Admin information
				activeChar.sendMessage("You gave the skill " + name + " to " + player.getName() + ".");
				
				if (Config.DEBUG)
				{
					LOG.debug("[GM]" + activeChar.getName() + " gave skill " + name + " to " + player.getName() + ".");
				}
				
				activeChar.sendSkillList();
			}
			else
			{
				activeChar.sendMessage("Error: there is no such skill.");
			}
			
			showMainPage(activeChar); // Back to start
		}
	}
	
	private void adminRemoveSkill(final L2PcInstance activeChar, final int idval)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		
		L2Skill skill = SkillTable.getInstance().getInfo(idval, player.getSkillLevel(idval));
		
		if (skill != null)
		{
			final String skillname = skill.getName();
			player.sendMessage("Admin removed the skill " + skillname + " from your skills list.");
			player.removeSkill(skill);
			// Admin information
			activeChar.sendMessage("You removed the skill " + skillname + " from " + player.getName() + ".");
			
			if (Config.DEBUG)
			{
				LOG.debug("[GM]" + activeChar.getName() + " removed skill " + skillname + " from " + player.getName() + ".");
			}
			
			activeChar.sendSkillList();
		}
		else
		{
			activeChar.sendMessage("Error: there is no such skill.");
		}
		
		// Back to previous page
		removeSkillsPage(activeChar, 0);
		
		skill = null;
		player = null;
		target = null;
	}
	
	private void adminAddClanSkill(final L2PcInstance activeChar, final int id, final int level)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			showMainPage(activeChar);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			
			return;
		}
		
		target = null;
		
		if (!player.isClanLeader())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addString(player.getName()));
			showMainPage(activeChar);
			
			return;
		}
		
		if (id < 370 || id > 391 || level < 1 || level > 3)
		{
			activeChar.sendMessage("Usage: //add_clan_skill <skill_id> <level>");
			showMainPage(activeChar);
			
			return;
		}
		
		final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
		if (skill != null)
		{
			String skillname = skill.getName();
			final SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED);
			sm.addSkillName(id);
			player.sendPacket(sm);
			player.getClan().broadcastToOnlineMembers(sm);
			player.getClan().addNewSkill(skill);
			activeChar.sendMessage("You gave the Clan Skill: " + skillname + " to the clan " + player.getClan().getName() + ".");
			
			activeChar.getClan().broadcastToOnlineMembers(new PledgeSkillList(activeChar.getClan()));
			
			for (final L2PcInstance member : activeChar.getClan().getOnlineMembers(""))
			{
				member.sendSkillList();
			}
			
			showMainPage(activeChar);
			skillname = null;
			return;
		}
		activeChar.sendMessage("Error: there is no such skill.");
	}
}

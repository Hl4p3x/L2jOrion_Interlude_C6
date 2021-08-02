/*
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

import java.util.Iterator;
import java.util.Set;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.datatables.sql.CharTemplateTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.enums.AchType;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.managers.FortSiegeManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Clan.SubPledge;
import l2jorion.game.model.L2ClanMember;
import l2jorion.game.model.L2PledgeSkillLearn;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.base.ClassType;
import l2jorion.game.model.base.PlayerClass;
import l2jorion.game.model.base.PlayerRace;
import l2jorion.game.model.base.SubClass;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.AquireSkillList;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class L2VillageMasterInstance extends L2FolkInstance
{
	protected static Logger LOG = LoggerFactory.getLogger(L2VillageMasterInstance.class);
	
	public L2VillageMasterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		String[] commandStr = command.split(" ");
		String actualCommand = commandStr[0]; // Get actual command
		
		String cmdParams = "";
		String cmdParams2 = "";
		
		if (commandStr.length >= 2)
		{
			cmdParams = commandStr[1];
		}
		if (commandStr.length >= 3)
		{
			cmdParams2 = commandStr[2];
		}
		
		commandStr = null;
		
		// Fix exploit stuck subclass and skills
		if (player.isLearningSkill() || player.isLocked())
		{
			return;
		}
		
		if (actualCommand.equalsIgnoreCase("create_clan"))
		{
			if (cmdParams.equals(""))
			{
				return;
			}
			
			ClanTable.getInstance().createClan(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("create_academy"))
		{
			if (cmdParams.equals(""))
			{
				return;
			}
			
			createSubPledge(player, cmdParams, null, L2Clan.SUBUNIT_ACADEMY, Config.ALT_CREATE_ACADEMY_CLAN_LEVEL);
		}
		else if (actualCommand.equalsIgnoreCase("rename_pledge"))
		{
			if (cmdParams.isEmpty() || cmdParams2.isEmpty())
			{
				return;
			}
			
			renameSubPledge(player, Integer.valueOf(cmdParams), cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_royal"))
		{
			if (cmdParams.equals(""))
			{
				return;
			}
			
			createSubPledge(player, cmdParams, cmdParams2, L2Clan.SUBUNIT_ROYAL1, 6);
		}
		else if (actualCommand.equalsIgnoreCase("create_knight"))
		{
			if (cmdParams.equals(""))
			{
				return;
			}
			
			createSubPledge(player, cmdParams, cmdParams2, L2Clan.SUBUNIT_KNIGHT1, 7);
		}
		else if (actualCommand.equalsIgnoreCase("assign_subpl_leader"))
		{
			if (cmdParams.equals(""))
			{
				return;
			}
			
			assignSubPledgeLeader(player, cmdParams, cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_ally"))
		{
			if (cmdParams.equals(""))
			{
				return;
			}
			
			if (!player.isClanLeader())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE));
				return;
			}
			player.getClan().createAlly(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_ally"))
		{
			if (!player.isClanLeader())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER));
				return;
			}
			player.getClan().dissolveAlly(player);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_clan"))
		{
			dissolveClan(player, player.getClanId());
		}
		else if (actualCommand.equalsIgnoreCase("change_clan_leader"))
		{
			if (cmdParams.equals(""))
			{
				return;
			}
			
			changeClanLeader(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("recover_clan"))
		{
			recoverClan(player, player.getClanId());
		}
		else if (actualCommand.equalsIgnoreCase("increase_clan_level"))
		{
			if (!player.isClanLeader())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
				return;
			}
			player.getClan().levelUpClan(player);
		}
		else if (actualCommand.equalsIgnoreCase("learn_clan_skills"))
		{
			showPledgeSkillList(player);
		}
		else if (command.startsWith("Subclass"))
		{
			int cmdChoice = Integer.parseInt(command.substring(9, 10).trim());
			
			// Subclasses may not be changed while a skill is in use.
			if (player.isCastingNow() || player.isAllSkillsDisabled())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE));
				return;
			}
			
			if (player.isInCombat())
			{
				player.sendMessage("You can't change Subclass when you are in combat.");
				return;
			}
			
			if (player.isCursedWeaponEquiped())
			{
				player.sendMessage("You can`t change Subclass while Cursed weapon equiped!");
				return;
			}
			
			TextBuilder content = new TextBuilder("<html><body>");
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			Set<PlayerClass> subsAvailable;
			
			int paramOne = 0;
			int paramTwo = 0;
			
			try
			{
				int endIndex = command.length();
				
				if (command.length() > 13)
				{
					endIndex = 13;
					paramTwo = Integer.parseInt(command.substring(13).trim());
				}
				
				if (endIndex > 11)
				{
					paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
				}
			}
			catch (Exception NumberFormatException)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					NumberFormatException.printStackTrace();
				}
			}
			
			switch (cmdChoice)
			{
				case 1: // Add Subclass - Initial
					// Avoid giving player an option to add a new sub class, if they have three already.
					if (player.getTotalSubClasses() == Config.ALLOWED_SUBCLASS)
					{
						player.sendMessage("You can now only change one of your current sub classes.");
						return;
					}
					
					subsAvailable = getAvailableSubClasses(player);
					
					if (subsAvailable != null && !subsAvailable.isEmpty())
					{
						content.append("Add Subclass:<br>Which sub class do you wish to add?<br>");
						
						for (PlayerClass subClass : subsAvailable)
						{
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 4 " + subClass.ordinal() + "\" msg=\"1268;" + formatClassForDisplay(subClass) + "\">" + formatClassForDisplay(subClass) + "</a><br>");
						}
					}
					else
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					break;
				case 2: // Change Class - Initial
					
					content.append("Change Subclass:<br>");
					
					final int baseClassId = player.getBaseClass();
					
					if (player.getSubClasses().isEmpty())
					{
						content.append("You can't change sub classes when you don't have a sub class to begin with.<br>" + "<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 1\">Add subclass.</a>");
					}
					else
					{
						content.append("Which class would you like to switch to?<br>");
						
						if (baseClassId == player.getActiveClass())
						{
							content.append(CharTemplateTable.getClassNameById(baseClassId) + "&nbsp;<font color=\"LEVEL\">(Base Class)</font><br><br>");
						}
						else
						{
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 0\">" + CharTemplateTable.getClassNameById(baseClassId) + "</a>&nbsp;" + "<font color=\"LEVEL\">(Base Class)</font><br><br>");
						}
						
						for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
						{
							SubClass subClass = subList.next();
							int subClassId = subClass.getClassId();
							
							if (subClassId == player.getActiveClass())
							{
								content.append(CharTemplateTable.getClassNameById(subClassId) + "<br>");
							}
							else
							{
								content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 " + subClass.getClassIndex() + "\">" + CharTemplateTable.getClassNameById(subClassId) + "</a><br>");
							}
						}
					}
					break;
				case 3: // Change/Cancel Subclass - Initial
					
					content.append("Change Subclass:<br>Which of the following sub classes would you like to change?<br>");
					int classIndex = 1;
					
					for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
					{
						SubClass subClass = subList.next();
						
						content.append("Sub-class " + classIndex + "<br1>");
						content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 6 " + subClass.getClassIndex() + "\">" + CharTemplateTable.getClassNameById(subClass.getClassId()) + "</a><br>");
						
						classIndex++;
					}
					content.append("<br>If you change a sub class, you'll start at level 40 after the 2nd class transfer.");
					break;
				case 4: // Add Subclass - Action (Subclass 4 x[x])
					/*
					 * If the character is less than level 75 on any of their previously chosen classes then disallow them to change to their most recently added sub-class choice.
					 */
					
					// Fix exploit stuck subclass and skills
					if (player.isLearningSkill() || player.isLocked())
					{
						return;
					}
					
					player.setLocked(true);
					boolean allowAddition = true;
					
					// Subclass exploit fix during add subclass
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("add subclass"))
					{
						LOG.warn("Player " + player.getName() + " has performed a subclass change too fast");
						player.setLocked(false);
						return;
					}
					
					// You can't add Subclass when you are registered in Events (TVT, CTF, DM)
					if (player._inEventTvT || player._inEventCTF || player._inEventDM)
					{
						player.sendMessage("You can't add a subclass while in an event.");
						player.setLocked(false);
						return;
					}
					
					// Check player level
					if (player.getLevel() < 75)
					{
						player.sendMessage("You may not add a new sub class before you are level 75 on your previous class.");
						allowAddition = false;
					}
					
					// You can't add Subclass when you are registered in Olympiad
					if (OlympiadManager.getInstance().isRegisteredInComp(player) || player.getOlympiadGameId() > 0)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT));
						player.setLocked(false);
						return;
					}
					
					if (allowAddition)
					{
						if (!player.getSubClasses().isEmpty())
						{
							for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
							{
								SubClass subClass = subList.next();
								
								if (subClass.getLevel() < 75)
								{
									player.sendMessage("You may not add a new sub class before you are level 75 on your previous sub class.");
									allowAddition = false;
									break;
								}
							}
						}
					}
					
					/*
					 * If quest checking is enabled, verify if the character has completed the Mimir's Elixir (Path to Subclass) and Fate's Whisper (A Grade Weapon) quests by checking for instances of their unique reward items. If they both exist, remove both unique items and continue with adding
					 * the sub-class.
					 */
					if (!Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS)
					{
						QuestState qs = player.getQuestState("235_MimirsElixir");
						if (qs == null || !qs.isCompleted())
						{
							player.sendMessage("You must have completed the Mimir's Elixir quest to continue adding your sub class.");
							player.setLocked(false);
							return;
						}
						
						/*
						 * qs = player.getQuestState("234_FatesWhisper"); if(qs == null || !qs.isCompleted()) { player.sendMessage("You must have completed the Fate's Whisper quest to continue adding your sub class."); player.setLocked(false); return; }
						 */
					}
					
					if (allowAddition)
					{
						String className = CharTemplateTable.getClassNameById(paramOne);
						
						if (!player.addSubClass(paramOne, player.getTotalSubClasses() + 1))
						{
							player.sendMessage("The sub class could not be added.");
							player.setLocked(false);
							return;
						}
						
						player.setActiveClass(player.getTotalSubClasses());
						
						if (player.getAchievement().getCount(AchType.SUBCLASS) < player.getSubClasses().size())
						{
							player.getAchievement().increase(AchType.SUBCLASS, player.getSubClasses().size(), false, false);
						}
						
						if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
						{
							player.checkAllowedSkills();
						}
						
						content.append("Add Subclass:<br>The sub class of <font color=\"LEVEL\">" + className + "</font> has been added.");
						player.sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER)); // Transfer to new class.
						
					}
					else
					{
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
					}
					
					player.setLocked(false);
					break;
				case 5: // Change Class - Action
					/*
					 * If the character is less than level 75 on any of their previously chosen classes then disallow them to change to their most recently added sub-class choice. Note: paramOne = classIndex
					 */
					
					// Fix exploit stuck subclass and skills
					if (player.isLearningSkill() || player.isLocked())
					{
						return;
					}
					
					player.setLocked(true);
					
					// Subclass exploit fix during change subclass
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("change subclass"))
					{
						LOG.warn("Player " + player.getName() + " has performed a subclass change too fast");
						player.setLocked(false);
						return;
					}
					
					// You can't change Subclass when you are registered in Events (TVT, CTF, DM)
					if (player._inEventTvT || player._inEventCTF || player._inEventDM)
					{
						player.sendMessage("You can't change subclass while in an event.");
						player.setLocked(false);
						return;
					}
					
					// You can't change Subclass when you are registered in Olympiad
					if (OlympiadManager.getInstance().isRegisteredInComp(player) || player.getOlympiadGameId() > 0)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT));
						player.setLocked(false);
						return;
					}
					
					player.setActiveClass(paramOne);
					
					content.append("Change Subclass:<br>Your active sub class is now a <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(player.getActiveClass()) + "</font>.");
					
					player.sendPacket(new SystemMessage(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED)); // Transfer completed.
					
					// check player skills
					if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
					{
						player.checkAllowedSkills();
					}
					
					player.setLocked(false);
					break;
				case 6: // Change/Cancel Subclass - Choice
					
					content.append("Please choose a sub class to change to. If the one you are looking for is not here, " + "please seek out the appropriate master for that class.<br>" + "<font color=\"LEVEL\">Warning!</font> All classes and skills for this class will be removed.<br><br>");
					
					subsAvailable = getAvailableSubClasses(player);
					
					if (subsAvailable != null && !subsAvailable.isEmpty())
					{
						for (PlayerClass subClass : subsAvailable)
						{
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 7 " + paramOne + " " + subClass.ordinal() + "\">" + formatClassForDisplay(subClass) + "</a><br>");
						}
					}
					else
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					break;
				case 7: // Cancel/Change Subclass - Action
					/*
					 * Warning: the information about this subclass will be removed from the subclass list even if false!
					 */
					
					// Fix exploit stuck subclass and skills
					if (player.isLearningSkill() || player.isLocked())
					{
						return;
					}
					
					player.setLocked(true);
					
					// Subclass exploit fix during delete subclass
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("delete subclass"))
					{
						LOG.warn("Player " + player.getName() + " has performed a subclass change too fast");
						player.setLocked(false);
						return;
					}
					
					// You can't delete Subclass when you are registered in Events (TVT, CTF, DM)
					if (player._inEventTvT || player._inEventCTF || player._inEventDM)
					{
						player.sendMessage("You can't delete a subclass while in an event.");
						player.setLocked(false);
						return;
					}
					
					// You can't delete Subclass when you are registered in Olympiad
					if (OlympiadManager.getInstance().isRegisteredInComp(player) || player.getOlympiadGameId() > 0)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT));
						player.setLocked(false);
						return;
					}
					
					if (player.modifySubClass(paramOne, paramTwo))
					{
						player.setActiveClass(paramOne);
						
						content.append("Change Subclass:<br>Your sub class has been changed to <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(paramTwo) + "</font>.");
						
						player.sendPacket(new SystemMessage(SystemMessageId.ADD_NEW_SUBCLASS)); // Subclass added.
						
						// check player skills
						if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
						{
							player.checkAllowedSkills();
						}
						
					}
					else
					{
						/*
						 * This isn't good! modifySubClass() removed subclass from memory we must update _classIndex! Else IndexOutOfBoundsException can turn up some place down the line along with other seemingly unrelated problems.
						 */
						
						player.setActiveClass(0); // Also updates _classIndex plus switching _classid to baseclass.
						
						if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
						{
							player.checkAllowedSkills();
						}
						
						player.sendMessage("The sub class could not be added, you have been reverted to your base class.");
						player.setLocked(false);
						return;
					}
					
					player.setLocked(false);
					break;
			}
			content.append("</body></html>");
			
			// If the content is greater than for a basic blank page,
			// then assume no external HTML file was assigned.
			if (content.length() > 26)
			{
				html.setHtml(content.toString());
			}
			
			player.sendPacket(html);
		}
		else
		{
			// this class dont know any other commands, let forward
			// the command to the parent class
			super.onBypassFeedback(player, command);
		}
	}
	
	@Override
	public String getHtmlPath(L2PcInstance player, final int npcId, final int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/villagemaster/" + pom + ".htm";
	}
	
	public void dissolveClan(L2PcInstance player, int clanId)
	{
		if (Config.DEBUG)
		{
			LOG.warn(player.getObjectId() + "(" + player.getName() + ") requested dissolve a clan from " + getObjectId() + "(" + getName() + ")");
		}
		
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		
		L2Clan clan = player.getClan();
		if (clan.getAllyId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISPERSE_THE_CLANS_IN_ALLY));
			return;
		}
		
		if (clan.isAtWar() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_WAR));
			return;
		}
		
		if (clan.getHasCastle() != 0 || clan.getHasHideout() != 0 || clan.getHasFort() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_OWNING_CLAN_HALL_OR_CASTLE));
			return;
		}
		
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, castle.getCastleId()))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_CAUSE_CLAN_WILL_PARTICIPATE_IN_CASTLE_SIEGE));
				return;
			}
		}
		
		for (Fort fort : FortManager.getInstance().getForts())
		{
			if (FortSiegeManager.getInstance().checkIsRegistered(clan, fort.getFortId()))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE));
				return;
			}
		}
		
		if (player.isInsideZone(ZoneId.ZONE_SIEGE))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE));
			return;
		}
		
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.DISSOLUTION_IN_PROGRESS));
			return;
		}
		
		clan.setDissolvingExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_DISSOLVE_DAYS * 86400000L); // 24*60*60*1000 = 86400000
		clan.updateClanInDB();
		
		ClanTable.getInstance().scheduleRemoveClan(clan.getClanId());
		
		// The clan leader should take the XP penalty of a full death.
		player.deathPenalty(false);
		
		clan = null;
	}
	
	/**
	 * Recover clan.
	 * @param player the player
	 * @param clanId the clan id
	 */
	public void recoverClan(L2PcInstance player, int clanId)
	{
		if (Config.DEBUG)
		{
			LOG.warn(player.getObjectId() + "(" + player.getName() + ") requested recover a clan from " + getObjectId() + "(" + getName() + ")");
		}
		
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		L2Clan clan = player.getClan();
		
		clan.setDissolvingExpiryTime(0);
		clan.updateClanInDB();
		
		clan = null;
	}
	
	/**
	 * Change clan leader.
	 * @param player the player
	 * @param target the target
	 */
	public void changeClanLeader(L2PcInstance player, String target)
	{
		if (Config.DEBUG)
		{
			LOG.warn(player.getObjectId() + "(" + player.getName() + ") requested change a clan leader from " + getObjectId() + "(" + getName() + ")");
		}
		
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		
		if (player.isFlying())
		{
			player.sendMessage("Get off the Wyvern first.");
			return;
		}
		
		if (player.isMounted())
		{
			player.sendMessage("Get off the Pet first.");
			return;
		}
		
		if (player.getName().equalsIgnoreCase(target))
		{
			return;
		}
		
		L2Clan clan = player.getClan();
		L2ClanMember member = clan.getClanMember(target);
		if (member == null)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
			sm.addString(target);
			player.sendPacket(sm);
			sm = null;
			return;
		}
		
		if (!member.isOnline())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INVITED_USER_NOT_ONLINE));
			return;
		}
		
		if (SiegeManager.getInstance().checkIsRegisteredInSiege(clan) || FortSiegeManager.getInstance().checkIsRegisteredInSiege(clan))
		{
			player.sendMessage("Cannot change clan leader while registered in Siege");
			return;
		}
		// Set old name/title color for old clan leader
		if (Config.CLAN_LEADER_COLOR_ENABLED && clan.getLevel() >= Config.CLAN_LEADER_COLOR_CLAN_LEVEL)
		{
			if (Config.CLAN_LEADER_COLORED == 1)
			{
				player.getAppearance().setNameColor(0x000000);
			}
			else
			{
				player.getAppearance().setTitleColor(0xFFFF77);
			}
		}
		// clan.setNewLeader(member);
		clan.setNewLeader(member, player);
		
		clan = null;
		member = null;
	}
	
	public void createSubPledge(L2PcInstance player, String clanName, String leaderName, int pledgeType, int minClanLvl)
	{
		if (Config.DEBUG)
		{
			LOG.warn(player.getObjectId() + "(" + player.getName() + ") requested sub clan creation from " + getObjectId() + "(" + getName() + ")");
		}
		
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		
		L2Clan clan = player.getClan();
		if (clan.getLevel() < minClanLvl)
		{
			if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN_ACADEMY));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_MILITARY_UNIT));
			}
			return;
		}
		
		if (!Util.isAlphaNumeric(clanName) || 2 > clanName.length())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return;
		}
		
		if (clanName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_TOO_LONG));
			return;
		}
		for (L2Clan tempClan : ClanTable.getInstance().getClans())
		{
			if (tempClan.getSubPledge(clanName) != null)
			{
				if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
					sm.addString(clanName);
					player.sendPacket(sm);
					sm = null;
				}
				else
				{
					player.sendPacket(new SystemMessage(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME));
				}
				return;
			}
		}
		
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			if (clan.getClanMember(leaderName) == null || clan.getClanMember(leaderName).getPledgeType() != 0)
			{
				if (pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED));
				}
				else if (pledgeType >= L2Clan.SUBUNIT_ROYAL1)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED));
				}
				return;
			}
		}
		
		if (clan.createSubPledge(player, pledgeType, leaderName, clanName) == null)
		{
			return;
		}
		
		SystemMessage sm;
		if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
		{
			sm = new SystemMessage(SystemMessageId.THE_S1S_CLAN_ACADEMY_HAS_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
		{
			sm = new SystemMessage(SystemMessageId.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= L2Clan.SUBUNIT_ROYAL1)
		{
			sm = new SystemMessage(SystemMessageId.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.CLAN_CREATED);
		}
		
		player.sendPacket(sm);
		
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
			if (leaderSubPledge.getPlayerInstance() == null)
			{
				return;
			}
			
			leaderSubPledge.getPlayerInstance().setPledgeClass(leaderSubPledge.calculatePledgeClass(leaderSubPledge.getPlayerInstance()));
			leaderSubPledge.getPlayerInstance().sendPacket(new UserInfo(leaderSubPledge.getPlayerInstance()));
		}
	}
	
	/**
	 * Rename sub pledge.
	 * @param player the player
	 * @param pledgeType the pledge type
	 * @param pledgeName the pledge name
	 */
	private static final void renameSubPledge(L2PcInstance player, int pledgeType, String pledgeName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		
		final L2Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(pledgeType);
		
		if (subPledge == null)
		{
			player.sendMessage("Pledge don't exists.");
			return;
		}
		if (!Util.isAlphaNumeric(pledgeName) || 2 > pledgeName.length())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return;
		}
		if (pledgeName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_TOO_LONG));
			return;
		}
		
		subPledge.setName(pledgeName);
		clan.updateSubPledgeInDB(subPledge.getId());
		clan.broadcastClanStatus();
		player.sendMessage("Pledge name changed.");
	}
	
	/**
	 * Assign sub pledge leader.
	 * @param player the player
	 * @param clanName the clan name
	 * @param leaderName the leader name
	 */
	public void assignSubPledgeLeader(L2PcInstance player, String clanName, String leaderName)
	{
		if (Config.DEBUG)
		{
			LOG.warn(player.getObjectId() + "(" + player.getName() + ") requested to assign sub clan" + clanName + "leader " + "(" + leaderName + ")");
		}
		
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		
		if (leaderName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NAMING_CHARNAME_UP_TO_16CHARS));
			return;
		}
		
		if (player.getName().equals(leaderName))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED));
			return;
		}
		
		L2Clan clan = player.getClan();
		SubPledge subPledge = player.getClan().getSubPledge(clanName);
		if (null == subPledge)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return;
		}
		
		if (subPledge.getId() == L2Clan.SUBUNIT_ACADEMY)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return;
		}
		
		if (clan.getClanMember(leaderName) == null || clan.getClanMember(leaderName).getPledgeType() != 0)
		{
			if (subPledge.getId() >= L2Clan.SUBUNIT_KNIGHT1)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED));
			}
			else if (subPledge.getId() >= L2Clan.SUBUNIT_ROYAL1)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED));
			}
			return;
		}
		
		subPledge.setLeaderName(leaderName);
		clan.updateSubPledgeInDB(subPledge.getId());
		L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
		
		if (leaderSubPledge.getPlayerInstance() != null)
		{
			
			leaderSubPledge.getPlayerInstance().setPledgeClass(leaderSubPledge.calculatePledgeClass(leaderSubPledge.getPlayerInstance()));
			leaderSubPledge.getPlayerInstance().sendPacket(new UserInfo(leaderSubPledge.getPlayerInstance()));
			clan.broadcastClanStatus();
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_SELECTED_AS_CAPTAIN_OF_S2);
			sm.addString(leaderName);
			sm.addString(clanName);
			clan.broadcastToOnlineMembers(sm);
			sm = null;
		}
		
		clan = null;
		subPledge = null;
		leaderSubPledge = null;
	}
	
	/**
	 * Gets the available sub classes.
	 * @param player the player
	 * @return the available sub classes If the race of your main class is Elf or Dark Elf, you may not select each class as a subclass to the other class, and you may not select Overlord and Warsmith class as a subclass. You may not select a similar class as the subclass. The occupations classified
	 *         as similar classes are as follows: Treasure Hunter, Plainswalker and Abyss Walker Hawkeye, Silver Ranger and Phantom Ranger Paladin, Dark Avenger, Temple Knight and Shillien Knight Warlocks, Elemental Summoner and Phantom Summoner Elder and Shillien Elder Swordsinger and Bladedancer
	 *         Sorcerer, Spellsinger and Spellhowler
	 */
	
	private final Set<PlayerClass> getAvailableSubClasses(L2PcInstance player)
	{
		int charClassId = player.getBaseClass();
		
		if (charClassId >= 88)
		{
			charClassId = player.getClassId().getParent().ordinal();
		}
		
		final PlayerRace npcRace = getVillageMasterRace();
		final ClassType npcTeachType = getVillageMasterTeachType();
		
		PlayerClass currClass = PlayerClass.values()[charClassId];
		
		Set<PlayerClass> availSubs = currClass.getAvailableSubclasses(player);
		
		if (availSubs != null)
		{
			for (PlayerClass availSub : availSubs)
			{
				for (SubClass subClass : player.getSubClasses().values())
				{
					if (subClass.getClassId() == availSub.ordinal())
					{
						availSubs.remove(availSub);
					}
				}
				
				for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					int charBaseClassId = player.getBaseClass();
					charBaseClassId = ClassId.values()[charBaseClassId].getParent().getId();
					
					SubClass prevSubClass = subList.next();
					int subClassId = prevSubClass.getClassId();
					if (subClassId >= 88)
					{
						subClassId = ClassId.values()[subClassId].getParent().getId();
					}
					
					if (availSub.ordinal() == subClassId || availSub.ordinal() == player.getBaseClass() || availSub.ordinal() == charBaseClassId)
					{
						availSubs.remove(PlayerClass.values()[availSub.ordinal()]);
					}
				}
				
				if (npcRace == PlayerRace.Human || npcRace == PlayerRace.LightElf)
				{
					// If the master is human or light elf, ensure that fighter-type
					// masters only teach fighter classes, and priest-type masters
					// only teach priest classes etc.
					if (!availSub.isOfType(npcTeachType))
					{
						availSubs.remove(availSub);
					}
					else if (!availSub.isOfRace(PlayerRace.Human) && !availSub.isOfRace(PlayerRace.LightElf))
					{
						availSubs.remove(availSub);
					}
				}
				else
				{
					// If the master is not human and not light elf,
					// then remove any classes not of the same race as the master.
					if (npcRace != PlayerRace.Human && npcRace != PlayerRace.LightElf && !availSub.isOfRace(npcRace))
					{
						availSubs.remove(availSub);
					}
				}
			}
		}
		return availSubs;
	}
	
	/**
	 * this displays PledgeSkillList to the player.
	 * @param player the player
	 */
	public void showPledgeSkillList(L2PcInstance player)
	{
		if (Config.DEBUG)
		{
			LOG.warn("PledgeSkillList activated on: " + getObjectId());
		}
		if (player.getClan() == null)
		{
			return;
		}
		
		L2PledgeSkillLearn[] skills = SkillTreeTable.getInstance().getAvailablePledgeSkills(player);
		AquireSkillList asl = new AquireSkillList(AquireSkillList.skillType.Clan);
		int counts = 0;
		
		for (L2PledgeSkillLearn s : skills)
		{
			int cost = s.getRepCost();
			counts++;
			
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}
		skills = null;
		
		if (counts == 0)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			
			if (player.getClan().getLevel() < 8)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN);
				sm.addNumber(player.getClan().getLevel() + 1);
				player.sendPacket(sm);
				sm = null;
			}
			else
			{
				TextBuilder sb = new TextBuilder();
				sb.append("<html><body>");
				sb.append("You've learned all skills available for your Clan.<br>");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
				html = null;
				sb = null;
			}
		}
		else
		{
			player.sendPacket(asl);
		}
		
		asl = null;
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Format class for display.
	 * @param className the class name
	 * @return the string
	 */
	private final String formatClassForDisplay(PlayerClass className)
	{
		String classNameStr = className.toString();
		char[] charArray = classNameStr.toCharArray();
		
		for (int i = 1; i < charArray.length; i++)
		{
			if (Character.isUpperCase(charArray[i]))
			{
				classNameStr = classNameStr.substring(0, i) + " " + classNameStr.substring(i);
			}
		}
		
		return classNameStr;
	}
	
	/**
	 * Gets the village master race.
	 * @return the village master race
	 */
	private final PlayerRace getVillageMasterRace()
	{
		String npcClass = getTemplate().getStatsSet().getString("jClass").toLowerCase();
		
		if (npcClass.indexOf("human") > -1)
		{
			return PlayerRace.Human;
		}
		
		if (npcClass.indexOf("darkelf") > -1)
		{
			return PlayerRace.DarkElf;
		}
		
		if (npcClass.indexOf("elf") > -1)
		{
			return PlayerRace.LightElf;
		}
		
		if (npcClass.indexOf("orc") > -1)
		{
			return PlayerRace.Orc;
		}
		
		return PlayerRace.Dwarf;
	}
	
	/**
	 * Gets the village master teach type.
	 * @return the village master teach type
	 */
	private final ClassType getVillageMasterTeachType()
	{
		String npcClass = getTemplate().getStatsSet().getString("jClass");
		
		if (npcClass.indexOf("sanctuary") > -1 || npcClass.indexOf("clergyman") > -1)
		{
			return ClassType.Priest;
		}
		
		if (npcClass.indexOf("mageguild") > -1 || npcClass.indexOf("patriarch") > -1)
		{
			return ClassType.Mystic;
		}
		
		return ClassType.Fighter;
	}
	
	/**
	 * Iter sub classes.
	 * @param player the player
	 * @return the iterator
	 */
	private Iterator<SubClass> iterSubClasses(L2PcInstance player)
	{
		return player.getSubClasses().values().iterator();
	}
}

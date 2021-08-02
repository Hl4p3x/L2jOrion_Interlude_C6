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
package l2jorion.game.handler.voice;

import java.util.Iterator;
import java.util.Set;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.datatables.sql.CharTemplateTable;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.base.PlayerClass;
import l2jorion.game.model.base.SubClass;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;

public class Sub implements IVoicedCommandHandler, ICustomByPassHandler
{
	private static final String PARENT_DIR = "data/html/mods/sub/";
	
	private static final String[] VOICED_COMMANDS =
	{
		"sub",
		"setlvl"
	};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance player, String target)
	{
		L2Object targetChar = player.getTarget();
		
		if (player.isInsideZone(ZoneId.ZONE_BOSS))
		{
			player.sendMessage("Command is not available in this area.");
			return false;
		}
		
		if (command.equalsIgnoreCase("sub"))
		{
			showHtm(player);
		}
		else if (command.equalsIgnoreCase("setlvl"))
		{
			try
			{
				if (!Config.ALLOW_COMMAND_LEVEL_UP)
				{
					return false;
				}
				
				if (targetChar == null)
				{
					player.setTarget(player);
					targetChar = player.getTarget();
				}
				
				if (targetChar != player || !(targetChar instanceof L2PlayableInstance))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT)); // incorrect
					player.sendMessage("Error! Set target yourself.");
					return false;
				}
				
				final L2PlayableInstance targetPlayer = (L2PlayableInstance) targetChar;
				final byte lvl = Byte.parseByte(target);
				int max_level = ExperienceData.getInstance().getMaxLevel();
				
				if (targetChar instanceof L2PcInstance && ((L2PcInstance) targetPlayer).isSubClassActive())
				{
					max_level = Config.MAX_SUBCLASS_LEVEL;
				}
				
				if (lvl >= 1 && lvl <= max_level)
				{
					final long pXp = targetPlayer.getStat().getExp();
					final long tXp = ExperienceData.getInstance().getExpForLevel(lvl);
					
					if (pXp > tXp)
					{
						targetPlayer.getStat().removeExpAndSp(pXp - tXp, 0);
					}
					else if (pXp < tXp)
					{
						targetPlayer.getStat().addExpAndSp(tXp - pXp, 0);
					}
				}
			}
			catch (final NumberFormatException e)
			{
				player.sendMessage("Error! You have to choose level, for example: .setlvl 81");
			}
		}
		return true;
	}
	
	private void showHtm(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		htm.setFile(PARENT_DIR + "SubClass.htm");
		player.sendPacket(htm);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"sub_0",
			"sub_1",
			"sub_2",
			"sub_3",
			"sub_4",
			"sub_5",
			"sub_6",
			"sub_7"
		};
	}
	
	private enum CommandEnum
	{
		sub_0,
		sub_1,
		sub_2,
		sub_3,
		sub_4,
		sub_5,
		sub_6,
		sub_7
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		CommandEnum comm = CommandEnum.valueOf(command);
		
		// Fix exploit stuck subclass and skills
		if (player.isLearningSkill() || player.isLocked())
		{
			return;
		}
		
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
		
		NpcHtmlMessage html = new NpcHtmlMessage(player.getLastQuestNpcObject());
		TextBuilder content = new TextBuilder("<html><body>");
		Set<PlayerClass> subsAvailable;
		
		switch (comm)
		{
			case sub_0:
			{
				showHtm(player);
				return;
			}
			case sub_1:
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
						content.append("<a action=\"bypass -h custom_sub_4 " + subClass.ordinal() + "\" msg=\"1268;" + formatClassForDisplay(subClass) + "\">" + formatClassForDisplay(subClass) + "</a><br>");
					}
				}
				else
				{
					player.sendMessage("There are no sub classes available at this time.");
					return;
				}
				break;
			case sub_2: // Change Class - Initial
				content.append("Change Subclass:<br>");
				
				final int baseClassId = player.getBaseClass();
				
				if (player.getSubClasses().isEmpty())
				{
					content.append("You can't change sub classes when you don't have a sub class to begin with.<br>" + "<a action=\"bypass -h custom_sub_1\">Add subclass.</a>");
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
						content.append("<a action=\"bypass -h custom_sub_5 0\">" + CharTemplateTable.getClassNameById(baseClassId) + "</a>&nbsp;" + "<font color=\"LEVEL\">(Base Class)</font><br><br>");
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
							content.append("<a action=\"bypass -h custom_sub_5 " + subClass.getClassIndex() + "\">" + CharTemplateTable.getClassNameById(subClassId) + "</a><br>");
						}
					}
				}
				break;
			case sub_3: // Change/Cancel Subclass - Initial
				content.append("Change Subclass:<br>Which of the following sub classes would you like to change?<br>");
				int classIndex = 1;
				
				for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					SubClass subClass = subList.next();
					content.append("Sub-class " + classIndex + "<br1>");
					content.append("<a action=\"bypass -h custom_sub_6 " + subClass.getClassIndex() + "\">" + CharTemplateTable.getClassNameById(subClass.getClassId()) + "</a><br>");
					classIndex++;
				}
				content.append("<br>If you change a sub class, you'll start at level 40 after the 2nd class transfer.");
				break;
			case sub_4: // Add Subclass - Action (Subclass 4 x[x])
				int flag = Integer.parseInt(parameters.trim());
				
				if (player.isLearningSkill() || player.isLocked())
				{
					return;
				}
				
				player.setLocked(true);
				boolean allowAddition = true;
				
				// Subclass exploit fix during add subclass
				if (!player.getFloodProtectors().getSubclass().tryPerformAction("add subclass"))
				{
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
				 * If quest checking is enabled, verify if the character has completed the Mimir's Elixir (Path to Subclass) and Fate's Whisper (A Grade Weapon) quests by checking for instances of their unique reward items. If they both exist, remove both unique items and continue with adding the
				 * sub-class.
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
					qs = player.getQuestState("234_FatesWhisper");
					
					if (qs == null || !qs.isCompleted())
					{
						player.sendMessage("You must have completed the Fate's Whisper quest to continue adding your sub class.");
						player.setLocked(false);
						return;
					}
				}
				
				if (allowAddition)
				{
					String className = CharTemplateTable.getClassNameById(flag);
					
					if (!player.addSubClass(flag, player.getTotalSubClasses() + 1))
					{
						player.sendMessage("The sub class could not be added.");
						player.setLocked(false);
						return;
					}
					
					player.setActiveClass(player.getTotalSubClasses());
					
					if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
					{
						player.checkAllowedSkills();
					}
					
					content.append("Add Subclass:<br>The sub class of <font color=\"LEVEL\">" + className + "</font> has been added.");
					player.sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER)); // Transfer to new class.
				}
				else
				{
					html.setFile("data/html/mods/sub/SubClass_Fail.htm");
				}
				
				player.setLocked(false);
				break;
			case sub_5: // Change Class - Action
				int flag2 = Integer.parseInt(parameters.trim());
				// Fix exploit stuck subclass and skills
				if (player.isLearningSkill() || player.isLocked())
				{
					return;
				}
				
				player.setLocked(true);
				
				// Subclass exploit fix during change subclass
				if (!player.getFloodProtectors().getSubclass().tryPerformAction("change subclass"))
				{
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
				
				player.setActiveClass(flag2);
				
				content.append("Change Subclass:<br>Your active sub class is now a <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(player.getActiveClass()) + "</font>.");
				
				player.sendPacket(new SystemMessage(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED)); // Transfer completed.
				
				// check player skills
				if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
				{
					player.checkAllowedSkills();
				}
				
				player.setLocked(false);
				break;
			case sub_6: // Change/Cancel Subclass - Choice
				int subIndex = Integer.parseInt(parameters.trim());
				content.append("Please choose a sub class to change to. If the one you are looking for is not here, " + "please seek out the appropriate master for that class.<br>" + "<font color=\"LEVEL\">Warning!</font> All classes and skills for this class will be removed.<br><br>");
				
				subsAvailable = getAvailableSubClasses(player);
				
				if (subsAvailable != null && !subsAvailable.isEmpty())
				{
					for (PlayerClass subClass : subsAvailable)
					{
						content.append("<a action=\"bypass -h custom_sub_7 " + subIndex + " " + subClass.ordinal() + "\">" + formatClassForDisplay(subClass) + "</a><br>");
					}
				}
				else
				{
					player.sendMessage("There are no sub classes available at this time.");
					return;
				}
				break;
			case sub_7:
				String strNum = parameters;
				String subIndexId = strNum.substring(0, 1);
				String subNewId = strNum.substring(2);
				
				int flag4 = Integer.parseInt(subIndexId);
				int flag5 = Integer.parseInt(subNewId);
				
				if (player.isLearningSkill() || player.isLocked())
				{
					return;
				}
				
				player.setLocked(true);
				
				// Subclass exploit fix during delete subclass
				if (!player.getFloodProtectors().getSubclass().tryPerformAction("delete subclass"))
				{
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
				
				if (player.modifySubClass(flag4, flag5))
				{
					player.setActiveClass(flag4);
					
					content.append("Change Subclass:<br>Your sub class has been changed to <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(flag5) + "</font>.");
					
					player.sendPacket(new SystemMessage(SystemMessageId.ADD_NEW_SUBCLASS)); // Subclass added.
					
					// check player skills
					if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
					{
						player.checkAllowedSkills();
					}
				}
				else
				{
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
		
		if (content.length() > 10)
		{
			html.setHtml(content.toString());
		}
		player.sendPacket(html);
	}
	
	private final Set<PlayerClass> getAvailableSubClasses(L2PcInstance player)
	{
		int charClassId = player.getBaseClass();
		
		if (charClassId >= 88)
		{
			charClassId = player.getClassId().getParent().ordinal();
		}
		
		PlayerClass currClass = PlayerClass.values()[charClassId];
		
		/**
		 * If the race of your main class is Elf or Dark Elf, you may not select each class as a subclass to the other class, and you may not select Overlord and Warsmith class as a subclass. You may not select a similar class as the subclass. The occupations classified as similar classes are as
		 * follows: Treasure Hunter, Plainswalker and Abyss Walker Hawkeye, Silver Ranger and Phantom Ranger Paladin, Dark Avenger, Temple Knight and Shillien Knight Warlocks, Elemental Summoner and Phantom Summoner Elder and Shillien Elder Swordsinger and Bladedancer Sorcerer, Spellsinger and
		 * Spellhowler
		 */
		
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
					SubClass prevSubClass = subList.next();
					int subClassId = prevSubClass.getClassId();
					if (subClassId >= 88)
					{
						subClassId = ClassId.values()[subClassId].getParent().getId();
					}
					
					if (availSub.ordinal() == subClassId || availSub.ordinal() == player.getBaseClass())
					{
						availSubs.remove(PlayerClass.values()[availSub.ordinal()]);
					}
				}
			}
		}
		return availSubs;
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
	 * Iter sub classes.
	 * @param player the player
	 * @return the iterator
	 */
	private Iterator<SubClass> iterSubClasses(L2PcInstance player)
	{
		return player.getSubClasses().values().iterator();
	}
}

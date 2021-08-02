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
package l2jorion.game.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import l2jorion.Config;
import l2jorion.game.GameServer;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.CharNameTable;
import l2jorion.game.datatables.sql.CharTemplateTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.QuestManager;
import l2jorion.game.model.L2ShortCut;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2SkillLearn;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.serverpackets.CharCreateFail;
import l2jorion.game.network.serverpackets.CharCreateOk;
import l2jorion.game.network.serverpackets.CharSelectInfo;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2PcTemplate;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public final class CharacterCreate extends L2GameClientPacket
{
	private static Logger LOG = LoggerFactory.getLogger(CharacterCreate.class);
	private String _name;
	private byte _sex, _hairStyle, _hairColor, _face;
	private int _classId;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
		readD();
		_sex = (byte) readD();
		_classId = readD();
		readD();
		readD();
		readD();
		readD();
		readD();
		readD();
		_hairStyle = (byte) readD();
		_hairColor = (byte) readD();
		_face = (byte) readD();
	}
	
	@Override
	protected void runImpl()
	{
		
		if (_name.length() < 3 || _name.length() > 16 || !Util.isAlphaNumeric(_name) || !isValidName(_name))
		{
			if (Config.DEBUG)
			{
				LOG.debug("DEBUG " + getType() + ": charname: " + _name + " is invalid. creation failed.");
			}
			
			sendPacket(new CharCreateFail(CharCreateFail.REASON_16_ENG_CHARS));
			return;
		}
		
		if (Config.DEBUG)
		{
			LOG.debug("DEBUG " + getType() + ": charname: " + _name + " classId: " + _classId);
		}
		
		L2PcInstance newChar = null;
		L2PcTemplate template = null;
		
		// Since checks for duplicate names are done using SQL, lock must be held until data is written to DB as well.
		synchronized (CharNameTable.getInstance())
		{
			if (CharNameTable.getInstance().accountCharNumber(getClient().getAccountName()) >= Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT && Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT != 0)
			{
				if (Config.DEBUG)
				{
					LOG.debug("DEBUG " + getType() + ": Max number of characters reached. Creation failed.");
				}
				
				sendPacket(new CharCreateFail(CharCreateFail.REASON_TOO_MANY_CHARACTERS));
				return;
			}
			else if (CharNameTable.getInstance().doesCharNameExist(_name))
			{
				if (Config.DEBUG)
				{
					LOG.debug("DEBUG " + getType() + ": charname: " + _name + " already exists. creation failed.");
				}
				
				sendPacket(new CharCreateFail(CharCreateFail.REASON_NAME_ALREADY_EXISTS));
				return;
			}
			else if (CharNameTable.getInstance().ipCharNumber(getClient().getConnection().getInetAddress().getHostName()) >= Config.MAX_CHARACTERS_NUMBER_PER_IP && Config.MAX_CHARACTERS_NUMBER_PER_IP != 0)
			{
				if (Config.DEBUG)
				{
					LOG.debug("DEBUG " + getType() + ": Max number of characters reached for IP. Creation failed.");
				}
				
				sendPacket(new CharCreateFail(CharCreateFail.REASON_TOO_MANY_CHARACTERS));
				return;
			}
			template = CharTemplateTable.getInstance().getTemplate(_classId);
			
			if (Config.DEBUG)
			{
				LOG.debug("DEBUG " + getType() + ": charname: " + _name + " classId: " + _classId + " template: " + template);
			}
			
			if (template == null || template.classBaseLevel > 1)
			{
				sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
				return;
			}
			
			final int objectId = IdFactory.getInstance().getNextId();
			newChar = L2PcInstance.create(objectId, template, getClient().getAccountName(), _name, _hairStyle, _hairColor, _face, _sex != 0);
			
			newChar.setCurrentHp(newChar.getMaxHp());// L2Off like
			// newChar.setCurrentCp(template.baseCpMax);
			newChar.setCurrentCp(0); // L2Off like
			newChar.setCurrentMp(newChar.getMaxMp());// L2Off like
			// newChar.setMaxLoad(template.baseLoad);
			if (Config.AUTO_LOOT)
			{
				newChar.setAutoLootEnabled(1);
			}
			else
			{
				newChar.setAutoLootEnabled(0);
			}
			
			if (Config.AUTO_LOOT_HERBS)
			{
				newChar.setAutoLootHerbs(1);
			}
			else
			{
				newChar.setAutoLootHerbs(0);
			}
			
			// send acknowledgement
			sendPacket(new CharCreateOk()); // Success
			initNewChar(getClient(), newChar);
		}
	}
	
	private boolean isValidName(final String text)
	{
		boolean result = true;
		final String test = text;
		Pattern pattern;
		
		try
		{
			pattern = Pattern.compile(Config.CNAME_TEMPLATE);
		}
		catch (final PatternSyntaxException e) // case of illegal pattern
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("ERROR " + getType() + ": Character name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		
		final Matcher regexp = pattern.matcher(test);
		if (!regexp.matches())
		{
			result = false;
		}
		
		return result;
	}
	
	private void initNewChar(final L2GameClient client, final L2PcInstance newChar)
	{
		if (Config.DEBUG)
		{
			LOG.debug("DEBUG " + getType() + ": Character init start");
		}
		
		L2World.getInstance().storeObject(newChar);
		final L2PcTemplate template = newChar.getTemplate();
		
		// Starting Items
		if (Config.STARTING_ADENA > 0)
		{
			newChar.addAdena("Init", Config.STARTING_ADENA, null, false);
		}
		
		if (Config.STARTING_AA > 0)
		{
			newChar.addAncientAdena("Init", Config.STARTING_AA, null, false);
		}
		
		if (Config.CUSTOM_STARTER_ITEMS_ENABLED)
		{
			if (newChar.isMageClass())
			{
				for (final int[] reward : Config.STARTING_CUSTOM_ITEMS_M)
				{
					if (ItemTable.getInstance().createDummyItem(reward[0]).isStackable())
					{
						newChar.getInventory().addItem("Starter Items Mage", reward[0], reward[1], newChar, null);
					}
					else
					{
						for (int i = 0; i < reward[1]; ++i)
						{
							newChar.getInventory().addItem("Starter Items Mage", reward[0], 1, newChar, null);
						}
					}
				}
			}
			else
			{
				for (final int[] reward : Config.STARTING_CUSTOM_ITEMS_F)
				{
					if (ItemTable.getInstance().createDummyItem(reward[0]).isStackable())
					{
						newChar.getInventory().addItem("Starter Items Fighter", reward[0], reward[1], newChar, null);
					}
					else
					{
						for (int i = 0; i < reward[1]; ++i)
						{
							newChar.getInventory().addItem("Starter Items Fighter", reward[0], 1, newChar, null);
						}
					}
				}
			}
			
			// cp
			if (newChar.getInventory().getItemByItemId(5592) != null && newChar.getInventory().getItemByItemId(5592).getCount() >= 1)
			{
				newChar.registerShortCut(new L2ShortCut(2, 1, 1, newChar.getInventory().getItemByItemId(5592).getObjectId(), -1, 1));
			}
			// hp
			if (newChar.getInventory().getItemByItemId(1539) != null && newChar.getInventory().getItemByItemId(1539).getCount() >= 1)
			{
				newChar.registerShortCut(new L2ShortCut(1, 1, 1, newChar.getInventory().getItemByItemId(1539).getObjectId(), -1, 1));
			}
			// mp
			if (newChar.getInventory().getItemByItemId(728) != null && newChar.getInventory().getItemByItemId(728).getCount() >= 1)
			{
				newChar.registerShortCut(new L2ShortCut(0, 1, 1, newChar.getInventory().getItemByItemId(728).getObjectId(), -1, 1));
			}
			
			// shots
			if (newChar.getInventory().getItemByItemId(3947) != null && newChar.getInventory().getItemByItemId(3947).getCount() >= 1)
			{
				newChar.registerShortCut(new L2ShortCut(10, 2, 1, newChar.getInventory().getItemByItemId(3947).getObjectId(), -1, 1));
			}
			
			if (newChar.getInventory().getItemByItemId(1835) != null && newChar.getInventory().getItemByItemId(1835).getCount() >= 1)
			{
				newChar.registerShortCut(new L2ShortCut(11, 2, 1, newChar.getInventory().getItemByItemId(1835).getObjectId(), -1, 1));
			}
		}
		
		if (Config.SPAWN_CHAR)
		{
			newChar.setXYZInvisible(Config.SPAWN_X, Config.SPAWN_Y, Config.SPAWN_Z);
		}
		else
		{
			newChar.setXYZInvisible(template.spawnX, template.spawnY, template.spawnZ);
		}
		
		if (Config.ALLOW_CREATE_LVL)
		{
			newChar.getStat().addExp(ExperienceData.getInstance().getExpForLevel(Config.CHAR_CREATE_LVL));
		}
		
		if (Config.CHAR_TITLE)
		{
			newChar.setTitle(Config.ADD_CHAR_TITLE);
		}
		else
		{
			newChar.setTitle("");
		}
		
		if (Config.CUSTOM_SHORTCUT_AND_MACRO)
		{
			// new macroses and shortcuts
			if (PowerPackConfig.BUFFER_USECOMMAND)
			{
				InsertNewMacro(newChar, 1000, 0, "Buffer", "", "BUFF", "3,0,0,.buffs;");
				newChar.registerShortCut(new L2ShortCut(5, 1, 4, 1000, -1, 0));
			}
			
			if (PowerPackConfig.GLOBALGK_USECOMMAND)
			{
				InsertNewMacro(newChar, 1001, 1, "Global GK", "", "GK", "3,0,0,.gk;");
				newChar.registerShortCut(new L2ShortCut(6, 1, 4, 1001, -1, 0));
			}
			
			InsertNewMacro(newChar, 1002, 2, "Vote Reward", "", "VR", "3,0,0,.votereward;");
			newChar.registerShortCut(new L2ShortCut(7, 1, 4, 1002, -1, 0));
			
			InsertNewMacro(newChar, 1003, 3, "Menu", "", "MENU", "3,0,0,.menu;");
			newChar.registerShortCut(new L2ShortCut(8, 1, 4, 1003, -1, 0));
			
			InsertNewMacro(newChar, 1004, 4, "Bosses", "", "BOSS", "3,0,0,.boss;");
			newChar.registerShortCut(new L2ShortCut(9, 1, 4, 1004, -1, 0));
			
			InsertNewMacro(newChar, 1005, 5, "Class Changer", "", "CLASS", "3,0,0,.class;");
			newChar.registerShortCut(new L2ShortCut(10, 1, 4, 1005, -1, 0));
			
			if (Config.CUSTOM_SUB_CLASS_COMMAND)
			{
				InsertNewMacro(newChar, 1006, 6, "Sub Class", "", "SUB", "3,0,0,.sub;");
				newChar.registerShortCut(new L2ShortCut(11, 1, 4, 1006, -1, 0));
			}
			
			if (PowerPackConfig.GMSHOP_USECOMMAND)
			{
				InsertNewMacro(newChar, 1007, 3, "Shop", "", "SHOP", "3,0,0,.shop;");
				newChar.registerShortCut(new L2ShortCut(4, 1, 4, 1007, -1, 0));
			}
		}
		
		if (Config.AUTOBUFFS_ON_CREATE)
		{
			ArrayList<L2Skill> skills_to_buff = new ArrayList<>();
			if (newChar.isMageClass())
			{
				for (int skillId : PowerPackConfig.MAGE_SKILL_LIST.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(skillId, PowerPackConfig.MAGE_SKILL_LIST.get(skillId));
					if (skill != null)
					{
						skills_to_buff.add(skill);
					}
				}
			}
			else
			{
				for (int skillId : PowerPackConfig.FIGHTER_SKILL_LIST.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(skillId, PowerPackConfig.FIGHTER_SKILL_LIST.get(skillId));
					if (skill != null)
					{
						skills_to_buff.add(skill);
					}
				}
			}
			for (L2Skill sk : skills_to_buff)
			{
				sk.getEffects(newChar, newChar, false, false, false);
			}
		}
		
		// Shortcuts
		newChar.registerShortCut(new L2ShortCut(0, 0, 3, 2, -1, 1)); // Attack
		newChar.registerShortCut(new L2ShortCut(3, 0, 3, 5, -1, 1)); // Take
		newChar.registerShortCut(new L2ShortCut(10, 0, 3, 0, -1, 1)); // Sit
		
		final L2Item[] items = template.getItems();
		for (final L2Item item2 : items)
		{
			final L2ItemInstance item = newChar.getInventory().addItem("Init", item2.getItemId(), 1, newChar, null);
			
			if (item.getItemId() == 5588)
			{
				newChar.registerShortCut(new L2ShortCut(11, 0, 1, item.getObjectId(), -1, 1)); // Tutorial Book shortcut
			}
			
			if (item.isEquipable())
			{
				if (newChar.getActiveWeaponItem() == null || !(item.getItem().getType2() != L2Item.TYPE2_WEAPON))
				{
					newChar.getInventory().equipItemAndRecord(item);
				}
				else
				{
					newChar.getInventory().equipItemAndRecord(item);
				}
			}
		}
		
		final L2SkillLearn[] startSkills = SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId());
		
		for (final L2SkillLearn startSkill : startSkills)
		{
			newChar.addSkill(SkillTable.getInstance().getInfo(startSkill.getId(), startSkill.getLevel()), true);
			
			if (startSkill.getId() == 1001 || startSkill.getId() == 1177)
			{
				newChar.registerShortCut(new L2ShortCut(1, 0, 2, startSkill.getId(), 1, 1));
			}
			
			if (startSkill.getId() == 1216)
			{
				newChar.registerShortCut(new L2ShortCut(10, 0, 2, startSkill.getId(), 1, 1));
			}
		}
		
		startTutorialQuest(newChar);
		newChar.store();
		newChar.deleteMe(); // Release the world of this character and it's inventory
		
		// Before the char selection, check shutdown status
		if (GameServer.gameServer.getSelectorThread().isShutdown())
		{
			client.closeNow();
			return;
		}
		
		// Send char list
		final CharSelectInfo cl = new CharSelectInfo(client.getAccountName(), client.getSessionId().playOkID1);
		client.getConnection().sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
		
		if (Config.DEBUG)
		{
			LOG.debug("DEBUG " + getType() + ": Character init end");
		}
	}
	
	private void InsertNewMacro(L2PcInstance player, int id, int icon, String name, String descr, String acronym, String command)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_macroses (char_obj_id,id,icon,name,descr,acronym,commands) values(?,?,?,?,?,?,?)");
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, id);
			statement.setInt(3, icon);
			statement.setString(4, name);
			statement.setString(5, descr);
			statement.setString(6, acronym);
			statement.setString(7, command);
			statement.execute();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.warn("could not store macro at create character:", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void startTutorialQuest(final L2PcInstance player)
	{
		final QuestState qs = player.getQuestState("255_Tutorial");
		Quest q = null;
		
		if (qs == null && !Config.ALT_DEV_NO_QUESTS)
		{
			q = QuestManager.getInstance().getQuest("255_Tutorial");
		}
		
		if (q != null)
		{
			q.newQuestState(player);
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 0B CharacterCreate";
	}
}
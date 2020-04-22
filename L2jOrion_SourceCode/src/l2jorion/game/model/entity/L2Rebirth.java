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
package l2jorion.game.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

/**
 * <strong>This 'Custom Engine' was developed for L2J Forum Member 'sauron3256' on November 1st, 2008.</strong><br>
 * <br>
 * <strong>Quick Summary:</strong><br>
 * This engine will grant the player special bonus skills at the cost of reseting him to level 1.<br>
 * The -USER- can set up to X Rebirths, the skills received and their respective levels, and the item and price of each rebirth.<br>
 * PLAYER's information is stored in an SQL Db under the table name: REBIRTH_MANAGER.<br>
 * @author <strong>Beetle and Shyla</strong>
 */
public class L2Rebirth
{
	private static Logger LOG = LoggerFactory.getLogger(L2Rebirth.class);
	
	/** The current instance - static repeller. */
	private static L2Rebirth _instance = null;
	
	/** Basically, this will act as a cache so it doesnt have to read DB information on relog. */
	private final HashMap<Integer, Integer> _playersRebirthInfo = new HashMap<>();
	
	/**
	 * Creates a new NON-STATIC instance.
	 */
	private L2Rebirth()
	{
		// Do Nothing ^_-
	}
	
	/**
	 * Receives the non-static instance of the RebirthManager.
	 * @return single instance of L2Rebirth
	 */
	public static L2Rebirth getInstance()
	{
		if (_instance == null)
		{
			_instance = new L2Rebirth();
		}
		return _instance;
	}
	
	/**
	 * This is what it called from the Bypass Handler. (I think that's all thats needed here).
	 * @param player the player
	 * @param command the command
	 */
	public void handleCommand(final L2PcInstance player, final String command)
	{
		if (command.startsWith("custom_rebirth_requestrebirth"))
		{
			displayRebirthWindow(player);
		}
		else if (command.startsWith("custom_rebirth_confirmrequest"))
		{
			requestRebirth(player);
		}
	}
	
	/**
	 * Display's an HTML window with the Rebirth Options.
	 * @param player the player
	 */
	public void displayRebirthWindow(final L2PcInstance player)
	{
		try
		{
			final int currBirth = getRebirthLevel(player); // Returns the player's current birth level
			
			// Don't send html if player is already at max rebirth count.
			if (currBirth >= Config.REBIRTH_MAX)
			{
				player.sendMessage("You are currently at your maximum rebirth count!");
				return;
			}
			
			// Returns true if BASE CLASS is a mage.
			final boolean isMage = player.getBaseTemplate().classId.isMage();
			// Returns the skill based on next Birth and if isMage.
			L2Skill skill = getRebirthSkill((currBirth + 1), isMage);
			
			String icon = "" + skill.getId();// Returns the skill's id.
			
			// Incase the skill is only 3 digits.
			if (icon.length() < 4)
			{
				icon = "0" + icon;
			}
			
			skill = null;
			icon = null;
			
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks to see if the player is eligible for a Rebirth, if so it grants it and stores information.
	 * @param player the player
	 */
	public void requestRebirth(final L2PcInstance player)
	{
		// Check to see if Rebirth is enabled to avoid hacks
		if (!Config.REBIRTH_ENABLE)
		{
			LOG.warn("[WARNING] Player " + player.getName() + " is trying to use rebirth system when it's disabled.");
			return;
		}
		
		// Check the player's level.
		if (player.getLevel() < Config.REBIRTH_MIN_LEVEL)
		{
			player.sendMessage("You do not meet the level requirement for a Rebirth!");
			return;
		}
		
		else if (player.isSubClassActive())
		{
			player.sendMessage("Please switch to your Main Class before attempting a Rebirth.");
			return;
		}
		
		final int currBirth = getRebirthLevel(player);
		int itemNeeded = 0;
		int itemAmount = 0;
		
		if (currBirth >= Config.REBIRTH_MAX)
		{
			player.sendMessage("You are currently at your maximum rebirth count!");
			return;
		}
		
		// Get the requirements
		int loopBirth = 0;
		for (final String readItems : Config.REBIRTH_ITEM_PRICE)
		{
			final String[] currItem = readItems.split(",");
			if (loopBirth == currBirth)
			{
				itemNeeded = Integer.parseInt(currItem[0]);
				itemAmount = Integer.parseInt(currItem[1]);
				break;
			}
			loopBirth++;
		}
		
		// Their is an item required
		if (itemNeeded != 0)
		{
			// Checks to see if player has required items, and takes them if so.
			if (!playerIsEligible(player, itemNeeded, itemAmount))
				return;
		}
		
		// Check and see if its the player's first Rebirth calling.
		final boolean firstBirth = currBirth == 0;
		// Player meets requirements and starts Rebirth Process.
		grantRebirth(player, (currBirth + 1), firstBirth);
	}
	
	/**
	 * Physically rewards player and resets status to nothing.
	 * @param player the player
	 * @param newBirthCount the new birth count
	 * @param firstBirth the first birth
	 */
	public void grantRebirth(final L2PcInstance player, final int newBirthCount, final boolean firstBirth)
	{
		try
		{
			final double actual_hp = player.getCurrentHp();
			final double actual_cp = player.getCurrentCp();
			
			int max_level = ExperienceData.getInstance().getMaxLevel();
			
			if (player.isSubClassActive())
			{
				max_level = Config.MAX_SUBCLASS_LEVEL;
			}
			
			// Protections
			Integer returnToLevel = Config.REBIRTH_RETURN_TO_LEVEL;
			if (returnToLevel < 1)
				returnToLevel = 1;
			if (returnToLevel > max_level)
				returnToLevel = max_level;
			
			// Resets character to first class.
			player.setClassId(player.getBaseClass());
			
			player.broadcastUserInfo();
			
			final byte lvl = Byte.parseByte(returnToLevel + "");
			
			final long pXp = player.getStat().getExp();
			final long tXp = ExperienceData.getInstance().getExpForLevel(lvl);
			
			if (pXp > tXp)
			{
				player.getStat().removeExpAndSp(pXp - tXp, 0);
			}
			else if (pXp < tXp)
			{
				player.getStat().addExpAndSp(tXp - pXp, 0);
			}
			
			// Remove the player's current skills.
			for (final L2Skill skill : player.getAllSkills())
			{
				player.removeSkill(skill);
			}
			// Give players their eligible skills.
			player.giveAvailableSkills();
			
			// restore Hp-Mp-Cp
			player.setCurrentCp(actual_cp);
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentHp(actual_hp);
			player.broadcastStatusUpdate();
			
			// Updates the player's information in the Character Database.
			player.store();
			
			if (firstBirth)
			{
				storePlayerBirth(player);
			}
			else
			{
				updatePlayerBirth(player, newBirthCount);
			}
			
			// Give the player his new Skills.
			grantRebirthSkills(player);
			
			// Displays a congratulation message to the player.
			displayCongrats(player);
			returnToLevel = null;
			
			// Update skill list
			player.sendSkillList();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Special effects when the player levels.
	 * @param player the player
	 */
	public void displayCongrats(final L2PcInstance player)
	{
		// Victory Social Action.
		player.setTarget(player);
		player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
		player.sendMessage("Congratulations " + player.getName() + ". You have been REBORN!");
	}
	
	/**
	 * Check and verify the player DOES have the item required for a request. Also, remove the item if he has.
	 * @param player the player
	 * @param itemId the item id
	 * @param itemAmount the item amount
	 * @return true, if successful
	 */
	public boolean playerIsEligible(final L2PcInstance player, final int itemId, final int itemAmount)
	{
		String itemName = ItemTable.getInstance().getTemplate(itemId).getName();
		L2ItemInstance itemNeeded = player.getInventory().getItemByItemId(itemId);
		
		if (itemNeeded == null || itemNeeded.getCount() < itemAmount)
		{
			player.sendMessage("You need atleast " + itemAmount + "  [ " + itemName + " ] to request a Rebirth!");
			return false;
		}
		
		// Player has the required items, so we're going to take them!
		player.getInventory().destroyItemByItemId("Rebirth Engine", itemId, itemAmount, player, null);
		player.sendMessage("Removed " + itemAmount + " " + itemName + " from your inventory!");
		
		itemName = null;
		itemNeeded = null;
		
		return true;
	}
	
	/**
	 * Gives the available Bonus Skills to the player.
	 * @param player the player
	 */
	public void grantRebirthSkills(final L2PcInstance player)
	{
		// returns the current Rebirth Level
		final int rebirthLevel = getRebirthLevel(player);
		// Returns true if BASE CLASS is a mage.
		final boolean isMage = player.getBaseTemplate().classId.isMage();
		
		// Simply return since no bonus skills are granted.
		if (rebirthLevel == 0)
			return;
		
		// Load the bonus skills unto the player.
		CreatureSay rebirthText = null;
		for (int i = 0; i < rebirthLevel; i++)
		{
			final L2Skill bonusSkill = getRebirthSkill((i + 1), isMage);
			player.addSkill(bonusSkill, false);
			
			// If you'd rather make it simple, simply comment this out and replace with a simple player.sendmessage();
			rebirthText = new CreatureSay(0, 18, "Rebirth Manager ", " Granted you [ " + bonusSkill.getName() + " ] level [ " + bonusSkill.getLevel() + " ]!");
			player.sendPacket(rebirthText);
		}
		
		rebirthText = null;
	}
	
	/**
	 * Return the player's current Rebirth Level.
	 * @param player the player
	 * @return the rebirth level
	 */
	public int getRebirthLevel(final L2PcInstance player)
	{
		final int playerId = player.getObjectId();
		
		if (_playersRebirthInfo.get(playerId) == null)
		{
			loadRebirthInfo(player);
		}
		
		return _playersRebirthInfo.get(playerId);
	}
	
	/**
	 * Return the L2Skill the player is going to be rewarded.
	 * @param rebirthLevel the rebirth level
	 * @param mage the mage
	 * @return the rebirth skill
	 */
	public L2Skill getRebirthSkill(final int rebirthLevel, final boolean mage)
	{
		L2Skill skill = null;
		
		// Player is a Mage.
		if (mage)
		{
			int loopBirth = 0;
			for (final String readSkill : Config.REBIRTH_MAGE_SKILL)
			{
				final String[] currSkill = readSkill.split(",");
				if (loopBirth == (rebirthLevel - 1))
				{
					skill = SkillTable.getInstance().getInfo(Integer.parseInt(currSkill[0]), Integer.parseInt(currSkill[1]));
					break;
				}
				loopBirth++;
			}
		}
		// Player is a Fighter.
		else
		{
			int loopBirth = 0;
			for (final String readSkill : Config.REBIRTH_FIGHTER_SKILL)
			{
				final String[] currSkill = readSkill.split(",");
				if (loopBirth == (rebirthLevel - 1))
				{
					skill = SkillTable.getInstance().getInfo(Integer.parseInt(currSkill[0]), Integer.parseInt(currSkill[1]));
					break;
				}
				loopBirth++;
			}
		}
		return skill;
	}
	
	/**
	 * Database caller to retrieve player's current Rebirth Level.
	 * @param player the player
	 */
	public void loadRebirthInfo(final L2PcInstance player)
	{
		final int playerId = player.getObjectId();
		int rebirthCount = 0;
		
		Connection con = null;
		try
		{
			ResultSet rset;
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM `rebirth_manager` WHERE playerId = ?");
			statement.setInt(1, playerId);
			rset = statement.executeQuery();
			
			while (rset.next())
			{
				rebirthCount = rset.getInt("rebirthCount");
			}
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
			statement = null;
			rset = null;
			
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		_playersRebirthInfo.put(playerId, rebirthCount);
	}
	
	/**
	 * Stores the player's information in the DB.
	 * @param player the player
	 */
	public void storePlayerBirth(final L2PcInstance player)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO `rebirth_manager` (playerId,rebirthCount) VALUES (?,1)");
			statement.setInt(1, player.getObjectId());
			statement.execute();
			statement = null;
			
			_playersRebirthInfo.put(player.getObjectId(), 1);
			
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Updates the player's information in the DB.
	 * @param player the player
	 * @param newRebirthCount the new rebirth count
	 */
	public void updatePlayerBirth(final L2PcInstance player, final int newRebirthCount)
	{
		Connection con = null;
		try
		{
			final int playerId = player.getObjectId();
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE `rebirth_manager` SET rebirthCount = ? WHERE playerId = ?");
			statement.setInt(1, newRebirthCount);
			statement.setInt(2, playerId);
			statement.execute();
			statement = null;
			
			_playersRebirthInfo.put(playerId, newRebirthCount);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
}
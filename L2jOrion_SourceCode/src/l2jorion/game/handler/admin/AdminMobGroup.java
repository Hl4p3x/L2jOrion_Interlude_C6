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
package l2jorion.game.handler.admin;

import l2jorion.Config;
import l2jorion.game.datatables.MobGroupTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2World;
import l2jorion.game.model.MobGroup;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.SetupGauge;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;

public class AdminMobGroup implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_mobmenu",
		"admin_mobgroup_list",
		"admin_mobgroup_create",
		"admin_mobgroup_remove",
		"admin_mobgroup_delete",
		"admin_mobgroup_spawn",
		"admin_mobgroup_unspawn",
		"admin_mobgroup_kill",
		"admin_mobgroup_idle",
		"admin_mobgroup_attack",
		"admin_mobgroup_rnd",
		"admin_mobgroup_return",
		"admin_mobgroup_follow",
		"admin_mobgroup_casting",
		"admin_mobgroup_nomove",
		"admin_mobgroup_attackgrp",
		"admin_mobgroup_invul",
		"admin_mobinst"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (command.equals("admin_mobmenu"))
		{
			showMainPage(activeChar, command);
			return true;
		}
		else if (command.equals("admin_mobinst"))
		{
			showMainPage(activeChar, command);
			return true;
		}
		else if (command.equals("admin_mobgroup_list"))
		{
			showGroupList(activeChar);
		}
		else if (command.startsWith("admin_mobgroup_create"))
		{
			createGroup(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_delete") || command.startsWith("admin_mobgroup_remove"))
		{
			removeGroup(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_spawn"))
		{
			spawnGroup(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_unspawn"))
		{
			unspawnGroup(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_kill"))
		{
			killGroup(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_attackgrp"))
		{
			attackGrp(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_attack"))
		{
			if (activeChar.getTarget() instanceof L2Character)
			{
				L2Character target = (L2Character) activeChar.getTarget();
				attack(command, activeChar, target);
				target = null;
			}
		}
		else if (command.startsWith("admin_mobgroup_rnd"))
		{
			setNormal(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_idle"))
		{
			idle(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_return"))
		{
			returnToChar(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_follow"))
		{
			follow(command, activeChar, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_casting"))
		{
			setCasting(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_nomove"))
		{
			noMove(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_invul"))
		{
			invul(command, activeChar);
		}
		else if (command.startsWith("admin_mobgroup_teleport"))
		{
			teleportGroup(command, activeChar);
		}
		
		showMainPage(activeChar, command);
		
		return true;
	}
	
	/**
	 * @param activeChar
	 * @param command
	 */
	private void showMainPage(final L2PcInstance activeChar, final String command)
	{
		String filename = "mobgroup.htm";
		
		if (command.contains("mobinst"))
		{
			filename = "mobgrouphelp.htm";
		}
		
		AdminHelpPage.showHelpPage(activeChar, filename);
		
		filename = null;
	}
	
	private void returnToChar(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Incorrect command arguments.");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		group.returnGroup(activeChar);
		
		group = null;
	}
	
	private void idle(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Incorrect command arguments.");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		group.setIdleMode();
		
		group = null;
	}
	
	private void setNormal(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Incorrect command arguments.");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		group.setAttackRandom();
		
		group = null;
	}
	
	private void attack(final String command, final L2PcInstance activeChar, final L2Character target)
	{
		int groupId;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Incorrect command arguments.");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		group.setAttackTarget(target);
		
		group = null;
	}
	
	private void follow(final String command, final L2PcInstance activeChar, final L2Character target)
	{
		int groupId;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Incorrect command arguments.");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		group.setFollowMode(target);
		group = null;
	}
	
	private void createGroup(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		int templateId;
		int mobCount;
		
		try
		{
			String[] cmdParams = command.split(" ");
			
			groupId = Integer.parseInt(cmdParams[1]);
			templateId = Integer.parseInt(cmdParams[2]);
			mobCount = Integer.parseInt(cmdParams[3]);
			cmdParams = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Usage: //mobgroup_create <group> <npcid> <count>");
			return;
		}
		
		if (MobGroupTable.getInstance().getGroup(groupId) != null)
		{
			activeChar.sendMessage("Mob group " + groupId + " already exists.");
			return;
		}
		
		L2NpcTemplate template = NpcTable.getInstance().getTemplate(templateId);
		
		if (template == null)
		{
			activeChar.sendMessage("Invalid NPC ID specified.");
			return;
		}
		
		MobGroup group = new MobGroup(groupId, template, mobCount);
		MobGroupTable.getInstance().addGroup(groupId, group);
		
		activeChar.sendMessage("Mob group " + groupId + " created.");
		
		template = null;
		group = null;
	}
	
	private void removeGroup(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Usage: //mobgroup_remove <groupId>");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		doAnimation(activeChar);
		group.unspawnGroup();
		
		if (MobGroupTable.getInstance().removeGroup(groupId))
		{
			activeChar.sendMessage("Mob group " + groupId + " unspawned and removed.");
		}
		
		group = null;
	}
	
	private void spawnGroup(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		boolean topos = false;
		int posx = 0;
		int posy = 0;
		int posz = 0;
		
		try
		{
			String[] cmdParams = command.split(" ");
			groupId = Integer.parseInt(cmdParams[1]);
			
			try
			{
				// we try to get a position
				posx = Integer.parseInt(cmdParams[2]);
				posy = Integer.parseInt(cmdParams[3]);
				posz = Integer.parseInt(cmdParams[4]);
				topos = true;
			}
			catch (final Exception e)
			{
				// no position given
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
			}
			
			cmdParams = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Usage: //mobgroup_spawn <group> [ x y z ]");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		doAnimation(activeChar);
		
		if (topos)
		{
			group.spawnGroup(posx, posy, posz);
		}
		else
		{
			group.spawnGroup(activeChar);
		}
		
		activeChar.sendMessage("Mob group " + groupId + " spawned.");
		
		group = null;
	}
	
	private void unspawnGroup(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Usage: //mobgroup_unspawn <groupId>");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		doAnimation(activeChar);
		group.unspawnGroup();
		
		activeChar.sendMessage("Mob group " + groupId + " unspawned.");
		
		group = null;
	}
	
	private void killGroup(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Usage: //mobgroup_kill <groupId>");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		doAnimation(activeChar);
		group.killGroup(activeChar);
		
		group = null;
	}
	
	private void setCasting(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Usage: //mobgroup_casting <groupId>");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		group.setCastMode();
		
		group = null;
	}
	
	private void noMove(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		
		String enabled;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
			enabled = command.split(" ")[2];
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Usage: //mobgroup_nomove <groupId> <on|off>");
			return;
		}
		
		final MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		if (enabled.equalsIgnoreCase("on") || enabled.equalsIgnoreCase("true"))
		{
			group.setNoMoveMode(true);
		}
		else if (enabled.equalsIgnoreCase("off") || enabled.equalsIgnoreCase("false"))
		{
			group.setNoMoveMode(false);
		}
		else
		{
			activeChar.sendMessage("Incorrect command arguments.");
		}
		
		enabled = null;
	}
	
	private void doAnimation(final L2PcInstance activeChar)
	{
		activeChar.broadcastPacket(new MagicSkillUser(activeChar, 1008, 1, 4000, 0), 2250000);
		activeChar.sendPacket(new SetupGauge(0, 4000));
	}
	
	private void attackGrp(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		int othGroupId;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
			othGroupId = Integer.parseInt(command.split(" ")[2]);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Usage: //mobgroup_attackgrp <groupId> <TargetGroupId>");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		MobGroup othGroup = MobGroupTable.getInstance().getGroup(othGroupId);
		
		if (othGroup == null)
		{
			activeChar.sendMessage("Incorrect target group.");
			return;
		}
		
		group.setAttackGroup(othGroup);
		
		group = null;
		othGroup = null;
	}
	
	private void invul(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		
		String enabled;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
			enabled = command.split(" ")[2];
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Usage: //mobgroup_invul <groupId> <on|off>");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		if (enabled.equalsIgnoreCase("on") || enabled.equalsIgnoreCase("true"))
		{
			group.setInvul(true);
		}
		else if (enabled.equalsIgnoreCase("off") || enabled.equalsIgnoreCase("false"))
		{
			group.setInvul(false);
		}
		else
		{
			activeChar.sendMessage("Incorrect command arguments.");
		}
		
		group = null;
		enabled = null;
	}
	
	private void teleportGroup(final String command, final L2PcInstance activeChar)
	{
		int groupId;
		String targetPlayerStr = null;
		L2PcInstance targetPlayer = null;
		
		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
			targetPlayerStr = command.split(" ")[2];
			
			if (targetPlayerStr != null)
			{
				targetPlayer = L2World.getInstance().getPlayer(targetPlayerStr);
			}
			
			if (targetPlayer == null)
			{
				targetPlayer = activeChar;
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			activeChar.sendMessage("Usage: //mobgroup_teleport <groupId> [playerName]");
			return;
		}
		
		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);
		
		if (group == null)
		{
			activeChar.sendMessage("Invalid group specified.");
			return;
		}
		
		group.teleportGroup(activeChar);
		
		group = null;
		targetPlayer = null;
		targetPlayerStr = null;
	}
	
	private void showGroupList(final L2PcInstance activeChar)
	{
		MobGroup[] mobGroupList = MobGroupTable.getInstance().getGroups();
		
		activeChar.sendMessage("======= <Mob Groups> =======");
		
		for (final MobGroup mobGroup : mobGroupList)
		{
			activeChar.sendMessage(mobGroup.getGroupId() + ": " + mobGroup.getActiveMobCount() + " alive out of " + mobGroup.getMaxMobCount() + " of NPC ID " + mobGroup.getTemplate().npcId + " (" + mobGroup.getStatus() + ")");
		}
		
		activeChar.sendPacket(new SystemMessage(SystemMessageId.FRIEND_LIST_FOOT));
		
		mobGroupList = null;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
}
